package etg.ipsipdown.launcher.services;

import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Глобальный обработчик аварий. При любой необработанной ошибке собирает
 * диагностический zip-отчёт в %APPDATA%\.eternalsky\crash-reports:
 * stacktrace + версии Java/ОС + логи лаунчера + latest.log Майнкрафта.
 */
public class CrashReporter {

    private static final Logger log = LoggerFactory.getLogger(CrashReporter.class);

    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler(CrashReporter::report);
    }

    public static void report(Thread thread, Throwable throwable) {
        log.error("Необработанная ошибка в потоке \"{}\"", thread.getName(), throwable);
        try {
            Path reportsDir = OsPaths.GAME_DIR.resolve("crash-reports");
            Files.createDirectories(reportsDir);

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            Path zipPath = reportsDir.resolve("crash-" + timestamp + ".zip");

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                zos.putNextEntry(new ZipEntry("report.txt"));
                zos.write(buildReport(thread, throwable).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                addFileIfExists(zos, OsPaths.LOGS_DIR.resolve("launcher.log"), "launcher.log");
                addFileIfExists(zos, OsPaths.LOGS_DIR.resolve("update.log"), "update.log");
                addFileIfExists(zos, OsPaths.LOGS_DIR.resolve("errors.log"), "errors.log");
                // Лог самой игры: .eternalsky — это gameDir профиля, Minecraft пишет туда
                addFileIfExists(zos, OsPaths.LOGS_DIR.resolve("latest.log"), "minecraft-latest.log");
            }

            log.info("Crash-отчёт сохранён: {}", zipPath);
            showDialog(zipPath);
        } catch (Exception e) {
            log.error("Не удалось сформировать crash-отчёт", e);
        }
    }

    private static String buildReport(Thread thread, Throwable throwable) {
        StringWriter stackTrace = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stackTrace));

        return "=== EternalSky Launcher Crash Report ===\n"
                + "Время:          " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()) + "\n"
                + "Версия лаунчера: " + SelfUpdateService.CURRENT_VERSION + "\n"
                + "Поток:          " + thread.getName() + "\n"
                + "\n--- Система ---\n"
                + "Java:           " + System.getProperty("java.version")
                + " (" + System.getProperty("java.vendor") + ")\n"
                + "Java home:      " + System.getProperty("java.home") + "\n"
                + "ОС:             " + System.getProperty("os.name")
                + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")\n"
                + "Память JVM:     " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB max, "
                + (Runtime.getRuntime().totalMemory() / 1024 / 1024) + " MB выделено\n"
                + "Рабочая папка:  " + System.getProperty("user.dir") + "\n"
                + "\n--- Stacktrace ---\n"
                + stackTrace;
    }

    private static void addFileIfExists(ZipOutputStream zos, Path file, String entryName) {
        if (!Files.exists(file)) return;
        try {
            zos.putNextEntry(new ZipEntry(entryName));
            Files.copy(file, zos);
            zos.closeEntry();
        } catch (Exception e) {
            log.warn("Не удалось добавить {} в crash-отчёт: {}", entryName, e.getMessage());
        }
    }

    private static void showDialog(Path zipPath) {
        if (GraphicsEnvironment.isHeadless()) return;
        SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showOptionDialog(null,
                    "Лаунчер столкнулся с непредвиденной ошибкой.\n"
                            + "Отчёт для разработчика сохранён:\n" + zipPath + "\n\n"
                            + "Отправь этот файл в Discord сервера.",
                    "Ошибка EternalSky",
                    JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
                    null, new Object[]{"Открыть папку", "Закрыть"}, "Открыть папку");
            if (choice == JOptionPane.YES_OPTION) {
                try {
                    Desktop.getDesktop().open(zipPath.getParent().toFile());
                } catch (Exception e) {
                    log.warn("Не удалось открыть папку с отчётом", e);
                }
            }
        });
    }
}
