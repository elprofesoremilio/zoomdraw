package es.elprofesoremilio.zoomdraw.commands;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.application.Platform;

/**
 * Command to toggle the annotation mode.
 */
public class ToggleAnnotationModeCommand implements Command {
    private final AnnotationManager annotationManager;

    public ToggleAnnotationModeCommand(AnnotationManager annotationManager) {
        this.annotationManager = annotationManager;
    }

    @Override
    public void execute() {
        Platform.runLater(annotationManager::toggleAnnotationMode);
    }
}
