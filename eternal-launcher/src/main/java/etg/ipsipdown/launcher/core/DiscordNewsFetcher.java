package etg.ipsipdown.launcher.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class DiscordNewsFetcher {

    // Вставь сюда токен своего бота (обязательно с приставкой Bot и пробелом!)
    private static final String BOT_TOKEN = "Bot MTUxMzQ0NDA3NjUxNzcyNDE4MQ.G20Q7E.N6Y-EHQz-1dQeXlMwdraA_7C_82YyusZe-24P8";

    // ID канала #новости (включи режим разработчика в дискорде, ПКМ по каналу -> Копировать ID)
    private static final String CHANNEL_ID = "1513443351691657216";

    // Сколько последних новостей грузить
    private static final int POSTS_LIMIT = 5;

    public static List<String> fetchLatestNews() {
        List<String> newsList = new ArrayList<>();

        try {
            String url = "https://discord.com/api/v10/channels/" + CHANNEL_ID + "/messages?limit=" + POSTS_LIMIT;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", BOT_TOKEN)
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Парсим JSON ответ от Дискорда
                JsonArray messages = JsonParser.parseString(response.body()).getAsJsonArray();

                for (JsonElement element : messages) {
                    String content = element.getAsJsonObject().get("content").getAsString();
                    // Игнорируем пустые сообщения (например, если там только картинка без текста)
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