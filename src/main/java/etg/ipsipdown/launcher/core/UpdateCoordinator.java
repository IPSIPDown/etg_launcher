package etg.ipsipdown.launcher.core;

import etg.ipsipdown.launcher.ui.LauncherWindow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateCoordinator {

    private final LauncherWindow window;

    public UpdateCoordinator(LauncherWindow window) {
        this.window = window;
    }

    public void startUpdateProcess(boolean isCleanLaunch) {
        new Thread(() -> {
            try {
                // Если выбран "Чистый запуск", принудительно включаем все моды
                if (isCleanLaunch) {
                    window.setStatus("Активация всех модов для чистого запуска...");
                    enableAllMods();
                }

                NeoForgeInstaller forgeInstaller = new NeoForgeInstaller(window);
                forgeInstaller.install();

                window.setStatus("Синхронизация файлов EternalSky...");
                ModManager modManager = new ModManager(window);
                modManager.syncFiles();

                window.setStatus("Интеграция профиля...");
                ProfileInjector.inject();

                window.setStatus("Запуск Minecraft!");
                window.setProgress(100);
                MinecraftLauncherService.launchOfficialMinecraft(window);

                Thread.sleep(2000);
                System.exit(0);

            } catch (Exception e) {
                e.printStackTrace();
                window.setStatus("Ошибка обновления: " + e.getMessage());
                window.setButtonsEnabled(true);
            }
        }).start();
    }

    private void enableAllMods() {
        String appData = System.getenv("APPDATA");
        // Убедись, что путь совпадает с тем, который ты используешь в SettingsPanel
        Path modsDir = Paths.get(appData, ".minecraft", "mods");

        if (Files.exists(modsDir)) {
            try {
                Files.list(modsDir).forEach(file -> {
                    if (file.toString().endsWith(".jar.disabled")) {
                        try {
                            String newName = file.getFileName().toString().replace(".disabled", "");
                            Files.move(file, file.resolveSibling(newName));
                        } catch (Exception e) {
                            System.err.println("Не удалось включить мод: " + e.getMessage());
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}