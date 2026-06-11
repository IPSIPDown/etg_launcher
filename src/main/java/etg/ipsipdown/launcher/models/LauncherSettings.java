package etg.ipsipdown.launcher.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class LauncherSettings {

    private static final Logger log = LoggerFactory.getLogger(LauncherSettings.class);

    // Настройки игры (дефолтные значения)
    public int ramMegabytes = 4096; // По умолчанию 4 ГБ
    public boolean autoConnect = false;
    public String serverIp = "eternal.playit.plus";
    public int serverPort = 25565;

    public int windowWidth = 1024;
    public int windowHeight = 768;
    public boolean isFullScreen = false;

    // Оптимальный набор флагов для G1GC
    public String jvmArgs = "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M";
    public String customJavaPath = "";

    private static final Path SETTINGS_FILE = OsPaths.GAME_DIR.resolve("settings.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Загрузить настройки из файла (если файла нет — создаст дефолтные)
    public static LauncherSettings load() {
        try {
            if (Files.exists(SETTINGS_FILE)) {
                String json = Files.readString(SETTINGS_FILE);
                LauncherSettings settings = GSON.fromJson(json, LauncherSettings.class);
                return settings != null ? settings : new LauncherSettings();
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить настройки, используем дефолт: {}", e.getMessage());
        }
        return new LauncherSettings();
    }

    // Сохранить текущие настройки в файл settings.json
    public void save() {
        try {
            if (!Files.exists(SETTINGS_FILE.getParent())) {
                Files.createDirectories(SETTINGS_FILE.getParent());
            }
            String json = GSON.toJson(this);
            Files.writeString(SETTINGS_FILE, json);
        } catch (Exception e) {
            log.error("Не удалось сохранить настройки", e);
        }
    }
}
