package etg.ipsipdown.launcher.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import etg.ipsipdown.launcher.models.ModInfo;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Чтение информации о модах из .jar файлов.
 * Метаданные и иконки кэшируются в cache\modicons (ключ — размер+время файла),
 * поэтому повторное открытие экрана «Сборка» не распаковывает архивы заново.
 */
public class LocalModReader {

    private static final Logger log = LoggerFactory.getLogger(LocalModReader.class);

    private static final Path ICON_CACHE_DIR = OsPaths.CACHE_DIR.resolve("modicons");
    private static final Path INDEX_FILE = ICON_CACHE_DIR.resolve("index.json");
    private static final Gson GSON = new Gson();

    /** Запись кэша метаданных одного мода. */
    private static class CacheEntry {
        long size;
        long mtime;
        String displayName;
        String version;
        String side;
        String updateDate;
        boolean hasIcon;
    }

    public static List<ModInfo> getInstalledMods(Path modsDirectory) {
        List<ModInfo> mods = new ArrayList<>();
        if (!Files.exists(modsDirectory)) return mods;

        Map<String, CacheEntry> index = loadIndex();
        boolean[] dirty = {false};

        try {
            Files.list(modsDirectory).forEach(file -> {
                String name = file.getFileName().toString();
                if (name.endsWith(".jar") || name.endsWith(".jar.disabled")) {
                    mods.add(readWithCache(file, index, dirty));
                }
            });
        } catch (Exception e) {
            log.warn("Ошибка при чтении папки модов: {}", e.getMessage());
        }

        if (dirty[0]) saveIndex(index);
        return mods;
    }

    private static ModInfo readWithCache(Path file, Map<String, CacheEntry> index, boolean[] dirty) {
        String fileName = file.getFileName().toString();
        try {
            long size = Files.size(file);
            long mtime = Files.getLastModifiedTime(file).toMillis();

            CacheEntry cached = index.get(fileName);
            if (cached != null && cached.size == size && cached.mtime == mtime) {
                return fromCache(file, fileName, cached);
            }

            // Кэша нет или файл изменился — читаем jar и обновляем кэш
            ModInfo info = readModInfo(file);
            CacheEntry entry = new CacheEntry();
            entry.size = size;
            entry.mtime = mtime;
            entry.displayName = info.displayName;
            entry.version = info.version;
            entry.side = info.sideType;
            entry.updateDate = info.updateDate;
            entry.hasIcon = info.icon instanceof BufferedImage;
            if (entry.hasIcon) {
                try {
                    Files.createDirectories(ICON_CACHE_DIR);
                    ImageIO.write((BufferedImage) info.icon, "png", iconFile(fileName).toFile());
                } catch (Exception e) {
                    entry.hasIcon = false;
                }
            }
            index.put(fileName, entry);
            dirty[0] = true;
            return info;
        } catch (Exception e) {
            return readModInfo(file);
        }
    }

    private static ModInfo fromCache(Path file, String fileName, CacheEntry cached) {
        ModInfo info = new ModInfo();
        info.fileName = fileName;
        info.filePath = file;
        info.isEnabled = !fileName.endsWith(".disabled");
        info.displayName = cached.displayName;
        info.version = cached.version;
        info.sideType = cached.side;
        info.updateDate = cached.updateDate;
        if (cached.hasIcon) {
            try {
                info.icon = ImageIO.read(iconFile(fileName).toFile());
            } catch (Exception ignored) {
            }
        }
        return info;
    }

    private static Path iconFile(String modFileName) {
        return ICON_CACHE_DIR.resolve(modFileName.replace(".disabled", "") + ".png");
    }

    private static Map<String, CacheEntry> loadIndex() {
        try {
            if (Files.exists(INDEX_FILE)) {
                Type mapType = new TypeToken<HashMap<String, CacheEntry>>() {}.getType();
                Map<String, CacheEntry> index = GSON.fromJson(Files.readString(INDEX_FILE), mapType);
                if (index != null) return index;
            }
        } catch (Exception e) {
            log.warn("Кэш модов повреждён, пересоздаём: {}", e.getMessage());
        }
        return new HashMap<>();
    }

    private static void saveIndex(Map<String, CacheEntry> index) {
        try {
            Files.createDirectories(ICON_CACHE_DIR);
            Files.writeString(INDEX_FILE, GSON.toJson(index));
        } catch (Exception e) {
            log.warn("Не удалось сохранить кэш модов: {}", e.getMessage());
        }
    }

    private static ModInfo readModInfo(Path file) {
        ModInfo info = new ModInfo();
        info.fileName = file.getFileName().toString();
        info.filePath = file;

        // Если расширение .disabled — значит мод выключен
        info.isEnabled = !info.fileName.endsWith(".disabled");

        // Базовые значения
        info.displayName = info.fileName.replace(".jar", "").replace(".disabled", "");
        info.version = "Неизвестно";
        info.sideType = "Общий"; // По умолчанию

        try {
            // Достаем дату изменения файла
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
            info.updateDate = new SimpleDateFormat("dd.MM.yyyy").format(new Date(attr.lastModifiedTime().toMillis()));

            // Вскрываем .jar файл как ZIP
            try (ZipFile zip = new ZipFile(file.toFile())) {

                // Ищем файл манифеста
                ZipEntry tomlEntry = zip.getEntry("META-INF/mods.toml");
                if (tomlEntry == null) tomlEntry = zip.getEntry("META-INF/neoforge.mods.toml");

                String logoFileName = null;

                if (tomlEntry != null) {
                    try (InputStream is = zip.getInputStream(tomlEntry)) {
                        String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                        // 1. Имя
                        Matcher nameMatcher = Pattern.compile("displayName\\s*=\\s*\"([^\"]+)\"").matcher(content);
                        if (nameMatcher.find()) info.displayName = nameMatcher.group(1);

                        // 2. Версия
                        Matcher versionMatcher = Pattern.compile("version\\s*=\\s*\"([^\"]+)\"").matcher(content);
                        if (versionMatcher.find() && !versionMatcher.group(1).equals("${file.jarVersion}")) {
                            info.version = versionMatcher.group(1);
                        }

                        // 3. Тип стороны
                        Matcher sideMatcher = Pattern.compile("side\\s*=\\s*\"([^\"]+)\"").matcher(content);
                        if (sideMatcher.find()) {
                            String side = sideMatcher.group(1).toLowerCase();
                            if (side.contains("client")) info.sideType = "Клиентский";
                            else if (side.contains("server")) info.sideType = "Серверный";
                            else info.sideType = "Общий";
                        }

                        // 4. Путь к лого
                        Matcher logoMatcher = Pattern.compile("logoFile\\s*=\\s*\"([^\"]+)\"").matcher(content);
                        if (logoMatcher.find()) logoFileName = logoMatcher.group(1);
                    }
                }

                // Достаем иконку, если нашли путь
                if (logoFileName != null) {
                    ZipEntry logoEntry = zip.getEntry(logoFileName);
                    if (logoEntry != null) {
                        try (InputStream is = zip.getInputStream(logoEntry)) {
                            info.icon = ImageIO.read(is);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Если архив поврежден, оставляем базовые значения
        }

        return info;
    }
}
