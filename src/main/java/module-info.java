/**
 * Módulo JPMS de ZoomDraw.
 *
 * Declarar el módulo elimina el warning:
 *   "Unsupported JavaFX configuration: classes were loaded from 'unnamed module'"
 *
 * JNativeHook 2.2.2 es un automatic module cuyo nombre viene definido en su
 * MANIFEST.MF (Automatic-Module-Name: com.github.kwhat.jnativehook).
 */
/**
 * Módulo JPMS de ZoomDraw.
 *
 * Declarar el módulo elimina el warning:
 * "Unsupported JavaFX configuration: classes were loaded from 'unnamed module'"
 */
module es.elprofesoremilio.zoomdraw { // Mejor usar el nombre del paquete base
    // Módulos de JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    // Módulo de AWT/Swing (necesario para Robot, SystemTray e interactuar con javafx.swing)
    requires java.desktop;
    // Librerías de terceros (Automatic modules)
    requires com.github.kwhat.jnativehook;

    // Exportar paquetes principales
    exports es.elprofesoremilio.zoomdraw;
    exports es.elprofesoremilio.zoomdraw.core;
    exports es.elprofesoremilio.zoomdraw.ui;
    exports es.elprofesoremilio.zoomdraw.app;
}