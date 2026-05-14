package es.elprofesoremilio.zoomdraw.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import javafx.stage.Screen;

import java.awt.*;

public class ScreenUtils {

    /**
     * Gets the full bounds of the screen where the mouse cursor is located.
     */
    public static Rectangle2D getScreenBoundsAtCursor() {
        Point cursor = MouseInfo.getPointerInfo().getLocation();
        for (Screen screen : Screen.getScreens()) {
            Rectangle2D bounds = screen.getBounds();
            if (bounds.contains(cursor.getX(), cursor.getY())) {
                return bounds;
            }
        }
        return Screen.getPrimary().getBounds();
    }
    /**
     * Captura la pantalla en los bounds especificados.
     */
    public static WritableImage captureScreen(Rectangle2D bounds) {
        // Creamos un hilo temporal para la captura
        final WritableImage[] result = new WritableImage[1];
        Thread captureThread = new Thread(() -> {
            try {
                Robot robot = new Robot();
                java.awt.Rectangle rect = new java.awt.Rectangle(
                        (int)bounds.getMinX(), (int)bounds.getMinY(),
                        (int)bounds.getWidth(), (int)bounds.getHeight());
                result[0] = SwingFXUtils.toFXImage(robot.createScreenCapture(rect), null);
            } catch (Exception ignored) {}
        });

        captureThread.start();
        try {
            // Esperamos máximo 800ms. Si GNOME no responde, abortamos.
            captureThread.join(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (result[0] == null) {
            System.err.println("Captura bloqueada por GNOME/Wayland. Usando fondo de emergencia.");
            return null; // El AnnotationStage ya maneja el fondo gris si esto es null
        }
        return result[0];
    }
//    public static WritableImage captureScreen(Rectangle2D bounds) {
//        try {
//            Robot robot = new Robot();
//            java.awt.Rectangle screenRect = new java.awt.Rectangle(
//                    (int) bounds.getMinX(),
//                    (int) bounds.getMinY(),
//                    (int) bounds.getWidth(),
//                    (int) bounds.getHeight()
//            );
//            System.out.println("DEBUG: Ejecutando createScreenCapture...");
//            java.awt.image.BufferedImage screenFullImage = robot.createScreenCapture(screenRect);
//            System.out.println("DEBUG: Captura AWT realizada con éxito.");
//
//            return javafx.embed.swing.SwingFXUtils.toFXImage(screenFullImage, null);
//        } catch (AWTException e) {
//            System.out.println("Imposible capturar pantalla");
//            return null;
//        }
//    }
}