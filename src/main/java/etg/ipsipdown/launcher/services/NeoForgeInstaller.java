package etg.ipsipdown.launcher.services;

import etg.ipsipdown.launcher.events.ProgressListener;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class NeoForgeInstaller {

    private static final Logger log = LoggerFactory.getLogger(NeoForgeInstaller.class);

    public static final String NEOFORGE_VERSION = "21.1.228";
    public static final String VERSION_NAME = "neoforge-" + NEOFORGE_VERSION;

    private static final String NEOFORGE_INSTALLER_URL =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/" + NEOFORGE_VERSION
                    + "/neoforge-" + NEOFORGE_VERSION + "-installer.jar";

    private final ProgressListener progress;
    private final DownloadService downloader;

    public NeoForgeInstaller(ProgressListener progress, DownloadService downloader) {
        this.progress = progress;
        this.downloader = downloader;
    }

    public void install() throws Exception {
        progress.onStatus("Проверка ядра NeoForge...");
        progress.onProgress(0);

        Path versionDir = OsPaths.MINECRAFT_DIR.resolve("versions").resolve(VERSION_NAME);
        if (Files.exists(versionDir.resolve(VERSION_NAME + ".json"))) {
            log.info("NeoForge уже установлен. Пропускаем.");
            return;
        }

        // Инсталлятору нужна Java — найдём или скачаем
        String javaCommand = JavaManager.findOrInstallJava(progress);

        progress.onStatus("Скачивание установщика NeoForge...");
        Path tempInstaller = OsPaths.GAME_DIR.resolve("neoforge-installer-temp.jar");
        downloader.downloadFile(NEOFORGE_INSTALLER_URL, tempInstaller);

        progress.onStatus("Установка ядра NeoForge (это займет время)...");
        progress.onProgress(50);

        ProcessBuilder pb = new ProcessBuilder(
                javaCommand,
                "-jar",
                tempInstaller.toAbsolutePath().toString(),
                "--installClient",
                OsPaths.MINECRAFT_DIR.toAbsolutePath().toString()
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

        progress.onStatus("Ядро успешно установлено!");
        progress.onProgress(100);
    }
}
