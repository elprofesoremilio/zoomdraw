package es.elprofesoremilio.zoomdraw.commands;

import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.List;

public class NumberingSessionCommand implements DrawingCommand {
    private final List<NumberedCircle> circles;

    public NumberingSessionCommand(List<NumberedCircle> circles) {
        this.circles = new ArrayList<>();
        for (NumberedCircle c : circles) {
            this.circles.add(new NumberedCircle(c)); // Deep copy
        }
    }

    @Override
    public void execute(GraphicsContext gc) {
        for (NumberedCircle c : circles) {
            c.draw(gc);
        }
    }

    public List<NumberedCircle> getCircles() {
        return circles;
    }
}
