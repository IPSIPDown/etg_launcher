package etg.ipsipdown.launcher;

import com.formdev.flatlaf.FlatDarculaLaf;
import etg.ipsipdown.launcher.services.CrashReporter;
import etg.ipsipdown.launcher.services.SelfUpdateService;
import etg.ipsipdown.launcher.ui.LauncherWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        CrashReporter.install();
        log.info("Запуск EternalSky Launcher {}", SelfUpdateService.CURRENT_VERSION);

        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception e) {
            log.error("Ошибка загрузки темы", e);
        }

        SwingUtilities.invokeLater(() -> {
            LauncherWindow window = new LauncherWindow();
            window.setVisible(true);

            // Проверка обновлений — в фоне, после показа окна:
            // лаунчер больше не зависает на старте при плохой сети
            SelfUpdateService.checkAsync();
        });
    }
}
