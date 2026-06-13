package es.elprofesoremilio.zoomdraw;

import com.github.kwhat.jnativehook.GlobalScreen;
import dorkbox.util.CacheUtil;
import es.elprofesoremilio.zoomdraw.commands.StopAnnotationModeCommand;
import es.elprofesoremilio.zoomdraw.commands.ToggleAnnotationModeCommand;
import es.elprofesoremilio.zoomdraw.commands.ToggleRouletteModeCommand;
import es.elprofesoremilio.zoomdraw.config.AppConfig;
import es.elprofesoremilio.zoomdraw.core.AnnotationManager;
import es.elprofesoremilio.zoomdraw.input.GlobalKeyHook;
import es.elprofesoremilio.zoomdraw.utils.AppLogger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.SystemTray;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

public class AppLauncher extends Application {

    private GlobalKeyHook globalKeyHook;


    BufferedImage trayIcon;

    // Tray nativo de AWT (Windows)
    private java.awt.SystemTray awtSystemTray;
    private TrayIcon awtTrayIcon;
    private AnnotationManager annotationManager;

    // Dorkbox SystemTray (Linux / otros)
    private SystemTray dorkboxSystemTray;

    @Override
    public void start(Stage primaryStage) {
        // silenciar logs de JNativeHook
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
        ToggleRouletteModeCommand toggleRouletteCommand = new ToggleRouletteModeCommand(annotationManager);

        // Registrar el hook global
        globalKeyHook = new GlobalKeyHook(annotationManager, toggleCommand, stopCommand, toggleRouletteCommand);
        globalKeyHook.register();

        // Inicializar bandeja de sistema: nativa en Windows, Dorkbox en el resto
        createTrayIcon();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            initAwtSystemTray();
        } else {
            initDorkboxSystemTray();
        }

        annotationManager.toggleHelpWindow();
    }

    private void createTrayIcon() {
        trayIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = trayIcon.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Letra Z (Zoom)
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2d.setColor(new Color(116, 185, 255)); // Azul brillante
        g2d.drawString("Z", 1, 10);

        // Letra D (Draw)
        g2d.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2d.setColor(new Color(255, 234, 167)); // Amarillo suave
        g2d.drawString("D", 7, 15);

        g2d.dispose();
    }

    /** Usa java.awt.SystemTray (estable en Windows; evita el NPE de Dorkbox en TrayPopup.doShow). */
    private void initAwtSystemTray() {
        try {

            PopupMenu popup = new PopupMenu();
            java.awt.MenuItem helpItem = new java.awt.MenuItem("Ayuda");
            helpItem.addActionListener(event -> {
                annotationManager.toggleHelpWindow();
            });
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Salir");
            exitItem.addActionListener(e -> {
                Platform.exit();
                System.exit(0);
            });
            popup.add(helpItem);
            popup.add(exitItem);

            awtTrayIcon = new TrayIcon(trayIcon, "ZoomDraw", popup);
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

            new CacheUtil("ZoomDraw").clear();

            SystemTray.DEBUG = true;

            dorkboxSystemTray = SystemTray.get("ZoomDraw");
            if (dorkboxSystemTray != null) {
                AppLogger.log("Dorkbox SystemTray inicializado.");

                dorkboxSystemTray.setImage(trayIcon);
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