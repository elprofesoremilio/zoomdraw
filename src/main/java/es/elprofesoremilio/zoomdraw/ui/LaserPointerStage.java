package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Screen; // <--- Nueva importación necesaria
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

        canvas = new Canvas(300, 300);
        gc = canvas.getGraphicsContext2D();
        redraw();
        StackPane root = new StackPane(canvas);
        root.setBackground(null);
        Scene scene = new Scene(root, 300, 300, Color.TRANSPARENT);
        setScene(scene);
        setAlwaysOnTop(true);
        setResizable(false);
        setOnShown(event -> makeClickThrough());
    }

    public void redraw() {
        redraw(150, 150);
    }

    private void redraw(double centerX, double centerY) {
        gc.clearRect(0, 0, 300, 300);
        double radius = AppConfig.circleRadius;
        Color baseColor = AppConfig.borderColor;
        Color strokeColor = new Color(
                baseColor.getRed(),
                baseColor.getGreen(),
                baseColor.getBlue(),
                AppConfig.borderOpacity
        );
        gc.setStroke(strokeColor);
        gc.setLineWidth(AppConfig.borderThickness);
        gc.strokeOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

        if (AppConfig.showCross) {
            gc.setStroke(AppConfig.crossColor);
            gc.setLineWidth(AppConfig.crossThickness);
            gc.strokeLine(centerX - 6, centerY, centerX + 6, centerY);
            gc.strokeLine(centerX, centerY - 6, centerX, centerY + 6);
        }
    }

    /**
     * Updates the position of the window, centering it at the provided coordinates.
     * Fully compatible with multi-monitor setups.
     */
    public void updatePosition(double x, double y) {
        double requestedX = x - 150;
        double requestedY = y - 150;

        // 1. Detectar en qué pantalla se encuentra el puntero del ratón actualmente
        Screen currentScreen = Screen.getPrimary();
        for (Screen screen : Screen.getScreens()) {
            if (screen.getBounds().contains(x, y)) {
                currentScreen = screen;
                break;
            }
        }

        // 2. Obtener el origen real de la pantalla actual (puede ser 0, positivo o negativo)
        double minX = currentScreen.getBounds().getMinX();
        double minY = currentScreen.getBounds().getMinY();

        // 3. Limitar (clamp) usando los límites reales de la pantalla actual
        double actualX = Math.max(minX, requestedX);
        double actualY = Math.max(minY, requestedY);

        setX(actualX);
        setY(actualY);

        // 4. Desfasar el centro del dibujo para compensar el límite de la pantalla actual
        double drawCenterX = 150 + (requestedX - actualX);
        double drawCenterY = 150 + (requestedY - actualY);

        redraw(drawCenterX, drawCenterY);
    }

    private void makeClickThrough() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            try {
                String uniqueTitle = "ZDLaserPtr_" + System.nanoTime();
                setTitle(uniqueTitle);
                com.sun.jna.platform.win32.WinDef.HWND hwnd =
                        com.sun.jna.platform.win32.User32.INSTANCE.FindWindow(null, uniqueTitle);
                setTitle("");
                if (hwnd != null) {
                    final int GWL_EXSTYLE = -20;
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