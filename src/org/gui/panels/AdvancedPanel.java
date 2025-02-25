package org.gui.panels;

import org.ScriptConfiguration;
import org.gui.util.GUIUtils;
import org.gui.util.StyleFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for advanced configuration settings
 */
public class AdvancedPanel extends JPanel {
    private final ScriptConfiguration config;

    /**
     * Creates a new advanced settings panel
     *
     * @param config The script configuration
     */
    public AdvancedPanel(ScriptConfiguration config) {
        this.config = config;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(StyleFactory.BG_DARK_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initializeComponents();
    }

    /**
     * Initializes all panel components
     */
    private void initializeComponents() {
        // Add header with warning
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = GUIUtils.createStyledLabel("Advanced Settings");
        titleLabel.setFont(StyleFactory.HEADER_FONT);
        titleLabel.setForeground(StyleFactory.HEADER_COLOR);

        JLabel descLabel = GUIUtils.createStyledSubtextLabel(
                "These settings are for advanced users. Changing them may impact script performance."
        );

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(descLabel, BorderLayout.CENTER);
        add(headerPanel);
        add(Box.createVerticalStrut(15));

        // Create coming soon panel
        JPanel comingSoonPanel = createComingSoonPanel();
        add(comingSoonPanel);
        add(Box.createVerticalStrut(15));

        // Add debug panel
        JPanel debugPanel = createDebugPanel();
        add(debugPanel);
        add(Box.createVerticalGlue());
    }

    /**
     * Creates the coming soon panel
     */
    private JPanel createComingSoonPanel() {
        JPanel comingSoonPanel = GUIUtils.createSectionPanel("Coming in Future Updates");
        comingSoonPanel.setLayout(new BorderLayout());

        JTextArea comingSoonText = new JTextArea(
                "• Custom loot profiles\n" +
                        "• Target profitability analysis\n" +
                        "• Performance optimization settings\n" +
                        "• Path customization\n" +
                        "• Session statistics tracking\n" +
                        "• Auto-logout conditions"
        );
        comingSoonText.setEditable(false);
        comingSoonText.setFont(StyleFactory.LABEL_FONT);
        comingSoonText.setForeground(StyleFactory.TEXT_COLOR);
        comingSoonText.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        comingSoonText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        comingSoonText.setLineWrap(true);
        comingSoonText.setWrapStyleWord(true);

        comingSoonPanel.add(comingSoonText, BorderLayout.CENTER);

        return comingSoonPanel;
    }

    /**
     * Creates the debug panel
     */
    private JPanel createDebugPanel() {
        JPanel debugPanel = GUIUtils.createSectionPanel("Debug Options");
        debugPanel.setLayout(new BorderLayout());

        JButton debugLogButton = GUIUtils.createStyledButton("View Debug Log");
        debugLogButton.setPreferredSize(new Dimension(120, 30));
        debugLogButton.setFont(StyleFactory.LABEL_FONT);
        debugLogButton.addActionListener(e -> showDebugLog());

        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonWrapper.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        buttonWrapper.add(debugLogButton);
        debugPanel.add(buttonWrapper, BorderLayout.CENTER);

        return debugPanel;
    }

    /**
     * Shows a debug log dialog
     */
    private void showDebugLog() {
        // Create a simple debug log display
        JDialog logDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Debug Log", true);
        logDialog.setSize(500, 400);
        logDialog.setLocationRelativeTo(this);

        JTextArea logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setText("Script initialized\n" +
                "Config loaded\n" +
                "Anti-ban profile created for: " + (config != null ? "Active User" : "Unknown") + "\n" +
                "Current thieving level: " + "Loading..." + "\n" +
                "Optimal target: " + "Determining..." + "\n" +
                "GUI started\n");

        JScrollPane scrollPane = new JScrollPane(logArea);
        logDialog.add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(event -> logDialog.dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        logDialog.add(buttonPanel, BorderLayout.SOUTH);

        logDialog.setVisible(true);
    }

    /**
     * Saves panel settings to the configuration object
     * Currently no settings to save from this panel
     */
    public void saveSettings() {
        // No settings to save in this panel yet
    }
}