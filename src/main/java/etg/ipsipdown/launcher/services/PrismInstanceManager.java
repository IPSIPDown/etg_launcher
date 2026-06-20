package etg.ipsipdown.launcher.services;

import etg.ipsipdown.launcher.models.LauncherSettings;
import etg.ipsipdown.launcher.services.ThirdPartyLauncherDetector.LauncherInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Создаёт и поддерживает актуальным инстанс EternalSky в PrismLauncher / MultiMC.
 * Формат конфигов совместим с PrismLauncher 8+ и MultiMC 0.7+.
 */
public class PrismInstanceManager {

    private static final Logger log = LoggerFactory.getLogger(PrismInstanceManager.class);

    public static final String INSTANCE_NAME = "EternalSky";

    /** true, если лаунчер поддерживает автоматическое управление инстансом. */
    public static boolean supports(LauncherInfo launcher) {
        String name = launcher.name().toLowerCase();
        return name.contains("prism") || name.contains("multimc");
    }

    /**
     * Создаёт инстанс EternalSky если его нет, обновляет настройки памяти.
     * Возвращает путь к .minecraft внутри инстанса.
     */
    public static Path ensureInstance(LauncherInfo launcher, LauncherSettings settings) throws Exception {
        Path dataDir    = findDataDir(launcher);
        Path instanceDir = dataDir.resolve("instances").resolve(INSTANCE_NAME);
        Path minecraftDir = instanceDir.resolve(".minecraft");

        if (!Files.exists(instanceDir.resolve("mmc-pack.json"))) {
            log.info("Создаю инстанс EternalSky в {}", instanceDir);
            createInstance(instanceDir, minecraftDir);
        }

        // Обновляем настройки памяти при каждом запуске
        writeInstanceCfg(instanceDir, settings);

        return minecraftDir;
    }

    /** Возвращает путь к папке данных лаунчера (где лежит /instances). */
    public static Path findDataDir(LauncherInfo launcher) {
        String home = System.getProperty("user.home");
        boolean isFlatpak = launcher.command().length > 0 && "flatpak".equals(launcher.command()[0]);
        boolean isMultiMC = launcher.name().toLowerCase().contains("multimc");

        if (isFlatpak) {
            // Flatpak хранит данные в ~/.var/app/<AppId>/data/<Name>
            String appId = launcher.command()[launcher.command().length - 1];
            String subDir = isMultiMC ? "multimc" : "PrismLauncher";
            return Path.of(home, ".var", "app", appId, "data", subDir);
        }

        if (isMultiMC) {
            Path p = Path.of(home, ".local", "share", "multimc");
            return Files.exists(p) ? p : Path.of(home, ".multimc");
        }

        return Path.of(home, ".local", "share", "PrismLauncher");
    }

    private static void createInstance(Path instanceDir, Path minecraftDir) throws Exception {
        Files.createDirectories(minecraftDir.resolve("mods"));
        Files.createDirectories(minecraftDir.resolve("config"));

        // mmc-pack.json — компоненты: LWJGL 3 + Minecraft 1.21.1 + NeoForge 21.1.228
        String mmcPack = """
                {
                    "components": [
                        {
                            "cachedName": "LWJGL 3",
                            "cachedVersion": "3.3.3",
                            "dependencyOnly": true,
                            "uid": "org.lwjgl3",
                            "version": "3.3.3"
                        },
                        {
                            "cachedName": "Minecraft",
                            "cachedRequires": [
                                {"suggests": "3.3.3", "uid": "org.lwjgl3"}
                            ],
                            "cachedVersion": "1.21.1",
                            "important": true,
                            "uid": "net.minecraft",
                            "version": "1.21.1"
                        },
                        {
                            "cachedName": "NeoForge",
                            "cachedRequires": [
                                {"equals": "1.21.1", "uid": "net.minecraft"}
                            ],
                            "cachedVersion": "21.1.228",
                            "uid": "net.neoforged",
                            "version": "21.1.228"
                        }
                    ],
                    "formatVersion": 1
                }
                """;
        Files.writeString(instanceDir.resolve("mmc-pack.json"), mmcPack);
        log.info("Инстанс EternalSky создан в {}", instanceDir);
    }

    private static void writeInstanceCfg(Path instanceDir, LauncherSettings settings) throws Exception {
        // PrismLauncher управляет -Xmx/-Xms через MaxMemAlloc — не дублируем их в JvmArgs
        String jvmArgs = settings.jvmArgs.trim();

        String cfg = "[General]\n"
                + "ConfigVersion=1.3\n"
                + "InstanceType=OneSix\n"
                + "name=" + INSTANCE_NAME + "\n"
                + "iconKey=default\n"
                + "OverrideMemory=true\n"
                + "MaxMemAlloc=" + settings.ramMegabytes + "\n"
                + "MinMemAlloc=512\n"
                + "OverrideJavaArgs=true\n"
                + "JvmArgs=" + jvmArgs + "\n";

        if (settings.customJavaPath != null && !settings.customJavaPath.isBlank()) {
            cfg += "OverrideJavaLocation=true\n"
                 + "JavaPath=" + settings.customJavaPath + "\n";
        }

        Files.writeString(instanceDir.resolve("instance.cfg"), cfg);
    }
}
