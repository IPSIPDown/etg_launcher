package etg.ipsipdown.launcher.services;

import etg.ipsipdown.launcher.models.ModInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LocalModReader {

    private static final Logger log = LoggerFactory.getLogger(LocalModReader.class);

    public static List<ModInfo> getInstalledMods(Path modsDirectory) {
        List<ModInfo> mods = new ArrayList<>();
        if (!Files.exists(modsDirectory)) return mods;

        try {
            Files.list(modsDirectory).forEach(file -> {
                String name = file.getFileName().toString();
                // Ищем только файлы модов (и выключенные моды)
                if (name.endsWith(".jar") || name.endsWith(".jar.disabled")) {
                    mods.add(readModInfo(file));
                }
            });
        } catch (Exception e) {
            log.warn("Ошибка при чтении папки модов: {}", e.getMessage());
        }
        return mods;
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
