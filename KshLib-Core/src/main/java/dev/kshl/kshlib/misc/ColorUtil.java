package dev.kshl.kshlib.misc;

import java.awt.*;

public class ColorUtil {
    public static String toHexString(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static Color fromHexString(String hex) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        hex = hex.toUpperCase();
        if (!hex.matches("[A-F0-9]{6}")) throw new IllegalArgumentException("Hex must be #RRGGBB: '" + hex + "'");
        return new Color(Integer.parseInt(hex, 16));
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
