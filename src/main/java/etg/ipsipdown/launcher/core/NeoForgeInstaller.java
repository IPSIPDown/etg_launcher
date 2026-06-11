package etg.ipsipdown.launcher.core;

import etg.ipsipdown.launcher.ui.LauncherWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
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

public class NeoForgeInstaller {

    private static final Logger log = LoggerFactory.getLogger(NeoForgeInstaller.class);

    private final LauncherWindow window;
    private final Path minecraftDir;
    private final HttpClient httpClient;


    private static final String NEOFORGE_INSTALLER_URL = "https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.228/neoforge-21.1.228-installer.jar";

    public NeoForgeInstaller(LauncherWindow window) {
        this.window = window;
        this.minecraftDir = Paths.get(System.getenv("APPDATA"), ".minecraft");
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    }

    public void install() throws Exception {
        window.setStatus("Проверка ядра NeoForge...");
        window.setProgress(0);

        String versionName = "neoforge-21.1.228";
        Path versionDir = minecraftDir.resolve("versions").resolve(versionName);


        if (Files.exists(versionDir.resolve(versionName + ".json"))) {
            log.info("NeoForge уже установлен. Пропускаем.");
            return;
        }

        window.setStatus("Скачивание установщика NeoForge...");
        Path tempInstaller = Paths.get("neoforge-installer-temp.jar");


        downloadFile(NEOFORGE_INSTALLER_URL, tempInstaller);

        window.setStatus("Установка ядра NeoForge (это займет время)...");
        window.setProgress(50);



        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                tempInstaller.toAbsolutePath().toString(),
                "--installClient",
                minecraftDir.toAbsolutePath().toString()
        );


        pb.redirectErrorStream(true);
        Process process = pb.start();


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {

                log.info("[NeoForge Installer] {}", line);
            }
        }


        int exitCode = process.waitFor();


        Files.deleteIfExists(tempInstaller);

        if (exitCode != 0) {
            throw new Exception("Ошибка при установке NeoForge! Код завершения: " + exitCode);
        }

        window.setStatus("Ядро успешно установлено!");
        window.setProgress(100);
    }

    private void downloadFile(String url, Path target) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new Exception("Ошибка скачивания инсталлятора: HTTP " + response.statusCode());
        }
        Files.copy(response.body(), target, StandardCopyOption.REPLACE_EXISTING);
    }
}