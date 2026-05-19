package es.elprofesoremilio.zoomdraw.commands;

import javafx.scene.canvas.GraphicsContext;

public class ClearCommand implements DrawingCommand {

    private final Runnable backgroundDrawer;

    public ClearCommand(Runnable backgroundDrawer) {
        this.backgroundDrawer = backgroundDrawer;
    }

    @Override
    public void execute(GraphicsContext gc) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
        if (backgroundDrawer != null) {
            backgroundDrawer.run();
        }
    }
}
