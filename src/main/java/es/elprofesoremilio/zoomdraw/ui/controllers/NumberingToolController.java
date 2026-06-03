package es.elprofesoremilio.zoomdraw.ui.controllers;

import es.elprofesoremilio.zoomdraw.commands.CommandHistory;
import es.elprofesoremilio.zoomdraw.commands.NumberedCircle;
import es.elprofesoremilio.zoomdraw.commands.NumberingSessionCommand;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class NumberingToolController {
    private final AnnotationStage stage;
    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final ZoomPanController zoomPanController;
    private final CommandHistory commandHistory;
    private final AnnotationManager manager;

    private boolean isNumberingModeActive = false;
    private final List<NumberedCircle> temporalCircles = new ArrayList<>();
    private NumberedCircle selectedCircle = null;
    private NumberedCircle hoveredCircle = null;
    private final Stack<List<NumberedCircle>> numberingUndoStack = new Stack<>();
    private final Stack<List<NumberedCircle>> numberingRedoStack = new Stack<>();
    private Point2D dragOffset = null;
    private Point2D dragStartCenter = null;

    public NumberingToolController(AnnotationStage stage, Canvas canvasPermanent, Canvas canvasTemporal,
                                   ZoomPanController zoomPanController, CommandHistory commandHistory,
                                   AnnotationManager manager) {
        this.stage = stage;
        this.canvasPermanent = canvasPermanent;
        this.gcPermanent = canvasPermanent.getGraphicsContext2D();
        this.canvasTemporal = canvasTemporal;
        this.gcTemporal = canvasTemporal.getGraphicsContext2D();
        this.zoomPanController = zoomPanController;
        this.commandHistory = commandHistory;
        this.manager = manager;
    }

    public boolean isActive() {
        return isNumberingModeActive;
    }

    public List<NumberedCircle> getTemporalCircles() {
        return temporalCircles;
    }

    public NumberedCircle getHoveredCircle() {
        return hoveredCircle;
    }

    public NumberedCircle getSelectedCircle() {
        return selectedCircle;
    }

    public void enterNumberingMode() {
        isNumberingModeActive = true;
        temporalCircles.clear();
        selectedCircle = null;
        hoveredCircle = null;
        numberingUndoStack.clear();
        numberingRedoStack.clear();
        stage.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
        stage.redrawAll();
    }

    public void exitNumberingMode() {
        isNumberingModeActive = false;

        if (!temporalCircles.isEmpty()) {
            NumberingSessionCommand cmd = new NumberingSessionCommand(temporalCircles);
            gcPermanent.save();
            zoomPanController.applyTransform(gcPermanent);
            commandHistory.execute(cmd, gcPermanent);
            gcPermanent.restore();
        }

        temporalCircles.clear();
        selectedCircle = null;
        hoveredCircle = null;
        numberingUndoStack.clear();
        numberingRedoStack.clear();

        stage.getScene().setCursor(stage.getPencilCursor());
        stage.redrawAll();
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    public void cancelNumberingMode() {
        manager.notifySubModeCancelled(); // stamp BEFORE clearing flags (race guard for GlobalKeyHook)
        isNumberingModeActive = false;
        temporalCircles.clear();
        selectedCircle = null;
        hoveredCircle = null;
        numberingUndoStack.clear();
        numberingRedoStack.clear();
        stage.getScene().setCursor(stage.getPencilCursor());
        stage.redrawAll();
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    private void pushNumberingUndoState() {
        List<NumberedCircle> copy = new ArrayList<>();
        for (NumberedCircle c : temporalCircles) {
            copy.add(new NumberedCircle(c));
        }
        numberingUndoStack.push(copy);
        numberingRedoStack.clear();
    }

    public void undoNumbering() {
        if (!numberingUndoStack.isEmpty()) {
            List<NumberedCircle> currentCopy = new ArrayList<>();
            for (NumberedCircle c : temporalCircles) {
                currentCopy.add(new NumberedCircle(c));
            }
            numberingRedoStack.push(currentCopy);

            List<NumberedCircle> prevState = numberingUndoStack.pop();
            temporalCircles.clear();
            for (NumberedCircle c : prevState) {
                temporalCircles.add(new NumberedCircle(c));
            }
            selectedCircle = null;
            hoveredCircle = null;
            stage.redrawAll();
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        }
    }

    public void redoNumbering() {
        if (!numberingRedoStack.isEmpty()) {
            List<NumberedCircle> currentCopy = new ArrayList<>();
            for (NumberedCircle c : temporalCircles) {
                currentCopy.add(new NumberedCircle(c));
            }
            numberingUndoStack.push(currentCopy);

            List<NumberedCircle> nextState = numberingRedoStack.pop();
            temporalCircles.clear();
            for (NumberedCircle c : nextState) {
                temporalCircles.add(new NumberedCircle(c));
            }
            selectedCircle = null;
            hoveredCircle = null;
            stage.redrawAll();
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        }
    }

    public void deleteSelectedNumber() {
        if (selectedCircle != null) {
            pushNumberingUndoState();
            int deletedNum = selectedCircle.getNumber();
            temporalCircles.remove(selectedCircle);
            selectedCircle = null;

            for (NumberedCircle c : temporalCircles) {
                if (c.getNumber() > deletedNum) {
                    c.setNumber(c.getNumber() - 1);
                }
            }

            stage.redrawAll();
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        }
    }

    public void handleKeyPressed(KeyEvent event) {
        boolean isCtrlKey = event.isControlDown();
        boolean isShift = event.isShiftDown();

        if (event.getCode() == KeyCode.ESCAPE) {
            cancelNumberingMode();
            event.consume();
        } else if (isCtrlKey && event.getCode() == KeyCode.Z) {
            undoNumbering();
            event.consume();
        } else if ((isCtrlKey && event.getCode() == KeyCode.Y) || (isCtrlKey && isShift && event.getCode() == KeyCode.Z)) {
            redoNumbering();
            event.consume();
        } else if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
            if (selectedCircle != null) {
                deleteSelectedNumber();
                event.consume();
            }
        } else {
            event.consume();
        }
    }

    public void handleMousePressed(MouseEvent event) {
        if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
            exitNumberingMode();
            event.consume();
            return;
        }
        if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
            double mouseX = event.getX();
            double mouseY = event.getY();
            double offsetX = zoomPanController.getOffsetX();
            double offsetY = zoomPanController.getOffsetY();
            double zoomFactor = zoomPanController.getZoomFactor();
            double cursorX_orig = (mouseX - offsetX) / zoomFactor;
            double cursorY_orig = (mouseY - offsetY) / zoomFactor;

            NumberedCircle clicked = null;
            double margin = 10.0;
            for (NumberedCircle c : temporalCircles) {
                double c_radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, c.getLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
                double dist = Math.hypot(cursorX_orig - c.getCenter().getX(), cursorY_orig - c.getCenter().getY());
                if (dist <= c_radius + margin) {
                    clicked = c;
                    break;
                }
            }

            if (clicked != null) {
                pushNumberingUndoState();
                selectedCircle = clicked;
                dragStartCenter = clicked.getCenter();
                dragOffset = new Point2D(cursorX_orig - clicked.getCenter().getX(), cursorY_orig - clicked.getCenter().getY());
                hoveredCircle = null;
            } else {
                if (selectedCircle != null) {
                    selectedCircle = null;
                } else {
                    if (!isPreviewSuperposed(mouseX, mouseY)) {
                        pushNumberingUndoState();
                        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, manager.getCurrentLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
                        double previewX_orig = (mouseX - offsetX) / zoomFactor;
                        double previewY_orig = (mouseY - offsetY) / zoomFactor - radius;

                        int nextNum = temporalCircles.size() + 1;
                        NumberedCircle newCircle = new NumberedCircle(
                                new Point2D(previewX_orig, previewY_orig),
                                nextNum,
                                manager.getCurrentColor(),
                                manager.getCurrentLineWidth(),
                                manager.getCurrentOpacity()
                        );
                        temporalCircles.add(newCircle);
                    }
                }
            }
            stage.redrawAll();
            drawNumberPreview(mouseX, mouseY);
        }
    }

    public void handleMouseDragged(MouseEvent event) {
        if (selectedCircle != null && dragOffset != null) {
            double mouseX = event.getX();
            double mouseY = event.getY();
            double offsetX = zoomPanController.getOffsetX();
            double offsetY = zoomPanController.getOffsetY();
            double zoomFactor = zoomPanController.getZoomFactor();
            double cursorX_orig = (mouseX - offsetX) / zoomFactor;
            double cursorY_orig = (mouseY - offsetY) / zoomFactor;

            selectedCircle.setCenter(new Point2D(cursorX_orig - dragOffset.getX(), cursorY_orig - dragOffset.getY()));

            stage.redrawAll();
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        }
    }

    public void handleMouseReleased(MouseEvent event) {
        if (selectedCircle != null && dragStartCenter != null) {
            if (selectedCircle.getCenter().distance(dragStartCenter) < 1.0) {
                if (!numberingUndoStack.isEmpty()) {
                    numberingUndoStack.pop();
                }
            }
            dragStartCenter = null;
            dragOffset = null;
        }
        double mouseX = event.getX();
        double mouseY = event.getY();
        stage.redrawAll();
        drawNumberPreview(mouseX, mouseY);
    }

    public void handleMouseMoved(double mouseX, double mouseY) {
        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, manager.getCurrentLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
        double offsetX = zoomPanController.getOffsetX();
        double offsetY = zoomPanController.getOffsetY();
        double zoomFactor = zoomPanController.getZoomFactor();
        double previewX_orig = (mouseX - offsetX) / zoomFactor;
        double previewY_orig = (mouseY - offsetY) / zoomFactor - radius;

        NumberedCircle nextHovered = null;
        double margin = 10.0;
        double cursorX_orig = (mouseX - offsetX) / zoomFactor;
        double cursorY_orig = (mouseY - offsetY) / zoomFactor;

        // 1. Direct Hover check
        for (NumberedCircle c : temporalCircles) {
            double c_radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, c.getLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
            double dist = Math.hypot(cursorX_orig - c.getCenter().getX(), cursorY_orig - c.getCenter().getY());
            if (dist <= c_radius + margin) {
                nextHovered = c;
                break;
            }
        }

        // 2. Superposition check
        if (nextHovered == null) {
            for (NumberedCircle c : temporalCircles) {
                double c_radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, c.getLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
                double dist = Math.hypot(previewX_orig - c.getCenter().getX(), previewY_orig - c.getCenter().getY());
                if (dist < radius + c_radius) {
                    nextHovered = c;
                    break;
                }
            }
        }

        if (hoveredCircle != nextHovered) {
            hoveredCircle = nextHovered;
            stage.redrawAll();
        }

        drawNumberPreview(mouseX, mouseY);
    }

    public void drawNumberPreview(double mouseX, double mouseY) {
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());

        if (!isNumberingModeActive) return;
        if (hoveredCircle != null || isPreviewSuperposed(mouseX, mouseY)) {
            return;
        }

        Color brushColor = manager.getCurrentColor();
        double lineWidth = manager.getCurrentLineWidth();
        double opacity = manager.getCurrentOpacity();

        double previewOpacity = opacity * 0.5;
        Color previewColor = new Color(brushColor.getRed(), brushColor.getGreen(), brushColor.getBlue(), previewOpacity);

        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, lineWidth * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
        double offsetX = zoomPanController.getOffsetX();
        double offsetY = zoomPanController.getOffsetY();
        double zoomFactor = zoomPanController.getZoomFactor();
        double previewX_orig = (mouseX - offsetX) / zoomFactor;
        double previewY_orig = (mouseY - offsetY) / zoomFactor - radius;

        gcTemporal.save();
        zoomPanController.applyTransform(gcTemporal);

        gcTemporal.setFill(previewColor);
        gcTemporal.fillOval(previewX_orig - radius, previewY_orig - radius, radius * 2, radius * 2);

        double luminance = 0.299 * previewColor.getRed() + 0.587 * previewColor.getGreen() + 0.114 * previewColor.getBlue();
        Color textColor = (luminance > 0.5) ? Color.BLACK : Color.WHITE;
        Color finalTextColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), previewColor.getOpacity());
        gcTemporal.setFill(finalTextColor);

        int nextNumber = temporalCircles.size() + 1;
        String text = String.valueOf(nextNumber);
        gcTemporal.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, radius * 0.9));
        gcTemporal.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gcTemporal.setTextBaseline(javafx.geometry.VPos.CENTER);
        gcTemporal.fillText(text, previewX_orig, previewY_orig);

        gcTemporal.restore();
    }

    private boolean isPreviewSuperposed(double mouseX, double mouseY) {
        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, manager.getCurrentLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
        double offsetX = zoomPanController.getOffsetX();
        double offsetY = zoomPanController.getOffsetY();
        double zoomFactor = zoomPanController.getZoomFactor();
        double previewX_orig = (mouseX - offsetX) / zoomFactor;
        double previewY_orig = (mouseY - offsetY) / zoomFactor - radius;

        for (NumberedCircle c : temporalCircles) {
            double c_radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, c.getLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
            double dist = Math.hypot(previewX_orig - c.getCenter().getX(), previewY_orig - c.getCenter().getY());
            if (dist < radius + c_radius) {
                return true;
            }
        }
        return false;
    }
}
