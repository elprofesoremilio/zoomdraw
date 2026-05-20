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
        cgc.setStroke(Color.BLACK);
        cgc.setFill(Color.YELLOW);
        cgc.setLineWidth(1.5);
        cgc.fillPolygon(new double[] { 8, 24, 28, 12 }, new double[] { 12, 28, 24, 8 }, 4);
        cgc.strokePolygon(new double[] { 8, 24, 28, 12 }, new double[] { 12, 28, 24, 8 }, 4);
        cgc.setFill(Color.TAN);
        cgc.fillPolygon(new double[] { 0, 8, 12 }, new double[] { 0, 12, 8 }, 3);
        cgc.strokePolygon(new double[] { 0, 8, 12 }, new double[] { 0, 12, 8 }, 3);
        cgc.setFill(Color.BLACK);
        cgc.fillPolygon(new double[] { 0, 3, 5 }, new double[] { 0, 5, 3 }, 3);
        cgc.setFill(Color.PINK);
        cgc.fillPolygon(new double[] { 24, 28, 31, 27 }, new double[] { 28, 24, 27, 31 }, 4);
        cgc.strokePolygon(new double[] { 24, 28, 31, 27 }, new double[] { 28, 24, 27, 31 }, 4);
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
                    break;
                case E:
                    isEPressed = true;
                    if (!event.isControlDown() && !event.isAltDown() && !event.isShiftDown() && !event.isMetaDown()) {
                        commandHistory.execute(new ClearCommand(this::drawCurrentBackground), gcPermanent);
                        redrawAll();
                    }
                    break;
                case F:
                    isFPressed = true;
                    break;
                case C:
                    isCPressed = true;
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
                    break;
                case E:
                    isEPressed = false;
                    break;
                case F:
                    isFPressed = false;
                    break;
                case C:
                    isCPressed = false;
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
                    currentTextCommand = new TextCommand(new Point2D(event.getX(), event.getY()),
                            manager.getCurrentColor(), manager.getCurrentLineWidth());
                    scene.setCursor(javafx.scene.Cursor.NONE);
                    redrawTextTemporal();
                }
                return; // Do not trigger other tools
            }

            if (event.isControlDown()) {
                if (isRPressed)
                    activeShapeMode = DrawMode.RECTANGLE;
                else if (event.isAltDown() && isEPressed)
                    activeShapeMode = DrawMode.CIRCLE;
                else if (isEPressed)
                    activeShapeMode = DrawMode.ELLIPSE;
                else if (isFPressed)
                    activeShapeMode = DrawMode.ARROW;
                else
                    activeShapeMode = DrawMode.LINE;

                isDrawingShape = true;
                shapeStartPoint = new Point2D(event.getX(), event.getY());
            } else if (event.isShiftDown() && (isRPressed || isEPressed || isCPressed)) {
                if (isRPressed)
                    activeShapeMode = DrawMode.FILLED_RECTANGLE;
                else if (event.isAltDown() && isEPressed)
                    activeShapeMode = DrawMode.FILLED_CIRCLE;
                else if (isEPressed)
                    activeShapeMode = DrawMode.FILLED_ELLIPSE;
                else { // if (isCPressed) { // sobrentendido
                    activeShapeMode = DrawMode.CENSOR_RECTANGLE;
                    javafx.scene.SnapshotParameters paramsCensor = new javafx.scene.SnapshotParameters();
                    paramsCensor.setFill(Color.TRANSPARENT);
                    currentCanvasSnapshot = canvasPermanent.snapshot(paramsCensor, null);
                }

                isDrawingShape = true;
                shapeStartPoint = new Point2D(event.getX(), event.getY());
            } else {
                activeShapeMode = DrawMode.PENCIL;
                isDrawingShape = false;
                currentStrokePoints.clear();
                currentStrokePoints.add(new Point2D(event.getX(), event.getY()));
            }
        });

        scene.setOnMouseDragged(event -> {
            if (isDrawingShape && activeShapeMode != DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                ShapeCommand previewShape = new ShapeCommand(shapeStartPoint, new Point2D(event.getX(), event.getY()),
                        activeShapeMode, manager.getCurrentColor(), manager.getCurrentLineWidth(),
                        currentCanvasSnapshot);
                previewShape.execute(gcTemporal);
            } else if (activeShapeMode == DrawMode.PENCIL) {
                currentStrokePoints.add(new Point2D(event.getX(), event.getY()));
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                PathCommand previewPath = new PathCommand(currentStrokePoints, manager.getCurrentColor(),
                        manager.getCurrentLineWidth());
                previewPath.execute(gcTemporal);
            }
        });

        scene.setOnMouseReleased(event -> {
            if (isDrawingShape && activeShapeMode != DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                ShapeCommand finalShape = new ShapeCommand(shapeStartPoint, new Point2D(event.getX(), event.getY()),
                        activeShapeMode, manager.getCurrentColor(), manager.getCurrentLineWidth(),
                        currentCanvasSnapshot);
                commandHistory.execute(finalShape, gcPermanent);
                redrawAll();
                isDrawingShape = false;
                activeShapeMode = DrawMode.PENCIL;
                currentCanvasSnapshot = null;
            } else if (activeShapeMode == DrawMode.PENCIL) {
                gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                if (currentStrokePoints.size() >= 2) {
                    PathCommand finalPath = new PathCommand(currentStrokePoints, manager.getCurrentColor(),
                            manager.getCurrentLineWidth());
                    commandHistory.execute(finalPath, gcPermanent);
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
        drawCurrentBackground();
        for (DrawingCommand cmd : commandHistory.getHistory()) {
            cmd.execute(gcPermanent);
        }
        updateBrushSettings();
    }

    private void finishTextCommand() {
        if (currentTextCommand != null) {
            currentTextCommand.setShowCursor(false);
            if (!currentTextCommand.isEmpty()) {
                commandHistory.execute(currentTextCommand, gcPermanent);
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
            currentTextCommand.execute(gcTemporal);
        }
    }
}