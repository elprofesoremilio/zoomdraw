package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.commands.*;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.core.DrawMode;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.input.KeyCode;
import javafx.scene.ImageCursor;
import javafx.scene.SnapshotParameters;

import java.util.ArrayList;
import java.util.List;

public class AnnotationStage extends Stage implements BrushSettingsUpdater {

    private final AnnotationManager manager;

    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;

    private final List<Point2D> currentStrokePoints = new ArrayList<>();
    private final CommandHistory commandHistory;
    private final WritableImage background;

    private double zoomFactor = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;

    private DrawMode activeShapeMode = DrawMode.PENCIL;
    private boolean isDrawingShape = false;
    private Point2D shapeStartPoint = null;

    private boolean isRPressed = false;
    private boolean isEPressed = false;
    private boolean isFPressed = false;
    private boolean isCPressed = false;

    private WritableImage currentCanvasSnapshot = null;

    private boolean isTextModeActive = false;
    private boolean isTyping = false;
    private TextCommand currentTextCommand = null;

    private Color backgroundColorOverride = null;

    private final ImageCursor pencilCursor;

    public boolean isTextModeActive() {
        return isTextModeActive;
    }

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

        // Set initial brush settings for both GCs
        updateBrushSettings();
        
        // Dibuja el historial sobre el fondo
        redrawAll();

        StackPane root = new StackPane(canvasPermanent, canvasTemporal);
        root.setBackground(null);
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);

        // Instantiate and attach the input handler
        AnnotationInputHandler inputHandler = new AnnotationInputHandler(manager, this); // Pass 'this' as
                                                                                         // BrushSettingsUpdater
        inputHandler.attach(scene);

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.T) {
                isTextModeActive = !isTextModeActive;
                if (!isTextModeActive && isTyping) {
                    finishTextCommand();
                }
                if (isTextModeActive) {
                    scene.setCursor(javafx.scene.Cursor.TEXT);
                    activeShapeMode = DrawMode.TEXT;
                } else {
                    scene.setCursor(pencilCursor);
                    activeShapeMode = DrawMode.PENCIL;
                }
                event.consume();
                return;
            }

            boolean noSpecialKeys = !event.isControlDown() && !event.isAltDown() && !event.isMetaDown();
            if (isTyping) {
                if (event.getCode() == KeyCode.ESCAPE) {
                    cancelTextCommand();
                    event.consume();
                } else if (event.getCode() == KeyCode.BACK_SPACE) {
                    currentTextCommand.removeLast();
                    redrawTextTemporal();
                    event.consume();
                } else if (event.getCode() == KeyCode.ENTER) {
                    currentTextCommand.append(es.elprofesoremilio.zoomdraw.core.text.TextTokens.ENTER_TOKEN,
                            manager.getCurrentColor(), manager.getCurrentLineWidth());
                    redrawTextTemporal();
                    event.consume();
                } else if (event.getCode() == KeyCode.TAB) {
                    currentTextCommand.append(es.elprofesoremilio.zoomdraw.core.text.TextTokens.TAB_TOKEN,
                            manager.getCurrentColor(), manager.getCurrentLineWidth());
                    redrawTextTemporal();
                    event.consume();
                } else if (noSpecialKeys) {
                    // Prevenir que otras teclas se procesen como atajos mientras se escribe
                }
            } else if (isTextModeActive) {
                if (event.getCode() == KeyCode.ESCAPE) {
                    isTextModeActive = false;
                    activeShapeMode = DrawMode.PENCIL;
                    scene.setCursor(pencilCursor);
                    event.consume();
                } else if (noSpecialKeys) {
                    // En modo texto pero sin escribir, consumimos las teclas de colores y formas
                    // para que no hagan nada
                    if (event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
                        event.consume();
                    }
                }
            } else if (!event.isControlDown() && !event.isAltDown() && !event.isMetaDown() && !event.isShiftDown()) {
                // Number keys for stroke thickness
                if (event.getCode() == KeyCode.DIGIT1) { manager.setCurrentLineWidth(AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
                else if (event.getCode() == KeyCode.DIGIT2) { manager.setCurrentLineWidth(2.0*AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
                else if (event.getCode() == KeyCode.DIGIT3) { manager.setCurrentLineWidth(3.0*AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
                else if (event.getCode() == KeyCode.DIGIT4) { manager.setCurrentLineWidth(4.0*AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
                else if (event.getCode() == KeyCode.DIGIT5) { manager.setCurrentLineWidth(5.0*AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
                else if (event.getCode() == KeyCode.DIGIT6) { manager.setCurrentLineWidth(6.0*AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
                else if (event.getCode() == KeyCode.DIGIT7) { manager.setCurrentLineWidth(7.0*AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
                else if (event.getCode() == KeyCode.DIGIT8) { manager.setCurrentLineWidth(8.0*AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
                else if (event.getCode() == KeyCode.DIGIT9) { manager.setCurrentLineWidth(9.0*AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
                else if (event.getCode() == KeyCode.DIGIT0) { manager.setCurrentLineWidth(10.0*AppConfig.LINE_WIDTH_MULTIPLIER); updateBrushSettings(); event.consume(); }
            }
        });

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event -> {
            if (isTyping) {
                String character = event.getCharacter();
                if (!character.isEmpty() && character.charAt(0) >= 32 && character.charAt(0) != 127) {
                    currentTextCommand.append(character, manager.getCurrentColor(), manager.getCurrentLineWidth());
                    redrawTextTemporal();
                }
                event.consume();
            } else if (isTextModeActive) {
                event.consume();
            }
        });

        // Key trackers for shapes
        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.isControlDown()) {
                if (event.getCode() == KeyCode.Z) {
                    commandHistory.undo(this::redrawAll);
                    event.consume();
                    return;
                } else if (event.getCode() == KeyCode.Y) {
                    commandHistory.redo(this::redrawAll);
                    event.consume();
                    return;
                } else if (event.getCode() == KeyCode.K) {
                    backgroundColorOverride = (backgroundColorOverride == Color.BLACK) ? null : Color.BLACK;
                    redrawAll();
                    event.consume();
                    return;
                } else if (event.getCode() == KeyCode.W) {
                    backgroundColorOverride = (backgroundColorOverride == Color.WHITE) ? null : Color.WHITE;
                    redrawAll();
                    event.consume();
                    return;
                }
            }
            switch (event.getCode()) {
                case R:
                    isRPressed = true;
                    event.consume();
                    break;
                case E:
                    isEPressed = true;
                    if (!event.isControlDown() && !event.isAltDown() && !event.isShiftDown() && !event.isMetaDown()) {
                        commandHistory.execute(new ClearCommand(this::drawCurrentBackground), gcPermanent);
                        redrawAll();
                    }
                    event.consume();
                    break;
                case F:
                    isFPressed = true;
                    event.consume();
                    break;
                case C:
                    isCPressed = true;
                    event.consume();
                    break;
                case ESCAPE:
                    if (isDrawingShape) {
                        isDrawingShape = false;
                        activeShapeMode = DrawMode.PENCIL;
                        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                        event.consume();
                    }
                    break;
                default:
                    break;
            }
        });

        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_RELEASED, event -> {
            switch (event.getCode()) {
                case R:
                    isRPressed = false;
                    event.consume();
                    break;
                case E:
                    isEPressed = false;
                    event.consume();
                    break;
                case F:
                    isFPressed = false;
                    event.consume();
                    break;
                case C:
                    isCPressed = false;
                    event.consume();
                    break;
                default:
                    break;
            }
        });

        // Mouse events for drawing
        scene.setOnMousePressed(event -> {
            if (!this.isFocused()) {
                this.requestFocus();
            }
            manager.bringHelpWindowToFront();

            if (isTextModeActive) {
                if (isTyping) {
                    finishTextCommand();
                    isTextModeActive = false;
                    activeShapeMode = DrawMode.PENCIL;
                    this.getScene().setCursor(pencilCursor);
                    return; // Do not trigger other tools and do not start a new text block
                }
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    isTyping = true;
                    double origX = (event.getX() - offsetX) / zoomFactor;
                    double origY = (event.getY() - offsetY) / zoomFactor;
                    currentTextCommand = new TextCommand(new Point2D(origX, origY),
                            manager.getCurrentColor(), manager.getCurrentLineWidth());
                    scene.setCursor(javafx.scene.Cursor.NONE);
                    redrawTextTemporal();
                }
                return; // Do not trigger other tools
            }

            boolean isCtrl = event.isControlDown();
            boolean isShift = event.isShiftDown();
            boolean isAlt = event.isAltDown();

            if (isShift && (isRPressed || isEPressed || isCPressed)) {
                if (isRPressed) {
                    activeShapeMode = DrawMode.FILLED_RECTANGLE;
                } else if (isEPressed) {
                    if (isAlt) {
                        activeShapeMode = DrawMode.FILLED_CIRCLE;
                    } else {
                        activeShapeMode = DrawMode.FILLED_ELLIPSE;
                    }
                } else { // isCPressed is true
                    activeShapeMode = DrawMode.CENSOR_RECTANGLE;
                    currentCanvasSnapshot = get1xCanvasSnapshot();
                }
                isDrawingShape = true;
                double origX = (event.getX() - offsetX) / zoomFactor;
                double origY = (event.getY() - offsetY) / zoomFactor;
                shapeStartPoint = new Point2D(origX, origY);
            } else if (isCtrl) {
                if (isRPressed) {
                    activeShapeMode = DrawMode.RECTANGLE;
                } else if (isEPressed) {
                    if (isAlt) {
                        activeShapeMode = DrawMode.CIRCLE;
                    } else {
                        activeShapeMode = DrawMode.ELLIPSE;
                    }
                } else if (isFPressed) {
                    activeShapeMode = DrawMode.ARROW;
                } else {
                    activeShapeMode = DrawMode.LINE;
                }
                isDrawingShape = true;
                double origX = (event.getX() - offsetX) / zoomFactor;
                double origY = (event.getY() - offsetY) / zoomFactor;
                shapeStartPoint = new Point2D(origX, origY);
            } else {
                activeShapeMode = DrawMode.PENCIL;
                isDrawingShape = false;
                currentStrokePoints.clear();
                double origX = (event.getX() - offsetX) / zoomFactor;
                double origY = (event.getY() - offsetY) / zoomFactor;
                currentStrokePoints.add(new Point2D(origX, origY));
            }
        });

        scene.setOnMouseDragged(event -> {
            double origX = (event.getX() - offsetX) / zoomFactor;
            double origY = (event.getY() - offsetY) / zoomFactor;
            Point2D origPoint = new Point2D(origX, origY);

            if (isDrawingShape && activeShapeMode != DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                gcTemporal.save();
                gcTemporal.translate(offsetX, offsetY);
                gcTemporal.scale(zoomFactor, zoomFactor);
                ShapeCommand previewShape = new ShapeCommand(shapeStartPoint, origPoint,
                        activeShapeMode, manager.getCurrentColor(), manager.getCurrentLineWidth(),
                        currentCanvasSnapshot);
                previewShape.execute(gcTemporal);
                gcTemporal.restore();
            } else if (activeShapeMode == DrawMode.PENCIL) {
                currentStrokePoints.add(origPoint);
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                gcTemporal.save();
                gcTemporal.translate(offsetX, offsetY);
                gcTemporal.scale(zoomFactor, zoomFactor);
                PathCommand previewPath = new PathCommand(currentStrokePoints, manager.getCurrentColor(),
                        manager.getCurrentLineWidth());
                previewPath.execute(gcTemporal);
                gcTemporal.restore();
            }
        });

        scene.setOnMouseReleased(event -> {
            double origX = (event.getX() - offsetX) / zoomFactor;
            double origY = (event.getY() - offsetY) / zoomFactor;
            Point2D origPoint = new Point2D(origX, origY);

            if (isDrawingShape && activeShapeMode != DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                ShapeCommand finalShape = new ShapeCommand(shapeStartPoint, origPoint,
                        activeShapeMode, manager.getCurrentColor(), manager.getCurrentLineWidth(),
                        currentCanvasSnapshot);
                gcPermanent.save();
                gcPermanent.translate(offsetX, offsetY);
                gcPermanent.scale(zoomFactor, zoomFactor);
                commandHistory.execute(finalShape, gcPermanent);
                gcPermanent.restore();
                redrawAll();
                isDrawingShape = false;
                activeShapeMode = DrawMode.PENCIL;
                currentCanvasSnapshot = null;
            } else if (activeShapeMode == DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                if (currentStrokePoints.size() >= 2) {
                    PathCommand finalPath = new PathCommand(currentStrokePoints, manager.getCurrentColor(),
                            manager.getCurrentLineWidth());
                    gcPermanent.save();
                    gcPermanent.translate(offsetX, offsetY);
                    gcPermanent.scale(zoomFactor, zoomFactor);
                    commandHistory.execute(finalPath, gcPermanent);
                    gcPermanent.restore();
                    redrawAll();
                }
                currentStrokePoints.clear();
            }
        });

        // Recuperar foco al clic
        scene.setOnMouseClicked(event -> {
            if (!this.isFocused()) {
                this.requestFocus();
            }
        });

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

    private void drawCurrentBackground() {
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

    private void redrawAll() {
        gcPermanent.clearRect(0, 0, canvasPermanent.getWidth(), canvasPermanent.getHeight());
        gcPermanent.save();
        gcPermanent.translate(offsetX, offsetY);
        gcPermanent.scale(zoomFactor, zoomFactor);
        gcPermanent.setImageSmoothing(true);
        
        drawCurrentBackground();
        for (DrawingCommand cmd : commandHistory.getHistory()) {
            cmd.execute(gcPermanent);
        }
        
        gcPermanent.restore();
        updateBrushSettings();
    }

    public void handleZoomScroll(javafx.scene.input.ScrollEvent event) {
        double deltaY = event.getDeltaY();
        if (deltaY == 0) return;

        double previousZoomFactor = zoomFactor;

        if (deltaY > 0) {
            // Zoom in: multiply by 1.2
            zoomFactor = Math.min(8.0, zoomFactor * 1.2);
        } else {
            // Zoom out: divide by 1.2
            zoomFactor = Math.max(1.0, zoomFactor / 1.2);
        }

        double mouseX = event.getX();
        double mouseY = event.getY();

        if (zoomFactor == 1.0) {
            offsetX = 0.0;
            offsetY = 0.0;
        } else {
            double origX = (mouseX - offsetX) / previousZoomFactor;
            double origY = (mouseY - offsetY) / previousZoomFactor;

            offsetX = mouseX - origX * zoomFactor;
            offsetY = mouseY - origY * zoomFactor;
        }

        redrawAll();
        if (isTyping) {
            redrawTextTemporal();
        }
    }

    private WritableImage get1xCanvasSnapshot() {
        Canvas tempCanvas = new Canvas(canvasPermanent.getWidth(), canvasPermanent.getHeight());
        GraphicsContext tempGc = tempCanvas.getGraphicsContext2D();
        
        if (backgroundColorOverride != null) {
            tempGc.setFill(backgroundColorOverride);
            tempGc.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
        } else {
            tempGc.setFill(new Color(1, 1, 1, 0.01));
            tempGc.fillRect(0, 0, tempCanvas.getWidth(), tempCanvas.getHeight());
            if (background != null) {
                tempGc.drawImage(background, 0, 0);
            }
        }
        
        for (DrawingCommand cmd : commandHistory.getHistory()) {
            cmd.execute(tempGc);
        }
        
        SnapshotParameters paramsCensor = new SnapshotParameters();
        paramsCensor.setFill(Color.TRANSPARENT);
        return tempCanvas.snapshot(paramsCensor, null);
    }

    private void finishTextCommand() {
        if (currentTextCommand != null) {
            currentTextCommand.setShowCursor(false);
            if (!currentTextCommand.isEmpty()) {
                gcPermanent.save();
                gcPermanent.translate(offsetX, offsetY);
                gcPermanent.scale(zoomFactor, zoomFactor);
                commandHistory.execute(currentTextCommand, gcPermanent);
                gcPermanent.restore();
                redrawAll();
            }
            currentTextCommand = null;
        }
        isTyping = false;
        this.getScene().setCursor(javafx.scene.Cursor.TEXT);
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    private void cancelTextCommand() {
        currentTextCommand = null;
        isTyping = false;
        isTextModeActive = false;
        this.getScene().setCursor(pencilCursor);
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    private void redrawTextTemporal() {
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        if (currentTextCommand != null) {
            gcTemporal.save();
            gcTemporal.translate(offsetX, offsetY);
            gcTemporal.scale(zoomFactor, zoomFactor);
            currentTextCommand.execute(gcTemporal);
            gcTemporal.restore();
        }
    }
}