package etg.ipsipdown.launcher.core;

import etg.ipsipdown.launcher.ui.LauncherWindow;

public class UpdateCoordinator {

    private final LauncherWindow window;

    public UpdateCoordinator(LauncherWindow window) {
        this.window = window;
    }

    public void startUpdateProcess() {
        new Thread(() -> {
            try {




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
                window.setButtonEnabled(true);
            }
        }).start();
    }
}
