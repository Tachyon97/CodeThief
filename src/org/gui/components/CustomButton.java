package org.gui.components;

import org.gui.util.StyleFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom button component with various styles and animations
 */
public class CustomButton extends JButton {
    // Button styles
    public enum ButtonStyle {
        DEFAULT,    // Standard rectangular button
        ROUND,      // Circular button
        PILL,       // Rounded rectangle button
        FLAT        // Flat button with hover effect
    }

    // Button colors
    private Color defaultColor = new Color(0, 153, 102);      // Default green
    private Color hoverColor = new Color(0, 180, 120);        // Lighter green for hover
    private Color pressedColor = new Color(0, 120, 80);       // Darker green for pressed
    private Color textColor = Color.WHITE;                    // White text
    private Color disabledColor = new Color(100, 100, 100);   // Gray for disabled state

    // Style properties
    private final ButtonStyle style;
    private int cornerRadius = 10;  // For rounded corners (PILL style)

    /**
     * Creates a custom button with the default style
     *
     * @param text The button text
     */
    public CustomButton(String text) {
        this(text, ButtonStyle.DEFAULT);
    }

    /**
     * Creates a custom button with the specified style
     *
     * @param text The button text
     * @param style The button style
     */
    public CustomButton(String text, ButtonStyle style) {
        super(text);
        this.style = style;

        // Set up basic properties
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFont(StyleFactory.BUTTON_FONT);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Set preferred size based on style
        switch (style) {
            case ROUND:
                // Default size for round button is square
                setPreferredSize(new Dimension(40, 40));
                break;
            case PILL:
            case FLAT:
            case DEFAULT:
                // Default size for other buttons
                setPreferredSize(new Dimension(120, 40));
                break;
        }

        // Add hover and click effects
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled()) {
                    repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    repaint();
                }
            }
        });
    }

    /**
     * Sets the button colors
     *
     * @param defaultColor Normal state color
     * @param hoverColor Hover state color
     * @param pressedColor Pressed state color
     * @return This button for method chaining
     */
    public CustomButton setColors(Color defaultColor, Color hoverColor, Color pressedColor) {
        this.defaultColor = defaultColor;
        this.hoverColor = hoverColor;
        this.pressedColor = pressedColor;
        repaint();
        return this;
    }

    /**
     * Sets the text color
     *
     * @param textColor The color for the button text
     * @return This button for method chaining
     */
    public CustomButton setTextColor(Color textColor) {
        this.textColor = textColor;
        repaint();
        return this;
    }

    /**
     * Sets the corner radius for PILL style buttons
     *
     * @param radius Corner radius in pixels
     * @return This button for method chaining
     */
    public CustomButton setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
        return this;
    }

    /**
     * Factory method to create a start button
     *
     * @param text Button text
     * @return A styled start button
     */
    public static CustomButton createStartButton(String text) {
        return new CustomButton(text, ButtonStyle.PILL)
                .setColors(
                        new Color(0, 153, 102),   // Green
                        new Color(0, 180, 120),   // Lighter green
                        new Color(0, 120, 80)     // Darker green
                )
                .setTextColor(Color.WHITE);
    }

    /**
     * Factory method to create a help button
     *
     * @return A styled help button with "?" text
     */
    public static CustomButton createHelpButton() {
        return new CustomButton("?", ButtonStyle.ROUND)
                .setColors(
                        new Color(0, 153, 153),   // Teal
                        new Color(0, 180, 180),   // Lighter teal
                        new Color(0, 120, 120)    // Darker teal
                )
                .setTextColor(Color.WHITE);
    }

    /**
     * Factory method to create a cancel/close button
     *
     * @param text Button text
     * @return A styled cancel button
     */
    public static CustomButton createCancelButton(String text) {
        return new CustomButton(text, ButtonStyle.FLAT)
                .setColors(
                        new Color(150, 150, 150),   // Gray
                        new Color(180, 180, 180),   // Lighter gray
                        new Color(120, 120, 120)    // Darker gray
                )
                .setTextColor(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Determine current color based on button state
        Color currentColor;
        if (!isEnabled()) {
            currentColor = disabledColor;
        } else if (getModel().isPressed()) {
            currentColor = pressedColor;
        } else if (getModel().isRollover()) {
            currentColor = hoverColor;
        } else {
            currentColor = defaultColor;
        }

        g2d.setColor(currentColor);

        // Draw the button shape based on style
        switch (style) {
            case ROUND:
                g2d.fillOval(0, 0, width, height);
                break;

            case PILL:
                g2d.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));
                break;

            case FLAT:
                if (getModel().isRollover() || getModel().isPressed()) {
                    g2d.fill(new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius));
                }
                break;

            case DEFAULT:
            default:
                g2d.fillRect(0, 0, width, height);
                break;
        }

        // Draw the text
        FontMetrics metrics = g2d.getFontMetrics(getFont());
        int textX = (width - metrics.stringWidth(getText())) / 2;
        int textY = ((height - metrics.getHeight()) / 2) + metrics.getAscent();

        g2d.setColor(textColor);
        g2d.setFont(getFont());
        g2d.drawString(getText(), textX, textY);

        g2d.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        // For ROUND style, ensure width and height are equal
        if (style == ButtonStyle.ROUND) {
            Dimension size = super.getPreferredSize();
            int max = Math.max(size.width, size.height);
            return new Dimension(max, max);
        }
        return super.getPreferredSize();
    }
}