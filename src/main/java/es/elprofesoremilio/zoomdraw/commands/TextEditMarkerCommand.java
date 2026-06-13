package es.elprofesoremilio.zoomdraw.commands;

import javafx.scene.canvas.GraphicsContext;

/**
 * Marker command pushed to the undo stack when an existing TextCommand is edited inline.
 * Does not draw anything itself; the actual TextCommand remains at its original stack
 * position and its content is mutated in-place by undoEdit()/redoEdit().
 */
public class TextEditMarkerCommand implements DrawingCommand {

    private final TextCommand target;
    private final String oldContent;
    private final String newContent;

    public TextEditMarkerCommand(TextCommand target, String oldContent, String newContent) {
        this.target = target;
        this.oldContent = oldContent;
        this.newContent = newContent;
    }

    @Override
    public void execute(GraphicsContext gc) {
        // Marker only – rendering is done by target at its own stack position.
    }

    public void undoEdit() {
        target.setFlatContent(oldContent);
    }

    public void redoEdit() {
        target.setFlatContent(newContent);
    }
}
