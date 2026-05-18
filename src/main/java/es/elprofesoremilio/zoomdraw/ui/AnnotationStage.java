package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.commands.*;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.core.DrawMode;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.input.KeyCode;

import java.util.ArrayList;
import java.util.List;

public class AnnotationStage extends Stage implements BrushSettingsUpdater {

    private final AnnotationManager manager;

    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;

    private final List<Point2D> currentStrokePoints = new ArrayList<>();
    private final CommandHistory commandHistory = new CommandHistory();
    private final WritableImage background;

    private DrawMode activeShapeMode = DrawMode.PENCIL;
    private boolean isDrawingShape = false;
    private Point2D shapeStartPoint = null;
    
    private boolean isRPressed = false;
    private boolean isEPressed = false;
    private boolean isFPressed = false;
    private boolean isCPressed = false;
    
    private WritableImage currentCanvasSnapshot = null;

    public AnnotationStage(AnnotationManager manager, Rectangle2D bounds, WritableImage background) {
        super(StageStyle.TRANSPARENT);
        this.manager = manager;
        this.background = background;

        // Initialize permanent canvas
        canvasPermanent = new Canvas(bounds.getWidth(), bounds.getHeight());
        gcPermanent = canvasPermanent.getGraphicsContext2D();
        gcPermanent.drawImage(this.background, 0, 0); // Draw background once

        // Initialize temporal canvas
        canvasTemporal = new Canvas(bounds.getWidth(), bounds.getHeight());
        gcTemporal = canvasTemporal.getGraphicsContext2D();

        // Set initial brush settings for both GCs
        updateBrushSettings();

        StackPane root = new StackPane(canvasPermanent, canvasTemporal);
        root.setBackground(null);
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);

        // Instantiate and attach the input handler
        AnnotationInputHandler inputHandler = new AnnotationInputHandler(manager, this); // Pass 'this' as BrushSettingsUpdater
        inputHandler.attach(scene);

        // Key trackers for shapes
        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.Z) {
                    commandHistory.undo(this::redrawAll);
                    event.consume();
                    return;
                } else if (event.getCode() == KeyCode.Y) {
                    commandHistory.redo(this::redrawAll);
                    event.consume();
                    return;
                }
            }
            switch(event.getCode()) {
                case R: isRPressed = true; break;
                case E: isEPressed = true; break;
                case F: isFPressed = true; break;
                case C: isCPressed = true; break;
                case ESCAPE: 
                    if (isDrawingShape) {
                        isDrawingShape = false;
                        activeShapeMode = DrawMode.PENCIL;
                        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                        event.consume();
                    }
                    break;
                default: break;
            }
        });

        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_RELEASED, event -> {
            switch(event.getCode()) {
                case R: isRPressed = false; break;
                case E: isEPressed = false; break;
                case F: isFPressed = false; break;
                case C: isCPressed = false; break;
                default: break;
            }
        });

        // Mouse events for drawing
        scene.setOnMousePressed(event -> {
            if (event.isControlDown()) {
                if (isRPressed) activeShapeMode = DrawMode.RECTANGLE;
                else if (event.isAltDown() && isEPressed) activeShapeMode = DrawMode.CIRCLE;
                else if (isEPressed) activeShapeMode = DrawMode.ELLIPSE;
                else if (isFPressed) activeShapeMode = DrawMode.ARROW;
                else activeShapeMode = DrawMode.LINE;
                
                isDrawingShape = true;
                shapeStartPoint = new Point2D(event.getX(), event.getY());
            } else if (event.isShiftDown() && (isRPressed || isEPressed || isCPressed)) {
                if (isRPressed) activeShapeMode = DrawMode.FILLED_RECTANGLE;
                else if (event.isAltDown() && isEPressed) activeShapeMode = DrawMode.FILLED_CIRCLE;
                else if (isEPressed) activeShapeMode = DrawMode.FILLED_ELLIPSE;
                else { // if (isCPressed) { // sobrentendido
                    activeShapeMode = DrawMode.CENSOR_RECTANGLE;
                    javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    currentCanvasSnapshot = canvasPermanent.snapshot(params, null);
                }
                
                isDrawingShape = true;
                shapeStartPoint = new Point2D(event.getX(), event.getY());
            } else {
                activeShapeMode = DrawMode.PENCIL;
                isDrawingShape = false;
                currentStrokePoints.clear();
                currentStrokePoints.add(new Point2D(event.getX(), event.getY()));
            }
        });

        scene.setOnMouseDragged(event -> {
            if (isDrawingShape && activeShapeMode != DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                ShapeCommand previewShape = new ShapeCommand(shapeStartPoint, new Point2D(event.getX(), event.getY()), 
                        activeShapeMode, manager.getCurrentColor(), manager.getCurrentLineWidth(), currentCanvasSnapshot);
                previewShape.execute(gcTemporal);
            } else if (activeShapeMode == DrawMode.PENCIL) {
                currentStrokePoints.add(new Point2D(event.getX(), event.getY()));
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                PathCommand previewPath = new PathCommand(currentStrokePoints, manager.getCurrentColor(), manager.getCurrentLineWidth());
                previewPath.execute(gcTemporal);
            }
        });

        scene.setOnMouseReleased(event -> {
            if (isDrawingShape && activeShapeMode != DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                ShapeCommand finalShape = new ShapeCommand(shapeStartPoint, new Point2D(event.getX(), event.getY()), 
                        activeShapeMode, manager.getCurrentColor(), manager.getCurrentLineWidth(), currentCanvasSnapshot);
                commandHistory.execute(finalShape, gcPermanent);
                redrawAll();
                isDrawingShape = false;
                activeShapeMode = DrawMode.PENCIL;
                currentCanvasSnapshot = null;
            } else if (activeShapeMode == DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                if (currentStrokePoints.size() >= 2) {
                    PathCommand finalPath = new PathCommand(currentStrokePoints, manager.getCurrentColor(), manager.getCurrentLineWidth());
                    commandHistory.execute(finalPath, gcPermanent);
                    redrawAll();
                }
                currentStrokePoints.clear();
            }
        });

        // Recuperar foco al clic
        scene.setOnMouseClicked(event -> {
            if (!this.isFocused()) {
                this.toFront();
                this.requestFocus();
            }
        });

        this.setScene(scene);
        this.setAlwaysOnTop(true);

        // Posicionamiento absoluto
        this.setX(bounds.getMinX());
        this.setY(bounds.getMinY());
        this.setWidth(bounds.getWidth());
        this.setHeight(bounds.getHeight());
        this.setResizable(false);
    }

    @Override
    public void updateBrushSettings() {
        Color currentColor = manager.getCurrentColor();
        double currentLineWidth = manager.getCurrentLineWidth();

        // Apply settings to permanent graphics context
        gcPermanent.setStroke(currentColor);
        gcPermanent.setLineWidth(currentLineWidth);
        gcPermanent.setLineCap(StrokeLineCap.ROUND);
        gcPermanent.setLineJoin(StrokeLineJoin.ROUND);

        // Apply settings to temporal graphics context
        gcTemporal.setStroke(currentColor);
        gcTemporal.setLineWidth(currentLineWidth);
        gcTemporal.setLineCap(StrokeLineCap.ROUND);
        gcTemporal.setLineJoin(StrokeLineJoin.ROUND);
    }

    private void redrawAll() {
        gcPermanent.clearRect(0, 0, canvasPermanent.getWidth(), canvasPermanent.getHeight());
        gcPermanent.drawImage(background, 0, 0);
        for (DrawingCommand cmd : commandHistory.getHistory()) {
            cmd.execute(gcPermanent);
        }
        updateBrushSettings();
    }
}