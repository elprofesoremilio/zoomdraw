package es.elprofesoremilio.zoomdraw;

/**
 * Punto de entrada separado de Application para que el fat-jar funcione
 * correctamente sin necesitar JavaFX en el manifest del módulo.
 */
public class Launcher {
    public static void main(String[] args) {
        ZoomDrawApp.main(args);
    }
}
