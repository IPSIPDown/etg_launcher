package etg.ipsipdown.launcher.ui;

import etg.ipsipdown.launcher.events.ProgressListener;
import etg.ipsipdown.launcher.services.SelfUpdateService;
import etg.ipsipdown.launcher.ui.components.NotificationManager;
import etg.ipsipdown.launcher.ui.screens.HomeScreen;
import etg.ipsipdown.launcher.ui.screens.LogsScreen;
import etg.ipsipdown.launcher.ui.screens.ModsScreen;
import etg.ipsipdown.launcher.ui.screens.SettingsScreen;
import etg.ipsipdown.launcher.ui.theme.Theme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Главное окно 2.0: левый сайдбар-навигация (Главная / Сборка / Настройки),
 * экраны на CardLayout, общий статус-бар с прогрессом внизу,
 * toast-уведомления поверх всего.
 */
public class LauncherWindow extends JFrame implements ProgressListener {

    private final CardLayout screenLayout = new CardLayout();
    private final JPanel screensContainer = new JPanel(screenLayout);
    private final Map<String, NavButton> navButtons = new LinkedHashMap<>();

    private JProgressBar progressBar;
    private JLabel statusLabel;

    private HomeScreen homeScreen;
    private NotificationManager notifications;

    public LauncherWindow() {
        setTitle("EternalSky");
        setSize(1100, 650);
        setMinimumSize(new Dimension(980, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(true);

        // Фон: баннер + затемнение
        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image bg = Theme.background();
                if (bg != null) {
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(Theme.BG);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                g.setColor(Theme.OVERLAY);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        notifications = new NotificationManager(getLayeredPane());

        // --- Экраны ---
        screensContainer.setOpaque(false);
        homeScreen = new HomeScreen(this);
        screensContainer.add(homeScreen, "HOME");
        screensContainer.add(new ModsScreen(this), "MODS");
        screensContainer.add(new SettingsScreen(this), "SETTINGS");
        screensContainer.add(new LogsScreen(), "LOGS");

        // --- Сайдбар ---
        root.add(createSidebar(), BorderLayout.WEST);
        root.add(screensContainer, BorderLayout.CENTER);
        root.add(createBottomBar(), BorderLayout.SOUTH);

        add(root);
        showScreen("HOME");
    }

    // ====================== НАВИГАЦИЯ ======================

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.SIDEBAR_BG);
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        sidebar.setOpaque(false);
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(190, 0));
        sidebar.setBorder(new EmptyBorder(22, 14, 18, 14));

        JLabel logo = new JLabel("EternalSky");
        logo.setFont(Theme.title(22f, true));
        logo.setForeground(Theme.TEXT);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 26)));

        addNavButton(sidebar, "HOME", "⌂  Главная");
        addNavButton(sidebar, "MODS", "🧩  Сборка");
        addNavButton(sidebar, "SETTINGS", "⚙  Настройки");
        addNavButton(sidebar, "LOGS", "≡  Логи");

        sidebar.add(Box.createVerticalGlue());

        JLabel version = new JLabel("v" + SelfUpdateService.CURRENT_VERSION);
        version.setFont(Theme.body(12f, false));
        version.setForeground(Theme.TEXT_DISABLED);
        version.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(version);

        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String screen, String text) {
        NavButton btn = new NavButton(text);
        btn.addActionListener(e -> showScreen(screen));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        navButtons.put(screen, btn);
        sidebar.add(btn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
    }

    public void showScreen(String screenName) {
        SwingUtilities.invokeLater(() -> {
            screenLayout.show(screensContainer, screenName);
            navButtons.forEach((name, btn) -> btn.setActive(name.equals(screenName)));
        });
    }

    /** Кнопка навигации в сайдбаре с состоянием выбора и hover-подсветкой. */
    private static class NavButton extends JButton {
        private boolean selectedNav = false;
        private boolean hovered = false;

        NavButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setHorizontalAlignment(LEFT);
            setFont(Theme.body(15f, false));
            setForeground(Theme.TEXT_MUTED);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            setBorder(new EmptyBorder(0, 12, 0, 12));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        void setActive(boolean selected) {
            this.selectedNav = selected;
            setForeground(selected ? Theme.TEXT : Theme.TEXT_MUTED);
            setFont(Theme.body(15f, selected));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (selectedNav || hovered) {
                g2.setColor(selectedNav ? new Color(255, 255, 255, 26) : new Color(255, 255, 255, 13));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            }
            if (selectedNav) {
                g2.setColor(Theme.ACCENT);
                g2.fillRoundRect(0, 8, 3, getHeight() - 16, 3, 3);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ====================== СТАТУС-БАР ======================

    private JPanel createBottomBar() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(4, 20, 16, 20));

        statusLabel = new JLabel("Готов к запуску");
        statusLabel.setFont(Theme.body(13f, false));
        statusLabel.setForeground(Theme.TEXT_MUTED);
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(900, 18));
        progressBar.setForeground(Theme.ACCENT);

        bottomPanel.add(statusLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        bottomPanel.add(progressBar);
        return bottomPanel;
    }

    // ====================== API ДЛЯ СЕРВИСОВ И ЭКРАНОВ ======================

    @Override
    public void onStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    @Override
    public void onProgress(int value) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(value));
    }

    public NotificationManager getNotifications() {
        return notifications;
    }

    /** Вызывается экраном настроек после сохранения — обновляем зависимые виджеты. */
    public void onSettingsSaved() {
        homeScreen.refreshServerStatus();
    }
}
