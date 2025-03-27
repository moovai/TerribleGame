package com.terriblegame.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import java.awt.Color;
import java.awt.Font;

/**
 * Manages application configuration loaded from a .env file or system properties.
 * Provides static final constants for various game parameters, database settings,
 * and UI styling. Uses Dotenv library for loading and provides default values.
 */
public class AppConfig {
    private static final Dotenv dotenv;

    static {
        Dotenv loadedDotenv = null;
        try {
            loadedDotenv = Dotenv.configure()
                                .ignoreIfMissing()
                                .systemProperties()
                                .load();
            System.out.println(".env file loaded successfully (or ignored if missing).");
        } catch (DotenvException e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }
        dotenv = loadedDotenv;
    }

    private static String loadStringEnv(String varName, String defaultValue) {
        if (dotenv == null) return defaultValue;
        return dotenv.get(varName, defaultValue);
    }

    private static int loadIntEnv(String varName, int defaultValue) {
        if (dotenv == null) return defaultValue;
        try {
            return Integer.parseInt(dotenv.get(varName, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid integer format for env var '" + varName + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    private static double loadDoubleEnv(String varName, double defaultValue) {
        if (dotenv == null) return defaultValue;
        try {
            return Double.parseDouble(dotenv.get(varName, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid double format for env var '" + varName + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    private static Color loadColorEnv(String varName, Color defaultValue) {
        if (dotenv == null) return defaultValue;
        String colorStr = dotenv.get(varName, "");
        if (colorStr.isEmpty()) {
            return defaultValue;
        }
        try {
            if (colorStr.startsWith("#")) {
                if (colorStr.length() == 9) {
                    long colorValue = Long.parseLong(colorStr.substring(1), 16);
                    int alpha = (int) ((colorValue >> 24) & 0xFF);
                    int red = (int) ((colorValue >> 16) & 0xFF);
                    int green = (int) ((colorValue >> 8) & 0xFF);
                    int blue = (int) (colorValue & 0xFF);
                    return new Color(red, green, blue, alpha);
                } else if (colorStr.length() == 7) {
                    return Color.decode(colorStr);
                } else {
                    throw new NumberFormatException("Invalid hex color format length. Expected #RRGGBB or #AARRGGBB.");
                }
            } else {
                try {
                    return (Color) Color.class.getField(colorStr.toUpperCase()).get(null);
                } catch (Exception fieldEx) {
                    System.err.println("Warning: Could not parse color name '" + colorStr + "'. Using default.");
                    return defaultValue;
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid color format for env var '" + varName + "' (use #RRGGBB, #AARRGGBB, or standard names). Using default.");
            return defaultValue;
        }
    }

    private static int loadFontStyleEnv(String varName, int defaultValue) {
        if (dotenv == null) return defaultValue;
        String styleStr = dotenv.get(varName, "").toUpperCase();
        switch (styleStr) {
            case "PLAIN": return Font.PLAIN;
            case "BOLD": return Font.BOLD;
            case "ITALIC": return Font.ITALIC;
            case "BOLD_ITALIC": return Font.BOLD | Font.ITALIC;
            default:
                System.err.println("Warning: Invalid font style for env var '" + varName + "' (use PLAIN, BOLD, ITALIC, BOLD_ITALIC). Using default.");
                return defaultValue;
        }
    }

    // --- Game Window & Timing ---
    public static final String APP_TITLE = loadStringEnv("APP_TITLE", "Secure Space Game");
    public static final int GAME_WIDTH = loadIntEnv("GAME_WIDTH", 800);
    public static final int GAME_HEIGHT = loadIntEnv("GAME_HEIGHT", 600);
    public static final int GAME_TICK_MS = loadIntEnv("GAME_TICK_MS", 16);
    public static final int BOTTOM_UI_BUFFER = loadIntEnv("BOTTOM_UI_BUFFER", 30);

    // --- Database ---
    public static final String DATABASE_URL = loadStringEnv("DATABASE_URL", "jdbc:sqlite:secure_game_data.db");
    public static final String JDBC_DRIVER = loadStringEnv("JDBC_DRIVER", "org.sqlite.JDBC");
    public static final int HIGH_SCORE_LIMIT = loadIntEnv("HIGH_SCORE_LIMIT", 10);
    public static final int BCRYPT_LOG_ROUNDS = loadIntEnv("BCRYPT_LOG_ROUNDS", 12);

    // --- Player ---
    public static final int PLAYER_WIDTH = loadIntEnv("PLAYER_WIDTH", 30);
    public static final int PLAYER_HEIGHT = loadIntEnv("PLAYER_HEIGHT", 15);
    public static final int PLAYER_SPEED = loadIntEnv("PLAYER_SPEED", 5);
    public static final Color PLAYER_COLOR = loadColorEnv("PLAYER_COLOR", Color.CYAN);
    public static final int INITIAL_LIVES = loadIntEnv("INITIAL_LIVES", 3);

    // --- Bullet ---
    public static final int BULLET_WIDTH = loadIntEnv("BULLET_WIDTH", 5);
    public static final int BULLET_HEIGHT = loadIntEnv("BULLET_HEIGHT", 10);
    public static final int BULLET_SPEED = loadIntEnv("BULLET_SPEED", 8);
    public static final Color BULLET_COLOR = loadColorEnv("BULLET_COLOR", Color.YELLOW);

    // --- Enemy ---
    public static final int MAX_ENEMIES = loadIntEnv("MAX_ENEMIES", 15);
    public static final double ENEMY_SPAWN_CHANCE = loadDoubleEnv("ENEMY_SPAWN_CHANCE", 0.05);
    public static final int ENEMY_BASE_SPEED = loadIntEnv("ENEMY_BASE_SPEED", 2);
    public static final int ENEMY_SPEED_VARIATION = loadIntEnv("ENEMY_SPEED_VARIATION", 3);
    public static final int ENEMY_BASE_SIZE = loadIntEnv("ENEMY_BASE_SIZE", 20);
    public static final int ENEMY_SIZE_VARIATION = loadIntEnv("ENEMY_SIZE_VARIATION", 30);
    public static final int ENEMY_SCORE_VALUE = loadIntEnv("ENEMY_SCORE_VALUE", 10);
    public static final Color ENEMY_COLOR = loadColorEnv("ENEMY_COLOR", Color.RED);

    // --- PowerUp ---
    public static final int MAX_POWERUPS = loadIntEnv("MAX_POWERUPS", 5);
    public static final double POWERUP_SPAWN_CHANCE = loadDoubleEnv("POWERUP_SPAWN_CHANCE", 0.02);
    public static final int POWERUP_WIDTH = loadIntEnv("POWERUP_WIDTH", 15);
    public static final int POWERUP_HEIGHT = loadIntEnv("POWERUP_HEIGHT", 15);
    public static final int POWERUP_BASE_SPEED = loadIntEnv("POWERUP_BASE_SPEED", 3);
    public static final int POWERUP_SPEED_VARIATION = loadIntEnv("POWERUP_SPEED_VARIATION", 2);
    public static final int POWERUP_SCORE_VALUE = loadIntEnv("POWERUP_SCORE_VALUE", 50);
    public static final Color POWERUP_COLOR = loadColorEnv("POWERUP_COLOR", Color.GREEN);

    // --- UI Elements (Login Screen) ---
    public static final Color LOGIN_BACKGROUND_COLOR = loadColorEnv("LOGIN_BACKGROUND_COLOR", Color.DARK_GRAY);
    public static final Color LOGIN_LABEL_COLOR = loadColorEnv("LOGIN_LABEL_COLOR", Color.WHITE);
    public static final Color LOGIN_STATUS_ERROR_COLOR = loadColorEnv("LOGIN_STATUS_ERROR_COLOR", Color.RED);
    public static final Color LOGIN_STATUS_SUCCESS_COLOR = loadColorEnv("LOGIN_STATUS_SUCCESS_COLOR", Color.GREEN);
    public static final Color LOGIN_HIGHSCORE_FG_COLOR = loadColorEnv("LOGIN_HIGHSCORE_FG_COLOR", Color.CYAN);
    public static final Color LOGIN_HIGHSCORE_BG_COLOR = loadColorEnv("LOGIN_HIGHSCORE_BG_COLOR", Color.BLACK);
    public static final String LOGIN_HIGHSCORE_FONT_NAME = loadStringEnv("LOGIN_HIGHSCORE_FONT_NAME", "Monospaced");
    public static final int LOGIN_HIGHSCORE_FONT_STYLE = loadFontStyleEnv("LOGIN_HIGHSCORE_FONT_STYLE", Font.PLAIN);
    public static final int LOGIN_HIGHSCORE_FONT_SIZE = loadIntEnv("LOGIN_HIGHSCORE_FONT_SIZE", 12);

    // --- UI Elements (Game Panel) ---
    public static final Color GAME_BACKGROUND_COLOR = loadColorEnv("GAME_BACKGROUND_COLOR", Color.BLACK);
    public static final Color GAME_UI_TEXT_COLOR = loadColorEnv("GAME_UI_TEXT_COLOR", Color.WHITE);
    public static final String GAME_UI_FONT_NAME = loadStringEnv("GAME_UI_FONT_NAME", "Consolas");
    public static final int GAME_UI_FONT_STYLE = loadFontStyleEnv("GAME_UI_FONT_STYLE", Font.BOLD);
    public static final int GAME_UI_FONT_SIZE = loadIntEnv("GAME_UI_FONT_SIZE", 16);

    public static final Color GAME_OVER_TEXT_COLOR = loadColorEnv("GAME_OVER_TEXT_COLOR", Color.YELLOW);
    public static final String GAME_OVER_FONT_NAME = loadStringEnv("GAME_OVER_FONT_NAME", "Arial");
    public static final int GAME_OVER_LARGE_FONT_STYLE = loadFontStyleEnv("GAME_OVER_LARGE_FONT_STYLE", Font.BOLD);
    public static final int GAME_OVER_LARGE_FONT_SIZE = loadIntEnv("GAME_OVER_LARGE_FONT_SIZE", 48);
    public static final int GAME_OVER_MEDIUM_FONT_STYLE = loadFontStyleEnv("GAME_OVER_MEDIUM_FONT_STYLE", Font.BOLD);
    public static final int GAME_OVER_MEDIUM_FONT_SIZE = loadIntEnv("GAME_OVER_MEDIUM_FONT_SIZE", 24);
    public static final int GAME_OVER_SMALL_FONT_STYLE = loadFontStyleEnv("GAME_OVER_SMALL_FONT_STYLE", Font.PLAIN);
    public static final int GAME_OVER_SMALL_FONT_SIZE = loadIntEnv("GAME_OVER_SMALL_FONT_SIZE", 16);

    public static final Color PAUSE_OVERLAY_COLOR = loadColorEnv("PAUSE_OVERLAY_COLOR", new Color(0, 0, 0, 150));
    public static final Color PAUSE_TEXT_COLOR = loadColorEnv("PAUSE_TEXT_COLOR", Color.WHITE);
    public static final String PAUSE_FONT_NAME = loadStringEnv("PAUSE_FONT_NAME", "Arial");
    public static final int PAUSE_LARGE_FONT_STYLE = loadFontStyleEnv("PAUSE_LARGE_FONT_STYLE", Font.BOLD);
    public static final int PAUSE_LARGE_FONT_SIZE = loadIntEnv("PAUSE_LARGE_FONT_SIZE", 48);
    public static final int PAUSE_SMALL_FONT_STYLE = loadFontStyleEnv("PAUSE_SMALL_FONT_STYLE", Font.PLAIN);
    public static final int PAUSE_SMALL_FONT_SIZE = loadIntEnv("PAUSE_SMALL_FONT_SIZE", 16);

    // --- Card Layout IDs ---
    public static final String LOGIN_PANEL_ID = loadStringEnv("LOGIN_PANEL_ID", "LoginPanel");
    public static final String GAME_PANEL_ID = loadStringEnv("GAME_PANEL_ID", "GamePanel");
}