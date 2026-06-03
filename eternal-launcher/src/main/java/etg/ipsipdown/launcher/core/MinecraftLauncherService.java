package etg.ipsipdown.launcher.core;

import etg.ipsipdown.launcher.ui.LauncherWindow;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MinecraftLauncherService {

    public static void launchOfficialMinecraft(LauncherWindow window) {
        String appData = System.getenv("APPDATA");
        String p86 = System.getenv("ProgramFiles(x86)");
        String pf = System.getenv("ProgramFiles");

        // 1. Проверяем обычные пути
        Path[] possiblePaths = {
                Paths.get(p86, "Minecraft Launcher", "MinecraftLauncher.exe"),
                Paths.get(pf, "Minecraft Launcher", "MinecraftLauncher.exe"),
                Paths.get(appData, ".minecraft", "MinecraftLauncher.exe")
        };

        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                try {
                    new ProcessBuilder(path.toString()).start();
                    return;
                } catch (Exception ignored) {}
            }
        }

        // 2. Если по путям не нашли, пробуем запустить через Shell (версия из Microsoft Store)
        try {
            // Запуск через explorer.exe сработает для URI формата shell:AppsFolder\...
            new ProcessBuilder("explorer.exe", "shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft").start();
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 3. Если вообще ничего не помогло
        window.setStatus("Готово! Открой лаунчер вручную.");
    }
}