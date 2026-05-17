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

        // 2. Configurar el pincel RECUPERANDO EL ESTADO GUARDADO del Manager
        gc.setStroke(manager.getCurrentColor());
        gc.setLineWidth(manager.getCurrentLineWidth());
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        StackPane root = new StackPane(canvas);
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

        // --- CONTROL DE GROSOR CON CTRL + RUEDA DE RATÓN ---
        scene.setOnScroll(event -> {
            if (event.isControlDown()) {
                double newWidth = manager.getCurrentLineWidth();
                if (event.getDeltaY() > 0) {
                    // Rueda hacia arriba: aumenta grosor (igual que la tecla +)
                    newWidth = Math.min(50.0, newWidth + 2.0);
                } else if (event.getDeltaY() < 0) {
                    // Rueda hacia abajo: disminuye grosor (igual que la tecla -)
                    newWidth = Math.max(1.0, newWidth - 2.0);
                }
                manager.setCurrentLineWidth(newWidth);
                gc.setLineWidth(newWidth);
                event.consume();
            }
        });

        // 4. EL CEREBRO DE LAS TECLAS (Colores, Grosor, Opacidad y Salida)
        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            boolean isShift = event.isShiftDown();

            // SALIDA
            if (code == KeyCode.ESCAPE) {
                manager.stopAnnotationMode();
                event.consume();
                return;
            }

            // --- LÓGICA DE COLORES ---
            Color newBaseColor = null;
            switch (code) {
                case R: newBaseColor = Color.RED; break;
                case G: newBaseColor = Color.GREEN; break;
                case B: newBaseColor = Color.BLUE; break;
                case Y: newBaseColor = Color.YELLOW; break;
                case O: newBaseColor = Color.ORANGE; break;
                case P: newBaseColor = Color.MAGENTA; break; // Rosa/Morado
                case K: newBaseColor = Color.BLACK; break;
                case W: newBaseColor = Color.WHITE; break;
                default: break;
            }

            if (newBaseColor != null) {
                // Si Shift está pulsado, aplicamos la opacidad por defecto del manager. Si no, 1.0 (opaco)
                double alpha = isShift ? manager.getDefaultSemiTransparentOpacity() : 1.0;
                Color finalColor = new Color(newBaseColor.getRed(), newBaseColor.getGreen(), newBaseColor.getBlue(), alpha);

                manager.setCurrentColor(finalColor);
                gc.setStroke(finalColor);
                event.consume();
                return;
            }

            // --- LÓGICA DE GROSOR (Flechas Arriba / Abajo) ---
            if (code == KeyCode.UP || code == KeyCode.PLUS || code == KeyCode.ADD) {
                double newWidth = Math.min(50.0, manager.getCurrentLineWidth() + 2.0);
                manager.setCurrentLineWidth(newWidth);
                gc.setLineWidth(newWidth);
                event.consume();
            }
            else if (code == KeyCode.DOWN || code == KeyCode.MINUS || code == KeyCode.SUBTRACT) {
                double newWidth = Math.max(1.0, manager.getCurrentLineWidth() - 2.0);
                manager.setCurrentLineWidth(newWidth);
                gc.setLineWidth(newWidth);
                event.consume();
            }

            // --- OPACIDAD GLOBAL DEL TRAZO (Flechas Izquierda / Derecha) ---
            if (code == KeyCode.LEFT) {
                // Disminuir opacidad global en saltos del 10%
                Color current = manager.getCurrentColor();
                double newAlpha = Math.max(0.1, current.getOpacity() - 0.1);
                Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
                manager.setCurrentColor(updatedColor);
                gc.setStroke(updatedColor);
                event.consume();
            }
            else if (code == KeyCode.RIGHT) {
                // Aumentar opacidad global en saltos del 10%
                Color current = manager.getCurrentColor();
                double newAlpha = Math.min(1.0, current.getOpacity() + 0.1);
                Color updatedColor = new Color(current.getRed(), current.getGreen(), current.getBlue(), newAlpha);
                manager.setCurrentColor(updatedColor);
                gc.setStroke(updatedColor);
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