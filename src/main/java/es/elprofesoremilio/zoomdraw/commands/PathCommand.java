package es.elprofesoremilio.zoomdraw.commands;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import java.util.ArrayList;
import java.util.List;

public class PathCommand implements DrawingCommand {
    private final List<Point2D> points;
    private final Color color;
    private final double lineWidth;

    public PathCommand(List<Point2D> points, Color color, double lineWidth) {
        this.points = new ArrayList<>(points); // Copy to prevent modification
        this.color = color;
        this.lineWidth = lineWidth;
    }

    @Override
    public void execute(GraphicsContext gc) {
        if (points.size() < 2) {
            return;
        }
        gc.setStroke(color);
        gc.setLineWidth(lineWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        gc.beginPath();
        gc.moveTo(points.get(0).getX(), points.get(0).getY());
        for (int i = 1; i < points.size(); i++) {
            gc.lineTo(points.get(i).getX(), points.get(i).getY());
        }
        gc.stroke();
    }

    public List<Point2D> getPoints() {
        return points;
    }

    public Color getColor() {
        return color;
    }

    public double getLineWidth() {
        return lineWidth;
    }
}
