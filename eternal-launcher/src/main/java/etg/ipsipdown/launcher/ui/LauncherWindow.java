package etg.ipsipdown.launcher.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import etg.ipsipdown.launcher.core.UpdateCoordinator;

public class LauncherWindow extends JFrame {

    private Font minecraftFont;
    private Image backgroundImage;

    // Элементы для управления извне
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JButton playButton;

    public LauncherWindow() {
        loadResources();

        setTitle("EternalSky");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Главная панель, которая рисует фон на всё окно
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g.setColor(new Color(30, 30, 30));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                // Легкое затемнение фона
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        // --- ВЕРХНЯЯ ЧАСТЬ (3 Колонки) ---
        JPanel columnsPanel = new JPanel(new GridLayout(1, 3));
        columnsPanel.setOpaque(false);

        MatteBorder separator = BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0, 0, 0, 150));

        // 1. Левая колонка (Новости)
        JPanel leftColumn = createColumnPanel();
        leftColumn.setBorder(BorderFactory.createCompoundBorder(separator, new EmptyBorder(20, 20, 20, 20)));
        buildSideColumn(leftColumn, "Новости", "<html>(типо обновы сборки,<br>уведы о стримах и т.д)</html>");

        // 2. Центральная колонка (Логотип и кнопка)
        JPanel centerColumn = new JPanel(new GridBagLayout()); // GridBag идеально центрирует
        centerColumn.setOpaque(false);
        centerColumn.setBorder(separator);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(10, 0, 10, 0); // Отступы между элементами по вертикали

        centerColumn.add(createLabel("EternalSky", 40, true), gbc);
        centerColumn.add(createLabel("Launcher", 28, true), gbc);

        // Добавляем пружину, чтобы оттолкнуть кнопку чуть ниже логотипа
        centerColumn.add(Box.createRigidArea(new Dimension(0, 50)), gbc);

        playButton = new MinecraftButton("Играть");
        if (minecraftFont != null) playButton.setFont(minecraftFont.deriveFont(Font.BOLD, 28f));
        centerColumn.add(playButton, gbc);
        playButton.addActionListener(e -> {
            setButtonEnabled(false); // Блокируем кнопку, чтобы игрок не нажал её 10 раз подряд
            new UpdateCoordinator(this).startUpdateProcess(); // Запускаем цепочку обновлений
        });

        // 3. Правая колонка (Настройки)
        JPanel rightColumn = createColumnPanel();
        rightColumn.setBorder(new EmptyBorder(20, 20, 20, 20));
        buildSideColumn(rightColumn, "Настройки", "<html>(ОЗУ, разрешение<br>и т.д.)</html>");

        columnsPanel.add(leftColumn);
        columnsPanel.add(centerColumn);
        columnsPanel.add(rightColumn);

        // --- НИЖНЯЯ ЧАСТЬ (Прогресс-бар и статус) ---
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        statusLabel = createLabel("Готов к запуску", 16, false);
        statusLabel.setForeground(new Color(200, 200, 200)); // Чуть более серый цвет
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setMaximumSize(new Dimension(960, 20)); // Бар на всю ширину с отступами
        progressBar.setForeground(new Color(46, 204, 113)); // Приятный зеленый цвет загрузки

        bottomPanel.add(statusLabel);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        bottomPanel.add(progressBar);

        // Собираем всё в главное окно
        mainPanel.add(columnsPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // --- МЕТОДЫ ДЛЯ MAIN.JAVA ---
    public void setStatus(String text) { SwingUtilities.invokeLater(() -> statusLabel.setText(text)); }
    public void setProgress(int value) { SwingUtilities.invokeLater(() -> progressBar.setValue(value)); }
    public void setButtonEnabled(boolean enabled) { SwingUtilities.invokeLater(() -> playButton.setEnabled(enabled)); }
    public JButton getPlayButton() { return playButton; }

    // --- ВСПОМОГАТЕЛЬНАЯ ЛОГИКА ---

    private void buildSideColumn(JPanel column, String title, String contentText) {
        JLabel titleLabel = createLabel(title, 30, true);
        titleLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Кастомная панель с полупрозрачным фоном и закруглениями
        RoundedPanel contentPanel = new RoundedPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel content = createLabel(contentText, 18, false);
        content.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(content, BorderLayout.CENTER);

        // Логика сворачивания/разворачивания по клику на заголовок
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                contentPanel.setVisible(!contentPanel.isVisible());
                column.revalidate();
                column.repaint();
            }
        });

        column.add(titleLabel);
        column.add(Box.createRigidArea(new Dimension(0, 15)));
        column.add(contentPanel);
    }

    private JPanel createColumnPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
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
        } catch (Exception e) {
            System.err.println("Ошибка ресурсов: " + e.getMessage());
        }
    }

    private JLabel createLabel(String text, float size, boolean isBold) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        if (minecraftFont != null) {
            label.setFont(minecraftFont.deriveFont(isBold ? Font.BOLD : Font.PLAIN, size));
        } else {
            label.setFont(new Font("SansSerif", isBold ? Font.BOLD : Font.PLAIN, (int)size));
        }
        return label;
    }

    // --- КАСТОМНЫЕ КОМПОНЕНТЫ ---

    // Панель с закругленными краями и полупрозрачным черным фоном
    private class RoundedPanel extends JPanel {
        public RoundedPanel() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 140)); // 140 - степень прозрачности
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30); // 30 - радиус закругления
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // 3D Кнопка
    private class MinecraftButton extends JButton {
        private boolean isHovered = false;

        public MinecraftButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(240, 60));
            setMinimumSize(new Dimension(240, 60));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent evt) { isHovered = true; repaint(); }
                public void mouseExited(MouseEvent evt) { isHovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(isHovered ? new Color(139, 139, 139) : new Color(110, 110, 110));
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setColor(new Color(180, 180, 180));
            g2.fillRect(0, 0, getWidth(), 4);
            g2.fillRect(0, 0, 4, getHeight());

            g2.setColor(new Color(60, 60, 60));
            g2.fillRect(0, getHeight() - 4, getWidth(), 4);
            g2.fillRect(getWidth() - 4, 0, 4, getHeight());

            super.paintComponent(g);
        }
    }
}