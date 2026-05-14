package es.elprofesoremilio.zoomdraw;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

/**
 * Escucha global de teclado registrada en JNativeHook.
 *
 * Funciona AUNQUE la aplicación no tenga el foco del sistema operativo.
 *
 * Sprint 1: detecta únicamente CTRL+1 en estado LISTENING.
 * Sprints posteriores ampliarán este listener o añadirán uno de ratón.
 *
 * IMPORTANTE: los callbacks de JNativeHook se ejecutan en su propio hilo
 * (distinto al hilo de JavaFX). Nunca toques la UI directamente aquí;
 * delega siempre en Platform.runLater() a través de App.
 */
public class GlobalKeyListener implements NativeKeyListener {

    private final App app;

    public GlobalKeyListener(App app) {
        this.app = app;
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // En modo ANNOTATION_ACTIVE la ventana JavaFX tiene el foco exclusivo
        // y gestiona sus propias teclas; aquí sólo actuamos en LISTENING.
        if (app.getCurrentState() != AppState.LISTENING) return;

        boolean ctrlDown = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
        boolean key1     = e.getKeyCode() == NativeKeyEvent.VC_1;

        if (ctrlDown && key1) {
            System.out.println("[ZoomDraw] CTRL+1 detectado → activando modo anotación.");
            app.activateAnnotationMode();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        // No utilizado en Sprint 1.
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent e) {
        // No utilizado en Sprint 1.
    }
}
