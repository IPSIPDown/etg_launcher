package etg.ipsipdown.launcher.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import etg.ipsipdown.launcher.utils.HashUtil;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Самообновление лаунчера (бывший LauncherUpdater).
 * 2.0: проверка в фоне (не блокирует старт), таймауты сети, обновление только
 * на более новую версию (semver), опциональная проверка SHA-256 скачанного файла.
 */
public class SelfUpdateService {

    private static final Logger log = LoggerFactory.getLogger(SelfUpdateService.class);

    public static final String CURRENT_VERSION = "2.1.0";

    private static final String VERSION_URL =
            "https://raw.githubusercontent.com/IPSIPDown/etg_launcher/main/src/main/resources/version-version.json";

    /** Запустить проверку в фоновом потоке — вызывается после показа окна. */
    public static void checkAsync() {
        Thread t = new Thread(SelfUpdateService::checkAndUpdate, "self-update");
        t.setDaemon(true);
        t.start();
    }

    static void checkAndUpdate() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(VERSION_URL))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Проверка обновлений: HTTP {}", response.statusCode());
                return;
            }

            JsonObject data = new Gson().fromJson(response.body(), JsonObject.class);
            String latestVersion = data.get("version").getAsString();

            // Выбираем URL для нашей платформы: url_linux или url (Windows/дефолт)
            String urlField = OsPaths.isWindows() ? "url" : "url_linux";
            if (!data.has(urlField) || data.get(urlField).getAsString().isBlank()) {
                urlField = "url";
            }
            String downloadUrl = data.get(urlField).getAsString();

            String expectedSha256Field = OsPaths.isWindows() ? "sha256" : "sha256_linux";
            String expectedSha256 = null;
            if (data.has(expectedSha256Field) && !data.get(expectedSha256Field).getAsString().isBlank()) {
                expectedSha256 = data.get(expectedSha256Field).getAsString();
            } else if (data.has("sha256") && !data.get("sha256").getAsString().isBlank()) {
                expectedSha256 = data.get("sha256").getAsString();
            }

            if (!isNewer(latestVersion, CURRENT_VERSION)) {
                log.info("Лаунчер актуален (текущая {}, на сервере {})", CURRENT_VERSION, latestVersion);
                return;
            }

            log.info("Найдено обновление лаунчера: {} -> {}", CURRENT_VERSION, latestVersion);
            JOptionPane.showMessageDialog(null,
                    "Найдено обновление лаунчера (" + latestVersion + ")! Сейчас он будет скачан и перезапущен.",
                    "Обновление EternalSky", JOptionPane.INFORMATION_MESSAGE);

            // 1. Узнаем точный путь и имя запущенного сейчас лаунчера
            File currentFile = new File(SelfUpdateService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            String currentFileName = currentFile.getName();
            File currentDir = currentFile.getParentFile();
            if (currentDir == null) currentDir = new File(".");

            boolean isWindows = OsPaths.isWindows();
            String defaultName = isWindows ? "EternalSky.exe" : "EternalSky.jar";
            if (!currentFileName.endsWith(".exe") && !currentFileName.endsWith(".jar")) {
                currentFileName = defaultName;
            }

            // 2. Скачиваем новый файл во временный
            String newFileName = isWindows ? "EternalSky_new.exe" : "EternalSky_new.jar";
            Path newFilePath = currentDir.toPath().resolve(newFileName);

            HttpRequest downloadReq = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
            HttpResponse<InputStream> downloadRes = client.send(downloadReq, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = downloadRes.body()) {
                Files.copy(body, newFilePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 3. Проверяем целостность, если в манифесте указан хэш
            if (expectedSha256 != null) {
                String actual = HashUtil.sha256(newFilePath);
                if (!actual.equalsIgnoreCase(expectedSha256)) {
                    Files.deleteIfExists(newFilePath);
                    log.error("SHA-256 скачанного обновления не совпал! Ожидалось {}, получено {}", expectedSha256, actual);
                    JOptionPane.showMessageDialog(null,
                            "Скачанное обновление повреждено и было удалено.\nПопробуй позже.",
                            "Обновление EternalSky", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                log.info("SHA-256 обновления подтверждён");
            } else {
                log.warn("В version-version.json нет sha256 — обновление не проверено по хэшу");
            }

            // 4. Создаем и запускаем скрипт замены, затем выходим
            createAndRunUpdateScript(currentFileName, newFileName, currentDir);
            System.exit(0);

        } catch (Exception e) {
            log.warn("Ошибка при проверке обновления лаунчера: {}", e.getMessage());
        }
    }

    /** true, если remote строго новее current (сравнение по числам: 1.2.10 > 1.2.9). */
    static boolean isNewer(String remote, String current) {
        try {
            String[] r = remote.trim().split("\\.");
            String[] c = current.trim().split("\\.");
            int len = Math.max(r.length, c.length);
            for (int i = 0; i < len; i++) {
                int rv = i < r.length ? Integer.parseInt(r[i].replaceAll("\\D", "")) : 0;
                int cv = i < c.length ? Integer.parseInt(c[i].replaceAll("\\D", "")) : 0;
                if (rv != cv) return rv > cv;
            }
            return false;
        } catch (Exception e) {
            log.warn("Не удалось сравнить версии \"{}\" и \"{}\"", remote, current);
            return false;
        }
    }

    private static void createAndRunUpdateScript(String currentFileName, String newFileName, File dir) throws Exception {
        if (OsPaths.isWindows()) {
            createAndRunBatScript(currentFileName, newFileName, dir);
        } else {
            createAndRunShScript(currentFileName, newFileName, dir);
        }
    }

    private static void createAndRunBatScript(String currentFileName, String newFileName, File dir) throws Exception {
        Path scriptPath = dir.toPath().resolve("update.bat");

        String scriptContent = "@echo off\n" +
                "cd /d \"" + dir.getAbsolutePath() + "\"\n" +
                "timeout /t 2 /nobreak > NUL\n" +
                "del /f /q \"" + currentFileName + "\"\n" +
                "move /y \"" + newFileName + "\" \"" + currentFileName + "\"\n" +
                "start \"\" \"" + currentFileName + "\"\n" +
                "del \"%~f0\"";

        Files.writeString(scriptPath, scriptContent);

        new ProcessBuilder("cmd", "/c", "start", "/min", scriptPath.toAbsolutePath().toString())
                .directory(dir)
                .start();
    }

    private static void createAndRunShScript(String currentFileName, String newFileName, File dir) throws Exception {
        Path scriptPath = dir.toPath().resolve("update.sh");

        String scriptContent = "#!/bin/sh\n" +
                "sleep 2\n" +
                "cd \"" + dir.getAbsolutePath() + "\"\n" +
                "rm -f \"" + currentFileName + "\"\n" +
                "mv \"" + newFileName + "\" \"" + currentFileName + "\"\n" +
                "java -jar \"" + currentFileName + "\" &\n" +
                "rm -- \"$0\"\n";

        Files.writeString(scriptPath, scriptContent);
        scriptPath.toFile().setExecutable(true);

        new ProcessBuilder("sh", scriptPath.toAbsolutePath().toString())
                .directory(dir)
                .start();
    }
}
