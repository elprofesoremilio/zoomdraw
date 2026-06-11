# ZoomDraw

Herramienta de anotación de pantalla para presentaciones y clases. Vive en la bandeja del sistema y se activa con un atajo global de teclado. Construida con JavaFX 21.

---

## Tecnologías

- **Java 21 + JavaFX 21** — UI y canvas de dibujo
- **JNativeHook** — atajos globales de teclado/ratón (fuera del foco de JavaFX)
- **Dorkbox SystemTray** — bandeja del sistema en Linux; `java.awt.SystemTray` en Windows
- **Launch4j** (solo Windows) — empaqueta el JAR como `.exe`
- **jpackage** (solo Linux) — produce `.deb`
- **Maven** — build y ejecución en desarrollo

---

## Comandos

```bash
# Desarrollo
mvn javafx:run

# Empaquetar (produce .exe en Windows, .deb en Linux)
mvn package
```

### VM options requeridas (IDE)

**Windows:**
```
--enable-native-access=ALL-UNNAMED
--add-reads=dorkbox.utilities=java.desktop
--add-reads=dorkbox.systemtray=java.desktop
--add-opens=java.desktop/java.awt=ALL-UNNAMED
--add-opens=java.desktop/sun.awt.windows=ALL-UNNAMED
```

**Linux** (omitir los dos `--add-opens`):
```
--enable-native-access=ALL-UNNAMED
--add-reads=dorkbox.utilities=java.desktop
--add-reads=dorkbox.systemtray=java.desktop
```

En Linux también es necesaria la librería `libayatana-appindicator3-1`:
```bash
sudo apt update && sudo apt install -y libayatana-appindicator3-1
```

El `pom.xml` inyecta estas opciones automáticamente para `mvn javafx:run`.

---

## Arquitectura y mapa de clases

### Flujo de arranque

```
Main
 └── adquiere file-lock (guarda instancia única)
      └── Application.launch(AppLauncher)
           ├── oculta primary stage, deshabilita implicit exit
           ├── instancia AnnotationManager (mediador central)
           ├── registra GlobalKeyHook (JNativeHook)
           └── configura bandeja del sistema
                ├── Windows → java.awt.SystemTray
                └── Linux   → Dorkbox SystemTray
```

### Mediador central: `AnnotationManager`

`core/AnnotationManager.java` — es el único objeto al que tanto `GlobalKeyHook` (hilo JNativeHook) como `AnnotationStage` (hilo JavaFX) acceden. Toda mutación de estado JavaFX pasa por `Platform.runLater`.

Gestiona tres modos mutuamente excluyentes:
- **Modo anotación** — abre `AnnotationStage` en la pantalla donde está el cursor.
- **Modo puntero láser** — muestra `LaserPointerStage`, sigue el ratón nativo.
- **Ventana de ayuda** — `HelpWindow` flotante en la esquina superior derecha.

Propiedades de estado persistentes entre sesiones de anotación: color actual, grosor de línea, opacidad, y `CommandHistory`.

### Canvas doble en `AnnotationStage`

`ui/AnnotationStage.java` — stage `TRANSPARENT` que cubre el monitor objetivo. Contiene:

| Canvas | Propósito |
|---|---|
| `canvasPermanent` | Trazos confirmados + fondo capturado. Se redibuja reproduciendo `commandHistory`. |
| `canvasTemporal` | Preview del trazo en curso, overlay de recorte, preview del borrador/numeración. |

El zoom se implementa con `zoomFactor` + `offsetX/offsetY` aplicados mediante `gcPermanent.setTransform(...)` antes de cada redibujado. Todas las coordenadas almacenadas están en **espacio original (pre-zoom)**.

### Patrón Command

`commands/DrawingCommand.java` — interfaz `execute(GraphicsContext gc)`. Implementaciones:

| Clase | Qué almacena |
|---|---|
| `PathCommand` | Trazo libre (lista de `Point2D`), color, grosor |
| `ShapeCommand` | Punto inicio/fin, `DrawMode`, color, grosor, snapshot de censor opcional |
| `TextCommand` | Bloque de texto (`TextBlock` → `TextRun` → `TextTokens`), posición, estilo |
| `ClearCommand` | Marca de "borrar todo el lienzo" |
| `NumberingSessionCommand` | Lista de `NumberedCircle` como una sola unidad de undo |
| `EraseCommand` | Diff entre historia original y resultante al borrar con el borrador |

`CommandHistory` — dos pilas (undo/redo). El `AnnotationManager` posee una única instancia compartida; el historial **sobrevive** al cerrar y reabrir el overlay.

### Controladores especializados

Todos en `ui/controllers/`. Cada uno recibe canvas, `ZoomPanController` y `CommandHistory` en el constructor.

| Controlador | Responsabilidad |
|---|---|
| `InputDispatcher` | Router central de eventos de teclado y ratón; delega a los controladores en orden de prioridad de modo activo |
| `ZoomPanController` | Estado de zoom (`zoomFactor`, `offsetX`, `offsetY`) y cálculo `zoomAtCursor` |
| `DrawingController` | Lápiz libre y formas geométricas (teclas de modo + arrastrar) |
| `TextToolController` | Modo texto: posicionamiento, edición multilínea, soporte IME/acentos |
| `NumberingToolController` | Modo numeración: insertar, mover, borrar, reordenar círculos numerados |
| `EraserToolController` | Modo borrador: elimina/recorta trazos y formas en el radio del cursor |
| `CropToolController` | Modo recorte: selección rectangular para captura parcial |
| `CaptureController` | Captura completa o recortada → portapapeles o archivo PNG |

### Atajo global: `GlobalKeyHook`

`input/GlobalKeyHook.java` — listener JNativeHook; nunca toca estado JavaFX directamente. Dispara comandos (`ToggleAnnotationModeCommand`, `StopAnnotationModeCommand`) y llama a métodos de `AnnotationManager` que usan `Platform.runLater`.

**Race condition ESC en Linux:** JNativeHook dispara ESC en su propio hilo. Si el usuario pulsa ESC para cancelar un sub-modo, el handler JavaFX llama a `manager.notifySubModeCancelled()` antes de limpiar los flags. `GlobalKeyHook.nativeKeyPressed` comprueba `wasSubModeRecentlyCancelled()` (ventana de 300 ms) para no cerrar también el modo anotación completo.

### Configuración

`config/AppConfig.java` — **todas** las constantes de la aplicación: colores, teclas, rangos de grosor/opacidad, apariencia del láser, nivel de log. No hay configuración dispersa por el código.

---

## Funcionalidades implementadas

### Modos globales (activan desde cualquier estado)

| Atajo | Modo | Clase principal |
|---|---|---|
| `Ctrl+1` | Activar/desactivar modo anotación | `GlobalKeyHook` → `AnnotationManager` |
| `Ctrl+2` | Activar/desactivar puntero láser | `GlobalKeyHook` → `AnnotationManager` → `LaserPointerStage` |
| `Ctrl+0` | Mostrar/ocultar ventana de ayuda | `GlobalKeyHook` → `AnnotationManager` → `HelpWindow` |

### Dibujo libre (modo anotación)

- **Lápiz (`PathCommand`):** click y arrastrar. Coordenadas almacenadas en espacio pre-zoom; redibujado con transform.
- **Borrar lienzo completo (`E`):** inserta `ClearCommand` en el historial (undo-able).
- **Modos de forma** — mantener tecla de modo + arrastrar:

| Combinación | Forma | `DrawMode` |
|---|---|---|
| `Ctrl` + arrastrar | Línea | `LINE` |
| `Ctrl+F` + arrastrar | Flecha | `ARROW` |
| `Ctrl+R` + arrastrar | Rectángulo | `RECTANGLE` |
| `Shift+R` + arrastrar | Rectángulo relleno | `FILLED_RECTANGLE` |
| `Ctrl+E` + arrastrar | Elipse | `ELLIPSE` |
| `Shift+E` + arrastrar | Elipse rellena | `FILLED_ELLIPSE` |
| `Ctrl+Alt+E` + arrastrar | Círculo | `CIRCLE` |
| `Shift+Alt+E` + arrastrar | Círculo relleno | `FILLED_CIRCLE` |
| `Shift+C` + arrastrar | Censura (pixelado) | `CENSOR_RECTANGLE` |

El efecto de censura captura un snapshot del canvas a zoom 1× (`get1xCanvasSnapshot()`) para pixelar la región con una escala reducida. Implementado en `DrawingController` y `ShapeCommand`.

### Modo texto (`T`)

`ui/controllers/TextToolController.java`

- Pulsar `T` activa el modo. Click en el canvas posiciona el cursor de texto.
- Soporte de acentos y caracteres especiales mediante un `TextField` oculto (solución para Linux/IME).
- Texto multilínea con `TextBlock` → `TextRun` → `TextTokens`; cada bloque admite mezcla de estilos.
- Preview en tiempo real en `canvasTemporal`; al confirmar se crea un `TextCommand`.
- Grosor de línea actual escala el tamaño de fuente (`AppConfig.TEXT_SIZE_MULTIPLIER = 10.0`).

### Modo numeración automática (`N`)

`ui/controllers/NumberingToolController.java`

- Click en vacío: inserta un `NumberedCircle` con número secuencial.
- Arrastrar un círculo existente: lo mueve.
- Click en círculo: lo selecciona; `Del`/`Backspace` lo borra y reordena la secuencia.
- `Ctrl+Z`/`Y`: deshacer/rehacer movimientos y borrados temporales dentro de la sesión.
- `ESC`: consolida todos los círculos en un único `NumberingSessionCommand` (una sola acción de undo global).

### Modo borrador (`Ctrl+D`)

`ui/controllers/EraserToolController.java`

- Cursor circular visible en `canvasTemporal` (blanco en reposo, rojo al borrar).
- Radio ajustable con `Ctrl+Rueda` o teclas `+`/`-`.
- Al arrastrar, elimina trazos (`PathCommand`) que intersectan el radio, recortando los segmentos externos y guardando los fragmentos como nuevos `PathCommand`.
- Elimina `ShapeCommand` y `TextCommand` que intersectan el radio.
- En `NumberingSessionCommand` borra solo los círculos dentro del radio y reindexea los restantes.
- Al soltar el ratón, si hubo cambios, inserta un `EraseCommand` en el historial (undo-able como una sola acción).

### Modo recorte y captura

`ui/controllers/CropToolController.java` y `CaptureController.java`

| Atajo | Acción |
|---|---|
| `Ctrl+C` | Captura pantalla completa → portapapeles |
| `Ctrl+S` | Captura pantalla completa → archivo PNG (diálogo de guardar) |
| `Ctrl+Alt+C` | Activar modo recorte → captura selección → portapapeles |
| `Ctrl+Alt+S` | Activar modo recorte → captura selección → archivo PNG |

El nombre de archivo por defecto incluye fecha y hora (`anotacion_YYYYMMDD_HHmmss.png`). Al capturar se muestra un flash blanco breve en `canvasTemporal`.

### Zoom y paneo

`ui/controllers/ZoomPanController.java`

- **Rueda del ratón** (sin modificador): zoom centrado en el cursor (1× – 8×).
- Las coordenadas de todos los comandos se almacenan en espacio original; `redrawAll()` aplica el transform `setTransform(zoom, 0, 0, zoom, offsetX, offsetY)` de forma absoluta (no concatenada) para evitar acumulación en Linux/GTK.
- El zoom está deshabilitado en modos texto, numeración y recorte.

### Color del trazo

12 colores de acceso rápido (tecla de letra sin modificadores):

| Tecla | Color |
|---|---|
| R | Rojo |
| G | Verde |
| B | Azul |
| Y | Amarillo |
| O | Naranja |
| M | Magenta |
| K | Negro |
| W | Blanco |
| C | Cyan |
| P | Rosa |
| L | Gris claro |
| D | Gris oscuro |

### Grosor de línea

- Teclas `↑`/`↓`, `+`/`-`, `Ctrl+Rueda`: pasos de 2 px (rango 1–50 px).
- Teclas `1`–`0` (sin modificador): grosor absoluto (factor × `LINE_WIDTH_MULTIPLIER = 3.0`).

### Opacidad del trazo

- Teclas `←`/`→` o `Shift+Rueda`: pasos de 0.1 (rango 0.1–1.0).
- `Shift+1`–`Shift+0`: opacidad absoluta 10%–100%.

### Fondo del canvas

- `Ctrl+K`: fondo negro (toggle; null restaura la captura de pantalla).
- `Ctrl+W`: fondo blanco (toggle).
- Sin override: fondo con la captura de pantalla del monitor al activar el modo.

### Undo / Redo

- `Ctrl+Z` / `Ctrl+Y`: implementados en `CommandHistory` con dos pilas.
- El historial **persiste** entre sesiones de anotación (el mediador `AnnotationManager` es el propietario).

### Puntero láser

`ui/LaserPointerStage.java`

- Círculo con borde configurable que sigue el cursor nativo.
- Configuración en tiempo real desde la `HelpWindow` (pestaña "Puntero Láser"):
  - Color y grosor del borde
  - Opacidad del borde
  - Radio del círculo
  - Cruz central (mostrar/ocultar, color, grosor)

### Ventana de ayuda (`HelpWindow`)

`ui/HelpWindow.java`

- Stage `TRANSPARENT`, siempre on-top, arrastrable (excepto sobre controles interactivos).
- Dos pestañas: atajos de teclado y configuración del puntero láser.
- Slider de opacidad de la propia ventana (20%–100%).
- Botón "Cerrar App" como alternativa cuando la bandeja no está disponible.
- CSS externo (`/help_style.css`) para el estilo oscuro del `TabPane`.

### Single-instance guard

`Main.java` — bloqueo de archivo en el directorio temporal. Si ya hay una instancia corriendo, la nueva termina inmediatamente.

---

## Notas de plataforma

| | Windows | Linux |
|---|---|---|
| Bandeja del sistema | `java.awt.SystemTray` | Dorkbox (Dorkbox tiene NPE en Windows) |
| GTK | — | `-Djdk.gtk.version=3` |
| `--add-opens` extra | `java.desktop/sun.awt.windows` | No necesario |
| Acentos en texto | Funciona directamente | Requiere `TextField` oculto como proxy IME |
| Zoom/redraw bug | — | `setTransform` absoluto (no `save/restore`) evita acumulación de transform en GTK |
