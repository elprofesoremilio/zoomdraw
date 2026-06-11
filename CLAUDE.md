# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

ZoomDraw is a JavaFX 21 screen-annotation tool that lives in the system tray. A global hotkey (Ctrl+1) opens a transparent fullscreen overlay on the current monitor where the user can draw, add text, number items, and capture screenshots. Ctrl+2 activates a laser pointer mode.

## Commands

**Run (development):**
```
mvn javafx:run
```

**Package:**
```
mvn package
```
On Windows this also produces `target/ZoomDraw.exe` via launch4j. On Linux it produces a `.deb` via jpackage.

**Required VM options when running from an IDE:**
```
--enable-native-access=ALL-UNNAMED
--add-reads=dorkbox.utilities=java.desktop
--add-reads=dorkbox.systemtray=java.desktop
--add-opens=java.desktop/java.awt=ALL-UNNAMED
--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED
```
On Linux omit the two `--add-opens` lines. The `pom.xml` injects these automatically for `mvn javafx:run`; they are also embedded in the `.exe` via the launch4j `<opts>` block.

There are no automated tests in this project.

## Architecture

### Entry point and lifecycle

`Main` → acquires a file lock (single-instance guard) → `Application.launch(AppLauncher.class)`.

`AppLauncher` (JavaFX `Application`):
- Hides the primary stage and disables implicit exit.
- Instantiates `AnnotationManager` (the central mediator).
- Registers `GlobalKeyHook` (JNativeHook global keyboard/mouse listener).
- Sets up the system tray: **AWT `SystemTray`** on Windows (Dorkbox has a NPE bug on Windows), **Dorkbox `SystemTray`** everywhere else.

### Central mediator: `AnnotationManager`

`AnnotationManager` owns all mode state and is the only object that both `GlobalKeyHook` (JNativeHook thread) and `AnnotationStage` (JavaFX thread) talk to. All JavaFX mutations are dispatched via `Platform.runLater`.

It manages three mutually exclusive modes:
- **Annotation mode** — opens `AnnotationStage` on the screen where the cursor is.
- **Laser pointer mode** — shows `LaserPointerStage`, tracks the native mouse.
- **Help window** — `HelpWindow` floats at top-right of the active screen.

#### ESC race condition
On Linux, JNativeHook fires ESC on its own thread. If the user presses ESC to cancel a sub-mode (text/numbering/crop), the JavaFX handler calls `manager.notifySubModeCancelled()` before clearing the sub-mode flags. `GlobalKeyHook.nativeKeyPressed` then checks `wasSubModeRecentlyCancelled()` (300 ms window) to avoid also closing the entire annotation mode.

### Drawing: `AnnotationStage`

A transparent `TRANSPARENT`-style `Stage` that covers the target monitor. It has two stacked JavaFX `Canvas` layers:

- **`canvasPermanent`** — committed strokes + background screenshot. Redrawn by replaying `commandHistory`.
- **`canvasTemporal`** — in-progress stroke preview, crop overlay, numbering ghost preview.

Zoom is implemented as `zoomFactor` + `offsetX/offsetY` applied via `gcPermanent.translate/scale` before every draw call. All stored coordinates are in **original (pre-zoom) space**; the transform is reapplied on every `redrawAll()`.

Input is split:
- `AnnotationInputHandler` — scroll (line width, opacity, zoom) and single-key shortcuts (colors, arrows).
- `AnnotationStage` scene event filters — shape keys (hold R/E/F/C + Ctrl/Shift while dragging), text mode, numbering mode, crop mode, Ctrl+Z/Y undo/redo.

### Command pattern

`DrawingCommand` (interface `execute(GraphicsContext gc)`) is implemented by:
- `PathCommand` — freehand pencil stroke (list of points).
- `ShapeCommand` — line, rectangle, ellipse, circle, arrow, censor-rect, and their filled variants (`DrawMode` enum).
- `TextCommand` — multi-run rich text block with `TextBlock`/`TextRun`/`TextTokens`.
- `ClearCommand` — clears and redraws the background.
- `NumberingSessionCommand` — commits a set of `NumberedCircle` objects as one undo unit.

`CommandHistory` holds two stacks (undo/redo). `AnnotationManager` owns a single `CommandHistory` shared across annotation sessions so undo history survives re-opening the overlay.

### Global hotkey: `GlobalKeyHook`

JNativeHook listener. Fires on its own thread — never touch JavaFX state directly here. Delegates to `Command` objects (`ToggleAnnotationModeCommand`, `StopAnnotationModeCommand`) and calls `manager.*` methods that internally use `Platform.runLater`.

### Configuration

All constants live in `AppConfig`. Colors, key bindings, default line width/opacity, laser pointer appearance, renderer text, and JNativeHook log level are all defined there — change them there, nowhere else.

### Platform notes

| | Windows | Linux |
|---|---|---|
| System tray | `java.awt.SystemTray` | Dorkbox `SystemTray` |
| Dorkbox required? | No (NPE in TrayPopup.doShow) | Yes |
| GTK version | — | `-Djdk.gtk.version=3` |
| Extra `--add-opens` | `java.desktop/sun.awt.windows` | Not needed |