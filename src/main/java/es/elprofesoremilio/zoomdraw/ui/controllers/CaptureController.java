package es.elprofesoremilio.zoomdraw.ui.controllers;

import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class CaptureController {
    public enum CropAction { CLIPBOARD, SAVE }

    private final AnnotationStage stage;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final AnnotationManager manager;

    public CaptureController(AnnotationStage stage, Canvas canvasTemporal, AnnotationManager manager) {
        this.stage = stage;
        this.canvasTemporal = canvasTemporal;
        this.gcTemporal = canvasTemporal.getGraphicsContext2D();
        this.manager = manager;
    }

    public void captureFull(CropAction action) {
        NumberingToolController numberingTool = stage.getNumberingToolController();
        boolean isNumberingActive = numberingTool != null && numberingTool.isActive();

        if (isNumberingActive) {
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = stage.getScene().getRoot().snapshot(params, null);

        if (isNumberingActive) {
            java.awt.Point cursor = java.awt.MouseInfo.getPointerInfo().getLocation();
            double mouseX = cursor.x - stage.getX();
            double mouseY = cursor.y - stage.getY();
            numberingTool.drawNumberPreview(mouseX, mouseY);
        }

        performCaptureAction(image, action);
    }

    public void captureCropped(double x, double y, double w, double h, CropAction action) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(x, y, w, h));

        WritableImage image = stage.getScene().getRoot().snapshot(params, null);
        performCaptureAction(image, action);
    }

    private void performCaptureAction(WritableImage image, CropAction action) {
        if (action == CropAction.CLIPBOARD) {
            boolean success = es.elprofesoremilio.zoomdraw.utils.CaptureUtils.copyToClipboard(image);
            if (success) {
                showFlashEffect();
            }
        } else if (action == CropAction.SAVE) {
            java.io.File file = showSaveDialog();
            if (file != null) {
                boolean success = es.elprofesoremilio.zoomdraw.utils.CaptureUtils.saveToFile(image, file);
                if (success) {
                    showFlashEffect();
                }
            }
        }
    }

    public void showFlashEffect() {
        gcTemporal.save();
        gcTemporal.setFill(new Color(1, 1, 1, 0.4));
        gcTemporal.fillRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        gcTemporal.restore();

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(80));
        pause.setOnFinished(e -> {
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
            CropToolController cropTool = stage.getCropToolController();
            if (cropTool != null && cropTool.isActive()) {
                cropTool.renderCropOverlay();
            }
        });
        pause.play();
    }

    private java.io.File showSaveDialog() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String defaultName = "anotacion_" + now.format(formatter) + ".png";

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Guardar captura de anotación");
        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Imagen PNG (*.png)", "*.png"));

        return fileChooser.showSaveDialog(stage);
    }
}
