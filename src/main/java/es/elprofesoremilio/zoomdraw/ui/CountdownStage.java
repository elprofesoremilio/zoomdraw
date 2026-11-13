package es.elprofesoremilio.zoomdraw.ui;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.config.ConfigManager;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Always-on-top countdown window. Resizable/maximizable; the time text scales with the window.
 */
public class CountdownStage extends Stage {

    private static final String BG_NORMAL = "-fx-background-color: #1e1e1e;";
    private static final String BG_FINISHED = "-fx-background-color: #b71c1c;";

    private final Label timeLabel = new Label();
    private final Spinner<Integer> spMinutes = new Spinner<>(0, 999, 0);
    private final Spinner<Integer> spSeconds = new Spinner<>(0, 59, 0);
    private final Button btnStartPause = new Button("Iniciar");
    private final Button btnReset = new Button("Reiniciar");
    private final HBox controls = new HBox(10);
    private final StackPane center = new StackPane(timeLabel);
    private final Timeline ticker = new Timeline(new KeyFrame(Duration.millis(100), e -> tick()));
    private final Timeline flasher = new Timeline(new KeyFrame(Duration.millis(500), e -> toggleFlash()));

    private long totalMs;
    private long remainingMs;
    private long endNanos;
    private boolean running = false;
    private boolean flashOn = false;
    private int beepsLeft = 0;

    public CountdownStage() {
        setTitle("ZoomDraw - Cuenta atrás");
        setAlwaysOnTop(true);
        setMinWidth(260);
        setMinHeight(160);
        setWidth(420);
        setHeight(260);
        setFullScreenExitHint("");
        setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);

        ticker.setCycleCount(Animation.INDEFINITE);
        flasher.setCycleCount(Animation.INDEFINITE);

        int seconds = Math.max(1, AppConfig.countdownDefaultSeconds);
        spMinutes.getValueFactory().setValue(seconds / 60);
        spSeconds.getValueFactory().setValue(seconds % 60);
        spMinutes.setEditable(true);
        spSeconds.setEditable(true);
        spMinutes.setPrefWidth(80);
        spSeconds.setPrefWidth(70);
        spMinutes.valueProperty().addListener((o, a, b) -> onDurationEdited());
        spSeconds.valueProperty().addListener((o, a, b) -> onDurationEdited());
        // Commit typed text when the spinner loses focus
        spMinutes.focusedProperty().addListener((o, was, is) -> { if (!is) commitSpinner(spMinutes); });
        spSeconds.focusedProperty().addListener((o, was, is) -> { if (!is) commitSpinner(spSeconds); });

        btnStartPause.setOnAction(e -> toggleRun());
        btnReset.setOnAction(e -> reset());

        controls.setAlignment(Pos.CENTER);
        controls.setPadding(new Insets(10));
        controls.getChildren().addAll(new Label("Min:"), spMinutes, new Label("Seg:"), spSeconds,
                btnStartPause, btnReset);
        controls.getChildren().stream().filter(n -> n instanceof Label)
                .forEach(n -> ((Label) n).setStyle("-fx-text-fill: white;"));

        center.setPadding(new Insets(10));
        center.setStyle(BG_NORMAL);
        center.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                setMaximized(!isMaximized());
            }
        });

        BorderPane root = new BorderPane(center);
        root.setBottom(controls);
        root.setStyle(BG_NORMAL);
        controls.setStyle(BG_NORMAL);
        // The controls get out of the way when the window is fullscreen
        controls.visibleProperty().bind(fullScreenProperty().not());
        controls.managedProperty().bind(fullScreenProperty().not());

        Scene scene = new Scene(root);
        // Font scales with the window, limited by height and by width
        Runnable resizeFont = () -> {
            double size = Math.min(scene.getHeight() * 0.45, scene.getWidth() * 0.22);
            timeLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: "
                    + Math.max(12, size) + "px;");
        };
        scene.widthProperty().addListener((o, a, b) -> resizeFont.run());
        scene.heightProperty().addListener((o, a, b) -> resizeFont.run());
        resizeFont.run();

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE && !(e.getTarget() instanceof javafx.scene.control.TextInputControl)
                    && !(e.getTarget() instanceof Button)) {
                toggleRun();
                e.consume();
            } else if (e.getCode() == KeyCode.F11) {
                setFullScreen(!isFullScreen());
                e.consume();
            } else if (e.getCode() == KeyCode.ESCAPE) {
                if (isFullScreen()) setFullScreen(false);
                else close();
                e.consume();
            }
        });
        setScene(scene);

        setOnHidden(e -> {
            ticker.stop();
            flasher.stop();
        });

        loadDurationFromSpinners();
        updateLabel();
    }

    private void commitSpinner(Spinner<Integer> sp) {
        try {
            sp.getValueFactory().setValue(Integer.parseInt(sp.getEditor().getText().trim()));
        } catch (NumberFormatException ex) {
            sp.getEditor().setText(String.valueOf(sp.getValue()));
        }
    }

    private void onDurationEdited() {
        if (running) return;
        loadDurationFromSpinners();
        stopFinishedEffects();
        updateLabel();
        long secs = totalMs / 1000;
        if (secs > 0) {
            AppConfig.countdownDefaultSeconds = (int) secs;
            ConfigManager.setAndSave("countdown_default_seconds", String.valueOf(secs));
        }
    }

    private void loadDurationFromSpinners() {
        totalMs = (spMinutes.getValue() * 60L + spSeconds.getValue()) * 1000L;
        remainingMs = totalMs;
    }

    private void toggleRun() {
        if (running) {
            pause();
            return;
        }
        if (remainingMs <= 0) {
            loadDurationFromSpinners();
        }
        if (remainingMs <= 0) return;
        stopFinishedEffects();
        endNanos = System.nanoTime() + remainingMs * 1_000_000L;
        running = true;
        btnStartPause.setText("Pausar");
        spMinutes.setDisable(true);
        spSeconds.setDisable(true);
        ticker.play();
    }

    private void pause() {
        ticker.stop();
        running = false;
        btnStartPause.setText("Reanudar");
        spMinutes.setDisable(false);
        spSeconds.setDisable(false);
    }

    private void reset() {
        ticker.stop();
        running = false;
        stopFinishedEffects();
        btnStartPause.setText("Iniciar");
        spMinutes.setDisable(false);
        spSeconds.setDisable(false);
        loadDurationFromSpinners();
        updateLabel();
    }

    private void tick() {
        remainingMs = Math.max(0, (endNanos - System.nanoTime()) / 1_000_000L);
        updateLabel();
        if (remainingMs <= 0) {
            ticker.stop();
            running = false;
            btnStartPause.setText("Iniciar");
            spMinutes.setDisable(false);
            spSeconds.setDisable(false);
            startFinishedEffects();
        }
    }

    private void updateLabel() {
        long totalSeconds = (remainingMs + 999) / 1000; // round up so it shows 00:00 only at the end
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        timeLabel.setText(h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s));
    }

    private void startFinishedEffects() {
        beepsLeft = 3;
        flashOn = false;
        toggleFlash();
        flasher.play();
    }

    private void stopFinishedEffects() {
        flasher.stop();
        flashOn = false;
        center.setStyle(BG_NORMAL);
    }

    private void toggleFlash() {
        flashOn = !flashOn;
        center.setStyle(flashOn ? BG_FINISHED : BG_NORMAL);
        if (flashOn && beepsLeft > 0) {
            beepsLeft--;
            // System beep: no audio file needed
            try {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } catch (Throwable ignored) {
                // No sound available; the visual flash is enough
            }
        }
        if (!flashOn && beepsLeft == 0) {
            flasher.stop();
            center.setStyle(BG_FINISHED);
        }
    }
}
