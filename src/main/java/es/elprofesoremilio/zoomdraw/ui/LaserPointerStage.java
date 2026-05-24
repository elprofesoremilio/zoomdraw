package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import es.elprofesoremilio.zoomdraw.utils.AppLogger;

public class LaserPointerStage extends Stage {

    private final Canvas canvas;
    private final GraphicsContext gc;

    public LaserPointerStage() {
        super(StageStyle.TRANSPARENT);
        
        // Fixed size of 300x300 is perfectly sized for any custom cursor size (radius up to 100)
        canvas = new Canvas(300, 300);
        gc = canvas.getGraphicsContext2D();

        redraw();

        StackPane root = new StackPane(canvas);
        root.setBackground(null);
        Scene scene = new Scene(root, 300, 300, Color.TRANSPARENT);

        setScene(scene);
        setAlwaysOnTop(true);
        setResizable(false);

        // Apply native click-through on Windows so the window is 100% mouse-transparent and non-focusable
        setOnShown(event -> makeClickThrough());
    }

    /**
     * Redraws the laser pointer circle and cross based on current settings in AppConfig.
     */
    public void redraw() {
        gc.clearRect(0, 0, 300, 300);

        double radius = AppConfig.circleRadius;
        double centerX = 150;
        double centerY = 150;

        // 1. Draw the outer border circle with configured opacity
        Color baseColor = AppConfig.borderColor;
        Color strokeColor = new Color(
                baseColor.getRed(),
                baseColor.getGreen(),
                baseColor.getBlue(),
                AppConfig.borderOpacity
        );

        gc.setStroke(strokeColor);
        gc.setLineWidth(AppConfig.bortherThickness);
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        // 2. Draw the central cross if active
        if (AppConfig.showCross) {
            gc.setStroke(AppConfig.crossColor);
            gc.setLineWidth(AppConfig.crossThickness);
            
            // Draw cross extending 6px from the center in each direction
            gc.strokeLine(centerX - 6, centerY, centerX + 6, centerY);
            gc.strokeLine(centerX, centerY - 6, centerX, centerY + 6);
        }
    }

    /**
     * Updates the position of the window, centering it at the provided coordinates.
     */
    public void updatePosition(double x, double y) {
        setX(x - 150);
        setY(y - 150);
    }

    /**
     * Applies Windows WS_EX_TRANSPARENT and WS_EX_NOACTIVATE flags using JNA User32.
     */
    private void makeClickThrough() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            try {
                // Temporarily rename window to find its HWND reliably
                String uniqueTitle = "ZDLaserPtr_" + System.nanoTime();
                setTitle(uniqueTitle);

                com.sun.jna.platform.win32.WinDef.HWND hwnd =
                    com.sun.jna.platform.win32.User32.INSTANCE.FindWindow(null, uniqueTitle);
                setTitle(""); // restore

                if (hwnd != null) {
                    // GWL_EXSTYLE = -20
                    final int GWL_EXSTYLE = -20;
                    // WS_EX_TRANSPARENT = 0x20, WS_EX_NOACTIVATE = 0x08000000
                    final int WS_EX_TRANSPARENT = 0x00000020;
                    final int WS_EX_NOACTIVATE  = 0x08000000;

                    int exStyle = com.sun.jna.platform.win32.User32.INSTANCE
                            .GetWindowLong(hwnd, GWL_EXSTYLE);
                    exStyle |= WS_EX_TRANSPARENT | WS_EX_NOACTIVATE;
                    com.sun.jna.platform.win32.User32.INSTANCE
                            .SetWindowLong(hwnd, GWL_EXSTYLE, exStyle);
                    AppLogger.log("✅ Click-Through nativo (JNA) aplicado al Puntero Láser.");
                } else {
                    AppLogger.log("⚠️ No se pudo obtener el HWND para aplicar click-through.");
                }
            } catch (Throwable t) {
                AppLogger.logError("Error al aplicar click-through con JNA", t);
            }
        }
    }
}
