package etg.ipsipdown.launcher.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Единственное место в проекте, которое знает, где лежат папки игры и лаунчера.
 */
public final class OsPaths {

    public static final Path MINECRAFT_DIR;
    public static final Path GAME_DIR;
    public static final Path MODS_DIR;
    public static final Path LOGS_DIR;
    public static final Path CACHE_DIR;
    public static final Path JRE_DIR;
    public static final Path CUSTOM_MODS_WHITELIST;

    static {
        if (isWindows()) {
            String appdata = System.getenv("APPDATA");
            MINECRAFT_DIR = Paths.get(appdata, ".minecraft");
            GAME_DIR = Paths.get(appdata, ".eternalsky");
        } else {
            String home = System.getProperty("user.home");
            MINECRAFT_DIR = Paths.get(home, ".minecraft");
            GAME_DIR = Paths.get(home, ".eternalsky");
        }
        MODS_DIR = GAME_DIR.resolve("mods");
        LOGS_DIR = GAME_DIR.resolve("logs");
        CACHE_DIR = GAME_DIR.resolve("cache");
        JRE_DIR = GAME_DIR.resolve("jre");
        CUSTOM_MODS_WHITELIST = GAME_DIR.resolve("custom_mods.txt");
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private OsPaths() {
    }
}
