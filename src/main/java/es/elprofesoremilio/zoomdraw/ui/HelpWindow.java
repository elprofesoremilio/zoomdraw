package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HelpWindow extends Stage {
    private double xOffset = 0;
    private double yOffset = 0;

    public HelpWindow() {
        initStyle(StageStyle.TRANSPARENT);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: black; -fx-background-radius: 10;");
        
        Label title = new Label("Atajos de Teclado");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);
        
        Label shortcuts = new Label(
            "CTRL + 1: Activar/desactivar modo anotación\n" +
                    "\t(se puede desactivar con ESC también)\n\n" +
                "CTRL + 0: Mostrar/Ocultar esta ayuda\n\n"+
                    "Dibujo y Herramientas:\n" +
            "\tClick Izquierdo: Dibujar trazo libre / texto\n" +
            "\tCTRL + Z: Deshacer\n" +
            "\tCTRL + Y: Rehacer\n" +
            "\tE: Borrar todos los trazos\n" +
            "\tT: Modo Texto\n" +
            "\tESC: Cancelar forma / salir de texto / salir de aplicación\n\n" +
            "Formas Geométricas (Mantener presionado y arrastrar):\n" +
            "\tCTRL + R: Rectángulo\n" +
            "\tCTRL + E: Elipse\n" +
            "\tCTRL + ALT + E: Círculo\n" +
            "\tCTRL + F: Flecha\n" +
            "\tCTRL: Línea\n" +
            "\tSHIFT + R / E / ALT+E: Formas rellenas\n" +
            "\tSHIFT + C: Modo Censura (Pixelado)\n\n" +
            "Color y Grosor:\n" +
            "\tR: Rojo | G: Verde | B: Azul | Y: Amarillo L: Gris claro | D: Gris oscuro\n" +
            "\tC: Cyan | O: Naranja | P: Rosa | M: Magenta | K: Negro | W: Blanco\n" +
            "\tFlecha Arriba / Abajo (o + / -): Cambiar grosor del trazo\n\n" +
            "Opacidad y Fondo:\n" +
            "\tFlecha Izq / Der: Cambiar opacidad del trazo (10%)\n" +
            "\tSHIFT + 1-9, 0: Establecer opacidad absoluta (10% - 100%)\n" +
            "\tCTRL + K: Alternar fondo negro\n" +
            "\tCTRL + W: Alternar fondo blanco\n\n"
        );
        shortcuts.setFont(Font.font("System", 14));
        shortcuts.setTextFill(Color.WHITE);

        // Control Panel (Button + Slider)
        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER_LEFT);
        controlBox.setPadding(new Insets(10, 0, 0, 0));
        
        Button hideButton = new Button("Ocultar");
        hideButton.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 5 15 5 15;");
        hideButton.setOnAction(e -> hide());

        Button closeButton = new Button("Cerrar App");
        closeButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 5 15 5 15;");
        closeButton.setOnAction(e -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        Label opacityLabel = new Label("Opacidad:");
        opacityLabel.setTextFill(Color.LIGHTGRAY);

        Slider opacitySlider = new Slider(AppConfig.HELP_WINDOW_OPACITY_MIN, AppConfig.HELP_WINDOW_OPACITY_MAX, AppConfig.HELP_WINDOW_OPACITY_DEFAULT);
        opacitySlider.setShowTickMarks(false);
        opacitySlider.setShowTickLabels(false);
        
        // Bind the root node's opacity to the slider (fixes opacity on Linux X11)
        root.opacityProperty().bind(opacitySlider.valueProperty());

        controlBox.getChildren().addAll(hideButton, closeButton, opacityLabel, opacitySlider);
        
        if (!AppConfig.systemTrayLoaded) {
            Label trayWarning = new Label("⚠️ Bandeja del sistema no soportada. Use 'Cerrar App' para salir.");
            trayWarning.setTextFill(Color.ORANGE);
            trayWarning.setFont(Font.font("System", FontWeight.BOLD, 12));
            root.getChildren().addAll(title, shortcuts, trayWarning, controlBox);
        } else {
            root.getChildren().addAll(title, shortcuts, controlBox);
        }
        
        // Window dragging logic
        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            setX(event.getScreenX() - xOffset);
            setY(event.getScreenY() - yOffset);
        });
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        
        setScene(scene);
        setAlwaysOnTop(true);
    }
}
