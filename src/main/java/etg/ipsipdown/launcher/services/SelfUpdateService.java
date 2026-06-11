package etg.ipsipdown.launcher.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import etg.ipsipdown.launcher.utils.HashUtil;
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
 * на более новую версию (semver), опциональная проверка SHA-256 скачанного exe.
 */
public class SelfUpdateService {

    private static final Logger log = LoggerFactory.getLogger(SelfUpdateService.class);

    public static final String CURRENT_VERSION = "2.0.0";

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
            String downloadUrl = data.get("url").getAsString();
            String expectedSha256 = data.has("sha256") && !data.get("sha256").getAsString().isBlank()
                    ? data.get("sha256").getAsString() : null;

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

            // На всякий случай (если запускают напрямую через IDE)
            if (!currentFileName.endsWith(".exe") && !currentFileName.endsWith(".jar")) {
                currentFileName = "EternalSky.exe";
            }

            // 2. Скачиваем новый файл во временный
            Path newExePath = currentDir.toPath().resolve("EternalSky_new.exe");

            HttpRequest downloadReq = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).build();
            HttpResponse<InputStream> downloadRes = client.send(downloadReq, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = downloadRes.body()) {
                Files.copy(body, newExePath, StandardCopyOption.REPLACE_EXISTING);
            }

            // 3. Проверяем целостность, если в манифесте указан хэш
            if (expectedSha256 != null) {
                String actual = HashUtil.sha256(newExePath);
                if (!actual.equalsIgnoreCase(expectedSha256)) {
                    Files.deleteIfExists(newExePath);
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
            createAndRunUpdateScript(currentFileName, currentDir);
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
            // Если формат версии нечитаемый — лучше не обновляться
            log.warn("Не удалось сравнить версии \"{}\" и \"{}\"", remote, current);
            return false;
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

        new ProcessBuilder("cmd", "/c", "start", "/min", scriptPath.toAbsolutePath().toString())
                .directory(dir)
                .start();
    }
}
