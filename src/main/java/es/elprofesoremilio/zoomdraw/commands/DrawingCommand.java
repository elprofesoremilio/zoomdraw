package es.elprofesoremilio.zoomdraw.commands;

import javafx.scene.canvas.GraphicsContext;

public interface DrawingCommand extends Command {
    void execute(GraphicsContext gc);
    
    @Override
    default void execute() {
        // Not used directly without GraphicsContext
    }
}
