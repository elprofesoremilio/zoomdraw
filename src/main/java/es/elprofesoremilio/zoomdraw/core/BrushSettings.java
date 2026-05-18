package es.elprofesoremilio.zoomdraw.core;

import javafx.scene.paint.Color;

/**
 * Encapsulates the current settings for the annotation brush.
 */
public class BrushSettings {
    private Color currentColor = Color.RED;
    private double currentLineWidth = 3.0;
    private double currentOpacity = 0.2; // 20% de opacidad para el modo sobresubrayado

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

    public double getCurrentOpacity() {
        return currentOpacity;
    }

    public void setCurrentOpacity(double currentOpacity) {
        this.currentOpacity = currentOpacity;
    }
}
