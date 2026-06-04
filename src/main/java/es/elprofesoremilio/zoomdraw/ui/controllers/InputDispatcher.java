package es.elprofesoremilio.zoomdraw.ui.controllers;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.commands.CommandHistory;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import es.elprofesoremilio.zoomdraw.ui.controllers.CaptureController.CropAction;
import javafx.scene.Scene;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

public class InputDispatcher {
    private final ZoomPanController zoomPanController;
    private final DrawingController drawingController;
    private final TextToolController textToolController;
    private final NumberingToolController numberingToolController;
    private final EraserToolController eraserToolController;
    private final CropToolController cropToolController;
    private final CaptureController captureController;
    private final AnnotationManager manager;
    private final AnnotationStage stage;
    private final CommandHistory commandHistory;

    public InputDispatcher(ZoomPanController zoomPanController,
                           DrawingController drawingController,
                           TextToolController textToolController,
                           NumberingToolController numberingToolController,
                           EraserToolController eraserToolController,
                           CropToolController cropToolController,
                           CaptureController captureController,
                           AnnotationManager manager,
                           AnnotationStage stage) {
        this.zoomPanController = zoomPanController;
        this.drawingController = drawingController;
        this.textToolController = textToolController;
        this.numberingToolController = numberingToolController;
        this.eraserToolController = eraserToolController;
        this.cropToolController = cropToolController;
        this.captureController = captureController;
        this.manager = manager;
        this.stage = stage;
        this.commandHistory = stage.getCommandHistory();
    }

    public void attach(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressedFilter);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, this::handleKeyReleasedFilter);
        scene.addEventFilter(KeyEvent.KEY_TYPED, this::handleKeyTyped);
        scene.addEventFilter(InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, this::handleInputMethodTextChanged);

        scene.setOnMousePressed(this::handleMousePressed);
        scene.setOnMouseDragged(this::handleMouseDragged);
        scene.setOnMouseReleased(this::handleMouseReleased);
        scene.setOnMouseMoved(this::handleMouseMoved);
        scene.setOnMouseClicked(this::handleMouseClicked);
        scene.setOnScroll(this::handleScroll);
        scene.setOnMouseExited(e -> {
            if (eraserToolController.isActive()) {
                eraserToolController.clearPreview();
            }
        });
    }

    private void handleKeyPressedFilter(KeyEvent event) {
        boolean isCtrl = event.isControlDown();
        boolean isAlt = event.isAltDown();
        boolean isShift = event.isShiftDown();
        KeyCode code = event.getCode();

        // 1. Text Mode Active - but typing!
        // If we are actively typing text, we only intercept keys defined in the text tool (Escape, Backspace, Enter, Tab).
        // Other keys (like letters) should fall through to the hiddenTextField.
        if (textToolController.isActive() && textToolController.isTyping()) {
            textToolController.handleKeyPressedFilter(event);
            return;
        }

        // 2. Global capture and crop shortcuts
        if (isCtrl) {
            if (isAlt) {
                if (code == KeyCode.C) {
                    if (numberingToolController.isActive()) {
                        numberingToolController.cancelNumberingMode();
                    }
                    if (textToolController.isActive()) {
                        textToolController.exitTextMode();
                    }
                    cropToolController.enterCropMode(CropAction.CLIPBOARD);
                    event.consume();
                    return;
                } else if (code == KeyCode.S) {
                    if (numberingToolController.isActive()) {
                        numberingToolController.cancelNumberingMode();
                    }
                    if (textToolController.isActive()) {
                        textToolController.exitTextMode();
                    }
                    cropToolController.enterCropMode(CropAction.SAVE);
                    event.consume();
                    return;
                }
            } else {
                if (code == KeyCode.C) {
                    captureController.captureFull(CropAction.CLIPBOARD);
                    event.consume();
                    return;
                } else if (code == KeyCode.S) {
                    captureController.captureFull(CropAction.SAVE);
                    event.consume();
                    return;
                }
            }
        }

        // Eraser Mode Keyboard Controls
        if (isCtrl && code == KeyCode.D) {
            if (eraserToolController.isActive()) {
                eraserToolController.exitEraserMode();
            } else {
                if (textToolController.isActive()) {
                    textToolController.exitTextMode();
                }
                if (numberingToolController.isActive()) {
                    numberingToolController.cancelNumberingMode();
                }
                eraserToolController.enterEraserMode();
            }
            event.consume();
            return;
        }

        if (eraserToolController.isActive()) {
            if (code == KeyCode.ESCAPE) {
                eraserToolController.exitEraserMode();
                event.consume();
                return;
            }
        }

        // 3. Crop Mode Active
        if (cropToolController.isActive()) {
            cropToolController.handleKeyPressed(event);
            return;
        }

        // 4. Numbering Mode Active
        if (numberingToolController.isActive()) {
            numberingToolController.handleKeyPressed(event);
            return;
        }

        // 5. Toggle text mode
        if (code == KeyCode.T) {
            if (textToolController.isActive()) {
                textToolController.exitTextMode();
            } else {
                if (numberingToolController.isActive()) {
                    numberingToolController.cancelNumberingMode();
                }
                textToolController.enterTextMode();
            }
            event.consume();
            return;
        }

        // 6. Global exit key logic
        if (code == AppConfig.EXIT_KEY) {
            manager.stopAnnotationMode();
            event.consume();
            return;
        }

        // 7. Global color keys
        if (!isCtrl && !isAlt && !isShift && !event.isMetaDown()) {
            Color newBaseColor = getColor(code);
            if (newBaseColor != null) {
                double alpha = manager.getCurrentOpacity();
                Color finalColor = new Color(newBaseColor.getRed(), newBaseColor.getGreen(), newBaseColor.getBlue(), alpha);
                manager.setCurrentColor(finalColor);
                stage.updateBrushSettings();
                event.consume();
                return;
            }
        }

        // 8. Global line width keys
        if (code == AppConfig.LINE_WIDTH_INCREASE_KEY_1 || code == AppConfig.LINE_WIDTH_INCREASE_KEY_2
                || code == AppConfig.LINE_WIDTH_INCREASE_KEY_3) {
            double newWidth = Math.min(AppConfig.LINE_WIDTH_MAX,
                    manager.getCurrentLineWidth() + AppConfig.LINE_WIDTH_SCROLL_STEP);
            manager.setCurrentLineWidth(newWidth);
            stage.updateBrushSettings();
            event.consume();
            return;
        } else if (code == AppConfig.LINE_WIDTH_DECREASE_KEY_1 || code == AppConfig.LINE_WIDTH_DECREASE_KEY_2
                || code == AppConfig.LINE_WIDTH_DECREASE_KEY_3) {
            double newWidth = Math.max(AppConfig.LINE_WIDTH_MIN,
                    manager.getCurrentLineWidth() - AppConfig.LINE_WIDTH_SCROLL_STEP);
            manager.setCurrentLineWidth(newWidth);
            stage.updateBrushSettings();
            event.consume();
            return;
        }

        // 9. Global opacity keys (LEFT / RIGHT)
        if (code == AppConfig.GLOBAL_OPACITY_DECREASE_KEY) {
            Color current = manager.getCurrentColor();
            double newAlpha = Math.max(AppConfig.GLOBAL_OPACITY_MIN,
                    current.getOpacity() - AppConfig.GLOBAL_OPACITY_STEP);
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
            stage.updateBrushSettings();
            event.consume();
            return;
        } else if (code == AppConfig.GLOBAL_OPACITY_INCREASE_KEY) {
            Color current = manager.getCurrentColor();
            double newAlpha = Math.min(AppConfig.GLOBAL_OPACITY_MAX,
                    current.getOpacity() + AppConfig.GLOBAL_OPACITY_STEP);
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
            stage.updateBrushSettings();
            event.consume();
            return;
        }

        // 10. Global opacity keys (Shift + 0-9)
        if (isShift) {
            double newAlpha = -1;
            switch (code) {
                case DIGIT1:
                case NUMPAD1: newAlpha = 0.1; break;
                case DIGIT2:
                case NUMPAD2: newAlpha = 0.2; break;
                case DIGIT3:
                case NUMPAD3: newAlpha = 0.3; break;
                case DIGIT4:
                case NUMPAD4: newAlpha = 0.4; break;
                case DIGIT5:
                case NUMPAD5: newAlpha = 0.5; break;
                case DIGIT6:
                case NUMPAD6: newAlpha = 0.6; break;
                case DIGIT7:
                case NUMPAD7: newAlpha = 0.7; break;
                case DIGIT8:
                case NUMPAD8: newAlpha = 0.8; break;
                case DIGIT9:
                case NUMPAD9: newAlpha = 0.9; break;
                case DIGIT0:
                case NUMPAD0: newAlpha = 1.0; break;
                default: break;
            }
            if (newAlpha != -1) {
                Color current = manager.getCurrentColor();
                Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
                manager.setCurrentColor(updatedColor);
                stage.updateBrushSettings();
                event.consume();
                return;
            }
        }

        // 11. Undo/Redo/Overrides keys
        if (isCtrl) {
            if (code == KeyCode.Z) {
                commandHistory.undo(stage::redrawAll);
                event.consume();
                return;
            } else if (code == KeyCode.Y) {
                commandHistory.redo(stage::redrawAll);
                event.consume();
                return;
            } else if (code == KeyCode.K) {
                stage.setBackgroundColorOverride(stage.getBackgroundColorOverride() == Color.BLACK ? null : Color.BLACK);
                stage.redrawAll();
                event.consume();
                return;
            } else if (code == KeyCode.W) {
                stage.setBackgroundColorOverride(stage.getBackgroundColorOverride() == Color.WHITE ? null : Color.WHITE);
                stage.redrawAll();
                event.consume();
                return;
            }
        }

        // 12. Toggle numbering mode key
        if (code == KeyCode.N) {
            if (!numberingToolController.isActive() && !textToolController.isActive()) {
                numberingToolController.enterNumberingMode();
                event.consume();
                return;
            }
        }

        // 13. Number keys (1-0) without modifiers for stroke thickness
        if (!isCtrl && !isAlt && !isShift && !event.isMetaDown()) {
            double widthFactor = -1;
            if (code == KeyCode.DIGIT1) widthFactor = 1.0;
            else if (code == KeyCode.DIGIT2) widthFactor = 2.0;
            else if (code == KeyCode.DIGIT3) widthFactor = 3.0;
            else if (code == KeyCode.DIGIT4) widthFactor = 4.0;
            else if (code == KeyCode.DIGIT5) widthFactor = 5.0;
            else if (code == KeyCode.DIGIT6) widthFactor = 6.0;
            else if (code == KeyCode.DIGIT7) widthFactor = 7.0;
            else if (code == KeyCode.DIGIT8) widthFactor = 8.0;
            else if (code == KeyCode.DIGIT9) widthFactor = 9.0;
            else if (code == KeyCode.DIGIT0) widthFactor = 10.0;

            if (widthFactor != -1) {
                manager.setCurrentLineWidth(widthFactor * AppConfig.LINE_WIDTH_MULTIPLIER);
                stage.updateBrushSettings();
                event.consume();
            }
        }

        // 14. Hand over shape/pencil drawing keys
        drawingController.handleKeyPressed(event);
    }

    private void handleKeyTyped(KeyEvent event) {
        if (cropToolController.isActive()) {
            event.consume();
            return;
        }
        if (numberingToolController.isActive()) {
            event.consume();
            return;
        }
        if (textToolController.isActive()) {
            textToolController.handleKeyTyped(event);
        }
    }

    private void handleInputMethodTextChanged(InputMethodEvent event) {
        if (cropToolController.isActive() || numberingToolController.isActive()) {
            event.consume();
            return;
        }
        if (textToolController.isActive()) {
            textToolController.handleInputMethodTextChanged(event);
        }
    }

    private void handleKeyReleasedFilter(KeyEvent event) {
        if (textToolController.isActive() && textToolController.isTyping()) {
            return;
        }
        if (numberingToolController.isActive()) {
            event.consume();
            return;
        }
        drawingController.handleKeyReleased(event);
    }

    private void handleMousePressed(MouseEvent event) {
        if (!stage.isFocused()) {
            stage.requestFocus();
        }
        manager.bringHelpWindowToFront();

        if (cropToolController.isActive()) {
            cropToolController.handleMousePressed(event);
            return;
        }
        if (eraserToolController.isActive()) {
            eraserToolController.handleMousePressed(event);
            return;
        }
        if (numberingToolController.isActive()) {
            numberingToolController.handleMousePressed(event);
            return;
        }
        if (textToolController.isActive()) {
            textToolController.handleMousePressed(event);
            return;
        }
        drawingController.handleMousePressed(event);
    }

    private void handleMouseDragged(MouseEvent event) {
        if (cropToolController.isActive()) {
            cropToolController.handleMouseDragged(event);
            return;
        }
        if (eraserToolController.isActive()) {
            eraserToolController.handleMouseDragged(event);
            return;
        }
        if (numberingToolController.isActive()) {
            numberingToolController.handleMouseDragged(event);
            return;
        }
        if (textToolController.isActive()) {
            return;
        }
        drawingController.handleMouseDragged(event);
    }

    private void handleMouseReleased(MouseEvent event) {
        if (cropToolController.isActive()) {
            cropToolController.handleMouseReleased(event);
            return;
        }
        if (eraserToolController.isActive()) {
            eraserToolController.handleMouseReleased(event);
            return;
        }
        if (numberingToolController.isActive()) {
            numberingToolController.handleMouseReleased(event);
            return;
        }
        if (textToolController.isActive()) {
            return;
        }
        drawingController.handleMouseReleased(event);
    }

    private void handleMouseMoved(MouseEvent event) {
        if (cropToolController.isActive()) {
            cropToolController.handleMouseMoved(event);
            return;
        }
        if (eraserToolController.isActive()) {
            eraserToolController.handleMouseMoved(event);
            return;
        }
        if (numberingToolController.isActive()) {
            numberingToolController.handleMouseMoved(event.getX(), event.getY());
        }
    }

    private void handleMouseClicked(MouseEvent event) {
        if (!stage.isFocused()) {
            stage.requestFocus();
        }
        if (cropToolController.isActive()) {
            cropToolController.handleMouseClicked(event);
        }
    }

    private void handleScroll(ScrollEvent event) {
        if (event.isControlDown()) {
            if (eraserToolController.isActive()) {
                if (event.getDeltaY() > 0) {
                    eraserToolController.increaseRadius();
                } else if (event.getDeltaY() < 0) {
                    eraserToolController.decreaseRadius();
                }
                eraserToolController.drawEraserPreview(event.getX(), event.getY(), false);
                event.consume();
                return;
            }
            double newWidth = manager.getCurrentLineWidth();
            if (event.getDeltaY() > 0) {
                newWidth = Math.min(AppConfig.LINE_WIDTH_MAX, newWidth + AppConfig.LINE_WIDTH_SCROLL_STEP);
            } else if (event.getDeltaY() < 0) {
                newWidth = Math.max(AppConfig.LINE_WIDTH_MIN, newWidth - AppConfig.LINE_WIDTH_SCROLL_STEP);
            }
            manager.setCurrentLineWidth(newWidth);
            stage.updateBrushSettings();
            event.consume();
        } else if (event.isShiftDown()) {
            double newAlpha = getNewAlpha(event);
            manager.setCurrentOpacity(newAlpha);
            Color current = manager.getCurrentColor();
            Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
            manager.setCurrentColor(updatedColor);
            stage.updateBrushSettings();
            event.consume();
        } else {
            if (cropToolController.isActive() || textToolController.isActive() || numberingToolController.isActive()) {
                // Do not zoom in crop/text/numbering mode
                event.consume();
                return;
            }
            zoomPanController.zoomAtCursor(event.getDeltaY(), event.getX(), event.getY());
            stage.redrawAll();
            if (textToolController.isTyping()) {
                textToolController.redrawTextTemporal();
            }
            if (eraserToolController.isActive()) {
                eraserToolController.drawEraserPreview(event.getX(), event.getY(), false);
            }
            event.consume();
        }
    }

    private double getNewAlpha(ScrollEvent event) {
        double newAlpha = manager.getCurrentOpacity();
        double delta = event.getDeltaY() != 0 ? event.getDeltaY() : event.getDeltaX();

        if (delta > 0) {
            newAlpha = Math.min(1.0, newAlpha + 0.05);
        } else if (delta < 0) {
            newAlpha = Math.max(0.05, newAlpha - 0.05);
        }
        return newAlpha;
    }

    private static Color getColor(KeyCode code) {
        if (code == AppConfig.COLOR_RED_KEY) return AppConfig.RED;
        if (code == AppConfig.COLOR_GREEN_KEY) return AppConfig.GREEN;
        if (code == AppConfig.COLOR_BLUE_KEY) return AppConfig.BLUE;
        if (code == AppConfig.COLOR_YELLOW_KEY) return AppConfig.YELLOW;
        if (code == AppConfig.COLOR_ORANGE_KEY) return AppConfig.ORANGE;
        if (code == AppConfig.COLOR_MAGENTA_KEY) return AppConfig.MAGENTA;
        if (code == AppConfig.COLOR_BLACK_KEY) return AppConfig.BLACK;
        if (code == AppConfig.COLOR_WHITE_KEY) return AppConfig.WHITE;
        if (code == AppConfig.COLOR_CYAN_KEY) return AppConfig.CYAN;
        if (code == AppConfig.COLOR_PINK_KEY) return AppConfig.PINK;
        if (code == AppConfig.COLOR_GREY_KEY) return AppConfig.GREY;
        if (code == AppConfig.COLOR_DARK_GREY_KEY) return AppConfig.DARK_GREY;
        return null;
    }
}
