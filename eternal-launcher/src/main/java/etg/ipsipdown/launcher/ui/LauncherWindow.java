package etg.ipsipdown.launcher.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import etg.ipsipdown.launcher.core.UpdateCoordinator;

public class LauncherWindow extends JFrame {

    private Font minecraftFont;
    private Image backgroundImage;

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
                g.setColor(new Color(0, 0, 0, 100));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(10, 0, 10, 0);

        centerPanel.add(createLabel("EternalSky", 60, true), gbc);
        centerPanel.add(createLabel("Launcher", 32, true), gbc);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 40)), gbc);

        
        playButton = new MinecraftButton("Играть");
        if (minecraftFont != null) playButton.setFont(minecraftFont.deriveFont(Font.BOLD, 28f));
        centerPanel.add(playButton, gbc);

        playButton.addActionListener(e -> {
            setButtonEnabled(false);
            new UpdateCoordinator(this).startUpdateProcess();
        });

        
        JButton addModButton = new JButton("Добавить свой мод...");
        addModButton.setContentAreaFilled(false);
        addModButton.setBorderPainted(false);
        addModButton.setFocusPainted(false);
        addModButton.setForeground(new Color(180, 180, 180)); 
        addModButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        if (minecraftFont != null) addModButton.setFont(minecraftFont.deriveFont(Font.PLAIN, 16f));

        
        addModButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) { addModButton.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent evt) { addModButton.setForeground(new Color(180, 180, 180)); }
        });

        
        addModButton.addActionListener(e -> handleAddCustomMod());

        
        gbc.insets = new Insets(0, 0, 0, 0);
        centerPanel.add(addModButton, gbc);
        

        
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

        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    
    private void handleAddCustomMod() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Выберите файлы модов (.jar)");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Java Archive (*.jar)", "jar"));

        
        fileChooser.setMultiSelectionEnabled(true);

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            
            File[] selectedFiles = fileChooser.getSelectedFiles();

            try {
                Path gameDir = Paths.get(System.getenv("APPDATA"), ".eternalsky");
                Path modsDir = gameDir.resolve("mods");
                if (!Files.exists(modsDir)) Files.createDirectories(modsDir);

                Path whitelist = gameDir.resolve("custom_mods.txt");

                
                for (File selectedFile : selectedFiles) {
                    Path targetFile = modsDir.resolve(selectedFile.getName());

                    
                    Files.copy(selectedFile.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);

                    
                    Files.writeString(whitelist, selectedFile.getName() + System.lineSeparator(),
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                }

                
                JOptionPane.showMessageDialog(this,
                        "Успешно добавлено модов: " + selectedFiles.length + " шт.!",
                        "Успех", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Ошибка при копировании модов: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    

    public void setStatus(String text) { SwingUtilities.invokeLater(() -> statusLabel.setText(text)); }
    public void setProgress(int value) { SwingUtilities.invokeLater(() -> progressBar.setValue(value)); }
    public void setButtonEnabled(boolean enabled) { SwingUtilities.invokeLater(() -> playButton.setEnabled(enabled)); }
    public JButton getPlayButton() { return playButton; }

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

    private class MinecraftButton extends JButton {
        private boolean isHovered = false;

        public MinecraftButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setPreferredSize(new Dimension(240, 60));
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
}