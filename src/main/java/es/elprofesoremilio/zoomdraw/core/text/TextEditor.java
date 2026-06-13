package es.elprofesoremilio.zoomdraw.core.text;

public class TextEditor {
    private final StringBuilder content;
    private int cursorPos;
    private int selectionAnchor; // -1 = no selection

    public TextEditor(String initialContent, int initialCursorPos) {
        this.content = new StringBuilder(initialContent);
        this.cursorPos = Math.max(0, Math.min(initialCursorPos, initialContent.length()));
        this.selectionAnchor = -1;
    }

    public String getContent() { return content.toString(); }
    public int getCursorPos() { return cursorPos; }

    public boolean hasSelection() {
        return selectionAnchor != -1 && selectionAnchor != cursorPos;
    }

    public int getSelectionStart() { return Math.min(selectionAnchor, cursorPos); }
    public int getSelectionEnd()   { return Math.max(selectionAnchor, cursorPos); }

    public String getSelectedText() {
        if (!hasSelection()) return "";
        return content.substring(getSelectionStart(), getSelectionEnd());
    }

    public void insert(String text) {
        if (hasSelection()) deleteSelection();
        content.insert(cursorPos, text);
        cursorPos += text.length();
        selectionAnchor = -1;
    }

    public void deleteBackward() {
        if (hasSelection()) { deleteSelection(); return; }
        if (cursorPos > 0) { content.deleteCharAt(cursorPos - 1); cursorPos--; }
    }

    public void deleteForward() {
        if (hasSelection()) { deleteSelection(); return; }
        if (cursorPos < content.length()) content.deleteCharAt(cursorPos);
    }

    private void deleteSelection() {
        int start = getSelectionStart();
        int end   = getSelectionEnd();
        content.delete(start, end);
        cursorPos = start;
        selectionAnchor = -1;
    }

    public void moveCursorLeft(boolean extend) {
        if (!extend && hasSelection()) { cursorPos = getSelectionStart(); selectionAnchor = -1; return; }
        prepareSelection(extend);
        if (cursorPos > 0) cursorPos--;
    }

    public void moveCursorRight(boolean extend) {
        if (!extend && hasSelection()) { cursorPos = getSelectionEnd(); selectionAnchor = -1; return; }
        prepareSelection(extend);
        if (cursorPos < content.length()) cursorPos++;
    }

    public void moveCursorUp(boolean extend) {
        prepareSelection(extend);
        int lineStart = getLineStart(cursorPos);
        int col = cursorPos - lineStart;
        if (lineStart == 0) { cursorPos = 0; return; }
        int prevLineEnd   = lineStart - 1;
        int prevLineStart = getLineStart(prevLineEnd);
        cursorPos = prevLineStart + Math.min(col, prevLineEnd - prevLineStart);
    }

    public void moveCursorDown(boolean extend) {
        prepareSelection(extend);
        int lineStart = getLineStart(cursorPos);
        int col       = cursorPos - lineStart;
        int lineEnd   = getLineEnd(cursorPos);
        if (lineEnd >= content.length()) { cursorPos = content.length(); return; }
        int nextLineStart = lineEnd + 1;
        int nextLineEnd   = getLineEnd(nextLineStart);
        cursorPos = nextLineStart + Math.min(col, nextLineEnd - nextLineStart);
    }

    public void moveCursorHome(boolean extend) {
        prepareSelection(extend);
        cursorPos = getLineStart(cursorPos);
    }

    public void moveCursorEnd(boolean extend) {
        prepareSelection(extend);
        cursorPos = getLineEnd(cursorPos);
    }

    public void moveWordLeft(boolean extend) {
        prepareSelection(extend);
        int pos = cursorPos;
        while (pos > 0 && isWordSep(content.charAt(pos - 1))) pos--;
        while (pos > 0 && !isWordSep(content.charAt(pos - 1))) pos--;
        cursorPos = pos;
    }

    public void moveWordRight(boolean extend) {
        prepareSelection(extend);
        int pos = cursorPos;
        int len = content.length();
        while (pos < len && isWordSep(content.charAt(pos))) pos++;
        while (pos < len && !isWordSep(content.charAt(pos))) pos++;
        cursorPos = pos;
    }

    public void setCursorToPos(int pos, boolean extend) {
        prepareSelection(extend);
        cursorPos = Math.max(0, Math.min(pos, content.length()));
    }

    public int getLineStart(int pos) {
        int i = pos;
        while (i > 0 && content.charAt(i - 1) != '\n') i--;
        return i;
    }

    public int getLineEnd(int pos) {
        int i = pos;
        int len = content.length();
        while (i < len && content.charAt(i) != '\n') i++;
        return i;
    }

    public int getLeadingTabCount(int lineStart) {
        int count = 0;
        int len = content.length();
        while (lineStart + count < len && content.charAt(lineStart + count) == '\t') count++;
        return count;
    }

    private void prepareSelection(boolean extend) {
        if (extend) { if (selectionAnchor == -1) selectionAnchor = cursorPos; }
        else selectionAnchor = -1;
    }

    private boolean isWordSep(char c) {
        return !Character.isLetterOrDigit(c) && c != '_';
    }
}
