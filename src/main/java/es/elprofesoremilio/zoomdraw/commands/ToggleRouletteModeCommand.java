package es.elprofesoremilio.zoomdraw.commands;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.application.Platform;

public class ToggleRouletteModeCommand implements Command {

    private final AnnotationManager manager;

    public ToggleRouletteModeCommand(AnnotationManager manager) {
        this.manager = manager;
    }

    @Override
    public void execute() {
        Platform.runLater(manager::toggleRouletteMode);
    }
}
