package es.elprofesoremilio.zoomdraw.ui;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

public class CanvasRenderer {

    private final Canvas canvas;
    private final double width;
    private final double height;

    public CanvasRenderer(double width, double height) {
        this.width = width;
        this.height = height;
        this.canvas = new Canvas(width, height);
    }

    public Canvas getCanvas() {
        return canvas;
    }

    /**
     * Dibuja el contenido inicial del canvas.
     * En el futuro, aquí se inyectará la captura de pantalla usando ScreenCaptureUtils.
     */
    public void dibujarFondoInicial() {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, width, height);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 28));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText("Pulse ESC para salir", width / 2, height / 2);

        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", FontWeight.NORMAL, 16));
        gc.fillText("(o CTRL+1 para activar/desactivar)", width / 2, height / 2 + 44);
    }
}