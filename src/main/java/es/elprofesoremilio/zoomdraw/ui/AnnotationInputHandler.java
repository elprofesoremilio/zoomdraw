package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

/**
 * Handles input events (keyboard and scroll) for the AnnotationStage,
 * delegating actions to the AnnotationManager and updating the DrawingCanvas.
 */
public class AnnotationInputHandler {

    private final AnnotationManager manager;
    private final DrawingCanvas drawingCanvas;

    public AnnotationInputHandler(AnnotationManager manager, DrawingCanvas drawingCanvas) {
        this.manager = manager;
        this.drawingCanvas = drawingCanvas;
    }

    public void attach(Scene scene) {
        scene.setOnScroll(this::handleScroll);
        scene.setOnKeyPressed(this::handleKeyPressed);
    }

    private void handleScroll(ScrollEvent event) {
        if (event.isControlDown()) {
            double newWidth = manager.getCurrentLineWidth();
            if (event.getDeltaY() > 0) {
                // Scroll up: increase line width
                newWidth = Math.min(50.0, newWidth + 2.0);
            } else if (event.getDeltaY() < 0) {
                // Scroll down: decrease line width
                newWidth = Math.max(1.0, newWidth - 2.0);
            }
            manager.setCurrentLineWidth(newWidth);
            drawingCanvas.updateBrushSettings();
            event.consume();
        }
    }

    private void handleKeyPressed(javafx.scene.input.KeyEvent event) {
        KeyCode code = event.getCode();
        boolean isShift = event.isShiftDown();

        // EXIT
        if (code == KeyCode.ESCAPE) {
            manager.stopAnnotationMode();
            event.consume();
            return;
        }

        // --- COLOR LOGIC ---
        Color newBaseColor = null;
        switch (code) {
            case R: newBaseColor = Color.RED; break;
            case G: newBaseColor = Color.GREEN; break;
            case B: newBaseColor = Color.BLUE; break;
            case Y: newBaseColor = Color.YELLOW; break;
            case O: newBaseColor = Color.ORANGE; break;
            case P: newBaseColor = Color.MAGENTA; break; // Pink/Purple
            case K: newBaseColor = Color.BLACK; break;
            case W: newBaseColor = Color.WHITE; break;
            default: break;
        }

        if (newBaseColor != null) {
            // If Shift is pressed, apply default semi-transparent opacity. Otherwise, 1.0 (opaque).
            double alpha = isShift ? manager.getDefaultSemiTransparentOpacity() : 1.0;
            Color finalColor = new Color(newBaseColor.getRed(), newBaseColor.getGreen(), newBaseColor.getBlue(), alpha);

            manager.setCurrentColor(finalColor);
            drawingCanvas.updateBrushSettings();
            event.consume();
            return;
        }

        // --- LINE WIDTH LOGIC (Up / Down arrows, +/-) ---
        if (code == KeyCode.UP || code == KeyCode.PLUS || code == KeyCode.ADD) {
            double newWidth = Math.min(50.0, manager.getCurrentLineWidth() + 2.0);
            manager.setCurrentLineWidth(newWidth);
            drawingCanvas.updateBrushSettings();
            event.consume();
        }
        else if (code == KeyCode.DOWN || code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
            double newWidth = Math.max(1.0, manager.getCurrentLineWidth() - 2.0);
            manager.setCurrentLineWidth(newWidth);
            drawingCanvas.updateBrushSettings();
            event.consume();
        }

        // --- GLOBAL OPACITY LOGIC (Left / Right arrows) ---
        if (code == KeyCode.LEFT) {
            // Decrease global opacity by 10% steps
            Color current = manager.getCurrentColor();
            double newAlpha = Math.max(0.1, current.getOpacity() - 0.1);
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
            drawingCanvas.updateBrushSettings();
            event.consume();
        }
        else if (code == KeyCode.RIGHT) {
            // Increase global opacity by 10% steps
            Color current = manager.getCurrentColor();
            double newAlpha = Math.min(1.0, current.getOpacity() + 0.1);
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
            drawingCanvas.updateBrushSettings();
            event.consume();
        }
    }
}
