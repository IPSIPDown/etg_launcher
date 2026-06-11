package etg.ipsipdown.launcher.ui.screens;

import etg.ipsipdown.launcher.ui.components.GhostButton;
import etg.ipsipdown.launcher.ui.components.RoundedPanel;
import etg.ipsipdown.launcher.ui.theme.Theme;
import etg.ipsipdown.launcher.utils.OsPaths;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Просмотр логов прямо в лаунчере: launcher.log / update.log / errors.log /
 * latest.log самой игры. Обновляется автоматически, пока экран открыт.
 */
public class LogsScreen extends JPanel {

    private static final int TAIL_LINES = 400;

    private final JComboBox<String> fileSelector;
    private final JTextArea logArea;
    private final Timer refreshTimer;
    private String lastContent = "";

    public LogsScreen() {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(18, 24, 10, 24));

        // --- Шапка ---
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Логи");
        title.setFont(Theme.title(28f, true));
        title.setForeground(Theme.TEXT);
        top.add(title, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controls.setOpaque(false);

        fileSelector = new JComboBox<>(new String[]{"launcher.log", "update.log", "errors.log", "latest.log (Minecraft)"});
        fileSelector.setFont(Theme.body(13f, false));
        fileSelector.addActionListener(e -> refresh(true));

        GhostButton refreshBtn = new GhostButton("⟳ Обновить", 14f);
        refreshBtn.addActionListener(e -> refresh(true));

        controls.add(fileSelector);
        controls.add(refreshBtn);
        top.add(controls, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        // --- Текст лога ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setOpaque(false);
        logArea.setForeground(new Color(200, 208, 216));
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setLineWrap(false);

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 12, 10, 12));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        card.add(scrollPane, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(14, 0, 0, 0));
        wrapper.add(card, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);

        // Автообновление, только пока экран реально показан
        refreshTimer = new Timer(2000, e -> {
            if (isShowing()) refresh(false);
        });
        refreshTimer.start();

        refresh(true);
    }

    private Path selectedFile() {
        int idx = fileSelector.getSelectedIndex();
        return switch (idx) {
            case 1 -> OsPaths.LOGS_DIR.resolve("update.log");
            case 2 -> OsPaths.LOGS_DIR.resolve("errors.log");
            case 3 -> OsPaths.LOGS_DIR.resolve("latest.log");
            default -> OsPaths.LOGS_DIR.resolve("launcher.log");
        };
    }

    private void refresh(boolean force) {
        Path file = selectedFile();
        String content;
        try {
            if (Files.exists(file)) {
                List<String> lines = Files.readAllLines(file);
                int from = Math.max(0, lines.size() - TAIL_LINES);
                content = String.join("\n", lines.subList(from, lines.size()));
            } else {
                content = "Файл ещё не создан: " + file;
            }
        } catch (Exception e) {
            content = "Не удалось прочитать лог: " + e.getMessage();
        }

        if (!force && content.equals(lastContent)) return;
        lastContent = content;

        boolean wasAtBottom = isScrolledToBottom();
        logArea.setText(content);
        if (force || wasAtBottom) {
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    private boolean isScrolledToBottom() {
        javax.swing.JScrollBar bar = ((JScrollPane) logArea.getParent().getParent()).getVerticalScrollBar();
        return bar.getValue() + bar.getVisibleAmount() >= bar.getMaximum() - 30;
    }
}
