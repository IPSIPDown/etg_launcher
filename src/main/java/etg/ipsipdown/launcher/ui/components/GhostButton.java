package etg.ipsipdown.launcher.ui.components;

import etg.ipsipdown.launcher.ui.theme.Theme;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Плоская текстовая кнопка с подсветкой при наведении.
 */
public class GhostButton extends JButton {

    public GhostButton(String text, float fontSize) {
        this(text, fontSize, Theme.TEXT_MUTED, Color.WHITE);
    }

    public GhostButton(String text, float fontSize, Color normal, Color hover) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setForeground(normal);
        setFont(Theme.body(fontSize, false));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setForeground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setForeground(normal);
            }
        });
    }
}
