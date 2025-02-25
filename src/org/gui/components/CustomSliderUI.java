package org.gui.components;

import org.gui.util.StyleFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;

/**
 * Custom slider UI for better appearance
 */
public class CustomSliderUI extends BasicSliderUI {
    private final Color accentColor;

    public CustomSliderUI(JSlider slider, Color accentColor) {
        super(slider);
        this.accentColor = accentColor;
    }

    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle trackBounds = trackRect;
        int trackY = trackBounds.y + (trackBounds.height / 2) - 2;

        // Draw background track
        g2d.setColor(StyleFactory.BG_DARK_COLOR);
        g2d.fillRoundRect(trackBounds.x, trackY, trackBounds.width, 4, 4, 4);

        // Draw filled portion
        int thumbX = thumbRect.x + (thumbRect.width / 2);
        int filledWidth = thumbX - trackBounds.x;
        if (filledWidth > 0) {
            g2d.setColor(accentColor);
            g2d.fillRoundRect(trackBounds.x, trackY, filledWidth, 4, 4, 4);
        }

        g2d.dispose();
    }

    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw thumb
        int thumbX = thumbRect.x;
        int thumbY = thumbRect.y;
        int thumbWidth = thumbRect.width;
        int thumbHeight = thumbRect.height;

        if (slider.isEnabled()) {
            g2d.setColor(slider.getValueIsAdjusting() ? StyleFactory.ACCENT_HOVER_COLOR : accentColor);
            g2d.fillOval(thumbX, thumbY, thumbWidth, thumbHeight);

            // Add highlight
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillOval(thumbX + 2, thumbY + 2, thumbWidth - 4, thumbHeight / 2 - 2);
        } else {
            g2d.setColor(StyleFactory.DISABLED_COLOR);
            g2d.fillOval(thumbX, thumbY, thumbWidth, thumbHeight);
        }

        g2d.dispose();
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(16, 16);
    }
}