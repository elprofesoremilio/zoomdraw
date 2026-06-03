package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
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

    public HelpWindow(AnnotationManager manager) {
        initStyle(StageStyle.TRANSPARENT);
        
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: rgba(20, 20, 20, 0.95); -fx-background-radius: 12; -fx-border-color: #34495e; -fx-border-radius: 12; -fx-border-width: 1.5;");
        
        // TabPane for premium categorization
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabPane.setStyle("-fx-background-color: transparent;");

        // ==========================================
        // TAB 1: Shortcuts & General Info
        // ==========================================
        Tab tabShortcuts = new Tab("Atajos de Teclado");
        VBox shortcutsBox = new VBox(10);
        shortcutsBox.setPadding(new Insets(15, 0, 10, 0));
        
        Label title = new Label("Atajos de Teclado");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.WHITE);
        title.setPadding(new Insets(0, 0, 10, 0));
        
        Label shortcuts = new Label(
            "CTRL + 1: Activar/desactivar modo anotación\n" +
            "CTRL + 2: Activar/desactivar modo puntero láser (desde inactivo)\n" +
            "CTRL + 0: Mostrar/Ocultar esta ventana de ayuda\n" +
            "ESC: Salir de modo anotación / puntero láser / texto\n\n" +
            "Dibujo y Herramientas (Modo Anotación):\n" +
            "\tClick Izquierdo: Dibujar trazo libre / colocar texto\n" +
            "\tCTRL + Z: Deshacer  |  CTRL + Y: Rehacer\n" +
            "\tE: Borrar lienzo completo  |  T: Modo Texto  |  N: Modo Numeración Automática\n\n" +
            "Modo Numeración Automática (N):\n" +
            "\tClick Izq en vacío: Insertar número secuencial (1, 2, 3...)\n" +
            "\tArrastrar número: Mover número libremente\n" +
            "\tClick en número: Seleccionar para arrastrar o borrar\n" +
            "\tDEL / Backspace: Borrar seleccionado y reordenar secuencia\n" +
            "\tCTRL + Z / Y: Deshacer / rehacer movimientos y borrados temporales\n" +
            "\tESC: Salir de numeración (combina todos los números en un único comando)\n\n" +
            "Formas Geométricas (Mantener atajo + arrastrar ratón):\n" +
            "\tCTRL: Línea  |  CTRL + F: Flecha\n" +
            "\tCTRL + R: Rectángulo  |  CTRL + E: Elipse  |  CTRL + ALT + E: Círculo\n" +
            "\tSHIFT + R / E / ALT+E: Formas rellenas\n" +
            "\tSHIFT + C: Modo Censura (Pixelado)\n\n" +
            "Color y Grosor del Trazo:\n" +
            "\tR: Rojo | G: Verde | B: Azul | Y: Amarillo | L: Gris claro | D: Gris oscuro\n" +
            "\tC: Cyan | O: Naranja | P: Rosa | M: Magenta | K: Negro | W: Blanco\n" +
            "\tFlecha Arriba / Abajo (o + / -) o CTRL + Rueda del ratón: Cambiar grosor\n\n" +
            "Fondo y Opacidad:\n" +
            "\tFlecha Izq / Der o SHIFT + Rueda del ratón: Cambiar opacidad del trazo\n" +
            "\tSHIFT + 1-9, 0: Establecer opacidad absoluta (10% - 100%)\n" +
            "\tCTRL + K: Fondo negro  |  CTRL + W: Fondo blanco\n\n" +
            "Zoom (Modo Anotación):\n" +
            "\tRueda del ratón (sin CTRL/SHIFT): Ampliar / reducir zoom (1x - 8x centrado en el cursor)"
        );
        shortcuts.setFont(Font.font("System", 13.5));
        shortcuts.setTextFill(Color.web("#dfe6e9"));

        shortcutsBox.getChildren().addAll(title, shortcuts);
        tabShortcuts.setContent(shortcutsBox);

        // ==========================================
        // TAB 2: Laser Pointer Configuration
        // ==========================================
        Tab tabLaser = new Tab("Puntero Láser");
        VBox laserBox = new VBox(15);
        laserBox.setPadding(new Insets(15, 5, 10, 5));

        Label laserTitle = new Label("Configuración del Puntero Láser");
        laserTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        laserTitle.setTextFill(Color.WHITE);
        laserTitle.setPadding(new Insets(0, 0, 10, 0));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER_LEFT);

        // 1. Border Color
        Label lblBorderColor = new Label("Color del borde:");
        lblBorderColor.setTextFill(Color.web("#b2bec3"));
        lblBorderColor.setFont(Font.font("System", 13));
        ColorPicker cpBorder = new ColorPicker(AppConfig.borderColor);
        cpBorder.setStyle("-fx-background-color: #2d3436; -fx-color-label-visible: false;");
        cpBorder.setOnAction(e -> {
            AppConfig.borderColor = cpBorder.getValue();
            manager.requestLaserRedraw();
        });
        grid.add(lblBorderColor, 0, 0);
        grid.add(cpBorder, 1, 0);

        // 2. Border Thickness
        Label lblBorderThick = new Label("Grosor del borde:");
        lblBorderThick.setTextFill(Color.web("#b2bec3"));
        lblBorderThick.setFont(Font.font("System", 13));
        Slider sliderBorderThick = new Slider(1, 30, AppConfig.borderThickness);
        sliderBorderThick.setBlockIncrement(1);
        Label valBorderThick = new Label(AppConfig.borderThickness + " px");
        valBorderThick.setTextFill(Color.WHITE);
        valBorderThick.setFont(Font.font("System", FontWeight.BOLD, 13));
        sliderBorderThick.valueProperty().addListener((obs, oldVal, newVal) -> {
            AppConfig.borderThickness = newVal.intValue();
            valBorderThick.setText(newVal.intValue() + " px");
            manager.requestLaserRedraw();
        });
        grid.add(lblBorderThick, 0, 1);
        grid.add(sliderBorderThick, 1, 1);
        grid.add(valBorderThick, 2, 1);

        // 3. Border Opacity
        Label lblBorderOpacity = new Label("Opacidad del borde:");
        lblBorderOpacity.setTextFill(Color.web("#b2bec3"));
        lblBorderOpacity.setFont(Font.font("System", 13));
        Slider sliderBorderOpacity = new Slider(0.0, 1.0, AppConfig.borderOpacity);
        Label valBorderOpacity = new Label((int)(AppConfig.borderOpacity * 100) + " %");
        valBorderOpacity.setTextFill(Color.WHITE);
        valBorderOpacity.setFont(Font.font("System", FontWeight.BOLD, 13));
        sliderBorderOpacity.valueProperty().addListener((obs, oldVal, newVal) -> {
            AppConfig.borderOpacity = newVal.doubleValue();
            valBorderOpacity.setText((int)(newVal.doubleValue() * 100) + " %");
            manager.requestLaserRedraw();
        });
        grid.add(lblBorderOpacity, 0, 2);
        grid.add(sliderBorderOpacity, 1, 2);
        grid.add(valBorderOpacity, 2, 2);

        // 4. Circle Radius
        Label lblRadius = new Label("Radio del círculo:");
        lblRadius.setTextFill(Color.web("#b2bec3"));
        lblRadius.setFont(Font.font("System", 13));
        Slider sliderRadius = new Slider(5, 100, AppConfig.circleRadius);
        Label valRadius = new Label(AppConfig.circleRadius + " px");
        valRadius.setTextFill(Color.WHITE);
        valRadius.setFont(Font.font("System", FontWeight.BOLD, 13));
        sliderRadius.valueProperty().addListener((obs, oldVal, newVal) -> {
            AppConfig.circleRadius = newVal.intValue();
            valRadius.setText(newVal.intValue() + " px");
            manager.requestLaserRedraw();
        });
        grid.add(lblRadius, 0, 3);
        grid.add(sliderRadius, 1, 3);
        grid.add(valRadius, 2, 3);

        // 5. Show Central Cross
        Label lblShowCross = new Label("Mostrar cruz central:");
        lblShowCross.setTextFill(Color.web("#b2bec3"));
        lblShowCross.setFont(Font.font("System", 13));
        CheckBox cbShowCross = new CheckBox();
        cbShowCross.setSelected(AppConfig.showCross);
        cbShowCross.setStyle("-fx-cursor: hand;");
        cbShowCross.setOnAction(e -> {
            AppConfig.showCross = cbShowCross.isSelected();
            manager.requestLaserRedraw();
        });
        grid.add(lblShowCross, 0, 4);
        grid.add(cbShowCross, 1, 4);

        // 6. Cross Color
        Label lblCrossColor = new Label("Color de la cruz:");
        lblCrossColor.setTextFill(Color.web("#b2bec3"));
        lblCrossColor.setFont(Font.font("System", 13));
        ColorPicker cpCross = new ColorPicker(AppConfig.crossColor);
        cpCross.setStyle("-fx-background-color: #2d3436; -fx-color-label-visible: false;");
        cpCross.setOnAction(e -> {
            AppConfig.crossColor = cpCross.getValue();
            manager.requestLaserRedraw();
        });
        grid.add(lblCrossColor, 0, 5);
        grid.add(cpCross, 1, 5);

        // 7. Cross Thickness
        Label lblCrossThick = new Label("Grosor de la cruz:");
        lblCrossThick.setTextFill(Color.web("#b2bec3"));
        lblCrossThick.setFont(Font.font("System", 13));
        Slider sliderCrossThick = new Slider(1, 10, AppConfig.crossThickness);
        Label valCrossThick = new Label(AppConfig.crossThickness + " px");
        valCrossThick.setTextFill(Color.WHITE);
        valCrossThick.setFont(Font.font("System", FontWeight.BOLD, 13));
        sliderCrossThick.valueProperty().addListener((obs, oldVal, newVal) -> {
            AppConfig.crossThickness = newVal.intValue();
            valCrossThick.setText(newVal.intValue() + " px");
            manager.requestLaserRedraw();
        });
        grid.add(lblCrossThick, 0, 6);
        grid.add(sliderCrossThick, 1, 6);
        grid.add(valCrossThick, 2, 6);

        laserBox.getChildren().addAll(laserTitle, grid);
        tabLaser.setContent(laserBox);

        // Style the tab pane to match the dark slate visual identity
        tabPane.getTabs().addAll(tabShortcuts, tabLaser);
        
        // Control Panel (Button + Slider)
        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER_LEFT);
        controlBox.setPadding(new Insets(15, 0, 0, 0));
        
        Button hideButton = new Button("Ocultar");
        hideButton.setStyle("-fx-background-color: #2d3436; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 6 18 6 18;");
        hideButton.setOnAction(e -> hide());

        Button closeButton = new Button("Cerrar App");
        closeButton.setStyle("-fx-background-color: #d9534f; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 6 18 6 18;");
        closeButton.setOnAction(e -> {
            javafx.application.Platform.exit();
            System.exit(0);
        });

        Label opacityLabel = new Label("Opacidad ventana:");
        opacityLabel.setTextFill(Color.web("#b2bec3"));
        opacityLabel.setFont(Font.font("System", 13));

        Slider opacitySlider = new Slider(AppConfig.HELP_WINDOW_OPACITY_MIN, AppConfig.HELP_WINDOW_OPACITY_MAX, AppConfig.HELP_WINDOW_OPACITY_DEFAULT);
        opacitySlider.setShowTickMarks(false);
        opacitySlider.setShowTickLabels(false);
        
        // Bind the root node's opacity to the slider
        root.opacityProperty().bind(opacitySlider.valueProperty());

        controlBox.getChildren().addAll(hideButton, closeButton, opacityLabel, opacitySlider);
        
        if (!AppConfig.systemTrayLoaded) {
            Label trayWarning = new Label("⚠️ Bandeja del sistema no soportada. Use 'Cerrar App' para salir.");
            trayWarning.setTextFill(Color.ORANGE);
            trayWarning.setFont(Font.font("System", FontWeight.BOLD, 12));
            root.getChildren().addAll(tabPane, trayWarning, controlBox);
        } else {
            root.getChildren().addAll(tabPane, controlBox);
        }
        
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        
        // Window dragging logic using scene filters to allow dragging from non-interactive components
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            javafx.scene.Node target = (javafx.scene.Node) event.getTarget();
            if (!isInteractive(target)) {
                xOffset = event.getSceneX();
                yOffset = event.getSceneY();
            } else {
                xOffset = -1; // Sentinel value to disable dragging
            }
        });
        
        scene.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
            if (xOffset != -1) {
                setX(event.getScreenX() - xOffset);
                setY(event.getScreenY() - yOffset);
            }
        });
        
        // Inject a dark and beautiful flat styling stylesheet for the TabPane
        java.net.URL cssUrl = getClass().getResource("/help_style.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        
        setScene(scene);
        setAlwaysOnTop(true);
    }

    private boolean isInteractive(javafx.scene.Node node) {
        javafx.scene.Node parent = node;
        while (parent != null) {
            if (parent instanceof Button || parent instanceof Slider || parent instanceof CheckBox || 
                parent instanceof ColorPicker || parent instanceof ScrollBar || parent instanceof ScrollPane) {
                return true;
            }
            for (String styleClass : parent.getStyleClass()) {
                if (styleClass.equals("tab") || styleClass.equals("tab-header-area") || 
                    styleClass.equals("tab-container") || styleClass.equals("tab-header-background")) {
                    return true;
                }
            }
            parent = parent.getParent();
        }
        return false;
    }
}

