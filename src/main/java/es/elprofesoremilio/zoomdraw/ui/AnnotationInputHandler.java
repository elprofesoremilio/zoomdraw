package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
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
    private final BrushSettingsUpdater brushSettingsUpdater;

    public AnnotationInputHandler(AnnotationManager manager, BrushSettingsUpdater brushSettingsUpdater) {
        this.manager = manager;
        this.brushSettingsUpdater = brushSettingsUpdater;
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
                newWidth = Math.min(AppConfig.LINE_WIDTH_MAX, newWidth + AppConfig.LINE_WIDTH_SCROLL_STEP);
            } else if (event.getDeltaY() < 0) {
                // Scroll down: decrease line width
                newWidth = Math.max(AppConfig.LINE_WIDTH_MIN, newWidth - AppConfig.LINE_WIDTH_SCROLL_STEP);
            }
            manager.setCurrentLineWidth(newWidth);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
        }
    }

    private void handleKeyPressed(javafx.scene.input.KeyEvent event) {
        KeyCode code = event.getCode();
        boolean isShift = event.isShiftDown();

        // EXIT
        if (code == AppConfig.EXIT_KEY) {
            manager.stopAnnotationMode();
            event.consume();
            return;
        }

        // --- COLOR LOGIC ---
        Color newBaseColor = null;
        if (code == AppConfig.COLOR_RED_KEY) newBaseColor = Color.RED;
        else if (code == AppConfig.COLOR_GREEN_KEY) newBaseColor = Color.GREEN;
        else if (code == AppConfig.COLOR_BLUE_KEY) newBaseColor = Color.BLUE;
        else if (code == AppConfig.COLOR_YELLOW_KEY) newBaseColor = Color.YELLOW;
        else if (code == AppConfig.COLOR_ORANGE_KEY) newBaseColor = Color.ORANGE;
        else if (code == AppConfig.COLOR_MAGENTA_KEY) newBaseColor = Color.MAGENTA;
        else if (code == AppConfig.COLOR_BLACK_KEY) newBaseColor = Color.BLACK;
        else if (code == AppConfig.COLOR_WHITE_KEY) newBaseColor = Color.WHITE;


        if (newBaseColor != null) {
            // If Shift is pressed, apply default semi-transparent opacity. Otherwise, 1.0 (opaque).
            double alpha = isShift ? manager.getDefaultSemiTransparentOpacity() : 1.0;
            Color finalColor = new Color(newBaseColor.getRed(), newBaseColor.getGreen(), newBaseColor.getBlue(), alpha);

            manager.setCurrentColor(finalColor);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
            return;
        }

        // --- LINE WIDTH LOGIC (Up / Down arrows, +/-) ---
        if (code == AppConfig.LINE_WIDTH_INCREASE_KEY_1 || code == AppConfig.LINE_WIDTH_INCREASE_KEY_2 || code == AppConfig.LINE_WIDTH_INCREASE_KEY_3) {
            double newWidth = Math.min(AppConfig.LINE_WIDTH_MAX, manager.getCurrentLineWidth() + AppConfig.LINE_WIDTH_SCROLL_STEP);
            manager.setCurrentLineWidth(newWidth);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
        }
        else if (code == AppConfig.LINE_WIDTH_DECREASE_KEY_1 || code == AppConfig.LINE_WIDTH_DECREASE_KEY_2 || code == AppConfig.LINE_WIDTH_DECREASE_KEY_3) {
            double newWidth = Math.max(AppConfig.LINE_WIDTH_MIN, manager.getCurrentLineWidth() - AppConfig.LINE_WIDTH_SCROLL_STEP);
            manager.setCurrentLineWidth(newWidth);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
        }

        // --- GLOBAL OPACITY LOGIC (Left / Right arrows) ---
        if (code == AppConfig.GLOBAL_OPACITY_DECREASE_KEY) {
            // Decrease global opacity by 10% steps
            Color current = manager.getCurrentColor();
            double newAlpha = Math.max(AppConfig.GLOBAL_OPACITY_MIN, current.getOpacity() - AppConfig.GLOBAL_OPACITY_STEP);
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
        }
        else if (code == AppConfig.GLOBAL_OPACITY_INCREASE_KEY) {
            // Increase global opacity by 10% steps
            Color current = manager.getCurrentColor();
            double newAlpha = Math.min(AppConfig.GLOBAL_OPACITY_MAX, current.getOpacity() + AppConfig.GLOBAL_OPACITY_STEP);
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
        }
    }
}