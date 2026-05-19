package es.elprofesoremilio.zoomdraw.core;

import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import es.elprofesoremilio.zoomdraw.ui.HelpWindow;
import es.elprofesoremilio.zoomdraw.utils.ScreenUtils;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;

/**
 * Orchestrates the annotation mode lifecycle.
 */
public class AnnotationManager {

    private AnnotationStage currentStage = null;
    private HelpWindow helpWindow = null;
    private volatile boolean active = false;
    private final BrushSettings brushSettings; // Use the new BrushSettings class

    public AnnotationManager() {
        this.brushSettings = new BrushSettings();
    }

    public double getCurrentOpacity() {
        return brushSettings.getCurrentOpacity();
    }

    public void setCurrentOpacity(double opacity) {
        brushSettings.setCurrentOpacity(opacity);
    }

    public javafx.scene.paint.Color getCurrentColor() {
        return brushSettings.getCurrentColor();
    }

    public void setCurrentColor(javafx.scene.paint.Color color) {
        this.brushSettings.setCurrentColor(color);
    }

    public double getCurrentLineWidth() {
        return brushSettings.getCurrentLineWidth();
    }

    public void setCurrentLineWidth(double width) {
        this.brushSettings.setCurrentLineWidth(width);
    }

    public void toggleHelpWindow() {
        Platform.runLater(() -> {
            if (helpWindow == null) {
                helpWindow = new HelpWindow();
            }
            if (helpWindow.isShowing()) {
                helpWindow.hide();
            } else {
                helpWindow.show();
                
                Rectangle2D targetBounds;
                if (active && currentStage != null) {
                    targetBounds = new Rectangle2D(currentStage.getX(), currentStage.getY(), currentStage.getWidth(), currentStage.getHeight());
                } else {
                    targetBounds = ScreenUtils.getScreenBoundsAtCursor();
                }
                
                double padding = 30.0; // Un poquito separado del borde
                double x = targetBounds.getMaxX() - helpWindow.getWidth() - padding;
                double y = targetBounds.getMinY() + padding;
                
                helpWindow.setX(x);
                helpWindow.setY(y);
                helpWindow.toFront();
            }
        });
    }

    public void bringHelpWindowToFront() {
        Platform.runLater(() -> {
            if (helpWindow != null && helpWindow.isShowing()) {
                helpWindow.toFront();
            }
        });
    }

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
        if (active)
            return;

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
                bringHelpWindowToFront();
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

    public boolean isTextModeActive() {
        return currentStage != null && currentStage.isTextModeActive();
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
                    bringHelpWindowToFront();
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}