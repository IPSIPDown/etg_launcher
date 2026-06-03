package etg.ipsipdown.launcher;

import com.formdev.flatlaf.FlatDarculaLaf;
import etg.ipsipdown.launcher.core.LauncherUpdater;
import etg.ipsipdown.launcher.ui.LauncherWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {

        // 1. СНАЧАЛА ПРОВЕРЯЕМ ОБНОВЛЕНИЯ САМОГО ЛАУНЧЕРА
        LauncherUpdater.checkAndUpdate();

        // 2. Включаем темную тему
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (Exception e) {
            System.err.println("Ошибка загрузки темы: " + e.getMessage());
        }

        // 3. Запускаем окно (это выполнится, только если обновления нет)
        SwingUtilities.invokeLater(() -> {
            LauncherWindow window = new LauncherWindow();
            window.setVisible(true);
        });
    }
}