package org.gui.components;

import org.gui.util.StyleFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Custom scroll bar UI for better appearance
 */
public class CustomScrollBarUI extends BasicScrollBarUI {
    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = StyleFactory.ACCENT_COLOR;
        this.thumbDarkShadowColor = StyleFactory.BG_DARK_COLOR;
        this.thumbHighlightColor = StyleFactory.ACCENT_COLOR;
        this.thumbLightShadowColor = StyleFactory.ACCENT_COLOR;
        this.trackColor = StyleFactory.BG_MEDIUM_COLOR;
        this.trackHighlightColor = StyleFactory.BG_MEDIUM_COLOR;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(StyleFactory.ACCENT_COLOR);
        g2d.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1,
                thumbBounds.width - 2, thumbBounds.height - 2, 7, 7);

        g2d.dispose();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setColor(StyleFactory.BG_MEDIUM_COLOR);
        g2d.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        g2d.dispose();
    }
}