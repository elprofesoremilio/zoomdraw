package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.config.ConfigManager;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
            AppConfig.hotkeyAnnotation.toString() + ": Activar/desactivar modo anotación\n" +
            AppConfig.hotkeyLaser.toString() + ": Activar/desactivar modo puntero láser (desde inactivo)\n" +
            AppConfig.hotkeyRoulette.toString() + ": Activar/desactivar modo ruleta (desde inactivo)\n" +
            "CTRL + 0: Mostrar/Ocultar esta ventana de ayuda\n" +
            "ESC: Salir de modo anotación / puntero láser / texto / borrador / ruleta\n\n" +
            "Dibujo y Herramientas (Modo Anotación):\n" +
            "\tClick Izquierdo: Dibujar trazo libre / colocar texto\n" +
            "\tCTRL + Z: Deshacer  |  CTRL + Y: Rehacer\n" +
            "\tE: Borrar lienzo completo  |  T: Modo Texto  |  N: Modo Numeración Automática\n" +
            "\tCTRL + D: Modo Borrador (CTRL + Rueda: cambiar radio)\n\n" +
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

        // ==========================================
        // TAB 3: Roulette Configuration
        // ==========================================
        Tab tabRuleta = new Tab("Ruleta");
        VBox ruletaBox = new VBox(15);
        ruletaBox.setPadding(new Insets(15, 5, 10, 5));

        Label ruletaTitle = new Label("Configuración de la Ruleta");
        ruletaTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        ruletaTitle.setTextFill(Color.WHITE);
        ruletaTitle.setPadding(new Insets(0, 0, 10, 0));

        // a) Archivo por defecto
        Label lblDefaultFile = new Label("Archivo por defecto:");
        lblDefaultFile.setTextFill(Color.web("#b2bec3"));
        lblDefaultFile.setFont(Font.font("System", 13));

        TextField tfDefaultFile = new TextField(AppConfig.rouletteDefaultFile);
        tfDefaultFile.setStyle("-fx-background-color: #2d3436; -fx-text-fill: white; -fx-border-color: #636e72; -fx-border-radius: 4;");
        tfDefaultFile.setPrefWidth(280);

        Button btnExaminar = new Button("Examinar...");
        btnExaminar.setStyle("-fx-background-color: #2d3436; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-border-color: #636e72; -fx-border-radius: 5; -fx-padding: 4 10 4 10;");
        btnExaminar.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar archivo de ruleta por defecto");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo de texto (*.txt)", "*.txt"));
            File chosen = fc.showOpenDialog(HelpWindow.this);
            if (chosen != null) {
                String path = chosen.getAbsolutePath();
                tfDefaultFile.setText(path);
                AppConfig.rouletteDefaultFile = path;
                ConfigManager.setAndSave("roulette_default_file", path);
            }
        });

        HBox defaultFileRow = new HBox(8, tfDefaultFile, btnExaminar);
        defaultFileRow.setAlignment(Pos.CENTER_LEFT);

        // b) Duración de animación
        Label lblDuration = new Label("Duración de animación (ms):");
        lblDuration.setTextFill(Color.web("#b2bec3"));
        lblDuration.setFont(Font.font("System", 13));

        TextField tfDuration = new TextField(String.valueOf(AppConfig.rouletteAnimationDurationMs));
        tfDuration.setPrefWidth(80);
        tfDuration.setStyle("-fx-background-color: #2d3436; -fx-text-fill: white; -fx-border-color: #636e72; -fx-border-radius: 4;");

        Slider sliderDuration = new Slider(
            AppConfig.ROULETTE_ANIMATION_DURATION_MS_MIN,
            AppConfig.ROULETTE_ANIMATION_DURATION_MS_MAX,
            AppConfig.rouletteAnimationDurationMs
        );
        sliderDuration.setBlockIncrement(AppConfig.ROULETTE_ANIMATION_DURATION_MS_STEP);
        sliderDuration.setPrefWidth(200);

        sliderDuration.valueProperty().addListener((obs, oldVal, newVal) -> {
            int val = (int)(Math.round(newVal.doubleValue() / AppConfig.ROULETTE_ANIMATION_DURATION_MS_STEP) * AppConfig.ROULETTE_ANIMATION_DURATION_MS_STEP);
            if (val != AppConfig.rouletteAnimationDurationMs) {
                AppConfig.rouletteAnimationDurationMs = val;
                tfDuration.setText(String.valueOf(val));
                ConfigManager.setAndSave("animation_duration_ms", String.valueOf(val));
            }
        });

        tfDuration.setOnAction(e -> {
            try {
                int val = Integer.parseInt(tfDuration.getText().trim());
                val = Math.max(AppConfig.ROULETTE_ANIMATION_DURATION_MS_MIN, Math.min(AppConfig.ROULETTE_ANIMATION_DURATION_MS_MAX, val));
                AppConfig.rouletteAnimationDurationMs = val;
                sliderDuration.setValue(val);
                ConfigManager.setAndSave("animation_duration_ms", String.valueOf(val));
            } catch (NumberFormatException ex) {
                tfDuration.setText(String.valueOf(AppConfig.rouletteAnimationDurationMs));
            }
        });

        HBox durationRow = new HBox(8, tfDuration, sliderDuration);
        durationRow.setAlignment(Pos.CENTER_LEFT);

        // c) Saltar animación
        Label lblSkip = new Label("Saltar animación:");
        lblSkip.setTextFill(Color.web("#b2bec3"));
        lblSkip.setFont(Font.font("System", 13));

        CheckBox cbSkip = new CheckBox();
        cbSkip.setSelected(AppConfig.rouletteSkipAnimation);
        cbSkip.setStyle("-fx-cursor: hand;");
        cbSkip.setOnAction(e -> {
            AppConfig.rouletteSkipAnimation = cbSkip.isSelected();
            ConfigManager.setAndSave("skip_animation", String.valueOf(cbSkip.isSelected()));
        });

        // d) Paleta de colores
        Label lblPalette = new Label("Paleta de colores:");
        lblPalette.setTextFill(Color.web("#b2bec3"));
        lblPalette.setFont(Font.font("System", 13));

        List<String> paletteOptions = new ArrayList<>();
        paletteOptions.add("VIVID");
        paletteOptions.add("PASTEL");
        paletteOptions.add(AppConfig.rouletteCustomPaletteName);

        ComboBox<String> cbPalette = new ComboBox<>();
        cbPalette.getItems().addAll(paletteOptions);
        cbPalette.setValue(AppConfig.rouletteColorPalette);
        cbPalette.setStyle("-fx-background-color: #2d3436; -fx-text-fill: white;");

        Color[] vividPreview = {
            Color.web("#E74C3C"), Color.web("#3498DB"), Color.web("#2ECC71"),
            Color.web("#F39C12"), Color.web("#9B59B6")
        };
        Color[] pastelPreview = {
            Color.web("#FFB3BA"), Color.web("#BAE1FF"), Color.web("#BAFFC9"),
            Color.web("#FFFFBA"), Color.web("#FFDFBA")
        };

        cbPalette.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Color[] swatches = getSwatchColors(item);
                    HBox swatchBox = buildSwatchRow(item, swatches);
                    setGraphic(swatchBox);
                    setText(null);
                    setStyle("-fx-background-color: #2d3436; -fx-text-fill: white;");
                }
            }

            private Color[] getSwatchColors(String name) {
                if ("VIVID".equalsIgnoreCase(name)) return vividPreview;
                if ("PASTEL".equalsIgnoreCase(name)) return pastelPreview;
                return getCustomPreviewColors();
            }
        });

        cbPalette.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Color[] swatches = "VIVID".equalsIgnoreCase(item) ? vividPreview
                        : "PASTEL".equalsIgnoreCase(item) ? pastelPreview
                        : getCustomPreviewColors();
                    setGraphic(buildSwatchRow(item, swatches));
                    setText(null);
                    setStyle("-fx-background-color: #2d3436; -fx-text-fill: white;");
                }
            }
        });

        cbPalette.setOnAction(e -> {
            String selected = cbPalette.getValue();
            if (selected == null) return;
            if ("VIVID".equalsIgnoreCase(selected) || "PASTEL".equalsIgnoreCase(selected)) {
                AppConfig.rouletteColorPalette = selected.toUpperCase();
                ConfigManager.setAndSave("color_palette", selected.toUpperCase());
            } else {
                openCustomPaletteEditor(cbPalette);
            }
        });

        // e) Modo repetición por defecto
        Label lblRepeat = new Label("Modo repetición por defecto:");
        lblRepeat.setTextFill(Color.web("#b2bec3"));
        lblRepeat.setFont(Font.font("System", 13));

        CheckBox cbRepeat = new CheckBox();
        cbRepeat.setSelected(AppConfig.rouletteRepeatModeDefault);
        cbRepeat.setStyle("-fx-cursor: hand;");
        cbRepeat.setOnAction(e -> {
            AppConfig.rouletteRepeatModeDefault = cbRepeat.isSelected();
            ConfigManager.setAndSave("repeat_mode_default", String.valueOf(cbRepeat.isSelected()));
        });

        GridPane ruletaGrid = new GridPane();
        ruletaGrid.setHgap(15);
        ruletaGrid.setVgap(12);
        ruletaGrid.setAlignment(Pos.CENTER_LEFT);

        ruletaGrid.add(lblDefaultFile, 0, 0);
        ruletaGrid.add(defaultFileRow, 1, 0);

        ruletaGrid.add(lblDuration, 0, 1);
        ruletaGrid.add(durationRow, 1, 1);

        ruletaGrid.add(lblSkip, 0, 2);
        ruletaGrid.add(cbSkip, 1, 2);

        ruletaGrid.add(lblPalette, 0, 3);
        ruletaGrid.add(cbPalette, 1, 3);

        ruletaGrid.add(lblRepeat, 0, 4);
        ruletaGrid.add(cbRepeat, 1, 4);

        ruletaBox.getChildren().addAll(ruletaTitle, ruletaGrid);
        tabRuleta.setContent(ruletaBox);

        // Style the tab pane to match the dark slate visual identity
        tabPane.getTabs().addAll(tabShortcuts, tabLaser, tabRuleta);
        
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

    private HBox buildSwatchRow(String name, Color[] swatches) {
        HBox row = new HBox(4);
        row.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(name);
        nameLabel.setTextFill(Color.web("#dfe6e9"));
        nameLabel.setFont(Font.font("System", 13));
        nameLabel.setMinWidth(60);
        row.getChildren().add(nameLabel);
        for (Color c : swatches) {
            Canvas swatch = new Canvas(14, 14);
            GraphicsContext gc = swatch.getGraphicsContext2D();
            gc.setFill(c);
            gc.fillRoundRect(0, 0, 14, 14, 3, 3);
            gc.setStroke(Color.web("#636e72"));
            gc.setLineWidth(0.5);
            gc.strokeRoundRect(0, 0, 14, 14, 3, 3);
            row.getChildren().add(swatch);
        }
        return row;
    }

    private Color[] getCustomPreviewColors() {
        String custom = AppConfig.rouletteCustomPaletteColors;
        if (custom == null || custom.isEmpty()) {
            return new Color[]{Color.web("#636e72"), Color.web("#636e72"), Color.web("#636e72"),
                               Color.web("#636e72"), Color.web("#636e72")};
        }
        String[] parts = custom.split(",");
        int count = Math.min(5, parts.length);
        Color[] result = new Color[count];
        for (int i = 0; i < count; i++) {
            try {
                result[i] = Color.web(parts[i].trim());
            } catch (Exception e) {
                result[i] = Color.web("#636e72");
            }
        }
        return result;
    }

    private void openCustomPaletteEditor(ComboBox<String> cbPalette) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(HelpWindow.this);
        dialog.setTitle("Editar paleta personalizada");
        dialog.setResizable(false);

        VBox layout = new VBox(12);
        layout.setPadding(new Insets(20));
        layout.setStyle("-fx-background-color: #1e1e2e;");

        Label titleLbl = new Label("Paleta personalizada");
        titleLbl.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLbl.setTextFill(Color.WHITE);

        Label nameLbl = new Label("Nombre de la paleta:");
        nameLbl.setTextFill(Color.web("#b2bec3"));
        nameLbl.setFont(Font.font("System", 13));

        TextField tfName = new TextField(AppConfig.rouletteCustomPaletteName);
        tfName.setStyle("-fx-background-color: #2d3436; -fx-text-fill: white; -fx-border-color: #636e72; -fx-border-radius: 4;");

        Label colorsLbl = new Label("Colores:");
        colorsLbl.setTextFill(Color.web("#b2bec3"));
        colorsLbl.setFont(Font.font("System", 13));

        List<Color> colorList = new ArrayList<>();
        if (!AppConfig.rouletteCustomPaletteColors.isEmpty()) {
            for (String part : AppConfig.rouletteCustomPaletteColors.split(",")) {
                try {
                    colorList.add(Color.web(part.trim()));
                } catch (Exception ignored) {}
            }
        }

        FlowPane colorFlow = new FlowPane(6, 6);
        colorFlow.setStyle("-fx-background-color: #2d3436; -fx-padding: 8; -fx-background-radius: 4;");
        colorFlow.setPrefWrapLength(360);
        ListView<Color> colorListView = new ListView<>();
        colorListView.setPrefHeight(150);
        colorListView.setStyle("-fx-background-color: #2d3436; -fx-control-inner-background: #2d3436; -fx-text-fill: white;");
        colorListView.setCellFactory(lv -> new ListCell<Color>() {
            @Override
            protected void updateItem(Color item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Canvas c = new Canvas(18, 18);
                    GraphicsContext gc = c.getGraphicsContext2D();
                    gc.setFill(item);
                    gc.fillRoundRect(0, 0, 18, 18, 4, 4);
                    gc.setStroke(Color.web("#dfe6e9"));
                    gc.setLineWidth(0.5);
                    gc.strokeRoundRect(0, 0, 18, 18, 4, 4);
                    String hex = String.format("#%02X%02X%02X",
                        (int)(item.getRed()*255), (int)(item.getGreen()*255), (int)(item.getBlue()*255));
                    Label lbl = new Label(hex);
                    lbl.setTextFill(Color.web("#dfe6e9"));
                    lbl.setFont(Font.font("System", 12));
                    HBox cell = new HBox(6, c, lbl);
                    cell.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(cell);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });
        colorListView.getItems().addAll(colorList);

        Button btnAdd = new Button("Añadir color");
        btnAdd.setStyle("-fx-background-color: #2d3436; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-border-color: #636e72; -fx-border-radius: 5; -fx-padding: 5 10 5 10;");
        btnAdd.setOnAction(e -> {
            ColorPicker cp = new ColorPicker(Color.web("#E74C3C"));
            Dialog<Color> cpDialog = new Dialog<>();
            cpDialog.setTitle("Seleccionar color");
            cpDialog.getDialogPane().setContent(cp);
            cpDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            cpDialog.setResultConverter(bt -> bt == ButtonType.OK ? cp.getValue() : null);
            cpDialog.showAndWait().ifPresent(color -> {
                colorListView.getItems().add(color);
            });
        });

        Button btnRemove = new Button("Eliminar seleccionado");
        btnRemove.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 5 10 5 10;");
        btnRemove.setOnAction(e -> {
            Color selected = colorListView.getSelectionModel().getSelectedItem();
            if (selected != null) colorListView.getItems().remove(selected);
        });

        HBox colorButtons = new HBox(8, btnAdd, btnRemove);
        colorButtons.setAlignment(Pos.CENTER_LEFT);

        Button btnOk = new Button("Aceptar");
        btnOk.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 6 18 6 18;");
        btnOk.setOnAction(e -> {
            String newName = tfName.getText().trim();
            if (newName.isEmpty()) newName = "Custom";
            AppConfig.rouletteCustomPaletteName = newName;
            ConfigManager.setAndSave("custom_palette_name", newName);

            List<String> hexColors = new ArrayList<>();
            for (Color c : colorListView.getItems()) {
                hexColors.add(String.format("#%02X%02X%02X",
                    (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255)));
            }
            String colorsStr = String.join(",", hexColors);
            AppConfig.rouletteCustomPaletteColors = colorsStr;
            ConfigManager.setAndSave("custom_palette_colors", colorsStr);
            AppConfig.rouletteColorPalette = newName;
            ConfigManager.setAndSave("color_palette", newName);

            cbPalette.getItems().clear();
            cbPalette.getItems().addAll("VIVID", "PASTEL", newName);
            cbPalette.setValue(newName);
            dialog.close();
        });

        Button btnCancel = new Button("Cancelar");
        btnCancel.setStyle("-fx-background-color: #636e72; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6; -fx-padding: 6 18 6 18;");
        btnCancel.setOnAction(e -> dialog.close());

        HBox dialogButtons = new HBox(10, btnOk, btnCancel);
        dialogButtons.setAlignment(Pos.CENTER_RIGHT);

        layout.getChildren().addAll(titleLbl, nameLbl, tfName, colorsLbl, colorListView, colorButtons, dialogButtons);

        Scene dialogScene = new Scene(layout, 420, 380);
        dialogScene.setFill(Color.web("#1e1e2e"));
        dialog.setScene(dialogScene);
        dialog.showAndWait();
    }

    private boolean isInteractive(javafx.scene.Node node) {
        javafx.scene.Node parent = node;
        while (parent != null) {
            if (parent instanceof Button || parent instanceof Slider || parent instanceof CheckBox ||
                parent instanceof ColorPicker || parent instanceof ScrollBar || parent instanceof ScrollPane ||
                parent instanceof TextField || parent instanceof ComboBox) {
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

