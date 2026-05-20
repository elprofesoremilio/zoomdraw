package es.elprofesoremilio.zoomdraw.utils;

import es.elprofesoremilio.zoomdraw.config.AppConfig;

public class AppLogger {
    public static void log(String message) {
        if (AppConfig.LOG_ENABLED) {
            System.out.println("[ZoomDraw] " + message);
        }
    }

    public static void logError(String message) {
        if (AppConfig.LOG_ENABLED) {
            System.err.println("[ZoomDraw ERROR] " + message);
        }
    }

    public static void logError(String message, Throwable t) {
        if (AppConfig.LOG_ENABLED) {
            System.err.println("[ZoomDraw ERROR] " + message);
            t.printStackTrace(System.err);
        }
    }
}
