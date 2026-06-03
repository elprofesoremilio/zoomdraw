package es.elprofesoremilio.zoomdraw.ui.controllers;

import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import es.elprofesoremilio.zoomdraw.ui.controllers.CaptureController.CropAction;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;

public class CropToolController {
    private enum DragType {
        NONE,
        MOVE,
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        RIGHT_CENTER, BOTTOM_RIGHT, BOTTOM_CENTER,
        BOTTOM_LEFT, LEFT_CENTER
    }

    private final AnnotationStage stage;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final CaptureController captureController;

    private boolean isCropModeActive = false;
    private double cropX = 0.0;
    private double cropY = 0.0;
    private double cropW = 0.0;
    private double cropH = 0.0;

    private CropAction pendingCropAction;
    private Timeline marchingAntsTimeline = null;
    private double dashOffset = 0.0;

    private DragType currentDragType = DragType.NONE;
    private double dragStartX = 0.0;
    private double dragStartY = 0.0;
    private double initialCropX = 0.0;
    private double initialCropY = 0.0;
    private double initialCropW = 0.0;
    private double initialCropH = 0.0;

    public CropToolController(AnnotationStage stage, Canvas canvasTemporal, CaptureController captureController) {
        this.stage = stage;
        this.canvasTemporal = canvasTemporal;
        this.gcTemporal = canvasTemporal.getGraphicsContext2D();
        this.captureController = captureController;
    }

    public boolean isActive() {
        return isCropModeActive;
    }

    public double getCropX() {
        return cropX;
    }

    public double getCropY() {
        return cropY;
    }

    public double getCropW() {
        return cropW;
    }

    public double getCropH() {
        return cropH;
    }

    public void enterCropMode(CropAction action) {
        isCropModeActive = true;
        pendingCropAction = action;

        double canvasW = canvasTemporal.getWidth();
        double canvasH = canvasTemporal.getHeight();
        cropW = canvasW / 4.0;
        cropH = canvasH / 4.0;
        cropX = (canvasW - cropW) / 2.0;
        cropY = (canvasH - cropH) / 2.0;

        stage.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
        startMarchingAnts();
        renderCropOverlay();
    }

    public void exitCropMode(boolean cancelled) {
        isCropModeActive = false;
        stopMarchingAnts();

        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        stage.getScene().setCursor(stage.getPencilCursor());

        if (cancelled) {
            stage.getManager().notifySubModeCancelled();
        }
    }

    public void confirmCrop() {
        double x = cropX;
        double y = cropY;
        double w = cropW;
        double h = cropH;
        CropAction action = pendingCropAction;

        exitCropMode(false);
        captureController.captureCropped(x, y, w, h, action);
    }

    private void startMarchingAnts() {
        if (marchingAntsTimeline != null) {
            marchingAntsTimeline.stop();
        }
        marchingAntsTimeline = new Timeline(
                new KeyFrame(
                        Duration.millis(100),
                        e -> {
                            dashOffset = (dashOffset + 2.0) % 16.0;
                            if (isCropModeActive) {
                                renderCropOverlay();
                            }
                        }
                )
        );
        marchingAntsTimeline.setCycleCount(Animation.INDEFINITE);
        marchingAntsTimeline.play();
    }

    private void stopMarchingAnts() {
        if (marchingAntsTimeline != null) {
            marchingAntsTimeline.stop();
            marchingAntsTimeline = null;
        }
    }

    public void renderCropOverlay() {
        if (!isCropModeActive) return;

        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());

        // Oscurecer
        gcTemporal.setFill(new Color(0, 0, 0, 0.6));
        gcTemporal.fillRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());

        // Cutout
        gcTemporal.clearRect(cropX, cropY, cropW, cropH);

        // Borde azul claro
        gcTemporal.setStroke(Color.web("#80d8ff"));
        gcTemporal.setLineWidth(2.0);
        gcTemporal.setLineDashes((double[]) null);
        gcTemporal.strokeRect(cropX, cropY, cropW, cropH);

        // Borde blanco dashed
        gcTemporal.setStroke(Color.WHITE);
        gcTemporal.setLineDashes(8.0, 8.0);
        gcTemporal.setLineDashOffset(dashOffset);
        gcTemporal.strokeRect(cropX, cropY, cropW, cropH);

        // Agarraderas
        drawHandle(gcTemporal, cropX, cropY);
        drawHandle(gcTemporal, cropX + cropW, cropY);
        drawHandle(gcTemporal, cropX, cropY + cropH);
        drawHandle(gcTemporal, cropX + cropW, cropY + cropH);

        drawHandle(gcTemporal, cropX + cropW / 2.0, cropY);
        drawHandle(gcTemporal, gcTemporal.getCanvas().getWidth() > 0 ? cropX + cropW / 2.0 : 0, cropY + cropH); // BC anchor
        drawHandle(gcTemporal, cropX, cropY + cropH / 2.0);
        drawHandle(gcTemporal, cropX + cropW, cropY + cropH / 2.0);
    }

    private void drawHandle(GraphicsContext gc, double x, double y) {
        double size = 8.0;
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.web("#0091ea"));
        gc.setLineWidth(1.5);
        gc.setLineDashes((double[]) null);
        gc.fillRect(x - size / 2.0, y - size / 2.0, size, size);
        gc.strokeRect(x - size / 2.0, y - size / 2.0, size, size);
    }

    public void handleKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE) {
            exitCropMode(true);
        } else if (event.getCode() == KeyCode.ENTER) {
            confirmCrop();
        }
        event.consume();
    }

    public void handleMousePressed(MouseEvent event) {
        if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
            double mx = event.getX();
            double my = event.getY();
            currentDragType = getDragType(mx, my);
            if (currentDragType != DragType.NONE) {
                dragStartX = mx;
                dragStartY = my;
                initialCropX = cropX;
                initialCropY = cropY;
                initialCropW = cropW;
                initialCropH = cropH;
            }
        }
        event.consume();
    }

    public void handleMouseDragged(MouseEvent event) {
        if (currentDragType != DragType.NONE) {
            double mx = event.getX();
            double my = event.getY();
            double dx = mx - dragStartX;
            double dy = my - dragStartY;

            double newX = initialCropX;
            double newY = initialCropY;
            double newW = initialCropW;
            double newH = initialCropH;

            switch (currentDragType) {
                case MOVE:
                    newX = initialCropX + dx;
                    newY = initialCropY + dy;
                    break;
                case TOP_LEFT:
                    newX = initialCropX + dx;
                    newY = initialCropY + dy;
                    newW = initialCropW - dx;
                    newH = initialCropH - dy;
                    break;
                case TOP_CENTER:
                    newY = initialCropY + dy;
                    newH = initialCropH - dy;
                    break;
                case TOP_RIGHT:
                    newY = initialCropY + dy;
                    newW = initialCropW + dx;
                    newH = initialCropH - dy;
                    break;
                case RIGHT_CENTER:
                    newW = initialCropW + dx;
                    break;
                case BOTTOM_RIGHT:
                    newW = initialCropW + dx;
                    newH = initialCropH + dy;
                    break;
                case BOTTOM_CENTER:
                    newH = initialCropH + dy;
                    break;
                case BOTTOM_LEFT:
                    newX = initialCropX + dx;
                    newW = initialCropW - dx;
                    newH = initialCropH + dy;
                    break;
                case LEFT_CENTER:
                    newX = initialCropX + dx;
                    newW = initialCropW - dx;
                    break;
            }

            // Min size guard
            double minSize = 20.0;
            if (newW < minSize) {
                if (currentDragType == DragType.TOP_LEFT || currentDragType == DragType.BOTTOM_LEFT || currentDragType == DragType.LEFT_CENTER) {
                    newX = initialCropX + initialCropW - minSize;
                }
                newW = minSize;
            }
            if (newH < minSize) {
                if (currentDragType == DragType.TOP_LEFT || currentDragType == DragType.TOP_RIGHT || currentDragType == DragType.TOP_CENTER) {
                    newY = initialCropY + initialCropH - minSize;
                }
                newH = minSize;
            }

            // Apply modifiers
            boolean isShift = event.isShiftDown();
            boolean isCtrl = event.isControlDown();

            if (currentDragType != DragType.MOVE) {
                if (isCtrl) {
                    // Square constraint: use shorter side
                    double s = Math.min(newW, newH);
                    newW = s;
                    newH = s;

                    // Adjust coordinates based on fixed point
                    switch (currentDragType) {
                        case TOP_LEFT:
                            newX = (initialCropX + initialCropW) - newW;
                            newY = (initialCropY + initialCropH) - newH;
                            break;
                        case TOP_RIGHT:
                            newX = initialCropX;
                            newY = (initialCropY + initialCropH) - newH;
                            break;
                        case BOTTOM_LEFT:
                            newX = (initialCropX + initialCropW) - newW;
                            newY = initialCropY;
                            break;
                        case BOTTOM_RIGHT:
                            newX = initialCropX;
                            newY = initialCropY;
                            break;
                        case TOP_CENTER:
                            newY = (initialCropY + initialCropH) - newH;
                            newX = initialCropX + (initialCropW - newW) / 2.0;
                            break;
                        case BOTTOM_CENTER:
                            newY = initialCropY;
                            newX = initialCropX + (initialCropW - newW) / 2.0;
                            break;
                        case LEFT_CENTER:
                            newX = (initialCropX + initialCropW) - newW;
                            newY = initialCropY + (initialCropH - newH) / 2.0;
                            break;
                        case RIGHT_CENTER:
                            newX = initialCropX;
                            newY = initialCropY + (initialCropH - newH) / 2.0;
                            break;
                    }
                } else if (isShift) {
                    // Aspect ratio constraint: maintain initial crop aspect ratio
                    double ratio = initialCropW / initialCropH;
                    // fit within newW, newH
                    double scale = Math.min(newW / initialCropW, newH / initialCropH);
                    newW = scale * initialCropW;
                    newH = scale * initialCropH;

                    // Adjust coordinates based on fixed point
                    switch (currentDragType) {
                        case TOP_LEFT:
                            newX = (initialCropX + initialCropW) - newW;
                            newY = (initialCropY + initialCropH) - newH;
                            break;
                        case TOP_RIGHT:
                            newX = initialCropX;
                            newY = (initialCropY + initialCropH) - newH;
                            break;
                        case BOTTOM_LEFT:
                            newX = (initialCropX + initialCropW) - newW;
                            newY = initialCropY;
                            break;
                        case BOTTOM_RIGHT:
                            newX = initialCropX;
                            newY = initialCropY;
                            break;
                        case TOP_CENTER:
                            newY = (initialCropY + initialCropH) - newH;
                            newX = initialCropX + (initialCropW - newW) / 2.0;
                            break;
                        case BOTTOM_CENTER:
                            newY = initialCropY;
                            newX = initialCropX + (initialCropW - newW) / 2.0;
                            break;
                        case LEFT_CENTER:
                            newX = (initialCropX + initialCropW) - newW;
                            newY = initialCropY + (initialCropH - newH) / 2.0;
                            break;
                        case RIGHT_CENTER:
                            newX = initialCropX;
                            newY = initialCropY + (initialCropH - newH) / 2.0;
                            break;
                    }
                }
            }

            // Clamp to canvas boundaries
            double canvasW = canvasTemporal.getWidth();
            double canvasH = canvasTemporal.getHeight();

            if (currentDragType == DragType.MOVE) {
                newX = Math.max(0, Math.min(canvasW - newW, newX));
                newY = Math.max(0, Math.min(canvasH - newH, newY));
            } else {
                double x1 = Math.max(0, newX);
                double y1 = Math.max(0, newY);
                double x2 = Math.min(canvasW, newX + newW);
                double y2 = Math.min(canvasH, newY + newH);

                newX = x1;
                newY = y1;
                newW = Math.max(minSize, x2 - x1);
                newH = Math.max(minSize, y2 - y1);
            }

            cropX = newX;
            cropY = newY;
            cropW = newW;
            cropH = newH;

            renderCropOverlay();
        }
        event.consume();
    }

    public void handleMouseReleased(MouseEvent event) {
        currentDragType = DragType.NONE;
        event.consume();
    }

    public void handleMouseMoved(MouseEvent event) {
        double mx = event.getX();
        double my = event.getY();
        DragType type = getDragType(mx, my);
        switch (type) {
            case TOP_LEFT:
            case BOTTOM_RIGHT:
                stage.getScene().setCursor(javafx.scene.Cursor.NW_RESIZE);
                break;
            case TOP_RIGHT:
            case BOTTOM_LEFT:
                stage.getScene().setCursor(javafx.scene.Cursor.NE_RESIZE);
                break;
            case TOP_CENTER:
            case BOTTOM_CENTER:
                stage.getScene().setCursor(javafx.scene.Cursor.N_RESIZE);
                break;
            case LEFT_CENTER:
            case RIGHT_CENTER:
                stage.getScene().setCursor(javafx.scene.Cursor.W_RESIZE);
                break;
            case MOVE:
                stage.getScene().setCursor(javafx.scene.Cursor.MOVE);
                break;
            default:
                stage.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
                break;
        }
        event.consume();
    }

    public void handleMouseClicked(MouseEvent event) {
        if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && event.getClickCount() == 2) {
            double mx = event.getX();
            double my = event.getY();
            if (mx >= cropX && mx <= cropX + cropW && my >= cropY && my <= cropY + cropH) {
                confirmCrop();
            }
        }
        event.consume();
    }

    private DragType getDragType(double x, double y) {
        double hitSize = 12.0;

        if (nearPoint(x, y, cropX, cropY, hitSize)) return DragType.TOP_LEFT;
        if (nearPoint(x, y, cropX + cropW, cropY, hitSize)) return DragType.TOP_RIGHT;
        if (nearPoint(x, y, cropX, cropY + cropH, hitSize)) return DragType.BOTTOM_LEFT;
        if (nearPoint(x, y, cropX + cropW, cropY + cropH, hitSize)) return DragType.BOTTOM_RIGHT;

        if (nearPoint(x, y, cropX + cropW / 2.0, cropY, hitSize)) return DragType.TOP_CENTER;
        if (nearPoint(x, y, cropX + cropW / 2.0, cropY + cropH, hitSize)) return DragType.BOTTOM_CENTER;
        if (nearPoint(x, y, cropX, cropY + cropH / 2.0, hitSize)) return DragType.LEFT_CENTER;
        if (nearPoint(x, y, cropX + cropW, cropY + cropH / 2.0, hitSize)) return DragType.RIGHT_CENTER;

        if (x >= cropX && x <= cropX + cropW && y >= cropY && y <= cropY + cropH) {
            return DragType.MOVE;
        }

        return DragType.NONE;
    }

    private boolean nearPoint(double x1, double y1, double x2, double y2, double hitSize) {
        return Math.abs(x1 - x2) <= hitSize && Math.abs(y1 - y2) <= hitSize;
    }
}
