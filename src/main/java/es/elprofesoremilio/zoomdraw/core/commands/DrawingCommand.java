package es.elprofesoremilio.zoomdraw.core.commands;

import javafx.scene.canvas.GraphicsContext;

public interface DrawingCommand {
    void execute(GraphicsContext gc);
}
