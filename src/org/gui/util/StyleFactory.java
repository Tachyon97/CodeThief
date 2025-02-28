package org.gui.util;

import java.awt.*;

public class StyleFactory {
    public static final Color BG_DARK_COLOR = new Color(20, 12, 28);
    public static final Color BG_MEDIUM_COLOR = new Color(30, 18, 40);
    public static final Color BG_LIGHT_COLOR = new Color(40, 25, 55);
    public static final Color TEXT_COLOR = new Color(230, 230, 230);
    public static final Color ACCENT_COLOR = new Color(0, 200, 170);
    public static final Color ACCENT_HOVER_COLOR = new Color(0, 230, 180);
    public static final Color HEADER_COLOR = new Color(147, 112, 219);
    public static final Color BORDER_COLOR = new Color(60, 40, 70);
    public static final Color DISABLED_COLOR = new Color(80, 70, 90);

    public static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 16);
    public static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);

    public static final Dimension MAIN_WINDOW_SIZE = new Dimension(600, 620);

    private StyleFactory() {
    }
}