package etg.ipsipdown.launcher.core;

import etg.ipsipdown.launcher.ui.LauncherWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JreManager {

    private static final Logger log = LoggerFactory.getLogger(JreManager.class);

    private static final String JRE_DOWNLOAD_URL = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jre/hotspot/normal/eclipse";

    public static void installIfMissing(LauncherWindow window) throws Exception {
        Path jreDir = Paths.get("jre");
        Path javaExe = jreDir.resolve("bin").resolve("java.exe");


        if (Files.exists(javaExe)) {
            log.info("JRE уже установлена. Пропускаем скачивание.");
            return;
        }

        window.setStatus("Скачивание Java (JRE 17)... Это может занять пару минут.");
        window.setProgress(0);

        if (!Files.exists(jreDir)) {
            Files.createDirectories(jreDir);
        }

        Path zipFile = Paths.get("jre_temp.zip");


        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(JRE_DOWNLOAD_URL)).build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new Exception("Не удалось скачать Java. Код: " + response.statusCode());
        }

        Files.copy(response.body(), zipFile, StandardCopyOption.REPLACE_EXISTING);
        window.setStatus("Распаковка Java...");


        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();



                int firstSlash = name.indexOf('/');
                if (firstSlash == -1) continue;

                String strippedName = name.substring(firstSlash + 1);
                if (strippedName.isEmpty()) continue;

                Path targetPath = jreDir.resolve(strippedName);

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }


        Files.deleteIfExists(zipFile);
        window.setStatus("Java успешно установлена!");
    }
}