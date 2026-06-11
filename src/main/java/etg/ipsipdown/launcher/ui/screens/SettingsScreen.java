package etg.ipsipdown.launcher.ui.screens;

import etg.ipsipdown.launcher.models.LauncherSettings;
import etg.ipsipdown.launcher.services.SelfUpdateService;
import etg.ipsipdown.launcher.ui.LauncherWindow;
import etg.ipsipdown.launcher.ui.components.GhostButton;
import etg.ipsipdown.launcher.ui.components.RoundedPanel;
import etg.ipsipdown.launcher.ui.theme.Theme;
import etg.ipsipdown.launcher.utils.OsPaths;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Настройки, разбитые по категориям: General / Minecraft / Java / Launcher / Advanced.
 */
public class SettingsScreen extends JPanel {

    private static final String DEFAULT_JVM_ARGS =
            "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M";

    private final LauncherWindow window;
    private final LauncherSettings settings;

    private final CardLayout categoryLayout = new CardLayout();
    private final JPanel categoryContainer = new JPanel(categoryLayout);
    private final Map<String, JButton> categoryButtons = new LinkedHashMap<>();

    // Поля
    private JSlider ramSlider;
    private JLabel ramValueLabel;
    private JTextField ipField;
    private JTextField portField;
    private JTextField javaPathField;
    private JTextField jvmArgsField;

    public SettingsScreen(LauncherWindow window) {
        this.window = window;
        this.settings = LauncherSettings.load();

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 24, 10, 24));

        // --- Шапка ---
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Настройки");
        title.setFont(Theme.title(28f, true));
        title.setForeground(Theme.TEXT);
        top.add(title, BorderLayout.WEST);

        GhostButton saveBtn = new GhostButton("Сохранить", 15f, Theme.ACCENT, Theme.ACCENT_HOVER);
        saveBtn.setFont(Theme.body(15f, true));
        saveBtn.addActionListener(e -> {
            saveAll();
            window.getNotifications().success("Настройки сохранены");
        });
        top.add(saveBtn, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // --- Категории слева + контент ---
        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(16, 0, 0, 0));

        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setOpaque(false);
        sidebar.setPreferredSize(new Dimension(140, 0));

        categoryContainer.setOpaque(false);

        addCategory(sidebar, "General", createGeneralPanel());
        addCategory(sidebar, "Minecraft", createMinecraftPanel());
        addCategory(sidebar, "Java", createJavaPanel());
        addCategory(sidebar, "Launcher", createLauncherPanel());
        addCategory(sidebar, "Advanced", createAdvancedPanel());

        center.add(sidebar, BorderLayout.WEST);
        center.add(categoryContainer, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        selectCategory("General");
    }

    private void addCategory(JPanel sidebar, String name, JPanel panel) {
        RoundedPanel wrapper = new RoundedPanel(new BorderLayout());
        wrapper.setBorder(new EmptyBorder(16, 20, 16, 20));
        wrapper.add(panel, BorderLayout.NORTH);
        categoryContainer.add(wrapper, name);

        GhostButton btn = new GhostButton(name, 15f);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setHorizontalAlignment(JButton.LEFT);
        btn.addActionListener(e -> selectCategory(name));
        categoryButtons.put(name, btn);
        sidebar.add(btn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    private void selectCategory(String name) {
        categoryLayout.show(categoryContainer, name);
        categoryButtons.forEach((n, b) -> {
            b.setForeground(n.equals(name) ? Theme.ACCENT : Theme.TEXT_MUTED);
            b.setFont(Theme.body(15f, n.equals(name)));
        });
    }

    // ---------- General ----------
    private JPanel createGeneralPanel() {
        JPanel panel = verticalPanel();

        panel.add(fieldLabel("IP сервера (для статуса на главном экране):"));
        ipField = textField(settings.serverIp);
        panel.add(ipField);
        panel.add(gap());

        panel.add(fieldLabel("Порт сервера:"));
        portField = textField(String.valueOf(settings.serverPort));
        panel.add(portField);
        panel.add(gap());

        panel.add(hint("После сохранения статус сервера на главном экране обновится автоматически."));
        return panel;
    }

    // ---------- Minecraft ----------
    private JPanel createMinecraftPanel() {
        JPanel panel = verticalPanel();

        panel.add(fieldLabel("Выделение оперативной памяти:"));
        ramSlider = new JSlider(2048, 16384, settings.ramMegabytes);
        ramSlider.setOpaque(false);
        ramSlider.setSnapToTicks(true);
        ramSlider.setMajorTickSpacing(1024);

        ramValueLabel = new JLabel((settings.ramMegabytes / 1024) + " ГБ");
        ramValueLabel.setForeground(Theme.ACCENT);
        ramValueLabel.setFont(Theme.body(15f, true));

        ramSlider.addChangeListener(e -> ramValueLabel.setText((ramSlider.getValue() / 1024) + " ГБ"));

        JPanel ramPanel = new JPanel(new BorderLayout(10, 0));
        ramPanel.setOpaque(false);
        ramPanel.add(ramSlider, BorderLayout.CENTER);
        ramPanel.add(ramValueLabel, BorderLayout.EAST);
        ramPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        panel.add(ramPanel);
        panel.add(gap());

        panel.add(hint("Память применяется к профилю EternalSky в официальном лаунчере при запуске."));
        return panel;
    }

    // ---------- Java ----------
    private JPanel createJavaPanel() {
        JPanel panel = verticalPanel();

        panel.add(fieldLabel("Свой путь к Java (оставь пустым — лаунчер найдёт или скачает сам):"));
        javaPathField = textField(settings.customJavaPath);
        panel.add(javaPathField);
        panel.add(gap());

        panel.add(hint("Если Java не найдена, лаунчер автоматически скачает Temurin 21 в папку игры."));
        return panel;
    }

    // ---------- Launcher ----------
    private JPanel createLauncherPanel() {
        JPanel panel = verticalPanel();

        JLabel version = fieldLabel("Версия лаунчера: " + SelfUpdateService.CURRENT_VERSION);
        panel.add(version);
        panel.add(gap());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttons.setOpaque(false);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        GhostButton checkUpdates = new GhostButton("Проверить обновления", 14f, Theme.BLUE, Theme.TEXT);
        checkUpdates.addActionListener(e -> {
            window.getNotifications().info("Проверка обновлений...");
            SelfUpdateService.checkAsync();
        });

        GhostButton openLogs = new GhostButton("Папка логов", 14f);
        openLogs.addActionListener(e -> openFolder(OsPaths.LOGS_DIR));

        GhostButton openGame = new GhostButton("Папка игры", 14f);
        openGame.addActionListener(e -> openFolder(OsPaths.GAME_DIR));

        GhostButton verifyFiles = new GhostButton("Проверить файлы", 14f, Theme.ORANGE, Theme.TEXT);
        verifyFiles.setToolTipText("Сбрасывает кэш проверки — при следующем запуске все файлы сборки будут проверены заново");
        verifyFiles.addActionListener(e -> {
            etg.ipsipdown.launcher.services.SyncService.resetHashCache();
            window.getNotifications().success("Кэш сброшен. При следующем запуске все файлы будут проверены заново.");
        });

        buttons.add(checkUpdates);
        buttons.add(verifyFiles);
        buttons.add(openLogs);
        buttons.add(openGame);
        panel.add(buttons);
        panel.add(gap());

        panel.add(hint("Логи: launcher.log — лаунчер, update.log — обновления, errors.log — только ошибки."));
        return panel;
    }

    // ---------- Advanced ----------
    private JPanel createAdvancedPanel() {
        JPanel panel = verticalPanel();

        panel.add(fieldLabel("Аргументы JVM (оптимизация G1GC):"));
        jvmArgsField = textField(settings.jvmArgs);
        panel.add(jvmArgsField);
        panel.add(gap());

        GhostButton resetBtn = new GhostButton("Сбросить к рекомендуемым", 13f, Theme.ORANGE, Theme.TEXT);
        resetBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        resetBtn.addActionListener(e -> jvmArgsField.setText(DEFAULT_JVM_ARGS));
        panel.add(resetBtn);
        panel.add(gap());

        panel.add(hint("Меняй только если понимаешь, что делаешь. Память (-Xmx) задаётся в категории Minecraft."));
        return panel;
    }

    // ---------- Сохранение ----------
    private void saveAll() {
        settings.ramMegabytes = ramSlider.getValue();
        settings.jvmArgs = jvmArgsField.getText().trim();
        settings.customJavaPath = javaPathField.getText().trim();
        settings.serverIp = ipField.getText().trim();
        try {
            settings.serverPort = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {
            // Если в порте не число — оставляем старое значение
        }
        settings.save();
        window.onSettingsSaved();
    }

    private void openFolder(java.nio.file.Path dir) {
        try {
            Files.createDirectories(dir);
            Desktop.getDesktop().open(dir.toFile());
        } catch (Exception ex) {
            window.getNotifications().warning("Не удалось открыть папку");
        }
    }

    // ---------- Помощники ----------
    private JPanel verticalPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        return panel;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Theme.TEXT);
        l.setFont(Theme.body(14f, false));
        l.setBorder(new EmptyBorder(4, 0, 6, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel hint(String text) {
        JLabel l = new JLabel("<html>" + text + "</html>");
        l.setForeground(Theme.TEXT_DISABLED);
        l.setFont(Theme.body(12f, false));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField textField(String value) {
        JTextField f = new JTextField(value);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        f.setFont(Theme.body(13f, false));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private Component gap() {
        return Box.createRigidArea(new Dimension(0, 14));
    }
}
