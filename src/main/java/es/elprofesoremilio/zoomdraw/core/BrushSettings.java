package es.elprofesoremilio.zoomdraw.core;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import javafx.scene.paint.Color;

/**
 * Encapsulates the current settings for the annotation brush.
 */
public class BrushSettings {
    private Color currentColor = AppConfig.DEFAULT_COLOR;
    private double currentLineWidth = AppConfig.DEFAULT_LINE_WIDTH;
    private double currentOpacity = AppConfig.DEFAULT_OPACITY;

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
