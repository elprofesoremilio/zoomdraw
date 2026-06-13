package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.utils.AppLogger;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RouletteStage extends Stage {

    private static final Color[] VIVID_PALETTE = {
        Color.web("#E74C3C"), Color.web("#3498DB"), Color.web("#2ECC71"),
        Color.web("#F39C12"), Color.web("#9B59B6"), Color.web("#1ABC9C"),
        Color.web("#E67E22"), Color.web("#E91E63"), Color.web("#00BCD4"), Color.web("#8BC34A")
    };

    private static final Color[] PASTEL_PALETTE = {
        Color.web("#FFB3BA"), Color.web("#BAE1FF"), Color.web("#BAFFC9"),
        Color.web("#FFFFBA"), Color.web("#FFDFBA"), Color.web("#E8BAFF"),
        Color.web("#BAF0FF"), Color.web("#FFBAF0"), Color.web("#C8FFC8"), Color.web("#FFE4BA")
    };

    private final AnnotationManager manager;
    private final DoubleProperty wheelAngle = new SimpleDoubleProperty(0.0);
    private boolean spinning = false;
    private File activeFile;
    private Timeline spinTimeline;

    private Canvas wheelCanvas;
    private TextArea listTextArea;
    private Label activeFileLabel;
    private CheckBox repeatModeCheckBox;
    private StackPane root;

    public RouletteStage(AnnotationManager manager) {
        this.manager = manager;

        setTitle("ZoomDraw – Ruleta");
        setAlwaysOnTop(true);
        setWidth(820);
        setHeight(560);
        setResizable(true);

        root = new StackPane();

        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: #1e1e2e;");

        // TOP bar
        HBox topBar = new HBox();
        topBar.setStyle("-fx-background-color: #2d3436; -fx-padding: 10 15 10 15;");
        topBar.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Ruleta Aleatoria");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.WHITE);
        topBar.getChildren().add(titleLabel);
        mainLayout.setTop(topBar);

        // CENTER
        HBox centerBox = new HBox(15);
        centerBox.setPadding(new Insets(15));
        centerBox.setStyle("-fx-background-color: #1e1e2e;");

        // LEFT: wheel + repeat mode checkbox
        VBox leftColumn = new VBox(8);
        leftColumn.setAlignment(Pos.TOP_CENTER);

        wheelCanvas = new Canvas(400, 400);
        wheelCanvas.setStyle("-fx-background-color: #1e1e2e;");
        wheelCanvas.setOnMouseClicked(e -> startSpin());
        wheelCanvas.setCursor(javafx.scene.Cursor.HAND);

        repeatModeCheckBox = new CheckBox("Modo con repetición");
        repeatModeCheckBox.setSelected(AppConfig.rouletteRepeatModeDefault);
        repeatModeCheckBox.setTextFill(Color.web("#dfe6e9"));
        repeatModeCheckBox.setFont(Font.font("System", 13));
        repeatModeCheckBox.setStyle("-fx-cursor: hand;");

        HBox repeatRow = new HBox(repeatModeCheckBox);
        repeatRow.setAlignment(Pos.CENTER_LEFT);

        leftColumn.getChildren().addAll(wheelCanvas, repeatRow);

        // RIGHT: list, file controls
        VBox rightColumn = new VBox(8);
        rightColumn.setMinWidth(280);
        rightColumn.setPrefWidth(300);

        Label listLabel = new Label("Lista de alumnos:");
        listLabel.setTextFill(Color.WHITE);
        listLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        listTextArea = new TextArea();
        listTextArea.setStyle(
            "-fx-control-inner-background: #2d3436; " +
            "-fx-text-fill: white; " +
            "-fx-background-color: #2d3436; " +
            "-fx-border-color: #636e72; " +
            "-fx-border-radius: 4; " +
            "-fx-font-size: 13;"
        );
        listTextArea.setWrapText(false);
        VBox.setVgrow(listTextArea, javafx.scene.layout.Priority.ALWAYS);
        listTextArea.textProperty().addListener((obs, oldVal, newVal) -> drawWheel());

        activeFileLabel = new Label("");
        activeFileLabel.setTextFill(Color.web("#b2bec3"));
        activeFileLabel.setFont(Font.font("System", 11));
        activeFileLabel.setWrapText(true);

        Button loadButton = createDarkButton("Cargar");
        Button saveButton = createDarkButton("Guardar");
        Button saveAsButton = createDarkButton("Guardar como");

        loadButton.setOnAction(e -> loadFile());
        saveButton.setOnAction(e -> saveToActiveFile());
        saveAsButton.setOnAction(e -> saveAs());

        HBox buttonsRow1 = new HBox(6, loadButton, saveButton, saveAsButton);
        buttonsRow1.setAlignment(Pos.CENTER_LEFT);

        Button reloadButton = createDarkButton("Recargar");
        reloadButton.setOnAction(e -> reloadFromActiveFile());

        HBox buttonsRow2 = new HBox(6, reloadButton);
        buttonsRow2.setAlignment(Pos.CENTER_LEFT);

        rightColumn.getChildren().addAll(listLabel, listTextArea, activeFileLabel, buttonsRow1, buttonsRow2);

        centerBox.getChildren().addAll(leftColumn, rightColumn);
        HBox.setHgrow(rightColumn, javafx.scene.layout.Priority.ALWAYS);

        mainLayout.setCenter(centerBox);

        root.getChildren().add(mainLayout);

        Scene scene = new Scene(root, 820, 560);
        scene.setFill(Color.web("#1e1e2e"));

        scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                event.consume();
                manager.stopRouletteMode();
            }
        });

        setScene(scene);
        setOnCloseRequest(e -> manager.stopRouletteMode());

        wheelAngle.addListener((obs, oldVal, newVal) -> drawWheel());

        loadFromDefaultFile();
    }

    private Button createDarkButton(String text) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color: #2d3436; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-cursor: hand; " +
            "-fx-background-radius: 5; " +
            "-fx-padding: 5 10 5 10; " +
            "-fx-border-color: #636e72; " +
            "-fx-border-radius: 5;"
        );
        return btn;
    }

    private Color[] getPalette() {
        String p = AppConfig.rouletteColorPalette;
        if ("PASTEL".equalsIgnoreCase(p)) return PASTEL_PALETTE;
        if (!"VIVID".equalsIgnoreCase(p)) {
            String custom = AppConfig.rouletteCustomPaletteColors;
            if (custom != null && !custom.isEmpty()) {
                String[] parts = custom.split(",");
                Color[] customPalette = new Color[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    try {
                        customPalette[i] = Color.web(parts[i].trim());
                    } catch (Exception e) {
                        customPalette[i] = VIVID_PALETTE[i % VIVID_PALETTE.length];
                    }
                }
                if (customPalette.length > 0) return customPalette;
            }
        }
        return VIVID_PALETTE;
    }

    private void drawWheel() {
        List<String> items = getCurrentItems();
        int N = items.size();
        double w = wheelCanvas.getWidth();
        double h = wheelCanvas.getHeight();
        double cx = w / 2;
        double cy = h / 2;
        double r = Math.min(cx, cy) * 0.85;
        GraphicsContext gc = wheelCanvas.getGraphicsContext2D();

        gc.setFill(Color.web("#1e1e2e"));
        gc.fillRect(0, 0, w, h);

        if (N == 0) {
            gc.setFill(Color.web("#636e72"));
            gc.fillOval(cx - r, cy - r, 2 * r, 2 * r);
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("System", 14));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText("Lista vacía", cx, cy);
            drawPointer(gc, cx, cy, r);
            return;
        }

        Color[] palette = getPalette();
        double sectorAngleRad = 2 * Math.PI / N;
        double currentRot = Math.toRadians(wheelAngle.get());

        for (int i = 0; i < N; i++) {
            double startA = -Math.PI / 2 + i * sectorAngleRad + currentRot;
            gc.setFill(palette[i % palette.length]);
            gc.beginPath();
            gc.moveTo(cx, cy);
            int steps = Math.max(30, (int)(r * sectorAngleRad * 2));
            for (int j = 0; j <= steps; j++) {
                double a = startA + (double) j * sectorAngleRad / steps;
                gc.lineTo(cx + r * Math.cos(a), cy + r * Math.sin(a));
            }
            gc.closePath();
            gc.fill();

            gc.setStroke(Color.web("#1e1e2e"));
            gc.setLineWidth(1.5);
            gc.stroke();

            if (sectorAngleRad > 0.08 && N <= 60) {
                double textAngle = -Math.PI / 2 + (i + 0.5) * sectorAngleRad + currentRot;
                double textR = r * 0.65;
                double textX = cx + textR * Math.cos(textAngle);
                double textY = cy + textR * Math.sin(textAngle);

                gc.save();
                gc.translate(textX, textY);
                gc.rotate(Math.toDegrees(textAngle) + 90);
                Color bg = palette[i % palette.length];
                double lum = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
                gc.setFill(lum > 0.55 ? Color.web("#2d3436") : Color.WHITE);
                double fontSize = Math.max(8, Math.min(13, r * sectorAngleRad * 0.35));
                gc.setFont(Font.font("System", FontWeight.BOLD, fontSize));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                String label = items.get(i);
                int maxChars = Math.max(3, (int)(sectorAngleRad * r / fontSize));
                if (label.length() > maxChars) label = label.substring(0, Math.max(1, maxChars - 1)) + "…";
                gc.fillText(label, 0, 0);
                gc.restore();
            }
        }

        gc.setFill(Color.web("#2d3436"));
        double cr = r * 0.08;
        gc.fillOval(cx - cr, cy - cr, 2 * cr, 2 * cr);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - cr, cy - cr, 2 * cr, 2 * cr);

        gc.setStroke(Color.web("#dfe6e9"));
        gc.setLineWidth(2);
        gc.strokeOval(cx - r, cy - r, 2 * r, 2 * r);

        drawPointer(gc, cx, cy, r);
    }

    private void drawPointer(GraphicsContext gc, double cx, double cy, double r) {
        double tipX = cx;
        double tipY = cy - r + 2;
        double ps = 14;
        gc.setFill(Color.web("#E74C3C"));
        gc.beginPath();
        gc.moveTo(tipX, tipY + ps * 1.8);
        gc.lineTo(tipX - ps * 0.7, tipY - ps * 0.2);
        gc.lineTo(tipX + ps * 0.7, tipY - ps * 0.2);
        gc.closePath();
        gc.fill();
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.stroke();
    }

    private void startSpin() {
        if (spinning) return;
        List<String> items = getCurrentItems();
        if (items.isEmpty()) {
            showWarning("La lista está vacía. Añade alumnos para usar la ruleta.");
            return;
        }
        spinning = true;

        int N = items.size();
        int winnerIdx = new Random().nextInt(N);
        double sectorAngleDeg = 360.0 / N;

        double currentA = wheelAngle.get();
        double baseTarget = -(winnerIdx + 0.5) * sectorAngleDeg;
        double extraSpins = 5 * 360.0;
        double k = Math.ceil((currentA + extraSpins - baseTarget) / 360.0);
        double targetAngle = baseTarget + k * 360.0;

        String winner = items.get(winnerIdx);

        if (AppConfig.rouletteSkipAnimation || AppConfig.rouletteAnimationDurationMs == 0) {
            wheelAngle.set(targetAngle);
            onSpinFinished(winnerIdx, winner);
            return;
        }

        spinTimeline = new Timeline();
        KeyValue kv = new KeyValue(wheelAngle, targetAngle, Interpolator.EASE_OUT);
        KeyFrame kf = new KeyFrame(Duration.millis(AppConfig.rouletteAnimationDurationMs), kv);
        spinTimeline.getKeyFrames().add(kf);
        spinTimeline.setOnFinished(e -> onSpinFinished(winnerIdx, winner));
        spinTimeline.play();
    }

    private void onSpinFinished(int winnerIdx, String winner) {
        spinning = false;
        boolean noRepeat = !repeatModeCheckBox.isSelected();
        if (noRepeat) {
            removeEntryByIndex(winnerIdx);
        }
        showResultOverlay(winner, winnerIdx, noRepeat);
    }

    private void showResultOverlay(String winner, int winnerIdx, boolean alreadyRemoved) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");
        overlay.prefWidthProperty().bind(root.widthProperty());
        overlay.prefHeightProperty().bind(root.heightProperty());

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(360);
        card.setStyle(
            "-fx-background-color: #2d3436; " +
            "-fx-background-radius: 12; " +
            "-fx-padding: 25; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 20, 0, 0, 4);"
        );

        Label subtitleLabel = new Label("Alumno seleccionado");
        subtitleLabel.setTextFill(Color.web("#b2bec3"));
        subtitleLabel.setFont(Font.font("System", 13));

        Label winnerLabel = new Label(winner);
        winnerLabel.setTextFill(Color.WHITE);
        winnerLabel.setFont(Font.font("System", FontWeight.BOLD, 22));
        winnerLabel.setWrapText(true);
        winnerLabel.setTextAlignment(TextAlignment.CENTER);
        winnerLabel.setAlignment(Pos.CENTER);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #636e72;");

        Button eliminarButton = new Button("Eliminar");
        eliminarButton.setStyle(
            "-fx-background-color: #c0392b; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 6 18 6 18;"
        );

        Button cerrarButton = new Button("Cerrar");
        cerrarButton.setStyle(
            "-fx-background-color: #636e72; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-cursor: hand; " +
            "-fx-background-radius: 6; " +
            "-fx-padding: 6 18 6 18;"
        );

        HBox buttonRow = new HBox(10, eliminarButton, cerrarButton);
        buttonRow.setAlignment(Pos.CENTER);

        card.getChildren().addAll(subtitleLabel, winnerLabel, sep, buttonRow);
        overlay.getChildren().add(card);

        eliminarButton.setOnAction(e -> {
            if (!alreadyRemoved) {
                removeEntryByIndex(winnerIdx);
            }
            root.getChildren().remove(overlay);
        });

        cerrarButton.setOnAction(e -> root.getChildren().remove(overlay));

        overlay.setOnMouseClicked(event -> {
            if (event.getTarget() == overlay) {
                root.getChildren().remove(overlay);
            }
        });

        root.getChildren().add(overlay);
    }

    private void loadFromDefaultFile() {
        File file = new File(AppConfig.rouletteDefaultFile);
        if (!file.exists()) createDefaultFile(file);
        activeFile = file;
        loadFromFile(file);
    }

    private void createDefaultFile(File file) {
        try (Writer w = new FileWriter(file, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= 6; i++) w.write("Opción " + i + "\n");
        } catch (IOException e) {
            AppLogger.logError("No se puede crear el archivo de ruleta por defecto", e);
        }
    }

    private void loadFromFile(File file) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\n");
            String text = sb.toString();
            if (text.endsWith("\n")) text = text.substring(0, text.length() - 1);
            listTextArea.setText(text);
        } catch (IOException e) {
            AppLogger.logError("No se puede cargar el archivo de ruleta: " + file.getPath(), e);
        }
        activeFileLabel.setText(file.getAbsolutePath());
        drawWheel();
    }

    private void saveToActiveFile() {
        if (activeFile == null) {
            saveAs();
            return;
        }
        saveToFile(activeFile);
    }

    private void saveToFile(File file) {
        try (Writer w = new FileWriter(file, StandardCharsets.UTF_8)) {
            List<String> items = getCurrentItems();
            for (String item : items) w.write(item + "\n");
            showSaveFlash();
        } catch (IOException e) {
            AppLogger.logError("No se puede guardar el archivo de ruleta: " + file.getPath(), e);
        }
    }

    private void saveAs() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Guardar lista como...");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo de texto (*.txt)", "*.txt"));
        File chosen = fc.showSaveDialog(this);
        if (chosen != null) {
            activeFile = chosen;
            activeFileLabel.setText(chosen.getAbsolutePath());
            saveToFile(chosen);
        }
    }

    private void loadFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Cargar lista...");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo de texto (*.txt)", "*.txt"));
        File chosen = fc.showOpenDialog(this);
        if (chosen != null) {
            activeFile = chosen;
            loadFromFile(chosen);
        }
    }

    private void reloadFromActiveFile() {
        if (activeFile != null && activeFile.exists()) loadFromFile(activeFile);
    }

    private void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Advertencia");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showSaveFlash() {
        Label savedLabel = new Label("Guardado ✓");
        savedLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        savedLabel.setTextFill(Color.web("#2ecc71"));
        savedLabel.setStyle(
            "-fx-background-color: rgba(46,204,113,0.15); " +
            "-fx-padding: 4 10 4 10; " +
            "-fx-background-radius: 4;"
        );
        StackPane.setAlignment(savedLabel, Pos.TOP_RIGHT);
        StackPane.setMargin(savedLabel, new Insets(10, 10, 0, 0));
        root.getChildren().add(savedLabel);

        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.millis(1500), new KeyValue(savedLabel.opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(2000), new KeyValue(savedLabel.opacityProperty(), 0.0))
        );
        fadeOut.setOnFinished(e -> root.getChildren().remove(savedLabel));
        fadeOut.play();
    }

    private List<String> getCurrentItems() {
        String text = listTextArea.getText();
        if (text == null || text.trim().isEmpty()) return new ArrayList<>();
        String[] lines = text.split("\n");
        List<String> out = new ArrayList<>();
        for (String l : lines) {
            String t = l.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private void updateTextArea(List<String> items) {
        listTextArea.setText(String.join("\n", items));
        drawWheel();
    }

    private void removeEntryByIndex(int idx) {
        List<String> items = getCurrentItems();
        if (idx >= 0 && idx < items.size()) {
            items.remove(idx);
            updateTextArea(items);
        }
    }
}
