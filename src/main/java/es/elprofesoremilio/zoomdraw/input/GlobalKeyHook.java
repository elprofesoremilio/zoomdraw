package es.elprofesoremilio.zoomdraw.input;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import es.elprofesoremilio.zoomdraw.commands.Command;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;

public class GlobalKeyHook implements NativeKeyListener {

    private final AnnotationManager manager; // Still needed for isActive() check
    private final Command toggleAnnotationModeCommand;
    private final Command stopAnnotationModeCommand;

    public GlobalKeyHook(AnnotationManager manager,
                         Command toggleAnnotationModeCommand,
                         Command stopAnnotationModeCommand) {
        this.manager = manager;
        this.toggleAnnotationModeCommand = toggleAnnotationModeCommand;
        this.stopAnnotationModeCommand = stopAnnotationModeCommand;
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
        boolean ctrlDown = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
        int keyCode = e.getKeyCode();

        // CTRL + 1
        if (ctrlDown && keyCode == NativeKeyEvent.VC_1) {
            toggleAnnotationModeCommand.execute();
        }

        // ESC (solo si la app está activa)
        if (keyCode == NativeKeyEvent.VC_ESCAPE && manager.isActive()) {
            stopAnnotationModeCommand.execute();
        }
    }
}