package etg.ipsipdown.launcher.services;

import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Простой локальный кэш в %APPDATA%\.eternalsky\cache.
 * Позволяет показывать новости и медиа мгновенно и работать без сети.
 */
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    /** Сохранить текст в кэш (например, news/news.json). */
    public static void putText(String relativePath, String content) {
        try {
            Path target = OsPaths.CACHE_DIR.resolve(relativePath);
            Files.createDirectories(target.getParent());
            Files.writeString(target, content);
        } catch (Exception e) {
            log.warn("Не удалось записать кэш {}: {}", relativePath, e.getMessage());
        }
    }

    /** Прочитать текст из кэша; null, если кэша нет. */
    public static String getText(String relativePath) {
        try {
            Path source = OsPaths.CACHE_DIR.resolve(relativePath);
            if (Files.exists(source)) {
                return Files.readString(source);
            }
        } catch (Exception e) {
            log.warn("Не удалось прочитать кэш {}: {}", relativePath, e.getMessage());
        }
        return null;
    }
}
