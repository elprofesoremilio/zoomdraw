package es.elprofesoremilio.zoomdraw.utils;

import es.elprofesoremilio.zoomdraw.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class AppLogger {
    
    static {
        configureLogLevel();
    }

    /**
     * Programmatically configures the root Logback logger level based on AppConfig.APP_LOG_LEVEL.
     */
    public static void configureLogLevel() {
        try {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.setLevel(Level.toLevel(AppConfig.APP_LOG_LEVEL, Level.INFO));
        } catch (Throwable t) {
            System.err.println("[ZoomDraw] Error configuring logging level: " + t.getMessage());
        }
    }

    /**
     * Logs an info-level message using the caller's class name as the logger.
     * 
     * @param message the message to log
     */
    public static void log(String message) {
        if (AppConfig.LOG_ENABLED) {
            String callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(s -> s.skip(1)
                            .findFirst()
                            .map(f -> f.getDeclaringClass().getName())
                            .orElse("ZoomDraw"));
            LoggerFactory.getLogger(callerClass).info(message);
        }
    }

    /**
     * Logs an error-level message using the caller's class name as the logger.
     * 
     * @param message the message to log
     */
    public static void logError(String message) {
        if (AppConfig.LOG_ENABLED) {
            String callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(s -> s.skip(1)
                            .findFirst()
                            .map(f -> f.getDeclaringClass().getName())
                            .orElse("ZoomDraw"));
            LoggerFactory.getLogger(callerClass).error(message);
        }
    }

    /**
     * Logs an error-level message with an associated Throwable using the caller's class name.
     * 
     * @param message the message to log
     * @param t the associated Throwable
     */
    public static void logError(String message, Throwable t) {
        if (AppConfig.LOG_ENABLED) {
            String callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(s -> s.skip(1)
                            .findFirst()
                            .map(f -> f.getDeclaringClass().getName())
                            .orElse("ZoomDraw"));
            LoggerFactory.getLogger(callerClass).error(message, t);
        }
    }
}
