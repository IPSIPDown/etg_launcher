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
import java.awt.Color;
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
    private JLabel serverNamesLabel;

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

        // Кнопки-ссылки внизу под новостями: Discord / GitHub / Поддержать автора.
        // Лежат внутри левой части, чтобы правая колонка доставала до самого низа.
        container.add(createBottomLinks(), BorderLayout.SOUTH);

        JLabel loading = mutedLabel("Загрузка новостей...");
        newsContent.add(loading);
        return container;
    }

    public void refreshNews() {
        CompletableFuture.supplyAsync(NewsService::fetchLatestNews)
                .thenAccept(news -> SwingUtilities.invokeLater(() -> {
                    notifyIfFreshNews(news);
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

    /** Показывает toast, если появилась новость, которой пользователь ещё не видел. */
    private void notifyIfFreshNews(List<NewsItem> news) {
        if (news.isEmpty()) return;
        NewsItem latest = news.get(0);
        String key = Integer.toHexString((latest.title + "|" + latest.date + "|" + latest.text).hashCode());
        String lastSeen = etg.ipsipdown.launcher.services.CacheService.getText("news/last_seen.txt");
        if (lastSeen != null && !lastSeen.trim().equals(key)) {
            String label = (latest.title != null && !latest.title.isBlank()) ? latest.title : shorten(latest.text, 40);
            window.getNotifications().info("Новая новость: " + label);
        }
        etg.ipsipdown.launcher.services.CacheService.putText("news/last_seen.txt", key);
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

        // Высоту считаем по реально обёрнутому тексту (минимальная ширина ленты ~430px),
        // иначе длинные новости обрезались снизу
        text.setSize(new Dimension(430, Short.MAX_VALUE));
        int textHeight = text.getPreferredSize().height;
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, textHeight + 70));
        return card;
    }

    // ====================== ПРАВАЯ КОЛОНКА ======================

    private JPanel createRightColumn() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(280, 0));

        JPanel serverCard = createServerCard();
        serverCard.setAlignmentX(Component.RIGHT_ALIGNMENT);
        column.add(serverCard);
        column.add(Box.createRigidArea(new Dimension(0, 12)));

        JPanel buildCard = createBuildCard();
        buildCard.setAlignmentX(Component.RIGHT_ALIGNMENT);
        column.add(buildCard);
        column.add(Box.createRigidArea(new Dimension(0, 10)));

        // Чистый запуск и Папка игры — сразу под блоком «Сборка», по правому краю
        cleanLaunchBtn = new GhostButton("Чистый запуск", 16f, Color.WHITE, Theme.RED);
        cleanLaunchBtn.setFont(Theme.title(16f, false));
        cleanLaunchBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        cleanLaunchBtn.setToolTipText("Включает все моды и проверяет файлы заново");
        cleanLaunchBtn.addActionListener(e -> launch(true));
        column.add(cleanLaunchBtn);
        column.add(Box.createRigidArea(new Dimension(0, 4)));

        GhostButton folderBtn = new GhostButton("Папка игры", 16f, Color.WHITE, Theme.ACCENT_HOVER);
        folderBtn.setFont(Theme.title(16f, false));
        folderBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);
        folderBtn.addActionListener(e -> {
            try {
                Files.createDirectories(OsPaths.GAME_DIR);
                Desktop.getDesktop().open(OsPaths.GAME_DIR.toFile());
            } catch (Exception ex) {
                window.getNotifications().warning("Не удалось открыть папку игры");
            }
        });
        column.add(folderBtn);

        // Играть — в самом низу колонки, вплотную к прогресс-бару
        column.add(Box.createVerticalGlue());
        playButton = new PrimaryButton("Играть");
        playButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        playButton.addActionListener(e -> launch(false));
        column.add(playButton);
        return column;
    }

    private JPanel createServerCard() {
        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));

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
        serverNamesLabel = mutedLabel("");
        serverNamesLabel.setFont(Theme.body(12f, false));

        rows.add(serverStateLabel);
        rows.add(Box.createRigidArea(new Dimension(0, 4)));
        rows.add(serverPlayersLabel);
        rows.add(serverNamesLabel);
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
            if (status.players.isEmpty()) {
                serverNamesLabel.setText("");
                serverNamesLabel.setToolTipText(null);
            } else {
                serverNamesLabel.setText(shorten("В игре: " + String.join(", ", status.players), 38));
                serverNamesLabel.setToolTipText(String.join(", ", status.players));
            }
            serverMotdLabel.setText(shorten(status.motd, 34));
            serverPingLabel.setText("Пинг: " + status.pingMs + " мс • " + status.version);
        } else {
            serverStateLabel.setText("● Офлайн");
            serverStateLabel.setForeground(Theme.RED);
            serverPlayersLabel.setText("Сервер недоступен");
            serverNamesLabel.setText("");
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

    /** Нижний ряд слева: Discord / GitHub / Поддержать автора. */
    private JPanel createBottomLinks() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 22, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 2, 0, 0));

        row.add(bottomLink("Discord", () -> openUrl(Theme.DISCORD_URL)));
        row.add(bottomLink("GitHub", () -> openUrl(Theme.SITE_URL)));
        row.add(bottomLink("Поддержать автора", () ->
                CompletableFuture.supplyAsync(etg.ipsipdown.launcher.services.SupportService::click)
                        .thenAccept(msg -> window.getNotifications().info(msg))));
        return row;
    }

    private JButton bottomLink(String text, Runnable action) {
        GhostButton btn = new GhostButton(text, 15f, Color.WHITE, Theme.GOLD);
        btn.setFont(Theme.title(15f, false));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void openUrl(String url) {
        if (url == null || url.isBlank()) {
            window.getNotifications().warning("Ссылка пока не настроена");
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (Exception ex) {
            window.getNotifications().warning("Не удалось открыть ссылку");
        }
    }

    private void launch(boolean clean) {
        setLaunchEnabled(false);
        window.getNotifications().info(clean ? "Чистый запуск: проверяем все файлы..." : "Подготовка к запуску...");
        new LaunchController(window, () -> {
            setLaunchEnabled(true);
            window.getNotifications().error("Не удалось запустить игру. Подробности в логах.");
        }, syncResult -> {
            window.getNotifications().success("Сборка обновлена: " + syncResult.summary());
            refreshBuildInfo();
        }).startLaunch(clean);
    }

    public void setLaunchEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            playButton.setEnabled(enabled);
            cleanLaunchBtn.setEnabled(enabled);
        });
    }

    private JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.body(13f, false));
        l.setForeground(Theme.TEXT_MUTED);
        return l;
    }
}
