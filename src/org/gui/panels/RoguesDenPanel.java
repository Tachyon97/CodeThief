package org.gui.panels;

import org.ScriptConfiguration;
import org.gui.util.GUIUtils;
import org.gui.util.StyleFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Panel for Rogues' Den specific settings
 */
public class RoguesDenPanel extends JPanel {
    private final ScriptConfiguration config;

    // GUI components
    private JCheckBox enableRoguesDenCheckbox;
    private JCheckBox stopAfterFullOutfitCheckbox;
    private JCheckBox useStaminaPotionsCheckbox;
    private JSpinner mazeRunsSpinner;
    private JPanel outfitPreviewPanel;

    /**
     * Creates a new Rogues' Den panel
     *
     * @param config The script configuration
     */
    public RoguesDenPanel(ScriptConfiguration config) {
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
        // Add header with explanation
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = GUIUtils.createStyledLabel("Rogues' Den Settings");
        titleLabel.setFont(StyleFactory.HEADER_FONT);
        titleLabel.setForeground(StyleFactory.HEADER_COLOR);

        JLabel descLabel = GUIUtils.createStyledSubtextLabel(
                "Configure options for obtaining the Rogues' outfit. Requires 50 Thieving and 50 Agility."
        );

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(descLabel, BorderLayout.CENTER);
        add(headerPanel);
        add(Box.createVerticalStrut(15));

        // Main settings panel
        JPanel settingsPanel = createSettingsPanel();
        add(settingsPanel);
        add(Box.createVerticalStrut(15));

        // Add outfit preview
        outfitPreviewPanel = createOutfitPreviewPanel();
        add(outfitPreviewPanel);
        add(Box.createVerticalStrut(15));

        // Add explanation
        JPanel explanationPanel = new JPanel(new BorderLayout());
        explanationPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        explanationPanel.setBorder(BorderFactory.createLineBorder(StyleFactory.BORDER_COLOR));

        JTextArea explanationText = new JTextArea(
                "The Rogues' outfit gives a 100% chance to double pickpocketing loot when the full set is worn. " +
                        "Each maze run takes approximately 5-10 minutes depending on your success rate."
        );
        explanationText.setFont(StyleFactory.LABEL_FONT);
        explanationText.setForeground(StyleFactory.TEXT_COLOR);
        explanationText.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        explanationText.setLineWrap(true);
        explanationText.setWrapStyleWord(true);
        explanationText.setEditable(false);
        explanationText.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        JLabel subLabel = GUIUtils.createStyledSubtextLabel(
                "Note: All equipment must be removed when entering Rogues' Den."
        );
        subLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        explanationPanel.add(explanationText, BorderLayout.CENTER);
        explanationPanel.add(subLabel, BorderLayout.SOUTH);
        add(explanationPanel);
        add(Box.createVerticalGlue());

        // Setup action listeners
        setupActionListeners();
    }

    /**
     * Creates the settings panel with all options
     */
    private JPanel createSettingsPanel() {
        JPanel settingsPanel = GUIUtils.createSectionPanel("Rogues' Den Options");
        settingsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.0;

        // Enable Rogues' Den option
        JLabel enableLabel = GUIUtils.createStyledLabel("Enable Rogues' Den:");
        enableLabel.setToolTipText("Enable Rogues' Den minigame to obtain the Rogues' outfit");
        settingsPanel.add(enableLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        enableRoguesDenCheckbox = GUIUtils.createStyledCheckBox();
        enableRoguesDenCheckbox.setToolTipText("When enabled, the script will run the Rogues' Den minigame");
        settingsPanel.add(enableRoguesDenCheckbox, gbc);

        // Stop after full outfit option
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        JLabel stopAfterLabel = GUIUtils.createStyledLabel("Stop After Full Outfit:");
        stopAfterLabel.setToolTipText("Stop running the minigame once the full outfit is obtained");
        settingsPanel.add(stopAfterLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        stopAfterFullOutfitCheckbox = GUIUtils.createStyledCheckBox();
        stopAfterFullOutfitCheckbox.setSelected(true);
        stopAfterFullOutfitCheckbox.setToolTipText("When enabled, the script will stop running Rogues' Den once all pieces are obtained");
        settingsPanel.add(stopAfterFullOutfitCheckbox, gbc);

        // Use stamina potions option
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0.0;
        JLabel staminaLabel = GUIUtils.createStyledLabel("Use Stamina Potions:");
        staminaLabel.setToolTipText("Use stamina potions to maintain run energy");
        settingsPanel.add(staminaLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        useStaminaPotionsCheckbox = GUIUtils.createStyledCheckBox();
        useStaminaPotionsCheckbox.setSelected(true);
        useStaminaPotionsCheckbox.setToolTipText("When enabled, the script will use stamina potions if available");
        settingsPanel.add(useStaminaPotionsCheckbox, gbc);

        // Maze runs limit spinner
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        JLabel mazeRunsLabel = GUIUtils.createStyledLabel("Max Maze Runs:");
        mazeRunsLabel.setToolTipText("Maximum number of maze runs to attempt (0 = unlimited)");
        settingsPanel.add(mazeRunsLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        SpinnerNumberModel model = new SpinnerNumberModel(0, 0, 1000, 1);
        mazeRunsSpinner = new JSpinner(model);
        mazeRunsSpinner.setToolTipText("Set to 0 for unlimited runs");
        mazeRunsSpinner.setPreferredSize(new Dimension(80, 25));
        JPanel spinnerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spinnerPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        spinnerPanel.add(mazeRunsSpinner);
        settingsPanel.add(spinnerPanel, gbc);

        return settingsPanel;
    }

    /**
     * Creates the outfit preview panel
     */
    private JPanel createOutfitPreviewPanel() {
        JPanel outfitPanel = GUIUtils.createSectionPanel("Rogues' Outfit");
        outfitPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        String[] pieceNames = {
                "Rogues' mask", "Rogues' top", "Rogues' trousers",
                "Rogues' gloves", "Rogues' boots"
        };

        for (int i = 0; i < pieceNames.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 1.0;

            JPanel piecePanel = new JPanel(new BorderLayout());
            piecePanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
            piecePanel.setBorder(BorderFactory.createLineBorder(StyleFactory.BORDER_COLOR));

            JLabel pieceLabel = GUIUtils.createStyledLabel(pieceNames[i]);
            piecePanel.add(pieceLabel, BorderLayout.WEST);

            JLabel statusLabel = GUIUtils.createStyledLabel("Not obtained");
            statusLabel.setForeground(Color.RED);
            piecePanel.add(statusLabel, BorderLayout.EAST);

            outfitPanel.add(piecePanel, gbc);
        }

        return outfitPanel;
    }

    /**
     * Setup action listeners for component interactions
     */
    private void setupActionListeners() {
        enableRoguesDenCheckbox.addActionListener(e -> {
            boolean enabled = enableRoguesDenCheckbox.isSelected();
            stopAfterFullOutfitCheckbox.setEnabled(enabled);
            useStaminaPotionsCheckbox.setEnabled(enabled);
            mazeRunsSpinner.setEnabled(enabled);

            // Update labels based on enabled state
            Component[] components = getComponents();
            for (Component component : components) {
                if (component instanceof JPanel) {
                    updateComponentState((JPanel) component, enabled);
                }
            }

            validate();
            repaint();
        });
    }

    /**
     * Updates component state in a panel based on enabled state
     */
    private void updateComponentState(JPanel panel, boolean enabled) {
        Component[] components = panel.getComponents();
        for (Component component : components) {
            if (component instanceof JLabel) {
                if (enabled) {
                    component.setForeground(StyleFactory.TEXT_COLOR);
                } else {
                    component.setForeground(StyleFactory.DISABLED_COLOR);
                }
            } else if (component instanceof JPanel) {
                updateComponentState((JPanel) component, enabled);
            }
        }
    }

    /**
     * Updates the outfit preview panel with obtained pieces
     */
    public void updateOutfitPreview(boolean[] obtainedPieces) {
        Component[] components = outfitPreviewPanel.getComponents();
        for (int i = 0; i < Math.min(components.length, obtainedPieces.length); i++) {
            if (components[i] instanceof JPanel) {
                JPanel piecePanel = (JPanel) components[i];
                Component[] piecePanelComponents = piecePanel.getComponents();
                for (Component c : piecePanelComponents) {
                    if (c instanceof JLabel && "Not obtained".equals(((JLabel) c).getText())) {
                        if (obtainedPieces[i]) {
                            ((JLabel) c).setText("Obtained");
                            ((JLabel) c).setForeground(new Color(0, 200, 0));
                        }
                    }
                }
            }
        }

        repaint();
    }

    /**
     * Saves panel settings to the configuration object
     */
    public void saveSettings() {
        config.setRoguesDenEnabled(enableRoguesDenCheckbox.isSelected());
        config.setStopAfterFullOutfit(stopAfterFullOutfitCheckbox.isSelected());
        config.setUseStaminaPotions(useStaminaPotionsCheckbox.isSelected());
        config.setMaxMazeRuns((Integer) mazeRunsSpinner.getValue());
        if (enableRoguesDenCheckbox.isSelected()) {
            config.setCurrentTargetName("Rogues' Den");
            // Optionally disable auto-progression to ensure manual selection is used
            config.setAutoProgressionEnabled(false);
        }
    }
}