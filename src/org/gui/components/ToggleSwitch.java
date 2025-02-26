package org.gui.components;

import org.gui.util.StyleFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Modern toggle switch component that provides ON/OFF functionality
 * with animation and custom styling
 */
public class ToggleSwitch extends JPanel {
    private boolean selected;
    private boolean animated;
    private float position;
    private final int width = 80;
    private final int height = 30;
    private final Color selectedColor = StyleFactory.ACCENT_COLOR;
    private final Color disabledColor = StyleFactory.DISABLED_COLOR;
    private final Color backgroundColor = StyleFactory.BG_DARK_COLOR;
    private final Color knobColor = Color.WHITE;
    private final List<ChangeListener> changeListeners = new ArrayList<>();
    private Timer animationTimer;

    /**
     * Creates a new toggle switch component
     */
    public ToggleSwitch() {
        setPreferredSize(new Dimension(width, height));
        setBackground(StyleFactory.BG_MEDIUM_COLOR);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        position = 0.0f;
        animated = true;

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    setSelected(!selected);
                }
            }
        });
    }

    /**
     * Sets whether the toggle is selected (ON)
     *
     * @param selected The selected state
     */
    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            fireStateChanged();
            animateToggle();
        }
    }

    /**
     * Gets whether the toggle is selected (ON)
     *
     * @return The selected state
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets whether the toggle should animate when changing state
     *
     * @param animated Whether to animate
     */
    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    /**
     * Adds a ChangeListener to the toggle
     *
     * @param listener The listener to add
     */
    public void addChangeListener(ChangeListener listener) {
        changeListeners.add(listener);
    }

    /**
     * Removes a ChangeListener from the toggle
     *
     * @param listener The listener to remove
     */
    public void removeChangeListener(ChangeListener listener) {
        changeListeners.remove(listener);
    }

    /**
     * Notifies all change listeners of a state change
     */
    protected void fireStateChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : changeListeners) {
            listener.stateChanged(event);
        }
    }

    /**
     * Animates the toggle between states
     */
    private void animateToggle() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }

        if (!animated) {
            position = selected ? 1.0f : 0.0f;
            repaint();
            return;
        }

        final float targetPosition = selected ? 1.0f : 0.0f;
        final float initialPosition = position;
        final float distance = Math.abs(targetPosition - initialPosition);
        final int steps = (int) (distance * 10); // Number of animation steps

        if (steps == 0) {
            position = targetPosition;
            repaint();
            return;
        }

        final int animationDuration = 200; // ms
        final int delay = animationDuration / steps;

        animationTimer = new Timer(delay, e -> {
            // Calculate position based on animation progress
            if (selected) {
                position += 0.1f;
                if (position >= 1.0f) {
                    position = 1.0f;
                    animationTimer.stop();
                }
            } else {
                position -= 0.1f;
                if (position <= 0.0f) {
                    position = 0.0f;
                    animationTimer.stop();
                }
            }
            repaint();
        });

        animationTimer.start();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate dimensions and positions
        int width = getWidth();
        int height = getHeight();
        int toggleWidth = width - 2;
        int toggleHeight = height - 8;
        int arcSize = toggleHeight;

        // Background track based on state
        if (isEnabled()) {
            Color trackColor = interpolateColor(backgroundColor, selectedColor, position);
            g2d.setColor(trackColor);
        } else {
            g2d.setColor(disabledColor);
        }

        // Draw rounded background track
        g2d.fillRoundRect(1, 4, toggleWidth, toggleHeight, arcSize, arcSize);

        // Draw ON/OFF text
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.setColor(Color.WHITE);

        // Calculate text opacity based on position
        int onOpacity = (int) (position * 255);
        int offOpacity = 255 - onOpacity;

        // Draw ON text
        g2d.setColor(new Color(255, 255, 255, onOpacity));
        g2d.drawString("ON", 15, height / 2 + 5);

        // Draw OFF text
        g2d.setColor(new Color(255, 255, 255, offOpacity));
        g2d.drawString("OFF", width - 40, height / 2 + 5);

        // Calculate knob position
        int knobDiameter = toggleHeight - 4;
        int knobX = 3 + (int) ((toggleWidth - knobDiameter - 4) * position);
        int knobY = 6;

        // Draw knob
        g2d.setColor(knobColor);
        g2d.fillOval(knobX, knobY, knobDiameter, knobDiameter);

        // Draw knob shadow
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.drawOval(knobX, knobY, knobDiameter, knobDiameter);

        g2d.dispose();
    }

    /**
     * Interpolates between two colors based on a position (0.0 to 1.0)
     */
    private Color interpolateColor(Color from, Color to, float position) {
        float[] fromHSB = Color.RGBtoHSB(from.getRed(), from.getGreen(), from.getBlue(), null);
        float[] toHSB = Color.RGBtoHSB(to.getRed(), to.getGreen(), to.getBlue(), null);

        float h = fromHSB[0] + (toHSB[0] - fromHSB[0]) * position;
        float s = fromHSB[1] + (toHSB[1] - fromHSB[1]) * position;
        float b = fromHSB[2] + (toHSB[2] - fromHSB[2]) * position;

        return Color.getHSBColor(h, s, b);
    }
}