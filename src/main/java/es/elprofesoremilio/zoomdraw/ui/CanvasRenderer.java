package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
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

        gc.setFill(AppConfig.RENDERER_BACKGROUND_COLOR);
        gc.fillRect(0, 0, width, height);

        gc.setFill(AppConfig.RENDERER_MAIN_TEXT_COLOR);
        gc.setFont(Font.font(AppConfig.RENDERER_MAIN_FONT_FAMILY, AppConfig.RENDERER_MAIN_FONT_WEIGHT, AppConfig.RENDERER_MAIN_FONT_SIZE));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText(AppConfig.RENDERER_MAIN_TEXT, width / 2, height / 2);

        gc.setFill(AppConfig.RENDERER_SUB_TEXT_COLOR);
        gc.setFont(Font.font(AppConfig.RENDERER_SUB_FONT_FAMILY, AppConfig.RENDERER_SUB_FONT_WEIGHT, AppConfig.RENDERER_SUB_FONT_SIZE));
        gc.fillText(AppConfig.RENDERER_SUB_TEXT, width / 2, height / 2 + AppConfig.RENDERER_SUB_TEXT_OFFSET_Y);
    }
}