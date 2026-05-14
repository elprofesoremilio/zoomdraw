package es.elprofesoremilio.zoomdraw.app;

import com.github.kwhat.jnativehook.GlobalScreen;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.input.GlobalKeyHook;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AppLauncher extends Application {

    private AnnotationManager annotationManager;
    private GlobalKeyHook globalKeyHook;

    @Override
    public void start(Stage primaryStage) {
        // Silenciar logs de JNativeHook
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        // Ocultar stage primario e impedir cierre implícito
        primaryStage.hide();
        Platform.setImplicitExit(false);

        // Inicializar el mediador central
        annotationManager = new AnnotationManager();

        // Registrar el hook global
        globalKeyHook = new GlobalKeyHook(annotationManager);
        globalKeyHook.register();
    }

    @Override
    public void stop() {
        globalKeyHook.unregister();
    }}