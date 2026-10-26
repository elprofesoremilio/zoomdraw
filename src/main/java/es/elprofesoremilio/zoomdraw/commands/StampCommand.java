package es.elprofesoremilio.zoomdraw.commands;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

/**
 * Draws a vector symbol (check or cross) centred on a point, in original (pre-zoom) coordinates.
 * Drawn with paths instead of font glyphs so it looks identical on Windows and Linux.
 */
public class StampCommand implements DrawingCommand {

    public enum Type { CHECK, CROSS }

    private final Type type;
    private final Point2D center;
    private final double size;
    private final Color color;

    public StampCommand(Type type, Point2D center, double size, Color color) {
        this.type = type;
        this.center = center;
        this.size = size;
        this.color = color;
    }

    public boolean intersects(Point2D circleCenter, double circleRadius) {
        return center.distance(circleCenter) <= circleRadius + size / 2.0;
    }

    @Override
    public void execute(GraphicsContext gc) {
        double cx = center.getX();
        double cy = center.getY();
        double h = size / 2.0;

        gc.save();
        gc.setStroke(color);
        gc.setLineWidth(size * 0.18);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);
        if (type == Type.CHECK) {
            gc.beginPath();
            gc.moveTo(cx - h * 0.9, cy + h * 0.05);
            gc.lineTo(cx - h * 0.3, cy + h * 0.7);
            gc.lineTo(cx + h * 0.95, cy - h * 0.75);
            gc.stroke();
        } else {
            double d = h * 0.8;
            gc.strokeLine(cx - d, cy - d, cx + d, cy + d);
            gc.strokeLine(cx - d, cy + d, cx + d, cy - d);
        }
        gc.restore();
    }
}
