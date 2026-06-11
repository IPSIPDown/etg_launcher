package etg.ipsipdown.launcher.ui.theme;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.io.InputStream;

/**
 * Единая точка стиля лаунчера: палитра, шрифты, ресурсы, ссылки.
 */
public final class Theme {

    private static final Logger log = LoggerFactory.getLogger(Theme.class);

    // --- Палитра (тёмная, в духе современных игровых лаунчеров) ---
    public static final Color BG = new Color(20, 23, 28);
    public static final Color OVERLAY = new Color(0, 0, 0, 130);          // затемнение поверх баннера
    public static final Color SIDEBAR_BG = new Color(13, 15, 19, 230);
    public static final Color CARD_BG = new Color(22, 26, 32, 215);
    public static final Color CARD_BORDER = new Color(255, 255, 255, 26);

    public static final Color ACCENT = new Color(46, 204, 113);
    public static final Color ACCENT_HOVER = new Color(64, 224, 132);
    public static final Color GOLD = new Color(255, 215, 0);
    public static final Color RED = new Color(231, 76, 60);
    public static final Color ORANGE = new Color(243, 156, 18);
    public static final Color BLUE = new Color(52, 152, 219);

    public static final Color TEXT = new Color(242, 243, 245);
    public static final Color TEXT_MUTED = new Color(154, 163, 173);
    public static final Color TEXT_DISABLED = new Color(110, 117, 126);

    // --- Полезные ссылки (пустая строка = кнопка не показывается) ---
    public static final String DISCORD_URL = "https://discord.gg/g45evaAt8Y";
    public static final String SITE_URL = "https://github.com/IPSIPDown";

    // --- Ресурсы ---
    private static Font minecraftFont;
    private static Image backgroundImage;
    private static boolean loaded;

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            InputStream fontStream = Theme.class.getResourceAsStream("/minecraft_font.ttf");
            if (fontStream != null) {
                minecraftFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(minecraftFont);
            }
            InputStream imgStream = Theme.class.getResourceAsStream("/background.jpg");
            if (imgStream != null) {
                backgroundImage = ImageIO.read(imgStream);
            }
        } catch (Exception e) {
            log.error("Ошибка загрузки ресурсов темы", e);
        }
    }

    /** Фирменный пиксельный шрифт — для заголовков и акцентов. */
    public static Font title(float size, boolean bold) {
        ensureLoaded();
        if (minecraftFont != null) return minecraftFont.deriveFont(bold ? Font.BOLD : Font.PLAIN, size);
        return new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, (int) size);
    }

    /** Системный шрифт — для обычного текста (читабельность). */
    public static Font body(float size, boolean bold) {
        return new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, Math.round(size));
    }

    public static Image background() {
        ensureLoaded();
        return backgroundImage;
    }

    private Theme() {
    }
}
