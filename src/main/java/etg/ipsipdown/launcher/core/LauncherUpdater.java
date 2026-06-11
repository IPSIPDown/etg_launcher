package etg.ipsipdown.launcher.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class LauncherUpdater {

    private static final Logger log = LoggerFactory.getLogger(LauncherUpdater.class);

    public static final String CURRENT_VERSION = "2.0.0";

    // ВНИМАНИЕ: Если ты закинул launcher-version.json на гитхаб,
    // замени эту ссылку на "Сырую" (Raw) ссылку с гитхаба!
    private static final String VERSION_URL = "https://raw.githubusercontent.com/IPSIPDown/etg_launcher/main/src/main/resources/version-version.json";

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
                        "Найдено обновление лаунчера! Сейчас он будет скачан и перезапущен.",
                        "Обновление EternalSky", JOptionPane.INFORMATION_MESSAGE);

                // 1. Узнаем точный путь и имя запущенного сейчас лаунчера
                File currentFile = new File(LauncherUpdater.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                String currentFileName = currentFile.getName();
                File currentDir = currentFile.getParentFile();
                if (currentDir == null) currentDir = new File(".");

                // На всякий случай (если запускают напрямую через IDEA)
                if (!currentFileName.endsWith(".exe") && !currentFileName.endsWith(".jar")) {
                    currentFileName = "EternalSky.exe";
                }

                // 2. Скачиваем новый файл во временный
                Path newExePath = currentDir.toPath().resolve("EternalSky_new.exe");

                HttpRequest downloadReq = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
                HttpResponse<InputStream> downloadRes = client.send(downloadReq, HttpResponse.BodyHandlers.ofInputStream());
                Files.copy(downloadRes.body(), newExePath, StandardCopyOption.REPLACE_EXISTING);

                // 3. Создаем и запускаем умный скрипт
                createAndRunUpdateScript(currentFileName, currentDir);

                // 4. Обязательно убиваем текущий процесс Java, иначе батник не сможет удалить файл
                System.exit(0);
            }
        } catch (Exception e) {
            log.warn("Ошибка при проверке обновления лаунчера: {}", e.getMessage());
        }
    }

    private static void createAndRunUpdateScript(String currentFileName, File dir) throws Exception {
        Path scriptPath = dir.toPath().resolve("update.bat");

        // Батник: переходит в папку лаунчера -> ждет закрытия -> удаляет старый -> переименовывает новый -> запускает
        String scriptContent = "@echo off\n" +
                "cd /d \"" + dir.getAbsolutePath() + "\"\n" +
                "timeout /t 2 /nobreak > NUL\n" +
                "del /f /q \"" + currentFileName + "\"\n" +
                "move /y \"EternalSky_new.exe\" \"" + currentFileName + "\"\n" +
                "start \"\" \"" + currentFileName + "\"\n" +
                "del \"%~f0\"";

        Files.writeString(scriptPath, scriptContent);

        // Запускаем скрытый батник, принудительно указав рабочую директорию
        new ProcessBuilder("cmd", "/c", "start", "/min", scriptPath.toAbsolutePath().toString())
                .directory(dir)
                .start();
    }
}