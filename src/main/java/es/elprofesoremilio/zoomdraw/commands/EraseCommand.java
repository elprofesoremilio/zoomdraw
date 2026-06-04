package es.elprofesoremilio.zoomdraw.commands;

import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class EraseCommand implements DrawingCommand {
    private final List<DrawingCommand> originalAffected;
    private final List<DrawingCommand> replacementCommands;
    private final List<DrawingCommand> beforeState;
    private final List<DrawingCommand> afterState;

    public EraseCommand(List<DrawingCommand> originalAffected,
                        List<DrawingCommand> replacementCommands,
                        List<DrawingCommand> beforeState,
                        List<DrawingCommand> afterState) {
        this.originalAffected = new ArrayList<>(originalAffected);
        this.replacementCommands = new ArrayList<>(replacementCommands);
        this.beforeState = new ArrayList<>(beforeState);
        this.afterState = new ArrayList<>(afterState);
    }

    @Override
    public void execute(GraphicsContext gc) {
        // No visual operation on execute as the replacement commands are already in the stack
    }

    public void undo(Stack<DrawingCommand> undoStack) {
        undoStack.clear();
        undoStack.addAll(beforeState);
    }

    public void redo(Stack<DrawingCommand> undoStack) {
        undoStack.clear();
        undoStack.addAll(afterState);
    }

    public List<DrawingCommand> getOriginalAffected() {
        return originalAffected;
    }

    public List<DrawingCommand> getReplacementCommands() {
        return replacementCommands;
    }
}
