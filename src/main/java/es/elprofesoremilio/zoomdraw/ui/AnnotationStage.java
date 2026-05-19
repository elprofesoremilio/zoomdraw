package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.commands.*;
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

import java.util.ArrayList;
import java.util.List;

public class AnnotationStage extends Stage implements BrushSettingsUpdater {

    private final AnnotationManager manager;

    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;

    private final List<Point2D> currentStrokePoints = new ArrayList<>();
    private final CommandHistory commandHistory = new CommandHistory();
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

    public boolean isTextModeActive() {
        return isTextModeActive;
    }

    public AnnotationStage(AnnotationManager manager, Rectangle2D bounds, WritableImage background) {
        super(StageStyle.TRANSPARENT);
        this.manager = manager;
        this.background = background;

        // Initialize permanent canvas
        canvasPermanent = new Canvas(bounds.getWidth(), bounds.getHeight());
        gcPermanent = canvasPermanent.getGraphicsContext2D();
        gcPermanent.drawImage(this.background, 0, 0); // Draw background once

        // Initialize temporal canvas
        canvasTemporal = new Canvas(bounds.getWidth(), bounds.getHeight());
        gcTemporal = canvasTemporal.getGraphicsContext2D();

        // Set initial brush settings for both GCs
        updateBrushSettings();

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
                    scene.setCursor(javafx.scene.Cursor.DEFAULT);
                    activeShapeMode = DrawMode.PENCIL;
                }
                event.consume();
                return;
            }

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
                } else if (!event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
                    // Prevenir que otras teclas se procesen como atajos mientras se escribe
                }
            } else if (isTextModeActive) {
                if (event.getCode() == KeyCode.ESCAPE) {
                    isTextModeActive = false;
                    activeShapeMode = DrawMode.PENCIL;
                    scene.setCursor(javafx.scene.Cursor.DEFAULT);
                    event.consume();
                } else if (!event.isControlDown() && !event.isAltDown() && !event.isMetaDown()) {
                    // En modo texto pero sin escribir, consumimos las teclas de colores y formas
                    // para que no hagan nada
                    if (event.getCode().isLetterKey() || event.getCode().isDigitKey()) {
                        event.consume();
                    }
                }
            }
        });

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_TYPED, event -> {
            if (isTyping) {
                String character = event.getCharacter();
                if (character.length() > 0 && character.charAt(0) >= 32 && character.charAt(0) != 127) {
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
                    this.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
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
                    javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
                    params.setFill(Color.TRANSPARENT);
                    currentCanvasSnapshot = canvasPermanent.snapshot(params, null);
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
            gcPermanent.drawImage(background, 0, 0);
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
        this.getScene().setCursor(javafx.scene.Cursor.DEFAULT);
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    private void redrawTextTemporal() {
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
        if (currentTextCommand != null) {
            currentTextCommand.execute(gcTemporal);
        }
    }
}