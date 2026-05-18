package es.elprofesoremilio.zoomdraw.commands;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.application.Platform;

/**
 * Command to stop the annotation mode, but only if the annotation stage is focused.
 */
public class StopAnnotationModeCommand implements Command {
    private final AnnotationManager annotationManager;

    public StopAnnotationModeCommand(AnnotationManager annotationManager) {
        this.annotationManager = annotationManager;
    }

    @Override
    public void execute() {
        Platform.runLater(() -> {
            if (annotationManager.isStageFocused()) {
                annotationManager.stopAnnotationMode();
            }
        });
    }
}
