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

        // Mouse events for drawing
        scene.setOnMousePressed(event -> {
            currentStrokePoints.clear();
            currentStrokePoints.add(new Point2D(event.getX(), event.getY()));
        });

        scene.setOnMouseDragged(event -> {
            currentStrokePoints.add(new Point2D(event.getX(), event.getY()));
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
            redrawStroke(gcTemporal, currentStrokePoints);
        });

        scene.setOnMouseReleased(event -> {
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
            redrawStroke(gcPermanent, currentStrokePoints);
            currentStrokePoints.clear();
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
}