package etg.ipsipdown.launcher.ui.components;

import etg.ipsipdown.launcher.ui.theme.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * Полупрозрачная скруглённая карточка — базовый строительный блок UI.
 */
public class RoundedPanel extends JPanel {

    private final int arc;
    private Color background = Theme.CARD_BG;
    private Color borderColor = Theme.CARD_BORDER;

    public RoundedPanel(LayoutManager layout) {
        this(layout, 14);
    }

    public RoundedPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
    }

    public void setCardBackground(Color color) {
        this.background = color;
        repaint();
    }

    public void setCardBorder(Color color) {
        this.borderColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(background);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}
