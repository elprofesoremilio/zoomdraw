package es.elprofesoremilio.zoomdraw.commands;

import javafx.scene.canvas.GraphicsContext;
import java.util.Stack;

public class CommandHistory {
    private final Stack<DrawingCommand> undoStack = new Stack<>();
    private final Stack<DrawingCommand> redoStack = new Stack<>();

    public void execute(DrawingCommand command, GraphicsContext gc) {
        command.execute(gc);
        undoStack.push(command);
        redoStack.clear();
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo(Runnable redrawCallback) {
        if (canUndo()) {
            redoStack.push(undoStack.pop());
            redrawCallback.run();
        }
    }

    public void redo(Runnable redrawCallback) {
        if (canRedo()) {
            undoStack.push(redoStack.pop());
            redrawCallback.run();
        }
    }

    public Iterable<DrawingCommand> getHistory() {
        return undoStack;
    }
}
