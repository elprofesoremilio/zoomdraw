package es.elprofesoremilio.zoomdraw.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * ZoomDraw Ubuntu/Wayland Professional Integrator.
 * Handles OS-level shortcut registration autonomously and manages the annotation lifecycle.
 */
public class UbuntuWaylandIntegrator extends Application {

    private static final int PORT = 9999;
    private static final String SCRIPT_PATH = System.getProperty("user.home") + "/.zoomdraw_trigger.sh";
    private static final String GNOME_BINDING_PATH = "/org/gnome/settings-daemon/plugins/media-keys/custom-keybindings/zoomdraw/";

    private Stage annotationStage = null;
    private volatile boolean active = false;

    public static void main(String[] args) {
        Application.launch(UbuntuWaylandIntegrator.class, args);
    }

    // -----------------------------------------------------------------------
    // Application Lifecycle
    // -----------------------------------------------------------------------
    @Override
    public void start(Stage primaryStage) {
        primaryStage.hide();
        Platform.setImplicitExit(false);

        // 1. Integración profesional: Configurar el OS automáticamente
        installNativeShortcut();

        // 2. Iniciar el oyente interno
        startListenerServer();

        System.out.println("✅ ZoomDraw Integrator iniciado.");
        System.out.println("✅ Atajo CTRL+1 registrado nativamente en GNOME.");
        System.out.println("Prueba a pulsar CTRL+1 ahora mismo.");
    }

    @Override
    public void stop() {
        // Limpiar el sistema operativo al salir (Clean Exit)
        removeNativeShortcut();
        System.out.println("🛑 ZoomDraw cerrado. Sistema limpio.");
        System.exit(0);
    }

    // -----------------------------------------------------------------------
    // OS Auto-Configuration (GNOME gsettings)
    // -----------------------------------------------------------------------
    private void installNativeShortcut() {
        try {
            // 1. Crear el script disparador (usa bash y /dev/tcp, disponible en todo Ubuntu)
            File script = new File(SCRIPT_PATH);
            try (PrintWriter out = new PrintWriter(script)) {
                out.println("#!/bin/bash");
                out.println("bash -c 'echo \"TOGGLE\" > /dev/tcp/127.0.0.1/" + PORT + "' 2>/dev/null");
            }
            script.setExecutable(true);

            // 2. Configurar el atajo en GNOME
            execCmd("gsettings", "set", "org.gnome.settings-daemon.plugins.media-keys.custom-keybinding:" + GNOME_BINDING_PATH, "name", "'ZoomDraw Toggle'");
            execCmd("gsettings", "set", "org.gnome.settings-daemon.plugins.media-keys.custom-keybinding:" + GNOME_BINDING_PATH, "command", "'" + SCRIPT_PATH + "'");
            execCmd("gsettings", "set", "org.gnome.settings-daemon.plugins.media-keys.custom-keybinding:" + GNOME_BINDING_PATH, "binding", "'<Primary>1'");

            // 3. Añadirlo a la lista activa de GNOME de forma segura
            String array = execCmd("gsettings", "get", "org.gnome.settings-daemon.plugins.media-keys", "custom-keybindings").trim();
            if (!array.contains(GNOME_BINDING_PATH)) {
                if (array.equals("@as []") || array.equals("[]")) {
                    array = "['" + GNOME_BINDING_PATH + "']";
                } else {
                    array = array.replace("]", ", '" + GNOME_BINDING_PATH + "']");
                }
                execCmd("gsettings", "set", "org.gnome.settings-daemon.plugins.media-keys", "custom-keybindings", array);
            }
        } catch (Exception e) {
            System.err.println("Error instalando atajo nativo: " + e.getMessage());
        }
    }

    private void removeNativeShortcut() {
        try {
            // 1. Borrar el script
            new File(SCRIPT_PATH).delete();

            // 2. Eliminarlo de la lista activa de GNOME
            String array = execCmd("gsettings", "get", "org.gnome.settings-daemon.plugins.media-keys", "custom-keybindings").trim();
            if (array.contains("'" + GNOME_BINDING_PATH + "'")) {
                array = array.replace(", '" + GNOME_BINDING_PATH + "'", "")
                        .replace("'" + GNOME_BINDING_PATH + "', ", "")
                        .replace("'" + GNOME_BINDING_PATH + "'", "");

                if (array.equals("[]") || array.equals("['']")) array = "@as []";

                execCmd("gsettings", "set", "org.gnome.settings-daemon.plugins.media-keys", "custom-keybindings", array);
            }

            // 3. Limpiar las propiedades
            execCmd("gsettings", "reset", "org.gnome.settings-daemon.plugins.media-keys.custom-keybinding:" + GNOME_BINDING_PATH, "name");
            execCmd("gsettings", "reset", "org.gnome.settings-daemon.plugins.media-keys.custom-keybinding:" + GNOME_BINDING_PATH, "command");
            execCmd("gsettings", "reset", "org.gnome.settings-daemon.plugins.media-keys.custom-keybinding:" + GNOME_BINDING_PATH, "binding");
        } catch (Exception ignored) {}
    }

    private String execCmd(String... command) {
        try {
            Process p = new ProcessBuilder(command).start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = br.readLine();
            p.waitFor();
            return line != null ? line : "";
        } catch (Exception e) {
            return "";
        }
    }

    // -----------------------------------------------------------------------
    // Internal Server (Listener)
    // -----------------------------------------------------------------------
    private void startListenerServer() {
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    try (Socket clientSocket = serverSocket.accept();
                         BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                        String message = in.readLine();
                        if ("TOGGLE".equals(message)) {
                            Platform.runLater(this::toggleAnnotationMode);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Listener Server Error: " + e.getMessage());
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    // -----------------------------------------------------------------------
    // Annotation UI Logic
    // -----------------------------------------------------------------------
    private void toggleAnnotationMode() {
        if (active) {
            if (annotationStage != null) {
                annotationStage.close();
                annotationStage = null;
            }
            active = false;
        } else {
            showAnnotationStage();
            active = true;
        }
    }

    private void showAnnotationStage() {
        Rectangle2D bounds = Screen.getPrimary().getBounds();

        annotationStage = new Stage(StageStyle.TRANSPARENT);
        Canvas canvas = new Canvas(bounds.getWidth(), bounds.getHeight());

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(new Color(0, 0, 0, 0.4)); // Fondo semitransparente para validar la superposición
        gc.fillRect(0, 0, bounds.getWidth(), bounds.getHeight());

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 40));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("ZoomDraw (Wayland Pro)\nPulsa ESC para salir", bounds.getWidth() / 2, bounds.getHeight() / 2);

        StackPane root = new StackPane(canvas);
        root.setBackground(null);
        Scene scene = new Scene(root, bounds.getWidth(), bounds.getHeight(), Color.TRANSPARENT);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                toggleAnnotationMode();
            }
        });

        annotationStage.setScene(scene);
        annotationStage.setAlwaysOnTop(true);
        annotationStage.setX(bounds.getMinX());
        annotationStage.setY(bounds.getMinY());
        annotationStage.show();
        annotationStage.toFront();
        annotationStage.requestFocus();
    }
}