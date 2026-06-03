package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.commands.*;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.core.DrawMode;
import javafx.scene.control.TextField;
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
import java.util.Stack;

public class AnnotationStage extends Stage implements BrushSettingsUpdater {

    private final AnnotationManager manager;

    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final TextField hiddenTextField; // <--- NUEVA VARIABLE

    private final List<Point2D> currentStrokePoints = new ArrayList<>();
    private final CommandHistory commandHistory;
    private final WritableImage background;

    private double zoomFactor = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;

    private boolean isNumberingModeActive = false;
    private final List<NumberedCircle> temporalCircles = new ArrayList<>();
    private NumberedCircle selectedCircle = null;
    private NumberedCircle hoveredCircle = null;
    private final Stack<List<NumberedCircle>> numberingUndoStack = new Stack<>();
    private final Stack<List<NumberedCircle>> numberingRedoStack = new Stack<>();
    private Point2D dragOffset = null;
    private Point2D dragStartCenter = null;

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

    public boolean isNumberingModeActive() {
        return isNumberingModeActive;
    }

    private boolean isCropModeActive = false;
    private double cropX = 0.0;
    private double cropY = 0.0;
    private double cropW = 0.0;
    private double cropH = 0.0;

    private enum CropAction { CLIPBOARD, SAVE }
    private CropAction pendingCropAction;
    private javafx.animation.Timeline marchingAntsTimeline = null;
    private double dashOffset = 0.0;

    private enum DragType {
        NONE,
        MOVE,
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        RIGHT_CENTER, BOTTOM_RIGHT, BOTTOM_CENTER,
        BOTTOM_LEFT, LEFT_CENTER
    }
    private DragType currentDragType = DragType.NONE;
    private double dragStartX = 0.0;
    private double dragStartY = 0.0;
    private double initialCropX = 0.0;
    private double initialCropY = 0.0;
    private double initialCropW = 0.0;
    private double initialCropH = 0.0;

    public boolean isCropModeActive() {
        return isCropModeActive;
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
        // --- NUEVO: Configuración del TextField oculto para soporte de acentos en Linux ---
        hiddenTextField = new TextField();
        hiddenTextField.setOpacity(0);             // Totalmente invisible
        hiddenTextField.setPrefSize(1, 1);         // Tamaño minúsculo
        hiddenTextField.setMaxSize(1, 1);
        hiddenTextField.setFocusTraversable(false); // Evita que interfiera con el tabulador normal
        // ----------------------------------------------------------------------------------

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

        StackPane root = new StackPane(canvasPermanent, canvasTemporal, hiddenTextField);
        root.setBackground(null);
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);

        // Instantiate and attach the input handler
        AnnotationInputHandler inputHandler = new AnnotationInputHandler(manager, this); // Pass 'this' as
                                                                                         // BrushSettingsUpdater
        inputHandler.attach(scene);

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            boolean isCtrl = event.isControlDown();
            boolean isAlt = event.isAltDown();

            if (isTextModeActive) {
                if (event.getCode() == KeyCode.ESCAPE) {
                    cancelTextCommand();
                    event.consume();
                    return;
                }
                if (isTyping) {
                    if (event.getCode() == KeyCode.BACK_SPACE) {
                        currentTextCommand.removeLast();
                        redrawTextTemporal();
                        event.consume();
                        return;
                    } else if (event.getCode() == KeyCode.ENTER) {
                        currentTextCommand.append(es.elprofesoremilio.zoomdraw.core.text.TextTokens.ENTER_TOKEN,
                                manager.getCurrentColor(), manager.getCurrentLineWidth());
                        redrawTextTemporal();
                        event.consume();
                        return;
                    } else if (event.getCode() == KeyCode.TAB) {
                        currentTextCommand.append(es.elprofesoremilio.zoomdraw.core.text.TextTokens.TAB_TOKEN,
                                manager.getCurrentColor(), manager.getCurrentLineWidth());
                        redrawTextTemporal();
                        event.consume();
                        return;
                    }
                }
                if (isCtrl) {
                    event.consume();
                }
                return;
            }

            // Atajos de captura y recorte
            if (isCtrl) {
                if (isAlt) {
                    if (event.getCode() == KeyCode.C) {
                        enterCropMode(CropAction.CLIPBOARD);
                        event.consume();
                        return;
                    } else if (event.getCode() == KeyCode.S) {
                        enterCropMode(CropAction.SAVE);
                        event.consume();
                        return;
                    }
                } else {
                    if (event.getCode() == KeyCode.C) {
                        captureFull(CropAction.CLIPBOARD);
                        event.consume();
                        return;
                    } else if (event.getCode() == KeyCode.S) {
                        captureFull(CropAction.SAVE);
                        event.consume();
                        return;
                    }
                }
            }

            // Si está activo el modo recorte, consumir todas las teclas
            if (isCropModeActive) {
                if (event.getCode() == KeyCode.ESCAPE) {
                    exitCropMode(true);
                } else if (event.getCode() == KeyCode.ENTER) {
                    confirmCrop();
                }
                event.consume();
                return;
            }

            if (isNumberingModeActive) {
                boolean isCtrlKey = event.isControlDown();
                boolean isShift = event.isShiftDown();

                if (event.getCode() == KeyCode.ESCAPE) {
                    // ESC cancels numbering mode without committing — discard circles, return to annotation
                    cancelNumberingMode();
                    event.consume();
                } else if (isCtrlKey && event.getCode() == KeyCode.Z) {
                    undoNumbering();
                    event.consume();
                } else if ((isCtrlKey && event.getCode() == KeyCode.Y) || (isCtrlKey && isShift && event.getCode() == KeyCode.Z)) {
                    redoNumbering();
                    event.consume();
                } else if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                    if (selectedCircle != null) {
                        deleteSelectedNumber();
                        event.consume();
                    }
                } else {
                    event.consume();
                }
                return;
            }

            if (event.getCode() == KeyCode.T) {
                isTextModeActive = !isTextModeActive;
                if (!isTextModeActive && isTyping) {
                    finishTextCommand();
                }
                if (isTextModeActive) {
                    scene.setCursor(javafx.scene.Cursor.TEXT);
                    activeShapeMode = DrawMode.TEXT;
                    hiddenTextField.requestFocus(); // <--- NUEVA LÍNEA: Despierta el Input Method de Linux
                } else {
                    scene.setCursor(pencilCursor);
                    activeShapeMode = DrawMode.PENCIL;
                }
                event.consume();
                return;
            }

            if (!event.isControlDown() && !event.isAltDown() && !event.isMetaDown() && !event.isShiftDown()) {
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
            if (isCropModeActive) {
                event.consume();
                return;
            }
            if (isNumberingModeActive) {
                event.consume();
                return;
            }
            if (event.isControlDown() || event.isMetaDown()) {
                event.consume();
                return;
            }
            if (isTyping) {
                String character = event.getCharacter();
                if (!character.isEmpty()) {
                    char c = character.charAt(0);
                    if (!Character.isISOControl(c)) {
                        currentTextCommand.append(character, manager.getCurrentColor(), manager.getCurrentLineWidth());
                        redrawTextTemporal();
                    }
                }
                event.consume();
            } else if (isTextModeActive) {
                event.consume();
            }
        });

        // === NUEVO: CAPTURA DE ACENTOS Y CARACTERES COMPUESTOS EN LINUX ===
        scene.addEventFilter(javafx.scene.input.InputMethodEvent.INPUT_METHOD_TEXT_CHANGED, event -> {
            if (isCropModeActive || isNumberingModeActive) {
                event.consume();
                return;
            }
            if (isTextModeActive && isTyping) {
                String committed = event.getCommitted();
                if (committed != null && !committed.isEmpty()) {
                    // Insertamos el carácter acentuado (á, é, í, ó, ú, ñ, etc.)
                    currentTextCommand.append(committed, manager.getCurrentColor(), manager.getCurrentLineWidth());
                    redrawTextTemporal();
                }
                event.consume();
            }
        });


        // Key trackers for shapes
        scene.addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (isTextModeActive) {
                return;
            }
            if (isNumberingModeActive) {
                event.consume();
                return;
            }
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
                case N:
                    if (!isNumberingModeActive && !isTextModeActive && !isTyping) {
                        enterNumberingMode();
                        event.consume();
                    }
                    break;
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
            if (isTextModeActive) {
                event.consume();
                return;
            }
            if (isNumberingModeActive) {
                event.consume();
                return;
            }
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

            if (isCropModeActive) {
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
                return;
            }

            if (isNumberingModeActive) {
                // Right-click commits the numbering session as a permanent stroke
                if (event.getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                    exitNumberingMode();
                    event.consume();
                    return;
                }
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    double mouseX = event.getX();
                    double mouseY = event.getY();
                    double cursorX_orig = (mouseX - offsetX) / zoomFactor;
                    double cursorY_orig = (mouseY - offsetY) / zoomFactor;

                    NumberedCircle clicked = null;
                    double margin = 10.0;
                    for (NumberedCircle c : temporalCircles) {
                        double c_radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, c.getLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
                        double dist = Math.hypot(cursorX_orig - c.getCenter().getX(), cursorY_orig - c.getCenter().getY());
                        if (dist <= c_radius + margin) {
                            clicked = c;
                            break;
                        }
                    }

                    if (clicked != null) {
                        pushNumberingUndoState();
                        selectedCircle = clicked;
                        dragStartCenter = clicked.getCenter();
                        dragOffset = new Point2D(cursorX_orig - clicked.getCenter().getX(), cursorY_orig - clicked.getCenter().getY());
                        hoveredCircle = null;
                    } else {
                        if (selectedCircle != null) {
                            selectedCircle = null;
                        } else {
                            if (!isPreviewSuperposed(mouseX, mouseY)) {
                                pushNumberingUndoState();
                                double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, manager.getCurrentLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
                                double previewX_orig = (mouseX - offsetX) / zoomFactor;
                                double previewY_orig = (mouseY - offsetY) / zoomFactor - radius;
                                
                                int nextNum = temporalCircles.size() + 1;
                                NumberedCircle newCircle = new NumberedCircle(
                                    new Point2D(previewX_orig, previewY_orig),
                                    nextNum,
                                    manager.getCurrentColor(),
                                    manager.getCurrentLineWidth(),
                                    manager.getCurrentOpacity()
                                );
                                temporalCircles.add(newCircle);
                            }
                        }
                    }
                    redrawAll();
                    drawNumberPreview(mouseX, mouseY);
                }
                return;
            }

            if (isTextModeActive) {
                if (isTyping) {
                    finishTextCommand();
                    isTextModeActive = false;
                    activeShapeMode = DrawMode.PENCIL;
                    this.getScene().setCursor(pencilCursor);
                    return;
                }
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    isTyping = true;
                    double origX = (event.getX() - offsetX) / zoomFactor;
                    double origY = (event.getY() - offsetY) / zoomFactor;
                    currentTextCommand = new TextCommand(new Point2D(origX, origY),
                            manager.getCurrentColor(), manager.getCurrentLineWidth());
                    scene.setCursor(javafx.scene.Cursor.NONE);

                    hiddenTextField.requestFocus(); // <--- NUEVA LÍNEA: Asegura el foco al hacer clic

                    redrawTextTemporal();
                }
                return;
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
            if (isCropModeActive) {
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
                return;
            }

            if (isNumberingModeActive) {
                if (selectedCircle != null && dragOffset != null) {
                    double mouseX = event.getX();
                    double mouseY = event.getY();
                    double cursorX_orig = (mouseX - offsetX) / zoomFactor;
                    double cursorY_orig = (mouseY - offsetY) / zoomFactor;

                    selectedCircle.setCenter(new Point2D(cursorX_orig - dragOffset.getX(), cursorY_orig - dragOffset.getY()));
                    
                    redrawAll();
                    gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
                }
                return;
            }

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
            if (isCropModeActive) {
                currentDragType = DragType.NONE;
                event.consume();
                return;
            }

            if (isNumberingModeActive) {
                if (selectedCircle != null && dragStartCenter != null) {
                    if (selectedCircle.getCenter().distance(dragStartCenter) < 1.0) {
                        if (!numberingUndoStack.isEmpty()) {
                            numberingUndoStack.pop();
                        }
                    }
                    dragStartCenter = null;
                    dragOffset = null;
                }
                double mouseX = event.getX();
                double mouseY = event.getY();
                redrawAll();
                drawNumberPreview(mouseX, mouseY);
                return;
            }

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

        scene.setOnMouseMoved(event -> {
            if (isCropModeActive) {
                double mx = event.getX();
                double my = event.getY();
                DragType type = getDragType(mx, my);
                switch (type) {
                    case TOP_LEFT:
                    case BOTTOM_RIGHT:
                        scene.setCursor(javafx.scene.Cursor.NW_RESIZE);
                        break;
                    case TOP_RIGHT:
                    case BOTTOM_LEFT:
                        scene.setCursor(javafx.scene.Cursor.NE_RESIZE);
                        break;
                    case TOP_CENTER:
                    case BOTTOM_CENTER:
                        scene.setCursor(javafx.scene.Cursor.N_RESIZE);
                        break;
                    case LEFT_CENTER:
                    case RIGHT_CENTER:
                        scene.setCursor(javafx.scene.Cursor.W_RESIZE);
                        break;
                    case MOVE:
                        scene.setCursor(javafx.scene.Cursor.MOVE);
                        break;
                    default:
                        scene.setCursor(javafx.scene.Cursor.DEFAULT);
                        break;
                }
                event.consume();
                return;
            }

            if (isNumberingModeActive) {
                handleNumberingMouseMoved(event.getX(), event.getY());
            }
        });

        // Recuperar foco al clic
        scene.setOnMouseClicked(event -> {
            if (!this.isFocused()) {
                this.requestFocus();
            }

            if (isCropModeActive) {
                if (event.getButton() == javafx.scene.input.MouseButton.PRIMARY && event.getClickCount() == 2) {
                    double mx = event.getX();
                    double my = event.getY();
                    if (mx >= cropX && mx <= cropX + cropW && my >= cropY && my <= cropY + cropH) {
                        confirmCrop();
                    }
                }
                event.consume();
                return;
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
        
        if (isNumberingModeActive) {
            for (NumberedCircle c : temporalCircles) {
                if (c == hoveredCircle) {
                    c.drawHoverHalo(gcPermanent);
                }
                c.draw(gcPermanent);
                if (c == selectedCircle) {
                    c.drawSelection(gcPermanent);
                }
            }
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
        double savedZoom = zoomFactor;
        double savedOffsetX = offsetX;
        double savedOffsetY = offsetY;
        
        zoomFactor = 1.0;
        offsetX = 0.0;
        offsetY = 0.0;
        
        redrawAll();
        
        SnapshotParameters paramsCensor = new SnapshotParameters();
        paramsCensor.setFill(Color.TRANSPARENT);
        WritableImage snapshot = canvasPermanent.snapshot(paramsCensor, null);
        
        zoomFactor = savedZoom;
        offsetX = savedOffsetX;
        offsetY = savedOffsetY;
        
        redrawAll();
        
        return snapshot;
    }

    private void enterNumberingMode() {
        isNumberingModeActive = true;
        temporalCircles.clear();
        selectedCircle = null;
        hoveredCircle = null;
        numberingUndoStack.clear();
        numberingRedoStack.clear();
        this.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
        redrawAll();
    }

    /** Commits the numbering session as a permanent stroke and returns to free-draw. */
    private void exitNumberingMode() {
        isNumberingModeActive = false;

        if (!temporalCircles.isEmpty()) {
            NumberingSessionCommand cmd = new NumberingSessionCommand(temporalCircles);
            gcPermanent.save();
            gcPermanent.translate(offsetX, offsetY);
            gcPermanent.scale(zoomFactor, zoomFactor);
            commandHistory.execute(cmd, gcPermanent);
            gcPermanent.restore();
        }

        temporalCircles.clear();
        selectedCircle = null;
        hoveredCircle = null;
        numberingUndoStack.clear();
        numberingRedoStack.clear();

        this.getScene().setCursor(pencilCursor);
        redrawAll();
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    /** Cancels (discards) the numbering session and returns to free-draw without committing. */
    private void cancelNumberingMode() {
        manager.notifySubModeCancelled(); // stamp BEFORE clearing flags (race guard for GlobalKeyHook)
        isNumberingModeActive = false;
        temporalCircles.clear();
        selectedCircle = null;
        hoveredCircle = null;
        numberingUndoStack.clear();
        numberingRedoStack.clear();
        this.getScene().setCursor(pencilCursor);
        redrawAll();
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    private void pushNumberingUndoState() {
        List<NumberedCircle> copy = new ArrayList<>();
        for (NumberedCircle c : temporalCircles) {
            copy.add(new NumberedCircle(c));
        }
        numberingUndoStack.push(copy);
        numberingRedoStack.clear();
    }

    private void undoNumbering() {
        if (!numberingUndoStack.isEmpty()) {
            List<NumberedCircle> currentCopy = new ArrayList<>();
            for (NumberedCircle c : temporalCircles) {
                currentCopy.add(new NumberedCircle(c));
            }
            numberingRedoStack.push(currentCopy);

            List<NumberedCircle> prevState = numberingUndoStack.pop();
            temporalCircles.clear();
            for (NumberedCircle c : prevState) {
                temporalCircles.add(new NumberedCircle(c));
            }
            selectedCircle = null;
            hoveredCircle = null;
            redrawAll();
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        }
    }

    private void redoNumbering() {
        if (!numberingRedoStack.isEmpty()) {
            List<NumberedCircle> currentCopy = new ArrayList<>();
            for (NumberedCircle c : temporalCircles) {
                currentCopy.add(new NumberedCircle(c));
            }
            numberingUndoStack.push(currentCopy);

            List<NumberedCircle> nextState = numberingRedoStack.pop();
            temporalCircles.clear();
            for (NumberedCircle c : nextState) {
                temporalCircles.add(new NumberedCircle(c));
            }
            selectedCircle = null;
            hoveredCircle = null;
            redrawAll();
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        }
    }

    private void deleteSelectedNumber() {
        if (selectedCircle != null) {
            pushNumberingUndoState();
            int deletedNum = selectedCircle.getNumber();
            temporalCircles.remove(selectedCircle);
            selectedCircle = null;
            
            for (NumberedCircle c : temporalCircles) {
                if (c.getNumber() > deletedNum) {
                    c.setNumber(c.getNumber() - 1);
                }
            }
            
            redrawAll();
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        }
    }

    private void handleNumberingMouseMoved(double mouseX, double mouseY) {
        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, manager.getCurrentLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
        double previewX_orig = (mouseX - offsetX) / zoomFactor;
        double previewY_orig = (mouseY - offsetY) / zoomFactor - radius;

        NumberedCircle nextHovered = null;
        double margin = 10.0;
        double cursorX_orig = (mouseX - offsetX) / zoomFactor;
        double cursorY_orig = (mouseY - offsetY) / zoomFactor;

        // 1. Direct Hover check
        for (NumberedCircle c : temporalCircles) {
            double c_radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, c.getLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
            double dist = Math.hypot(cursorX_orig - c.getCenter().getX(), cursorY_orig - c.getCenter().getY());
            if (dist <= c_radius + margin) {
                nextHovered = c;
                break;
            }
        }

        // 2. Superposition check
        if (nextHovered == null) {
            for (NumberedCircle c : temporalCircles) {
                double c_radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, c.getLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
                double dist = Math.hypot(previewX_orig - c.getCenter().getX(), previewY_orig - c.getCenter().getY());
                if (dist < radius + c_radius) {
                    nextHovered = c;
                    break;
                }
            }
        }

        if (hoveredCircle != nextHovered) {
            hoveredCircle = nextHovered;
            redrawAll();
        }

        drawNumberPreview(mouseX, mouseY);
    }

    private void drawNumberPreview(double mouseX, double mouseY) {
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        
        if (!isNumberingModeActive) return;
        if (hoveredCircle != null || isPreviewSuperposed(mouseX, mouseY)) {
            return;
        }

        Color brushColor = manager.getCurrentColor();
        double lineWidth = manager.getCurrentLineWidth();
        double opacity = manager.getCurrentOpacity();

        double previewOpacity = opacity * 0.5;
        Color previewColor = new Color(brushColor.getRed(), brushColor.getGreen(), brushColor.getBlue(), previewOpacity);

        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, lineWidth * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
        double previewX_orig = (mouseX - offsetX) / zoomFactor;
        double previewY_orig = (mouseY - offsetY) / zoomFactor - radius;

        gcTemporal.save();
        gcTemporal.translate(offsetX, offsetY);
        gcTemporal.scale(zoomFactor, zoomFactor);

        gcTemporal.setFill(previewColor);
        gcTemporal.fillOval(previewX_orig - radius, previewY_orig - radius, radius * 2, radius * 2);

        double luminance = 0.299 * previewColor.getRed() + 0.587 * previewColor.getGreen() + 0.114 * previewColor.getBlue();
        Color textColor = (luminance > 0.5) ? Color.BLACK : Color.WHITE;
        Color finalTextColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), previewColor.getOpacity());
        gcTemporal.setFill(finalTextColor);

        int nextNumber = temporalCircles.size() + 1;
        String text = String.valueOf(nextNumber);
        gcTemporal.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, radius * 0.9));
        gcTemporal.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gcTemporal.setTextBaseline(javafx.geometry.VPos.CENTER);
        gcTemporal.fillText(text, previewX_orig, previewY_orig);

        gcTemporal.restore();
    }

    private boolean isPreviewSuperposed(double mouseX, double mouseY) {
        double radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, manager.getCurrentLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
        double previewX_orig = (mouseX - offsetX) / zoomFactor;
        double previewY_orig = (mouseY - offsetY) / zoomFactor - radius;

        for (NumberedCircle c : temporalCircles) {
            double c_radius = Math.max(AppConfig.NUMBERING_CIRCLE_RADIUS_MIN, c.getLineWidth() * AppConfig.NUMBERING_CIRCLE_RADIUS_MULTIPLIER);
            double dist = Math.hypot(previewX_orig - c.getCenter().getX(), previewY_orig - c.getCenter().getY());
            if (dist < radius + c_radius) {
                return true;
            }
        }
        return false;
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
        manager.notifySubModeCancelled(); // stamp BEFORE clearing flags (race guard for GlobalKeyHook)
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

    private void captureFull(CropAction action) {
        if (isNumberingModeActive) {
            gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        }

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = getScene().getRoot().snapshot(params, null);

        if (isNumberingModeActive) {
            java.awt.Point cursor = java.awt.MouseInfo.getPointerInfo().getLocation();
            double mouseX = cursor.x - getX();
            double mouseY = cursor.y - getY();
            drawNumberPreview(mouseX, mouseY);
        }

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

    private void enterCropMode(CropAction action) {
        isCropModeActive = true;
        pendingCropAction = action;

        double canvasW = canvasTemporal.getWidth();
        double canvasH = canvasTemporal.getHeight();
        cropW = canvasW / 4.0;
        cropH = canvasH / 4.0;
        cropX = (canvasW - cropW) / 2.0;
        cropY = (canvasH - cropH) / 2.0;

        getScene().setCursor(javafx.scene.Cursor.DEFAULT);
        startMarchingAnts();
        renderCropOverlay();
    }

    private void startMarchingAnts() {
        if (marchingAntsTimeline != null) {
            marchingAntsTimeline.stop();
        }
        marchingAntsTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(100),
                e -> {
                    dashOffset = (dashOffset + 2.0) % 16.0;
                    if (isCropModeActive) {
                        renderCropOverlay();
                    }
                }
            )
        );
        marchingAntsTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        marchingAntsTimeline.play();
    }

    private void stopMarchingAnts() {
        if (marchingAntsTimeline != null) {
            marchingAntsTimeline.stop();
            marchingAntsTimeline = null;
        }
    }

    private void renderCropOverlay() {
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

    private void exitCropMode(boolean cancelled) {
        isCropModeActive = false;
        stopMarchingAnts();

        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        getScene().setCursor(pencilCursor);

        if (cancelled) {
            manager.notifySubModeCancelled();
        }
    }

    private void confirmCrop() {
        double x = cropX;
        double y = cropY;
        double w = cropW;
        double h = cropH;
        CropAction action = pendingCropAction;

        exitCropMode(false);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(x, y, w, h));

        WritableImage image = getScene().getRoot().snapshot(params, null);

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
            if (isCropModeActive) {
                renderCropOverlay();
            }
        });
        pause.play();
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

    private java.io.File showSaveDialog() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String defaultName = "anotacion_" + now.format(formatter) + ".png";

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Guardar captura de anotación");
        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Imagen PNG (*.png)", "*.png"));

        return fileChooser.showSaveDialog(this);
    }

}