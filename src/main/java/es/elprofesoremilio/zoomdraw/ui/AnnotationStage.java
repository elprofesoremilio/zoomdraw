package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AnnotationStage extends Stage {

    private final AnnotationManager manager;
    private final DrawingCanvas drawingCanvas; // Use the new DrawingCanvas
    private final AnnotationInputHandler inputHandler; // New input handler

    public AnnotationStage(AnnotationManager manager, Rectangle2D bounds, WritableImage background) {
        super(StageStyle.TRANSPARENT);
        this.manager = manager;

        // Instantiate DrawingCanvas
        this.drawingCanvas = new DrawingCanvas(manager, bounds.getWidth(), bounds.getHeight(), background);

        StackPane root = new StackPane(drawingCanvas); // Pass drawingCanvas to StackPane
        root.setBackground(null);
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);

        // Instantiate and attach the input handler
        this.inputHandler = new AnnotationInputHandler(manager, drawingCanvas);
        this.inputHandler.attach(scene);

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
}