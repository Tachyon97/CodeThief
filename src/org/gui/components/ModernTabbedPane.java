package org.gui.components;

import org.gui.util.StyleFactory;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class ModernTabbedPane extends JTabbedPane {
    private final Color selectedTabColor = StyleFactory.ACCENT_COLOR;
    private final Color hoverTabColor = new Color(0, 180, 150);
    private final Color unselectedTabColor = StyleFactory.BG_MEDIUM_COLOR;
    private final Color textColor = StyleFactory.TEXT_COLOR;
    private final Color selectedTextColor = Color.WHITE;
    private int hoverIndex = -1;

    public ModernTabbedPane() {
        super(JTabbedPane.TOP);
        setBackground(StyleFactory.BG_DARK_COLOR);
        setForeground(textColor);
        setFont(StyleFactory.LABEL_FONT);
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        setBorder(BorderFactory.createEmptyBorder());
        setUI(new ModernTabbedPaneUI());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoverIndex = -1;
                repaint();
            }
        });

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int oldHoverIndex = hoverIndex;
                hoverIndex = indexAtLocation(e.getX(), e.getY());
                if (oldHoverIndex != hoverIndex) {
                    repaint();
                }
            }
        });
    }

    private class ModernTabbedPaneUI extends BasicTabbedPaneUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabInsets = new Insets(8, 12, 8, 12);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            contentBorderInsets = new Insets(1, 0, 0, 0);
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(selectedTabColor);
            int x = tabPane.getX();
            int y = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight) - 1;
            int w = tabPane.getWidth();
            g2d.fillRect(x, y, w, 2);
            g2d.dispose();
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color tabColor;
            if (isSelected) {
                tabColor = selectedTabColor;
            } else if (tabIndex == hoverIndex) {
                tabColor = hoverTabColor;
            } else {
                tabColor = unselectedTabColor;
            }

            RoundRectangle2D tabShape = new RoundRectangle2D.Float(x, y, w, h, 8, 8);
            g2d.setColor(tabColor);
            g2d.fill(tabShape);
            g2d.dispose();
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(font);

            if (isSelected) {
                g2d.setColor(selectedTextColor);
            } else {
                g2d.setColor(textColor);
            }

            int textWidth = metrics.stringWidth(title);
            int textX = textRect.x + (textRect.width - textWidth) / 2;
            int textY = textRect.y + ((textRect.height - metrics.getHeight()) / 2) + metrics.getAscent();
            g2d.drawString(title, textX, textY);
            g2d.dispose();
        }
    }

    public void addTab(String title, Component component) {
        super.addTab(title, null, component);
    }

    public void addTab(String title, Component component, String tip) {
        super.addTab(title, null, component, tip);
    }
}