package es.elprofesoremilio.zoomdraw.core;

import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import es.elprofesoremilio.zoomdraw.ui.HelpWindow;
import es.elprofesoremilio.zoomdraw.utils.ScreenUtils;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import es.elprofesoremilio.zoomdraw.utils.AppLogger;
import es.elprofesoremilio.zoomdraw.commands.CommandHistory;

/**
 * Orchestrates the annotation mode lifecycle.
 */
public class AnnotationManager {

    private AnnotationStage currentStage = null;
    private HelpWindow helpWindow = null;
    private volatile boolean active = false;
    private es.elprofesoremilio.zoomdraw.ui.LaserPointerStage laserStage = null;
    private volatile boolean laserActive = false;
    private final BrushSettings brushSettings; // Use the new BrushSettings class
    private final CommandHistory globalHistory;

    public AnnotationManager() {
        this.brushSettings = new BrushSettings();
        this.globalHistory = new CommandHistory();
    }

    public CommandHistory getGlobalHistory() {
        return globalHistory;
    }

    public void startLaserMode() {
        if (laserActive || active) return;
        AppLogger.log("Starting laser pointer mode");
        Platform.runLater(() -> {
            try {
                laserStage = new es.elprofesoremilio.zoomdraw.ui.LaserPointerStage();
                java.awt.Point cursor = java.awt.MouseInfo.getPointerInfo().getLocation();
                laserStage.updatePosition(cursor.x, cursor.y);
                laserStage.show();
                laserActive = true;
            } catch (Exception e) {
                AppLogger.logError("Error starting laser pointer mode: " + e.getMessage());
            }
        });
    }

    public void stopLaserMode() {
        Platform.runLater(() -> {
            if (laserStage != null) {
                laserStage.close();
                laserStage = null;
            }
            laserActive = false;
        });
    }

    public void toggleLaserMode() {
        if (active) return; // Do nothing if in annotation mode
        if (laserActive) {
            stopLaserMode();
        } else {
            startLaserMode();
        }
    }

    public boolean isLaserActive() {
        return laserActive;
    }

    public void requestLaserRedraw() {
        Platform.runLater(() -> {
            if (laserStage != null) {
                laserStage.redraw();
            }
        });
    }

    public void updateLaserPosition(double x, double y) {
        Platform.runLater(() -> {
            if (laserStage != null && laserActive) {
                laserStage.updatePosition(x, y);
            }
        });
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
                helpWindow = new HelpWindow(this);
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

        // Desactivar puntero láser si está activo
        if (laserActive) {
            stopLaserMode();
        }

        AppLogger.log("Starting annotation mode");

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
            AppLogger.logError("Error starting annotation mode: " + e.getMessage());
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