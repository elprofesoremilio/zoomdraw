package es.elprofesoremilio.zoomdraw.commands;

import es.elprofesoremilio.zoomdraw.core.text.TextBlock;
import es.elprofesoremilio.zoomdraw.core.text.TextRun;
import es.elprofesoremilio.zoomdraw.core.text.TextTokens;
import javafx.geometry.Point2D;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class TextCommand implements DrawingCommand {

    private final TextBlock textBlock;
    private boolean showCursor;

    public TextCommand(Point2D startPosition, Color initialColor, double initialSize) {
        // We map line width to font size using the configurable multiplier.
        double fontSize = Math.max(12.0, initialSize * es.elprofesoremilio.zoomdraw.config.AppConfig.TEXT_SIZE_MULTIPLIER);
        this.textBlock = new TextBlock(startPosition, initialColor, fontSize);
        this.showCursor = true;
    }

    public void append(String str, Color color, double size) {
        double fontSize = Math.max(12.0, size * es.elprofesoremilio.zoomdraw.config.AppConfig.TEXT_SIZE_MULTIPLIER);
        textBlock.appendCharacter(str, color, fontSize);
    }

    public void removeLast() {
        textBlock.removeLastCharacter();
    }

    public void setShowCursor(boolean show) {
        this.showCursor = show;
    }
    
    public boolean isEmpty() {
        return textBlock.isEmpty();
    }

    @Override
    public void execute(GraphicsContext gc) {
        Point2D currentPos = textBlock.getStartPosition();
        double currentX = currentPos.getX();
        double currentY = currentPos.getY();

        double maxHeightInLine = 0;
        
        // Initial defaults
        double lastSize = textBlock.getBaseSize();

        for (TextRun run : textBlock.getRuns()) {
            gc.setFill(run.getColor());
            Font font = Font.font(run.getSize());
            gc.setFont(font);
            lastSize = run.getSize();

            String textStr = run.getText();
            for (int i = 0; i < textStr.length(); i++) {
                char c = textStr.charAt(i);
                String strChar = String.valueOf(c);

                if (strChar.equals(TextTokens.ENTER_TOKEN)) {
                    currentX = textBlock.getStartPosition().getX();
                    currentY += (maxHeightInLine > 0 ? maxHeightInLine : lastSize * 1.2);
                    maxHeightInLine = 0;
                } else {
                    String renderStr = strChar;
                    if (strChar.equals(TextTokens.TAB_TOKEN)) {
                        renderStr = TextTokens.TAB_REPLACEMENT;
                    }
                    
                    Text textNode = new Text(renderStr);
                    textNode.setFont(font);
                    double width = textNode.getLayoutBounds().getWidth();
                    double height = textNode.getLayoutBounds().getHeight();
                    maxHeightInLine = Math.max(maxHeightInLine, height);
                    
                    // Note: fillText draws from the baseline, so we need to offset Y
                    gc.fillText(renderStr, currentX, currentY + height * 0.8);
                    currentX += width;
                }
            }
        }

        if (showCursor) {
            gc.setStroke(textBlock.getBaseColor());
            gc.setLineWidth(1.0);
            double cursorHeight = lastSize * 1.2;
            gc.strokeLine(currentX, currentY, currentX, currentY + cursorHeight);
        }
    }

    public Bounds getBounds() {
        Point2D startPos = textBlock.getStartPosition();
        double startX = startPos.getX();
        double startY = startPos.getY();
        
        double currentX = startX;
        double currentY = startY;
        
        double minX = startX;
        double maxX = startX;
        double minY = startY;
        double maxY = startY;
        
        double maxHeightInLine = 0;
        double lastSize = textBlock.getBaseSize();
        
        for (TextRun run : textBlock.getRuns()) {
            Font font = Font.font(run.getSize());
            lastSize = run.getSize();
            String textStr = run.getText();
            
            for (int i = 0; i < textStr.length(); i++) {
                char c = textStr.charAt(i);
                String strChar = String.valueOf(c);
                
                if (strChar.equals(TextTokens.ENTER_TOKEN)) {
                    currentX = startX;
                    currentY += (maxHeightInLine > 0 ? maxHeightInLine : lastSize * 1.2);
                    maxHeightInLine = 0;
                } else {
                    String renderStr = strChar;
                    if (strChar.equals(TextTokens.TAB_TOKEN)) {
                        renderStr = TextTokens.TAB_REPLACEMENT;
                    }
                    
                    Text textNode = new Text(renderStr);
                    textNode.setFont(font);
                    double width = textNode.getLayoutBounds().getWidth();
                    double height = textNode.getLayoutBounds().getHeight();
                    maxHeightInLine = Math.max(maxHeightInLine, height);
                    
                    maxX = Math.max(maxX, currentX + width);
                    maxY = Math.max(maxY, currentY + height);
                    
                    currentX += width;
                }
            }
        }
        
        // Fallback if empty
        if (minX == maxX && minY == maxY) {
            double cursorHeight = lastSize * 1.2;
            return new BoundingBox(startX, startY, 2.0, cursorHeight);
        }
        
        return new BoundingBox(minX, minY, maxX - minX, maxY - minY);
    }

    public boolean intersects(Point2D center, double radius) {
        Bounds bounds = getBounds();
        double closestX = Math.max(bounds.getMinX(), Math.min(center.getX(), bounds.getMaxX()));
        double closestY = Math.max(bounds.getMinY(), Math.min(center.getY(), bounds.getMaxY()));
        double dist = Math.hypot(center.getX() - closestX, center.getY() - closestY);
        return dist <= radius;
    }
}
