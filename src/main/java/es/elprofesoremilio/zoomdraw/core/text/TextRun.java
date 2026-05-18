package es.elprofesoremilio.zoomdraw.core.text;

import javafx.scene.paint.Color;

public class TextRun {
    private String text;
    private Color color;
    private double size;

    public TextRun(String text, Color color, double size) {
        this.text = text;
        this.color = color;
        this.size = size;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void append(String str) {
        this.text += str;
    }

    public void removeLast() {
        if (!this.text.isEmpty()) {
            this.text = this.text.substring(0, this.text.length() - 1);
        }
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public double getSize() {
        return size;
    }

    public void setSize(double size) {
        this.size = size;
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }
}
