package es.elprofesoremilio.zoomdraw.core.commands;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class ShapeCommand implements DrawingCommand {
    private final Point2D start;
    private final Point2D end;
    private final DrawMode mode;
    private final Color color;
    private final double lineWidth;
    private final WritableImage snapshot;

    public ShapeCommand(Point2D start, Point2D end, DrawMode mode, Color color, double lineWidth, WritableImage snapshot) {
        this.start = start;
        this.end = end;
        this.mode = mode;
        this.color = color;
        this.lineWidth = lineWidth;
        this.snapshot = snapshot; // Only used for CENSOR_RECTANGLE
    }

    @Override
    public void execute(GraphicsContext gc) {
        gc.setStroke(color);
        gc.setFill(color);
        gc.setLineWidth(lineWidth);
        gc.setLineCap(StrokeLineCap.ROUND);
        gc.setLineJoin(StrokeLineJoin.ROUND);

        double x1 = start.getX();
        double y1 = start.getY();
        double x2 = end.getX();
        double y2 = end.getY();

        switch (mode) {
            case LINE:
                gc.strokeLine(x1, y1, x2, y2);
                break;
            case RECTANGLE:
                double rx = Math.min(x1, x2);
                double ry = Math.min(y1, y2);
                double rw = Math.abs(x1 - x2);
                double rh = Math.abs(y1 - y2);
                gc.strokeRect(rx, ry, rw, rh);
                break;
            case FILLED_RECTANGLE:
                double rxF = Math.min(x1, x2);
                double ryF = Math.min(y1, y2);
                double rwF = Math.abs(x1 - x2);
                double rhF = Math.abs(y1 - y2);
                gc.fillRect(rxF, ryF, rwF, rhF);
                break;
            case CIRCLE:
                double radius = Math.hypot(x2 - x1, y2 - y1);
                gc.strokeOval(x1 - radius, y1 - radius, radius * 2, radius * 2);
                break;
            case FILLED_CIRCLE:
                double radiusF = Math.hypot(x2 - x1, y2 - y1);
                gc.fillOval(x1 - radiusF, y1 - radiusF, radiusF * 2, radiusF * 2);
                break;
            case ELLIPSE:
                double ex = Math.min(x1, x2);
                double ey = Math.min(y1, y2);
                double ew = Math.abs(x1 - x2);
                double eh = Math.abs(y1 - y2);
                gc.strokeOval(ex, ey, ew, eh);
                break;
            case FILLED_ELLIPSE:
                double exF = Math.min(x1, x2);
                double eyF = Math.min(y1, y2);
                double ewF = Math.abs(x1 - x2);
                double ehF = Math.abs(y1 - y2);
                gc.fillOval(exF, eyF, ewF, ehF);
                break;
            case CENSOR_RECTANGLE:
                double cx = Math.min(x1, x2);
                double cy = Math.min(y1, y2);
                double cw = Math.abs(x1 - x2);
                double ch = Math.abs(y1 - y2);
                drawCensoredRect(gc, cx, cy, cw, ch);
                break;
            case ARROW:
                drawArrow(gc, x1, y1, x2, y2);
                break;
            default:
                break;
        }
    }

    private void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2) {
        gc.strokeLine(x1, y1, x2, y2);
        
        double angle = Math.atan2(y2 - y1, x2 - x1);
        double headLength = Math.max(15, lineWidth * 3);
        
        double angle1 = angle - Math.PI / 6;
        double angle2 = angle + Math.PI / 6;
        
        double px1 = x2 - headLength * Math.cos(angle1);
        double py1 = y2 - headLength * Math.sin(angle1);
        double px2 = x2 - headLength * Math.cos(angle2);
        double py2 = y2 - headLength * Math.sin(angle2);
        
        gc.beginPath();
        gc.moveTo(x2, y2);
        gc.lineTo(px1, py1);
        gc.stroke();
        
        gc.beginPath();
        gc.moveTo(x2, y2);
        gc.lineTo(px2, py2);
        gc.stroke();
    }

    private void drawCensoredRect(GraphicsContext gc, double x, double y, double w, double h) {
        if (w <= 0 || h <= 0 || snapshot == null) return;
        
        int startX = (int) Math.max(0, x);
        int startY = (int) Math.max(0, y);
        int endX = (int) Math.min(snapshot.getWidth(), x + w);
        int endY = (int) Math.min(snapshot.getHeight(), y + h);
        
        int width = endX - startX;
        int height = endY - startY;
        
        if (width <= 0 || height <= 0) return;

        int blockSize = 15;
        
        javafx.scene.image.PixelReader reader = snapshot.getPixelReader();
        WritableImage censoredImage = new WritableImage(width, height);
        javafx.scene.image.PixelWriter writer = censoredImage.getPixelWriter();

        for (int by = 0; by < height; by += blockSize) {
            for (int bx = 0; bx < width; bx += blockSize) {
                int bw = Math.min(blockSize, width - bx);
                int bh = Math.min(blockSize, height - by);
                
                long r = 0, g = 0, b = 0, a = 0;
                int count = 0;
                
                for (int py = 0; py < bh; py++) {
                    for (int px = 0; px < bw; px++) {
                        int argb = reader.getArgb(startX + bx + px, startY + by + py);
                        a += (argb >>> 24) & 0xFF;
                        r += (argb >> 16) & 0xFF;
                        g += (argb >> 8) & 0xFF;
                        b += argb & 0xFF;
                        count++;
                    }
                }
                
                if (count > 0) {
                    int avgA = (int) (a / count);
                    int avgR = (int) (r / count);
                    int avgG = (int) (g / count);
                    int avgB = (int) (b / count);
                    int avgArgb = (avgA << 24) | (avgR << 16) | (avgG << 8) | avgB;
                    
                    for (int py = 0; py < bh; py++) {
                        for (int px = 0; px < bw; px++) {
                            writer.setArgb(bx + px, by + py, avgArgb);
                        }
                    }
                }
            }
        }
        
        gc.drawImage(censoredImage, startX, startY);
    }
}
