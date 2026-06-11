package es.elprofesoremilio.zoomdraw;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import javafx.application.Application;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Entry point for the ZoomDraw application.
 */
public class Main {

    // Mantenemos referencias estáticas para que la JVM NO libere el lock
    // mientras la aplicación siga en ejecución.
    private static RandomAccessFile lockRaf;
    private static FileChannel lockChannel;
    private static FileLock lock;

    public static void main(String[] args) {
        if (!acquireSingleInstanceLock()) {
            showAlreadyRunningDialog();
            System.exit(0);
        }

        // Liberar el lock limpiamente al salir
        Runtime.getRuntime().addShutdownHook(new Thread(Main::releaseSingleInstanceLock, "lock-release"));

        // Forzar a JNativeHook a extraer sus librerías nativas en el directorio temporal del usuario
        System.setProperty("jnativehook.lib.path", System.getProperty("java.io.tmpdir"));

        AppConfig.loadSystemProperties();
        java.awt.Toolkit.getDefaultToolkit();
        Application.launch(AppLauncher.class, args);
    }

    /**
     * Intenta adquirir el lock de instancia única.
     * Devuelve {@code true} si somos la primera instancia, {@code false} si ya hay otra en marcha.
     */
    private static boolean acquireSingleInstanceLock() {
        try {
            File lockFile = new File(System.getProperty("java.io.tmpdir"), "zoomdraw.lock");
            lockRaf = new RandomAccessFile(lockFile, "rw");
            lockChannel = lockRaf.getChannel();
            lock = lockChannel.tryLock();
            return lock != null;
        } catch (Exception e) {
            es.elprofesoremilio.zoomdraw.utils.AppLogger.logError("Error obteniendo lock de instancia única", e);
            // En caso de error, permitimos el arranque para no bloquear al usuario
            return true;
        }
    }

    /** Libera el lock y cierra los recursos asociados. */
    private static void releaseSingleInstanceLock() {
        try {
            if (lock != null)    { lock.release();    lock = null; }
            if (lockChannel != null) { lockChannel.close(); lockChannel = null; }
            if (lockRaf != null) { lockRaf.close();   lockRaf = null; }
        } catch (Exception ignored) { }
    }

    /** Muestra un diálogo AWT visible (JavaFX aún no está iniciado en este punto). */
    private static void showAlreadyRunningDialog() {
        es.elprofesoremilio.zoomdraw.utils.AppLogger.log("ZoomDraw ya está en ejecución. Saliendo...");
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                javax.swing.JOptionPane.showMessageDialog(
                        null,
                        "ZoomDraw ya está en ejecución.\nRevisa el área de notificaciones del sistema.",
                        "ZoomDraw",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                );
            });
        } catch (Exception ignored) { }
    }
}