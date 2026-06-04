package es.elprofesoremilio.zoomdraw.ui.controllers;

import es.elprofesoremilio.zoomdraw.commands.*;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class EraserToolController {
    private final AnnotationStage stage;
    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final ZoomPanController zoomPanController;
    private final CommandHistory commandHistory;
    private final AnnotationManager manager;

    private boolean isActive = false;
    private double screenRadius = AppConfig.ERASER_RADIUS_DEFAULT;
    private boolean isDragging = false;

    private List<DrawingCommand> originalHistory = null;
    private List<DrawingCommand> workingHistory = null;

    public EraserToolController(AnnotationStage stage, Canvas canvasPermanent, Canvas canvasTemporal,
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
        return isActive;
    }

    public void enterEraserMode() {
        isActive = true;
        isDragging = false;
        stage.getScene().setCursor(javafx.scene.Cursor.NONE);
        stage.redrawAll();
    }

    public void exitEraserMode() {
        isActive = false;
        isDragging = false;
        stage.getScene().setCursor(stage.getPencilCursor());
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        stage.redrawAll();
    }

    public void clearPreview() {
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    public void increaseRadius() {
        screenRadius = Math.min(AppConfig.ERASER_RADIUS_MAX, screenRadius + AppConfig.ERASER_RADIUS_STEP);
    }

    public void decreaseRadius() {
        screenRadius = Math.max(AppConfig.ERASER_RADIUS_MIN, screenRadius - AppConfig.ERASER_RADIUS_STEP);
    }

    public void drawEraserPreview(double mouseX, double mouseY, boolean isDraggingPreview) {
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());

        if (!isActive) return;

        double r = screenRadius;
        double borderThickness = 2.0;
        double drawR = r - borderThickness / 2.0;

        gcTemporal.save();
        // Drawn without zoom transformation (screen coordinates)

        if (isDraggingPreview) {
            gcTemporal.setFill(new Color(1.0, 0.0, 0.0, 0.3)); // Red semitransparent
        } else {
            gcTemporal.setFill(new Color(1.0, 1.0, 1.0, 0.3)); // White semitransparent
        }
        gcTemporal.fillOval(mouseX - drawR, mouseY - drawR, 2 * drawR, 2 * drawR);

        gcTemporal.setStroke(new Color(0.8, 0.8, 0.8, 0.3)); // Light gray semitransparent
        gcTemporal.setLineWidth(borderThickness);
        gcTemporal.strokeOval(mouseX - drawR, mouseY - drawR, 2 * drawR, 2 * drawR);

        gcTemporal.restore();
    }

    public void handleMouseMoved(MouseEvent event) {
        if (isActive && !isDragging) {
            drawEraserPreview(event.getX(), event.getY(), false);
        }
    }

    public void handleMousePressed(MouseEvent event) {
        if (!isActive) return;

        if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
            isDragging = true;

            // Capture initial states
            originalHistory = new ArrayList<>();
            for (DrawingCommand cmd : commandHistory.getHistory()) {
                originalHistory.add(cmd);
            }
            workingHistory = new ArrayList<>(originalHistory);

            double mouseX = event.getX();
            double mouseY = event.getY();

            applyEraserAtMouse(mouseX, mouseY);
            drawEraserPreview(mouseX, mouseY, true);
        }
    }

    public void handleMouseDragged(MouseEvent event) {
        if (!isActive) return;

        if (isDragging) {
            double mouseX = event.getX();
            double mouseY = event.getY();

            applyEraserAtMouse(mouseX, mouseY);
            drawEraserPreview(mouseX, mouseY, true);
        }
    }

    public void handleMouseReleased(MouseEvent event) {
        if (!isActive) return;

        if (isDragging) {
            isDragging = false;

            double mouseX = event.getX();
            double mouseY = event.getY();

            applyEraserAtMouse(mouseX, mouseY);

            // Consolidate the erase command if something changed
            boolean historyChanged = false;
            if (originalHistory.size() != workingHistory.size()) {
                historyChanged = true;
            } else {
                for (int i = 0; i < originalHistory.size(); i++) {
                    if (originalHistory.get(i) != workingHistory.get(i)) {
                        historyChanged = true;
                        break;
                    }
                }
            }

            if (historyChanged) {
                List<DrawingCommand> originalAffected = new ArrayList<>();
                for (DrawingCommand cmd : originalHistory) {
                    if (!workingHistory.contains(cmd)) {
                        originalAffected.add(cmd);
                    }
                }

                List<DrawingCommand> replacementCommands = new ArrayList<>();
                for (DrawingCommand cmd : workingHistory) {
                    if (!originalHistory.contains(cmd)) {
                        replacementCommands.add(cmd);
                    }
                }

                // Restore stack temporarily to original so execute can register it
                commandHistory.setUndoStack(originalHistory);

                EraseCommand eraseCmd = new EraseCommand(originalAffected, replacementCommands, originalHistory, workingHistory);

                gcPermanent.save();
                zoomPanController.applyTransform(gcPermanent);
                commandHistory.execute(eraseCmd, gcPermanent);
                gcPermanent.restore();

                // Now finalize the stack to workingHistory + eraseCmd
                commandHistory.setUndoStack(workingHistory);
                ((java.util.Stack<DrawingCommand>) commandHistory.getHistory()).push(eraseCmd);
            }

            stage.redrawAll();
            drawEraserPreview(mouseX, mouseY, false);
        }
    }

    private void applyEraserAtMouse(double mouseX, double mouseY) {
        double offsetX = zoomPanController.getOffsetX();
        double offsetY = zoomPanController.getOffsetY();
        double zoomFactor = zoomPanController.getZoomFactor();

        double origX = (mouseX - offsetX) / zoomFactor;
        double origY = (mouseY - offsetY) / zoomFactor;
        Point2D center1x = new Point2D(origX, origY);

        double radius1x = screenRadius / zoomFactor;

        boolean changed = applyEraserAt(center1x, radius1x);
        if (changed) {
            commandHistory.setUndoStack(workingHistory);
            stage.redrawAll();
        }
    }

    private boolean applyEraserAt(Point2D center, double radius) {
        boolean anyChanged = false;
        List<DrawingCommand> nextWorkingHistory = new ArrayList<>();

        for (DrawingCommand cmd : workingHistory) {
            if (cmd instanceof PathCommand) {
                PathCommand path = (PathCommand) cmd;
                boolean intersects = false;
                for (Point2D p : path.getPoints()) {
                    if (p.distance(center) <= radius) {
                        intersects = true;
                        break;
                    }
                }
                if (intersects) {
                    anyChanged = true;
                    List<PathCommand> splitPaths = erasePath(path, center, radius);
                    nextWorkingHistory.addAll(splitPaths);
                } else {
                    nextWorkingHistory.add(path);
                }
            } else if (cmd instanceof ShapeCommand) {
                ShapeCommand shape = (ShapeCommand) cmd;
                if (shape.intersects(center, radius)) {
                    anyChanged = true;
                } else {
                    nextWorkingHistory.add(shape);
                }
            } else if (cmd instanceof TextCommand) {
                TextCommand text = (TextCommand) cmd;
                if (text.intersects(center, radius)) {
                    anyChanged = true;
                } else {
                    nextWorkingHistory.add(text);
                }
            } else if (cmd instanceof NumberingSessionCommand) {
                NumberingSessionCommand session = (NumberingSessionCommand) cmd;
                DrawingCommand updatedSession = eraseNumberingSession(session, center, radius);
                if (updatedSession != session) {
                    anyChanged = true;
                    if (updatedSession != null) {
                        nextWorkingHistory.add(updatedSession);
                    }
                } else {
                    nextWorkingHistory.add(session);
                }
            } else {
                nextWorkingHistory.add(cmd);
            }
        }

        if (anyChanged) {
            workingHistory = nextWorkingHistory;
        }
        return anyChanged;
    }

    private List<PathCommand> erasePath(PathCommand path, Point2D center, double radius) {
        List<Point2D> points = path.getPoints();
        List<PathCommand> newPaths = new ArrayList<>();
        List<Point2D> currentSegment = new ArrayList<>();

        for (Point2D p : points) {
            double dist = p.distance(center);
            if (dist > radius) {
                currentSegment.add(p);
            } else {
                if (currentSegment.size() >= 3) {
                    newPaths.add(new PathCommand(currentSegment, path.getColor(), path.getLineWidth()));
                }
                currentSegment = new ArrayList<>();
            }
        }
        if (currentSegment.size() >= 3) {
            newPaths.add(new PathCommand(currentSegment, path.getColor(), path.getLineWidth()));
        }
        return newPaths;
    }

    private DrawingCommand eraseNumberingSession(NumberingSessionCommand session, Point2D center, double radius) {
        List<NumberedCircle> remaining = new ArrayList<>();
        boolean changed = false;
        for (NumberedCircle c : session.getCircles()) {
            double circleRadius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, c.getLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
            double dist = c.getCenter().distance(center);
            if (dist <= radius + circleRadius) {
                changed = true;
            } else {
                remaining.add(c);
            }
        }

        if (!changed) {
            return session;
        }

        if (remaining.isEmpty()) {
            return null;
        }

        // Re-index remaining circles sequentially from 1 based on original order
        List<NumberedCircle> reindexed = new ArrayList<>();
        for (int i = 0; i < remaining.size(); i++) {
            NumberedCircle oldC = remaining.get(i);
            NumberedCircle newC = new NumberedCircle(oldC);
            newC.setNumber(i + 1);
            reindexed.add(newC);
        }

        return new NumberingSessionCommand(reindexed);
    }
}
