package es.elprofesoremilio.zoomdraw.ui.controllers;

import javafx.scene.canvas.GraphicsContext;

public class ZoomPanController {
    private double zoomFactor = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;

    public ZoomPanController() {
    }

    public double getZoomFactor() {
        return zoomFactor;
    }

    public void setZoomFactor(double zoomFactor) {
        this.zoomFactor = zoomFactor;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public void zoomAtCursor(double delta, double mouseX, double mouseY) {
        if (delta == 0) return;

        double previousZoomFactor = zoomFactor;

        if (delta > 0) {
            zoomFactor = Math.min(8.0, zoomFactor * 1.2);
        } else {
            zoomFactor = Math.max(1.0, zoomFactor / 1.2);
        }

        if (zoomFactor == 1.0) {
            offsetX = 0.0;
            offsetY = 0.0;
        } else {
            double origX = (mouseX - offsetX) / previousZoomFactor;
            double origY = (mouseY - offsetY) / previousZoomFactor;

            offsetX = mouseX - origX * zoomFactor;
            offsetY = mouseY - origY * zoomFactor;
        }
    }

    public void reset() {
        zoomFactor = 1.0;
        offsetX = 0.0;
        offsetY = 0.0;
    }

    public void applyTransform(GraphicsContext gc) {
        gc.translate(offsetX, offsetY);
        gc.scale(zoomFactor, zoomFactor);
    }
}
