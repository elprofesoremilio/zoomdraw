package es.elprofesoremilio.zoomdraw.ui.controllers;

import es.elprofesoremilio.zoomdraw.commands.ClearCommand;
import es.elprofesoremilio.zoomdraw.commands.CommandHistory;
import es.elprofesoremilio.zoomdraw.commands.PathCommand;
import es.elprofesoremilio.zoomdraw.commands.ShapeCommand;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.core.DrawMode;
import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.List;

public class DrawingController {
    private final AnnotationStage stage;
    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final ZoomPanController zoomPanController;
    private final CommandHistory commandHistory;
    private final AnnotationManager manager;

    private final List<Point2D> currentStrokePoints = new ArrayList<>();
    private DrawMode activeShapeMode = DrawMode.PENCIL;
    private boolean isDrawingShape = false;
    private Point2D shapeStartPoint = null;
    private WritableImage currentCanvasSnapshot = null;

    private boolean isRPressed = false;
    private boolean isEPressed = false;
    private boolean isFPressed = false;
    private boolean isCPressed = false;

    public DrawingController(AnnotationStage stage, Canvas canvasPermanent, Canvas canvasTemporal,
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

    public boolean isDrawingShape() {
        return isDrawingShape;
    }

    public void cancelDrawingShape() {
        isDrawingShape = false;
        activeShapeMode = DrawMode.PENCIL;
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    public void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case R:
                isRPressed = true;
                event.consume();
                break;
            case E:
                isEPressed = true;
                if (!event.isControlDown() && !event.isAltDown() && !event.isShiftDown() && !event.isMetaDown()) {
                    commandHistory.execute(new ClearCommand(stage::drawCurrentBackground), gcPermanent);
                    stage.redrawAll();
                }
                event.consume();
                break;
            case F:
                isFPressed = true;
                event.consume();
                break;
            case C:
                isCPressed = true;
                event.consume();
                break;
            case ESCAPE:
                if (isDrawingShape) {
                    cancelDrawingShape();
                    event.consume();
                }
                break;
            default:
                break;
        }
    }

    public void handleKeyReleased(KeyEvent event) {
        switch (event.getCode()) {
            case R:
                isRPressed = false;
                event.consume();
                break;
            case E:
                isEPressed = false;
                event.consume();
                break;
            case F:
                isFPressed = false;
                event.consume();
                break;
            case C:
                isCPressed = false;
                event.consume();
                break;
            default:
                break;
        }
    }

    public void handleMousePressed(MouseEvent event) {
        boolean isCtrl = event.isControlDown();
        boolean isShift = event.isShiftDown();
        boolean isAlt = event.isAltDown();

        double offsetX = zoomPanController.getOffsetX();
        double offsetY = zoomPanController.getOffsetY();
        double zoomFactor = zoomPanController.getZoomFactor();

        if (isShift && (isRPressed || isEPressed || isCPressed)) {
            if (isRPressed) {
                activeShapeMode = DrawMode.FILLED_RECTANGLE;
            } else if (isEPressed) {
                if (isAlt) {
                    activeShapeMode = DrawMode.FILLED_CIRCLE;
                } else {
                    activeShapeMode = DrawMode.FILLED_ELLIPSE;
                }
            } else { // isCPressed is true
                activeShapeMode = DrawMode.CENSOR_RECTANGLE;
                currentCanvasSnapshot = stage.get1xCanvasSnapshot();
            }
            isDrawingShape = true;
            double origX = (event.getX() - offsetX) / zoomFactor;
            double origY = (event.getY() - offsetY) / zoomFactor;
            shapeStartPoint = new Point2D(origX, origY);
        } else if (isCtrl) {
            if (isRPressed) {
                activeShapeMode = DrawMode.RECTANGLE;
            } else if (isEPressed) {
                if (isAlt) {
                    activeShapeMode = DrawMode.CIRCLE;
                } else {
                    activeShapeMode = DrawMode.ELLIPSE;
                }
            } else if (isFPressed) {
                activeShapeMode = DrawMode.ARROW;
            } else {
                activeShapeMode = DrawMode.LINE;
            }
            isDrawingShape = true;
            double origX = (event.getX() - offsetX) / zoomFactor;
            double origY = (event.getY() - offsetY) / zoomFactor;
            shapeStartPoint = new Point2D(origX, origY);
        } else {
            activeShapeMode = DrawMode.PENCIL;
            isDrawingShape = false;
            currentStrokePoints.clear();
            double origX = (event.getX() - offsetX) / zoomFactor;
            double origY = (event.getY() - offsetY) / zoomFactor;
            currentStrokePoints.add(new Point2D(origX, origY));
        }
    }

    public void handleMouseDragged(MouseEvent event) {
        double offsetX = zoomPanController.getOffsetX();
        double offsetY = zoomPanController.getOffsetY();
        double zoomFactor = zoomPanController.getZoomFactor();

        double origX = (event.getX() - offsetX) / zoomFactor;
        double origY = (event.getY() - offsetY) / zoomFactor;
        Point2D origPoint = new Point2D(origX, origY);

        if (isDrawingShape && activeShapeMode != DrawMode.PENCIL) {
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
            gcTemporal.save();
            zoomPanController.applyTransform(gcTemporal);
            ShapeCommand previewShape = new ShapeCommand(shapeStartPoint, origPoint,
                    activeShapeMode, manager.getCurrentColor(), manager.getCurrentLineWidth(),
                    currentCanvasSnapshot);
            previewShape.execute(gcTemporal);
            gcTemporal.restore();
        } else if (activeShapeMode == DrawMode.PENCIL) {
            currentStrokePoints.add(origPoint);
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
            gcTemporal.save();
            zoomPanController.applyTransform(gcTemporal);
            PathCommand previewPath = new PathCommand(currentStrokePoints, manager.getCurrentColor(),
                    manager.getCurrentLineWidth());
            previewPath.execute(gcTemporal);
            gcTemporal.restore();
        }
    }

    public void handleMouseReleased(MouseEvent event) {
        double offsetX = zoomPanController.getOffsetX();
        double offsetY = zoomPanController.getOffsetY();
        double zoomFactor = zoomPanController.getZoomFactor();

        double origX = (event.getX() - offsetX) / zoomFactor;
        double origY = (event.getY() - offsetY) / zoomFactor;
        Point2D origPoint = new Point2D(origX, origY);

        if (isDrawingShape && activeShapeMode != DrawMode.PENCIL) {
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
            ShapeCommand finalShape = new ShapeCommand(shapeStartPoint, origPoint,
                    activeShapeMode, manager.getCurrentColor(), manager.getCurrentLineWidth(),
                    currentCanvasSnapshot);
            gcPermanent.save();
            zoomPanController.applyTransform(gcPermanent);
            commandHistory.execute(finalShape, gcPermanent);
            gcPermanent.restore();
            stage.redrawAll();
            isDrawingShape = false;
            activeShapeMode = DrawMode.PENCIL;
            currentCanvasSnapshot = null;
        } else if (activeShapeMode == DrawMode.PENCIL) {
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
            if (currentStrokePoints.size() >= 2) {
                PathCommand finalPath = new PathCommand(currentStrokePoints, manager.getCurrentColor(),
                        manager.getCurrentLineWidth());
                gcPermanent.save();
                zoomPanController.applyTransform(gcPermanent);
                commandHistory.execute(finalPath, gcPermanent);
                gcPermanent.restore();
                stage.redrawAll();
            }
            currentStrokePoints.clear();
        }
    }
}
