package etg.ipsipdown.launcher.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import etg.ipsipdown.launcher.ui.LauncherWindow;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MinecraftLauncherService {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void launchOfficialMinecraft(LauncherWindow window) {
        String appData = System.getenv("APPDATA");
        String p86 = System.getenv("ProgramFiles(x86)");
        String pf = System.getenv("ProgramFiles");

        // --- ОБНОВЛЕНИЕ ПРОФИЛЕЙ ПЕРЕД ЗАПУСКОМ ---
        updateLauncherProfiles(appData);
        // ------------------------------------------

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
                } catch (Exception ignored) {
                }
            }
        }

        try {
            new ProcessBuilder("explorer.exe", "shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft").start();
            return;
        } catch (Exception e) {
            e.printStackTrace();
        }

        window.setStatus("Готово! Открой лаунчер вручную.");
    }

    private static void updateLauncherProfiles(String appData) {
        Path profilesPath = Paths.get(appData, ".minecraft", "launcher_profiles.json");
        if (!Files.exists(profilesPath)) return;

        try {
            LauncherSettings settings = LauncherSettings.load();

            // Формируем базу аргументов
            String formattedJavaArgs = "-Xmx" + settings.ramMegabytes + "M -Xms" + settings.ramMegabytes + "M " + settings.jvmArgs;

            // --- ДОБАВЛЯЕМ АРГУМЕНТЫ АВТО-ВХОДА ---
            if (settings.autoConnect) {
                formattedJavaArgs += " --server " + settings.serverIp + " --port " + settings.serverPort;
            }
            // --------------------------------------

            JsonObject root;
            try (Reader reader = Files.newBufferedReader(profilesPath)) {
                root = GSON.fromJson(reader, JsonObject.class);
            }

            if (root == null || !root.has("profiles")) return;
            JsonObject profiles = root.getAsJsonObject("profiles");

            // Ищем профиль (оставляем твой код поиска)
            String targetProfileId = "EternalSky";
            JsonObject targetProfile = null;

            if (profiles.has(targetProfileId)) {
                targetProfile = profiles.getAsJsonObject(targetProfileId);
            } else {
                for (String key : profiles.keySet()) {
                    JsonObject profile = profiles.getAsJsonObject(key);
                    if (profile.has("name") && profile.get("name").getAsString().equalsIgnoreCase("EternalSky")) {
                        targetProfile = profile;
                        break;
                    }
                }
            }

            if (targetProfile != null) {
                // Обновляем аргументы
                targetProfile.addProperty("javaArgs", formattedJavaArgs);

                if (settings.customJavaPath != null && !settings.customJavaPath.isEmpty()) {
                    targetProfile.addProperty("javaDir", settings.customJavaPath);
                }

                try (Writer writer = Files.newBufferedWriter(profilesPath)) {
                    GSON.toJson(root, writer);
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка обновления настроек: " + e.getMessage());
        }
    }
}