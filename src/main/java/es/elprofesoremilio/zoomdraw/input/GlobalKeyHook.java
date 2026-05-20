package es.elprofesoremilio.zoomdraw.input;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import es.elprofesoremilio.zoomdraw.commands.Command;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.utils.AppLogger;

import java.lang.reflect.Field;

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
            AppLogger.logError("Failed to register Global Hook: " + e.getMessage());
        }
    }

    public void unregister() {
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            AppLogger.logError("Error unregistering Global Hook", e);
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        boolean ctrlDown = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
        int keyCode = e.getKeyCode();

        // CTRL + 1
        if (ctrlDown && keyCode == NativeKeyEvent.VC_1) {
            consumeEvent(e);
            toggleAnnotationModeCommand.execute();
        }

        // CTRL + 0
        if (ctrlDown && keyCode == NativeKeyEvent.VC_0) {
            consumeEvent(e);
            manager.toggleHelpWindow();
        }

        // ESC (solo si la app está activa y no estamos en modo texto)
        // Para no interferir con la cancelación de formas o modo texto en AnnotationStage,
        // GlobalKeyHook procesará el ESC solo si isTextModeActive es false
        if (keyCode == NativeKeyEvent.VC_ESCAPE && manager.isActive() && !manager.isTextModeActive()) {
            consumeEvent(e);
            stopAnnotationModeCommand.execute();
        }
    }

    /**
     * Intenta consumir el evento en JNativeHook para evitar la propagación al OS.
     * 
     * TODO: Encontrar una forma limpia de evitar la propagación en Linux
     * sin usar reflexión sobre campos privados o hacks obsoletos (sun.misc.Unsafe),
     * ya que esto dispara restricciones del sistema de módulos de Java (JPMS)
     * y advertencias de deprecación en JDKs modernos.
     */
    private void consumeEvent(NativeKeyEvent e) {
        // Obviado por el momento debido a restricciones de seguridad de JPMS en Java 9+
        // y advertencias de deprecación de Unsafe.
    }
}