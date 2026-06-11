package etg.ipsipdown.launcher.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Единственное место в проекте, которое знает, где лежат папки игры и лаунчера.
 */
public final class OsPaths {

    public static final Path MINECRAFT_DIR = Paths.get(System.getenv("APPDATA"), ".minecraft");
    public static final Path GAME_DIR = Paths.get(System.getenv("APPDATA"), ".eternalsky");
    public static final Path MODS_DIR = GAME_DIR.resolve("mods");
    public static final Path LOGS_DIR = GAME_DIR.resolve("logs");
    public static final Path CACHE_DIR = GAME_DIR.resolve("cache");
    public static final Path JRE_DIR = GAME_DIR.resolve("jre");
    public static final Path CUSTOM_MODS_WHITELIST = GAME_DIR.resolve("custom_mods.txt");

    private OsPaths() {
    }
}
