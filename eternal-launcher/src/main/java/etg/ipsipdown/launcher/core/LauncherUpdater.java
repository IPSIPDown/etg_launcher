package etg.ipsipdown.launcher.core;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LauncherUpdater {

    // Текущая версия лаунчера. Меняй её здесь перед тем, как собрать новый .exe!
    public static final String CURRENT_VERSION = "1.0.0";

    // Ссылка на JSON с информацией о последней версии
    private static final String VERSION_URL = "https://pub-97e8a596b9e44332a4b339c99ee9ef01.r2.dev/launcher-version.json";

    public static void checkAndUpdate() {
        try {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(VERSION_URL)).build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return; // Если сервер лежит, просто игнорируем и запускаем лаунчер как обычно

            JsonObject data = new Gson().fromJson(response.body(), JsonObject.class);
            String latestVersion = data.get("version").getAsString();
            String downloadUrl = data.get("url").getAsString();

            // Если версии не совпадают — начинаем обновление
            if (!CURRENT_VERSION.equals(latestVersion)) {

                // Показываем окошко, чтобы игрок не пугался, почему лаунчер завис
                JOptionPane.showMessageDialog(null,
                        "Найдено обновление лаунчера! Сейчас он будет обновлен и перезапущен.",
                        "Обновление EternalSky", JOptionPane.INFORMATION_MESSAGE);

                Path newExePath = Paths.get("EternalSky_new.exe");

                // 1. Скачиваем новый экзешник
                HttpRequest downloadReq = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
                HttpResponse<InputStream> downloadRes = client.send(downloadReq, HttpResponse.BodyHandlers.ofInputStream());
                Files.copy(downloadRes.body(), newExePath, StandardCopyOption.REPLACE_EXISTING);

                // 2. Создаем батник-ниндзя для подмены файлов
                createAndRunUpdateScript();

                // 3. Убиваем текущий лаунчер, чтобы батник мог его удалить
                System.exit(0);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при проверке обновления лаунчера: " + e.getMessage());
            // Если произошла ошибка (нет инета), ничего не делаем, лаунчер запустится дальше
        }
    }

    private static void createAndRunUpdateScript() throws Exception {
        Path scriptPath = Paths.get("update.bat");

        // Пишем команды в батник:
        // timeout /t 2 -> ждем 2 секунды (пока старый exe закроется)
        // del -> удаляем старый exe
        // move -> переименовываем новый exe в старый
        // start -> запускаем обновленный лаунчер
        // del "%~f0" -> батник удаляет сам себя
        String scriptContent = "@echo off\n" +
                "timeout /t 2 /nobreak > NUL\n" +
                "del /f /q \"EternalSky.exe\"\n" +
                "move /y \"EternalSky_new.exe\" \"EternalSky.exe\"\n" +
                "start \"\" \"EternalSky.exe\"\n" +
                "del \"%~f0\"";

        Files.writeString(scriptPath, scriptContent);

        // Запускаем этот батник невидимо через VBS или просто как процесс
        new ProcessBuilder("cmd", "/c", "start", "/min", "update.bat").start();
    }
}