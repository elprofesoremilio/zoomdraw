package es.elprofesoremilio.zoomdraw.utils;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import java.awt.MouseInfo;
import java.awt.Point;

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
}