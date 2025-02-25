package org.gui.panels;

import org.ScriptConfiguration;
import org.gui.util.GUIUtils;
import org.gui.util.StyleFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for anti-ban settings
 */
public class AntiBanPanel extends JPanel {
    private final ScriptConfiguration config;
    private final String currentUsername;

    // GUI components
    private JCheckBox antiBanEnabledCheckbox;
    private JSlider antiBanIntensitySlider;
    private JTextArea antiBanProfileText;
    private JLabel intensityValueLabel;

    /**
     * Creates a new anti-ban panel
     *
     * @param config   The script configuration
     * @param username The current player username
     */
    public AntiBanPanel(ScriptConfiguration config, String username) {
        this.config = config;
        this.currentUsername = username;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(StyleFactory.BG_DARK_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        initializeComponents();
    }

    /**
     * Initializes all panel components
     */
    private void initializeComponents() {
        // Add header with explanation
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = GUIUtils.createStyledLabel("Anti-Ban Settings");
        titleLabel.setFont(StyleFactory.HEADER_FONT);
        titleLabel.setForeground(StyleFactory.HEADER_COLOR);

        JLabel descLabel = GUIUtils.createStyledSubtextLabel(
                "Configure anti-ban behavior to make the script more human-like (account-specific)."
        );

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(descLabel, BorderLayout.CENTER);
        add(headerPanel);
        add(Box.createVerticalStrut(15));

        // Main anti-ban settings panel
        JPanel antiBanSettingsPanel = createAntiBanSettingsPanel();
        add(antiBanSettingsPanel);
        add(Box.createVerticalStrut(15));

        // Add anti-ban explanation
        JPanel explanationPanel = new JPanel(new BorderLayout());
        explanationPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        explanationPanel.setBorder(BorderFactory.createLineBorder(StyleFactory.BORDER_COLOR));

        JTextArea explanationText = new JTextArea(
                "Anti-ban measures help avoid detection by adding human-like behavior to the script. " +
                        "Each account has a unique profile for consistent but varied behavior."
        );
        explanationText.setFont(StyleFactory.LABEL_FONT);
        explanationText.setForeground(StyleFactory.TEXT_COLOR);
        explanationText.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        explanationText.setLineWrap(true);
        explanationText.setWrapStyleWord(true);
        explanationText.setEditable(false);
        explanationText.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel subLabel = GUIUtils.createStyledSubtextLabel(
                "Higher intensity = more human-like but possibly lower XP rates"
        );
        subLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        explanationPanel.add(explanationText, BorderLayout.CENTER);
        explanationPanel.add(subLabel, BorderLayout.SOUTH);
        add(explanationPanel);
        add(Box.createVerticalStrut(15));

        // Add profile preview area
        JPanel profilePanel = createProfilePreviewPanel();
        add(profilePanel);
        add(Box.createVerticalStrut(10));

        // Add regenerate button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(StyleFactory.BG_DARK_COLOR);

        JButton regenerateButton = GUIUtils.createStyledButton("Regenerate Profile");
        regenerateButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        regenerateButton.setPreferredSize(new Dimension(150, 30));
        regenerateButton.addActionListener(e -> regenerateProfile());
        buttonPanel.add(regenerateButton);

        add(buttonPanel);
        add(Box.createVerticalGlue());

        // Setup action listeners
        setupActionListeners();

        // Initialize profile display
        updateAntiBanProfileDisplay();
    }

    /**
     * Creates the anti-ban settings panel
     */
    private JPanel createAntiBanSettingsPanel() {
        JPanel antiBanSettingsPanel = GUIUtils.createSectionPanel("Anti-Ban Configuration");
        antiBanSettingsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);

        // Add enable anti-ban checkbox
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JLabel enableLabel = GUIUtils.createStyledLabel("Enable Anti-Ban:");
        enableLabel.setToolTipText("Turn anti-ban measures on/off");
        antiBanSettingsPanel.add(enableLabel, gbc);

        gbc.gridx = 1;
        antiBanEnabledCheckbox = GUIUtils.createStyledCheckBox();
        antiBanEnabledCheckbox.setSelected(true);
        antiBanEnabledCheckbox.setToolTipText("Recommended: ON - Makes the script behave more like a human player");
        antiBanSettingsPanel.add(antiBanEnabledCheckbox, gbc);

        // Add anti-ban intensity slider
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel intensityLabel = GUIUtils.createStyledLabel("Anti-Ban Intensity:");
        intensityLabel.setToolTipText("Controls how aggressive the anti-ban measures are");
        antiBanSettingsPanel.add(intensityLabel, gbc);

        gbc.gridx = 1;
        antiBanIntensitySlider = GUIUtils.createStyledSlider(0, 100, 50);
        antiBanIntensitySlider.setMajorTickSpacing(25);
        antiBanIntensitySlider.setMinorTickSpacing(5);
        antiBanIntensitySlider.setPaintTicks(true);
        antiBanIntensitySlider.setPaintLabels(true);
        antiBanIntensitySlider.setToolTipText("Higher values make behavior more human-like but may reduce efficiency");

        // Add live value display
        JPanel intensityPanel = new JPanel(new BorderLayout(5, 0));
        intensityPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);

        intensityValueLabel = GUIUtils.createStyledLabel("50%");
        intensityValueLabel.setPreferredSize(new Dimension(40, 20));
        intensityValueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        antiBanIntensitySlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            intensityValueLabel.setText(source.getValue() + "%");
            updateAntiBanProfileDisplay(); // Update the profile display when slider changes
        });

        intensityPanel.add(antiBanIntensitySlider, BorderLayout.CENTER);
        intensityPanel.add(intensityValueLabel, BorderLayout.EAST);

        gbc.gridx = 1;
        antiBanSettingsPanel.add(intensityPanel, gbc);

        // Add account-specific profile note
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JLabel profileLabel = GUIUtils.createStyledLabel("Account-Specific Profile: " + currentUsername);
        profileLabel.setForeground(StyleFactory.HEADER_COLOR);
        profileLabel.setHorizontalAlignment(SwingConstants.CENTER);
        profileLabel.setToolTipText("Anti-ban behavior is customized for this account");
        antiBanSettingsPanel.add(profileLabel, gbc);

        return antiBanSettingsPanel;
    }

    /**
     * Creates the profile preview panel
     */
    private JPanel createProfilePreviewPanel() {
        JPanel profilePanel = GUIUtils.createSectionPanel("Profile Preview");
        profilePanel.setLayout(new BorderLayout());

        antiBanProfileText = new JTextArea(6, 30);
        antiBanProfileText.setEditable(false);
        antiBanProfileText.setFont(new Font("Monospaced", Font.PLAIN, 12));
        antiBanProfileText.setBackground(StyleFactory.BG_DARK_COLOR);
        antiBanProfileText.setForeground(StyleFactory.TEXT_COLOR);
        antiBanProfileText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane profileScroll = GUIUtils.createStyledScrollPane(antiBanProfileText);
        profilePanel.add(profileScroll, BorderLayout.CENTER);

        return profilePanel;
    }

    /**
     * Setup action listeners for component interactions
     */
    private void setupActionListeners() {
        antiBanEnabledCheckbox.addActionListener(e -> {
            boolean enabled = antiBanEnabledCheckbox.isSelected();
            antiBanIntensitySlider.setEnabled(enabled);

            // Find and update intensity label
            Component[] components = getComponents();
            for (Component component : components) {
                if (component instanceof JPanel) {
                    findAndUpdateLabels((JPanel) component, enabled);
                }
            }

            updateAntiBanProfileDisplay();
            validate();
            repaint();
        });
    }

    /**
     * Helper method to find and update labels in panels
     */
    private void findAndUpdateLabels(JPanel panel, boolean enabled) {
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JLabel) {
                JLabel label = (JLabel) component;
                if (label.getText().equals("Anti-Ban Intensity:")) {
                    label.setForeground(enabled ? StyleFactory.TEXT_COLOR : StyleFactory.DISABLED_COLOR);
                } else if (label.getText().startsWith("Account-Specific Profile:")) {
                    label.setForeground(enabled ? StyleFactory.HEADER_COLOR : StyleFactory.DISABLED_COLOR);
                }
            } else if (component instanceof JPanel) {
                findAndUpdateLabels((JPanel) component, enabled);
            }
        }
    }

    /**
     * Updates the anti-ban profile preview display
     */
    private void updateAntiBanProfileDisplay() {
        if (!antiBanEnabledCheckbox.isSelected()) {
            antiBanProfileText.setText("Anti-Ban is disabled. No profile will be used.");
            return;
        }

        int intensity = antiBanIntensitySlider.getValue();

        // Generate a preview of what the profile would look like
        StringBuilder preview = new StringBuilder();
        preview.append("Anti-Ban Profile for: ").append(currentUsername).append("\n");

        // Calculate values based on intensity
        double baseRotation = 0.15 + (intensity / 200.0);
        double baseSkill = 0.05 + (intensity / 400.0);
        double baseBreak = 0.02 + (intensity / 500.0);
        double baseMouse = 0.10 + (intensity / 250.0);
        double baseMisclick = 0.02 + (intensity / 600.0);

        // Add some randomness to make it look authentic
        double rotationJitter = Math.random() * 0.03 - 0.015;
        double skillJitter = Math.random() * 0.02 - 0.01;
        double breakJitter = Math.random() * 0.01 - 0.005;
        double mouseJitter = Math.random() * 0.03 - 0.015;
        double misclickJitter = Math.random() * 0.01 - 0.005;

        // Format the values
        preview.append("Camera Rotation: ").append(String.format("%.1f%%", (baseRotation + rotationJitter) * 100)).append("\n");
        preview.append("Skill Checking: ").append(String.format("%.1f%%", (baseSkill + skillJitter) * 100)).append("\n");
        preview.append("Break Frequency: ").append(String.format("%.1f%%", (baseBreak + breakJitter) * 100)).append("\n");
        preview.append("Mouse Movement: ").append(String.format("%.1f%%", (baseMouse + mouseJitter) * 100)).append("\n");
        preview.append("Misclick Rate: ").append(String.format("%.1f%%", (baseMisclick + misclickJitter) * 100)).append("\n");

        // Add behavior style notes
        int styleIndex = Math.abs(currentUsername.hashCode() % 4);
        String[] rotationStyles = {"Full 360°", "90° Preference", "Limited Range", "Micro-Adjustments"};
        String[] mouseStyles = {"Natural Circular", "Grid Pattern", "Rectangle Pattern", "Small Jittery"};
        String[] breakStyles = {"Short Breaks", "Medium Breaks", "Long Breaks", "Variable Breaks"};

        preview.append("\nStyle Patterns:").append("\n");
        preview.append("• Camera: ").append(rotationStyles[styleIndex]).append("\n");
        preview.append("• Mouse: ").append(mouseStyles[(styleIndex + 2) % 4]).append("\n");
        preview.append("• Breaks: ").append(breakStyles[(styleIndex + 1) % 4]);

        antiBanProfileText.setText(preview.toString());
    }

    /**
     * Simulates regenerating a profile and updates the display
     */
    private void regenerateProfile() {
        // Simply update the display with new random values
        updateAntiBanProfileDisplay();

        JOptionPane.showMessageDialog(this,
                "Created new anti-ban profile for " + currentUsername + "\n" +
                        "This profile will be used when the script runs.",
                "Profile Generated",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Saves panel settings to the configuration object
     */
    public void saveSettings() {
        config.setAntiBanEnabled(antiBanEnabledCheckbox.isSelected());
        config.setAntiBanIntensity(antiBanIntensitySlider.getValue());
    }
}