package es.elprofesoremilio.zoomdraw.input;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseMotionListener;
import es.elprofesoremilio.zoomdraw.commands.Command;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.utils.AppLogger;

public class GlobalKeyHook implements NativeKeyListener, NativeMouseMotionListener {

    private final AnnotationManager manager;
    private final Command toggleAnnotationModeCommand;
    private final Command stopAnnotationModeCommand;
    private final Command toggleRouletteModeCommand;

    public GlobalKeyHook(AnnotationManager manager,
                         Command toggleAnnotationModeCommand,
                         Command stopAnnotationModeCommand,
                         Command toggleRouletteModeCommand) {
        this.manager = manager;
        this.toggleAnnotationModeCommand = toggleAnnotationModeCommand;
        this.stopAnnotationModeCommand = stopAnnotationModeCommand;
        this.toggleRouletteModeCommand = toggleRouletteModeCommand;
    }

    public void register() {
        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.addNativeMouseMotionListener(this);
        } catch (NativeHookException e) {
            AppLogger.logError("Failed to register Global Hook: " + e.getMessage());
        }
    }

    public void unregister() {
        try {
            GlobalScreen.removeNativeMouseMotionListener(this);
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            AppLogger.logError("Error unregistering Global Hook", e);
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        boolean ctrlDown = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
        int keyCode = e.getKeyCode();

        // Modo anotación
        if (AppConfig.hotkeyAnnotation != null && AppConfig.hotkeyAnnotation.matches(e)) {
            consumeEvent(e);
            toggleAnnotationModeCommand.execute();
            return;
        }

        // Modo Láser - solo si no está en modo anotación
        if (AppConfig.hotkeyLaser != null && AppConfig.hotkeyLaser.matches(e)) {
            consumeEvent(e);
            if (!manager.isActive()) {
                manager.toggleLaserMode();
            }
            return;
        }

        // Modo Ruleta - solo si no está en modo anotación ni láser
        if (AppConfig.hotkeyRoulette != null && AppConfig.hotkeyRoulette.matches(e)) {
            consumeEvent(e);
            if (!manager.isActive() && !manager.isLaserActive()) {
                toggleRouletteModeCommand.execute();
            }
            return;
        }

        // CTRL + 0
        if (ctrlDown && keyCode == AppConfig.HELP_WINDOW_KEY) {
            consumeEvent(e);
            manager.toggleHelpWindow();
        }

        // ESC cierra el modo ruleta
        if (keyCode == NativeKeyEvent.VC_ESCAPE && manager.isRouletteActive()) {
            consumeEvent(e);
            manager.stopRouletteMode();
        }

        // ESC
        // Si el láser está activo, ESC lo apaga
        if (keyCode == NativeKeyEvent.VC_ESCAPE && manager.isLaserActive()) {
            consumeEvent(e);
            manager.stopLaserMode();
        }
        // Solo cerrar el modo anotación si:
        //  - Ningún sub-modo (texto / numeración / recorte) está activo, Y
        //  - No se acaba de cancelar un sub-modo (ventana de 300 ms para cubrir la
        //    condición de carrera entre el hilo JNativeHook y el hilo JavaFX en Linux).
        else if (keyCode == NativeKeyEvent.VC_ESCAPE && manager.isActive()
                && !manager.isTextModeActive()
                && !manager.isNumberingModeActive()
                && !manager.isCropModeActive()
                && !manager.wasSubModeRecentlyCancelled()) {
            consumeEvent(e);
            stopAnnotationModeCommand.execute();
        }
    }

    @Override
    public void nativeMouseMoved(NativeMouseEvent e) {
        manager.updateLaserPosition(e.getX(), e.getY());
    }

    @Override
    public void nativeMouseDragged(NativeMouseEvent e) {
        manager.updateLaserPosition(e.getX(), e.getY());
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