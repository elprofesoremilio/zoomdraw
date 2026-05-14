package es.elprofesoremilio.zoomdraw;

//public class Main {
//    public static void main(String[] args) {
//        // Aquí puedes realizar comprobaciones previas
//        // (ej. ¿ya hay otra instancia ejecutándose?)
//
//        // Lanzamos la aplicación JavaFX
//        ZoomDrawApp.main(args);
//    }
//}

import es.elprofesoremilio.zoomdraw.app.AppLauncher;
import javafx.application.Application;

/**
 * Entry point for the ZoomDraw application.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(AppLauncher.class, args);
    }
}