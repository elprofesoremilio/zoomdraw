package es.elprofesoremilio.zoomdraw.input;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.application.Platform;

public class GlobalKeyHook implements NativeKeyListener {

    private final AnnotationManager manager;

    public GlobalKeyHook(AnnotationManager manager) {
        this.manager = manager;
    }

    public void register() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException e) {
            System.err.println("Failed to register Global Hook: " + e.getMessage());
        }
    }

    public void unregister() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        // LOG TEMPORAL: Si ves esto en consola al pulsar teclas, el Hook funciona.
//        System.out.println("Key Pressed: " + NativeKeyEvent.getKeyText(e.getKeyCode()));

        boolean ctrlDown = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
        int keyCode = e.getKeyCode();

        // CTRL + 1
        if (ctrlDown && keyCode == NativeKeyEvent.VC_1) {
            Platform.runLater(manager::toggleAnnotationMode);
        }

        // ESC (solo si la app está activa pero no tiene el foco JavaFX)
        if (keyCode == NativeKeyEvent.VC_ESCAPE && manager.isActive()) {
            Platform.runLater(() -> {
                if (manager.isStageFocused()) {
                    manager.stopAnnotationMode();
                }
            });
        }
    }
}