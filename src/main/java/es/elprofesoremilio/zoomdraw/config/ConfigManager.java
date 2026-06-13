package es.elprofesoremilio.zoomdraw.config;

import es.elprofesoremilio.zoomdraw.utils.AppLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class ConfigManager {

    private static final String CONFIG_FILE = "config.properties";
    private static final Properties props = new Properties();

    public static void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
                props.load(reader);
                AppLogger.log("Configuración cargada desde " + file.getAbsolutePath());
            } catch (IOException e) {
                AppLogger.logError("Error cargando config.properties", e);
            }
        }
        applyToAppConfig();
    }

    public static void save() {
        File file = new File(CONFIG_FILE);
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            props.store(writer, "ZoomDraw configuration");
            AppLogger.log("Configuración guardada en " + file.getAbsolutePath());
        } catch (IOException e) {
            AppLogger.logError("Error guardando config.properties", e);
        }
    }

    public static String get(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    public static void set(String key, String value) {
        props.setProperty(key, value);
    }

    public static void setAndSave(String key, String value) {
        set(key, value);
        save();
    }

    private static void applyToAppConfig() {
        AppConfig.rouletteDefaultFile = get("roulette_default_file", AppConfig.ROULETTE_DEFAULT_FILE_DEFAULT);

        String durationStr = get("animation_duration_ms", String.valueOf(AppConfig.ROULETTE_ANIMATION_DURATION_MS_DEFAULT));
        try {
            int duration = Integer.parseInt(durationStr);
            if (duration >= AppConfig.ROULETTE_ANIMATION_DURATION_MS_MIN && duration <= AppConfig.ROULETTE_ANIMATION_DURATION_MS_MAX) {
                AppConfig.rouletteAnimationDurationMs = duration;
            }
        } catch (NumberFormatException e) {
            AppConfig.rouletteAnimationDurationMs = AppConfig.ROULETTE_ANIMATION_DURATION_MS_DEFAULT;
        }

        String skipStr = get("skip_animation", String.valueOf(AppConfig.ROULETTE_SKIP_ANIMATION_DEFAULT));
        AppConfig.rouletteSkipAnimation = Boolean.parseBoolean(skipStr);

        AppConfig.rouletteColorPalette = get("color_palette", AppConfig.ROULETTE_COLOR_PALETTE_DEFAULT);

        String repeatStr = get("repeat_mode_default", String.valueOf(AppConfig.ROULETTE_REPEAT_MODE_DEFAULT));
        AppConfig.rouletteRepeatModeDefault = Boolean.parseBoolean(repeatStr);

        AppConfig.rouletteCustomPaletteName = get("custom_palette_name", "Custom");
        AppConfig.rouletteCustomPaletteColors = get("custom_palette_colors", "");
    }
}
