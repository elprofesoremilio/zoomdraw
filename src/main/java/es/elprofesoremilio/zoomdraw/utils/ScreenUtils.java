package es.elprofesoremilio.zoomdraw.utils;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import javafx.stage.Screen;

import java.awt.*;

public class ScreenUtils {

    /**
     * Gets the full bounds of the screen where the mouse cursor is located.
     */
    /**
     * Gets the full bounds of the screen where the mouse cursor is located.
     */
    public static Rectangle2D getScreenBoundsAtCursor() {
        Point cursor = MouseInfo.getPointerInfo().getLocation();
        System.out.println("\n📍 Cursor físico (AWT): X=" + cursor.x + ", Y=" + cursor.y);

        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getBounds();
            System.out.println("🖥️ Monitor detectado -> Inicio X: " + bounds.getMinX() + " | Ancho: " + bounds.getWidth());

            if (bounds.contains(cursor.getX(), cursor.getY())) {
                System.out.println("✅ ¡Match! Asignando lienzo al monitor que empieza en X=" + bounds.getMinX() + "\n");
                return bounds;
            }
        }

        System.out.println("⚠️ Alerta: No hubo match exacto. Usando principal.\n");
        return Screen.getPrimary().getBounds();
    }
    /**
     * Captura la pantalla en los bounds especificados.
     */
    /**
     * Captura la pantalla en los bounds especificados usando el motor nativo de JavaFX.
     */
    public static WritableImage captureScreen(Rectangle2D bounds) {
        try {
            // Usamos el Robot moderno de JavaFX, que es instantáneo y nativo.
            javafx.scene.robot.Robot fxRobot = new javafx.scene.robot.Robot();
            return fxRobot.getScreenCapture(null, bounds);
        } catch (Exception e) {
            System.err.println("Error nativo al capturar pantalla: " + e.getMessage());
            return null;
        }
    }

}