package es.elprofesoremilio.zoomdraw.commands;

import javafx.scene.canvas.GraphicsContext;

/**
 * Limpia el canvas de anotaciones.
 * NO dibuja el fondo — eso es responsabilidad de AnnotationStage.redrawAll(),
 * que siempre redibuja el fondo de la sesión actual tras reproducir este comando.
 * Esto evita que el ClearCommand capture una referencia al stage antiguo
 * (que pertenece a una sesión anterior) cuando el CommandHistory se reutiliza
 * entre sesiones.
 */
public class ClearCommand implements DrawingCommand {

    @Override
    public void execute(GraphicsContext gc) {
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
    }
}
