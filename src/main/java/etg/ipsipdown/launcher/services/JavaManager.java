package etg.ipsipdown.launcher.services;

import etg.ipsipdown.launcher.events.ProgressListener;
import etg.ipsipdown.launcher.models.LauncherSettings;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Поиск и установка Java для запуска инсталлятора NeoForge.
 * Порядок: путь из настроек -> своя JRE в .eternalsky\jre -> системная java -> скачать Temurin 21.
 */
public class JavaManager {

    private static final Logger log = LoggerFactory.getLogger(JavaManager.class);

    // NeoForge для MC 1.21 требует Java 21
    private static final String JRE_DOWNLOAD_URL =
            "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jre/hotspot/normal/eclipse";

    /** Возвращает команду java (полный путь или просто "java"), при необходимости скачивая JRE. */
    public static String findOrInstallJava(ProgressListener progress) throws Exception {
        // 1. Пользователь указал свой путь в настройках
        LauncherSettings settings = LauncherSettings.load();
        if (settings.customJavaPath != null && !settings.customJavaPath.isBlank()) {
            Path custom = Path.of(settings.customJavaPath);
            if (Files.isDirectory(custom)) custom = custom.resolve("bin").resolve("java.exe");
            if (Files.exists(custom)) {
                log.info("Используем Java из настроек: {}", custom);
                return custom.toString();
            }
            log.warn("Путь к Java из настроек не существует: {}", settings.customJavaPath);
        }

        // 2. Своя JRE, скачанная лаунчером ранее
        Path bundledJava = OsPaths.JRE_DIR.resolve("bin").resolve("java.exe");
        if (Files.exists(bundledJava)) {
            log.info("Используем JRE лаунчера: {}", bundledJava);
            return bundledJava.toString();
        }

        // 3. Системная Java подходящей версии (17+ достаточно для запуска инсталлятора)
        int systemMajor = detectSystemJavaMajor();
        if (systemMajor >= 17) {
            log.info("Используем системную Java {}", systemMajor);
            return "java";
        }

        // 4. Скачиваем Temurin 21 JRE
        log.info("Подходящая Java не найдена (системная: {}), скачиваем Temurin 21", systemMajor);
        installJre(progress);
        return bundledJava.toString();
    }

    /** Определяет мажорную версию системной java; -1, если java нет. */
    private static int detectSystemJavaMajor() {
        try {
            Process process = new ProcessBuilder("java", "-version")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor(10, TimeUnit.SECONDS);

            // строка вида: java version "21.0.1" / openjdk version "17.0.9"
            java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("version \"(\\d+)(?:\\.(\\d+))?")
                    .matcher(output);
            if (m.find()) {
                int major = Integer.parseInt(m.group(1));
                // старый формат 1.8 -> 8
                if (major == 1 && m.group(2) != null) major = Integer.parseInt(m.group(2));
                return major;
            }
        } catch (Exception e) {
            log.info("Системная Java не найдена: {}", e.getMessage());
        }
        return -1;
    }

    private static void installJre(ProgressListener progress) throws Exception {
        progress.onStatus("Скачивание Java 21... Это может занять пару минут.");
        progress.onProgress(0);

        Files.createDirectories(OsPaths.JRE_DIR);
        Path zipFile = OsPaths.GAME_DIR.resolve("jre_temp.zip");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(JRE_DOWNLOAD_URL)).build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new Exception("Не удалось скачать Java. Код: " + response.statusCode());
        }

        try (InputStream body = response.body()) {
            Files.copy(body, zipFile, StandardCopyOption.REPLACE_EXISTING);
        }
        progress.onStatus("Распаковка Java...");

        // Архив Temurin содержит корневую папку jdk-21.x — срезаем её
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                int firstSlash = name.indexOf('/');
                if (firstSlash == -1) continue;

                String strippedName = name.substring(firstSlash + 1);
                if (strippedName.isEmpty()) continue;

                Path targetPath = OsPaths.JRE_DIR.resolve(strippedName);
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Files.deleteIfExists(zipFile);
        progress.onStatus("Java успешно установлена!");
        log.info("Temurin 21 JRE установлена в {}", OsPaths.JRE_DIR);
    }
}
