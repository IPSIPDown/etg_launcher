package etg.ipsipdown.launcher.controllers;

import etg.ipsipdown.launcher.events.ProgressListener;
import etg.ipsipdown.launcher.models.LauncherSettings;
import etg.ipsipdown.launcher.models.SyncResult;
import etg.ipsipdown.launcher.services.DownloadService;
import etg.ipsipdown.launcher.services.MinecraftLauncherService;
import etg.ipsipdown.launcher.services.NeoForgeInstaller;
import etg.ipsipdown.launcher.services.PrismInstanceManager;
import etg.ipsipdown.launcher.services.ProfileInjector;
import etg.ipsipdown.launcher.services.SyncService;
import etg.ipsipdown.launcher.services.ThirdPartyLauncherDetector;
import etg.ipsipdown.launcher.services.ThirdPartyLauncherDetector.LauncherInfo;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Оркестратор запуска:
 * - Official: NeoForge → синхронизация → профиль → официальный лаунчер
 * - Third-party: синхронизация → авто-определение лаунчера → диалог → запуск
 */
public class LaunchController {

    private static final Logger log = LoggerFactory.getLogger(LaunchController.class);

    private final ProgressListener progress;
    private final Runnable onLaunchFailed;
    private final Consumer<SyncResult> onSyncResult;

    public LaunchController(ProgressListener progress, Runnable onLaunchFailed,
                            Consumer<SyncResult> onSyncResult) {
        this.progress = progress;
        this.onLaunchFailed = onLaunchFailed;
        this.onSyncResult = onSyncResult;
    }

    public void startLaunch(boolean isCleanLaunch) {
        Thread t = new Thread(() -> {
            try {
                if (isCleanLaunch) {
                    progress.onStatus("Активация всех модов для чистого запуска...");
                    enableAllMods();
                }

                LauncherSettings settings = LauncherSettings.load();
                DownloadService downloader = new DownloadService();

                if ("thirdparty".equals(settings.launcherType)) {
                    launchThirdParty(downloader);
                } else {
                    launchOfficial(downloader);
                }

            } catch (Exception e) {
                log.error("Ошибка процесса обновления", e);
                progress.onStatus("Ошибка обновления: " + e.getMessage());
                onLaunchFailed.run();
            }
        }, "launch");
        t.start();
    }

    private void launchOfficial(DownloadService downloader) throws Exception {
        new NeoForgeInstaller(progress, downloader).install();

        progress.onStatus("Синхронизация файлов EternalSky...");
        SyncResult syncResult = new SyncService(progress, downloader).syncFiles();
        notifySyncResult(syncResult);

        progress.onStatus("Интеграция профиля...");
        ProfileInjector.inject();

        progress.onStatus("Запуск Minecraft!");
        progress.onProgress(100);
        MinecraftLauncherService.launchOfficialMinecraft(progress);

        Thread.sleep(2000);
        etg.ipsipdown.launcher.services.CompanionMode.enterOrExit();
    }

    private void launchThirdParty(DownloadService downloader) throws Exception {
        // 1. Найти доступные лаунчеры
        progress.onStatus("Поиск сторонних лаунчеров...");
        List<LauncherInfo> launchers = ThirdPartyLauncherDetector.detect();

        if (launchers.isEmpty()) {
            progress.onStatus("Сторонний лаунчер не найден.");
            JOptionPane.showMessageDialog(null,
                    "Не удалось найти ни одного стороннего лаунчера.\n" +
                    "Установи PrismLauncher, MultiMC или другой и попробуй снова.",
                    "Лаунчер не найден", JOptionPane.WARNING_MESSAGE);
            onLaunchFailed.run();
            return;
        }

        // 2. Спросить пользователя какой запустить
        LauncherInfo[] selected = {null};
        SwingUtilities.invokeAndWait(() -> selected[0] = showPickerDialog(launchers));

        if (selected[0] == null) {
            onLaunchFailed.run();
            return;
        }

        // 3. Для PrismLauncher/MultiMC — создать инстанс и синхронизировать в его .minecraft
        LauncherSettings settings = LauncherSettings.load();
        LauncherInfo launcherToRun = selected[0];
        java.nio.file.Path syncDir = OsPaths.GAME_DIR;

        if (PrismInstanceManager.supports(selected[0])) {
            progress.onStatus("Подготовка инстанса EternalSky...");
            java.nio.file.Path minecraftDir = PrismInstanceManager.ensureInstance(selected[0], settings);
            syncDir = minecraftDir;

            // Запускать конкретный инстанс, а не просто открывать лаунчер
            String binary = selected[0].command()[0];
            launcherToRun = new LauncherInfo(
                    selected[0].name(),
                    new String[]{binary, "--launch", PrismInstanceManager.INSTANCE_NAME}
            );
        }

        // 4. Синхронизировать моды в нужную папку
        progress.onStatus("Синхронизация файлов EternalSky...");
        SyncResult syncResult = new SyncService(progress, downloader, syncDir).syncFiles();
        notifySyncResult(syncResult);

        // 5. Запустить
        progress.onStatus("Запуск " + selected[0].name() + "!");
        progress.onProgress(100);
        MinecraftLauncherService.launchThirdParty(launcherToRun);

        Thread.sleep(2000);
        etg.ipsipdown.launcher.services.CompanionMode.enterOrExit();
    }

    /** Показывает диалог «У вас есть X. Запустить?» и возвращает выбранный лаунчер или null. */
    private static LauncherInfo showPickerDialog(List<LauncherInfo> launchers) {
        if (launchers.size() == 1) {
            LauncherInfo only = launchers.get(0);
            int choice = JOptionPane.showConfirmDialog(null,
                    "У вас есть " + only.name() + ". Запустить?",
                    "Сторонний лаунчер", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            return choice == JOptionPane.YES_OPTION ? only : null;
        }

        // Несколько лаунчеров — предлагаем выбор
        String[] buttons = new String[launchers.size() + 1];
        for (int i = 0; i < launchers.size(); i++) buttons[i] = launchers.get(i).name();
        buttons[launchers.size()] = "Отмена";

        StringBuilder msg = new StringBuilder("Найдено несколько лаунчеров:");
        for (LauncherInfo l : launchers) msg.append("\n  • ").append(l.name());
        msg.append("\n\nКакой запустить?");

        int choice = JOptionPane.showOptionDialog(null, msg.toString(),
                "Сторонний лаунчер", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, buttons, buttons[0]);

        if (choice < 0 || choice >= launchers.size()) return null;
        return launchers.get(choice);
    }

    private void notifySyncResult(SyncResult syncResult) {
        if (onSyncResult != null && syncResult.hasModChanges()) {
            onSyncResult.accept(syncResult);
        }
    }

    private void enableAllMods() {
        Path modsDir = OsPaths.MODS_DIR;
        if (Files.exists(modsDir)) {
            try {
                Files.list(modsDir).forEach(file -> {
                    if (file.toString().endsWith(".jar.disabled")) {
                        try {
                            String newName = file.getFileName().toString().replace(".disabled", "");
                            Files.move(file, file.resolveSibling(newName));
                        } catch (Exception e) {
                            log.warn("Не удалось включить мод: {}", e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Ошибка при включении модов", e);
            }
        }
    }
}
