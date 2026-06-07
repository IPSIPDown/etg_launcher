package etg.ipsipdown.launcher.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LauncherUpdater {

    
    public static final String CURRENT_VERSION = "1.0.1";

    
    private static final String VERSION_URL = "https://pub-97e8a596b9e44332a4b339c99ee9ef01.r2.dev/Launcher/launcher-version.json";

    public static void checkAndUpdate() {
        try {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(VERSION_URL)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return; 

            JsonObject data = new Gson().fromJson(response.body(), JsonObject.class);
            String latestVersion = data.get("version").getAsString();
            String downloadUrl = data.get("url").getAsString();

            
            if (!CURRENT_VERSION.equals(latestVersion)) {

                JOptionPane.showMessageDialog(null,
                        "Найдено обновление лаунчера! Сейчас он будет обновлен и перезапущен.",
                        "Обновление EternalSky", JOptionPane.INFORMATION_MESSAGE);

                Path newExePath = Paths.get("EternalSky_new.exe");

                
                HttpRequest downloadReq = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
                HttpResponse<InputStream> downloadRes = client.send(downloadReq, HttpResponse.BodyHandlers.ofInputStream());
                Files.copy(downloadRes.body(), newExePath, StandardCopyOption.REPLACE_EXISTING);

                
                createAndRunUpdateScript();

                
                System.exit(0);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке обновления лаунчера: " + e.getMessage());
        }
    }

    private static void createAndRunUpdateScript() throws Exception {
        Path scriptPath = Paths.get("update.bat");

        String scriptContent = "@echo off\n" +
                "timeout /t 2 /nobreak > NUL\n" +
                "del /f /q \"EternalSky.exe\"\n" +
                "move /y \"EternalSky_new.exe\" \"EternalSky.exe\"\n" +
                "start \"\" \"EternalSky.exe\"\n" +
                "del \"%~f0\"";

        Files.writeString(scriptPath, scriptContent);

        new ProcessBuilder("cmd", "/c", "start", "/min", "update.bat").start();
    }
}