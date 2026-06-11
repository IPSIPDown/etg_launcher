package etg.ipsipdown.launcher.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Кнопка «Поддержать автора»: отправляет сообщение в Discord-канал через webhook.
 * Пока WEBHOOK_URL пустой — работает как заглушка (просто благодарит локально).
 *
 * Как включить: в Discord на нужном канале -> Настройки канала -> Интеграции ->
 * Вебхуки -> Создать вебхук -> скопировать URL и вставить сюда.
 *
 * Анти-спам: локальный кулдаун на установку лаунчера (12 часов между нажатиями).
 */
public class SupportService {

    private static final Logger log = LoggerFactory.getLogger(SupportService.class);

    /** URL Discord-вебхука. Пусто = режим заглушки. */
    private static final String WEBHOOK_URL = ""; // TODO: вставь URL вебхука канала

    private static final String MESSAGE = "Кто-то решил поддержать автора лаунчера. Спасибо! ❤";
    private static final long COOLDOWN_MS = 12L * 60 * 60 * 1000; // 12 часов
    private static final String CACHE_KEY = "support/last_click.txt";

    /** Обрабатывает нажатие; возвращает текст для toast-уведомления. */
    public static String click() {
        // Кулдаун
        try {
            String last = CacheService.getText(CACHE_KEY);
            if (last != null) {
                long lastMs = Long.parseLong(last.trim());
                long left = COOLDOWN_MS - (System.currentTimeMillis() - lastMs);
                if (left > 0) {
                    long hours = Math.max(1, left / (60 * 60 * 1000));
                    return "Ты уже поддерживал недавно — спасибо! Снова можно через ~" + hours + " ч.";
                }
            }
        } catch (Exception ignored) {
        }
        CacheService.putText(CACHE_KEY, String.valueOf(System.currentTimeMillis()));

        if (WEBHOOK_URL.isBlank()) {
            log.info("Кнопка поддержки нажата (webhook не настроен — заглушка)");
            return "Спасибо за поддержку! ❤ (кнопка пока в тестовом режиме)";
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            String json = "{\"content\": \"" + MESSAGE + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(WEBHOOK_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Сообщение поддержки отправлено в Discord");
                return "Спасибо за поддержку! ❤ Сообщение отправлено.";
            }
            log.warn("Webhook ответил кодом {}", response.statusCode());
        } catch (Exception e) {
            log.warn("Не удалось отправить сообщение поддержки: {}", e.getMessage());
        }
        return "Спасибо за поддержку! ❤";
    }
}
