package es.elprofesoremilio.zoomdraw.core;

import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import es.elprofesoremilio.zoomdraw.utils.ScreenUtils;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;

/**
 * Orchestrates the annotation mode lifecycle.
 */
public class AnnotationManager {

    private AnnotationStage currentStage = null;
    private volatile boolean active = false;

    // --- ESTADO GUARDADO DEL PINCEL (v0.2) ---
    private javafx.scene.paint.Color currentColor = javafx.scene.paint.Color.RED;
    private double currentLineWidth = 3.0;
    private final double defaultSemiTransparentOpacity = 0.4; // 40% de opacidad para el modo sobresubrayado

    public double getDefaultSemiTransparentOpacity() { return defaultSemiTransparentOpacity; }
    public javafx.scene.paint.Color getCurrentColor() { return currentColor; }
    public void setCurrentColor(javafx.scene.paint.Color color) { this.currentColor = color; }

    public double getCurrentLineWidth() { return currentLineWidth; }
    public void setCurrentLineWidth(double width) { this.currentLineWidth = width; }

    public void toggleAnnotationMode() {
        // Forzamos que todo el cambio de estado ocurra en el hilo de JavaFX
        Platform.runLater(() -> {
            if (active) {
                stopAnnotationMode();
            } else {
                startAnnotationMode();
            }
        });
    }

    public void startAnnotationMode() {
        if (active) return;

        System.out.println("Starting annotation mode");

        try {
            // Obtenemos las coordenadas (esto sí puede ir fuera del hilo)
            Rectangle2D targetBounds = ScreenUtils.getScreenBoundsAtCursor();

            Platform.runLater(() -> {
                // 1. Capturamos la pantalla DENTRO del hilo de JavaFX (es casi instantáneo)
                final WritableImage background = ScreenUtils.captureScreen(targetBounds);

                // 2. Creamos la ventana
                currentStage = new AnnotationStage(this, targetBounds, background);

                // 3. Forzamos visibilidad y coordenadas (El hack de X11)
                currentStage.setOpacity(1.0);
                currentStage.setX(targetBounds.getMinX());
                currentStage.setY(targetBounds.getMinY());

                currentStage.show();

                currentStage.setX(targetBounds.getMinX());
                currentStage.setY(targetBounds.getMinY());

                currentStage.toFront();
                triggerFocusHammer();
                active = true;
            });

        } catch (Exception e) {
            System.out.println("Error starting annotation mode: " + e.getMessage());
        }
    }

    public void stopAnnotationMode() {
        if (currentStage != null) {
            currentStage.close();
            currentStage = null;
        }
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isStageFocused() {
        return currentStage != null && currentStage.isFocused();
    }

    private void triggerFocusHammer() {
        new Thread(() -> {
            try {
                Thread.sleep(50);
                Platform.runLater(() -> {
                    if (currentStage != null) {
                        currentStage.setIconified(true);
                        currentStage.setIconified(false);
                        currentStage.toFront();
                        currentStage.requestFocus();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}