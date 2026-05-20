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
    private static FileChannel fileChannel;
    private static FileLock lock;

    public static void main(String[] args) {
        try {
            File lockFile = new File(System.getProperty("java.io.tmpdir"), "zoomdraw.lock");
            fileChannel = new RandomAccessFile(lockFile, "rw").getChannel();
            lock = fileChannel.tryLock();
            if (lock == null) {
                es.elprofesoremilio.zoomdraw.utils.AppLogger.log("La aplicación ya está en ejecución. Saliendo...");
                System.exit(0);
            }
        } catch (Exception e) {
            es.elprofesoremilio.zoomdraw.utils.AppLogger.logError("Error obtaining lock", e);
            System.exit(1);
        }

        // Forzar a JNativeHook a extraer sus librerías nativas en el directorio temporal del usuario
        System.setProperty("jnativehook.lib.path", System.getProperty("java.io.tmpdir"));

        AppConfig.loadSystemProperties();
        java.awt.Toolkit.getDefaultToolkit();
        Application.launch(AppLauncher.class, args);
    }
}