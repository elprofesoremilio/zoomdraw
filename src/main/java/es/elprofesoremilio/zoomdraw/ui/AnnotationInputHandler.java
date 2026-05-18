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
        } else if (event.isShiftDown()) {
            double currentOpacity = manager.getCurrentOpacity();
            double newAlpha = currentOpacity;
            
            // Note: JavaFX converts vertical scrolling to horizontal scrolling when SHIFT is held.
            // Therefore, we must check both deltaY and deltaX.
            double delta = event.getDeltaY() != 0 ? event.getDeltaY() : event.getDeltaX();
            
            if (delta > 0) {
                // Scroll up/right: increase opacity
                newAlpha = Math.min(1.0, newAlpha + 0.05);
            } else if (delta < 0) {
                // Scroll down/left: decrease opacity
                newAlpha = Math.max(0.05, newAlpha - 0.05);
            }
            
            manager.setCurrentOpacity(newAlpha);
            
            Color current = manager.getCurrentColor();
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
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
        if (code == AppConfig.COLOR_RED_KEY)
            newBaseColor = Color.RED;
        else if (code == AppConfig.COLOR_GREEN_KEY)
            newBaseColor = Color.GREEN;
        else if (code == AppConfig.COLOR_BLUE_KEY)
            newBaseColor = Color.BLUE;
        else if (code == AppConfig.COLOR_YELLOW_KEY)
            newBaseColor = Color.YELLOW;
        else if (code == AppConfig.COLOR_ORANGE_KEY)
            newBaseColor = Color.ORANGE;
        else if (code == AppConfig.COLOR_MAGENTA_KEY)
            newBaseColor = Color.MAGENTA;
        else if (code == AppConfig.COLOR_BLACK_KEY)
            newBaseColor = Color.BLACK;
        else if (code == AppConfig.COLOR_WHITE_KEY)
            newBaseColor = Color.WHITE;

        if (newBaseColor != null) {
            // If Shift is pressed, apply default semi-transparent opacity. Otherwise, 1.0
            // (opaque).
            double alpha = isShift ? manager.getCurrentOpacity() : 1.0;
            Color finalColor = new Color(newBaseColor.getRed(), newBaseColor.getGreen(), newBaseColor.getBlue(), alpha);

            manager.setCurrentColor(finalColor);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
            return;
        }

        // --- LINE WIDTH LOGIC (Up / Down arrows, +/-) ---
        if (code == AppConfig.LINE_WIDTH_INCREASE_KEY_1 || code == AppConfig.LINE_WIDTH_INCREASE_KEY_2
                || code == AppConfig.LINE_WIDTH_INCREASE_KEY_3) {
            double newWidth = Math.min(AppConfig.LINE_WIDTH_MAX,
                    manager.getCurrentLineWidth() + AppConfig.LINE_WIDTH_SCROLL_STEP);
            manager.setCurrentLineWidth(newWidth);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
        } else if (code == AppConfig.LINE_WIDTH_DECREASE_KEY_1 || code == AppConfig.LINE_WIDTH_DECREASE_KEY_2
                || code == AppConfig.LINE_WIDTH_DECREASE_KEY_3) {
            double newWidth = Math.max(AppConfig.LINE_WIDTH_MIN,
                    manager.getCurrentLineWidth() - AppConfig.LINE_WIDTH_SCROLL_STEP);
            manager.setCurrentLineWidth(newWidth);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
        }

        // --- GLOBAL OPACITY LOGIC (Left / Right arrows) ---
        if (code == AppConfig.GLOBAL_OPACITY_DECREASE_KEY) {
            // Decrease global opacity by 10% steps
            Color current = manager.getCurrentColor();
            double newAlpha = Math.max(AppConfig.GLOBAL_OPACITY_MIN,
                    current.getOpacity() - AppConfig.GLOBAL_OPACITY_STEP);
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
        } else if (code == AppConfig.GLOBAL_OPACITY_INCREASE_KEY) {
            // Increase global opacity by 10% steps
            Color current = manager.getCurrentColor();
            double newAlpha = Math.min(AppConfig.GLOBAL_OPACITY_MAX,
                    current.getOpacity() + AppConfig.GLOBAL_OPACITY_STEP);
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
            brushSettingsUpdater.updateBrushSettings();
            event.consume();
        }

        // --- GLOBAL OPACITY LOGIC (SHIFT + 0-9) ---
        if (isShift) {
            double newAlpha = -1;
            switch (code) {
                case DIGIT1:
                case NUMPAD1:
                    newAlpha = 0.1;
                    break;
                case DIGIT2:
                case NUMPAD2:
                    newAlpha = 0.2;
                    break;
                case DIGIT3:
                case NUMPAD3:
                    newAlpha = 0.3;
                    break;
                case DIGIT4:
                case NUMPAD4:
                    newAlpha = 0.4;
                    break;
                case DIGIT5:
                case NUMPAD5:
                    newAlpha = 0.5;
                    break;
                case DIGIT6:
                case NUMPAD6:
                    newAlpha = 0.6;
                    break;
                case DIGIT7:
                case NUMPAD7:
                    newAlpha = 0.7;
                    break;
                case DIGIT8:
                case NUMPAD8:
                    newAlpha = 0.8;
                    break;
                case DIGIT9:
                case NUMPAD9:
                    newAlpha = 0.9;
                    break;
                case DIGIT0:
                case NUMPAD0:
                    newAlpha = 1.0;
                    break;
                default:
                    break;
            }
            if (newAlpha != -1) {
                Color current = manager.getCurrentColor();
                Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
                manager.setCurrentColor(updatedColor);
                brushSettingsUpdater.updateBrushSettings();
                event.consume();
            }
        }
    }
}