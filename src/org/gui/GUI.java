package org.gui;

import org.CodeThiefPro;
import org.core.config.ScriptConfiguration;
import org.dreambot.api.methods.interactive.Players;
import org.gui.components.CustomButton;
import org.gui.components.ModernTabbedPane;
import org.gui.panels.AntiBanPanel;
import org.gui.panels.FoodPanel;
import org.gui.panels.MainSettingsPanel;
import org.gui.util.GUIUtils;
import org.gui.util.SettingsPersistence;
import org.gui.util.StyleFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class GUI extends JFrame {
    private final CodeThiefPro script;
    private final ScriptConfiguration config;

    private MainSettingsPanel mainSettingsPanel;
    private FoodPanel foodPanel;
    private AntiBanPanel antiBanPanel;
    private ModernTabbedPane tabbedPane;

    private String currentUsername;
    private SettingsPersistence settingsPersistence;

    public GUI(CodeThiefPro script, ScriptConfiguration config) {
        this.script = script;
        this.config = config;

        try {
            this.currentUsername = Players.getLocal().getName();
            if (this.currentUsername == null || this.currentUsername.isEmpty()) {
                this.currentUsername = "Unknown User";
            }
        } catch (Exception e) {
            this.currentUsername = "Unknown User";
            System.err.println("Error getting player name: " + e.getMessage());
        }

        // Initialize settings persistence
        try {
            this.settingsPersistence = new SettingsPersistence(currentUsername);
            System.out.println("Settings persistence initialized for user: " + currentUsername);

            // Load settings before initializing GUI components
            loadPersistedSettings();
        } catch (Exception e) {
            System.err.println("Error initializing settings persistence: " + e.getMessage());
            e.printStackTrace();
            // Continue with default settings
        }

        if (SwingUtilities.isEventDispatchThread()) {
            setupGUI();
        } else {
            try {
                SwingUtilities.invokeAndWait(this::setupGUI);
            } catch (Exception e) {
                System.err.println("Error in GUI initialization: " + e.getMessage());
            }
        }
    }

    private void setupGUI() {
        try {
            setTitle("CodeThief Pro Configuration");
            setSize(StyleFactory.MAIN_WINDOW_SIZE);
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout());
            setResizable(true);

            ((JComponent) getContentPane()).setBackground(StyleFactory.BG_DARK_COLOR);

            JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            contentPanel.setBackground(StyleFactory.BG_DARK_COLOR);

            JPanel headerPanel = createHeaderPanel();
            contentPanel.add(headerPanel, BorderLayout.NORTH);

            createTabbedPane();
            contentPanel.add(tabbedPane, BorderLayout.CENTER);

            JPanel buttonPanel = createButtonPanel();
            contentPanel.add(buttonPanel, BorderLayout.SOUTH);

            add(contentPanel);
            setLocationRelativeTo(null);
            GUIUtils.forceRender(this);
        } catch (Exception e) {
            System.err.println("Error in setupGUI: " + e.getMessage());
            e.printStackTrace();
            setupSimpleGUI();
        }
    }

    private void setupSimpleGUI() {
        try {
            getContentPane().removeAll();
            setTitle("CodeThief Pro - Simple Mode");
            setSize(400, 300);
            setLayout(new BorderLayout());

            JTabbedPane tabs = new JTabbedPane();

            mainSettingsPanel = new MainSettingsPanel(config);
            foodPanel = new FoodPanel(config);
            antiBanPanel = new AntiBanPanel(config, currentUsername);

            tabs.addTab("Main", mainSettingsPanel);
            tabs.addTab("Food", foodPanel);
            tabs.addTab("Anti-Ban", antiBanPanel);

            add(tabs, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout());
            JButton startButton = new JButton("Start Script");
            startButton.addActionListener(e -> saveAndClose());
            buttonPanel.add(startButton);

            add(buttonPanel, BorderLayout.SOUTH);
            setLocationRelativeTo(null);
        } catch (Exception e) {
            System.err.println("Error in fallback simple GUI: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Error creating GUI. Please report this bug.\n" + e.getMessage(),
                    "GUI Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(40, 20, 60),
                        w, h, new Color(20, 10, 30)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, w, h);

                g2d.setColor(StyleFactory.ACCENT_COLOR);
                g2d.fillRect(0, h - 2, w, 2);

                g2d.dispose();
            }
        };
        headerPanel.setPreferredSize(new Dimension(500, 60));
        headerPanel.setLayout(new BorderLayout());

        JLabel titleLabel = new JLabel("CodeThief Pro v1.2");
        titleLabel.setForeground(StyleFactory.TEXT_COLOR);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        JLabel subtitleLabel = new JLabel("Advanced Thieving Script");
        subtitleLabel.setForeground(StyleFactory.HEADER_COLOR);
        subtitleLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        return headerPanel;
    }

    private void createTabbedPane() {
        tabbedPane = new ModernTabbedPane();

        mainSettingsPanel = new MainSettingsPanel(config);
        foodPanel = new FoodPanel(config);
        antiBanPanel = new AntiBanPanel(config, currentUsername);

        // Apply loaded settings to UI components
        mainSettingsPanel.applySettings();
        foodPanel.applySettings();
        antiBanPanel.applySettings();

        JScrollPane mainScroll = new JScrollPane(mainSettingsPanel);
        JScrollPane foodScroll = new JScrollPane(foodPanel);
        JScrollPane antiBanScroll = new JScrollPane(antiBanPanel);

        configureScrollPane(mainScroll);
        configureScrollPane(foodScroll);
        configureScrollPane(antiBanScroll);

        tabbedPane.addTab("Main Settings", mainScroll, "Configure basic thieving settings");
        tabbedPane.addTab("Food & Health", foodScroll, "Configure food and health settings");
        tabbedPane.addTab("Anti-Ban", antiBanScroll, "Configure anti-ban behavior");

        tabbedPane.setSelectedIndex(0);
    }

    private void configureScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(StyleFactory.BG_DARK_COLOR);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(StyleFactory.BG_DARK_COLOR);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, StyleFactory.BORDER_COLOR));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        buttonRow.setBackground(StyleFactory.BG_DARK_COLOR);

        CustomButton helpButton = CustomButton.createHelpButton();
        helpButton.setToolTipText("Get help with script configuration");
        helpButton.addActionListener(e -> showHelpDialog());

        CustomButton startButton = CustomButton.createStartButton("Start Script");
        startButton.setPreferredSize(new Dimension(200, 40));
        startButton.addActionListener(e -> {
            try {
                saveAndClose();
            } catch (Exception ex) {
                System.err.println("Error during saveAndClose: " + ex.getMessage());
                ex.printStackTrace();

                // Ensure script can continue even if settings persistence fails
                JOptionPane.showMessageDialog(this,
                        "Error saving settings: " + ex.getMessage() + "\n" +
                                "The script will continue with current settings.",
                        "Warning",
                        JOptionPane.WARNING_MESSAGE);

                config.setConfigured(true);
                dispose();
            }
        });

        buttonRow.add(helpButton);
        buttonRow.add(startButton);
        panel.add(buttonRow, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(StyleFactory.BG_DARK_COLOR);

        JLabel versionLabel = new JLabel("v1.2.0");
        versionLabel.setFont(StyleFactory.SMALL_FONT);
        versionLabel.setForeground(new Color(120, 120, 120));
        versionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 10));

        statusPanel.add(versionLabel, BorderLayout.EAST);

        // Add status indicator
        JLabel statusLabel = new JLabel("Settings: " +
                (settingsPersistence != null ? "Ready" : "Warning"));
        statusLabel.setFont(StyleFactory.SMALL_FONT);
        statusLabel.setForeground(settingsPersistence != null ?
                new Color(0, 180, 0) : new Color(200, 100, 0));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 0));
        statusPanel.add(statusLabel, BorderLayout.WEST);

        panel.add(statusPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void showHelpDialog() {
        // Existing implementation...
    }

    private void saveAndClose() {
        try {
            List<String> validationErrors = validateSettings();

            if (!validationErrors.isEmpty()) {
                StringBuilder message = new StringBuilder("The following settings are missing or incomplete:\n\n");
                for (String error : validationErrors) {
                    message.append("• ").append(error).append("\n");
                }
                message.append("\nDo you want to continue anyway?");

                int response = JOptionPane.showConfirmDialog(this,
                        message.toString(),
                        "Missing Settings",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (response != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            mainSettingsPanel.saveSettings();
            foodPanel.saveSettings();
            antiBanPanel.saveSettings();

            savePersistedSettings();
            config.setConfigured(true);

            dispose();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error saving configuration: " + e.getMessage() + "\n" +
                            "The script will continue with default settings.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);

            System.err.println("Error saving configuration: " + e.getMessage());

            config.setConfigured(true);
            dispose();
        }
    }

    private List<String> validateSettings() {
        List<String> errors = new ArrayList<>();

        String mainError = mainSettingsPanel.validateSettings();
        if (mainError != null) {
            errors.add(mainError);
        }

        String foodError = foodPanel.validateSettings();
        if (foodError != null) {
            errors.add(foodError);
        }

        return errors;
    }

    private void savePersistedSettings() {
        java.util.Map<String, Object> settings = new java.util.HashMap<>();

        settings.put("currentTargetName", config.getCurrentTargetName());
        settings.put("autoProgressionEnabled", config.isAutoProgressionEnabled());
        settings.put("preferStalls", config.preferStalls());
        settings.put("freestyleMode", config.isFreestyleMode());
        settings.put("bankingEnabled", config.isBankingEnabled());
        settings.put("healthThreshold", config.getHealthThreshold());

        settings.put("antiBanEnabled", config.isAntiBanEnabled());
        settings.put("antiBanIntensity", config.getAntiBanIntensity());

        // Save food items
        settings.put("foodItems", config.getFoodItems());

        System.out.println("Saving settings to persistence for user: " + currentUsername);
        for (java.util.Map.Entry<String, Object> entry : settings.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }

        settingsPersistence.saveSettings(settings);
    }

    private void loadPersistedSettings() {
        java.util.Map<String, Object> settings = settingsPersistence.loadSettings();

        if (settings != null && !settings.isEmpty()) {
            System.out.println("Loading saved settings for user: " + currentUsername);
            for (java.util.Map.Entry<String, Object> entry : settings.entrySet()) {
                System.out.println("  " + entry.getKey() + " = " + entry.getValue());
            }

            if (settings.containsKey("currentTargetName")) {
                config.setCurrentTargetName((String) settings.get("currentTargetName"));
            }
            if (settings.containsKey("autoProgressionEnabled")) {
                config.setAutoProgressionEnabled((Boolean) settings.get("autoProgressionEnabled"));
            }
            if (settings.containsKey("preferStalls")) {
                config.setPreferStalls((Boolean) settings.get("preferStalls"));
            }
            if (settings.containsKey("freestyleMode")) {
                config.setFreestyleMode((Boolean) settings.get("freestyleMode"));
            }
            if (settings.containsKey("bankingEnabled")) {
                config.setBankingEnabled((Boolean) settings.get("bankingEnabled"));
            }
            if (settings.containsKey("healthThreshold")) {
                config.setHealthThreshold((Integer) settings.get("healthThreshold"));
            }
            if (settings.containsKey("antiBanEnabled")) {
                config.setAntiBanEnabled((Boolean) settings.get("antiBanEnabled"));
            }
            if (settings.containsKey("antiBanIntensity")) {
                config.setAntiBanIntensity((Integer) settings.get("antiBanIntensity"));
            }
            if (settings.containsKey("foodItems")) {
                config.setFoodItems((List<String>) settings.get("foodItems"));
            }
        } else {
            System.out.println("No saved settings found for user: " + currentUsername + ". Using defaults.");
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            if (tabbedPane != null) {
                tabbedPane.setSelectedIndex(0);
            }

            SwingUtilities.invokeLater(() -> {
                validate();
                repaint();

                Timer timer = new Timer(100, e -> {
                    validate();
                    repaint();
                });
                timer.setRepeats(false);
                timer.start();
            });
        }
    }
}