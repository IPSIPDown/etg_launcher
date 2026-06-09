package etg.ipsipdown.launcher.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties; // Добавили импорт для чтения настроек

public class DiscordNewsFetcher {

    // ID канала #новости
    private static final String CHANNEL_ID = "1513443351691657216";

    // Сколько последних новостей грузить
    private static final int POSTS_LIMIT = 5;

    // НОВЫЙ МЕТОД: Достаем токен из скрытого файла
    private static String getBotToken() {
        try (InputStream is = DiscordNewsFetcher.class.getResourceAsStream("/config.properties")) {
            if (is == null) return null;
            Properties props = new Properties();
            props.load(is);
            return props.getProperty("discord.token");
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> fetchLatestNews() {
        List<String> newsList = new ArrayList<>();

        // Получаем токен перед запросом
        String botToken = getBotToken();

        if (botToken == null || botToken.isEmpty()) {
            newsList.add("Ошибка: Токен Discord не найден в сборке.");
            return newsList;
        }

        try {
            String url = "https://discord.com/api/v10/channels/" + CHANNEL_ID + "/messages?limit=" + POSTS_LIMIT;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", botToken) // Подставляем токен из файла
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Парсим JSON ответ от Дискорда
                JsonArray messages = JsonParser.parseString(response.body()).getAsJsonArray();

                for (JsonElement element : messages) {
                    String content = element.getAsJsonObject().get("content").getAsString();
                    // Игнорируем пустые сообщения
                    if (!content.trim().isEmpty()) {
                        newsList.add(content);
                    }
                }
            } else {
                System.err.println("Ошибка парсинга Discord. Код: " + response.statusCode());
                System.err.println("Ответ: " + response.body());
                newsList.add("Не удалось загрузить новости. Сервер Discord недоступен.");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при получении новостей: " + e.getMessage());
            newsList.add("Ошибка загрузки новостей.");
        }

        return newsList;
    }
}