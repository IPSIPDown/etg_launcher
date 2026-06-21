package etg.ipsipdown.launcher.ui.screens;

import etg.ipsipdown.launcher.models.ModInfo;
import etg.ipsipdown.launcher.services.LocalModReader;
import etg.ipsipdown.launcher.ui.LauncherWindow;
import etg.ipsipdown.launcher.ui.components.GhostButton;
import etg.ipsipdown.launcher.ui.components.RoundedPanel;
import etg.ipsipdown.launcher.ui.theme.Theme;
import etg.ipsipdown.launcher.utils.OsPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Экран «Сборка»: список установленных модов, вкл/выкл, добавление своих, поиск по названию.
 */
public class ModsScreen extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(ModsScreen.class);

    private final LauncherWindow window;
    private final JPanel listContainer;
    private JTextField searchField;

    private List<ModInfo> allMods = List.of();

    public ModsScreen(LauncherWindow window) {
        this.window = window;
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 24, 10, 24));

        // --- Шапка (заголовок + кнопки + поиск) ---
        JPanel northArea = new JPanel(new BorderLayout(0, 10));
        northArea.setOpaque(false);
        northArea.add(createTopBar(), BorderLayout.NORTH);
        northArea.add(createSearchBar(), BorderLayout.SOUTH);
        add(northArea, BorderLayout.NORTH);

        // --- Список модов ---
        listContainer = new JPanel();
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(listContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(new EmptyBorder(10, 0, 0, 0));
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        scrollPane.getVerticalScrollBar().setBlockIncrement(100);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        add(scrollPane, BorderLayout.CENTER);

        refresh();
    }

    private JPanel createTopBar() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Сборка");
        title.setFont(Theme.title(28f, true));
        title.setForeground(Theme.TEXT);
        top.add(title, BorderLayout.WEST);

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topButtons.setOpaque(false);

        GhostButton addModBtn = new GhostButton("+ Добавить мод", 15f, Theme.ACCENT, Theme.ACCENT_HOVER);
        addModBtn.setFont(Theme.body(15f, true));
        addModBtn.addActionListener(e -> addMod());

        GhostButton refreshBtn = new GhostButton("⟳ Обновить", 15f);
        refreshBtn.addActionListener(e -> refresh());

        topButtons.add(refreshBtn);
        topButtons.add(addModBtn);
        top.add(topButtons, BorderLayout.EAST);
        return top;
    }

    private JPanel createSearchBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setOpaque(false);

        JLabel label = new JLabel("Поиск:");
        label.setForeground(Theme.TEXT_MUTED);
        label.setFont(Theme.body(13f, false));
        bar.add(label, BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        searchField.setFont(Theme.body(13f, false));
        searchField.putClientProperty("JTextField.placeholderText", "Название мода...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        bar.add(searchField, BorderLayout.CENTER);
        return bar;
    }

    public void refresh() {
        listContainer.removeAll();
        JLabel loadingLabel = new JLabel("Загрузка модов...", SwingConstants.CENTER);
        loadingLabel.setForeground(Theme.TEXT_MUTED);
        loadingLabel.setFont(Theme.body(14f, false));
        listContainer.add(loadingLabel);
        listContainer.revalidate();
        listContainer.repaint();

        CompletableFuture.runAsync(() -> {
            List<ModInfo> mods = LocalModReader.getInstalledMods(OsPaths.MODS_DIR);
            SwingUtilities.invokeLater(() -> {
                allMods = mods;
                applyFilter();
            });
        });
    }

    private void applyFilter() {
        String query = searchField != null ? searchField.getText().toLowerCase(Locale.ROOT).trim() : "";

        List<ModInfo> filtered = query.isEmpty()
                ? allMods
                : allMods.stream()
                        .filter(m -> m.displayName.toLowerCase(Locale.ROOT).contains(query)
                                || m.fileName.toLowerCase(Locale.ROOT).contains(query))
                        .collect(Collectors.toList());

        listContainer.removeAll();

        if (allMods.isEmpty()) {
            JLabel empty = new JLabel("Папка модов пуста. Нажми «Играть» — сборка скачается автоматически.", SwingConstants.CENTER);
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setFont(Theme.body(14f, false));
            listContainer.add(empty);
        } else if (filtered.isEmpty()) {
            JLabel none = new JLabel("Ничего не найдено по запросу «" + query + "»", SwingConstants.CENTER);
            none.setForeground(Theme.TEXT_MUTED);
            none.setFont(Theme.body(14f, false));
            listContainer.add(none);
        } else {
            for (ModInfo mod : filtered) {
                listContainer.add(createModPanel(mod));
                listContainer.add(Box.createRigidArea(new Dimension(0, 6)));
            }
        }

        listContainer.revalidate();
        listContainer.repaint();
    }

    private void addMod() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Minecraft Mods (.jar)", "jar"));
        if (fileChooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File selectedFile = fileChooser.getSelectedFile();
        try {
            Files.createDirectories(OsPaths.MODS_DIR);
            Path target = OsPaths.MODS_DIR.resolve(selectedFile.getName());
            Files.copy(selectedFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            addToWhitelist(selectedFile.getName());
            window.getNotifications().success("Мод добавлен: " + selectedFile.getName());
            refresh();
        } catch (Exception ex) {
            log.error("Ошибка при добавлении мода", ex);
            window.getNotifications().error("Ошибка при копировании: " + ex.getMessage());
        }
    }

    private void addToWhitelist(String fileName) throws Exception {
        Path whitelist = OsPaths.CUSTOM_MODS_WHITELIST;
        if (Files.exists(whitelist) && Files.readAllLines(whitelist).contains(fileName)) return;
        Files.writeString(whitelist, fileName + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private JPanel createModPanel(ModInfo mod) {
        RoundedPanel modPanel = new RoundedPanel(new BorderLayout(15, 0), 10);
        modPanel.setBorder(new EmptyBorder(8, 12, 8, 12));
        modPanel.setPreferredSize(new Dimension(750, 60));
        modPanel.setMinimumSize(new Dimension(200, 60));
        modPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        modPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel iconLabel = new JLabel();
        if (mod.icon != null) {
            Image scaled = mod.icon.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(scaled));
        } else {
            iconLabel.setPreferredSize(new Dimension(40, 40));
            iconLabel.setOpaque(true);
            iconLabel.setBackground(new java.awt.Color(60, 60, 60));
        }
        modPanel.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new java.awt.GridLayout(2, 1));
        textPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(mod.displayName);
        nameLabel.setForeground(Theme.TEXT);
        nameLabel.setFont(Theme.body(15f, true));

        String modType = (mod.sideType != null) ? mod.sideType : "Общий";
        JLabel descLabel = new JLabel("Тип: " + modType + " | Версия: " + mod.version);
        descLabel.setForeground(Theme.TEXT_MUTED);
        descLabel.setFont(Theme.body(12f, false));

        textPanel.add(nameLabel);
        textPanel.add(descLabel);
        modPanel.add(textPanel, BorderLayout.CENTER);

        JCheckBox toggle = new JCheckBox(mod.isEnabled ? "Включен" : "Выключен", mod.isEnabled);
        toggle.setOpaque(false);
        toggle.setForeground(mod.isEnabled ? Theme.ACCENT : Theme.RED);
        toggle.setFont(Theme.body(13f, true));
        toggle.setFocusPainted(false);

        toggle.addActionListener(e -> {
            boolean isNowEnabled = toggle.isSelected();
            try {
                String newName = isNowEnabled ? mod.fileName.replace(".disabled", "") : mod.fileName + ".disabled";
                Path newPath = mod.filePath.resolveSibling(newName);
                Files.move(mod.filePath, newPath, StandardCopyOption.REPLACE_EXISTING);
                mod.filePath = newPath;
                mod.fileName = newName;
                mod.isEnabled = isNowEnabled;
                toggle.setText(isNowEnabled ? "Включен" : "Выключен");
                toggle.setForeground(isNowEnabled ? Theme.ACCENT : Theme.RED);
            } catch (Exception ex) {
                log.warn("Не удалось переключить мод {}: {}", mod.fileName, ex.getMessage());
                toggle.setSelected(!isNowEnabled);
                window.getNotifications().warning("Не удалось переключить мод");
            }
        });

        modPanel.add(toggle, BorderLayout.EAST);
        return modPanel;
    }
}
