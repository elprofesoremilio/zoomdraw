package es.elprofesoremilio.zoomdraw.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class CaptureUtils {

    /**
     * Copia una imagen al portapapeles del sistema.
     *
     * @param image La imagen a copiar.
     * @return true si se copió con éxito, false en caso contrario.
     */
    public static boolean copyToClipboard(WritableImage image) {
        if (image == null) {
            AppLogger.logError("No se puede copiar una imagen nula al portapapeles.");
            return false;
        }
        try {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putImage(image);
            clipboard.setContent(content);
            AppLogger.log("Imagen copiada al portapapeles con éxito.");
            return true;
        } catch (Exception e) {
            AppLogger.logError("Error al copiar la imagen al portapapeles: " + e.getMessage());
            return false;
        }
    }

    /**
     * Guarda una imagen en formato PNG en el archivo especificado.
     *
     * @param image La imagen a guardar.
     * @param file  El archivo destino.
     * @return true si se guardó con éxito, false en caso contrario.
     */
    public static boolean saveToFile(WritableImage image, File file) {
        if (image == null || file == null) {
            AppLogger.logError("Datos inválidos al intentar guardar la imagen en archivo.");
            return false;
        }
        try {
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
            boolean success = ImageIO.write(bufferedImage, "png", file);
            if (success) {
                AppLogger.log("Imagen guardada correctamente en: " + file.getAbsolutePath());
            } else {
                AppLogger.logError("El escritor de ImageIO no pudo guardar la imagen como PNG.");
            }
            return success;
        } catch (IOException e) {
            AppLogger.logError("Excepción al guardar la imagen en archivo: " + e.getMessage());
            return false;
        }
    }
}
