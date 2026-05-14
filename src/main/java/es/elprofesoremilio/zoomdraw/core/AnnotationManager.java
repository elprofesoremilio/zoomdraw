package es.elprofesoremilio.zoomdraw.core;

import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import es.elprofesoremilio.zoomdraw.utils.ScreenUtils;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;

/**
 * Orchestrates the annotation mode lifecycle.
 */
public class AnnotationManager {

    private AnnotationStage currentStage = null;
    private volatile boolean active = false;

    public void toggleAnnotationMode() {
        if (active) {
            stopAnnotationMode();
        } else {
            startAnnotationMode();
        }
    }

    public void startAnnotationMode() {
        if (active) return;

        Rectangle2D targetBounds = ScreenUtils.getScreenBoundsAtCursor();
        currentStage = new AnnotationStage(this, targetBounds);
        currentStage.show();

        // El "Martillo de Foco"
        triggerFocusHammer();
        active = true;
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