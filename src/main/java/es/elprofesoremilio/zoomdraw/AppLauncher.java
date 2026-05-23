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

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;
import java.awt.PopupMenu;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.util.logging.Logger;

public class AppLauncher extends Application {

    private GlobalKeyHook globalKeyHook;

    // Tray nativo de AWT (Windows)
    private java.awt.SystemTray awtSystemTray;
    private TrayIcon awtTrayIcon;
    private AnnotationManager annotationManager;

    // Dorkbox SystemTray (Linux / otros)
    private SystemTray dorkboxSystemTray;

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

        // Inicializar bandeja de sistema: nativa en Windows, Dorkbox en el resto
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            initAwtSystemTray();
        } else {
            initDorkboxSystemTray();
        }

        annotationManager.toggleHelpWindow();
    }

    /** Usa java.awt.SystemTray (estable en Windows; evita el NPE de Dorkbox en TrayPopup.doShow). */
    private void initAwtSystemTray() {
        try {
            BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = img.createGraphics();
            g2d.setColor(Color.RED);
            g2d.fillOval(0, 0, 16, 16);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(0, 0, 15, 15);
            g2d.dispose();

            PopupMenu popup = new PopupMenu();
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Salir");
            exitItem.addActionListener(e -> {
                Platform.exit();
                System.exit(0);
            });
            popup.add(exitItem);

            awtTrayIcon = new TrayIcon(img, "ZoomDraw", popup);
            awtTrayIcon.setImageAutoSize(true);

            awtSystemTray = java.awt.SystemTray.getSystemTray();
            awtSystemTray.add(awtTrayIcon);

            AppLogger.log("java.awt.SystemTray (Windows nativo) inicializado correctamente.");
            AppConfig.systemTrayLoaded = true;
        } catch (Exception e) {
            AppLogger.logError("Error al inicializar java.awt.SystemTray", e);
            AppConfig.systemTrayLoaded = false;
        }
    }

    /** Usa Dorkbox SystemTray para soporte de GTK3/X11 en Linux y otros sistemas. */
    private void initDorkboxSystemTray() {
        try {
            dorkboxSystemTray = SystemTray.get("ZoomDraw");
            if (dorkboxSystemTray != null) {
                AppLogger.log("Dorkbox SystemTray inicializado.");

                BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();
                g2d.setColor(Color.RED);
                g2d.fillOval(0, 0, 16, 16);
                g2d.setColor(Color.WHITE);
                g2d.drawOval(0, 0, 15, 15);
                g2d.dispose();

                dorkboxSystemTray.setImage(img);
                dorkboxSystemTray.setTooltip("ZoomDraw");

                dorkboxSystemTray.getMenu().add(new MenuItem("Ayuda", e -> {
                    annotationManager.toggleHelpWindow();
                }));
                dorkboxSystemTray.getMenu().add(new MenuItem("Salir", e -> {
                    Platform.exit();
                    System.exit(0);
                }));
                AppConfig.systemTrayLoaded = true;
            } else {
                AppLogger.log("Advertencia: El SystemTray de Dorkbox no está disponible.");
                AppConfig.systemTrayLoaded = false;
            }
        } catch (Exception e) {
            AppLogger.logError("Error al inicializar el SystemTray de Dorkbox", e);
        }
    }

    @Override
    public void stop() {
        globalKeyHook.unregister();
        if (awtSystemTray != null && awtTrayIcon != null) {
            awtSystemTray.remove(awtTrayIcon);
        }
        if (dorkboxSystemTray != null) {
            dorkboxSystemTray.shutdown();
        }
    }
}