package etg.ipsipdown.launcher.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

public class ProfileInjector {

    public static void inject() {
        try {
            String appData = System.getenv("APPDATA");
            Path mcDir = Paths.get(appData, ".minecraft");
            Path profilesFile = mcDir.resolve("launcher_profiles.json");
            Path gameDir = Paths.get(appData, ".eternalsky");


            if (!Files.exists(profilesFile)) {
                System.out.println("Официальный лаунчер не найден.");
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


            myProfile.addProperty("lastVersionId", "neoforge-21.1.228");

            myProfile.addProperty("icon", "Furnace");
            myProfile.addProperty("gameDir", gameDir.toAbsolutePath().toString());




            JsonObject profilesObject = profilesJson.getAsJsonObject("profiles");
            profilesObject.add("EternalSky_Profile", myProfile);


            try (Writer writer = Files.newBufferedWriter(profilesFile)) {
                gson.toJson(profilesJson, writer);
            }

        } catch (Exception e) {
            System.err.println("Ошибка при создании профиля: " + e.getMessage());
        }
    }
}