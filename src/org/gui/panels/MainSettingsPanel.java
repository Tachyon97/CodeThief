package org.gui.panels;

import org.ScriptConfiguration;
import org.dreambot.api.methods.skills.Skill;
import org.dreambot.api.methods.skills.Skills;
import org.gui.util.GUIUtils;
import org.gui.util.StyleFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for main script settings like thieving method and auto-progression
 * Updated to remove non-working stall options
 */
public class MainSettingsPanel extends JPanel {
    private final ScriptConfiguration config;

    // GUI components
    private JComboBox<String> methodSelector;
    private JCheckBox autoProgressionCheckbox;
    private JCheckBox freestyleModeCheckbox;
    private JCheckBox bankingEnabledCheckbox;
    private JCheckBox preferStallsCheckbox;

    /**
     * Creates a new main settings panel
     *
     * @param config The script configuration
     */
    public MainSettingsPanel(ScriptConfiguration config) {
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
        // Info panel explaining this section
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = GUIUtils.createStyledLabel("Main Configuration");
        titleLabel.setFont(StyleFactory.HEADER_FONT);
        titleLabel.setForeground(StyleFactory.HEADER_COLOR);

        JLabel descLabel = GUIUtils.createStyledSubtextLabel(
                "Configure the primary thieving settings below. Choose your target and strategy options."
        );

        infoPanel.add(titleLabel, BorderLayout.NORTH);
        infoPanel.add(descLabel, BorderLayout.CENTER);
        add(infoPanel);
        add(Box.createVerticalStrut(15));

        // Settings panel with all controls
        JPanel settingsPanel = createSettingsPanel();
        add(settingsPanel);
        add(Box.createVerticalStrut(10));

        // Add auto-progression explanation
        JPanel explanationPanel = new JPanel(new BorderLayout());
        explanationPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        explanationPanel.setBorder(BorderFactory.createLineBorder(StyleFactory.BORDER_COLOR));

        JTextArea explanationText = new JTextArea(
                "Auto-Progression will automatically choose the best thieving method based on your level. " +
                        "If disabled, you must select a specific method."
        );
        explanationText.setFont(StyleFactory.LABEL_FONT);
        explanationText.setForeground(StyleFactory.TEXT_COLOR);
        explanationText.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        explanationText.setLineWrap(true);
        explanationText.setWrapStyleWord(true);
        explanationText.setEditable(false);
        explanationText.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel subLabel = GUIUtils.createStyledSubtextLabel(
                "With Auto-Progression enabled, the Method selector above will be ignored."
        );
        subLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        explanationPanel.add(explanationText, BorderLayout.CENTER);
        explanationPanel.add(subLabel, BorderLayout.SOUTH);
        add(explanationPanel);

        // Add new stall limitation notice
        JPanel stallInfoPanel = new JPanel(new BorderLayout());
        stallInfoPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        stallInfoPanel.setBorder(BorderFactory.createLineBorder(StyleFactory.BORDER_COLOR));

        JTextArea stallInfoText = new JTextArea(
                "Note: Only Tea Stall and Cake Stall are available as stall options. " +
                        "These have been thoroughly tested and optimized for reliable performance."
        );
        stallInfoText.setFont(StyleFactory.SMALL_FONT);
        stallInfoText.setForeground(new Color(255, 165, 0)); // Orange color for notice
        stallInfoText.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        stallInfoText.setLineWrap(true);
        stallInfoText.setWrapStyleWord(true);
        stallInfoText.setEditable(false);
        stallInfoText.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        stallInfoPanel.add(stallInfoText, BorderLayout.CENTER);
        add(Box.createVerticalStrut(10));
        add(stallInfoPanel);

        // Add view current level button
        JPanel levelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        levelPanel.setBackground(StyleFactory.BG_DARK_COLOR);
        levelPanel.setOpaque(true);

        JButton viewLevelButton = GUIUtils.createStyledButton("View Current Level");
        viewLevelButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        viewLevelButton.setPreferredSize(new Dimension(150, 30));
        viewLevelButton.addActionListener(e -> showCurrentLevel());
        levelPanel.add(viewLevelButton);
        add(Box.createVerticalStrut(10));
        add(levelPanel);

        // Setup action listeners
        setupActionListeners();
    }

    /**
     * Creates the settings panel with all main controls
     */
    private JPanel createSettingsPanel() {
        JPanel settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBackground(StyleFactory.BG_DARK_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Method selector with enhanced styling
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel methodLabel = GUIUtils.createStyledLabel("Thieving Method:");
        methodLabel.setToolTipText("Select which NPC or object to steal from");
        settingsPanel.add(methodLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0; // Allow component to expand
        // Use the updated available methods list from ScriptConfiguration
        methodSelector = GUIUtils.createStyledComboBox(config.getAllThievingMethods());
        methodSelector.setPreferredSize(new Dimension(220, 30));
        methodSelector.setMinimumSize(new Dimension(220, 30));
        methodSelector.setToolTipText("Higher level methods give better XP and loot");
        settingsPanel.add(methodSelector, gbc);
        gbc.weightx = 0.0; // Reset weight

        // Auto-progression checkbox
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel autoProgressLabel = GUIUtils.createStyledLabel("Auto-Progression:");
        autoProgressLabel.setToolTipText("Automatically select the best method for your level");
        settingsPanel.add(autoProgressLabel, gbc);

        gbc.gridx = 1;
        autoProgressionCheckbox = GUIUtils.createStyledCheckBox();
        autoProgressionCheckbox.setSelected(true);
        autoProgressionCheckbox.setToolTipText("Recommended: ON - Script will choose optimal method based on thieving level");
        autoProgressionCheckbox.setOpaque(true);
        autoProgressionCheckbox.setBackground(StyleFactory.BG_DARK_COLOR);
        settingsPanel.add(autoProgressionCheckbox, gbc);

        // Prefer stalls checkbox
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel stallsLabel = GUIUtils.createStyledLabel("Prefer Stalls:");
        stallsLabel.setToolTipText("Prefer tea/cake stalls over NPCs when both are available at your level");
        settingsPanel.add(stallsLabel, gbc);

        gbc.gridx = 1;
        preferStallsCheckbox = GUIUtils.createStyledCheckBox();
        preferStallsCheckbox.setToolTipText("When enabled, Tea/Cake stalls will be preferred over NPCs at equivalent levels");
        preferStallsCheckbox.setOpaque(true);
        preferStallsCheckbox.setBackground(StyleFactory.BG_DARK_COLOR);
        settingsPanel.add(preferStallsCheckbox, gbc);

        // Add separator
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JPanel separatorPanel = new JPanel(new GridBagLayout());
        separatorPanel.setBackground(StyleFactory.BG_DARK_COLOR);
        GridBagConstraints sepGbc = new GridBagConstraints();

        JLabel separatorLabel = new JLabel("Inventory Management");
        separatorLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        separatorLabel.setForeground(StyleFactory.HEADER_COLOR);

        sepGbc.gridx = 0;
        sepGbc.gridy = 0;
        sepGbc.insets = new Insets(5, 0, 5, 5);
        separatorPanel.add(separatorLabel, sepGbc);

        JSeparator separator = new JSeparator();
        separator.setForeground(StyleFactory.BORDER_COLOR);
        sepGbc.gridx = 1;
        sepGbc.gridy = 0;
        sepGbc.fill = GridBagConstraints.HORIZONTAL;
        sepGbc.weightx = 1.0;
        separatorPanel.add(separator, sepGbc);

        settingsPanel.add(separatorPanel, gbc);
        gbc.gridwidth = 1;

        // Freestyle mode checkbox
        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel freestyleLabel = GUIUtils.createStyledLabel("Freestyle Mode:");
        freestyleLabel.setToolTipText("Drop items instead of banking them");
        settingsPanel.add(freestyleLabel, gbc);

        gbc.gridx = 1;
        freestyleModeCheckbox = GUIUtils.createStyledCheckBox();
        freestyleModeCheckbox.setToolTipText("When enabled, items will be dropped instead of banked (disables banking)");
        freestyleModeCheckbox.setOpaque(true);
        freestyleModeCheckbox.setBackground(StyleFactory.BG_DARK_COLOR);
        settingsPanel.add(freestyleModeCheckbox, gbc);

        // Banking enabled checkbox
        gbc.gridx = 0;
        gbc.gridy = 5;
        JLabel bankingLabel = GUIUtils.createStyledLabel("Enable Banking:");
        bankingLabel.setToolTipText("Bank items when inventory is full");
        settingsPanel.add(bankingLabel, gbc);

        gbc.gridx = 1;
        bankingEnabledCheckbox = GUIUtils.createStyledCheckBox();
        bankingEnabledCheckbox.setSelected(true);
        bankingEnabledCheckbox.setToolTipText("When enabled, script will bank items when inventory is full");
        bankingEnabledCheckbox.setOpaque(true);
        bankingEnabledCheckbox.setBackground(StyleFactory.BG_DARK_COLOR);
        settingsPanel.add(bankingEnabledCheckbox, gbc);

        // Wrap settings in a proper panel for layout
        JPanel settingsWrapper = new JPanel(new BorderLayout());
        settingsWrapper.setBackground(StyleFactory.BG_DARK_COLOR);
        settingsWrapper.add(settingsPanel, BorderLayout.NORTH);

        return settingsWrapper;
    }

    /**
     * Setup action listeners for component interactions
     */
    private void setupActionListeners() {
        // Freestyle mode handling
        freestyleModeCheckbox.addActionListener(e -> {
            if (freestyleModeCheckbox.isSelected()) {
                bankingEnabledCheckbox.setSelected(false);
                bankingEnabledCheckbox.setEnabled(false);
                for (Component comp : getComponents()) {
                    if (comp instanceof JPanel) {
                        for (Component inner : ((JPanel) comp).getComponents()) {
                            if (inner instanceof JLabel && ((JLabel) inner).getText().equals("Enable Banking:")) {
                                inner.setForeground(StyleFactory.DISABLED_COLOR);
                                break;
                            }
                        }
                    }
                }
            } else {
                bankingEnabledCheckbox.setEnabled(true);
                for (Component comp : getComponents()) {
                    if (comp instanceof JPanel) {
                        for (Component inner : ((JPanel) comp).getComponents()) {
                            if (inner instanceof JLabel && ((JLabel) inner).getText().equals("Enable Banking:")) {
                                inner.setForeground(StyleFactory.TEXT_COLOR);
                                break;
                            }
                        }
                    }
                }
            }
            this.validate();
            this.repaint();
        });

        // Auto progression handling
        autoProgressionCheckbox.addActionListener(e -> {
            boolean autoProgressEnabled = autoProgressionCheckbox.isSelected();
            methodSelector.setEnabled(!autoProgressEnabled);

            for (Component comp : getComponents()) {
                if (comp instanceof JPanel) {
                    for (Component inner : ((JPanel) comp).getComponents()) {
                        if (inner instanceof JLabel && ((JLabel) inner).getText().equals("Thieving Method:")) {
                            inner.setForeground(autoProgressEnabled ? StyleFactory.DISABLED_COLOR : StyleFactory.TEXT_COLOR);
                            break;
                        }
                    }
                }
            }

            // Update appearance based on enabled state
            if (autoProgressEnabled) {
                methodSelector.setForeground(StyleFactory.DISABLED_COLOR);
                methodSelector.setBackground(StyleFactory.BG_MEDIUM_COLOR.darker());
            } else {
                methodSelector.setForeground(StyleFactory.TEXT_COLOR);
                methodSelector.setBackground(StyleFactory.BG_MEDIUM_COLOR);
            }

            this.validate();
            this.repaint();
        });

        // Trigger initial states
        SwingUtilities.invokeLater(() -> {
            autoProgressionCheckbox.getActionListeners()[0].actionPerformed(null);
            freestyleModeCheckbox.getActionListeners()[0].actionPerformed(null);
        });
    }

    /**
     * Shows a dialog with current thieving level information
     */
    private void showCurrentLevel() {
        try {
            int thievingLevel = Skills.getRealLevel(Skill.THIEVING);
            String optimalTarget = config.determineOptimalTarget(thievingLevel);

            // Show level and recommended target
            JOptionPane.showMessageDialog(this,
                    "Current Thieving Level: " + thievingLevel + "\n" +
                            "Recommended Target: " + optimalTarget + "\n\n" +
                            "Available Stalls: Tea Stall and Cake Stall",
                    "Thieving Information",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not retrieve thieving level.\nMake sure you're logged in.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Saves panel settings to the configuration object
     */
    public void saveSettings() {
        config.setCurrentTargetName((String) methodSelector.getSelectedItem());
        config.setAutoProgressionEnabled(autoProgressionCheckbox.isSelected());
        config.setFreestyleMode(freestyleModeCheckbox.isSelected());
        config.setBankingEnabled(bankingEnabledCheckbox.isSelected());
        config.setPreferStalls(preferStallsCheckbox.isSelected());
    }

    /**
     * Validates panel settings and returns any errors
     *
     * @return Error message or null if valid
     */
    public String validateSettings() {
        if (!autoProgressionCheckbox.isSelected() && methodSelector.getSelectedItem() == null) {
            return "Thieving Method must be selected when Auto-Progression is disabled";
        }
        return null;
    }
}