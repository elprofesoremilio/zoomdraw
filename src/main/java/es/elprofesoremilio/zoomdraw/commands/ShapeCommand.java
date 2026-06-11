package es.elprofesoremilio.zoomdraw.commands;

import es.elprofesoremilio.zoomdraw.core.DrawMode;
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
    private final WritableImage canvasSnapshot;

    public ShapeCommand(Point2D start, Point2D end, DrawMode mode, Color color, double lineWidth, WritableImage canvasSnapshot) {
        this.start = start;
        this.end = end;
        this.mode = mode;
        this.color = color;
        this.lineWidth = lineWidth;
        this.canvasSnapshot = canvasSnapshot;
    }

    @Override
    public void execute(GraphicsContext gc) {
        gc.setStroke(color);
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
                gc.setFill(color);
                gc.fillRect(rxF, ryF, rwF, rhF);
                break;
            case CIRCLE:
                double radius = Math.hypot(x2 - x1, y2 - y1);
                gc.strokeOval(x1 - radius, y1 - radius, radius * 2, radius * 2);
                break;
            case FILLED_CIRCLE:
                double radiusF = Math.hypot(x2 - x1, y2 - y1);
                gc.setFill(color);
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
                gc.setFill(color);
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
        if (w <= 0 || h <= 0 || canvasSnapshot == null) return;
        
        int startX = (int) Math.max(0, x);
        int startY = (int) Math.max(0, y);
        int endX = (int) Math.min(canvasSnapshot.getWidth(), x + w);
        int endY = (int) Math.min(canvasSnapshot.getHeight(), y + h);
        
        int width = endX - startX;
        int height = endY - startY;
        
        if (width <= 0 || height <= 0) return;

        int blockSize = 15;
        
        javafx.scene.image.PixelReader reader = canvasSnapshot.getPixelReader();
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

    public Point2D getStart() {
        return start;
    }

    public Point2D getEnd() {
        return end;
    }

    public DrawMode getMode() {
        return mode;
    }

    public Color getColor() {
        return color;
    }

    public double getLineWidth() {
        return lineWidth;
    }

    public WritableImage getCanvasSnapshot() {
        return canvasSnapshot;
    }

    public boolean intersects(Point2D circleCenter, double circleRadius) {
        double x1 = start.getX();
        double y1 = start.getY();
        double x2 = end.getX();
        double y2 = end.getY();

        // Umbral efectivo: radio del borrador + la mitad del grosor del trazo
        double effectiveRadius = circleRadius + lineWidth / 2.0;

        switch (mode) {
            case LINE:
            case ARROW:
                return distanceToSegment(circleCenter, start, end) <= effectiveRadius;

            case RECTANGLE: {
                // Forma sin relleno: solo se detecta si el borrador toca uno de los 4 bordes
                double minX = Math.min(x1, x2);
                double maxX = Math.max(x1, x2);
                double minY = Math.min(y1, y2);
                double maxY = Math.max(y1, y2);
                Point2D tl = new Point2D(minX, minY);
                Point2D tr = new Point2D(maxX, minY);
                Point2D bl = new Point2D(minX, maxY);
                Point2D br = new Point2D(maxX, maxY);
                return distanceToSegment(circleCenter, tl, tr) <= effectiveRadius  // borde superior
                    || distanceToSegment(circleCenter, bl, br) <= effectiveRadius  // borde inferior
                    || distanceToSegment(circleCenter, tl, bl) <= effectiveRadius  // borde izquierdo
                    || distanceToSegment(circleCenter, tr, br) <= effectiveRadius; // borde derecho
            }

            case FILLED_RECTANGLE:
            case CENSOR_RECTANGLE: {
                // Forma con relleno: se detecta si el borrador toca el área interior
                double minX = Math.min(x1, x2);
                double maxX = Math.max(x1, x2);
                double minY = Math.min(y1, y2);
                double maxY = Math.max(y1, y2);
                double closestX = Math.max(minX, Math.min(circleCenter.getX(), maxX));
                double closestY = Math.max(minY, Math.min(circleCenter.getY(), maxY));
                double dist = Math.hypot(circleCenter.getX() - closestX, circleCenter.getY() - closestY);
                return dist <= effectiveRadius;
            }

            case CIRCLE: {
                // Forma sin relleno: solo se detecta si el borrador toca el borde circular
                double radius = Math.hypot(x2 - x1, y2 - y1);
                double dist = start.distance(circleCenter);
                return Math.abs(dist - radius) <= effectiveRadius;
            }

            case FILLED_CIRCLE: {
                // Forma con relleno: se detecta si el borrador toca el área interior
                double radius = Math.hypot(x2 - x1, y2 - y1);
                double dist = start.distance(circleCenter);
                return dist <= radius + effectiveRadius;
            }

            case ELLIPSE: {
                // Forma sin relleno: se detecta si el borrador está en la banda anular del borde
                double minX = Math.min(x1, x2);
                double maxX = Math.max(x1, x2);
                double minY = Math.min(y1, y2);
                double maxY = Math.max(y1, y2);
                double a = (maxX - minX) / 2.0; // semiejex
                double b = (maxY - minY) / 2.0; // semiejey
                double cx = (minX + maxX) / 2.0;
                double cy = (minY + maxY) / 2.0;
                if (a <= 0 || b <= 0) return false;
                double dx = circleCenter.getX() - cx;
                double dy = circleCenter.getY() - cy;
                // Dentro de la elipse exterior (a+r, b+r) y fuera de la elipse interior (a-r, b-r)
                double aOuter = a + effectiveRadius;
                double bOuter = b + effectiveRadius;
                double aInner = Math.max(0, a - effectiveRadius);
                double bInner = Math.max(0, b - effectiveRadius);
                boolean insideOuter = (dx * dx) / (aOuter * aOuter) + (dy * dy) / (bOuter * bOuter) <= 1.0;
                boolean outsideInner = (aInner <= 0 || bInner <= 0)
                    || (dx * dx) / (aInner * aInner) + (dy * dy) / (bInner * bInner) >= 1.0;
                return insideOuter && outsideInner;
            }

            case FILLED_ELLIPSE: {
                // Forma con relleno: se detecta si el borrador toca el área interior
                double minX = Math.min(x1, x2);
                double maxX = Math.max(x1, x2);
                double minY = Math.min(y1, y2);
                double maxY = Math.max(y1, y2);
                double a = (maxX - minX) / 2.0;
                double b = (maxY - minY) / 2.0;
                double cx = (minX + maxX) / 2.0;
                double cy = (minY + maxY) / 2.0;
                if (a <= 0 || b <= 0) return false;
                double dx = circleCenter.getX() - cx;
                double dy = circleCenter.getY() - cy;
                double aOuter = a + effectiveRadius;
                double bOuter = b + effectiveRadius;
                return (dx * dx) / (aOuter * aOuter) + (dy * dy) / (bOuter * bOuter) <= 1.0;
            }

            default:
                return false;
        }
    }

    private double distanceToSegment(Point2D p, Point2D s1, Point2D s2) {
        double x = p.getX();
        double y = p.getY();
        double x1 = s1.getX();
        double y1 = s1.getY();
        double x2 = s2.getX();
        double y2 = s2.getY();
        
        double l2 = Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2);
        if (l2 == 0) return Math.hypot(x - x1, y - y1);
        
        double t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / l2;
        t = Math.max(0, Math.min(1, t));
        
        double projX = x1 + t * (x2 - x1);
        double projY = y1 + t * (y2 - y1);
        
        return Math.hypot(x - projX, y - projY);
    }
}
