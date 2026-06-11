package etg.ipsipdown.launcher.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import etg.ipsipdown.launcher.models.NewsItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Загружает новости из файла news.json в корне репозитория на GitHub —
 * чтобы опубликовать новость, достаточно отредактировать этот файл в ветке main.
 * Ожидаемый формат файла:
 * [
 *   { "title": "Заголовок", "date": "2026-06-11", "text": "Текст новости" }
 * ]
 * Поля title и date необязательные — достаточно text.
 * Последний удачный ответ кэшируется и показывается, если сети нет.
 */
public class NewsService {

    private static final Logger log = LoggerFactory.getLogger(NewsService.class);

    private static final String NEWS_URL = "https://raw.githubusercontent.com/IPSIPDown/etg_launcher/refs/heads/v2/news.json";
    private static final String CACHE_KEY = "news/news.json";

    // Сколько последних новостей показывать
    private static final int POSTS_LIMIT = 5;

    public static List<NewsItem> fetchLatestNews() {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NEWS_URL))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Не удалось загрузить новости. Код: {}", response.statusCode());
                return parse(CacheService.getText(CACHE_KEY));
            }

            CacheService.putText(CACHE_KEY, response.body());
            return parse(response.body());
        } catch (Exception e) {
            log.warn("Ошибка при получении новостей: {}", e.getMessage());
            return parse(CacheService.getText(CACHE_KEY));
        }
    }

    private static List<NewsItem> parse(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            Type listType = new TypeToken<List<NewsItem>>() {}.getType();
            List<NewsItem> news = new Gson().fromJson(json, listType);
            if (news == null) return List.of();

            List<NewsItem> result = new ArrayList<>();
            for (NewsItem item : news) {
                if (item != null && item.text != null && !item.text.isBlank()) {
                    result.add(item);
                    if (result.size() >= POSTS_LIMIT) break;
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("Не удалось разобрать news.json: {}", e.getMessage());
            return List.of();
        }
    }
}
