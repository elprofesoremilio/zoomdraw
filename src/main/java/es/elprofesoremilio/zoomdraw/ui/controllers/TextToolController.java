package es.elprofesoremilio.zoomdraw.ui.controllers;

import es.elprofesoremilio.zoomdraw.commands.CommandHistory;
import es.elprofesoremilio.zoomdraw.commands.DrawingCommand;
import es.elprofesoremilio.zoomdraw.commands.TextCommand;
import es.elprofesoremilio.zoomdraw.commands.TextEditMarkerCommand;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.core.text.TextEditor;
import es.elprofesoremilio.zoomdraw.ui.AnnotationStage;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.InputMethodEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class TextToolController {

    private final AnnotationStage stage;
    private final Canvas canvasTemporal;
    private final GraphicsContext gcTemporal;
    private final Canvas canvasPermanent;
    private final GraphicsContext gcPermanent;
    private final ZoomPanController zoomPanController;
    private final CommandHistory commandHistory;
    private final AnnotationManager manager;
    private final TextField hiddenTextField;

    // Mode flags
    private boolean isTextModeActive = false;
    private boolean isEditing = false;

    // Current editing session
    private TextEditor editor = null;
    private TextCommand editingCommand = null;   // non-null when editing an existing block
    private String contentBeforeEdit  = null;    // original content for cancel
    private Point2D editStartPosition = null;
    private double  editFontSize      = 12.0;
    private Color   editColor         = Color.RED;

    // Cursor blinking
    private boolean  cursorVisible = true;
    private Timeline blinkTimeline = null;

    public TextToolController(AnnotationStage stage, Canvas canvasPermanent, Canvas canvasTemporal,
                              TextField hiddenTextField, ZoomPanController zoomPanController,
                              CommandHistory commandHistory, AnnotationManager manager) {
        this.stage             = stage;
        this.canvasPermanent   = canvasPermanent;
        this.gcPermanent       = canvasPermanent.getGraphicsContext2D();
        this.canvasTemporal    = canvasTemporal;
        this.gcTemporal        = canvasTemporal.getGraphicsContext2D();
        this.hiddenTextField   = hiddenTextField;
        this.zoomPanController = zoomPanController;
        this.commandHistory    = commandHistory;
        this.manager           = manager;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public boolean isActive()  { return isTextModeActive; }
    public boolean isTyping()  { return isEditing; }   // kept for InputDispatcher compat
    public boolean isEditing() { return isEditing; }

    public void enterTextMode() {
        isTextModeActive = true;
        startBlink();
        stage.getScene().setCursor(javafx.scene.Cursor.TEXT);
        hiddenTextField.requestFocus();
    }

    /** Commits any active edit and exits text mode (used by T-toggle). */
    public void exitTextMode() {
        if (isEditing) {
            finishEdit();
        } else {
            isTextModeActive = false;
            stopBlink();
            clearTemporal();
            stage.getScene().setCursor(stage.getPencilCursor());
            if (stage.getScene() != null) stage.getScene().getRoot().requestFocus();
        }
    }

    /** Discards the active edit and exits text mode entirely (used by ESC). */
    public void cancelEdit() {
        manager.notifySubModeCancelled();
        if (isEditing && editingCommand != null) {
            editingCommand.setHiddenForEditing(false);
            stage.redrawAll();
        }
        editor              = null;
        editingCommand      = null;
        contentBeforeEdit   = null;
        editStartPosition   = null;
        isEditing           = false;
        isTextModeActive    = false;
        stopBlink();
        clearTemporal();
        stage.getScene().setCursor(stage.getPencilCursor());
        if (stage.getScene() != null) stage.getScene().getRoot().requestFocus();
    }

    // -------------------------------------------------------------------------
    // Mouse
    // -------------------------------------------------------------------------

    public void handleMousePressed(MouseEvent event) {
        double origX = toOrigX(event.getX());
        double origY = toOrigY(event.getY());

        if (event.getButton() == MouseButton.SECONDARY) {
            if (isEditing) finishEdit();
            return;
        }

        if (event.getButton() != MouseButton.PRIMARY) return;

        if (isEditing) {
            Bounds editBounds = computeEditBounds();
            if (editBounds != null && editBounds.contains(origX, origY)) {
                // Reposition cursor inside active block
                int pos = findCharPosForClick(origX, origY);
                editor.setCursorToPos(pos, false);
                resetBlink();
                redrawTextTemporal();
            }
            // Left-click outside the editing area does nothing while editing
        } else {
            handlePrimaryClick(origX, origY);
        }
    }

    private void handlePrimaryClick(double origX, double origY) {
        // Check if click lands on an existing committed TextCommand
        for (DrawingCommand cmd : commandHistory.getHistory()) {
            if (cmd instanceof TextCommand) {
                TextCommand tc = (TextCommand) cmd;
                if (!tc.isHiddenForEditing()) {
                    Bounds b = tc.getBounds();
                    if (b != null && b.contains(origX, origY)) {
                        startEditExisting(tc);
                        return;
                    }
                }
            }
        }
        startNewBlock(origX, origY);
    }

    // -------------------------------------------------------------------------
    // Keyboard
    // -------------------------------------------------------------------------

    public void handleKeyPressedFilter(KeyEvent event) {
        boolean isCtrl  = event.isControlDown();
        boolean isShift = event.isShiftDown();
        KeyCode code    = event.getCode();

        if (code == KeyCode.ESCAPE) {
            cancelEdit();
            event.consume();
            return;
        }

        if (!isEditing) {
            if (isCtrl) event.consume();
            return;
        }

        switch (code) {
            case BACK_SPACE:
                editor.deleteBackward();
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            case DELETE:
                editor.deleteForward();
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            case ENTER: {
                // Auto-indent: inherit leading tabs from current line
                int lineStart = editor.getLineStart(editor.getCursorPos());
                int tabs      = editor.getLeadingTabCount(lineStart);
                editor.insert("\n" + "\t".repeat(tabs));
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            }
            case TAB:
                editor.insert("\t");
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            case LEFT:
                if (isCtrl) editor.moveWordLeft(isShift);
                else        editor.moveCursorLeft(isShift);
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            case RIGHT:
                if (isCtrl) editor.moveWordRight(isShift);
                else        editor.moveCursorRight(isShift);
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            case UP:
                editor.moveCursorUp(isShift);
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            case DOWN:
                editor.moveCursorDown(isShift);
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            case HOME:
                editor.moveCursorHome(isShift);
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            case END:
                editor.moveCursorEnd(isShift);
                resetBlink();
                redrawTextTemporal();
                event.consume();
                return;
            default:
                break;
        }

        if (isCtrl) {
            // AltGr on Windows fires as Ctrl+Alt. Let it through so characters like {}[]
            // are produced by handleKeyTyped instead of being swallowed here.
            if (event.isAltDown()) return;

            switch (code) {
                case C:
                    if (editor.hasSelection()) copyToClipboard(editor.getSelectedText());
                    event.consume();
                    return;
                case X:
                    if (editor.hasSelection()) {
                        copyToClipboard(editor.getSelectedText());
                        editor.deleteBackward();
                        resetBlink();
                        redrawTextTemporal();
                    }
                    event.consume();
                    return;
                case V: {
                    Clipboard cb = Clipboard.getSystemClipboard();
                    if (cb.hasString()) {
                        editor.insert(cb.getString());
                        resetBlink();
                        redrawTextTemporal();
                    }
                    event.consume();
                    return;
                }
                default:
                    event.consume();
                    return;
            }
        }
    }

    public void handleKeyTyped(KeyEvent event) {
        // isControlDown + isAltDown = AltGr on Windows; allow those characters through.
        if ((event.isControlDown() && !event.isAltDown()) || event.isMetaDown()) { event.consume(); return; }
        if (isEditing) {
            String ch = event.getCharacter();
            if (!ch.isEmpty() && !Character.isISOControl(ch.charAt(0))) {
                editor.insert(ch);
                resetBlink();
                redrawTextTemporal();
            }
            event.consume();
        } else {
            event.consume();
        }
    }

    public void handleInputMethodTextChanged(InputMethodEvent event) {
        if (isEditing) {
            String committed = event.getCommitted();
            if (committed != null && !committed.isEmpty()) {
                editor.insert(committed);
                resetBlink();
                redrawTextTemporal();
            }
            event.consume();
        }
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    public void redrawTextTemporal() {
        clearTemporal();
        if (!isEditing || editor == null || editStartPosition == null) return;
        gcTemporal.save();
        zoomPanController.applyTransform(gcTemporal);
        renderEditing(gcTemporal);
        gcTemporal.restore();
    }

    private void renderEditing(GraphicsContext gc) {
        String content    = editor.getContent();
        double startX     = editStartPosition.getX();
        double startY     = editStartPosition.getY();
        Font   font       = Font.font(editFontSize);
        double lineHeight = editFontSize * 1.2;

        // Tab width
        Text tabMeasure = new Text(" ".repeat(AppConfig.TEXT_TAB_SPACES));
        tabMeasure.setFont(font);
        double tabWidth = tabMeasure.getLayoutBounds().getWidth();

        int cursorPos = editor.getCursorPos();
        int selStart  = editor.hasSelection() ? editor.getSelectionStart() : -1;
        int selEnd    = editor.hasSelection() ? editor.getSelectionEnd()   : -1;

        double x = startX, y = startY;
        double cursorX = x, cursorY = y;
        double maxX = startX + 4;  // ensure minimum border width

        gc.setFont(font);

        for (int i = 0; i <= content.length(); i++) {
            if (i == cursorPos) { cursorX = x; cursorY = y; }

            if (i == content.length()) break;

            char c = content.charAt(i);
            if (c == '\n') {
                if (selStart != -1 && i >= selStart && i < selEnd) {
                    gc.setFill(AppConfig.TEXT_SELECTION_COLOR);
                    gc.fillRect(x, y, 5, lineHeight);
                }
                x = startX;
                y += lineHeight;
            } else if (c == '\t') {
                if (selStart != -1 && i >= selStart && i < selEnd) {
                    gc.setFill(AppConfig.TEXT_SELECTION_COLOR);
                    gc.fillRect(x, y, tabWidth, lineHeight);
                }
                x += tabWidth;
                maxX = Math.max(maxX, x);
            } else {
                String s = String.valueOf(c);
                Text node = new Text(s);
                node.setFont(font);
                double w  = node.getLayoutBounds().getWidth();
                double h  = node.getLayoutBounds().getHeight();
                if (selStart != -1 && i >= selStart && i < selEnd) {
                    gc.setFill(AppConfig.TEXT_SELECTION_COLOR);
                    gc.fillRect(x, y, w, lineHeight);
                }
                gc.setFill(editColor);
                gc.fillText(s, x, y + h * 0.8);
                x   += w;
                maxX = Math.max(maxX, x);
            }
        }

        // Cursor
        if (cursorVisible) {
            gc.setStroke(editColor);
            gc.setLineWidth(1.5);
            gc.strokeLine(cursorX, cursorY, cursorX, cursorY + lineHeight);
        }

        // Editing border (dashed blue rectangle)
        double pad     = 4;
        double borderX = startX - pad;
        double borderY = startY - pad;
        double borderW = Math.max(maxX - startX, 16) + pad * 2;
        double borderH = (y - startY) + lineHeight + pad * 2;
        gc.setStroke(AppConfig.TEXT_EDIT_BORDER_COLOR);
        gc.setLineWidth(1.0);
        gc.setLineDashes(5.0, 4.0);
        gc.strokeRect(borderX, borderY, borderW, borderH);
        gc.setLineDashes();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void startNewBlock(double origX, double origY) {
        editingCommand    = null;
        contentBeforeEdit = null;
        editStartPosition = new Point2D(origX, origY);
        editFontSize      = Math.max(12.0, manager.getCurrentLineWidth() * AppConfig.TEXT_SIZE_MULTIPLIER);
        editColor         = manager.getCurrentColor();
        editor            = new TextEditor("", 0);
        isEditing         = true;
        stage.getScene().setCursor(javafx.scene.Cursor.TEXT);
        hiddenTextField.requestFocus();
        resetBlink();
        redrawTextTemporal();
    }

    private void startEditExisting(TextCommand tc) {
        editingCommand    = tc;
        contentBeforeEdit = tc.getFlatContent();
        editStartPosition = tc.getStartPosition();
        editFontSize      = tc.getBaseSize();
        editColor         = tc.getBaseColor();
        editor            = new TextEditor(contentBeforeEdit, contentBeforeEdit.length());
        tc.setHiddenForEditing(true);
        stage.redrawAll();   // remove the original from permanent canvas
        isEditing = true;
        stage.getScene().setCursor(javafx.scene.Cursor.TEXT);
        hiddenTextField.requestFocus();
        resetBlink();
        redrawTextTemporal();
    }

    private void finishEdit() {
        if (!isEditing) return;

        String finalContent = editor != null ? editor.getContent() : "";

        if (editingCommand != null) {
            // Editing an existing block
            editingCommand.setHiddenForEditing(false);
            if (!finalContent.equals(contentBeforeEdit)) {
                String before = contentBeforeEdit;
                editingCommand.setFlatContent(finalContent);
                commandHistory.execute(
                        new TextEditMarkerCommand(editingCommand, before, finalContent),
                        gcPermanent
                );
            }
            stage.redrawAll();
        } else {
            // New block
            if (!finalContent.isEmpty()) {
                TextCommand newCmd = TextCommand.createFromContent(
                        editStartPosition, editColor, editFontSize, finalContent);
                gcPermanent.save();
                zoomPanController.applyTransform(gcPermanent);
                commandHistory.execute(newCmd, gcPermanent);
                gcPermanent.restore();
                stage.redrawAll();
            }
        }

        editor              = null;
        editingCommand      = null;
        contentBeforeEdit   = null;
        editStartPosition   = null;
        isEditing           = false;
        isTextModeActive    = false;
        stopBlink();
        clearTemporal();
        stage.getScene().setCursor(stage.getPencilCursor());
        if (stage.getScene() != null) stage.getScene().getRoot().requestFocus();
    }

    /** Computes the bounding box (original space) of the text currently in the editor. */
    private Bounds computeEditBounds() {
        if (editor == null || editStartPosition == null) return null;
        String content    = editor.getContent();
        double startX     = editStartPosition.getX();
        double startY     = editStartPosition.getY();
        Font   font       = Font.font(editFontSize);
        double lineHeight = editFontSize * 1.2;
        Text   tabMeasure = new Text(" ".repeat(AppConfig.TEXT_TAB_SPACES));
        tabMeasure.setFont(font);
        double tabWidth = tabMeasure.getLayoutBounds().getWidth();

        double x = startX, y = startY;
        double maxX = startX + 16, maxY = startY + lineHeight;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\n') {
                x = startX; y += lineHeight;
                maxY = Math.max(maxY, y + lineHeight);
            } else if (c == '\t') {
                x   += tabWidth;
                maxX = Math.max(maxX, x);
            } else {
                Text node = new Text(String.valueOf(c));
                node.setFont(font);
                x   += node.getLayoutBounds().getWidth();
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y + lineHeight);
            }
        }

        double pad = 6;
        return new BoundingBox(startX - pad, startY - pad,
                maxX - startX + pad * 2, maxY - startY + pad * 2);
    }

    /** Returns the character index (in editor content) closest to the given original-space click. */
    private int findCharPosForClick(double clickX, double clickY) {
        if (editor == null || editStartPosition == null) return 0;
        String content    = editor.getContent();
        double startX     = editStartPosition.getX();
        Font   font       = Font.font(editFontSize);
        double lineHeight = editFontSize * 1.2;
        Text   tabMeasure = new Text(" ".repeat(AppConfig.TEXT_TAB_SPACES));
        tabMeasure.setFont(font);
        double tabWidth = tabMeasure.getLayoutBounds().getWidth();

        double x = startX, y = editStartPosition.getY();
        int    bestPos  = 0;
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i <= content.length(); i++) {
            double dist = Math.hypot(clickX - x, clickY - (y + lineHeight * 0.5));
            if (dist < bestDist) { bestDist = dist; bestPos = i; }

            if (i < content.length()) {
                char c = content.charAt(i);
                if (c == '\n')      { x = startX; y += lineHeight; }
                else if (c == '\t') { x += tabWidth; }
                else {
                    Text node = new Text(String.valueOf(c));
                    node.setFont(font);
                    x += node.getLayoutBounds().getWidth();
                }
            }
        }
        return bestPos;
    }

    private void startBlink() {
        if (blinkTimeline == null) {
            blinkTimeline = new Timeline(new KeyFrame(
                    Duration.millis(AppConfig.TEXT_CURSOR_BLINK_MS),
                    e -> { cursorVisible = !cursorVisible; if (isEditing) redrawTextTemporal(); }
            ));
            blinkTimeline.setCycleCount(Timeline.INDEFINITE);
        }
        cursorVisible = true;
        blinkTimeline.stop();
        blinkTimeline.play();
    }

    private void stopBlink() {
        if (blinkTimeline != null) blinkTimeline.stop();
        cursorVisible = true;
    }

    private void resetBlink() {
        cursorVisible = true;
        if (blinkTimeline != null) { blinkTimeline.stop(); blinkTimeline.play(); }
    }

    private void clearTemporal() {
        gcTemporal.clearRect(0, 0, canvasTemporal.getWidth(), canvasTemporal.getHeight());
    }

    private static void copyToClipboard(String text) {
        ClipboardContent cc = new ClipboardContent();
        cc.putString(text);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    /**
     * Updates the scene cursor based on mouse position within the annotation stage.
     * Called from InputDispatcher on every mouse-moved and mouse-dragged event.
     * - While editing: TEXT cursor over the active editing block, X cursor elsewhere.
     * - While waiting (text mode active, not editing): TEXT cursor everywhere.
     */
    public void handleMouseMoved(double screenX, double screenY) {
        if (!isTextModeActive) return;

        if (isEditing) {
            double origX = toOrigX(screenX);
            double origY = toOrigY(screenY);
            Bounds editBounds = computeEditBounds();
            boolean overEdit = editBounds != null && editBounds.contains(origX, origY);
            stage.getScene().setCursor(overEdit ? javafx.scene.Cursor.TEXT : stage.getXCursor());
        } else {
            stage.getScene().setCursor(javafx.scene.Cursor.TEXT);
        }
    }

    private double toOrigX(double screenX) {
        return (screenX - zoomPanController.getOffsetX()) / zoomPanController.getZoomFactor();
    }

    private double toOrigY(double screenY) {
        return (screenY - zoomPanController.getOffsetY()) / zoomPanController.getZoomFactor();
    }
}
