package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Custom Canvas for handling drawing operations and displaying the background.
 */
public class DrawingCanvas extends Canvas {

    private final AnnotationManager manager;
    private final GraphicsContext gc;

    public DrawingCanvas(AnnotationManager manager, double width, double height, WritableImage background) {
        super(width, height);
        this.manager = manager;
        this.gc = getGraphicsContext2D();

        initializeGraphicsContext();
        drawBackground(background);
        setupDrawingHandlers();
    }

    private void initializeGraphicsContext() {
        // Configurar estilo de dibujo por defecto (o recuperar del manager)
        gc.setStroke(manager.getCurrentColor());
        gc.setLineWidth(manager.getCurrentLineWidth());
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
    }

    private void drawBackground(WritableImage background) {
        if (background != null) {
            gc.drawImage(background, 0, 0);
        } else {
            // Fallback (Plan B): Un cristal hiper-transparente en vez de gris oscuro.
            // Opacidad 0.01 es invisible al ojo humano, pero X11 lo detecta como "sólido"
            // para que los clics no se cuelen a las ventanas de atrás.
            gc.setFill(new Color(1.0, 1.0, 1.0, 0.01));
            gc.fillRect(0, 0, getWidth(), getHeight());
            System.out.println("Fondo capturado nulo, usando cristal transparente.");
        }
    }

    private void setupDrawingHandlers() {
        setOnMousePressed(e -> {
            gc.beginPath();
            gc.moveTo(e.getX(), e.getY());
            gc.stroke();
        });

        setOnMouseDragged(e -> {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        });
    }

    /**
     * Updates the GraphicsContext with the current brush settings from the AnnotationManager.
     * This method should be called when brush settings change.
     */
    public void updateBrushSettings() {
        gc.setStroke(manager.getCurrentColor());
        gc.setLineWidth(manager.getCurrentLineWidth());
    }
}
