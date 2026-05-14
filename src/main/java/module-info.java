/**
 * Módulo JPMS de ZoomDraw.
 *
 * Declarar el módulo elimina el warning:
 *   "Unsupported JavaFX configuration: classes were loaded from 'unnamed module'"
 *
 * JNativeHook 2.2.2 es un automatic module cuyo nombre viene definido en su
 * MANIFEST.MF (Automatic-Module-Name: com.github.kwhat.jnativehook).
 */
module com.zoomdraw {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;          // necesario para javafx-swing en el classpath
    requires com.github.kwhat.jnativehook;

    exports es.elprofesoremilio.zoomdraw;
}