package es.elprofesoremilio.zoomdraw.input;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import java.lang.reflect.Field;
import java.util.Objects;

public class Hotkey {
    private final boolean ctrl;
    private final boolean alt;
    private final boolean shift;
    private final boolean win;
    private final int keyCode;
    private final String rawString;

    public Hotkey(boolean ctrl, boolean alt, boolean shift, boolean win, int keyCode, String rawString) {
        this.ctrl = ctrl;
        this.alt = alt;
        this.shift = shift;
        this.win = win;
        this.keyCode = keyCode;
        this.rawString = rawString;
    }

    public boolean isCtrl() { return ctrl; }
    public boolean isAlt() { return alt; }
    public boolean isShift() { return shift; }
    public boolean isWin() { return win; }
    public int getKeyCode() { return keyCode; }
    public String getRawString() { return rawString; }

    /**
     * Parses a string representation of a hotkey (e.g. "Win+F2", "Ctrl+Shift+A").
     */
    public static Hotkey parse(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("Hotkey string cannot be empty");
        }
        
        String[] parts = str.split("\\+");
        boolean ctrl = false;
        boolean alt = false;
        boolean shift = false;
        boolean win = false;
        int keyCode = 0;
        String keyPart = "";

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim().toLowerCase();
            if (i < parts.length - 1) {
                // Modifiers
                switch (part) {
                    case "ctrl":
                    case "control":
                        ctrl = true;
                        break;
                    case "alt":
                        alt = true;
                        break;
                    case "shift":
                        shift = true;
                        break;
                    case "win":
                    case "super":
                    case "meta":
                    case "cmd":
                    case "command":
                        win = true;
                        break;
                    default:
                        break;
                }
            } else {
                // The last part is the main key
                keyPart = parts[i].trim();
                keyCode = parseKeyCode(keyPart);
            }
        }

        return new Hotkey(ctrl, alt, shift, win, keyCode, str);
    }

    private static int parseKeyCode(String keyStr) {
        String keyUpper = keyStr.toUpperCase();
        try {
            Field field = NativeKeyEvent.class.getField("VC_" + keyUpper);
            return field.getInt(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown key: " + keyStr, e);
        }
    }

    /**
     * Checks if this hotkey matches the given NativeKeyEvent.
     */
    public boolean matches(NativeKeyEvent e) {
        if (e.getKeyCode() != keyCode) {
            return false;
        }
        int modifiers = e.getModifiers();
        boolean ctrlDown = (modifiers & NativeKeyEvent.CTRL_MASK) != 0;
        boolean altDown = (modifiers & NativeKeyEvent.ALT_MASK) != 0;
        boolean shiftDown = (modifiers & NativeKeyEvent.SHIFT_MASK) != 0;
        boolean winDown = (modifiers & NativeKeyEvent.META_MASK) != 0;

        return (ctrl == ctrlDown) && (alt == altDown) && (shift == shiftDown) && (win == winDown);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (ctrl) sb.append("Ctrl+");
        if (alt) sb.append("Alt+");
        if (shift) sb.append("Shift+");
        if (win) sb.append("Win+");
        
        String keyName = rawString;
        int lastPlus = rawString.lastIndexOf('+');
        if (lastPlus >= 0 && lastPlus < rawString.length() - 1) {
            keyName = rawString.substring(lastPlus + 1).trim();
        }
        sb.append(keyName);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hotkey hotkey = (Hotkey) o;
        return ctrl == hotkey.ctrl && alt == hotkey.alt && shift == hotkey.shift && win == hotkey.win && keyCode == hotkey.keyCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctrl, alt, shift, win, keyCode);
    }
}
