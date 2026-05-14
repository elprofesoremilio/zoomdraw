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
            int contador=0;
            System.out.println(contador++);

            Rectangle2D targetBounds = ScreenUtils.getScreenBoundsAtCursor();
            System.out.println(contador++);

            // La captura de pantalla debe ser ANTES de mostrar la ventana
            final WritableImage background = ScreenUtils.captureScreen(targetBounds);
            System.out.println(contador++);

            Platform.runLater(() -> {
            currentStage = new AnnotationStage(this, targetBounds, background);
            System.out.println("runlater");

            // IMPORTANTE: Primero configuramos y luego mostramos
            // Forzar visibilidad
            currentStage.setOpacity(1.0);
            currentStage.show();
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