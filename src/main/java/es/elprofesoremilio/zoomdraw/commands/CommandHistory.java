package es.elprofesoremilio.zoomdraw.commands;

import javafx.scene.canvas.GraphicsContext;
import java.util.Stack;
import java.util.List;

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
            DrawingCommand cmd = undoStack.pop();
            if (cmd instanceof EraseCommand) {
                ((EraseCommand) cmd).undo(undoStack);
            }
            redoStack.push(cmd);
            redrawCallback.run();
        }
    }

    public void redo(Runnable redrawCallback) {
        if (canRedo()) {
            DrawingCommand cmd = redoStack.pop();
            if (cmd instanceof EraseCommand) {
                ((EraseCommand) cmd).redo(undoStack);
            }
            undoStack.push(cmd);
            redrawCallback.run();
        }
    }

    public void setUndoStack(List<DrawingCommand> commands) {
        this.undoStack.clear();
        this.undoStack.addAll(commands);
    }

    public Iterable<DrawingCommand> getHistory() {
        return undoStack;
    }
}
