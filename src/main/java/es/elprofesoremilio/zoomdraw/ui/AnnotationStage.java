package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AnnotationStage extends Stage {

    private final AnnotationManager manager;
    private final Canvas canvas;
    private final GraphicsContext gc;

    public AnnotationStage(AnnotationManager manager, Rectangle2D bounds, WritableImage background) {
        super(StageStyle.TRANSPARENT);
        this.manager = manager;

        this.canvas = new Canvas(bounds.getWidth(), bounds.getHeight());
        this.gc = canvas.getGraphicsContext2D();

        // Configurar estilo de dibujo por defecto (v0.2: Rojo, 3px)
        gc.setStroke(Color.RED);
        gc.setLineWidth(3.0);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        // 1. Dibujar el fondo capturado
        if (background != null) {
            gc.drawImage(background, 0, 0);
        } else {
            // Fallback (Plan B): Un cristal hiper-transparente en vez de gris oscuro.
            // Opacidad 0.01 es invisible al ojo humano, pero X11 lo detecta como "sólido"
            // para que los clics no se cuelen a las ventanas de atrás.
            gc.setFill(new Color(1.0, 1.0, 1.0, 0.01));
            gc.fillRect(0, 0, bounds.getWidth(), bounds.getHeight());
            System.out.println("Fondo capturado nulo, usando cristal transparente.");
        }

        // 2. Configurar el pincel por defecto (v0.2)
        gc.setStroke(Color.RED);
        gc.setLineWidth(3);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        gc.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);

        StackPane root = new StackPane(canvas);
        // Asegúrate de que el root no sea negro opaco si quieres ver la captura
        root.setBackground(null);
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);

        // 3. Lógica de Trazado Libre
        canvas.setOnMousePressed(e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
        });

        canvas.setOnMouseDragged(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        });

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

        // Posicionamiento absoluto
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