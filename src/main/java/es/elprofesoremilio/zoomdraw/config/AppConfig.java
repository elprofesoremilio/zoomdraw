package es.elprofesoremilio.zoomdraw.config;

import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;

import java.util.logging.Level;

public class AppConfig {

    // System Properties
    public static final String UI_SCALE = "1.0";
    public static final String JDK_GTK_VERSION = "3";
    public static final boolean AWT_HEADLESS = false;
    public static final boolean JAVA2D_OPENGL = false;

    // JNativeHook Logger Level
    public static final Level JNATIVEHOOK_LOG_LEVEL = Level.OFF;

    // DrawingCanvas Fallback Background
    public static final Color FALLBACK_BACKGROUND_COLOR = Color.WHITE;
    public static final double FALLBACK_BACKGROUND_OPACITY = 0.01;

    // CanvasRenderer
    public static final Color RENDERER_BACKGROUND_COLOR = Color.BLACK;
    public static final Color RENDERER_MAIN_TEXT_COLOR = Color.WHITE;
    public static final String RENDERER_MAIN_FONT_FAMILY = "System";
    public static final FontWeight RENDERER_MAIN_FONT_WEIGHT = FontWeight.BOLD;
    public static final double RENDERER_MAIN_FONT_SIZE = 28;
    public static final String RENDERER_MAIN_TEXT = "Pulse ESC para salir";
    public static final Color RENDERER_SUB_TEXT_COLOR = Color.LIGHTGRAY;
    public static final String RENDERER_SUB_FONT_FAMILY = "System";
    public static final FontWeight RENDERER_SUB_FONT_WEIGHT = FontWeight.NORMAL;
    public static final double RENDERER_SUB_FONT_SIZE = 16;
    public static final String RENDERER_SUB_TEXT = "(o CTRL+1 para activar/desactivar)";
    public static final double RENDERER_SUB_TEXT_OFFSET_Y = 44;

    // AnnotationInputHandler
    public static final double LINE_WIDTH_SCROLL_STEP = 2.0;
    public static final double LINE_WIDTH_MIN = 1.0;
    public static final double LINE_WIDTH_MAX = 50.0;
    public static final double GLOBAL_OPACITY_STEP = 0.1;
    public static final double GLOBAL_OPACITY_MIN = 0.1;
    public static final double GLOBAL_OPACITY_MAX = 1.0;
    
    // Text Tool
    public static final double TEXT_SIZE_MULTIPLIER = 10.0;

    // Help Window
    public static final double HELP_WINDOW_OPACITY_MIN = 0.2;
    public static final double HELP_WINDOW_OPACITY_MAX = 1.0;
    public static final double HELP_WINDOW_OPACITY_DEFAULT = 0.7;

    // KeyCodes for colors
    public static final KeyCode COLOR_RED_KEY = KeyCode.R;
    public static final KeyCode COLOR_GREEN_KEY = KeyCode.G;
    public static final KeyCode COLOR_BLUE_KEY = KeyCode.B;
    public static final KeyCode COLOR_YELLOW_KEY = KeyCode.Y;
    public static final KeyCode COLOR_ORANGE_KEY = KeyCode.O;
    public static final KeyCode COLOR_MAGENTA_KEY = KeyCode.M;
    public static final KeyCode COLOR_BLACK_KEY = KeyCode.K;
    public static final KeyCode COLOR_WHITE_KEY = KeyCode.W;
    public static final KeyCode COLOR_CYAN_KEY = KeyCode.C;
    public static final KeyCode COLOR_PINK_KEY = KeyCode.P;
    public static final KeyCode COLOR_GREY_KEY = KeyCode.L;
    public static final KeyCode COLOR_DARK_GREY_KEY = KeyCode.D;

    // KeyCodes for line width
    public static final KeyCode LINE_WIDTH_INCREASE_KEY_1 = KeyCode.UP;
    public static final KeyCode LINE_WIDTH_INCREASE_KEY_2 = KeyCode.PLUS;
    public static final KeyCode LINE_WIDTH_INCREASE_KEY_3 = KeyCode.ADD;
    public static final KeyCode LINE_WIDTH_DECREASE_KEY_1 = KeyCode.DOWN;
    public static final KeyCode LINE_WIDTH_DECREASE_KEY_2 = KeyCode.MINUS;
    public static final KeyCode LINE_WIDTH_DECREASE_KEY_3 = KeyCode.SUBTRACT;

    // Colors
    public static final Color GREEN = new Color(0,1,0.2,1);
    public static final Color RED = new Color(1,0,0,1);
    public static final Color BLUE = new Color(0.1,0.4,1,1);
    public static final Color YELLOW = new Color(1,1,0,1);
    public static final Color ORANGE = new Color(1,0.45,0,1);
    public static final Color PINK = new Color(1,0.6,1,1);
    public static final Color CYAN = new Color(0,0.8,1,1);
    public static final Color MAGENTA = new Color(1,0,1,1);
    public static final Color GREY = new Color(0.7,0.7,0.7,1);
    public static final Color DARK_GREY = new Color(0.4,0.4,0.4,1);
    public static final Color BLACK = new Color(0,0,0,1);
    public static final Color WHITE = new Color(1,1,1,1);

    // KeyCodes for global opacity
    public static final KeyCode GLOBAL_OPACITY_DECREASE_KEY = KeyCode.LEFT;
    public static final KeyCode GLOBAL_OPACITY_INCREASE_KEY = KeyCode.RIGHT;

    // KeyCode for exit
    public static final KeyCode EXIT_KEY = KeyCode.ESCAPE;
    public static final Color DEFAULT_COLOR = AppConfig.RED;
    public static final double DEFAULT_LINE_WIDTH = 3.0;
    public static final double DEFAULT_OPACITY = 1.0;


    public static void loadSystemProperties() {
        System.setProperty("sun.java2d.uiScale", UI_SCALE);
        System.setProperty("jdk.gtk.version", JDK_GTK_VERSION);
        System.setProperty("java.awt.headless", String.valueOf(AWT_HEADLESS));
        System.setProperty("sun.java2d.opengl", String.valueOf(JAVA2D_OPENGL));
    }
}