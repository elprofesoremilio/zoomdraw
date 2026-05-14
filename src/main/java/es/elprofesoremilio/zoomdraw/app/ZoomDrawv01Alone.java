package es.elprofesoremilio.zoomdraw.app;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ZoomDrawApp v0.1 — prototipo monolítico para validar el ciclo de vida
 * del modo anotación según la especificación.
 *
 * ── Problema de foco en Windows 11 ─────────────────────────────────────────
 * Windows 10/11 impide por diseño que una aplicación en segundo plano robe
 * el foco a la ventana activa (FOREGROUND_LOCK). La llamada a stage.requestFocus()
 * desde JavaFX no es suficiente porque opera a nivel JVM, no a nivel Win32.
 *
 * Solución aplicada (sin JNA, solo JavaFX puro):
 *   1. Crear un Stage auxiliar TRANSPARENTE 1×1 px justo antes de mostrar
 *      la ventana de anotación. Al mostrarlo el SO concede el "foreground token"
 *      a nuestro proceso.
 *   2. Mostrar inmediatamente la ventana de anotación y cerrar el Stage auxiliar.
 *   3. Llamar a toFront() + requestFocus() ya con el token concedido.
 *
 * Este truco es equivalente al que usan muchos frameworks Java (IntelliJ,
 * NetBeans) para forzar el foco en Windows sin invocar SetForegroundWindow
 * directamente via JNI/JNA.
 * ───────────────────────────────────────────────────────────────────────────
 */
public class ZoomDrawv01Alone extends Application {

    // -----------------------------------------------------------------------
    // Estado de la aplicación
    // -----------------------------------------------------------------------

    /** Referencia a la ventana de anotación activa, null cuando está inactiva. */
    private Stage annotationStage = null;

    /** true mientras la ventana de anotación está visible. */
    private volatile boolean annotationActive = false;

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        // Silenciar los logs verbosos de JNativeHook
        Logger jnhLogger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        jnhLogger.setLevel(Level.OFF);
        jnhLogger.setUseParentHandlers(false);
        
        launch(args);
    }

    // -----------------------------------------------------------------------
    // JavaFX Application lifecycle
    // -----------------------------------------------------------------------

    @Override
    public void start(Stage primaryStage) {
        // La ventana primaria de JavaFX no se usa: la ocultamos completamente.
        // Así la aplicación vive en segundo plano sin mostrar nada al usuario.
        primaryStage.hide();

        // Impedir que la aplicación se cierre al cerrar la ventana de anotación.
        // La salida solo ocurre cuando el usuario cierra desde el menú de bandeja
        // (futuro) o cierra el proceso.
        Platform.setImplicitExit(false);

        // Registrar el hook global de teclado
        registrarHookGlobal();
    }

    @Override
    public void stop() {
        // Limpiar JNativeHook al cerrar la aplicación
        try {
            if (GlobalScreen.isNativeHookRegistered()) {
                GlobalScreen.unregisterNativeHook();
            }
        } catch (NativeHookException e) {
            System.err.println("Error al desregistrar JNativeHook: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // JNativeHook — captura global de teclado
    // -----------------------------------------------------------------------

    private void registrarHookGlobal() {
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            System.err.println("No se pudo registrar JNativeHook: " + e.getMessage());
            System.err.println("En Linux, asegúrate de tener acceso a /dev/input o XWayland.");
            return;
        }

        GlobalScreen.addNativeKeyListener(new NativeKeyListener() {

            @Override
            public void nativeKeyPressed(NativeKeyEvent e) {
                boolean ctrlDown = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
                int keyCode = e.getKeyCode();

                // ── CTRL+1: activar o desactivar modo anotación ──────────────
                // Funciona tanto con como sin foco (hook global).
                if (ctrlDown && keyCode == NativeKeyEvent.VC_1) {
                    Platform.runLater(ZoomDrawv01Alone.this::toggleModoAnotacion);
                    return;
                }

                // ── ESC: desactivar SOLO si la ventana tiene el foco ─────────
                // Regla crítica del spec: ESC no se consume si otra app tiene el foco.
                if (keyCode == NativeKeyEvent.VC_ESCAPE && annotationActive) {
                    Platform.runLater(() -> {
                        if (annotationStage != null && annotationStage.isFocused()) {
                            desactivarModoAnotacion();
                        }
                        // Si no tiene foco, ESC se propaga normalmente (no hacemos nada).
                    });
                }
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent e) { /* no usado */ }

            @Override
            public void nativeKeyTyped(NativeKeyEvent e) { /* no usado */ }
        });
    }

    // -----------------------------------------------------------------------
    // Lógica de activación / desactivación
    // -----------------------------------------------------------------------

    /**
     * Alterna el modo anotación: si está inactivo lo activa, si está activo lo desactiva.
     * Debe llamarse siempre desde el hilo JavaFX (Platform.runLater).
     */
    private void toggleModoAnotacion() {
        if (annotationActive) {
            desactivarModoAnotacion();
        } else {
            activarModoAnotacion();
        }
    }

    /**
     * Activa el modo anotación.
     *
     * El orden importa para conseguir el foco en Windows 11:
     *   1. Obtener bounds del monitor activo.
     *   2. Forzar foreground token con el Stage auxiliar (ver crearStageFocusHelper).
     *   3. Crear y mostrar la ventana de anotación ya con el token concedido.
     *   4. Cerrar el Stage auxiliar.
     */
    private void activarModoAnotacion() {
        Rectangle2D boundsMonitor = obtenerBoundsMonitorCursor();

        // 1. Crear el stage
        annotationStage = crearVentanaAnotacion(boundsMonitor);

        // 2. Configuración previa indispensable
        annotationStage.setAlwaysOnTop(true);

        // 3. Mostrar la ventana
        annotationStage.show();

        // 4. El "Martillo de Foco":
        // No basta con Platform.runLater, necesitamos un pequeñísimo delay
        // para que el SO registre que la ventana ya existe físicamente.
        new Thread(() -> {
            try {
                Thread.sleep(50); // 50ms es imperceptible pero vital para el SO
                Platform.runLater(() -> {
                    if (annotationStage != null) {
                        annotationStage.setIconified(true);
                        annotationStage.setIconified(false); // Forzar restauración
                        annotationStage.toFront();
                        annotationStage.requestFocus();
                    }
                });
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }).start();

        annotationActive = true;
    }

    /**
     * Desactiva el modo anotación: destruye la ventana y libera recursos.
     * Debe llamarse siempre desde el hilo JavaFX.
     */
    private void desactivarModoAnotacion() {
        if (annotationStage != null) {
            annotationStage.close();
            annotationStage = null;
        }
        annotationActive = false;
    }

    // -----------------------------------------------------------------------
    // Truco de foco para Windows 10/11
    // -----------------------------------------------------------------------

    /**
     * Crea un Stage auxiliar mínimo, transparente y sin decoración.
     *
     * Windows 11 aplica "FOREGROUND_LOCK": solo el proceso que actualmente tiene
     * el foco puede concedérselo a otra ventana. Si nuestra app está en segundo
     * plano, llamar directamente a requestFocus() en la ventana de anotación
     * no tiene efecto; la barra de tareas parpadea pero el foco no se mueve.
     *
     * El truco: mostrar brevemente un Stage en primer plano hace que el SO
     * transfiera el "foreground token" a nuestro proceso. Inmediatamente después
     * mostramos la ventana real y cerramos este helper, y el token ya está en
     * nuestro proceso para que requestFocus() funcione.
     *
     * El Stage se crea 1×1 píxel, transparente y con opacidad 0, por lo que
     * el usuario no lo percibe (aparece y desaparece en el mismo frame de render).
     */
    private Stage crearStageFocusHelper(Rectangle2D bounds) {
        Stage helper = new Stage(StageStyle.TRANSPARENT);
        helper.setAlwaysOnTop(true);
        helper.setWidth(1);
        helper.setHeight(1);
        helper.setOpacity(0);   // completamente invisible

        Scene scene = new Scene(new StackPane(), 1, 1, Color.TRANSPARENT);
        helper.setScene(scene);

        // Esquina superior izquierda del monitor activo
        helper.setX(bounds.getMinX());
        helper.setY(bounds.getMinY());

        return helper;
    }

    // -----------------------------------------------------------------------
    // Construcción de la ventana de anotación
    // -----------------------------------------------------------------------

    /**
     * Crea y configura el Stage de anotación con las propiedades del spec v0.1:
     *  - Sin decoración de ventana.
     *  - Always-on-top.
     *  - Posicionado manualmente sobre el monitor activo (sin setFullScreen()
     *    para que cubra también la barra de tareas / paneles del sistema).
     *  - Fondo negro con texto de instrucción.
     *  - Consume ESC mediante el listener JavaFX (cuando tiene foco).
     */
    private Stage crearVentanaAnotacion(Rectangle2D bounds) {
        Stage stage = new Stage(StageStyle.UNDECORATED);
    stage.initStyle(StageStyle.TRANSPARENT);
        // ── Canvas de anotación ──────────────────────────────────────────────
        Canvas canvas = new Canvas(bounds.getWidth(), bounds.getHeight());
        dibujarFondoInicial(canvas, bounds.getWidth(), bounds.getHeight());

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: black;");

        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight());
        scene.setFill(Color.BLACK);

        // ── Atajos JavaFX locales (solo cuando la ventana tiene foco) ────────
        // El hook global gestiona CTRL+1 y ESC sin foco; este listener es el
        // refuerzo para cuando SÍ tenemos el foco (más fiable para ESC).
        scene.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode() == KeyCode.ESCAPE) {
                desactivarModoAnotacion();
                keyEvent.consume(); // evitar propagación a otras aplicaciones
            }
        });

        // ── Recuperar foco al hacer clic sobre la ventana ────────────────────
        // Cuando está en estado "Activo sin foco" (ALT+TAB, clic fuera),
        // el siguiente clic sobre ella debe devolverle el foco (spec §3.1.2).
        scene.setOnMouseClicked(mouseEvent -> {
            if (!stage.isFocused()) {
                stage.toFront();
                stage.requestFocus();
            }
        });

        // ── Configuración del Stage ──────────────────────────────────────────
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
        stage.setFullScreenExitHint("");

        // Posicionado manual sobre el monitor activo.
        // NO usamos setFullScreen(true) para cubrir barra de tareas / docks.
        stage.setX(bounds.getMinX());
        stage.setY(bounds.getMinY());
        stage.setWidth(bounds.getWidth());
        stage.setHeight(bounds.getHeight());

        stage.setResizable(false);

        root.setFocusTraversable(true);

        return stage;
    }

    /**
     * Dibuja el contenido inicial del canvas: fondo negro + texto de instrucción.
     * En la versión real se sustituirá por la captura de pantalla como textura base.
     */
    private void dibujarFondoInicial(Canvas canvas, double w, double h) {
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Fondo negro
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, w, h);

        // Texto central principal
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 28));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(javafx.geometry.VPos.CENTER);
        gc.fillText("Pulse ESC para salir", w / 2, h / 2);

        // Subtexto informativo
        gc.setFill(Color.LIGHTGRAY);
        gc.setFont(Font.font("System", FontWeight.NORMAL, 16));
        gc.fillText("(o CTRL+1 para activar/desactivar)", w / 2, h / 2 + 44);
    }

    // -----------------------------------------------------------------------
    // Utilidades de pantalla
    // -----------------------------------------------------------------------

    /**
     * Determina los bounds COMPLETOS (incluyendo barra de tareas) del monitor
     * donde se encuentra el cursor del ratón en el momento de llamar a este método.
     *
     * getBounds() devuelve los bounds totales del monitor; a diferencia de
     * getVisualBounds() que excluye la barra de tareas / dock del sistema.
     */
    private Rectangle2D obtenerBoundsMonitorCursor() {
        Point cursorAWT = MouseInfo.getPointerInfo().getLocation();
        double cx = cursorAWT.getX();
        double cy = cursorAWT.getY();

        for (Screen screen : Screen.getScreens()) {
            Rectangle2D fullBounds = screen.getBounds();
            if (fullBounds.contains(cx, cy)) {
                return fullBounds;
            }
        }

        // Fallback: pantalla primaria
        return Screen.getPrimary().getBounds();
    }
}