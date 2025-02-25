package org.gui;

import org.CodeThiefPro;
import org.ScriptConfiguration;
import org.dreambot.api.methods.interactive.Players;
import org.gui.panels.AdvancedPanel;
import org.gui.panels.AntiBanPanel;
import org.gui.panels.FoodPanel;
import org.gui.panels.MainSettingsPanel;
import org.gui.util.GUIUtils;
import org.gui.util.StyleFactory;
import org.pushingpixels.substance.api.skin.SubstanceBusinessLookAndFeel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced configuration GUI for the thieving script with improved styling, organization, and user experience.
 */
public class GUI extends JFrame {
    private final CodeThiefPro script;
    private final ScriptConfiguration config;

    // Panel components
    private MainSettingsPanel mainSettingsPanel;
    private FoodPanel foodPanel;
    private AntiBanPanel antiBanPanel;
    private AdvancedPanel advancedPanel;

    private JTabbedPane tabbedPane;

    // Current username for anti-ban profile
    private String currentUsername;

    /**
     * Creates a new enhanced GUI for the script
     *
     * @param script The parent script
     * @param config The configuration object
     */
    public GUI(CodeThiefPro script, ScriptConfiguration config) {
        this.script = script;
        this.config = config;

        // Get current username for anti-ban profile
        try {
            this.currentUsername = Players.getLocal().getName();
            if (this.currentUsername == null || this.currentUsername.isEmpty()) {
                this.currentUsername = "Unknown User";
            }
        } catch (Exception e) {
            this.currentUsername = "Unknown User";
            System.err.println("Error getting player name: " + e.getMessage());
        }

        // Initialize the GUI on the Event Dispatch Thread to avoid threading issues
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

    /**
     * Sets up the basic GUI frame and components
     */
    private void setupGUI() {
        setTitle("CodeThief Pro Configuration");
        setSize(StyleFactory.MAIN_WINDOW_SIZE);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(true);

        // Apply system look and feel with custom colors
        try {
            UIManager.setLookAndFeel(new SubstanceBusinessLookAndFeel());
        } catch (Exception e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        // Set panel background
        ((JComponent) getContentPane()).setBackground(StyleFactory.BG_DARK_COLOR);

        // Create header panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Create tabs and panels
        createTabbedPane();

        // Create bottom button panel
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);

        // Center on screen
        setLocationRelativeTo(null);

        // Ensure proper rendering
        GUIUtils.forceRender(this);
    }

    /**
     * Creates the tabbed pane with all panels
     */
    private void createTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(StyleFactory.BG_DARK_COLOR);
        tabbedPane.setForeground(StyleFactory.TEXT_COLOR);
        tabbedPane.setFont(StyleFactory.LABEL_FONT);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder());

        // Create and add all panels
        mainSettingsPanel = new MainSettingsPanel(config);
        foodPanel = new FoodPanel(config);
        antiBanPanel = new AntiBanPanel(config, currentUsername);
        advancedPanel = new AdvancedPanel(config);

        tabbedPane.addTab("Main Settings", mainSettingsPanel);
        tabbedPane.addTab("Food & Health", foodPanel);
        tabbedPane.addTab("Anti-Ban", antiBanPanel);
        tabbedPane.addTab("Advanced", advancedPanel);

        // Add tooltips to tabs
        tabbedPane.setToolTipTextAt(0, "Configure basic thieving settings");
        tabbedPane.setToolTipTextAt(1, "Configure food and health settings");
        tabbedPane.setToolTipTextAt(2, "Configure anti-ban behavior");
        tabbedPane.setToolTipTextAt(3, "Advanced script configuration");

        // Ensure tab contents render properly when switching tabs
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex >= 0) {
                Component component = tabbedPane.getComponentAt(selectedIndex);
                component.validate();
                component.repaint();
            }
        });

        // Add tabbed pane with scroll support
        JScrollPane tabbedScrollPane = new JScrollPane(tabbedPane);
        tabbedScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tabbedScrollPane.setViewportBorder(null);
        tabbedScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(tabbedScrollPane, BorderLayout.CENTER);
    }

    /**
     * Creates a styled header panel
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Create gradient
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(40, 20, 60),
                        w, h, new Color(20, 10, 30)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, w, h);

                // Draw bottom accent line
                g2d.setColor(StyleFactory.ACCENT_COLOR);
                g2d.fillRect(0, h - 2, w, 2);

                g2d.dispose();
            }
        };
        headerPanel.setPreferredSize(new Dimension(500, 60));
        headerPanel.setLayout(new BorderLayout());

        // Create title label
        JLabel titleLabel = new JLabel("CodeThief Pro v1.1");
        titleLabel.setForeground(StyleFactory.TEXT_COLOR);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Add subtitle
        JLabel subtitleLabel = new JLabel("Advanced Thieving Script");
        subtitleLabel.setForeground(StyleFactory.HEADER_COLOR);
        subtitleLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        return headerPanel;
    }

    /**
     * Creates the button panel
     */
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(StyleFactory.BG_DARK_COLOR);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, StyleFactory.BORDER_COLOR));

        // Create button row with help button and start button
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
        buttonRow.setBackground(StyleFactory.BG_DARK_COLOR);

        // Help button
        JButton helpButton = GUIUtils.createStyledButton("?");
        helpButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        helpButton.setPreferredSize(new Dimension(40, 40));
        helpButton.setToolTipText("Get help with script configuration");
        helpButton.addActionListener(e -> showHelpDialog());

        // Start button
        // UI components
        JButton startButton = GUIUtils.createStyledButton("Start Script");
        startButton.setPreferredSize(new Dimension(200, 40));
        startButton.addActionListener(e -> saveAndClose());

        buttonRow.add(helpButton);
        buttonRow.add(startButton);
        panel.add(buttonRow, BorderLayout.CENTER);

        // Add version number
        JLabel versionLabel = new JLabel("v1.1.0");
        versionLabel.setFont(StyleFactory.SMALL_FONT);
        versionLabel.setForeground(new Color(120, 120, 120));
        versionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 10));
        panel.add(versionLabel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Shows a help dialog with information about the script
     */
    private void showHelpDialog() {
        JDialog helpDialog = new JDialog(this, "CodeThief Pro Help", true);
        helpDialog.setSize(500, 500);
        helpDialog.setLocationRelativeTo(this);

        JPanel helpPanel = new JPanel();
        helpPanel.setLayout(new BorderLayout());
        helpPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create a tabbed help system
        JTabbedPane helpTabs = new JTabbedPane();

        // General help tab
        JTextArea generalHelp = new JTextArea(
                "CodeThief Pro - Thieving Script Help\n\n" +
                        "This script automates thieving activities in OSRS private servers. " +
                        "It supports pickpocketing NPCs and stealing from stalls with auto-progression " +
                        "based on your thieving level.\n\n" +
                        "Key Features:\n" +
                        "• Auto-progression between thieving methods\n" +
                        "• Intelligent banking and inventory management\n" +
                        "• Advanced anti-ban system with account-specific profiles\n" +
                        "• Customizable food and health management\n\n" +
                        "For best results, make sure your character has food in the bank " +
                        "or inventory before starting the script."
        );
        generalHelp.setEditable(false);
        generalHelp.setLineWrap(true);
        generalHelp.setWrapStyleWord(true);
        helpTabs.addTab("General", new JScrollPane(generalHelp));

        // Main settings help
        JTextArea mainHelp = new JTextArea(
                "Main Settings Help\n\n" +
                        "• Thieving Method: Select which NPC or stall to thieve from\n\n" +
                        "• Auto-Progression: Automatically select the best method for your level\n" +
                        "  - When enabled, the script will choose the optimal method\n" +
                        "  - When disabled, the script will use your selected method\n\n" +
                        "• Prefer Stalls: Choose stalls over NPCs when both are available\n\n" +
                        "• Freestyle Mode: Drop items instead of banking them\n" +
                        "  - Useful for training quickly without banking\n" +
                        "  - Disables banking when enabled\n\n" +
                        "• Enable Banking: Bank items when inventory is full\n" +
                        "  - Automatically walks to nearest bank when inventory is full\n" +
                        "  - Disables when Freestyle Mode is enabled"
        );
        mainHelp.setEditable(false);
        mainHelp.setLineWrap(true);
        mainHelp.setWrapStyleWord(true);
        helpTabs.addTab("Main Settings", new JScrollPane(mainHelp));

        // Food and health help
        JTextArea foodHelp = new JTextArea(
                "Food & Health Help\n\n" +
                        "• Health Threshold: Script will eat food when health drops below this percentage\n" +
                        "  - Higher values (70+) are safer but use more food\n" +
                        "  - Lower values (30-50) are more efficient but riskier\n\n" +
                        "• Primary Food: First food type the script will try to use\n" +
                        "  - Script will look for this food first in bank and inventory\n\n" +
                        "• Additional Foods: Backup food options\n" +
                        "  - Hold Ctrl key to select multiple options\n" +
                        "  - Script will try these foods if primary food is unavailable\n" +
                        "  - Foods will be tried in order of selection\n\n" +
                        "The script automatically recognizes generic food items with keywords " +
                        "like \"food\", \"brew\", and \"potion\" as fallbacks."
        );
        foodHelp.setEditable(false);
        foodHelp.setLineWrap(true);
        foodHelp.setWrapStyleWord(true);
        helpTabs.addTab("Food & Health", new JScrollPane(foodHelp));

        // Anti-ban help
        JTextArea antiBanHelp = new JTextArea(
                "Anti-Ban Help\n\n" +
                        "• Enable Anti-Ban: Turn anti-ban measures on/off\n" +
                        "  - Recommended: ON for safer botting\n" +
                        "  - OFF for maximum efficiency (higher risk)\n\n" +
                        "• Anti-Ban Intensity: Controls how aggressive anti-ban measures are\n" +
                        "  - Higher values (70+) are safer but less efficient\n" +
                        "  - Medium values (40-60) balance safety and efficiency\n" +
                        "  - Lower values (below 30) are more efficient but riskier\n\n" +
                        "Account-Specific Profiles:\n" +
                        "The script creates unique anti-ban profiles for each account. This ensures " +
                        "your character behaves consistently across sessions while still varying " +
                        "behaviors to avoid detection.\n\n" +
                        "Anti-ban behaviors include camera rotation, checking skills tab, short breaks, " +
                        "mouse movements, and occasional misclicks to simulate human behavior."
        );
        antiBanHelp.setEditable(false);
        antiBanHelp.setLineWrap(true);
        antiBanHelp.setWrapStyleWord(true);
        helpTabs.addTab("Anti-Ban", new JScrollPane(antiBanHelp));

        // Troubleshooting help
        JTextArea troubleshootHelp = new JTextArea(
                "Troubleshooting\n\n" +
                        "Common Issues:\n\n" +
                        "• Script not starting:\n" +
                        "  - Make sure you're logged in to the game\n" +
                        "  - Check that you've clicked the Start Script button\n\n" +
                        "• Script not finding food:\n" +
                        "  - Make sure you have food in your bank or inventory\n" +
                        "  - Check that you've selected the correct food types\n\n" +
                        "• Script walking to incorrect locations:\n" +
                        "  - Make sure you're on a compatible server\n" +
                        "  - Try toggling Auto-Progression on/off\n\n" +
                        "• Script stuck at bank:\n" +
                        "  - Check if bank is accessible\n" +
                        "  - Make sure your bank contains the selected food\n\n" +
                        "• NPCs not reachable:\n" +
                        "  - The script will automatically try to find reachable NPCs\n" +
                        "  - Some NPCs might be behind closed doors or obstacles\n\n" +
                        "For additional help, check the script's thread on the forum."
        );
        troubleshootHelp.setEditable(false);
        troubleshootHelp.setLineWrap(true);
        troubleshootHelp.setWrapStyleWord(true);
        helpTabs.addTab("Troubleshooting", new JScrollPane(troubleshootHelp));

        helpPanel.add(helpTabs, BorderLayout.CENTER);

        // Add close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> helpDialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(closeButton);
        helpPanel.add(buttonPanel, BorderLayout.SOUTH);

        helpDialog.add(helpPanel);
        helpDialog.setVisible(true);
    }

    /**
     * Saves all configuration settings and closes the GUI
     */
    private void saveAndClose() {
        try {
            // Check for validation errors
            List<String> validationErrors = validateSettings();

            if (!validationErrors.isEmpty()) {
                // Show warning if settings are missing
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
                    return; // Don't save and close if user cancels
                }
            }

            // Save settings from all panels
            mainSettingsPanel.saveSettings();
            foodPanel.saveSettings();
            antiBanPanel.saveSettings();
            advancedPanel.saveSettings();

            // Set configured flag
            config.setConfigured(true);

            // Show confirmation
            JOptionPane.showMessageDialog(this,
                    "Configuration saved. Starting script...",
                    "CodeThief Pro",
                    JOptionPane.INFORMATION_MESSAGE);

            // Close the GUI
            dispose();
        } catch (Exception e) {
            // Show error message
            JOptionPane.showMessageDialog(this,
                    "Error saving configuration: " + e.getMessage() + "\n" +
                            "The script will continue with default settings.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);

            // Log the error
            System.err.println("Error saving configuration: " + e.getMessage());

            // Still mark as configured and close
            config.setConfigured(true);
            dispose();
        }
    }

    /**
     * Validates settings across all panels
     *
     * @return List of validation error messages
     */
    private List<String> validateSettings() {
        List<String> errors = new ArrayList<>();

        // Check main settings
        String mainError = mainSettingsPanel.validateSettings();
        if (mainError != null) {
            errors.add(mainError);
        }

        // Check food settings
        String foodError = foodPanel.validateSettings();
        if (foodError != null) {
            errors.add(foodError);
        }

        return errors;
    }

    /**
     * Forces the GUI to properly render all components on display
     * Override to ensure components render properly
     */
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);

        if (visible) {
            // Force another validation and repaint after becoming visible
            SwingUtilities.invokeLater(() -> {
                validate();
                repaint();

                // Create a small delay and do another repaint to catch any missed components
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