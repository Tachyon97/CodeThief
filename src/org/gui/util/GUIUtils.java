package org.gui.util;

import org.gui.components.CheckBoxIcon;
import org.gui.components.CustomScrollBarUI;
import org.gui.components.CustomSliderUI;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GUIUtils {

    private GUIUtils() {
    }

    public static JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StyleFactory.LABEL_FONT);
        label.setForeground(StyleFactory.TEXT_COLOR);
        return label;
    }

    public static JLabel createStyledSubtextLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(StyleFactory.SMALL_FONT);
        label.setForeground(StyleFactory.TEXT_COLOR.darker());
        return label;
    }

    public static JCheckBox createStyledCheckBox() {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setBackground(StyleFactory.BG_DARK_COLOR);
        checkBox.setForeground(StyleFactory.TEXT_COLOR);
        checkBox.setIcon(new CheckBoxIcon(false));
        checkBox.setSelectedIcon(new CheckBoxIcon(true));
        checkBox.setOpaque(true);
        return checkBox;
    }

    public static JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(StyleFactory.LABEL_FONT);
        comboBox.setForeground(StyleFactory.TEXT_COLOR);
        comboBox.setBackground(StyleFactory.BG_MEDIUM_COLOR);

        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (isSelected) {
                    component.setBackground(StyleFactory.ACCENT_COLOR);
                    component.setForeground(Color.WHITE);
                } else {
                    component.setBackground(StyleFactory.BG_MEDIUM_COLOR);
                    component.setForeground(StyleFactory.TEXT_COLOR);
                }

                ((JComponent) component).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                return component;
            }
        });

        return comboBox;
    }

    public static JSlider createStyledSlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max, value);
        slider.setFont(StyleFactory.SMALL_FONT);
        slider.setForeground(StyleFactory.TEXT_COLOR);
        slider.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        slider.setUI(new CustomSliderUI(slider, StyleFactory.ACCENT_COLOR));
        return slider;
    }

    public static JScrollPane createStyledScrollPane(Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createLineBorder(StyleFactory.BORDER_COLOR));
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
        return scrollPane;
    }

    public static JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(StyleFactory.BUTTON_FONT);
        button.setForeground(Color.WHITE);
        button.setBackground(StyleFactory.ACCENT_COLOR);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(StyleFactory.ACCENT_HOVER_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(StyleFactory.ACCENT_COLOR);
            }
        });

        return button;
    }

    public static JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(StyleFactory.BG_MEDIUM_COLOR);

        Border titledBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(StyleFactory.BORDER_COLOR),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                StyleFactory.LABEL_FONT,
                StyleFactory.HEADER_COLOR
        );

        Border emptyBorder = BorderFactory.createEmptyBorder(6, 6, 6, 6);
        panel.setBorder(new CompoundBorder(titledBorder, emptyBorder));

        return panel;
    }

    public static JPanel createInfoPanel(String title, String description) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = createStyledLabel(title);
        titleLabel.setFont(StyleFactory.HEADER_FONT);
        titleLabel.setForeground(StyleFactory.HEADER_COLOR);

        JLabel descLabel = createStyledSubtextLabel(description);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(descLabel, BorderLayout.CENTER);

        return panel;
    }

    public static JPanel createExplanationPanel(String mainText, String subText) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        panel.setBorder(BorderFactory.createLineBorder(StyleFactory.BORDER_COLOR));

        JTextArea explanationText = new JTextArea(mainText);
        explanationText.setFont(StyleFactory.LABEL_FONT);
        explanationText.setForeground(StyleFactory.TEXT_COLOR);
        explanationText.setBackground(StyleFactory.BG_MEDIUM_COLOR);
        explanationText.setLineWrap(true);
        explanationText.setWrapStyleWord(true);
        explanationText.setEditable(false);
        explanationText.setBorder(BorderFactory.createEmptyBorder(8, 8, 5, 8));

        JLabel subLabel = createStyledSubtextLabel(subText);
        subLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));

        panel.add(explanationText, BorderLayout.CENTER);
        panel.add(subLabel, BorderLayout.SOUTH);

        return panel;
    }

    public static JComponent createSeparator(String text) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(StyleFactory.BG_DARK_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.ITALIC, 12));
        label.setForeground(StyleFactory.HEADER_COLOR);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 0, 5, 5);
        panel.add(label, gbc);

        JSeparator separator = new JSeparator();
        separator.setForeground(StyleFactory.BORDER_COLOR);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(separator, gbc);

        return panel;
    }

    public static void forceRender(JFrame frame) {
        frame.validate();
        frame.repaint();

        SwingUtilities.invokeLater(() -> {
            frame.validate();
            frame.repaint();

            Component[] components = frame.getContentPane().getComponents();
            for (Component component : components) {
                if (component instanceof JTabbedPane) {
                    JTabbedPane tabbedPane = (JTabbedPane) component;
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        Component tab = tabbedPane.getComponentAt(i);
                        tab.validate();
                        tab.repaint();
                    }
                }
            }
        });
    }
}