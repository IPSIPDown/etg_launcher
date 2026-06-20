package etg.ipsipdown.launcher.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ищет установленные сторонние лаунчеры Minecraft.
 * Проверяет бинарники в стандартных PATH-директориях и Flatpak.
 */
public class ThirdPartyLauncherDetector {

    private static final Logger log = LoggerFactory.getLogger(ThirdPartyLauncherDetector.class);

    public record LauncherInfo(String name, String[] command) {}

    // {Отображаемое имя, имя бинарника, Flatpak App ID или null}
    private static final String[][] KNOWN_LAUNCHERS = {
            {"PrismLauncher", "prismlauncher", "org.prismlauncher.PrismLauncher"},
            {"MultiMC",       "multimc",        "org.multimc.MultiMC"},
            {"Modrinth App",  "modrinth",       "com.modrinth.ModrinthApp"},
            {"ATLauncher",    "ATLauncher",      null},
            {"FTB App",       "ftb-app",         null},
            {"GDLauncher",    "gdlauncher-carbon", null},
    };

    private static final String[] BINARY_DIRS = {
            "/usr/bin",
            "/usr/local/bin",
            System.getProperty("user.home") + "/.local/bin",
            "/opt/prismlauncher/bin",
    };

    public static List<LauncherInfo> detect() {
        List<LauncherInfo> found = new ArrayList<>();

        for (String[] entry : KNOWN_LAUNCHERS) {
            String displayName = entry[0];
            String binary     = entry[1];
            String flatpakId  = entry[2];

            String binaryPath = findInDirs(binary);
            if (binaryPath != null) {
                log.info("Найден лаунчер {} по пути {}", displayName, binaryPath);
                found.add(new LauncherInfo(displayName, new String[]{binaryPath}));
                continue;
            }

            if (flatpakId != null && isFlatpakInstalled(flatpakId)) {
                log.info("Найден лаунчер {} через Flatpak ({})", displayName, flatpakId);
                found.add(new LauncherInfo(displayName, new String[]{"flatpak", "run", flatpakId}));
            }
        }

        return found;
    }

    private static String findInDirs(String binary) {
        for (String dir : BINARY_DIRS) {
            Path path = Path.of(dir, binary);
            if (Files.isExecutable(path)) return path.toString();
        }
        // Попытка через which как запасной вариант
        try {
            Process p = new ProcessBuilder("which", binary)
                    .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor(5, TimeUnit.SECONDS);
            if (p.exitValue() == 0 && !out.isBlank()) return out;
        } catch (Exception ignored) {}
        return null;
    }

    private static boolean isFlatpakInstalled(String appId) {
        try {
            Process p = new ProcessBuilder("flatpak", "info", appId)
                    .redirectErrorStream(true).start();
            p.waitFor(5, TimeUnit.SECONDS);
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
