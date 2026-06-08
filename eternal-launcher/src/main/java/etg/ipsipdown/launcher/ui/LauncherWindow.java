package etg.ipsipdown.launcher.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import etg.ipsipdown.launcher.core.UpdateCoordinator;

public class LauncherWindow extends JFrame {

    private Font minecraftFont;
    private Image backgroundImage;

    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton playButton;
    private JButton cleanLaunchBtn;

    private CardLayout mainCardLayout;
    private JPanel screensContainer;

    public LauncherWindow() {
        loadResources();

        setTitle("EternalSky");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel rootBackgroundPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(30, 30, 30));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        mainCardLayout = new CardLayout();
        screensContainer = new JPanel(mainCardLayout);
        screensContainer.setOpaque(false);

        // --- 1. ЭКРАН ГЛАВНОГО МЕНЮ ---
        JPanel mainScreen = new JPanel(new BorderLayout());
        mainScreen.setOpaque(false);

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 20));
        topBar.setOpaque(false);
        JButton settingsBtn = new JButton("⚙ Настройки");
        settingsBtn.setContentAreaFilled(false);
        settingsBtn.setBorderPainted(false);
        settingsBtn.setFocusPainted(false);
        settingsBtn.setForeground(new Color(200, 200, 200));
        settingsBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (minecraftFont != null) settingsBtn.setFont(minecraftFont.deriveFont(Font.PLAIN, 22f));
        settingsBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { settingsBtn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent evt) { settingsBtn.setForeground(new Color(200, 200, 200)); }
        });
        settingsBtn.addActionListener(e -> showScreen("SETTINGS"));
        topBar.add(settingsBtn);
        mainScreen.add(topBar, BorderLayout.NORTH);

        JPanel middleArea = new JPanel(new BorderLayout());
        middleArea.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        titlePanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JLabel l1 = createLabel("EternalSky", 60, true);
        l1.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel l2 = createLabel("Launcher", 32, true);
        l2.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(l1);
        titlePanel.add(l2);
        middleArea.add(titlePanel, BorderLayout.NORTH);

        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(new EmptyBorder(0, 0, 20, 50));

        actionPanel.add(Box.createVerticalGlue());

        playButton = new MinecraftButton("Играть");
        if (minecraftFont != null) playButton.setFont(minecraftFont.deriveFont(Font.BOLD, 28f));
        playButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        playButton.addActionListener(e -> {
            setButtonsEnabled(false);
            new UpdateCoordinator(this).startUpdateProcess(false);
        });

        cleanLaunchBtn = new JButton("Чистый запуск");
        cleanLaunchBtn.setContentAreaFilled(false);
        cleanLaunchBtn.setBorderPainted(false);
        cleanLaunchBtn.setFocusPainted(false);
        cleanLaunchBtn.setForeground(new Color(150, 150, 150));
        cleanLaunchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (minecraftFont != null) cleanLaunchBtn.setFont(minecraftFont.deriveFont(Font.PLAIN, 16f));
        cleanLaunchBtn.setAlignmentX(Component.RIGHT_ALIGNMENT);

        cleanLaunchBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { cleanLaunchBtn.setForeground(new Color(255, 100, 100)); }
            public void mouseExited(MouseEvent evt) { cleanLaunchBtn.setForeground(new Color(150, 150, 150)); }
        });
        cleanLaunchBtn.addActionListener(e -> {
            setButtonsEnabled(false);
            new UpdateCoordinator(this).startUpdateProcess(true);
        });

        actionPanel.add(playButton);
        actionPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        actionPanel.add(cleanLaunchBtn);

        middleArea.add(actionPanel, BorderLayout.EAST);
        middleArea.add(createNewsPanel(), BorderLayout.CENTER);
        mainScreen.add(middleArea, BorderLayout.CENTER);

        // --- 2. ЭКРАН НАСТРОЕК ---
        SettingsPanel settingsScreen = new SettingsPanel(this);
        screensContainer.add(mainScreen, "MAIN");
        screensContainer.add(settingsScreen, "SETTINGS");

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(0, 20, 30, 20));

        statusLabel = createLabel("Готов к запуску", 16, false);
        statusLabel.setForeground(new Color(200, 200, 200));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(800, 20));
        progressBar.setForeground(new Color(46, 204, 113));

        bottomPanel.add(statusLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        bottomPanel.add(progressBar);

        mainScreen.add(bottomPanel, BorderLayout.SOUTH);

        rootBackgroundPanel.add(screensContainer, BorderLayout.CENTER);
        add(rootBackgroundPanel);

        mainCardLayout.show(screensContainer, "MAIN");
    }

    public void showScreen(String screenName) { SwingUtilities.invokeLater(() -> mainCardLayout.show(screensContainer, screenName)); }
    public Font getMinecraftFont() { return minecraftFont; }
    public void setStatus(String text) { SwingUtilities.invokeLater(() -> statusLabel.setText(text)); }
    public void setProgress(int value) { SwingUtilities.invokeLater(() -> progressBar.setValue(value)); }

    public void setButtonsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            playButton.setEnabled(enabled);
            cleanLaunchBtn.setEnabled(enabled);
        });
    }

    private void loadResources() {
        try {
            InputStream fontStream = getClass().getResourceAsStream("/minecraft_font.ttf");
            if (fontStream != null) {
                minecraftFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(minecraftFont);
            }
            InputStream imgStream = getClass().getResourceAsStream("/background.jpg");
            if (imgStream != null) {
                backgroundImage = ImageIO.read(imgStream);
            }
        } catch (Exception e) { System.err.println("Ошибка ресурсов: " + e.getMessage()); }
    }

    private JLabel createLabel(String text, float size, boolean isBold) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        if (minecraftFont != null) label.setFont(minecraftFont.deriveFont(isBold ? Font.BOLD : Font.PLAIN, size));
        else label.setFont(new Font("SansSerif", isBold ? Font.BOLD : Font.PLAIN, (int)size));
        return label;
    }

    private class MinecraftButton extends JButton {
        private boolean isHovered = false;
        public MinecraftButton(String text) {
            super(text);
            setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false);
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(240, 60));
            setMinimumSize(new Dimension(240, 60));
            setMaximumSize(new Dimension(240, 60));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) { isHovered = true; repaint(); }
                public void mouseExited(MouseEvent evt) { isHovered = false; repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isHovered ? new Color(139, 139, 139) : new Color(110, 110, 110));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            super.paintComponent(g);
        }
    }

    private JPanel createNewsPanel() {
        JPanel newsPanel = new JPanel(new BorderLayout());
        newsPanel.setOpaque(false);
        newsPanel.setBorder(new EmptyBorder(10, 40, 10, 20));
        JLabel newsTitle = createLabel("Последние новости", 20, true);
        newsTitle.setForeground(new Color(255, 215, 0));
        newsTitle.setBorder(new EmptyBorder(0, 0, 15, 0));
        newsPanel.add(newsTitle, BorderLayout.NORTH);
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setOpaque(false); scrollPane.getViewport().setOpaque(false); scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(0, 0));
        newsPanel.add(scrollPane, BorderLayout.CENTER);
        java.util.concurrent.CompletableFuture.supplyAsync(() -> etg.ipsipdown.launcher.core.DiscordNewsFetcher.fetchLatestNews())
                .thenAccept(news -> SwingUtilities.invokeLater(() -> {
                    contentPanel.removeAll();
                    if (news.isEmpty()) contentPanel.add(createLabel("Новостей пока нет.", 14, false));
                    else {
                        for (String post : news) {
                            JTextArea textArea = new JTextArea(post);
                            textArea.setLineWrap(true); textArea.setWrapStyleWord(true); textArea.setEditable(false);
                            textArea.setOpaque(false); textArea.setForeground(Color.WHITE);
                            if (minecraftFont != null) textArea.setFont(minecraftFont.deriveFont(Font.PLAIN, 15f));
                            else textArea.setFont(new Font("SansSerif", Font.PLAIN, 15));
                            textArea.setBorder(new EmptyBorder(0, 0, 20, 0));
                            contentPanel.add(textArea);
                        }
                    }
                    contentPanel.revalidate(); contentPanel.repaint();
                }));
        return newsPanel;
    }
}