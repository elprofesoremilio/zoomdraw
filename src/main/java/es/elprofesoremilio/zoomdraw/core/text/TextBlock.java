package es.elprofesoremilio.zoomdraw.core.text;

import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class TextBlock {
    private final Point2D startPosition;
    private final List<TextRun> runs;
    private final Color baseColor;
    private final double baseSize;

    public TextBlock(Point2D startPosition, Color baseColor, double baseSize) {
        this.startPosition = startPosition;
        this.runs = new ArrayList<>();
        this.baseColor = baseColor;
        this.baseSize = baseSize;
    }

    public void addRun(TextRun run) {
        this.runs.add(run);
    }

    public List<TextRun> getRuns() {
        return runs;
    }

    public void appendCharacter(String character, Color color, double size) {
        if (runs.isEmpty()) {
            runs.add(new TextRun(character, color, size));
        } else {
            TextRun lastRun = runs.get(runs.size() - 1);
            if (lastRun.getColor().equals(color) && lastRun.getSize() == size) {
                lastRun.append(character);
            } else {
                runs.add(new TextRun(character, color, size));
            }
        }
    }

    public void removeLastCharacter() {
        if (!runs.isEmpty()) {
            TextRun lastRun = runs.get(runs.size() - 1);
            lastRun.removeLast();
            if (lastRun.isEmpty()) {
                runs.remove(runs.size() - 1);
            }
        }
    }

    public Point2D getStartPosition() {
        return startPosition;
    }

    public Color getBaseColor() {
        return baseColor;
    }

    public double getBaseSize() {
        return baseSize;
    }

    public boolean isEmpty() {
        return runs.isEmpty();
    }

    public void clear() {
        runs.clear();
    }
}
