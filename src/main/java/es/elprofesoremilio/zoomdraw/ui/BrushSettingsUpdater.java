package es.elprofesoremilio.zoomdraw.ui;

/**
 * Interface for components that can update brush settings.
 * Used by AnnotationInputHandler to notify when brush properties (color, width, opacity) change.
 */
public interface BrushSettingsUpdater {
    void updateBrushSettings();
}
