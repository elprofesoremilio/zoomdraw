package es.elprofesoremilio.zoomdraw;

/**
 * Estados globales de la aplicación.
 *
 * LISTENING        → modo silencioso; sólo escucha CTRL+1 (via JNativeHook).
 * ANNOTATION_ACTIVE → ventana de anotación visible y con foco exclusivo.
 */
public enum AppState {
    LISTENING,
    ANNOTATION_ACTIVE
}
