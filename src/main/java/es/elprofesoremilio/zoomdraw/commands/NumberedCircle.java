package es.elprofesoremilio.zoomdraw.commands;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class NumberedCircle {
    private Point2D center; // 1x coordinates
    private int number;
    private Color color;
    private double lineWidth;
    private double opacity;

    public NumberedCircle(Point2D center, int number, Color color, double lineWidth, double opacity) {
        this.center = center;
        this.number = number;
        this.color = color;
        this.lineWidth = lineWidth;
        this.opacity = opacity;
    }

    public NumberedCircle(NumberedCircle other) {
        this.center = new Point2D(other.center.getX(), other.center.getY());
        this.number = other.number;
        this.color = other.color;
        this.lineWidth = other.lineWidth;
        this.opacity = other.opacity;
    }

    public Point2D getCenter() {
        return center;
    }

    public void setCenter(Point2D center) {
        this.center = center;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public Color getColor() {
        return color;
    }

    public double getLineWidth() {
        return lineWidth;
    }

    public double getOpacity() {
        return opacity;
    }

    public void draw(GraphicsContext gc) {
        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, lineWidth * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
        gc.save();
        
        // 1. Draw the filled circle using circle color
        gc.setFill(color);
        gc.fillOval(center.getX() - radius, center.getY() - radius, radius * 2, radius * 2);

        // 2. High-contrast text color based on background color luminance
        double luminance = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
        Color textColor = (luminance > 0.5) ? Color.BLACK : Color.WHITE;
        Color finalTextColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), color.getOpacity());
        gc.setFill(finalTextColor);

        // 3. Draw text centered
        String text = String.valueOf(number);
        gc.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, radius * 0.9));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText(text, center.getX(), center.getY());
        
        gc.restore();
    }

    public void drawSelection(GraphicsContext gc) {
        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, lineWidth * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
        gc.save();
        
        double luminance = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
        Color selectionColor = (luminance > 0.5) ? Color.BLUE : Color.YELLOW;
        
        gc.setStroke(selectionColor);
        gc.setLineWidth(3.0);
        gc.strokeOval(center.getX() - radius - 3, center.getY() - radius - 3, (radius + 3) * 2, (radius + 3) * 2);
        
        gc.restore();
    }

    public void drawHoverHalo(GraphicsContext gc) {
        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, lineWidth * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
        gc.save();
        
        gc.setFill(new Color(1.0, 1.0, 1.0, 0.4));
        gc.fillOval(center.getX() - radius - 6, center.getY() - radius - 6, (radius + 6) * 2, (radius + 6) * 2);
        
        gc.restore();
    }
}
