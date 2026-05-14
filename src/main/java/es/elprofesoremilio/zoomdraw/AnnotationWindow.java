package es.elprofesoremilio.zoomdraw;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Ventana de anotación de ZoomDraw.
 *
 * Características (Sprint 1):
 *  - Pantalla completa, sin decoración, siempre encima.
 *  - Fondo negro temporal (sustituido por captura real en Sprint 2).
 *  - Bloquea toda interacción con el sistema operativo mientras está visible:
 *      * roba el foco en cuanto aparece,
 *      * es la única ventana que puede recibir teclado/ratón.
 *  - ESC cierra la ventana y vuelve al modo LISTENING.
 *      El evento se consume dentro de JavaFX; al tener el foco exclusivo
 *      el SO no lo enruta a otras aplicaciones.
 *
 * Sprint 2 añadirá: captura real de pantalla como fondo.
 * Sprint 3+ añadirá: trazado libre, formas, texto, etc.
 */
public class AnnotationWindow {

    private final Stage stage;
    private final App   app;

    public AnnotationWindow(App app) {
        this.app   = app;
        this.stage = new Stage();
        buildStage();
    }

    // -------------------------------------------------------------------------
    // Construcción de la ventana
    // -------------------------------------------------------------------------

    private void buildStage() {
        // --- Contenedor raíz ---
        // Fondo negro como placeholder hasta que Sprint 2 ponga la captura.
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: black;");

        // --- Escena ---
        double w = Screen.getPrimary().getBounds().getWidth();
        double h = Screen.getPrimary().getBounds().getHeight();
        Scene scene = new Scene(root, w, h, Color.BLACK);

        // --- Teclas ---
        configureKeyHandlers(scene);

        // --- Stage ---
        stage.initStyle(StageStyle.UNDECORATED);   // sin barra de título ni bordes
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);                // siempre encima de todo
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");            // oculta el mensaje "Pulsa ESC…"

        /*
         * CRÍTICO: deshabilitar la salida de pantalla completa por defecto de JavaFX.
         * Sin esto, JavaFX capturaría ESC por su cuenta (para salir de fullscreen)
         * antes de que llegue a nuestro handler, con comportamiento impredecible.
         * Nosotros gestionamos ESC manualmente en configureKeyHandlers().
         */
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
    }

    private void configureKeyHandlers(Scene scene) {
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                /*
                 * e.consume() impide que el evento siga propagándose por el
                 * árbol de nodos de JavaFX. Dado que la ventana tiene foco
                 * exclusivo del SO, el ESC no llega a otras aplicaciones.
                 */
                e.consume();
                System.out.println("[ZoomDraw] ESC pulsado → desactivando modo anotación.");
                app.deactivateAnnotationMode();
            }
            // Sprint 3+: aquí se añadirán el resto de atajos (r, o, g, CTRL+Z…)
        });
    }

    // -------------------------------------------------------------------------
    // API pública
    // -------------------------------------------------------------------------

    /** Muestra la ventana y roba el foco inmediatamente. */
    public void show() {
        stage.show();
        stage.requestFocus();   // fuerza el foco aunque otra ventana lo tenga
        stage.toFront();        // sube al frente por si alwaysOnTop no bastara
    }

    /** Oculta (no destruye) la ventana. */
    public void close() {
        stage.hide();
    }
}
