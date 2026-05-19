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
            "Dibujo y Herramientas:\n" +
            "Click Izquierdo: Dibujar trazo libre / texto\n" +
            "CTRL + Z: Deshacer\n" +
            "CTRL + Y: Rehacer\n" +
            "E (sin modificadores): Borrar todos los trazos\n" +
            "CTRL + T: Modo Texto\n" +
            "ESC: Cancelar forma / salir de texto / salir de aplicación\n\n" +
            "Formas Geométricas (Mantener presionado y arrastrar):\n" +
            "CTRL + R: Rectángulo\n" +
            "CTRL + E: Elipse\n" +
            "CTRL + ALT + E: Círculo\n" +
            "CTRL + F: Flecha\n" +
            "CTRL + (Nada): Línea\n" +
            "SHIFT + R / E / ALT+E: Formas rellenas\n" +
            "SHIFT + C: Modo Censura (Pixelado)\n\n" +
            "Color y Grosor:\n" +
            "R: Rojo | G: Verde | B: Azul | Y: Amarillo\n" +
            "O: Naranja | P: Magenta | K: Negro | W: Blanco\n" +
            "Flecha Arriba / Abajo (o + / -): Cambiar grosor del trazo\n\n" +
            "Opacidad y Fondo:\n" +
            "Flecha Izq / Der: Cambiar opacidad del trazo (10%)\n" +
            "SHIFT + 1-9, 0: Establecer opacidad absoluta (10% - 100%)\n" +
            "CTRL + K: Alternar fondo negro\n" +
            "CTRL + W: Alternar fondo blanco\n" +
            "CTRL + 0: Mostrar/Ocultar esta ayuda"
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

        Label opacityLabel = new Label("Opacidad:");
        opacityLabel.setTextFill(Color.LIGHTGRAY);

        Slider opacitySlider = new Slider(AppConfig.HELP_WINDOW_OPACITY_MIN, AppConfig.HELP_WINDOW_OPACITY_MAX, AppConfig.HELP_WINDOW_OPACITY_DEFAULT);
        opacitySlider.setShowTickMarks(false);
        opacitySlider.setShowTickLabels(false);
        
        // Bind the stage opacity to the slider
        this.opacityProperty().bind(opacitySlider.valueProperty());

        controlBox.getChildren().addAll(hideButton, opacityLabel, opacitySlider);
        
        root.getChildren().addAll(title, shortcuts, controlBox);
        
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
