package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
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

import java.util.ArrayList;
import java.util.List;

public class AnnotationStage extends Stage implements BrushSettingsUpdater {

    private final AnnotationManager manager;
    private final AnnotationInputHandler inputHandler;

    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;

    private final List<Point2D> currentStrokePoints = new ArrayList<>();

    private enum DrawMode {
        PENCIL, LINE, RECTANGLE, CIRCLE, ELLIPSE, ARROW
    }
    private DrawMode activeShapeMode = DrawMode.PENCIL;
    private boolean isDrawingShape = false;
    private Point2D shapeStartPoint = null;
    
    private boolean isRPressed = false;
    private boolean isEPressed = false;
    private boolean isFPressed = false;

    public AnnotationStage(AnnotationManager manager, Rectangle2D bounds, WritableImage background) {
        super(StageStyle.TRANSPARENT);
        this.manager = manager;

        // Initialize permanent canvas
        canvasPermanent = new Canvas(bounds.getWidth(), bounds.getHeight());
        gcPermanent = canvasPermanent.getGraphicsContext2D();
        gcPermanent.drawImage(background, 0, 0); // Draw background once

        // Initialize temporal canvas
        canvasTemporal = new Canvas(bounds.getWidth(), bounds.getHeight());
        gcTemporal = canvasTemporal.getGraphicsContext2D();

        // Set initial brush settings for both GCs
        updateBrushSettings();

        StackPane root = new StackPane(canvasPermanent, canvasTemporal);
        root.setBackground(null);
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);

        // Instantiate and attach the input handler
        this.inputHandler = new AnnotationInputHandler(manager, this); // Pass 'this' as BrushSettingsUpdater
        this.inputHandler.attach(scene);

        // Key trackers for shapes
        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            switch(event.getCode()) {
                case R: isRPressed = true; break;
                case E: isEPressed = true; break;
                case F: isFPressed = true; break;
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
                drawShape(gcTemporal, shapeStartPoint, new Point2D(event.getX(), event.getY()), activeShapeMode);
            } else if (activeShapeMode == DrawMode.PENCIL) {
                currentStrokePoints.add(new Point2D(event.getX(), event.getY()));
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                redrawStroke(gcTemporal, currentStrokePoints);
            }
        });

        scene.setOnMouseReleased(event -> {
            if (isDrawingShape && activeShapeMode != DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                drawShape(gcPermanent, shapeStartPoint, new Point2D(event.getX(), event.getY()), activeShapeMode);
                isDrawingShape = false;
                activeShapeMode = DrawMode.PENCIL;
            } else if (activeShapeMode == DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                redrawStroke(gcPermanent, currentStrokePoints);
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

    private void redrawStroke(GraphicsContext gc, List<Point2D> points) {
        if (points.size() < 2) {
            return;
        }
        gc.beginPath();
        gc.moveTo(points.get(0).getX(), points.get(0).getY());
        for (int i = 1; i < points.size(); i++) {
            gc.lineTo(points.get(i).getX(), points.get(i).getY());
        }
        gc.stroke();
    }

    private void drawShape(GraphicsContext gc, Point2D start, Point2D end, DrawMode mode) {
        double x1 = start.getX();
        double y1 = start.getY();
        double x2 = end.getX();
        double y2 = end.getY();

        switch (mode) {
            case LINE:
                gc.strokeLine(x1, y1, x2, y2);
                break;
            case RECTANGLE:
                double rx = Math.min(x1, x2);
                double ry = Math.min(y1, y2);
                double rw = Math.abs(x1 - x2);
                double rh = Math.abs(y1 - y2);
                gc.strokeRect(rx, ry, rw, rh);
                break;
            case CIRCLE:
                double radius = Math.hypot(x2 - x1, y2 - y1);
                gc.strokeOval(x1 - radius, y1 - radius, radius * 2, radius * 2);
                break;
            case ELLIPSE:
                double ex = Math.min(x1, x2);
                double ey = Math.min(y1, y2);
                double ew = Math.abs(x1 - x2);
                double eh = Math.abs(y1 - y2);
                gc.strokeOval(ex, ey, ew, eh);
                break;
            case ARROW:
                drawArrow(gc, x1, y1, x2, y2);
                break;
            default:
                break;
        }
    }

    private void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2) {
        gc.strokeLine(x1, y1, x2, y2);
        
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double headLength = Math.max(15, manager.getCurrentLineWidth() * 3);
        
        double angle1 = angle - Math.PI / 6;
        double angle2 = angle + Math.PI / 6;
        
        double px1 = x2 - headLength * Math.cos(angle1);
        double py1 = y2 - headLength * Math.sin(angle1);
        double px2 = x2 - headLength * Math.cos(angle2);
        double py2 = y2 - headLength * Math.sin(angle2);
        
        gc.beginPath();
        gc.moveTo(x2, y2);
        gc.lineTo(px1, py1);
        gc.stroke();
        
        gc.beginPath();
        gc.moveTo(x2, y2);
        gc.lineTo(px2, py2);
        gc.stroke();
    }
}