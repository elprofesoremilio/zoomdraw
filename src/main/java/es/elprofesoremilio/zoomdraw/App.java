package es.elprofesoremilio.zoomdraw;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Aplicación JavaFX principal de ZoomDraw.
 *
 * Ciclo de vida:
 *  1. start() → arranque silencioso (sin ventanas visibles).
 *  2. JNativeHook escucha globalmente CTRL+1.
 *  3. CTRL+1 → activateAnnotationMode() → abre AnnotationWindow.
 *  4. ESC    → deactivateAnnotationMode() → cierra la ventana, vuelve a LISTENING.
 *  5. stop() → desregistra el hook nativo al cerrar la app.
 */
public class App extends Application {

    // volatile garantiza visibilidad entre el hilo de JNativeHook y el de JavaFX.
    private volatile AppState currentState = AppState.LISTENING;

    private AnnotationWindow annotationWindow;

    // -------------------------------------------------------------------------
    // JavaFX lifecycle
    // -------------------------------------------------------------------------

    @Override
    public void start(Stage primaryStage) {
        /*
         * Modo silencioso: la Stage primaria que JavaFX abre automáticamente
         * se oculta de inmediato. No se muestra ninguna ventana.
         *
         * setImplicitExit(false) → el proceso NO termina cuando no hay ventanas
         * visibles, lo que permite permanecer en modo escucha indefinidamente.
         */
        primaryStage.hide();
        Platform.setImplicitExit(false);

        initGlobalKeyHook();

        System.out.println("[ZoomDraw] Iniciado en modo escucha. Pulsa CTRL+1 para anotar.");
    }

    @Override
    public void stop() {
        unregisterHook();
    }

    // -------------------------------------------------------------------------
    // JNativeHook: registro / baja del hook global de teclado
    // -------------------------------------------------------------------------

    private void initGlobalKeyHook() {
        // JNativeHook es muy verboso por defecto; silenciamos sus logs.
        suppressNativeHookLogging();

        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            System.err.println("[ZoomDraw] ERROR: No se pudo registrar el hook de teclado: "
                    + e.getMessage());
            System.err.println("           En Linux asegúrate de tener acceso a /dev/input "
                    + "o ejecutar con el servidor X11 activo (no Wayland puro).");
            Platform.exit();
            return;
        }

        GlobalScreen.addNativeKeyListener(new GlobalKeyListener(this));
        System.out.println("[ZoomDraw] Hook global de teclado registrado.");
    }

    private void unregisterHook() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            System.err.println("[ZoomDraw] Aviso: no se pudo desregistrar el hook: "
                    + e.getMessage());
        }
    }

    private static void suppressNativeHookLogging() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }

    // -------------------------------------------------------------------------
    // Gestión de estados
    // -------------------------------------------------------------------------

    /**
     * LISTENING → ANNOTATION_ACTIVE.
     * Llamado desde el hilo de JNativeHook; usa Platform.runLater para
     * interactuar con JavaFX de forma segura.
     */
    public void activateAnnotationMode() {
        if (currentState != AppState.LISTENING) return;

        currentState = AppState.ANNOTATION_ACTIVE;
        System.out.println("[ZoomDraw] Estado → ANNOTATION_ACTIVE");

        Platform.runLater(() -> {
            annotationWindow = new AnnotationWindow(this);
            annotationWindow.show();
        });
    }

    /**
     * ANNOTATION_ACTIVE → LISTENING.
     * Llamado desde el hilo de JavaFX (manejador de tecla ESC en la ventana).
     */
    public void deactivateAnnotationMode() {
        if (currentState != AppState.ANNOTATION_ACTIVE) return;

        currentState = AppState.LISTENING;
        System.out.println("[ZoomDraw] Estado → LISTENING");

        Platform.runLater(() -> {
            if (annotationWindow != null) {
                annotationWindow.close();
                annotationWindow = null;
            }
        });
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public AppState getCurrentState() {
        return currentState;
    }

    // -------------------------------------------------------------------------
    // Main
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        launch(args);
    }
}
