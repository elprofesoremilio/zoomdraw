package es.elprofesoremilio.zoomdraw;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import javafx.application.Application;

/**
 * Entry point for the ZoomDraw application.
 */
public class Main {
    public static void main(String[] args) {
        // Forzar a JNativeHook a extraer sus librerías nativas en el directorio temporal del usuario
        System.setProperty("jnativehook.lib.path", System.getProperty("java.io.tmpdir"));

        AppConfig.loadSystemProperties();
        java.awt.Toolkit.getDefaultToolkit();
        Application.launch(AppLauncher.class, args);
    }
}