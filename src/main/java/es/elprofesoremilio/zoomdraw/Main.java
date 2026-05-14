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
// ESTO ES VITAL EN LINUX GNOME/WAYLAND:
        // Fuerza a Java a usar el pipeline de X11 para que Robot pueda capturar.
        System.setProperty("sun.java2d.uiScale", "1.0"); // Opcional: evita problemas de escalado
        System.setProperty("jdk.gtk.version", "3");

        // Si esto no funciona, prueba a descomentar la siguiente línea:
        System.setProperty("java.awt.headless", "false");
        // Si estás en Ubuntu 22 o 24, esta línea es la que suele "desatascar" al Robot
        // al evitar que intente usar aceleración por hardware que Wayland bloquea
        System.setProperty("sun.java2d.opengl", "false");
        java.awt.Toolkit.getDefaultToolkit();
        Application.launch(AppLauncher.class, args);
    }
}