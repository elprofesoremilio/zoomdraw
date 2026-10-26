package es.elprofesoremilio.zoomdraw.ui.controllers;

import es.elprofesoremilio.zoomdraw.commands.CommandHistory;
import es.elprofesoremilio.zoomdraw.commands.StampCommand;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

/**
 * Stamp mode: V / X turn the cursor into a semi-transparent check / cross that is placed on
 * every left click. Right click leaves the mode. Size follows the current line width.
 */
public class StampToolController {
    private final AnnotationStage stage;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final GraphicsContext gcPermanent;
    private final ZoomPanController zoomPanController;
    private final CommandHistory commandHistory;
    private final AnnotationManager manager;

    private StampCommand.Type activeType = null;
    private double lastMouseX = Double.NaN;
    private double lastMouseY = Double.NaN;

    public StampToolController(AnnotationStage stage, Canvas canvasPermanent, Canvas canvasTemporal,
                               ZoomPanController zoomPanController, CommandHistory commandHistory,
                               AnnotationManager manager) {
        this.stage = stage;
        this.gcPermanent = canvasPermanent.getGraphicsContext2D();
        this.canvasTemporal = canvasTemporal;
        this.gcTemporal = canvasTemporal.getGraphicsContext2D();
        this.zoomPanController = zoomPanController;
        this.commandHistory = commandHistory;
        this.manager = manager;
    }

    public boolean isActive() {
        return activeType != null;
    }

    /** Enters stamp mode with the given symbol, or switches symbol if already active. */
    public void select(StampCommand.Type type) {
        if (activeType == type) return;
        activeType = type;
        stage.getScene().setCursor(Cursor.NONE);
        drawPreview();
    }

    public void exit() {
        if (activeType == null) return;
        activeType = null;
        clearPreview();
        stage.getScene().setCursor(stage.getPencilCursor());
    }

    public void handleMousePressed(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            exit();
            return;
        }
        if (event.getButton() != MouseButton.PRIMARY) return;

        lastMouseX = event.getX();
        lastMouseY = event.getY();
        StampCommand stamp = createStamp(lastMouseX, lastMouseY, false);
        gcPermanent.save();
        zoomPanController.applyTransform(gcPermanent);
        commandHistory.execute(stamp, gcPermanent);
        gcPermanent.restore();
        stage.redrawAll();
        drawPreview();
    }

    public void handleMouseMoved(double mouseX, double mouseY) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        drawPreview();
    }

    /** Redraws the ghost at the last known mouse position (after size or zoom changes). */
    public void drawPreview() {
        clearPreview();
        if (activeType == null || Double.isNaN(lastMouseX)) return;
        StampCommand ghost = createStamp(lastMouseX, lastMouseY, true);
        gcTemporal.save();
        zoomPanController.applyTransform(gcTemporal);
        ghost.execute(gcTemporal);
        gcTemporal.restore();
    }

    private void clearPreview() {
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    private StampCommand createStamp(double mouseX, double mouseY, boolean ghost) {
        double zoom = zoomPanController.getZoomFactor();
        Point2D center = new Point2D((mouseX - zoomPanController.getOffsetX()) / zoom,
                (mouseY - zoomPanController.getOffsetY()) / zoom);
        Color base = activeType == StampCommand.Type.CHECK ? AppConfig.STAMP_CHECK_COLOR : AppConfig.STAMP_CROSS_COLOR;
        Color color = ghost
                ? new Color(base.getRed(), base.getGreen(), base.getBlue(), AppConfig.STAMP_GHOST_OPACITY)
                : base;
        double size = manager.getCurrentLineWidth() * AppConfig.STAMP_SIZE_FACTOR;
        return new StampCommand(activeType, center, size, color);
    }
}
