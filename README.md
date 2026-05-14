# ZoomDraw — Sprint 1

Herramienta de anotaciones sobre captura de pantalla, compatible con Linux y Windows.

## Requisitos

| Herramienta | Versión mínima |
|-------------|----------------|
| JDK         | 21             |
| Maven       | 3.9+           |

> **Linux / Wayland**: JNativeHook requiere X11 para el hook global de teclado.
> Si tu sesión es Wayland pura, arranca la aplicación en modo XWayland:
> ```
> DISPLAY=:0 mvn javafx:run
> ```
> o inicia la sesión de escritorio en modo X11.

> **Linux / permisos de input**: En algunas distribuciones JNativeHook necesita
> pertenecer al grupo `input` o ejecutarse con `sudo` la primera vez.
> ```
> sudo usermod -aG input $USER   # añadir tu usuario al grupo input
> ```
> Cierra sesión y vuelve a entrar para que el cambio tenga efecto.

---

## Estructura del proyecto

```
zoomdraw/
├── pom.xml
└── src/main/java/com/zoomdraw/
    ├── Launcher.java          ← punto de entrada JVM
    ├── App.java               ← Application JavaFX + gestión de estados
    ├── AppState.java          ← enum LISTENING / ANNOTATION_ACTIVE
    ├── GlobalKeyListener.java ← hook global (JNativeHook) → detecta CTRL+1
    └── AnnotationWindow.java  ← ventana fullscreen a pantalla completa
```

---

## Compilar y ejecutar

```bash
# Compilar
mvn compile

# Ejecutar en desarrollo (gestiona el module-path de JavaFX automáticamente)
mvn javafx:run

# Generar fat-JAR (target/zoomdraw-1.0-SNAPSHOT-fat.jar)
mvn package
java -jar target/zoomdraw-1.0-SNAPSHOT-fat.jar
```

---

## Uso (Sprint 1)

| Acción        | Efecto                                      |
|---------------|---------------------------------------------|
| `CTRL + 1`    | Abre la ventana de anotación (fondo negro)  |
| `ESC`         | Cierra la ventana y vuelve al modo escucha  |

La aplicación no muestra ninguna ventana al arrancar.
Los mensajes de estado se imprimen en la consola (stdout).

---

## Autoarranque con el sistema (fuera de Sprint 1)

### Linux (systemd --user)
Crea `~/.config/systemd/user/zoomdraw.service`:
```ini
[Unit]
Description=ZoomDraw annotation tool

[Service]
ExecStart=/usr/bin/java -jar /ruta/a/zoomdraw-fat.jar
Restart=on-failure
Environment=DISPLAY=:0

[Install]
WantedBy=default.target
```
```bash
systemctl --user enable --now zoomdraw.service
```

### Windows (Registro)
Añade al registro:
```
HKCU\Software\Microsoft\Windows\CurrentVersion\Run
Nombre: ZoomDraw
Valor:  "C:\Program Files\Java\jdk-21\bin\java.exe" -jar "C:\ruta\zoomdraw-fat.jar"
```

---

## Backlog / sprints futuros

- **Sprint 2** — Captura real de pantalla como fondo de la ventana de anotación.
- **Sprint 3** — Trazado libre con ratón (color rojo por defecto, grosor 3).
- **Sprint 4** — Formas básicas (rectángulo, círculo, elipse, flecha, línea recta).
- **Sprint 5** — Formas con relleno semitransparente.
- **Sprint 6** — Modo texto (CTRL+T).
- **Sprint 7** — Deshacer / rehacer (CTRL+Z / CTRL+Y).
- **Sprint 8** — Teclas de propósito general (CTRL+D, CTRL+K, CTRL+W, CTRL+2).
- **Sprint 9** — Zoom y guardado de captura.
