package etg.ipsipdown.launcher.ui.components;

import etg.ipsipdown.launcher.ui.theme.Theme;

import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Главная акцентная кнопка («Играть») с плавной анимацией наведения.
 */
public class PrimaryButton extends JButton {

    private float hover = 0f; // 0..1, анимируется таймером
    private final Timer animator;
    private boolean targetHover = false;

    public PrimaryButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Color.WHITE);
        setFont(Theme.title(26f, true));
        setPreferredSize(new Dimension(240, 60));
        setMinimumSize(new Dimension(240, 60));
        setMaximumSize(new Dimension(240, 60));
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        animator = new Timer(16, e -> {
            float target = targetHover ? 1f : 0f;
            float next = hover + (target - hover) * 0.25f;
            if (Math.abs(next - target) < 0.02f) {
                next = target;
                ((Timer) e.getSource()).stop();
            }
            hover = next;
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                targetHover = true;
                animator.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                targetHover = false;
                animator.start();
            }
        });
    }

    private static Color lerp(Color a, Color b, float t) {
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color base = isEnabled() ? lerp(Theme.ACCENT, Theme.ACCENT_HOVER, hover) : new Color(80, 86, 94);
        g2.setColor(base);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }
}
