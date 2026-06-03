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
                // Шаг 1: Проверка и скачивание портативной Java (JRE)
                //JreManager.installIfMissing(window);

                // Шаг 2: Установка ядра NeoForge
                NeoForgeInstaller forgeInstaller = new NeoForgeInstaller(window);
                forgeInstaller.install();

                // Шаг 3: Синхронизация модов и библиотек по манифесту
                window.setStatus("Синхронизация файлов EternalSky...");
                ModManager modManager = new ModManager(window);
                modManager.syncFiles();

                // Шаг 4: Интеграция профиля в launcher_profiles.json
                window.setStatus("Интеграция профиля...");
                ProfileInjector.inject();

                // Шаг 5: Запуск игры
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
