package es.elprofesoremilio.zoomdraw.wayland;
import javafx.application.Application;

/**
 * Entry point for the ZoomDraw application.
 */
public class MainWayland {
    public static void main(String[] args) {
        Application.launch(UbuntuWaylandIntegrator.class, args);
    }

    /*
    Funciona genial pero necesito algunos ajustes:
    - la escena se abre siempre en el mismo monitor, independientemente de
    donde esté el cursor, haz que detecte en qué monitor estamos y que se abra ahí.
    - una vez abierta la escena se cierra bien si pulso CTRL+1, pero para cerrarse con la tecla ESC
    tengo que estar con el foco en dicha escena. Eso está bien, es justo lo que queremos, el
    problema es que la escena no coge el foco al pulsar CTRL+1 cuando se activa el modo anotación. Haz que lo coja.
     */
}