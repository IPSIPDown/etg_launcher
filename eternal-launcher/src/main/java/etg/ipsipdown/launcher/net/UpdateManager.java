package etg.ipsipdown.launcher.net;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import etg.ipsipdown.launcher.ui.LauncherWindow;
import etg.ipsipdown.launcher.core.ProfileInjector;

import java.io.InputStream;
import java.lang.reflect.Type;
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

    private static final String BASE_URL = "https://pub-97e8a596b9e44332a4b339c99ee9ef01.r2.dev";
    private static final String MANIFEST_URL = BASE_URL + "/manifest.json";

    private final Path gameDir;
    private final Path minecraftDir;
    private final LauncherWindow window;
    private final HttpClient httpClient;
    private final Gson gson;

    public UpdateManager(LauncherWindow window) {
        this.window = window;
        this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
        this.gson = new Gson();

        String appData = System.getenv("APPDATA");
        this.gameDir = Paths.get(appData, ".eternalsky");
        this.minecraftDir = Paths.get(appData, ".minecraft");
    }

    public void startUpdate() {
        new Thread(() -> {
            try {
                window.setStatus("Получение списка файлов...");
                window.setProgress(0);

                if (!Files.exists(gameDir)) Files.createDirectories(gameDir);

                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(MANIFEST_URL)).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    throw new RuntimeException("Сервер недоступен (Код: " + response.statusCode() + ")");
                }

                String jsonBody = response.body().trim();
                List<Manifest.ManifestFile> files;

                if (jsonBody.startsWith("[")) {
                    Type listType = new TypeToken<List<Manifest.ManifestFile>>(){}.getType();
                    files = gson.fromJson(jsonBody, listType);
                } else {
                    Manifest manifest = gson.fromJson(jsonBody, Manifest.class);
                    files = manifest != null ? manifest.files : null;
                }

                if (files == null || files.isEmpty()) {
                    window.setStatus("Ошибка: Лаунчер увидел 0 файлов!");
                    Thread.sleep(3000);
                    finishUpdate();
                    return;
                }

                int totalFiles = files.size();
                window.setStatus("Успешно прочитано: " + totalFiles + " файлов");
                Thread.sleep(1000);

                int downloaded = 0;
                for (Manifest.ManifestFile fileInfo : files) {
                    String cleanPath = fileInfo.path.replace("\\", "/");
                    if (cleanPath.startsWith("/")) cleanPath = cleanPath.substring(1);

                    Path targetPath = (cleanPath.startsWith("versions") || cleanPath.startsWith("libraries"))
                            ? minecraftDir.resolve(cleanPath)
                            : gameDir.resolve(cleanPath);

                    if (requiresDownload(targetPath, fileInfo.hash)) {
                        window.setStatus("Скачивание: " + targetPath.getFileName());
                        String fileDownloadUrl = BASE_URL + "/" + cleanPath;
                        downloadFile(fileDownloadUrl, targetPath);
                    }

                    downloaded++;
                    window.setProgress((int) (((double) downloaded / totalFiles) * 100));
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

    private boolean requiresDownload(Path file, String expectedHash) {
        if (!Files.exists(file)) return true;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file);
                 DigestInputStream dis = new DigestInputStream(is, digest)) {
                byte[] buffer = new byte[8192];
                while (dis.read(buffer) != -1) {}
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) sb.append(String.format("%02x", b));
            return !sb.toString().equalsIgnoreCase(expectedHash);
        } catch (Exception e) { return true; }
    }

    private void downloadFile(String rawUrl, Path target) throws Exception {
        if (target.getParent() != null) Files.createDirectories(target.getParent());

        String safeUrl = rawUrl.replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(safeUrl)).build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) throw new Exception("HTTP " + response.statusCode());
        Files.copy(response.body(), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void finishUpdate() {
        try {
            window.setStatus("Интеграция профиля...");
            ProfileInjector.inject();
            window.setStatus("Запуск Minecraft!");
            window.setProgress(100);
            launchOfficialMinecraft();
            Thread.sleep(2000);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
            window.setButtonEnabled(true);
        }
    }

    private void launchOfficialMinecraft() {
        String appData = System.getenv("APPDATA");
        String p86 = System.getenv("ProgramFiles(x86)");
        String pf = System.getenv("ProgramFiles");

        Path[] possiblePaths = {
                Paths.get(p86, "Minecraft Launcher", "MinecraftLauncher.exe"),
                Paths.get(pf, "Minecraft Launcher", "MinecraftLauncher.exe"),
                Paths.get(appData, ".minecraft", "MinecraftLauncher.exe")
        };

        for (Path path : possiblePaths) {
            if (Files.exists(path)) {
                try { new ProcessBuilder(path.toString()).start(); return; } catch (Exception ignored) {}
            }
        }
        window.setStatus("Готово! Открой лаунчер вручную.");
    }
}