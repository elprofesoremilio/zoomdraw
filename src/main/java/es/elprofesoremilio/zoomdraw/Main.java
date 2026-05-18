package es.elprofesoremilio.zoomdraw;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import javafx.application.Application;

/**
 * Entry point for the ZoomDraw application.
 */
public class Main {
    public static void main(String[] args) {
        AppConfig.loadSystemProperties();
        java.awt.Toolkit.getDefaultToolkit();
        Application.launch(AppLauncher.class, args);
    }
}