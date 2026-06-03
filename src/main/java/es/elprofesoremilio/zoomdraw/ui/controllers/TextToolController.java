package es.elprofesoremilio.zoomdraw.ui.controllers;

import es.elprofesoremilio.zoomdraw.commands.CommandHistory;
import es.elprofesoremilio.zoomdraw.commands.TextCommand;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public class TextToolController {
    private final AnnotationStage stage;
    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final ZoomPanController zoomPanController;
    private final CommandHistory commandHistory;
    private final AnnotationManager manager;
    private final TextField hiddenTextField;

    private boolean isTextModeActive = false;
    private boolean isTyping = false;
    private TextCommand currentTextCommand = null;

    public TextToolController(AnnotationStage stage, Canvas canvasPermanent, Canvas canvasTemporal,
                              TextField hiddenTextField, ZoomPanController zoomPanController,
                              CommandHistory commandHistory, AnnotationManager manager) {
        this.stage = stage;
        this.canvasPermanent = canvasPermanent;
        this.gcPermanent = canvasPermanent.getGraphicsContext2D();
        this.canvasTemporal = canvasTemporal;
        this.gcTemporal = canvasTemporal.getGraphicsContext2D();
        this.zoomPanController = zoomPanController;
        this.commandHistory = commandHistory;
        this.manager = manager;
        this.hiddenTextField = hiddenTextField;
    }

    public boolean isActive() {
        return isTextModeActive;
    }

    public boolean isTyping() {
        return isTyping;
    }

    public void enterTextMode() {
        isTextModeActive = true;
        stage.getScene().setCursor(javafx.scene.Cursor.TEXT);
        hiddenTextField.requestFocus();
    }

    public void exitTextMode() {
        if (isTyping) {
            finishTextCommand();
        }
        isTextModeActive = false;
        stage.getScene().setCursor(stage.getPencilCursor());
        if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            stage.getScene().getRoot().requestFocus();
        }
    }

    public void handleKeyPressedFilter(KeyEvent event) {
        boolean isCtrl = event.isControlDown();

        if (event.getCode() == KeyCode.ESCAPE) {
            cancelTextCommand();
            event.consume();
            return;
        }

        if (isTyping) {
            if (event.getCode() == KeyCode.BACK_SPACE) {
                currentTextCommand.removeLast();
                redrawTextTemporal();
                event.consume();
                return;
            } else if (event.getCode() == KeyCode.ENTER) {
                currentTextCommand.append(es.elprofesoremilio.zoomdraw.core.text.TextTokens.ENTER_TOKEN,
                        manager.getCurrentColor(), manager.getCurrentLineWidth());
                redrawTextTemporal();
                event.consume();
                return;
            } else if (event.getCode() == KeyCode.TAB) {
                currentTextCommand.append(es.elprofesoremilio.zoomdraw.core.text.TextTokens.TAB_TOKEN,
                        manager.getCurrentColor(), manager.getCurrentLineWidth());
                redrawTextTemporal();
                event.consume();
                return;
            }
        }

        if (isCtrl) {
            event.consume();
        }
    }

    public void handleKeyTyped(KeyEvent event) {
        if (event.isControlDown() || event.isMetaDown()) {
            event.consume();
            return;
        }
        if (isTyping) {
            String character = event.getCharacter();
            if (!character.isEmpty()) {
                char c = character.charAt(0);
                if (!Character.isISOControl(c)) {
                    currentTextCommand.append(character, manager.getCurrentColor(), manager.getCurrentLineWidth());
                    redrawTextTemporal();
                }
            }
            event.consume();
        } else {
            event.consume();
        }
    }

    public void handleInputMethodTextChanged(InputMethodEvent event) {
        if (isTyping) {
            String committed = event.getCommitted();
            if (committed != null && !committed.isEmpty()) {
                currentTextCommand.append(committed, manager.getCurrentColor(), manager.getCurrentLineWidth());
                redrawTextTemporal();
            }
            event.consume();
        }
    }

    public void handleMousePressed(MouseEvent event) {
        if (isTyping) {
            finishTextCommand();
            isTextModeActive = false;
            stage.getScene().setCursor(stage.getPencilCursor());
            return;
        }
        if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
            isTyping = true;
            double origX = (event.getX() - zoomPanController.getOffsetX()) / zoomPanController.getZoomFactor();
            double origY = (event.getY() - zoomPanController.getOffsetY()) / zoomPanController.getZoomFactor();
            currentTextCommand = new TextCommand(new Point2D(origX, origY),
                    manager.getCurrentColor(), manager.getCurrentLineWidth());
            stage.getScene().setCursor(javafx.scene.Cursor.NONE);
            hiddenTextField.requestFocus();
            redrawTextTemporal();
        }
    }

    public void finishTextCommand() {
        if (currentTextCommand != null) {
            currentTextCommand.setShowCursor(false);
            if (!currentTextCommand.isEmpty()) {
                gcPermanent.save();
                zoomPanController.applyTransform(gcPermanent);
                commandHistory.execute(currentTextCommand, gcPermanent);
                gcPermanent.restore();
                stage.redrawAll();
            }
            currentTextCommand = null;
        }
        isTyping = false;
        stage.getScene().setCursor(javafx.scene.Cursor.TEXT);
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    public void cancelTextCommand() {
        manager.notifySubModeCancelled();
        currentTextCommand = null;
        isTyping = false;
        isTextModeActive = false;
        stage.getScene().setCursor(stage.getPencilCursor());
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        if (stage.getScene() != null && stage.getScene().getRoot() != null) {
            stage.getScene().getRoot().requestFocus();
        }
    }

    public void redrawTextTemporal() {
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        if (currentTextCommand != null) {
            gcTemporal.save();
            zoomPanController.applyTransform(gcTemporal);
            currentTextCommand.execute(gcTemporal);
            gcTemporal.restore();
        }
    }
}
