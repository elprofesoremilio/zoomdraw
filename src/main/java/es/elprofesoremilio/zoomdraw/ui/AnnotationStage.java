package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.commands.*;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.ui.controllers.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.ImageCursor;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AnnotationStage extends Stage implements BrushSettingsUpdater {

    private final AnnotationManager manager;

    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final TextField hiddenTextField;

    private final CommandHistory commandHistory;
    private final WritableImage background;

    private final ImageCursor pencilCursor;

    private Color backgroundColorOverride = null;

    // Specialized Controllers
    private final ZoomPanController zoomPanController;
    private final DrawingController drawingController;
    private final TextToolController textToolController;
    private final NumberingToolController numberingToolController;
    private final CropToolController cropToolController;
    private final CaptureController captureController;
    private final InputDispatcher inputDispatcher;

    public AnnotationStage(AnnotationManager manager, Rectangle2D bounds, WritableImage background) {
        super(StageStyle.TRANSPARENT);
        this.manager = manager;
        this.background = background;
        this.commandHistory = manager.getGlobalHistory();

        // Initialize permanent canvas
        canvasPermanent = new Canvas(bounds.getWidth(), bounds.getHeight());
        gcPermanent = canvasPermanent.getGraphicsContext2D();
        gcPermanent.drawImage(this.background, 0, 0); // Draw background once

        // Initialize temporal canvas
        canvasTemporal = new Canvas(bounds.getWidth(), bounds.getHeight());
        gcTemporal = canvasTemporal.getGraphicsContext2D();

        // --- Configuración del TextField oculto para soporte de acentos en Linux ---
        hiddenTextField = new TextField();
        hiddenTextField.setOpacity(0);             // Totalmente invisible
        hiddenTextField.setPrefSize(1, 1);         // Tamaño minúsculo
        hiddenTextField.setMaxSize(1, 1);
        hiddenTextField.setFocusTraversable(false); // Evita que interfiera con el tabulador normal

        // Create pencil cursor
        Canvas cursorCanvas = new Canvas(32, 32);
        GraphicsContext cgc = cursorCanvas.getGraphicsContext2D();
        cgc.setLineWidth(1.2);
        cgc.setStroke(Color.BLACK);
        //   CUERPO
        cgc.setFill(Color.GOLD);
        double[] bodyX = {8, 23, 28, 13};
        double[] bodyY = {13, 28, 23, 8};
        cgc.fillPolygon(bodyX, bodyY, 4);
        cgc.strokePolygon(bodyX, bodyY, 4);
        //   MADERA
        cgc.setFill(Color.TAN);
        double[] woodX = {2, 8, 13};
        double[] woodY = {2, 13, 8};
        cgc.fillPolygon(woodX, woodY, 3);
        cgc.strokePolygon(woodX, woodY, 3);
        //   PUNTA
        cgc.setFill(Color.BLACK);
        double[] tipX = {0, 3, 5};
        double[] tipY = {0, 5, 3};
        cgc.fillPolygon(tipX, tipY, 3);
        //   GOMA
        cgc.setFill(Color.HOTPINK);
        double[] eraserX = {23, 28, 31, 26};
        double[] eraserY = {28, 23, 26, 31};
        cgc.fillPolygon(eraserX, eraserY, 4);
        cgc.strokePolygon(eraserX, eraserY, 4);
        //   METAL
        cgc.setFill(Color.LIGHTGRAY);
        double[] metalX = {20, 23, 26, 23};
        double[] metalY = {25, 28, 25, 22};
        cgc.fillPolygon(metalX, metalY, 4);
        cgc.strokePolygon(metalX, metalY, 4);
        //   SNAPSHOT
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage cursorImage = cursorCanvas.snapshot(params, null);
        this.pencilCursor = new ImageCursor(cursorImage, 0, 0);

        // Instantiate specialized controllers
        this.zoomPanController = new ZoomPanController();
        this.drawingController = new DrawingController(this, canvasPermanent, canvasTemporal, zoomPanController, commandHistory, manager);
        this.textToolController = new TextToolController(this, canvasPermanent, canvasTemporal, hiddenTextField, zoomPanController, commandHistory, manager);
        this.numberingToolController = new NumberingToolController(this, canvasPermanent, canvasTemporal, zoomPanController, commandHistory, manager);
        this.captureController = new CaptureController(this, canvasTemporal, manager);
        this.cropToolController = new CropToolController(this, canvasTemporal, captureController);

        // Link dispatcher
        this.inputDispatcher = new InputDispatcher(
                zoomPanController,
                drawingController,
                textToolController,
                numberingToolController,
                cropToolController,
                captureController,
                manager,
                this
        );

        // Set initial brush settings for both GCs
        updateBrushSettings();
        
        // Dibuja el historial sobre el fondo
        redrawAll();

        StackPane root = new StackPane(canvasPermanent, canvasTemporal, hiddenTextField);
        root.setBackground(null);
        root.setFocusTraversable(true);
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);

        // Attach input handler dispatcher to scene
        inputDispatcher.attach(scene);

        scene.setCursor(pencilCursor);
        this.setScene(scene);
        this.setAlwaysOnTop(true);

        // Posicionamiento absoluto
        this.setX(bounds.getMinX());
        this.setY(bounds.getMinY());
        this.setWidth(bounds.getWidth());
        this.setHeight(bounds.getHeight());
        this.setResizable(false);
    }

    @Override
    public void updateBrushSettings() {
        Color currentColor = manager.getCurrentColor();
        double currentLineWidth = manager.getCurrentLineWidth();

        // Apply settings to permanent graphics context
        gcPermanent.setStroke(currentColor);
        gcPermanent.setLineWidth(currentLineWidth);
        gcPermanent.setLineCap(StrokeLineCap.ROUND);
        gcPermanent.setLineJoin(StrokeLineJoin.ROUND);

        // Apply settings to temporal graphics context
        gcTemporal.setStroke(currentColor);
        gcTemporal.setLineWidth(currentLineWidth);
        gcTemporal.setLineCap(StrokeLineCap.ROUND);
        gcTemporal.setLineJoin(StrokeLineJoin.ROUND);
    }

    public void drawCurrentBackground() {
        if (backgroundColorOverride != null) {
            gcPermanent.setFill(backgroundColorOverride);
            gcPermanent.fillRect(0, 0, canvasPermanent.getWidth(), canvasPermanent.getHeight());
        } else {
            // Limpiamos el canvas
            gcPermanent.clearRect(0, 0, canvasPermanent.getWidth(), canvasPermanent.getHeight());
            // Aplicamos un fondo casi invisible (1% opacidad) para asegurar que el OS
            // no trate la ventana como "click-through" (traspasable) si la captura falla
            // o tiene píxeles transparentes por accidente.
            gcPermanent.setFill(new Color(1, 1, 1, 0.01));
            gcPermanent.fillRect(0, 0, canvasPermanent.getWidth(), canvasPermanent.getHeight());
            
            if (background != null) {
                gcPermanent.drawImage(background, 0, 0);
            }
        }
    }

    public void redrawAll() {
        gcPermanent.clearRect(0, 0, canvasPermanent.getWidth(), canvasPermanent.getHeight());
        gcPermanent.save();
        zoomPanController.applyTransform(gcPermanent);
        gcPermanent.setImageSmoothing(true);
        
        drawCurrentBackground();
        for (DrawingCommand cmd : commandHistory.getHistory()) {
            cmd.execute(gcPermanent);
        }
        
        if (numberingToolController.isActive()) {
            for (NumberedCircle c : numberingToolController.getTemporalCircles()) {
                if (c == numberingToolController.getHoveredCircle()) {
                    c.drawHoverHalo(gcPermanent);
                }
                c.draw(gcPermanent);
                if (c == numberingToolController.getSelectedCircle()) {
                    c.drawSelection(gcPermanent);
                }
            }
        }
        
        gcPermanent.restore();
        updateBrushSettings();
    }

    public WritableImage get1xCanvasSnapshot() {
        double savedZoom = zoomPanController.getZoomFactor();
        double savedOffsetX = zoomPanController.getOffsetX();
        double savedOffsetY = zoomPanController.getOffsetY();
        
        zoomPanController.setZoomFactor(1.0);
        zoomPanController.setOffsetX(0.0);
        zoomPanController.setOffsetY(0.0);
        
        redrawAll();
        
        SnapshotParameters paramsCensor = new SnapshotParameters();
        paramsCensor.setFill(Color.TRANSPARENT);
        WritableImage snapshot = canvasPermanent.snapshot(paramsCensor, null);
        
        zoomPanController.setZoomFactor(savedZoom);
        zoomPanController.setOffsetX(savedOffsetX);
        zoomPanController.setOffsetY(savedOffsetY);
        
        redrawAll();
        
        return snapshot;
    }

    // Getters for stage properties
    public AnnotationManager getManager() {
        return manager;
    }

    public CommandHistory getCommandHistory() {
        return commandHistory;
    }

    public ImageCursor getPencilCursor() {
        return pencilCursor;
    }

    public Color getBackgroundColorOverride() {
        return backgroundColorOverride;
    }

    public void setBackgroundColorOverride(Color color) {
        this.backgroundColorOverride = color;
    }

    // Getters for specialized controllers
    public ZoomPanController getZoomPanController() {
        return zoomPanController;
    }

    public DrawingController getDrawingController() {
        return drawingController;
    }

    public TextToolController getTextToolController() {
        return textToolController;
    }

    public NumberingToolController getNumberingToolController() {
        return numberingToolController;
    }

    public CropToolController getCropToolController() {
        return cropToolController;
    }

    public CaptureController getCaptureController() {
        return captureController;
    }

    // Submode queries for AnnotationManager
    public boolean isTextModeActive() {
        return textToolController != null && textToolController.isActive();
    }

    public boolean isNumberingModeActive() {
        return numberingToolController != null && numberingToolController.isActive();
    }

    public boolean isCropModeActive() {
        return cropToolController != null && cropToolController.isActive();
    }
}