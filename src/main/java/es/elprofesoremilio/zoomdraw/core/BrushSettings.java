package es.elprofesoremilio.zoomdraw.core;

import javafx.scene.paint.Color;

/**
 * Encapsulates the current settings for the annotation brush.
 */
public class BrushSettings {
    private Color currentColor = Color.RED;
    private double currentLineWidth = 3.0;
    private final double defaultSemiTransparentOpacity = 0.4; // 40% de opacidad para el modo sobresubrayado

    public Color getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(Color currentColor) {
        this.currentColor = currentColor;
    }

    public double getCurrentLineWidth() {
        return currentLineWidth;
    }

    public void setCurrentLineWidth(double currentLineWidth) {
        this.currentLineWidth = currentLineWidth;
    }

    public double getDefaultSemiTransparentOpacity() {
        return defaultSemiTransparentOpacity;
    }
}
