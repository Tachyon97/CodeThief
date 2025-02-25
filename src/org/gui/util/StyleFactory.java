package org.gui.util;

import java.awt.*;

/**
 * Central style definitions for the GUI
 */
public class StyleFactory {
    // Color scheme
    public static final Color BG_DARK_COLOR = new Color(20, 12, 28);     // Darker background
    public static final Color BG_MEDIUM_COLOR = new Color(30, 18, 40);   // Medium background
    public static final Color BG_LIGHT_COLOR = new Color(40, 25, 55);    // Lighter background for hover
    public static final Color TEXT_COLOR = new Color(230, 230, 230);     // Slightly off-white text
    public static final Color ACCENT_COLOR = new Color(0, 200, 170);     // Teal accent
    public static final Color ACCENT_HOVER_COLOR = new Color(0, 230, 180); // Brighter teal for hover
    public static final Color HEADER_COLOR = new Color(147, 112, 219);   // Medium purple for headers
    public static final Color BORDER_COLOR = new Color(60, 40, 70);      // Subtle border color
    public static final Color DISABLED_COLOR = new Color(80, 70, 90);    // Color for disabled elements

    // Fonts
    public static final Font HEADER_FONT = new Font("SansSerif", Font.BOLD, 16);
    public static final Font LABEL_FONT = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font SMALL_FONT = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);

    // Sizes
    public static final Dimension MAIN_WINDOW_SIZE = new Dimension(600, 650);

    private StyleFactory() {
        // Private constructor to prevent instantiation
    }
}