package etg.ipsipdown.launcher.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import etg.ipsipdown.launcher.events.ProgressListener;
import etg.ipsipdown.launcher.models.LauncherSettings;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MinecraftLauncherService {

    private static final Logger log = LoggerFactory.getLogger(MinecraftLauncherService.class);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void launchOfficialMinecraft(ProgressListener progress) {
        String p86 = System.getenv("ProgramFiles(x86)");
        String pf = System.getenv("ProgramFiles");

        // Обновляем профиль (память, JVM-аргументы) перед запуском
        updateLauncherProfiles();

        Path[] possiblePaths = {
                Paths.get(p86, "Minecraft Launcher", "MinecraftLauncher.exe"),
                Paths.get(pf, "Minecraft Launcher", "MinecraftLauncher.exe"),
                OsPaths.MINECRAFT_DIR.resolve("MinecraftLauncher.exe")
        };

        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                try {
                    new ProcessBuilder(path.toString()).start();
                    return;
                } catch (Exception e) {
                    log.warn("Не удалось запустить {}: {}", path, e.getMessage());
                }
            }
        }

        try {
            new ProcessBuilder("explorer.exe", "shell:AppsFolder\\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft").start();
            return;
        } catch (Exception e) {
            log.error("Не удалось запустить официальный лаунчер Minecraft", e);
        }

        progress.onStatus("Готово! Открой лаунчер вручную.");
    }

    private static void updateLauncherProfiles() {
        Path profilesPath = OsPaths.MINECRAFT_DIR.resolve("launcher_profiles.json");
        if (!Files.exists(profilesPath)) return;

        try {
            LauncherSettings settings = LauncherSettings.load();

            // ВАЖНО: сюда идут только аргументы JVM. Игровые аргументы (--server и т.п.)
            // официальный лаунчер через javaArgs не передаёт — раньше это ломало запуск.
            String formattedJavaArgs = "-Xmx" + settings.ramMegabytes + "M -Xms" + settings.ramMegabytes + "M " + settings.jvmArgs;

            JsonObject root;
            try (Reader reader = Files.newBufferedReader(profilesPath)) {
                root = GSON.fromJson(reader, JsonObject.class);
            }

            if (root == null || !root.has("profiles")) return;
            JsonObject profiles = root.getAsJsonObject("profiles");

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
                targetProfile.addProperty("javaArgs", formattedJavaArgs);

                if (settings.customJavaPath != null && !settings.customJavaPath.isEmpty()) {
                    targetProfile.addProperty("javaDir", settings.customJavaPath);
                }

                try (Writer writer = Files.newBufferedWriter(profilesPath)) {
                    GSON.toJson(root, writer);
                }
            }
        } catch (Exception e) {
            log.error("Ошибка обновления настроек профиля", e);
        }
    }
}
