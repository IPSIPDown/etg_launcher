package etg.ipsipdown.launcher.ui;

import etg.ipsipdown.launcher.core.LauncherSettings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SettingsPanel extends JPanel {

    private final LauncherWindow mainWindow;
    private final LauncherSettings settings;

    private final CardLayout subCardLayout;
    private final JPanel subContentPanel;


    private JSlider ramSlider;
    private JLabel ramValueLabel;
    private JCheckBox autoConnectCheck;
    private JTextField widthField;
    private JTextField heightField;
    private JCheckBox fullScreenCheck;
    private JTextField jvmArgsField;
    private JTextField javaPathField;
    private JTextField ipField;   // Убедись, что это здесь есть!
    private JTextField portField; // И это тоже!

    public SettingsPanel(LauncherWindow mainWindow) {
        this.mainWindow = mainWindow;
        this.settings = LauncherSettings.load();

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(30, 40, 30, 40));

        // --- ВЕРХНЯЯ ЧАСТЬ ---
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("НАСТРОЙКИ");
        titleLabel.setForeground(Color.WHITE);
        applyFont(titleLabel, 28f, Font.BOLD);

        JButton backButton = createMenuButton("<- Назад", 16f);
        backButton.addActionListener(e -> {
            saveAllSettings();
            mainWindow.showScreen("MAIN");
        });

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(backButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- ЦЕНТРАЛЬНАЯ ЧАСТЬ ---
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(30, 0, 0, 0));

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(150, 0));

        JButton gameSettingsBtn = createMenuButton("Игра", 18f);
        JButton buildSettingsBtn = createMenuButton("Сборка", 18f);

        sidebar.add(gameSettingsBtn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));
        sidebar.add(buildSettingsBtn);

        subCardLayout = new CardLayout();
        subContentPanel = new JPanel(subCardLayout);
        subContentPanel.setOpaque(false);
        subContentPanel.setBorder(new EmptyBorder(0, 10, 0, 0));

        subContentPanel.add(createGameSettingsPanel(), "GAME");
        subContentPanel.add(createBuildSettingsPanel(), "BUILD");

        gameSettingsBtn.addActionListener(e -> subCardLayout.show(subContentPanel, "GAME"));
        buildSettingsBtn.addActionListener(e -> subCardLayout.show(subContentPanel, "BUILD"));

        centerPanel.add(sidebar, BorderLayout.WEST);
        centerPanel.add(subContentPanel, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createGameSettingsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);

        // 1. ОЗУ
        panel.add(createFieldLabel("Выделение оперативной памяти:"));
        ramSlider = new JSlider(2048, 16384, settings.ramMegabytes);
        ramSlider.setOpaque(false);
        ramSlider.setSnapToTicks(true);
        ramSlider.setMajorTickSpacing(1024);

        ramValueLabel = new JLabel((settings.ramMegabytes / 1024) + " ГБ");
        ramValueLabel.setForeground(new Color(46, 204, 113));
        applyFont(ramValueLabel, 16f, Font.BOLD);

        ramSlider.addChangeListener(e -> {
            int gb = ramSlider.getValue() / 1024;
            ramValueLabel.setText(gb + " ГБ");
        });

        JPanel ramPanel = new JPanel(new BorderLayout());
        ramPanel.setOpaque(false);
        ramPanel.add(ramSlider, BorderLayout.CENTER);
        ramPanel.add(ramValueLabel, BorderLayout.EAST);
        panel.add(ramPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 2. Автоподключение
        autoConnectCheck = new JCheckBox("Автоматическое подключение к серверу", settings.autoConnect);
        autoConnectCheck.setOpaque(false);
        autoConnectCheck.setForeground(Color.WHITE);
        autoConnectCheck.setFocusPainted(false);
        applyFont(autoConnectCheck, 14f, Font.PLAIN);
        panel.add(autoConnectCheck);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 3. Разрешение
        panel.add(createFieldLabel("Разрешение экрана (Ширина x Высота):"));
        JPanel resPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        resPanel.setOpaque(false);

        widthField = new JTextField(String.valueOf(settings.windowWidth), 5);
        heightField = new JTextField(String.valueOf(settings.windowHeight), 5);
        applyFont(widthField, 14f, Font.PLAIN);
        applyFont(heightField, 14f, Font.PLAIN);

        fullScreenCheck = new JCheckBox("На весь экран", settings.isFullScreen);
        fullScreenCheck.setOpaque(false);
        fullScreenCheck.setForeground(Color.WHITE);
        fullScreenCheck.setFocusPainted(false);
        applyFont(fullScreenCheck, 14f, Font.PLAIN);

        JLabel xLabel = new JLabel("  x  ");
        xLabel.setForeground(Color.WHITE);
        applyFont(xLabel, 14f, Font.PLAIN);

        resPanel.add(widthField);
        resPanel.add(xLabel);
        resPanel.add(heightField);
        resPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        resPanel.add(fullScreenCheck);
        panel.add(resPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 4. JVM
        panel.add(createFieldLabel("Аргументы Java (Оптимизация G1GC):"));
        jvmArgsField = new JTextField(settings.jvmArgs);
        jvmArgsField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        applyFont(jvmArgsField, 12f, Font.PLAIN);
        panel.add(jvmArgsField);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 5. Путь к Java
        panel.add(createFieldLabel("Свой путь к Java (Оставьте пустым по умолчанию):"));
        javaPathField = new JTextField(settings.customJavaPath);
        javaPathField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        applyFont(javaPathField, 12f, Font.PLAIN);
        panel.add(javaPathField);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        // 6. IP сервера (ИСПОЛЬЗУЕМ ПЕРЕМЕННЫЕ КЛАССА)
        panel.add(createFieldLabel("IP сервера:"));
        ipField = new JTextField(settings.serverIp);
        ipField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        applyFont(ipField, 14f, Font.PLAIN);
        panel.add(ipField);

        // 7. Порт сервера (ИСПОЛЬЗУЕМ ПЕРЕМЕННЫЕ КЛАССА)
        panel.add(createFieldLabel("Порт сервера:"));
        portField = new JTextField(String.valueOf(settings.serverPort));
        portField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        applyFont(portField, 14f, Font.PLAIN);
        panel.add(portField);

        return panel;
    }

    private JPanel createBuildSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // --- ВЕРХНЯЯ ПАНЕЛЬ С КНОПКОЙ ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setOpaque(false);

        JButton addModBtn = new JButton("+ Добавить мод");
        addModBtn.setFocusPainted(false);
        addModBtn.setContentAreaFilled(false);
        addModBtn.setForeground(new Color(46, 204, 113)); // Ярко-зеленый
        addModBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyFont(addModBtn, 16f, Font.BOLD);

        // Логика добавления файла
        addModBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Minecraft Mods (.jar)", "jar"));
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                java.io.File selectedFile = fileChooser.getSelectedFile();
                try {
                    // Путь к папке модов (убедись, что он совпадает с тем, что ниже!)
                    java.nio.file.Path target = java.nio.file.Paths.get(System.getenv("APPDATA"), ".minecraft", "mods", selectedFile.getName());
                    java.nio.file.Files.copy(selectedFile.toPath(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                    // Перезагружаем интерфейс, чтобы новый мод сразу появился в списке
                    mainWindow.showScreen("SETTINGS");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка при копировании: " + ex.getMessage());
                }
            }
        });

        topPanel.add(addModBtn);
        panel.add(topPanel, BorderLayout.NORTH);

        // --- СПИСОК МОДОВ ---
        JPanel listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);

        // Вот здесь секрет: явно говорим, на сколько пикселей сдвигать при повороте колесика
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);

        // Включаем отображение полосы прокрутки, если контент не влезает
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Если полоса прокрутки всё равно выглядит уродливо (стандартная серая),
        // можно скрыть её, но настроить жесткую привязку:
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));

        panel.add(scrollPane, BorderLayout.CENTER);

        // Путь к папке модов
        String appData = System.getenv("APPDATA");
        java.nio.file.Path modsDir = java.nio.file.Paths.get(appData, ".minecraft", "mods");

        JLabel loadingLabel = new JLabel("Загрузка модов...", SwingConstants.CENTER);
        loadingLabel.setForeground(Color.LIGHT_GRAY);
        applyFont(loadingLabel, 14f, Font.PLAIN);
        listContainer.add(loadingLabel);

        // Асинхронная загрузка
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            java.util.List<etg.ipsipdown.launcher.core.LocalModReader.ModInfo> mods =
                    etg.ipsipdown.launcher.core.LocalModReader.getInstalledMods(modsDir);

            SwingUtilities.invokeLater(() -> {
                listContainer.removeAll();
                if (mods.isEmpty()) {
                    JLabel empty = new JLabel("Папка модов пуста.", SwingConstants.CENTER);
                    empty.setForeground(Color.LIGHT_GRAY);
                    applyFont(empty, 14f, Font.PLAIN);
                    listContainer.add(empty);
                } else {
                    for (etg.ipsipdown.launcher.core.LocalModReader.ModInfo mod : mods) {
                        listContainer.add(createModPanel(mod));
                        listContainer.add(Box.createRigidArea(new Dimension(0, 5)));
                    }
                }
                listContainer.revalidate();
                listContainer.repaint();
            });
        });

        return panel;
    }

    // Создание красивой плашки для одного мода
    private JPanel createModPanel(etg.ipsipdown.launcher.core.LocalModReader.ModInfo mod) {
        JPanel modPanel = new JPanel(new BorderLayout(15, 0));
        modPanel.setOpaque(false);
        // Жестко фиксируем размер, чтобы скролл работал плавно
        modPanel.setPreferredSize(new Dimension(750, 60));
        modPanel.setMinimumSize(new Dimension(750, 60));
        modPanel.setMaximumSize(new Dimension(750, 60));
        modPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 30)));

        // 1. Иконка мода
        JLabel iconLabel = new JLabel();
        if (mod.icon != null) {
            Image scaled = mod.icon.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaled));
        } else {
            iconLabel.setPreferredSize(new Dimension(40, 40));
            iconLabel.setOpaque(true);
            iconLabel.setBackground(new Color(60, 60, 60));
        }
        modPanel.add(iconLabel, BorderLayout.WEST);

        // 2. Текст (Имя + Версия/Тип)
        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(mod.displayName);
        nameLabel.setForeground(Color.WHITE);
        applyFont(nameLabel, 16f, Font.BOLD);

        // Объединяем Тип и Версию
        String modType = (mod.sideType != null) ? mod.sideType : "Общий";
        JLabel descLabel = new JLabel("Тип: " + modType + " | Версия: " + mod.version);
        descLabel.setForeground(new Color(150, 150, 150));
        applyFont(descLabel, 12f, Font.PLAIN);

        textPanel.add(nameLabel);
        textPanel.add(descLabel);
        modPanel.add(textPanel, BorderLayout.CENTER);

        // 3. Кнопка Вкл/Выкл
        JCheckBox toggle = new JCheckBox(mod.isEnabled ? "Включен" : "Выключен", mod.isEnabled);
        toggle.setOpaque(false);
        toggle.setForeground(mod.isEnabled ? new Color(46, 204, 113) : new Color(200, 50, 50));
        applyFont(toggle, 14f, Font.BOLD);
        toggle.setFocusPainted(false);

        toggle.addActionListener(e -> {
            boolean isNowEnabled = toggle.isSelected();
            try {
                String newName = isNowEnabled ? mod.fileName.replace(".disabled", "") : mod.fileName + ".disabled";
                java.nio.file.Path newPath = mod.filePath.resolveSibling(newName);
                java.nio.file.Files.move(mod.filePath, newPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                mod.filePath = newPath;
                mod.fileName = newName;
                mod.isEnabled = isNowEnabled;
                toggle.setText(isNowEnabled ? "Включен" : "Выключен");
                toggle.setForeground(isNowEnabled ? new Color(46, 204, 113) : new Color(200, 50, 50));
            } catch (Exception ex) {
                toggle.setSelected(!isNowEnabled);
            }
        });

        modPanel.add(toggle, BorderLayout.EAST);
        return modPanel;
    }

    private void saveAllSettings() {
        // 1. Сохраняем основные настройки
        settings.ramMegabytes = ramSlider.getValue();
        settings.autoConnect = autoConnectCheck.isSelected();
        settings.isFullScreen = fullScreenCheck.isSelected();
        settings.jvmArgs = jvmArgsField.getText().trim();
        settings.customJavaPath = javaPathField.getText().trim();

        // 2. Сохраняем IP и порт сервера
        settings.serverIp = ipField.getText().trim();
        try {
            settings.serverPort = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {
            // Если в порте написано что-то кроме цифр, оставляем старое значение
        }

        // 3. Сохраняем разрешение экрана
        try {
            settings.windowWidth = Integer.parseInt(widthField.getText().trim());
            settings.windowHeight = Integer.parseInt(heightField.getText().trim());
        } catch (NumberFormatException ignored) {
            // Если в разрешении цифры не распознаны, оставляем как есть
        }

        // 4. Финальное сохранение в файл
        settings.save();
    }

    private JLabel createFieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.LIGHT_GRAY);
        l.setBorder(new EmptyBorder(5, 0, 5, 0));
        applyFont(l, 14f, Font.PLAIN);
        return l;
    }

    private JButton createMenuButton(String text, float fontSize) {
        JButton btn = new JButton(text);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setForeground(new Color(180, 180, 180));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        applyFont(btn, fontSize, Font.PLAIN);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent evt) { btn.setForeground(new Color(180, 180, 180)); }
        });
        return btn;
    }

    // Вспомогательный метод для удобного применения шрифта
    private void applyFont(JComponent comp, float size, int style) {
        if (mainWindow.getMinecraftFont() != null) {
            comp.setFont(mainWindow.getMinecraftFont().deriveFont(style, size));
        } else {
            comp.setFont(new Font("SansSerif", style, (int) size));
        }
    }
}