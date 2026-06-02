package etg.ipsipdown.launcher.net;

import com.google.gson.Gson;
import etg.ipsipdown.launcher.ui.LauncherWindow;
import etg.ipsipdown.launcher.core.ProfileInjector;

import java.io.IOException;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.List;

public class UpdateManager {

    // 1. Разделяем базовый URL и путь к манифесту
    // Если Cloudflare будет ругаться на лимиты .r2.dev, можно поменять BASE_URL на "https://dl.yummycalyx.com"
    private static final String BASE_URL = "https://pub-97e8a596b9e44332a4b339c99ee9ef01.r2.dev";
    private static final String MANIFEST_URL = BASE_URL + "/manifest.json";

    private final Path gameDir;       // Для модов (.eternalsky)
    private final Path minecraftDir;  // Для Forge (.minecraft)
    private final LauncherWindow window;
    private final HttpClient httpClient;
    private final Gson gson;

    public UpdateManager(LauncherWindow window) {
        this.window = window;
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        this.gson = new Gson();

        // Определяем путь к AppData/.eternalsky
        String appData = System.getenv("APPDATA");
        this.gameDir = Paths.get(appData, ".eternalsky");
        this.minecraftDir = Paths.get(appData, ".minecraft"); // ДОБАВИТЬ ЭТО
    }

    public void startUpdate() {
        new Thread(() -> {
            try {
                window.setStatus("Получение списка файлов...");
                window.setProgress(0);

                // Создаем корневую папку, если её нет
                if (!Files.exists(gameDir)) {
                    Files.createDirectories(gameDir);
                }

                // Запрос идет точно по адресу manifest.json
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(MANIFEST_URL)).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Сервер недоступен (Код: " + response.statusCode() + ")");
                }

                Manifest manifest = gson.fromJson(response.body(), Manifest.class);
                List<Manifest.ManifestFile> files = manifest.files;

                if (files == null || files.isEmpty()) {
                    window.setStatus("Сборка актуальна!");
                    finishUpdate();
                    return;
                }

                int totalFiles = files.size();
                int downloaded = 0;

                for (Manifest.ManifestFile fileInfo : files) {
                    Path targetPath;

                    // Если это ядро Forge/NeoForge или библиотеки — кидаем в оригинальный .minecraft
                    if (fileInfo.path.startsWith("versions") || fileInfo.path.startsWith("libraries")) {
                        targetPath = minecraftDir.resolve(fileInfo.path);
                    } else {
                        // Моды, конфиги, ресурспаки кидаем в изолированную папку сборки
                        targetPath = gameDir.resolve(fileInfo.path);
                    }

                    downloaded++;
                    int progress = (int) (((double) downloaded / totalFiles) * 100);
                    window.setProgress(progress);
                }

                window.setStatus("Все файлы успешно обновлены!");
                finishUpdate();

            } catch (Exception e) {
                e.printStackTrace();
                window.setStatus("Ошибка обновления: " + e.getMessage());
                window.setButtonEnabled(true);
            }
        }).start();
    }

    // --- Вспомогательные методы ---

    private boolean requiresDownload(Path file, String expectedHash) {
        if (!Files.exists(file)) return true;

        try {
            // Используем SHA-256 под манифест Cloudflare
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Читаем файл потоком (чанками по 8 КБ)
            try (InputStream is = Files.newInputStream(file);
                 DigestInputStream dis = new DigestInputStream(is, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {
                    // Пустой цикл: файл прогоняется через MessageDigest автоматически
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String localHash = sb.toString();

            return !localHash.equalsIgnoreCase(expectedHash);
        } catch (Exception e) {
            return true;
        }
    }

    private void downloadFile(String rawUrl, Path target) throws Exception {
        if (target.getParent() != null) {
            Files.createDirectories(target.getParent());
        }

        // Экранируем пробелы и скобки, чтобы Cloudflare не выдавал ошибку 400/404
        String safeUrl = rawUrl
                .replace(" ", "%20")
                .replace("[", "%5B")
                .replace("]", "%5D");

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(safeUrl)).build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        Files.copy(response.body(), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void finishUpdate() {
        try {
            Thread.sleep(500);
            window.setStatus("Интеграция профиля...");

            // Вызываем нашу магию
            etg.ipsipdown.launcher.core.ProfileInjector.inject();

            window.setStatus("Запуск Minecraft!");
            window.setProgress(100);

            // Автоматически открываем официальный лаунчер
            launchOfficialMinecraft();

            // Закрываем наш патчер через 2 секунды
            Thread.sleep(2000);
            System.exit(0);

        } catch (InterruptedException e) { // <-- ИСПРАВЛЕНИЕ ЗДЕСЬ (Убрали IOException)
            e.printStackTrace();
            window.setButtonEnabled(true);
        }
    }

    private void launchOfficialMinecraft() {
        String appData = System.getenv("APPDATA");
        String programFilesX86 = System.getenv("ProgramFiles(x86)");
        String programFiles = System.getenv("ProgramFiles");

        // Список всех дефолтных путей установки официального лаунчера
        Path[] possiblePaths = {
                Paths.get(programFilesX86, "Minecraft Launcher", "MinecraftLauncher.exe"),
                Paths.get(programFiles, "Minecraft Launcher", "MinecraftLauncher.exe"),
                Paths.get(appData, ".minecraft", "MinecraftLauncher.exe")
        };

        boolean launched = false;

        for (Path path : possiblePaths) {
            if (path != null && Files.exists(path)) {
                try {
                    new ProcessBuilder(path.toString()).start();
                    launched = true;
                    System.out.println("Лаунчер найден и запущен: " + path.toString());
                    break;
                } catch (Exception e) {
                    System.err.println("Не удалось запустить: " + path.toString());
                }
            }
        }

        if (!launched) {
            // Если лаунчера нигде нет (или используется Prism/CurseForge),
            // не крашим систему, а просто пишем красивый статус
            window.setStatus("Готово! Теперь открой свой лаунчер.");

            // Оставляем окно открытым чуть дольше, чтобы игрок успел прочитать
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
        }
    }
}