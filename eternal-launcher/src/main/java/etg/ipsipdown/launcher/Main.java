package etg.ipsipdown.launcher;

import com.formdev.flatlaf.FlatDarculaLaf;
import etg.ipsipdown.launcher.net.UpdateManager;
import etg.ipsipdown.launcher.ui.LauncherWindow;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            LauncherWindow window = new LauncherWindow();
            window.setVisible(true);

            // Инициализируем наш менеджер обновлений
            UpdateManager updateManager = new UpdateManager(window);

            window.getPlayButton().addActionListener(e -> {
                // Блокируем кнопку, чтобы игрок не нажал дважды
                window.setButtonEnabled(false);

                // Запускаем реальный процесс загрузки
                updateManager.startUpdate();
            });
        });
    }
}