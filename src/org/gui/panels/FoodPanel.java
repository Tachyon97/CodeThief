package org.gui.panels;

import org.ScriptConfiguration;
import org.gui.util.GUIUtils;
import org.gui.util.StyleFactory;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for food and health settings
 */
public class FoodPanel extends JPanel {
    private final ScriptConfiguration config;

    // GUI components
    private JSlider healthThresholdSlider;
    private JList<String> foodList;
    private JComboBox<String> primaryFoodSelector;
    private JLabel valueLabel;

    /**
     * Creates a new food and health panel
     *
     * @param config The script configuration
     */
    public FoodPanel(ScriptConfiguration config) {
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

        JLabel titleLabel = GUIUtils.createStyledLabel("Food & Health Settings");
        titleLabel.setFont(StyleFactory.HEADER_FONT);
        titleLabel.setForeground(StyleFactory.HEADER_COLOR);

        JLabel descLabel = GUIUtils.createStyledSubtextLabel(
                "Configure how the script manages your health and food preferences."
        );

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(descLabel, BorderLayout.CENTER);
        add(headerPanel);
        add(Box.createVerticalStrut(15));

        // Health settings section
        JPanel healthPanel = createHealthPanel();
        add(healthPanel);
        add(Box.createVerticalStrut(15));

        // Food selection section
        JPanel foodSelectionPanel = createFoodSelectionPanel();
        add(foodSelectionPanel);

        // Add preview button
        JPanel previewPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        previewPanel.setBackground(StyleFactory.BG_DARK_COLOR);

        JButton previewButton = GUIUtils.createStyledButton("Preview Food Priority");
        previewButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        previewButton.setPreferredSize(new Dimension(160, 30));
        previewButton.addActionListener(e -> showFoodPriorityPreview());
        previewPanel.add(previewButton);

        add(Box.createVerticalStrut(10));
        add(previewPanel);
        add(Box.createVerticalGlue());
    }

    /**
     * Creates the health settings panel
     */
    private JPanel createHealthPanel() {
        JPanel healthPanel = GUIUtils.createSectionPanel("Health Settings");

        // Add health threshold slider with custom styling
        JPanel sliderPanel = new JPanel(new BorderLayout(0, 10));
        sliderPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);

        JLabel healthLabel = GUIUtils.createStyledLabel("Health Threshold:");
        healthLabel.setToolTipText("Script will eat food when health drops below this percentage");
        sliderPanel.add(healthLabel, BorderLayout.NORTH);

        healthThresholdSlider = GUIUtils.createStyledSlider(1, 99, 50);
        healthThresholdSlider.setMajorTickSpacing(20);
        healthThresholdSlider.setMinorTickSpacing(5);
        healthThresholdSlider.setPaintTicks(true);
        healthThresholdSlider.setPaintLabels(true);
        healthThresholdSlider.setToolTipText("Lower values are more risky but may be more efficient");

        // Add live value display
        JPanel sliderWithValuePanel = new JPanel(new BorderLayout(5, 0));
        sliderWithValuePanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);

        valueLabel = GUIUtils.createStyledLabel("50%");
        valueLabel.setPreferredSize(new Dimension(40, 20));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        healthThresholdSlider.addChangeListener(e -> {
            JSlider source = (JSlider) e.getSource();
            valueLabel.setText(source.getValue() + "%");
        });

        sliderWithValuePanel.add(healthThresholdSlider, BorderLayout.CENTER);
        sliderWithValuePanel.add(valueLabel, BorderLayout.EAST);
        sliderPanel.add(sliderWithValuePanel, BorderLayout.CENTER);

        JLabel sliderDescription = GUIUtils.createStyledSubtextLabel(
                "Script will eat food when health drops below this percentage"
        );
        sliderPanel.add(sliderDescription, BorderLayout.SOUTH);

        healthPanel.add(sliderPanel);

        return healthPanel;
    }

    /**
     * Creates the food selection panel
     */
    private JPanel createFoodSelectionPanel() {
        JPanel foodSelectionPanel = GUIUtils.createSectionPanel("Food Selection");

        // Primary food selector
        JPanel primaryFoodPanel = new JPanel(new BorderLayout(0, 5));
        primaryFoodPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        primaryFoodPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel primaryFoodLabel = GUIUtils.createStyledLabel("Primary Food:");
        primaryFoodLabel.setToolTipText("Script will try to use this food first");
        primaryFoodPanel.add(primaryFoodLabel, BorderLayout.NORTH);

        String[] foodOptions = {"Monkfish", "Lobster", "Swordfish", "Shark", "Tuna", "Salmon", "Cake", "Bread", "Wine"};
        primaryFoodSelector = GUIUtils.createStyledComboBox(foodOptions);
        primaryFoodSelector.setToolTipText("Select your preferred primary food item");
        primaryFoodPanel.add(primaryFoodSelector, BorderLayout.CENTER);

        // Available food list
        JPanel availableFoodPanel = new JPanel(new BorderLayout(0, 5));
        availableFoodPanel.setBackground(StyleFactory.BG_MEDIUM_COLOR);

        JLabel availableFoodLabel = GUIUtils.createStyledLabel("Additional Foods (Select Multiple):");
        availableFoodLabel.setToolTipText("Script will use these foods as backup options");
        availableFoodPanel.add(availableFoodLabel, BorderLayout.NORTH);

        foodList = new JList<>(foodOptions);
        foodList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        foodList.setFont(StyleFactory.LABEL_FONT);
        foodList.setForeground(StyleFactory.TEXT_COLOR);
        foodList.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        foodList.setSelectionBackground(StyleFactory.ACCENT_COLOR);
        foodList.setSelectionForeground(Color.WHITE);
        foodList.setToolTipText("Hold Ctrl to select multiple items");

        // Pre-select a few common food items
        foodList.setSelectedIndices(new int[]{0, 1, 3}); // Monkfish, Lobster, Shark

        JScrollPane foodScrollPane = GUIUtils.createStyledScrollPane(foodList);
        foodScrollPane.setPreferredSize(new Dimension(300, 120));
        availableFoodPanel.add(foodScrollPane, BorderLayout.CENTER);

        // Add explanation
        JLabel foodExplanation = GUIUtils.createStyledSubtextLabel(
                "Script will try foods in order of selection priority"
        );
        availableFoodPanel.add(foodExplanation, BorderLayout.SOUTH);

        // Add components to food selection panel
        JPanel foodComponents = new JPanel();
        foodComponents.setLayout(new BoxLayout(foodComponents, BoxLayout.Y_AXIS));
        foodComponents.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        foodComponents.add(primaryFoodPanel);
        foodComponents.add(availableFoodPanel);

        foodSelectionPanel.add(foodComponents);

        return foodSelectionPanel;
    }

    /**
     * Shows a preview of the food priority list
     */
    private void showFoodPriorityPreview() {
        // Build food priority list
        List<String> foodPriorities = new ArrayList<>();

        // Add primary food
        String primaryFood = (String) primaryFoodSelector.getSelectedItem();
        foodPriorities.add(primaryFood);

        // Add secondary foods
        for (String food : foodList.getSelectedValuesList()) {
            if (!food.equals(primaryFood) && !foodPriorities.contains(food)) {
                foodPriorities.add(food);
            }
        }

        // Show food priority dialog
        StringBuilder message = new StringBuilder("Food Priority Order:\n\n");
        for (int i = 0; i < foodPriorities.size(); i++) {
            message.append(i + 1).append(". ").append(foodPriorities.get(i)).append("\n");
        }

        // Show generic items that will always be included
        message.append("\nGeneric items (always included):\n");
        message.append("- Food\n");
        message.append("- Brew\n");
        message.append("- Potion");

        JOptionPane.showMessageDialog(this,
                message.toString(),
                "Food Priority Preview",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Saves panel settings to the configuration object
     */
    public void saveSettings() {
        config.setHealthThreshold(healthThresholdSlider.getValue());

        // Save food settings
        List<String> selectedFoods = new ArrayList<>();

        // Add primary food
        String primaryFood = (String) primaryFoodSelector.getSelectedItem();
        selectedFoods.add(primaryFood);

        // Add additional selected foods
        for (String food : foodList.getSelectedValuesList()) {
            if (!selectedFoods.contains(food)) {
                selectedFoods.add(food);
            }
        }

        // Always include some generic terms
        selectedFoods.add("Food");
        selectedFoods.add("Brew");
        selectedFoods.add("Potion");

        config.setFoodItems(selectedFoods);
    }

    /**
     * Validates panel settings and returns any errors
     *
     * @return Error message or null if valid
     */
    public String validateSettings() {
        if (foodList.getSelectedIndices().length == 0) {
            return "Please select at least one backup food option";
        }
        return null;
    }
}