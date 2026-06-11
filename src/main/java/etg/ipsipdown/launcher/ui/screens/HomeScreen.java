package etg.ipsipdown.launcher.ui.screens;

import etg.ipsipdown.launcher.controllers.LaunchController;
import etg.ipsipdown.launcher.models.NewsItem;
import etg.ipsipdown.launcher.models.ServerStatus;
import etg.ipsipdown.launcher.services.NewsService;
import etg.ipsipdown.launcher.services.ServerStatusService;
import etg.ipsipdown.launcher.models.LauncherSettings;
import etg.ipsipdown.launcher.services.NeoForgeInstaller;
import etg.ipsipdown.launcher.ui.LauncherWindow;
import etg.ipsipdown.launcher.ui.components.GhostButton;
import etg.ipsipdown.launcher.ui.components.PrimaryButton;
import etg.ipsipdown.launcher.ui.components.RoundedPanel;
import etg.ipsipdown.launcher.ui.theme.Theme;
import etg.ipsipdown.launcher.utils.OsPaths;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Главный экран: баннер-заголовок, новости, статус сервера,
 * информация о сборке, кнопка запуска и полезные ссылки.
 */
public class HomeScreen extends JPanel {

    private final LauncherWindow window;

    private PrimaryButton playButton;
    private GhostButton cleanLaunchBtn;

    // Карточка статуса сервера
    private JLabel serverStateLabel;
    private JLabel serverPlayersLabel;
    private JLabel serverMotdLabel;
    private JLabel serverPingLabel;

    // Карточка сборки
    private JLabel modsCountLabel;

    public HomeScreen(LauncherWindow window) {
        this.window = window;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 24, 10, 24));

        // --- Заголовок-баннер ---
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel("EternalSky");
        title.setFont(Theme.title(52f, true));
        title.setForeground(Theme.TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Приватный сервер Minecraft • NeoForge " + NeoForgeInstaller.NEOFORGE_VERSION);
        subtitle.setFont(Theme.body(14f, false));
        subtitle.setForeground(Theme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titlePanel.add(title);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 4)));
        titlePanel.add(subtitle);
        titlePanel.add(Box.createRigidArea(new Dimension(0, 14)));
        add(titlePanel, BorderLayout.NORTH);

        // --- Центр: новости слева, карточки справа ---
        JPanel center = new JPanel(new BorderLayout(18, 0));
        center.setOpaque(false);

        center.add(createNewsPanel(), BorderLayout.CENTER);
        center.add(createRightColumn(), BorderLayout.EAST);

        add(center, BorderLayout.CENTER);

        refreshNews();
        startServerStatusUpdates();
        refreshBuildInfo();
    }

    // ====================== НОВОСТИ ======================

    private JPanel newsContent;

    private JPanel createNewsPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        JLabel newsTitle = new JLabel("Последние новости");
        newsTitle.setFont(Theme.title(20f, true));
        newsTitle.setForeground(Theme.GOLD);
        newsTitle.setBorder(new EmptyBorder(0, 2, 10, 0));
        container.add(newsTitle, BorderLayout.NORTH);

        newsContent = new JPanel();
        newsContent.setLayout(new BoxLayout(newsContent, BoxLayout.Y_AXIS));
        newsContent.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(newsContent);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(6, 0));
        container.add(scrollPane, BorderLayout.CENTER);

        JLabel loading = mutedLabel("Загрузка новостей...");
        newsContent.add(loading);
        return container;
    }

    public void refreshNews() {
        CompletableFuture.supplyAsync(NewsService::fetchLatestNews)
                .thenAccept(news -> SwingUtilities.invokeLater(() -> {
                    newsContent.removeAll();
                    if (news.isEmpty()) {
                        newsContent.add(mutedLabel("Новостей пока нет."));
                    } else {
                        for (NewsItem item : news) {
                            newsContent.add(createNewsCard(item));
                            newsContent.add(Box.createRigidArea(new Dimension(0, 10)));
                        }
                    }
                    newsContent.revalidate();
                    newsContent.repaint();
                }));
    }

    private JPanel createNewsCard(NewsItem item) {
        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        if (item.title != null && !item.title.isBlank()) {
            JLabel t = new JLabel(item.title);
            t.setFont(Theme.body(15f, true));
            t.setForeground(Theme.TEXT);
            header.add(t, BorderLayout.WEST);
        }
        if (item.date != null && !item.date.isBlank()) {
            JLabel d = new JLabel(item.date);
            d.setFont(Theme.body(12f, false));
            d.setForeground(Theme.TEXT_MUTED);
            header.add(d, BorderLayout.EAST);
        }
        card.add(header, BorderLayout.NORTH);

        JTextArea text = new JTextArea(item.text);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFocusable(false);
        text.setOpaque(false);
        text.setForeground(Theme.TEXT_MUTED);
        text.setFont(Theme.body(13f, false));
        text.setBorder(new EmptyBorder(6, 0, 0, 0));
        card.add(text, BorderLayout.CENTER);

        // Не даём карточке растягиваться по вертикали больше содержимого
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height + 60));
        return card;
    }

    // ====================== ПРАВАЯ КОЛОНКА ======================

    private JPanel createRightColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(280, 0));

        column.add(createServerCard());
        column.add(Box.createRigidArea(new Dimension(0, 12)));
        column.add(createBuildCard());
        column.add(Box.createVerticalGlue());
        column.add(createActionPanel());
        return column;
    }

    private JPanel createServerCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JLabel header = new JLabel("Сервер");
        header.setFont(Theme.body(13f, true));
        header.setForeground(Theme.TEXT_MUTED);
        card.add(header, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.setBorder(new EmptyBorder(6, 0, 0, 0));

        serverStateLabel = new JLabel("Проверка...");
        serverStateLabel.setFont(Theme.body(16f, true));
        serverStateLabel.setForeground(Theme.TEXT_MUTED);

        serverPlayersLabel = mutedLabel("");
        serverMotdLabel = mutedLabel("");
        serverPingLabel = mutedLabel("");

        rows.add(serverStateLabel);
        rows.add(Box.createRigidArea(new Dimension(0, 4)));
        rows.add(serverPlayersLabel);
        rows.add(serverMotdLabel);
        rows.add(serverPingLabel);
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private void startServerStatusUpdates() {
        refreshServerStatus();
        // Автообновление раз в 60 секунд
        Timer timer = new Timer(60_000, e -> refreshServerStatus());
        timer.start();
    }

    public void refreshServerStatus() {
        LauncherSettings settings = LauncherSettings.load();
        String host = settings.serverIp;
        int port = settings.serverPort;

        CompletableFuture.supplyAsync(() -> ServerStatusService.ping(host, port))
                .thenAccept(status -> SwingUtilities.invokeLater(() -> updateServerCard(status)));
    }

    private void updateServerCard(ServerStatus status) {
        if (status.online) {
            serverStateLabel.setText("● Онлайн");
            serverStateLabel.setForeground(Theme.ACCENT);
            serverPlayersLabel.setText("Игроки: " + status.playersOnline + " / " + status.playersMax);
            serverMotdLabel.setText(shorten(status.motd, 34));
            serverPingLabel.setText("Пинг: " + status.pingMs + " мс • " + status.version);
        } else {
            serverStateLabel.setText("● Офлайн");
            serverStateLabel.setForeground(Theme.RED);
            serverPlayersLabel.setText("Сервер недоступен");
            serverMotdLabel.setText("");
            serverPingLabel.setText("");
        }
    }

    private static String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // ====================== СБОРКА ======================

    private JPanel createBuildCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel header = new JLabel("Сборка");
        header.setFont(Theme.body(13f, true));
        header.setForeground(Theme.TEXT_MUTED);
        card.add(header, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setOpaque(false);
        rows.setBorder(new EmptyBorder(6, 0, 0, 0));

        JLabel versionLabel = mutedLabel("Minecraft 1.21.1 • NeoForge " + NeoForgeInstaller.NEOFORGE_VERSION);
        modsCountLabel = mutedLabel("Модов: ...");

        rows.add(versionLabel);
        rows.add(modsCountLabel);
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    public void refreshBuildInfo() {
        CompletableFuture.supplyAsync(() -> {
            try (Stream<java.nio.file.Path> stream = Files.list(OsPaths.MODS_DIR)) {
                return stream.filter(p -> p.toString().endsWith(".jar")).count();
            } catch (Exception e) {
                return -1L;
            }
        }).thenAccept(count -> SwingUtilities.invokeLater(() ->
                modsCountLabel.setText(count >= 0 ? "Модов установлено: " + count : "Сборка ещё не установлена")));
    }

    // ====================== ЗАПУСК И ССЫЛКИ ======================

    private JPanel createActionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(10, 0, 0, 0));

        playButton = new PrimaryButton("Играть");
        playButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playButton.addActionListener(e -> launch(false));

        cleanLaunchBtn = new GhostButton("Чистый запуск", 13f, Theme.TEXT_MUTED, Theme.RED);
        cleanLaunchBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cleanLaunchBtn.setToolTipText("Включает все моды и проверяет файлы заново");
        cleanLaunchBtn.addActionListener(e -> launch(true));

        panel.add(playButton);
        panel.add(Box.createRigidArea(new Dimension(0, 6)));
        panel.add(cleanLaunchBtn);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(createLinksRow());
        return panel;
    }

    private void launch(boolean clean) {
        setLaunchEnabled(false);
        window.getNotifications().info(clean ? "Чистый запуск: проверяем все файлы..." : "Подготовка к запуску...");
        new LaunchController(window, () -> {
            setLaunchEnabled(true);
            window.getNotifications().error("Не удалось запустить игру. Подробности в логах.");
        }).startLaunch(clean);
    }

    public void setLaunchEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            playButton.setEnabled(enabled);
            cleanLaunchBtn.setEnabled(enabled);
        });
    }

    private JPanel createLinksRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        row.setOpaque(false);

        if (!Theme.DISCORD_URL.isBlank()) {
            row.add(linkButton("Discord", Theme.DISCORD_URL));
        }
        if (!Theme.SITE_URL.isBlank()) {
            row.add(linkButton("Сайт", Theme.SITE_URL));
        }

        GhostButton folderBtn = new GhostButton("Папка игры", 12f);
        folderBtn.addActionListener(e -> {
            try {
                Files.createDirectories(OsPaths.GAME_DIR);
                Desktop.getDesktop().open(OsPaths.GAME_DIR.toFile());
            } catch (Exception ex) {
                window.getNotifications().warning("Не удалось открыть папку игры");
            }
        });
        row.add(folderBtn);
        return row;
    }

    private JButton linkButton(String text, String url) {
        GhostButton btn = new GhostButton(text, 12f, Theme.TEXT_MUTED, Theme.BLUE);
        btn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (Exception ex) {
                window.getNotifications().warning("Не удалось открыть ссылку");
            }
        });
        return btn;
    }

    private JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.body(13f, false));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }
}
