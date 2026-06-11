package etg.ipsipdown.launcher.controllers;

import etg.ipsipdown.launcher.events.ProgressListener;
import etg.ipsipdown.launcher.models.SyncResult;
import etg.ipsipdown.launcher.services.DownloadService;
import etg.ipsipdown.launcher.services.MinecraftLauncherService;
import etg.ipsipdown.launcher.services.NeoForgeInstaller;
import etg.ipsipdown.launcher.services.ProfileInjector;
import etg.ipsipdown.launcher.services.SyncService;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Оркестратор запуска (бывший UpdateCoordinator):
 * NeoForge -> синхронизация файлов -> профиль -> официальный лаунчер.
 */
public class LaunchController {

    private static final Logger log = LoggerFactory.getLogger(LaunchController.class);

    private final ProgressListener progress;
    private final Runnable onLaunchFailed;
    private final java.util.function.Consumer<SyncResult> onSyncResult;

    public LaunchController(ProgressListener progress, Runnable onLaunchFailed,
                            java.util.function.Consumer<SyncResult> onSyncResult) {
        this.progress = progress;
        this.onLaunchFailed = onLaunchFailed;
        this.onSyncResult = onSyncResult;
    }

    public void startLaunch(boolean isCleanLaunch) {
        Thread t = new Thread(() -> {
            try {
                // Если выбран "Чистый запуск", принудительно включаем все моды
                if (isCleanLaunch) {
                    progress.onStatus("Активация всех модов для чистого запуска...");
                    enableAllMods();
                }

                DownloadService downloader = new DownloadService();

                new NeoForgeInstaller(progress, downloader).install();

                progress.onStatus("Синхронизация файлов EternalSky...");
                SyncResult syncResult = new SyncService(progress, downloader).syncFiles();
                if (onSyncResult != null && syncResult.hasModChanges()) {
                    onSyncResult.accept(syncResult);
                }

                progress.onStatus("Интеграция профиля...");
                ProfileInjector.inject();

                progress.onStatus("Запуск Minecraft!");
                progress.onProgress(100);
                MinecraftLauncherService.launchOfficialMinecraft(progress);

                Thread.sleep(2000);
                System.exit(0);

            } catch (Exception e) {
                log.error("Ошибка процесса обновления", e);
                progress.onStatus("Ошибка обновления: " + e.getMessage());
                onLaunchFailed.run();
            }
        }, "launch");
        t.start();
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
