package etg.ipsipdown.launcher.services;

import etg.ipsipdown.launcher.ui.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Window;
import java.awt.image.BufferedImage;

/**
 * Режим «компаньона» (включается флагом DiscordRichPresence.COMPANION_MODE):
 * после запуска игры лаунчер сворачивается в трей, держит Discord-статус
 * «Играет на EternalSky» и сам закрывается, когда игра завершилась.
 */
public class CompanionMode {

    private static final Logger log = LoggerFactory.getLogger(CompanionMode.class);

    /** Сколько ждать появления процесса игры (игрок ещё кликает в официальном лаунчере). */
    private static final long WAIT_FOR_GAME_MS = 10 * 60 * 1000;
    private static final long POLL_INTERVAL_MS = 15 * 1000;

    /**
     * Вызывается после запуска официального лаунчера вместо немедленного выхода.
     * Если режим выключен или трей недоступен — просто завершает процесс (старое поведение).
     */
    public static void enterOrExit() {
        if (!DiscordRichPresence.COMPANION_MODE || !SystemTray.isSupported()) {
            System.exit(0);
        }

        try {
            SwingUtilities.invokeLater(() -> {
                for (Window w : Window.getWindows()) w.setVisible(false);
            });
            installTrayIcon();
            DiscordRichPresence.updateActivity("Играет на EternalSky", "Приватный сервер Minecraft");
            log.info("Режим компаньона: лаунчер свернут в трей, следим за игрой");

            watchGameAndExit();
        } catch (Exception e) {
            log.warn("Режим компаньона не удался, выходим: {}", e.getMessage());
            System.exit(0);
        }
    }

    private static void installTrayIcon() throws Exception {
        TrayIcon icon = new TrayIcon(createTrayImage(), "EternalSky — игра запущена");
        PopupMenu menu = new PopupMenu();
        MenuItem exit = new MenuItem("Выход");
        exit.addActionListener(e -> System.exit(0));
        menu.add(exit);
        icon.setPopupMenu(menu);
        SystemTray.getSystemTray().add(icon);
    }

    /** Простая иконка для трея: зелёный квадрат с буквой E. */
    private static BufferedImage createTrayImage() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Theme.ACCENT);
        g.fillRoundRect(0, 0, 16, 16, 5, 5);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("E", 4, 13);
        g.dispose();
        return img;
    }

    /** Ждём появления процесса игры, затем его завершения — и выходим. */
    private static void watchGameAndExit() throws InterruptedException {
        long waitStart = System.currentTimeMillis();
        boolean gameSeen = false;

        while (true) {
            boolean running = isGameRunning();

            if (running) {
                gameSeen = true;
            } else if (gameSeen) {
                log.info("Игра завершилась — закрываем лаунчер");
                System.exit(0);
            } else if (System.currentTimeMillis() - waitStart > WAIT_FOR_GAME_MS) {
                log.info("Игра так и не запустилась за 10 минут — закрываем лаунчер");
                System.exit(0);
            }

            Thread.sleep(POLL_INTERVAL_MS);
        }
    }

    /**
     * Ищем java-процесс игры: его командная строка содержит путь .eternalsky (gameDir).
     * На Windows commandLine() бывает недоступен — тогда ориентируемся по факту
     * наличия javaw.exe, запущенного позже нас.
     */
    private static boolean isGameRunning() {
        long ourStart = ProcessHandle.current().info().startInstant()
                .map(java.time.Instant::toEpochMilli).orElse(0L);

        return ProcessHandle.allProcesses().anyMatch(p -> {
            ProcessHandle.Info info = p.info();
            String cmd = info.commandLine().orElse("");
            if (!cmd.isEmpty()) {
                return cmd.contains(".eternalsky") && cmd.contains("java");
            }
            // Фолбэк: javaw.exe, стартовавший после нас (так запускается клиент MC)
            String exe = info.command().orElse("");
            if (!exe.endsWith("javaw.exe")) return false;
            long started = info.startInstant().map(java.time.Instant::toEpochMilli).orElse(0L);
            return started > ourStart;
        });
    }
}
