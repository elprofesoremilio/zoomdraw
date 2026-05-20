package es.elprofesoremilio.zoomdraw;

import com.github.kwhat.jnativehook.GlobalScreen;
import es.elprofesoremilio.zoomdraw.commands.StopAnnotationModeCommand;
import es.elprofesoremilio.zoomdraw.commands.ToggleAnnotationModeCommand;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.input.GlobalKeyHook;
import es.elprofesoremilio.zoomdraw.utils.AppLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class AppLauncher extends Application {

    private AnnotationManager annotationManager;
    private GlobalKeyHook globalKeyHook;

    @Override
    public void start(Stage primaryStage) {
        // Silenciar logs de JNativeHook
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(AppConfig.JNATIVEHOOK_LOG_LEVEL);
        logger.setUseParentHandlers(false);

        // Ocultar stage primario e impedir cierre implícito
        primaryStage.hide();
        Platform.setImplicitExit(false);

        // Inicializar el mediador central
        annotationManager = new AnnotationManager();

        // Crear comandos
        ToggleAnnotationModeCommand toggleCommand = new ToggleAnnotationModeCommand(annotationManager);
        StopAnnotationModeCommand stopCommand = new StopAnnotationModeCommand(annotationManager);

        // Registrar el hook global
        globalKeyHook = new GlobalKeyHook(annotationManager, toggleCommand, stopCommand);
        globalKeyHook.register();

        es.elprofesoremilio.zoomdraw.utils.AppLogger.log("SystemTray support is " + (java.awt.SystemTray.isSupported() ? "enabled" : "disabled"));
        // System Tray
        if (java.awt.SystemTray.isSupported()) {
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            
            // Generar un icono simple rojo para el tray icon
            java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g2d = img.createGraphics();
            g2d.setColor(java.awt.Color.RED);
            g2d.fillOval(0,0,16,16);
            g2d.setColor(java.awt.Color.WHITE);
            g2d.drawOval(0,0,15,15);
            g2d.dispose();
            
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(img, "ZoomDraw");
            trayIcon.setImageAutoSize(true);
            
            java.awt.PopupMenu popup = new java.awt.PopupMenu();
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Salir");
            exitItem.addActionListener(e -> {
                Platform.exit();
                System.exit(0);
            });
            popup.add(exitItem);
            
            trayIcon.setPopupMenu(popup);
            try {
                tray.add(trayIcon);
            } catch (java.awt.AWTException e) {
                AppLogger.logError("Error al agregar el icono al tray");
            }
        }

        annotationManager.toggleHelpWindow();
    }

    @Override
    public void stop() {
        globalKeyHook.unregister();
    }
}