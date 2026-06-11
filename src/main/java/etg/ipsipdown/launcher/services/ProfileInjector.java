package etg.ipsipdown.launcher.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class ProfileInjector {

    private static final Logger log = LoggerFactory.getLogger(ProfileInjector.class);

    public static void inject() {
        try {
            Path profilesFile = OsPaths.MINECRAFT_DIR.resolve("launcher_profiles.json");

            if (!Files.exists(profilesFile)) {
                log.warn("Официальный лаунчер не найден (нет launcher_profiles.json).");
                return;
            }

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonObject profilesJson;

            try (Reader reader = Files.newBufferedReader(profilesFile)) {
                profilesJson = gson.fromJson(reader, JsonObject.class);
            }

            JsonObject myProfile = new JsonObject();
            myProfile.addProperty("name", "EternalSky");
            myProfile.addProperty("type", "custom");
            myProfile.addProperty("created", Instant.now().toString());
            myProfile.addProperty("lastVersionId", NeoForgeInstaller.VERSION_NAME);
            myProfile.addProperty("icon", "Furnace");
            myProfile.addProperty("gameDir", OsPaths.GAME_DIR.toAbsolutePath().toString());

            JsonObject profilesObject = profilesJson.getAsJsonObject("profiles");
            profilesObject.add("EternalSky_Profile", myProfile);

            try (Writer writer = Files.newBufferedWriter(profilesFile)) {
                gson.toJson(profilesJson, writer);
            }

        } catch (Exception e) {
            log.error("Ошибка при создании профиля", e);
        }
    }
}
