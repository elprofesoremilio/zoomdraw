package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AnnotationStage extends Stage {

    private final AnnotationManager manager;
    private final Canvas canvas;

    public AnnotationStage(AnnotationManager manager, Rectangle2D bounds) {
        super(StageStyle.TRANSPARENT);
        this.manager = manager;

        this.canvas = new Canvas(bounds.getWidth(), bounds.getHeight());
        drawPlaceholderContent(bounds.getWidth(), bounds.getHeight());

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: black;");

        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.BLACK);

        // Atajo ESC local (cuando tiene foco)
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                manager.stopAnnotationMode();
                event.consume();
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
        this.setX(bounds.getMinX());
        this.setY(bounds.getMinY());
        this.setWidth(bounds.getWidth());
        this.setHeight(bounds.getHeight());
        this.setResizable(false);
    }

    private void drawPlaceholderContent(double w, double h) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 28));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("Press ESC to exit", w / 2, h / 2);

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", FontWeight.NORMAL, 16));
        gc.fillText("(or CTRL+1 to toggle)", w / 2, h / 2 + 44);
    }
}