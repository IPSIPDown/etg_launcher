package etg.ipsipdown.launcher.ui.components;

import etg.ipsipdown.launcher.ui.theme.Theme;

import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Современные toast-уведомления: успех / предупреждение / ошибка / инфо.
 * Складываются в очередь в правом верхнем углу окна, появляются и исчезают плавно.
 */
public class NotificationManager {

    public enum Type {
        SUCCESS("✔", Theme.ACCENT),
        WARNING("⚠", Theme.ORANGE),
        ERROR("✖", Theme.RED),
        INFO("ℹ", Theme.BLUE);

        final String icon;
        final Color color;

        Type(String icon, Color color) {
            this.icon = icon;
            this.color = color;
        }
    }

    private static final int TOAST_WIDTH = 320;
    private static final int TOAST_HEIGHT = 56;
    private static final int MARGIN = 14;
    private static final int LIFETIME_MS = 4000;

    private final JLayeredPane layeredPane;
    private final List<Toast> active = new ArrayList<>();

    public NotificationManager(JLayeredPane layeredPane) {
        this.layeredPane = layeredPane;
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                layoutToasts();
            }
        });
    }

    public void success(String message) { show(Type.SUCCESS, message); }
    public void warning(String message) { show(Type.WARNING, message); }
    public void error(String message)   { show(Type.ERROR, message); }
    public void info(String message)    { show(Type.INFO, message); }

    public void show(Type type, String message) {
        SwingUtilities.invokeLater(() -> {
            Toast toast = new Toast(type, message);
            active.add(toast);
            layeredPane.add(toast, JLayeredPane.POPUP_LAYER);
            layoutToasts();
            toast.fadeIn();

            Timer lifetime = new Timer(LIFETIME_MS, e -> dismiss(toast));
            lifetime.setRepeats(false);
            lifetime.start();
        });
    }

    private void dismiss(Toast toast) {
        toast.fadeOut(() -> {
            active.remove(toast);
            layeredPane.remove(toast);
            layeredPane.repaint();
            layoutToasts();
        });
    }

    private void layoutToasts() {
        int x = layeredPane.getWidth() - TOAST_WIDTH - MARGIN;
        int y = MARGIN;
        for (Toast toast : active) {
            toast.setBounds(x, y, TOAST_WIDTH, TOAST_HEIGHT);
            y += TOAST_HEIGHT + 10;
        }
        layeredPane.repaint();
    }

    /** Одно уведомление: скруглённая плашка с иконкой, анимация прозрачности. */
    private static class Toast extends JPanel {

        private float alpha = 0f;
        private final Type type;

        Toast(Type type, String message) {
            super(new BorderLayout(10, 0));
            this.type = type;
            setOpaque(false);
            setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 14, 8, 14));

            JLabel icon = new JLabel(type.icon);
            icon.setForeground(type.color);
            icon.setFont(Theme.body(20f, true));
            add(icon, BorderLayout.WEST);

            JLabel text = new JLabel("<html>" + escape(message) + "</html>");
            text.setForeground(Theme.TEXT);
            text.setFont(Theme.body(13f, false));
            add(text, BorderLayout.CENTER);
        }

        private static String escape(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        }

        void fadeIn() {
            animateTo(1f, null);
        }

        void fadeOut(Runnable onDone) {
            animateTo(0f, onDone);
        }

        private void animateTo(float target, Runnable onDone) {
            Timer t = new Timer(16, null);
            t.addActionListener(e -> {
                alpha += (target - alpha) * 0.25f;
                if (Math.abs(alpha - target) < 0.03f) {
                    alpha = target;
                    t.stop();
                    if (onDone != null) onDone.run();
                }
                repaint();
            });
            t.start();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(25, 29, 36, 245));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(type.color);
            g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
            g2.setColor(Theme.CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);

            super.paint(g2);
            g2.dispose();
        }
    }
}
