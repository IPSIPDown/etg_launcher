package etg.ipsipdown.launcher;

import com.formdev.flatlaf.FlatDarculaLaf;
import etg.ipsipdown.launcher.core.LauncherUpdater;
import etg.ipsipdown.launcher.ui.LauncherWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {


        LauncherUpdater.checkAndUpdate();


        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception e) {
            System.err.println("Ошибка загрузки темы: " + e.getMessage());
        }


        SwingUtilities.invokeLater(() -> {
            LauncherWindow window = new LauncherWindow();
            window.setVisible(true);
        });
    }
}