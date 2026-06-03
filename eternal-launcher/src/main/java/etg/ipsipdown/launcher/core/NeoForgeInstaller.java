package etg.ipsipdown.launcher.core;

import etg.ipsipdown.launcher.ui.LauncherWindow;

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

    private final LauncherWindow window;
    private final Path minecraftDir;
    private final HttpClient httpClient;

    // Прямая ссылка на официальный установщик NeoForge нужной тебе версии
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

        // 1. Проверяем, установлено ли ядро. Если json уже есть, значит всё готово!
        if (Files.exists(versionDir.resolve(versionName + ".json"))) {
            System.out.println("NeoForge уже установлен. Пропускаем.");
            return;
        }

        window.setStatus("Скачивание установщика NeoForge...");
        Path tempInstaller = Paths.get("neoforge-installer-temp.jar");

        // 2. Скачиваем официальный инсталлятор
        downloadFile(NEOFORGE_INSTALLER_URL, tempInstaller);

        window.setStatus("Установка ядра NeoForge (это займет время)...");
        window.setProgress(50);

        // 3. Запускаем инсталлятор в скрытом режиме (фоном)
        // Команда: java -jar neoforge-installer-temp.jar --installClient "C:\Users\...\AppData\Roaming\.minecraft"
        ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                tempInstaller.toAbsolutePath().toString(),
                "--installClient",
                minecraftDir.toAbsolutePath().toString()
        );

        // Объединяем вывод ошибок и стандартный вывод
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // 4. ОЧЕНЬ ВАЖНО: Читаем вывод консоли инсталлятора, иначе он зависнет!
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Выводим в нашу консоль IDE, чтобы ты видел, что происходит под капотом
                System.out.println("[NeoForge Installer] " + line);
            }
        }

        // 5. Ждем завершения установки
        int exitCode = process.waitFor();

        // 6. Удаляем временный файл инсталлятора
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