package etg.ipsipdown.launcher.services;

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
 * Единая точка скачивания. HttpClient с таймаутом подключения,
 * чтобы лаунчер никогда не зависал намертво при проблемах с сетью.
 */
public class DownloadService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    /** Скачать текст (манифест, json и т.п.). Бросает исключение при любом сбое. */
    public String fetchString(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Сервер недоступен (Код: " + response.statusCode() + ")");
        }
        return response.body();
    }

    /** Скачать файл в target (создаёт родительские папки, перезаписывает существующий). */
    public void downloadFile(String rawUrl, Path target) throws Exception {
        if (target.getParent() != null) Files.createDirectories(target.getParent());

        String safeUrl = rawUrl.replace(" ", "%20").replace("[", "%5B").replace("]", "%5D");

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(safeUrl)).GET().build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) throw new Exception("HTTP " + response.statusCode() + " при скачивании " + rawUrl);
        try (InputStream body = response.body()) {
            Files.copy(body, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public HttpClient client() {
        return httpClient;
    }
}
