import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.Random;
// import java.util.Collections; // Potentially needed for unmodifiable lists, but sticking to current pattern for now

import org.mindrot.jbcrypt.BCrypt; // Import for password hashing

// --- Configuration Loading ---

/**
 * Manages application configuration loaded from a .env file or system properties.
 * Provides static final constants for various game parameters, database settings,
 * and UI styling. Uses Dotenv library for loading and provides default values.
 */
class AppConfig {
    private static final Dotenv dotenv;

    /**
     * Static initializer block to load the .env file configuration.
     * It attempts to load configuration from a .env file in the project root.
     * If the file is missing, it's ignored. If loading fails for other reasons,
     * an error is printed. System properties can also override .env values.
     */
    static {
        Dotenv loadedDotenv = null;
        try {
            // Configure Dotenv to ignore missing .env files and look at system properties.
            loadedDotenv = Dotenv.configure()
                                 .ignoreIfMissing()
                                 .systemProperties()
                                 .load();
            System.out.println(".env file loaded successfully (or ignored if missing).");
        } catch (DotenvException e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }
        dotenv = loadedDotenv; // Assign the loaded (or null) dotenv instance.
    }

    /**
     * Loads a String value from the environment variables or returns a default value.
     *
     * @param varName      The name of the environment variable.
     * @param defaultValue The default value to return if the variable is not found or dotenv failed to load.
     * @return The loaded String value or the default value.
     */
    private static String loadStringEnv(String varName, String defaultValue) {
        if (dotenv == null) return defaultValue;
        return dotenv.get(varName, defaultValue);
    }

    /**
     * Loads an integer value from the environment variables or returns a default value.
     * Handles potential NumberFormatException during parsing.
     *
     * @param varName      The name of the environment variable.
     * @param defaultValue The default value to return if the variable is not found, dotenv failed, or parsing fails.
     * @return The loaded integer value or the default value.
     */
    private static int loadIntEnv(String varName, int defaultValue) {
        if (dotenv == null) return defaultValue;
        try {
            return Integer.parseInt(dotenv.get(varName, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid integer format for env var '" + varName + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Loads a double value from the environment variables or returns a default value.
     * Handles potential NumberFormatException during parsing.
     *
     * @param varName      The name of the environment variable.
     * @param defaultValue The default value to return if the variable is not found, dotenv failed, or parsing fails.
     * @return The loaded double value or the default value.
     */
    private static double loadDoubleEnv(String varName, double defaultValue) {
         if (dotenv == null) return defaultValue;
        try {
            return Double.parseDouble(dotenv.get(varName, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid double format for env var '" + varName + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    /**
     * Loads a Color value from the environment variables or returns a default value.
     * Supports hex formats (#RRGGBB, #AARRGGBB) and standard color names (case-insensitive).
     *
     * @param varName      The name of the environment variable.
     * @param defaultValue The default Color to return if the variable is not found, dotenv failed, or parsing fails.
     * @return The loaded Color object or the default Color.
     */
    private static Color loadColorEnv(String varName, Color defaultValue) {
        if (dotenv == null) return defaultValue;
        String colorStr = dotenv.get(varName, "");
        if (colorStr.isEmpty()) {
            return defaultValue;
        }
        try {
            if (colorStr.startsWith("#")) {
                if (colorStr.length() == 9) { // Format #AARRGGBB
                    // Parse hexadecimal string to long, then extract ARGB components
                    long colorValue = Long.parseLong(colorStr.substring(1), 16);
                    int alpha = (int) ((colorValue >> 24) & 0xFF);
                    int red = (int) ((colorValue >> 16) & 0xFF);
                    int green = (int) ((colorValue >> 8) & 0xFF);
                    int blue = (int) (colorValue & 0xFF);
                    return new Color(red, green, blue, alpha);
                } else if (colorStr.length() == 7) { // Format #RRGGBB
                     // Use standard Java Color decoding for #RRGGBB
                     return Color.decode(colorStr);
                } else {
                    throw new NumberFormatException("Invalid hex color format length. Expected #RRGGBB or #AARRGGBB.");
                }
            } else {
                // Attempt to parse known color names using reflection (case-insensitive)
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

     /**
      * Loads a font style integer (e.g., Font.BOLD) from the environment variables or returns a default value.
      * Supports style names: "PLAIN", "BOLD", "ITALIC", "BOLD_ITALIC" (case-insensitive).
      *
      * @param varName      The name of the environment variable.
      * @param defaultValue The default font style (e.g., Font.PLAIN) to return if the variable is not found,
      *                     dotenv failed, or the value is unrecognized.
      * @return The loaded font style integer or the default value.
      */
     private static int loadFontStyleEnv(String varName, int defaultValue) {
        if (dotenv == null) return defaultValue;
        String styleStr = dotenv.get(varName, "").toUpperCase(); // Load and convert to uppercase for case-insensitivity
        switch (styleStr) {
            case "PLAIN": return Font.PLAIN;
            case "BOLD": return Font.BOLD;
            case "ITALIC": return Font.ITALIC;
            case "BOLD_ITALIC": return Font.BOLD | Font.ITALIC; // Combine flags for BOLD_ITALIC
            default:
                // Log a warning if the style string is not recognized
                System.err.println("Warning: Invalid font style for env var '" + varName + "' (use PLAIN, BOLD, ITALIC, BOLD_ITALIC). Using default.");
                return defaultValue;
        }
    }

    // --- Game Window & Timing ---
    /** The title displayed in the game window's title bar. */
    public static final String APP_TITLE = loadStringEnv("APP_TITLE", "Secure Space Game");
    /** The width of the game window in pixels. */
    public static final int GAME_WIDTH = loadIntEnv("GAME_WIDTH", 800);
    /** The height of the game window in pixels. */
    public static final int GAME_HEIGHT = loadIntEnv("GAME_HEIGHT", 600);
    /** The duration of each game tick in milliseconds (controls game speed and frame rate). */
    public static final int GAME_TICK_MS = loadIntEnv("GAME_TICK_MS", 16); // Approx 60 FPS
    /** The buffer space at the bottom of the screen reserved for UI elements, preventing game objects from overlapping. */
    public static final int BOTTOM_UI_BUFFER = loadIntEnv("BOTTOM_UI_BUFFER", 30);

    // --- Database ---
    /** The JDBC URL for connecting to the game's database (defaults to an SQLite file). */
    public static final String DATABASE_URL = loadStringEnv("DATABASE_URL", "jdbc:sqlite:secure_game_data.db");
    /** The fully qualified name of the JDBC driver class to use (defaults to SQLite driver). */
    public static final String JDBC_DRIVER = loadStringEnv("JDBC_DRIVER", "org.sqlite.JDBC");
    /** The maximum number of high scores to retrieve and display. */
    public static final int HIGH_SCORE_LIMIT = loadIntEnv("HIGH_SCORE_LIMIT", 10);
    /** The log rounds (work factor) for the BCrypt password hashing algorithm. Higher values are more secure but slower. */
    public static final int BCRYPT_LOG_ROUNDS = loadIntEnv("BCRYPT_LOG_ROUNDS", 12);

    // --- Player ---
    /** The width of the player's spaceship in pixels. */
    public static final int PLAYER_WIDTH = loadIntEnv("PLAYER_WIDTH", 30);
    /** The height of the player's spaceship in pixels. */
    public static final int PLAYER_HEIGHT = loadIntEnv("PLAYER_HEIGHT", 15);
    /** The speed of the player's movement in pixels per game tick. */
    public static final int PLAYER_SPEED = loadIntEnv("PLAYER_SPEED", 5);
    /** The color of the player's spaceship. */
    public static final Color PLAYER_COLOR = loadColorEnv("PLAYER_COLOR", Color.CYAN);
    /** The initial number of lives the player starts with. */
    public static final int INITIAL_LIVES = loadIntEnv("INITIAL_LIVES", 3);

    // --- Bullet ---
    /** The width of a player's bullet in pixels. */
    public static final int BULLET_WIDTH = loadIntEnv("BULLET_WIDTH", 5);
    /** The height of a player's bullet in pixels. */
    public static final int BULLET_HEIGHT = loadIntEnv("BULLET_HEIGHT", 10);
    /** The speed of a player's bullet in pixels per game tick. */
    public static final int BULLET_SPEED = loadIntEnv("BULLET_SPEED", 8);
    /** The color of the player's bullets. */
    public static final Color BULLET_COLOR = loadColorEnv("BULLET_COLOR", Color.YELLOW);

    // --- Enemy ---
    /** The maximum number of enemies allowed on screen at any time. */
    public static final int MAX_ENEMIES = loadIntEnv("MAX_ENEMIES", 15);
    /** The probability (0.0 to 1.0) of a new enemy spawning in each game tick. */
    public static final double ENEMY_SPAWN_CHANCE = loadDoubleEnv("ENEMY_SPAWN_CHANCE", 0.05);
    /** The base speed of enemies in pixels per game tick. */
    public static final int ENEMY_BASE_SPEED = loadIntEnv("ENEMY_BASE_SPEED", 2);
    /** The maximum additional random speed added to the base speed for enemies. */
    public static final int ENEMY_SPEED_VARIATION = loadIntEnv("ENEMY_SPEED_VARIATION", 3);
    /** The base size (width and height) of enemies in pixels. */
    public static final int ENEMY_BASE_SIZE = loadIntEnv("ENEMY_BASE_SIZE", 20);
    /** The maximum additional random size added to the base size for enemies. */
    public static final int ENEMY_SIZE_VARIATION = loadIntEnv("ENEMY_SIZE_VARIATION", 30);
    /** The score awarded to the player for destroying an enemy. */
    public static final int ENEMY_SCORE_VALUE = loadIntEnv("ENEMY_SCORE_VALUE", 10);
    /** The color of the enemy ships. */
    public static final Color ENEMY_COLOR = loadColorEnv("ENEMY_COLOR", Color.RED);

    // --- PowerUp ---
    /** The maximum number of power-ups allowed on screen at any time. */
    public static final int MAX_POWERUPS = loadIntEnv("MAX_POWERUPS", 5);
    /** The probability (0.0 to 1.0) of a new power-up spawning in each game tick. */
    public static final double POWERUP_SPAWN_CHANCE = loadDoubleEnv("POWERUP_SPAWN_CHANCE", 0.02);
    /** The width of a power-up item in pixels. */
    public static final int POWERUP_WIDTH = loadIntEnv("POWERUP_WIDTH", 15);
    /** The height of a power-up item in pixels. */
    public static final int POWERUP_HEIGHT = loadIntEnv("POWERUP_HEIGHT", 15);
    /** The base speed of power-ups in pixels per game tick. */
    public static final int POWERUP_BASE_SPEED = loadIntEnv("POWERUP_BASE_SPEED", 3);
    /** The maximum additional random speed added to the base speed for power-ups. */
    public static final int POWERUP_SPEED_VARIATION = loadIntEnv("POWERUP_SPEED_VARIATION", 2);
    /** The score awarded to the player for collecting a power-up. */
    public static final int POWERUP_SCORE_VALUE = loadIntEnv("POWERUP_SCORE_VALUE", 50);
    /** The color of the power-up items. */
    public static final Color POWERUP_COLOR = loadColorEnv("POWERUP_COLOR", Color.GREEN);

    // --- UI Elements (Login Screen) ---
    /** The background color of the login screen panel. */
    public static final Color LOGIN_BACKGROUND_COLOR = loadColorEnv("LOGIN_BACKGROUND_COLOR", Color.DARK_GRAY);
    /** The text color for labels (e.g., "Username:", "Password:") on the login screen. */
    public static final Color LOGIN_LABEL_COLOR = loadColorEnv("LOGIN_LABEL_COLOR", Color.WHITE);
    /** The text color for status messages indicating errors on the login screen. */
    public static final Color LOGIN_STATUS_ERROR_COLOR = loadColorEnv("LOGIN_STATUS_ERROR_COLOR", Color.RED);
    /** The text color for status messages indicating success on the login screen. */
    public static final Color LOGIN_STATUS_SUCCESS_COLOR = loadColorEnv("LOGIN_STATUS_SUCCESS_COLOR", Color.GREEN);
    /** The foreground (text) color for the high score display area on the login screen. */
    public static final Color LOGIN_HIGHSCORE_FG_COLOR = loadColorEnv("LOGIN_HIGHSCORE_FG_COLOR", Color.CYAN);
    /** The background color for the high score display area on the login screen. */
    public static final Color LOGIN_HIGHSCORE_BG_COLOR = loadColorEnv("LOGIN_HIGHSCORE_BG_COLOR", Color.BLACK);
    /** The font name for the high score display text on the login screen. */
    public static final String LOGIN_HIGHSCORE_FONT_NAME = loadStringEnv("LOGIN_HIGHSCORE_FONT_NAME", "Monospaced");
    /** The font style (e.g., Font.PLAIN, Font.BOLD) for the high score display text. */
    public static final int LOGIN_HIGHSCORE_FONT_STYLE = loadFontStyleEnv("LOGIN_HIGHSCORE_FONT_STYLE", Font.PLAIN);
    /** The font size for the high score display text on the login screen. */
    public static final int LOGIN_HIGHSCORE_FONT_SIZE = loadIntEnv("LOGIN_HIGHSCORE_FONT_SIZE", 12);

    // --- UI Elements (Game Panel) ---
    /** The background color of the main game panel during gameplay. */
    public static final Color GAME_BACKGROUND_COLOR = loadColorEnv("GAME_BACKGROUND_COLOR", Color.BLACK);
    /** The text color for UI elements like score and lives during gameplay. */
    public static final Color GAME_UI_TEXT_COLOR = loadColorEnv("GAME_UI_TEXT_COLOR", Color.WHITE);
    /** The font name for the score and lives display during gameplay. */
    public static final String GAME_UI_FONT_NAME = loadStringEnv("GAME_UI_FONT_NAME", "Consolas");
    /** The font style for the score and lives display during gameplay. */
    public static final int GAME_UI_FONT_STYLE = loadFontStyleEnv("GAME_UI_FONT_STYLE", Font.BOLD);
    /** The font size for the score and lives display during gameplay. */
    public static final int GAME_UI_FONT_SIZE = loadIntEnv("GAME_UI_FONT_SIZE", 16);

    /** The text color for the "GAME OVER" messages. */
    public static final Color GAME_OVER_TEXT_COLOR = loadColorEnv("GAME_OVER_TEXT_COLOR", Color.YELLOW);
    /** The font name used for game over messages. */
    public static final String GAME_OVER_FONT_NAME = loadStringEnv("GAME_OVER_FONT_NAME", "Arial");
    /** The font style for the large "GAME OVER" text. */
    public static final int GAME_OVER_LARGE_FONT_STYLE = loadFontStyleEnv("GAME_OVER_LARGE_FONT_STYLE", Font.BOLD);
    /** The font size for the large "GAME OVER" text. */
    public static final int GAME_OVER_LARGE_FONT_SIZE = loadIntEnv("GAME_OVER_LARGE_FONT_SIZE", 48);
    /** The font style for the medium-sized text on the game over screen (e.g., final score). */
    public static final int GAME_OVER_MEDIUM_FONT_STYLE = loadFontStyleEnv("GAME_OVER_MEDIUM_FONT_STYLE", Font.BOLD);
    /** The font size for the medium-sized text on the game over screen. */
    public static final int GAME_OVER_MEDIUM_FONT_SIZE = loadIntEnv("GAME_OVER_MEDIUM_FONT_SIZE", 24);
    /** The font style for the small text on the game over screen (e.g., instructions). */
    public static final int GAME_OVER_SMALL_FONT_STYLE = loadFontStyleEnv("GAME_OVER_SMALL_FONT_STYLE", Font.PLAIN);
    /** The font size for the small text on the game over screen. */
    public static final int GAME_OVER_SMALL_FONT_SIZE = loadIntEnv("GAME_OVER_SMALL_FONT_SIZE", 16);

    /** The translucent color used for the overlay when the game is paused. */
    public static final Color PAUSE_OVERLAY_COLOR = loadColorEnv("PAUSE_OVERLAY_COLOR", new Color(0, 0, 0, 150)); // Black with alpha
    /** The text color used for messages on the pause screen. */
    public static final Color PAUSE_TEXT_COLOR = loadColorEnv("PAUSE_TEXT_COLOR", Color.WHITE);
    /** The font name used for pause screen messages. */
    public static final String PAUSE_FONT_NAME = loadStringEnv("PAUSE_FONT_NAME", "Arial");
    /** The font style for the large "PAUSED" text. */
    public static final int PAUSE_LARGE_FONT_STYLE = loadFontStyleEnv("PAUSE_LARGE_FONT_STYLE", Font.BOLD);
    /** The font size for the large "PAUSED" text. */
    public static final int PAUSE_LARGE_FONT_SIZE = loadIntEnv("PAUSE_LARGE_FONT_SIZE", 48);
    /** The font style for the small text on the pause screen (e.g., instructions). */
    public static final int PAUSE_SMALL_FONT_STYLE = loadFontStyleEnv("PAUSE_SMALL_FONT_STYLE", Font.PLAIN);
    /** The font size for the small text on the pause screen. */
    public static final int PAUSE_SMALL_FONT_SIZE = loadIntEnv("PAUSE_SMALL_FONT_SIZE", 16);

    // --- Card Layout IDs ---
    /** The identifier used for the LoginScreen panel within the CardLayout. */
    public static final String LOGIN_PANEL_ID = loadStringEnv("LOGIN_PANEL_ID", "LoginPanel");
    /** The identifier used for the GamePanel panel within the CardLayout. */
    public static final String GAME_PANEL_ID = loadStringEnv("GAME_PANEL_ID", "GamePanel");
}


// --- Core Data Models ---

/**
 * Abstract base class for all interactive entities within the game world,
 * such as the player, enemies, bullets, and power-ups.
 * Defines common properties like position, size, and collision bounds,
 * as well as abstract methods for updating state and drawing.
 */
abstract class GameObject {
    /** The x-coordinate of the top-left corner of the object. */
    protected int x;
    /** The y-coordinate of the top-left corner of the object. */
    protected int y;
    /** The width of the object in pixels. */
    protected int width;
    /** The height of the object in pixels. */
    protected int height;
    /** The rectangular boundary used for collision detection. */
    protected Rectangle bounds;

    /**
     * Constructs a new GameObject with specified position and dimensions.
     * Initializes the collision bounds based on these parameters.
     *
     * @param x      The initial x-coordinate.
     * @param y      The initial y-coordinate.
     * @param width  The width of the object.
     * @param height The height of the object.
     */
    public GameObject(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new Rectangle(x, y, width, height);
    }

    /**
     * Gets the current x-coordinate of the object.
     * @return The x-coordinate.
     */
    public int getX() { return x; }

    /**
     * Gets the current y-coordinate of the object.
     * @return The y-coordinate.
     */
    public int getY() { return y; }

    /**
     * Gets the width of the object.
     * @return The width in pixels.
     */
    public int getWidth() { return width; }

    /**
     * Gets the height of the object.
     * @return The height in pixels.
     */
    public int getHeight() { return height; }

    /**
     * Updates the position of the internal {@link Rectangle} bounds to match the object's current x and y coordinates.
     * This should be called after the object's position changes.
     */
    protected void updateBounds() {
        bounds.setLocation(x, y);
    }

    /**
     * Gets the rectangular bounds of this object, used for collision detection.
     * Note: The returned rectangle's position might be slightly out of sync if {@link #updateBounds()}
     * hasn't been called after the last position change.
     * @return A {@link Rectangle} representing the object's boundaries.
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Updates the state of the game object. This typically involves moving the object
     * based on its speed or player input. Must be implemented by subclasses.
     */
    public abstract void update();

    /**
     * Renders the game object onto the screen. Must be implemented by subclasses.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    public abstract void draw(Graphics2D g2d);

    /**
     * Checks if the object has moved completely outside the vertical bounds of the screen.
     *
     * @param screenHeight The height of the game screen.
     * @return {@code true} if the object is entirely above the top or below the bottom edge, {@code false} otherwise.
     */
    public boolean isOutOfBounds(int screenHeight) {
        // Checks if the object is completely below the screen OR completely above the screen.
        return y > screenHeight || y + height < 0;
    }

    /**
     * Checks if this game object's bounds intersect with another game object's bounds.
     *
     * @param other The other {@link GameObject} to check for intersection.
     * @return {@code true} if the bounds intersect, {@code false} otherwise.
     */
    public boolean intersects(GameObject other) {
        return this.bounds.intersects(other.getBounds());
    }
}

/**
 * Represents the player-controlled spaceship. Handles movement based on input flags
 * and provides the ability to shoot bullets.
 */
class Player extends GameObject {
    private int speed;
    private boolean movingLeft, movingRight, movingUp, movingDown;
    private boolean wantsToShoot = false;

    /**
     * Constructs a new Player object at the specified starting position.
     * Initializes speed and dimensions based on {@link AppConfig}.
     *
     * @param startX The initial x-coordinate for the player.
     * @param startY The initial y-coordinate for the player.
     */
    public Player(int startX, int startY) {
        super(startX, startY, AppConfig.PLAYER_WIDTH, AppConfig.PLAYER_HEIGHT);
        this.speed = AppConfig.PLAYER_SPEED;
        this.movingLeft = false;
        this.movingRight = false;
        this.movingUp = false;
        this.movingDown = false;
    }

    /**
     * Sets the flag indicating whether the player should move left.
     * @param movingLeft {@code true} to move left, {@code false} to stop.
     */
    public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }

    /**
     * Sets the flag indicating whether the player should move right.
     * @param movingRight {@code true} to move right, {@code false} to stop.
     */
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }

    /**
     * Sets the flag indicating whether the player should move up.
     * @param movingUp {@code true} to move up, {@code false} to stop.
     */
    public void setMovingUp(boolean movingUp) { this.movingUp = movingUp; }

    /**
     * Sets the flag indicating whether the player should move down.
     * @param movingDown {@code true} to move down, {@code false} to stop.
     */
    public void setMovingDown(boolean movingDown) { this.movingDown = movingDown; }

    /**
     * Sets the flag indicating whether the player intends to shoot on the next update cycle.
     * @param wantsToShoot {@code true} if the player wants to shoot, {@code false} otherwise.
     */
    public void setWantsToShoot(boolean wantsToShoot) { this.wantsToShoot = wantsToShoot; }

    /**
     * Checks if the player currently intends to shoot.
     * @return {@code true} if the shoot action is requested, {@code false} otherwise.
     */
    public boolean isWantsToShoot() { return wantsToShoot; }

    /**
     * Updates the player's position based on the current movement flags.
     * Ensures the player stays within the game boundaries. Updates collision bounds.
     */
    @Override
    public void update() {
        int dx = 0; // Change in x
        int dy = 0; // Change in y

        // Calculate delta based on movement flags and speed
        if (movingLeft) dx -= speed;
        if (movingRight) dx += speed;
        if (movingUp) dy -= speed;
        if (movingDown) dy += speed;

        // Update position
        x += dx;
        y += dy;

        // Clamp position to stay within screen bounds, considering UI buffer at the bottom
        x = Math.max(0, x); // Prevent moving past left edge
        x = Math.min(AppConfig.GAME_WIDTH - width, x); // Prevent moving past right edge
        y = Math.max(0, y); // Prevent moving past top edge
        y = Math.min(AppConfig.GAME_HEIGHT - height - AppConfig.BOTTOM_UI_BUFFER, y); // Prevent moving past bottom edge (above UI)

        // Update the collision bounds rectangle
        updateBounds();
    }

    /**
     * Draws the player's spaceship on the screen using its configured color.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.PLAYER_COLOR);
        g2d.fillRect(x, y, width, height);
    }

    /**
     * Creates and returns a new {@link Bullet} object originating from the player's current position.
     * Resets the {@code wantsToShoot} flag to {@code false}.
     *
     * @return A new {@link Bullet} instance ready to be added to the game state.
     */
    public Bullet shoot() {
         this.wantsToShoot = false; // Consume the shoot request
         // Calculate bullet starting position (centered above the player)
         int bulletX = this.x + this.width / 2 - AppConfig.BULLET_WIDTH / 2;
         int bulletY = this.y - AppConfig.BULLET_HEIGHT;
         return new Bullet(bulletX, bulletY);
    }

    /**
     * Resets the player's position to a specified starting point and clears all movement/shooting flags.
     * Typically used when starting a new game or after losing a life.
     *
     * @param startX The x-coordinate to reset the player to.
     * @param startY The y-coordinate to reset the player to.
     */
    public void resetPosition(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.movingLeft = false;
        this.movingRight = false;
        this.movingUp = false;
        this.movingDown = false;
        this.wantsToShoot = false;
        updateBounds(); // Ensure bounds match the new position
    }
}

/**
 * Represents a projectile fired by the player. Moves upwards.
 */
class Bullet extends GameObject {
    private int speed;

    /**
     * Constructs a new Bullet object at the specified position.
     * Initializes speed and dimensions based on {@link AppConfig}.
     *
     * @param x The initial x-coordinate of the bullet.
     * @param y The initial y-coordinate of the bullet.
     */
    public Bullet(int x, int y) {
        super(x, y, AppConfig.BULLET_WIDTH, AppConfig.BULLET_HEIGHT);
        this.speed = AppConfig.BULLET_SPEED;
    }

    /**
     * Updates the bullet's position, moving it upwards based on its speed.
     * Updates collision bounds.
     */
    @Override
    public void update() {
        y -= speed; // Move upwards
        updateBounds();
    }

    /**
     * Draws the bullet on the screen using its configured color.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.BULLET_COLOR);
        g2d.fillRect(x, y, width, height);
    }
}

/**
 * Represents an enemy ship that moves downwards towards the player.
 */
class Enemy extends GameObject {
    private int speed;

    /**
     * Constructs a new Enemy object at the specified position with given dimensions and speed.
     *
     * @param x      The initial x-coordinate of the enemy.
     * @param y      The initial y-coordinate of the enemy.
     * @param width  The width of the enemy.
     * @param height The height of the enemy.
     * @param speed  The downward speed of the enemy in pixels per tick.
     */
    public Enemy(int x, int y, int width, int height, int speed) {
        super(x, y, width, height);
        this.speed = speed;
    }

    /**
     * Updates the enemy's position, moving it downwards based on its speed.
     * Updates collision bounds.
     */
    @Override
    public void update() {
        y += speed; // Move downwards
        updateBounds();
    }

    /**
     * Draws the enemy ship on the screen using its configured color.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.ENEMY_COLOR);
        g2d.fillRect(x, y, width, height);
    }
}

/**
 * Represents a power-up item that moves downwards and provides a benefit (e.g., score) when collected by the player.
 */
class PowerUp extends GameObject {
    private int speed;

    /**
     * Constructs a new PowerUp object at the specified position with a given speed.
     * Initializes dimensions based on {@link AppConfig}.
     *
     * @param x     The initial x-coordinate of the power-up.
     * @param y     The initial y-coordinate of the power-up.
     * @param speed The downward speed of the power-up in pixels per tick.
     */
    public PowerUp(int x, int y, int speed) {
        super(x, y, AppConfig.POWERUP_WIDTH, AppConfig.POWERUP_HEIGHT);
        this.speed = speed;
    }

    /**
     * Updates the power-up's position, moving it downwards based on its speed.
     * Updates collision bounds.
     */
    @Override
    public void update() {
        y += speed; // Move downwards
        updateBounds();
    }

    /**
     * Draws the power-up item on the screen using its configured color (as an oval).
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.POWERUP_COLOR);
        g2d.fillOval(x, y, width, height); // Drawn as an oval
    }

    /**
     * Applies the effect of this power-up to the game state.
     * Currently, it increases the player's score.
     *
     * @param gameState The current {@link GameState} to modify.
     */
    public void applyEffect(GameState gameState) {
        gameState.increaseScore(AppConfig.POWERUP_SCORE_VALUE);
    }
}

// --- Database Management (REFACTORED FOR SECURITY) ---

/**
 * Encapsulates all database interactions for the game, including user authentication
 * and high score management. Implements security best practices like using
 * parameterized queries (PreparedStatements) to prevent SQL injection and
 * BCrypt for password hashing. Manages database connections using try-with-resources
 * for automatic closure.
 */
class DatabaseManager {
    private final String dbUrl;
    private final String jdbcDriver; // Keep track of driver used, primarily for logging.

    /**
     * Static initializer to load the configured JDBC driver once when the class is loaded.
     * Throws a RuntimeException if the driver cannot be found, as the application cannot function without it.
     */
    static {
        try {
            // Dynamically load the JDBC driver class specified in AppConfig.
            Class.forName(AppConfig.JDBC_DRIVER);
            System.out.println("JDBC Driver loaded: " + AppConfig.JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            // This is a fatal error if the driver is missing.
            System.err.println("FATAL: JDBC Driver not found! Check classpath: " + AppConfig.JDBC_DRIVER);
            throw new RuntimeException("Failed to load JDBC Driver", e);
        }
    }

    /**
     * Constructs a DatabaseManager instance.
     * Initializes database URL and driver from {@link AppConfig} and ensures
     * the necessary database tables are created.
     */
    public DatabaseManager() {
        this.dbUrl = AppConfig.DATABASE_URL;
        this.jdbcDriver = AppConfig.JDBC_DRIVER; // Store for potential future use/logging
        initializeDatabaseTables();
    }

    /**
     * Establishes and returns a NEW database connection using the configured URL.
     * It is the responsibility of the calling method to close this connection,
     * typically achieved using a try-with-resources statement.
     *
     * @return A new {@link Connection} object to the database.
     * @throws SQLException if a database access error occurs during connection.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Initializes the database by creating the `users` and `high_scores` tables
     * if they do not already exist. The `users` table is configured to store
     * hashed passwords (`password_hash`) instead of plaintext.
     * Uses try-with-resources to ensure database resources are closed.
     */
    private void initializeDatabaseTables() {
        // SQL statement to create the users table if it doesn't exist.
        // SECURITY: `password_hash` column stores the BCrypt hash (TEXT is suitable).
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    " username TEXT UNIQUE NOT NULL," +
                                    " password_hash TEXT NOT NULL" + // Column renamed for clarity
                                    ");";
        // SQL statement to create the high_scores table if it doesn't exist.
        String createHighScoresTable = "CREATE TABLE IF NOT EXISTS high_scores (" +
                                      " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                      " username TEXT NOT NULL," +
                                      " score INTEGER NOT NULL," +
                                      " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" + // Records when score was achieved
                                      ");";

        // Use try-with-resources to automatically close the Connection and Statement.
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {

            statement.execute(createUsersTable);       // Execute creation of users table.
            statement.execute(createHighScoresTable); // Execute creation of high_scores table.
            System.out.println("Database tables checked/created successfully (Password stored as hash).");

        } catch (SQLException e) {
            // Log a critical error if table initialization fails.
            handleError("CRITICAL: Error initializing database tables. Application might not function correctly.", e);
        }
    }

    /**
     * Registers a new user in the database. Hashes the provided password using BCrypt
     * before storing it. Prevents registration if the username already exists.
     * Uses parameterized queries (PreparedStatement) to prevent SQL injection.
     *
     * @param username          The desired username for the new user.
     * @param plainTextPassword The user's chosen password in plain text (will be hashed).
     * @return {@code true} if the user was successfully registered, {@code false} if the username
     *         already exists, input is invalid, or a database/hashing error occurred.
     */
    public boolean registerUser(String username, String plainTextPassword) {
        // Basic validation before proceeding.
        if (!isInputValid(username, plainTextPassword)) {
            System.err.println("Registration failed: Invalid username or password provided.");
            return false;
        }

        // SQL query to check if a username already exists. Parameterized with '?'.
        String checkUserSql = "SELECT id FROM users WHERE username = ?";
        // SQL query to insert a new user. Parameterized for username and hashed password.
        // SECURITY: Stores `password_hash`, not the plain text password.
        String insertUserSql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        // Use try-with-resources for the connection. PreparedStatements are managed within nested try-blocks.
        try (Connection conn = getConnection()) {
            // 1. Check if the username already exists using a PreparedStatement.
            try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
                checkStmt.setString(1, username); // Set the username parameter safely.
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        // If a result is found, the username exists.
                        System.out.println("Registration failed: Username '" + username + "' already exists.");
                        return false;
                    }
                } // ResultSet is automatically closed here.
            } // PreparedStatement `checkStmt` is automatically closed here.

            // 2. If username doesn't exist, hash the password.
            // SECURITY: Hash the password using BCrypt with a configurable work factor (log rounds).
            // `gensalt` automatically generates a unique salt for each password.
            String hashedPassword = BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(AppConfig.BCRYPT_LOG_ROUNDS));

            // 3. Insert the new user with the hashed password using another PreparedStatement.
            try (PreparedStatement insertStmt = conn.prepareStatement(insertUserSql)) {
                insertStmt.setString(1, username);       // Set the username parameter.
                insertStmt.setString(2, hashedPassword); // Set the hashed password parameter.
                int result = insertStmt.executeUpdate(); // Execute the insert operation.

                // Check if the insertion was successful (executeUpdate returns the number of affected rows).
                if (result > 0) {
                     System.out.println("User '" + username + "' registered successfully.");
                     return true;
                } else {
                    // This case should ideally not happen with a valid insert statement unless there's a constraint violation missed.
                    System.err.println("Registration failed: Insert returned 0 rows affected.");
                    return false;
                }
            } // PreparedStatement `insertStmt` is automatically closed here.

        } catch (SQLException e) {
            // Handle potential database errors during connection or query execution.
            handleError("Error during user registration for username: " + username, e);
            return false;
        } catch (Exception e) {
            // Catch potential errors during BCrypt hashing (less likely but possible).
            handleError("Error during password hashing for user: " + username, e);
            return false;
        }
    }

    /**
     * Validates a user's login attempt by comparing the hash of the provided password
     * against the stored hash in the database.
     * Uses parameterized queries (PreparedStatement) to prevent SQL injection and
     * {@link BCrypt#checkpw(String, String)} for secure password comparison.
     *
     * @param username          The username attempting to log in.
     * @param plainTextPassword The plain text password provided by the user.
     * @return {@code true} if the username exists and the provided password matches the
     *         stored hash, {@code false} otherwise (username not found, password mismatch,
     *         invalid input, or database/hashing error).
     */
    public boolean validateUser(String username, String plainTextPassword) {
        // Basic validation before proceeding.
        if (!isInputValid(username, plainTextPassword)) {
             System.err.println("Validation failed: Invalid username or password provided.");
            return false;
        }

        // SQL query to retrieve the stored password hash for a given username. Parameterized with '?'.
        // SECURITY: Selects `password_hash`.
        String querySql = "SELECT password_hash FROM users WHERE username = ?";

        // Use try-with-resources for Connection and PreparedStatement.
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySql)) {

            pstmt.setString(1, username); // Set the username parameter safely.
            try (ResultSet rs = pstmt.executeQuery()) {
                // Check if a user with the given username was found.
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash"); // Retrieve the stored hash.

                    // Ensure the stored hash is not null before attempting comparison.
                    if (storedHash != null) {
                        // SECURITY: Use BCrypt.checkpw to securely compare the plain text password
                        // against the stored hash. This method handles the salt extraction and comparison.
                        try {
                            return BCrypt.checkpw(plainTextPassword, storedHash);
                        } catch (IllegalArgumentException ex) {
                             // Handle cases where the stored hash might be corrupted or in an invalid format for BCrypt.
                             handleError("Invalid hash format encountered during validation for user: " + username, ex);
                             return false;
                        }
                    } else {
                         // This should not happen if the DB schema enforces NOT NULL, but check defensively.
                         System.err.println("Warning: Retrieved null password hash for user: " + username);
                         return false;
                    }
                } else {
                    // No user found with the given username.
                    return false;
                }
            } // ResultSet is automatically closed here.
        } catch (SQLException e) {
            // Handle potential database errors.
            handleError("Error validating user: " + username, e);
            return false;
        }
        // PreparedStatement and Connection are automatically closed here.
    }

    /**
     * Saves a high score entry to the database for a given user.
     * Uses a parameterized query (PreparedStatement) to prevent SQL injection.
     * Performs basic validation on input parameters.
     *
     * @param username The username of the player who achieved the score.
     * @param score    The score value to save. Must be non-negative.
     */
    public void saveScore(String username, int score) {
        // Validate input: username must not be null/empty, score must be non-negative.
        if (username == null || username.trim().isEmpty() || score < 0) {
            System.err.println("Invalid data for saving score (User: " + username + ", Score: " + score + ")");
            return; // Do not proceed if input is invalid.
        }

        // SQL query to insert a new high score. Parameterized for username and score.
        String insertScoreSql = "INSERT INTO high_scores (username, score) VALUES (?, ?)";

        // Use try-with-resources for Connection and PreparedStatement.
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertScoreSql)) {

            pstmt.setString(1, username); // Set the username parameter.
            pstmt.setInt(2, score);       // Set the score parameter.
            pstmt.executeUpdate();        // Execute the insert operation.
            System.out.println("Score " + score + " for user " + username + " saved.");

        } catch (SQLException e) {
            // Handle potential database errors.
            handleError("Error saving score for user: " + username, e);
        }
        // PreparedStatement and Connection are automatically closed here.
    }

    /**
     * Retrieves a list of the top high scores from the database, ordered from highest to lowest.
     * The number of scores retrieved is limited by {@link AppConfig#HIGH_SCORE_LIMIT}.
     * Uses a parameterized query (PreparedStatement) for the limit parameter.
     *
     * @return A {@link List} of strings, each formatted to display a ranked high score entry (e.g., "1. PlayerOne : 1000").
     *         If no scores are found, returns a list containing a single message indicating this.
     *         If a database error occurs, returns a list containing an error message.
     */
    public List<String> getHighScores() {
        int limit = AppConfig.HIGH_SCORE_LIMIT; // Get the configured limit.
        List<String> scores = new ArrayList<>();
        // SQL query to select top scores. Parameterized for the LIMIT clause.
        String queryHighScores = "SELECT username, score FROM high_scores ORDER BY score DESC LIMIT ?";

        // Use try-with-resources for Connection and PreparedStatement.
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(queryHighScores)) {

            pstmt.setInt(1, limit); // Set the limit parameter safely.
            try (ResultSet rs = pstmt.executeQuery()) {
                int rank = 1; // Initialize rank counter.
                // Iterate through the results.
                while (rs.next()) {
                    String username = rs.getString("username");
                    int scoreValue = rs.getInt("score");
                    // Format the score entry string with rank, username, and score.
                    scores.add(String.format("%d. %-15s : %d", rank++, username, scoreValue));
                }
                // If no scores were found (rank is still 1), add a message.
                if (rank == 1) {
                    scores.add("No scores recorded yet.");
                }
            } // ResultSet is automatically closed here.

        } catch (SQLException e) {
            // Handle potential database errors.
            handleError("Error fetching high scores", e);
            scores.clear(); // Clear any partially added scores.
            scores.add("Error loading scores due to database issue."); // Add error message.
        }
        // PreparedStatement and Connection are automatically closed here.

        return scores; // Return the list of score strings or error message.
    }

    /**
     * This method is deprecated as database connections are now managed automatically
     * within each method using try-with-resources, eliminating the need for manual closure
     * at the DatabaseManager level. Calling this method has no effect.
     *
     * @deprecated Connections are managed per-operation via try-with-resources.
     */
    @Deprecated
    public void closeConnection() {
        // Log that this method is deprecated and no longer necessary.
        System.out.println("DatabaseManager.closeConnection() is deprecated. Connection management is automatic via try-with-resources.");
        // No actual connection closing logic is needed here anymore.
    }

    /**
     * Performs basic validation on username and password strings.
     * Checks if they are non-null and not empty/whitespace-only.
     * More complex validation (e.g., length, character requirements) could be added here.
     *
     * @param username The username string to validate.
     * @param password The password string to validate.
     * @return {@code true} if both username and password are considered valid (non-null, not empty), {@code false} otherwise.
     */
    private boolean isInputValid(String username, String password) {
        // Check for null or empty/whitespace-only strings.
        return username != null && !username.trim().isEmpty() &&
               password != null && !password.isEmpty(); // Passwords can potentially contain only whitespace, so just check isEmpty.
    }

    /**
     * Centralized handler for logging database and security-related errors.
     * Prints an error message to standard error, including the original exception message.
     * In a production application, this should integrate with a proper logging framework (e.g., Log4j, SLF4j).
     *
     * @param message A descriptive message indicating the context of the error.
     * @param e       The exception that occurred.
     */
    private void handleError(String message, Exception e) {
        System.err.println("DATABASE/SECURITY ERROR: " + message + " - " + e.getMessage());
        // Optionally uncomment the following line for detailed stack traces during development/debugging.
        // e.printStackTrace();
    }
}


// --- NEW: Game State Encapsulation ---

/**
 * Encapsulates the dynamic state of the game, including player status,
 * lists of active game objects (enemies, bullets, power-ups), score, lives,
 * and game status flags (running, paused, game over).
 * Provides controlled methods for accessing and modifying this state.
 */
class GameState {
    private Player player;
    private final List<Enemy> enemies;
    private final List<PowerUp> powerUps;
    private final List<Bullet> bullets;
    private int score;
    private int lives;
    private boolean isGameOver;
    private boolean isPaused;
    private boolean isRunning; // Indicates if the game loop should be active

    /**
     * Constructs a new GameState object, initializing all game elements to their default starting values.
     * Creates empty lists for dynamic objects and sets initial score, lives, and status flags.
     */
    public GameState() {
        // Player is initialized but positioned during reset/start.
        this.player = new Player(0, 0);
        this.enemies = new ArrayList<>();
        this.powerUps = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.score = 0;
        this.lives = AppConfig.INITIAL_LIVES;
        this.isGameOver = false;
        this.isPaused = false;
        this.isRunning = false; // Game doesn't start running immediately
    }

    /**
     * Resets the game state to its initial configuration for starting a new game.
     * Clears all dynamic game objects, resets score and lives, positions the player,
     * and sets status flags to their default non-playing state.
     */
    public void reset() {
        // Reposition player to the center bottom area.
        player.resetPosition(
            AppConfig.GAME_WIDTH / 2 - AppConfig.PLAYER_WIDTH / 2,
            AppConfig.GAME_HEIGHT - AppConfig.BOTTOM_UI_BUFFER - AppConfig.PLAYER_HEIGHT - 10
        );
        // Clear all dynamic object lists.
        enemies.clear();
        powerUps.clear();
        bullets.clear();
        // Reset score and lives.
        score = 0;
        lives = AppConfig.INITIAL_LIVES;
        // Reset status flags.
        isGameOver = false;
        isPaused = false;
        isRunning = false;
    }

    /**
     * Gets the player object.
     * @return The {@link Player} instance.
     */
    public Player getPlayer() { return player; }

    /**
     * Gets the list of active enemies.
     * Note: Modifying this list directly might cause concurrency issues if done outside the game loop's update cycle.
     * Consider returning an unmodifiable list if stricter encapsulation is needed.
     * @return The {@link List} of {@link Enemy} objects.
     */
    public List<Enemy> getEnemies() { return enemies; }

    /**
     * Gets the list of active power-ups.
     * Note: Modifying this list directly might cause concurrency issues.
     * @return The {@link List} of {@link PowerUp} objects.
     */
    public List<PowerUp> getPowerUps() { return powerUps; }

    /**
     * Gets the list of active bullets.
     * Note: Modifying this list directly might cause concurrency issues.
     * @return The {@link List} of {@link Bullet} objects.
     */
    public List<Bullet> getBullets() { return bullets; }

    /**
     * Gets the current player score.
     * @return The score value.
     */
    public int getScore() { return score; }

    /**
     * Gets the number of lives remaining for the player.
     * @return The number of lives.
     */
    public int getLives() { return lives; }

    /**
     * Checks if the game is currently in a "Game Over" state.
     * @return {@code true} if the game is over, {@code false} otherwise.
     */
    public boolean isGameOver() { return isGameOver; }

    /**
     * Checks if the game is currently paused.
     * @return {@code true} if the game is paused, {@code false} otherwise.
     */
    public boolean isPaused() { return isPaused; }

    /**
     * Checks if the game logic loop is currently running (not paused and not game over).
     * @return {@code true} if the game is actively running, {@code false} otherwise.
     */
    public boolean isRunning() { return isRunning; }

    /**
     * Sets the game over state. If set to {@code true}, also sets {@code isRunning} to {@code false}.
     * @param gameOver {@code true} to mark the game as over, {@code false} otherwise.
     */
    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
        if (gameOver) {
            this.isRunning = false; // Game loop stops when game is over.
        }
    }

    /**
     * Sets the paused state. The game can only be paused if it is currently running and not game over.
     * @param paused {@code true} to pause the game, {@code false} to resume.
     */
    public void setPaused(boolean paused) {
        // Only allow pausing/resuming if the game is running and not over.
        if (this.isRunning && !this.isGameOver) {
            this.isPaused = paused;
        }
    }

    /**
     * Sets the running state, indicating whether the main game loop should process updates.
     * Cannot set to running if the game is already over. Setting to not running also clears the paused state.
     * @param running {@code true} to start or continue the game loop, {@code false} to stop it.
     */
    public void setRunning(boolean running) {
        if (!this.isGameOver) { // Can only set running if not game over
            this.isRunning = running;
            if (!running) {
                // If stopping the game, ensure it's not marked as paused.
                this.isPaused = false;
            }
        } else {
            // If game is over, ensure isRunning remains false.
            this.isRunning = false;
        }
    }

    /**
     * Decreases the player's lives by one. If lives reach zero or less,
     * sets the game over state. Logs the life loss.
     */
    public void decreaseLives() {
        if (lives > 0) {
            lives--;
            System.out.println("Life lost. Lives remaining: " + lives);
        }
        // Check if this life loss triggers game over.
        if (lives <= 0 && !isGameOver) {
            setGameOver(true); // Use the setter to ensure isRunning is also updated.
            System.out.println("Lives reached zero. Game Over triggered.");
        }
    }

    /**
     * Increases the player's score by a specified amount.
     * Score only increases if the amount is positive and the game is currently running.
     *
     * @param amount The positive amount to add to the score.
     */
    public void increaseScore(int amount) {
        if (amount > 0 && isRunning) { // Ensure score increases only during active gameplay.
            score += amount;
        }
    }

    /**
     * Adds a bullet to the list of active bullets. Ignores null bullets.
     * @param bullet The {@link Bullet} object to add.
     */
    public void addBullet(Bullet bullet) {
        if (bullet != null) bullets.add(bullet);
    }

    /**
     * Adds an enemy to the list of active enemies. Ignores null enemies.
     * @param enemy The {@link Enemy} object to add.
     */
    public void addEnemy(Enemy enemy) {
        if (enemy != null) enemies.add(enemy);
    }

    /**
     * Adds a power-up to the list of active power-ups. Ignores null power-ups.
     * @param powerUp The {@link PowerUp} object to add.
     */
    public void addPowerUp(PowerUp powerUp) {
        if (powerUp != null) powerUps.add(powerUp);
    }

    /**
     * Removes a collection of bullets from the active list.
     * Typically used after bullets hit enemies or go off-screen.
     * @param bulletsToRemove A {@link List} containing the {@link Bullet} objects to remove. If null, no action is taken.
     */
    public void removeBullets(List<Bullet> bulletsToRemove) {
        if (bulletsToRemove != null) bullets.removeAll(bulletsToRemove);
    }

    /**
     * Removes a collection of enemies from the active list.
     * Typically used after enemies are destroyed or go off-screen.
     * @param enemiesToRemove A {@link List} containing the {@link Enemy} objects to remove. If null, no action is taken.
     */
    public void removeEnemies(List<Enemy> enemiesToRemove) {
        if (enemiesToRemove != null) enemies.removeAll(enemiesToRemove);
    }

    /**
     * Removes a collection of power-ups from the active list.
     * Typically used after power-ups are collected or go off-screen.
     * @param powerUpsToRemove A {@link List} containing the {@link PowerUp} objects to remove. If null, no action is taken.
     */
    public void removePowerUps(List<PowerUp> powerUpsToRemove) {
        if (powerUpsToRemove != null) powerUps.removeAll(powerUpsToRemove);
    }
}


// --- Game Logic ---

/**
 * Manages the core game mechanics and updates the {@link GameState}.
 * This includes handling player actions (shooting), updating object positions,
 * detecting collisions, managing object lifecycles (spawning, removal),
 * and applying game rules. Operates on a provided {@link GameState} instance.
 */
class GameLogic {
    private final Random randomGenerator = new Random();
    private final GameState gameState; // Reference to the mutable game state

    /**
     * Constructs a new GameLogic instance associated with a specific {@link GameState}.
     *
     * @param state The {@link GameState} object that this logic will operate on. Must not be null.
     * @throws IllegalArgumentException if the provided {@code state} is null.
     */
    public GameLogic(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("GameState cannot be null for GameLogic");
        }
        this.gameState = state;
    }

    /**
     * Initializes the game state for a new game session by resetting it.
     * Should be called before starting the game loop for a new game.
     */
    public void initializeNewGame() {
        gameState.reset(); // Use GameState's reset method
        System.out.println("Game logic initialized for a new game.");
    }

    /**
     * Performs a single update tick of the game logic.
     * This method should be called repeatedly by the main game loop (e.g., via a Timer).
     * It only processes updates if the game is running, not paused, and not over.
     * Updates player, bullets, enemies, power-ups, handles shooting, collisions, and spawning.
     */
    public void update() {
        // Do nothing if the game is not in an active state
        if (!gameState.isRunning() || gameState.isPaused() || gameState.isGameOver()) {
            return;
        }

        // Update the player's state (movement)
        gameState.getPlayer().update();
        // Process player shooting request
        handleShooting();

        // Update all active game objects and remove those out of bounds
        updateGameObjects(gameState.getBullets());
        updateGameObjects(gameState.getEnemies());
        updateGameObjects(gameState.getPowerUps());

        // Detect and resolve collisions between game objects
        handleCollisions();
        // Handle spawning of new enemies and power-ups based on chance
        handleSpawning();

        // Note: Checking for game over condition (e.g., lives <= 0) is handled
        // within GameState.decreaseLives() and checked by GameController after update().
    }

    /**
     * Helper method to update a list of {@link GameObject}s.
     * Iterates through the list, calls the {@code update()} method on each object,
     * and removes objects that have gone out of the screen bounds.
     * Uses an Iterator to safely remove elements during iteration.
     *
     * @param list A {@link List} of {@link GameObject}s (or a subclass) to update.
     * @param <T> The specific type of GameObject in the list.
     */
    private <T extends GameObject> void updateGameObjects(List<T> list) {
        Iterator<T> iterator = list.iterator();
        while (iterator.hasNext()) {
            T obj = iterator.next();
            obj.update(); // Update the object's state (e.g., position)
            // Check if the object is out of the vertical screen bounds
            if (obj.isOutOfBounds(AppConfig.GAME_HEIGHT)) {
                iterator.remove(); // Remove the object from the list
            }
        }
    }

    /**
     * Checks if the player wants to shoot and, if so, creates a new bullet
     * and adds it to the game state. The player's {@code wantsToShoot} flag is
     * consumed in the {@link Player#shoot()} method.
     */
    private void handleShooting() {
        Player player = gameState.getPlayer();
        if (player.isWantsToShoot()) {
            // Player.shoot() creates the bullet and resets the flag.
            gameState.addBullet(player.shoot());
        }
    }

    /**
     * Detects and handles collisions between different types of game objects:
     * - Player vs Enemy: Decreases player lives, removes enemy.
     * - Bullet vs Enemy: Removes both bullet and enemy, increases score.
     * - Player vs PowerUp: Applies power-up effect, removes power-up.
     * Uses temporary lists to collect objects for removal to avoid ConcurrentModificationException.
     */
    private void handleCollisions() {
        Player player = gameState.getPlayer();
        // Lists to store objects that need to be removed after collision checks.
        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<PowerUp> powerUpsToRemove = new ArrayList<>();

        // 1. Player vs Enemy Collisions
        for (Enemy enemy : gameState.getEnemies()) {
            // Check intersection only if the enemy hasn't already been marked for removal
            if (!enemiesToRemove.contains(enemy) && player.intersects(enemy)) {
                gameState.decreaseLives(); // Player loses a life
                enemiesToRemove.add(enemy); // Mark enemy for removal
                // If game is over due to life loss, stop further collision checks in this tick
                if (gameState.isGameOver()) {
                    // Remove collected objects immediately and return
                    gameState.removeEnemies(enemiesToRemove);
                    gameState.removeBullets(bulletsToRemove); // Also remove any bullets collected so far
                    gameState.removePowerUps(powerUpsToRemove); // Also remove any powerups collected so far
                    return;
                }
            }
        }

        // 2. Bullet vs Enemy Collisions
        for (Bullet bullet : gameState.getBullets()) {
            // Skip bullet if already marked for removal (e.g., hit an enemy earlier in the loop)
             if (bulletsToRemove.contains(bullet)) continue;

            for (Enemy enemy : gameState.getEnemies()) {
                // Skip enemy if already marked for removal (e.g., hit by player or another bullet)
                 if (enemiesToRemove.contains(enemy)) continue;

                // Check intersection
                if (bullet.intersects(enemy)) {
                    bulletsToRemove.add(bullet); // Mark bullet for removal
                    enemiesToRemove.add(enemy);  // Mark enemy for removal
                    gameState.increaseScore(AppConfig.ENEMY_SCORE_VALUE); // Award score
                    // A single bullet should only hit one enemy, so break the inner loop.
                    break;
                }
            }
        }

        // 3. Player vs PowerUp Collisions
        for (PowerUp powerUp : gameState.getPowerUps()) {
             // Check intersection only if the power-up hasn't already been marked for removal
             if (!powerUpsToRemove.contains(powerUp) && player.intersects(powerUp)) {
                powerUp.applyEffect(gameState); // Apply the power-up's effect (e.g., increase score)
                powerUpsToRemove.add(powerUp); // Mark power-up for removal
            }
        }

        // Remove all collided objects from the game state
        gameState.removeEnemies(enemiesToRemove);
        gameState.removeBullets(bulletsToRemove);
        gameState.removePowerUps(powerUpsToRemove);
    }

    /**
     * Handles the random spawning of new enemies and power-ups based on configured
     * probabilities and maximum counts. Checks occur each game tick.
     */
    private void handleSpawning() {
        // Spawn Enemy? Check count and probability.
        if (gameState.getEnemies().size() < AppConfig.MAX_ENEMIES &&
            randomGenerator.nextDouble() < AppConfig.ENEMY_SPAWN_CHANCE) {
            spawnEnemy();
        }
        // Spawn PowerUp? Check count and probability.
        if (gameState.getPowerUps().size() < AppConfig.MAX_POWERUPS &&
            randomGenerator.nextDouble() < AppConfig.POWERUP_SPAWN_CHANCE) {
            spawnPowerUp();
        }
    }

    /**
     * Creates a new enemy instance with randomized size, position (spawning above the screen),
     * and speed, then adds it to the game state.
     */
    private void spawnEnemy() {
        // Calculate random size within configured range
        int sizeVar = AppConfig.ENEMY_SIZE_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SIZE_VARIATION) : 0;
        int size = AppConfig.ENEMY_BASE_SIZE + sizeVar;
        // Calculate random horizontal position ensuring enemy is fully within screen width
        int enemyX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - size);
        int enemyY = -size; // Start just above the screen
        // Calculate random speed within configured range
        int speedVar = AppConfig.ENEMY_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SPEED_VARIATION) : 0;
        int enemySpeed = AppConfig.ENEMY_BASE_SPEED + speedVar;
        // Create and add the new enemy
        gameState.addEnemy(new Enemy(enemyX, enemyY, size, size, enemySpeed));
    }

    /**
     * Creates a new power-up instance with randomized position (spawning above the screen)
     * and speed, then adds it to the game state.
     */
    private void spawnPowerUp() {
        // Calculate random horizontal position ensuring power-up is fully within screen width
        int powerUpX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - AppConfig.POWERUP_WIDTH);
        int powerUpY = -AppConfig.POWERUP_HEIGHT; // Start just above the screen
        // Calculate random speed within configured range
        int speedVar = AppConfig.POWERUP_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.POWERUP_SPEED_VARIATION) : 0;
        int powerUpSpeed = AppConfig.POWERUP_BASE_SPEED + speedVar;
        // Create and add the new power-up
        gameState.addPowerUp(new PowerUp(powerUpX, powerUpY, powerUpSpeed));
    }
}


// --- Input Handling ---

/**
 * Handles keyboard input for the game. Extends {@link KeyAdapter} to listen for
 * key press and release events. Translates key events into actions on the
 * {@link Player} object within the {@link GameState} or triggers actions
 * on the {@link GameController} (like pausing, restarting, exiting).
 */
class InputHandler extends KeyAdapter {
    private final GameState gameState;
    private final GameController gameController; // To trigger game-level actions (pause, restart, exit)

    /**
     * Constructs a new InputHandler.
     *
     * @param state      The {@link GameState} containing the player object to control. Must not be null.
     * @param controller The {@link GameController} to notify for game-level actions (pause, etc.). Must not be null.
     * @throws IllegalArgumentException if either {@code state} or {@code controller} is null.
     */
    public InputHandler(GameState state, GameController controller) {
        if (state == null || controller == null) {
            throw new IllegalArgumentException("GameState and GameController cannot be null for InputHandler");
        }
        this.gameState = state;
        this.gameController = controller;
    }

    /**
     * Handles key pressed events.
     * Maps specific keys (P, ESC, R) to game control actions (pause, exit, restart).
     * Maps movement keys (Arrows, WASD) and shooting keys (Space, Ctrl) to player actions,
     * but only if the game is actively running (not paused, not game over).
     *
     * @param e The {@link KeyEvent} associated with the key press.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // --- Game Control Keys (handled regardless of game running state, except restart) ---
        switch (keyCode) {
            case KeyEvent.VK_P: // Toggle Pause
                gameController.togglePause();
                return; // Consume event
            case KeyEvent.VK_ESCAPE: // Exit Game
                gameController.exitGame();
                return; // Consume event
            case KeyEvent.VK_R: // Restart Game (only if game is over)
                if (gameState.isGameOver()) {
                    System.out.println("Restart key (R) pressed - Requesting restart.");
                    gameController.requestRestart();
                }
                return; // Consume event
        }

        // --- Player Control Keys (only handled if game is running, not paused, not game over) ---
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
             Player player = gameState.getPlayer();
             // Defensive check, although player should exist if game is running.
             if (player == null) return;

            // Set movement/shooting flags based on key code
            switch (keyCode) {
                case KeyEvent.VK_LEFT: case KeyEvent.VK_A: player.setMovingLeft(true); break;
                case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: player.setMovingRight(true); break;
                case KeyEvent.VK_UP: case KeyEvent.VK_W: player.setMovingUp(true); break;
                case KeyEvent.VK_DOWN: case KeyEvent.VK_S: player.setMovingDown(true); break;
                case KeyEvent.VK_SPACE: case KeyEvent.VK_CONTROL: player.setWantsToShoot(true); break;
            }
        }
    }

    /**
     * Handles key released events.
     * Clears the corresponding movement flags on the {@link Player} object when
     * movement keys (Arrows, WASD) are released. Shooting flag is not cleared here,
     * it's consumed when the bullet is fired.
     *
     * @param e The {@link KeyEvent} associated with the key release.
     */
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        Player player = gameState.getPlayer();
        // Defensive check
        if (player == null) return;

        // Clear movement flags based on released key
        switch (keyCode) {
            case KeyEvent.VK_LEFT: case KeyEvent.VK_A: player.setMovingLeft(false); break;
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: player.setMovingRight(false); break;
            case KeyEvent.VK_UP: case KeyEvent.VK_W: player.setMovingUp(false); break;
            case KeyEvent.VK_DOWN: case KeyEvent.VK_S: player.setMovingDown(false); break;
            // No action needed for releasing shoot key (SPACE/CONTROL) as Player.shoot() resets the flag.
            // No action needed for releasing P, ESC, R.
        }
    }
}


// --- UI Panels ---

/**
 * Represents the Login Screen as a {@link JPanel}.
 * Provides UI elements for username and password input, login and register buttons,
 * a status message label, and a text area to display high scores.
 * Interacts with the {@link DatabaseManager} for user authentication and registration,
 * ensuring secure handling of credentials. Notifies the main application {@link TerribleGame}
 * upon successful login.
 */
class LoginScreen extends JPanel implements ActionListener {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JTextArea highScoreTextArea;
    private final TerribleGame mainApp; // Reference to the main application frame for switching views
    private final DatabaseManager dbManager; // Reference to the secure database manager

    /**
     * Constructs the LoginScreen panel.
     *
     * @param mainApp   A reference to the main {@link TerribleGame} application instance, used for callbacks (e.g., switching panels). Must not be null.
     * @param dbManager The {@link DatabaseManager} instance used for user authentication and registration. Must be the secure, refactored version. Must not be null.
     * @throws IllegalArgumentException if either {@code mainApp} or {@code dbManager} is null.
     */
    public LoginScreen(TerribleGame mainApp, DatabaseManager dbManager) {
        if (mainApp == null || dbManager == null) {
            throw new IllegalArgumentException("MainApp and DatabaseManager cannot be null for LoginScreen");
        }
        this.mainApp = mainApp;
        this.dbManager = dbManager; // Store the injected secure manager
        setupUI(); // Initialize and layout the UI components
    }

    /**
     * Initializes and lays out the Swing components for the login screen.
     * Uses absolute positioning (setLayout(null)). Configures component appearance
     * based on values from {@link AppConfig}.
     */
    private void setupUI() {
        setLayout(null); // Use absolute positioning
        setBackground(AppConfig.LOGIN_BACKGROUND_COLOR);
        setPreferredSize(new Dimension(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT));

        // Define common layout variables
        int centerX = AppConfig.GAME_WIDTH / 2;
        int fieldWidth = 160;
        int labelWidth = 80;
        int startY = 150; // Initial Y position for the first elements
        int fieldSpacing = 40; // Vertical space between input fields
        int buttonWidth = 120;
        int buttonSpacing = 20; // Horizontal space between buttons

        // Username Label and Field
        JLabel usernameLabel = createLabel("Username:", centerX - fieldWidth / 2 - labelWidth, startY);
        usernameField = new JTextField();
        usernameField.setBounds(centerX - fieldWidth / 2, startY, fieldWidth, 25);
        add(usernameLabel);
        add(usernameField);

        // Password Label and Field
        JLabel passwordLabel = createLabel("Password:", centerX - fieldWidth / 2 - labelWidth, startY + fieldSpacing);
        passwordField = new JPasswordField();
        passwordField.setBounds(centerX - fieldWidth / 2, startY + fieldSpacing, fieldWidth, 25);
        add(passwordLabel);
        add(passwordField);

        // Login and Register Buttons
        int totalButtonWidth = buttonWidth * 2 + buttonSpacing; // Calculate total width needed for both buttons and spacing
        loginButton = createButton("Login", centerX - totalButtonWidth / 2, startY + 2 * fieldSpacing, buttonWidth, 30);
        registerButton = createButton("Register", centerX - totalButtonWidth / 2 + buttonWidth + buttonSpacing, startY + 2 * fieldSpacing, buttonWidth, 30);
        add(loginButton);
        add(registerButton);

        // Status Label (for messages like "Login successful", "Invalid credentials", etc.)
        statusLabel = new JLabel("", SwingConstants.CENTER); // Centered text
        statusLabel.setBounds(centerX - (fieldWidth + labelWidth) / 2, startY + 3 * fieldSpacing, fieldWidth + labelWidth, 25);
        statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR); // Default to error color
        add(statusLabel);

        // High Score Display Area
        int hsX = 50; // X position for high score area
        int hsY = startY + 4 * fieldSpacing + 20; // Y position below other elements
        int hsWidth = AppConfig.GAME_WIDTH - 2 * hsX; // Width spanning most of the panel
        int hsHeight = AppConfig.GAME_HEIGHT - hsY - 30; // Height filling remaining space (with bottom margin)

        highScoreTextArea = new JTextArea();
        highScoreTextArea.setEditable(false); // User cannot type in this area
        highScoreTextArea.setForeground(AppConfig.LOGIN_HIGHSCORE_FG_COLOR);
        highScoreTextArea.setBackground(AppConfig.LOGIN_HIGHSCORE_BG_COLOR);
        // Set font based on AppConfig
        highScoreTextArea.setFont(new Font(AppConfig.LOGIN_HIGHSCORE_FONT_NAME,
                                           AppConfig.LOGIN_HIGHSCORE_FONT_STYLE,
                                           AppConfig.LOGIN_HIGHSCORE_FONT_SIZE));
        JScrollPane scrollPane = new JScrollPane(highScoreTextArea); // Add scroll bars if needed
        scrollPane.setBounds(hsX, hsY, hsWidth, hsHeight);
        add(scrollPane);

        // Ensure the panel can receive focus for potential keyboard navigation (though fields are primary)
        setFocusable(true);
    }

    /**
     * Helper method to create and configure a {@link JLabel} for the login form.
     *
     * @param text The text for the label.
     * @param x    The x-coordinate for the label.
     * @param y    The y-coordinate for the label.
     * @return The configured {@link JLabel} instance.
     */
    private JLabel createLabel(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, 80, 25); // Fixed size for alignment
        label.setForeground(AppConfig.LOGIN_LABEL_COLOR);
        return label;
    }

    /**
     * Helper method to create and configure a {@link JButton} for the login form.
     * Adds this {@link LoginScreen} instance as the ActionListener.
     *
     * @param text The text for the button.
     * @param x    The x-coordinate for the button.
     * @param y    The y-coordinate for the button.
     * @param w    The width of the button.
     * @param h    The height of the button.
     * @return The configured {@link JButton} instance.
     */
    private JButton createButton(String text, int x, int y, int w, int h) {
        JButton button = new JButton(text);
        button.setBounds(x, y, w, h);
        button.addActionListener(this); // Register this panel to handle button clicks
        return button;
    }

     /**
      * Fetches the latest high scores from the {@link DatabaseManager} and updates
      * the {@code highScoreTextArea}. Prepends a header line. Handles potential
      * null or error message list from the database manager.
      */
     public void refreshHighScores() {
        List<String> scores = dbManager.getHighScores(); // Fetch scores using the secure DB Manager
        highScoreTextArea.setText("--- High Scores (Top " + AppConfig.HIGH_SCORE_LIMIT + ") ---\n"); // Set header
        if (scores != null) {
            // Append each score line (or message like "No scores") to the text area
            for (String scoreLine : scores) {
                highScoreTextArea.append(scoreLine + "\n");
            }
        } else {
            // Should ideally not happen if getHighScores returns a list even on error, but handle defensively
             highScoreTextArea.append("Error loading scores.\n");
        }
        highScoreTextArea.setCaretPosition(0); // Scroll to the top
     }

     /**
      * Clears the username field, password field, and status label text.
      * Resets the status label color to the default error color.
      * Typically called when switching back to the login screen.
      */
     public void clearForm() {
         usernameField.setText("");
         passwordField.setText("");
         statusLabel.setText("");
         // Reset status label color to default (error color), success color is set explicitly on success.
         statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
     }

    /**
     * Handles action events, specifically button clicks for "Login" and "Register".
     * Retrieves username and password, performs validation, and calls the appropriate
     * {@link DatabaseManager} method ({@code validateUser} or {@code registerUser}).
     * Updates the status label based on the result. On successful login, triggers
     * a delayed switch to the game screen via the {@code mainApp}.
     * **Crucially, clears the password from memory (char array) after use.**
     *
     * @param e The {@link ActionEvent} triggered by a button press.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText().trim(); // Get username, remove leading/trailing whitespace
        char[] passwordChars = passwordField.getPassword(); // Get password as char array for security
        // SECURITY: Convert char[] to String only when needed by BCrypt and clear array afterwards.
        String plainTextPassword = new String(passwordChars);

        // Basic input validation
        if (username.isEmpty() || plainTextPassword.isEmpty()) {
            statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
            statusLabel.setText("Username and password cannot be empty.");
            // SECURITY: Clear the password char array even on validation failure before returning.
            java.util.Arrays.fill(passwordChars, ' '); // Overwrite with spaces
            return;
        }

        try {
            if (e.getSource() == loginButton) {
                // Attempt login using the secure validateUser method
                // SECURITY: dbManager.validateUser uses BCrypt.checkpw internally.
                if (dbManager.validateUser(username, plainTextPassword)) {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                    statusLabel.setText("Login Successful!");
                    // Use a short timer to display success message before switching panel
                    Timer switchTimer = new Timer(500, ae -> mainApp.userLoggedIn(username));
                    switchTimer.setRepeats(false); // Only run once
                    switchTimer.start();
                } else {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                    statusLabel.setText("Login failed. Check credentials.");
                    passwordField.setText(""); // Clear password field on failure for convenience/security
                }
            } else if (e.getSource() == registerButton) {
                 // Attempt registration using the secure registerUser method
                 // SECURITY: dbManager.registerUser uses BCrypt.hashpw internally.
                if (dbManager.registerUser(username, plainTextPassword)) {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                    statusLabel.setText("Registration successful! Please log in.");
                    usernameField.setText(""); // Clear fields after successful registration
                    passwordField.setText("");
                    // Optionally, refresh high scores if registration might affect them (unlikely here)
                    // refreshHighScores();
                } else {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                    // Provide a slightly more generic error for security (avoid confirming exact reason like "username exists")
                    statusLabel.setText("Registration failed (username might exist or error occurred).");
                    passwordField.setText(""); // Clear password field on failure
                }
            }
        } finally {
             // SECURITY: *** CRITICAL ***
             // Always clear the plaintext password char array from memory immediately after it's been used
             // (either for hashing/checking or because validation failed).
             java.util.Arrays.fill(passwordChars, ' '); // Overwrite with spaces or null chars

             // The 'plainTextPassword' String object itself will eventually be garbage collected.
             // Clearing the char array is the most direct and recommended way to remove the sensitive data from memory sooner.
        }
    }
}

/**
 * Represents the main Game Screen as a {@link JPanel}.
 * Responsible for rendering the current {@link GameState}, including the background,
 * player, enemies, bullets, power-ups, UI elements (score, lives), and overlays (pause/game over messages).
 * Relies on the {@link GameState} object passed to it for all data to be drawn.
 */
class GamePanel extends JPanel {
    private final GameState gameState; // Reference to the state object containing all drawable elements

    // Pre-create Font objects for performance
    private final Font uiFont;
    private final Font gameOverLargeFont;
    private final Font gameOverMediumFont;
    private final Font gameOverSmallFont;
    private final Font pauseLargeFont;
    private final Font pauseSmallFont;


    /**
     * Constructs the GamePanel.
     *
     * @param state The {@link GameState} instance containing the data to be rendered. Must not be null.
     * @throws IllegalArgumentException if the provided {@code state} is null.
     */
    public GamePanel(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("GameState cannot be null for GamePanel");
        }
        this.gameState = state;

        // Initialize fonts based on AppConfig values
        uiFont = new Font(AppConfig.GAME_UI_FONT_NAME, AppConfig.GAME_UI_FONT_STYLE, AppConfig.GAME_UI_FONT_SIZE);
        gameOverLargeFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_LARGE_FONT_STYLE, AppConfig.GAME_OVER_LARGE_FONT_SIZE);
        gameOverMediumFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_MEDIUM_FONT_STYLE, AppConfig.GAME_OVER_MEDIUM_FONT_SIZE);
        gameOverSmallFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_SMALL_FONT_STYLE, AppConfig.GAME_OVER_SMALL_FONT_SIZE);
        pauseLargeFont = new Font(AppConfig.PAUSE_FONT_NAME, AppConfig.PAUSE_LARGE_FONT_STYLE, AppConfig.PAUSE_LARGE_FONT_SIZE);
        pauseSmallFont = new Font(AppConfig.PAUSE_FONT_NAME, AppConfig.PAUSE_SMALL_FONT_STYLE, AppConfig.PAUSE_SMALL_FONT_SIZE);

        // Configure panel properties
        setPreferredSize(new Dimension(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT));
        setBackground(AppConfig.GAME_BACKGROUND_COLOR);
        setDoubleBuffered(true); // Use double buffering for smoother rendering
        setFocusable(true); // Panel needs focus to receive key events
        setRequestFocusEnabled(true); // Allow requesting focus programmatically
        setFocusTraversalKeysEnabled(false); // Prevent Tab/Shift-Tab from changing focus away from the panel
    }

    /**
     * Overrides the {@link JComponent#paintComponent(Graphics)} method to custom render the game state.
     * This method is called by Swing whenever the panel needs to be redrawn.
     * It orchestrates the drawing of the background, game objects, UI, and overlays.
     *
     * @param g The {@link Graphics} context provided by Swing for drawing.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Call superclass method first (clears the panel)
        Graphics2D g2d = (Graphics2D) g; // Cast to Graphics2D for more features

        // Enable anti-aliasing for smoother shapes and text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw elements in layers: background -> objects -> UI -> overlays
        drawBackground(g2d);

        // Only draw game objects if the game is considered "running" or if it's game over
        // (to show the final state)
        if (gameState.isRunning() || gameState.isGameOver()) {
            drawGameObjects(g2d);
        }

        drawUI(g2d); // Draw score, lives, etc.
        drawOverlays(g2d); // Draw pause or game over messages if applicable

        // Ensure graphics resources are released (though managed by Swing here)
        g2d.dispose(); // Recommended practice, although Swing often reuses the Graphics object
    }

    /**
     * Fills the panel's background with the configured color.
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_BACKGROUND_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight()); // Fill the entire panel area
    }

    /**
     * Draws all active game objects (player, bullets, enemies, power-ups) onto the screen.
     * Iterates through the lists obtained from {@link GameState}.
     * Uses defensive copies of the lists to potentially mitigate concurrency issues if the lists
     * were modified on another thread (though in this single-threaded Swing Timer design, it's less critical).
     * Includes error handling for safety during rendering.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    private void drawGameObjects(Graphics2D g2d) {
        try {
            // Draw Player
            Player player = gameState.getPlayer();
            if (player != null) {
                player.draw(g2d);
            }

            // Draw Bullets (using a copy of the list)
            List<Bullet> bulletsToDraw = new ArrayList<>(gameState.getBullets());
            for (Bullet bullet : bulletsToDraw) {
                bullet.draw(g2d);
            }

            // Draw Enemies (using a copy of the list)
            List<Enemy> enemiesToDraw = new ArrayList<>(gameState.getEnemies());
            for (Enemy enemy : enemiesToDraw) {
                enemy.draw(g2d);
            }

            // Draw PowerUps (using a copy of the list)
            List<PowerUp> powerUpsToDraw = new ArrayList<>(gameState.getPowerUps());
            for (PowerUp powerUp : powerUpsToDraw) {
                powerUp.draw(g2d);
            }
        } catch (Exception e) {
            // Log rendering errors to avoid crashing the application
            System.err.println("Error during rendering game objects: " + e.getMessage());
            e.printStackTrace(); // Provide stack trace for debugging
        }
    }

    /**
     * Draws the static UI elements, such as the player's score and remaining lives,
     * at the top of the screen. Uses configured colors and fonts.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    private void drawUI(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_UI_TEXT_COLOR);
        g2d.setFont(uiFont);
        // Define margins for placing the text
        int uiMarginX = 10; // Left margin for score
        int uiMarginYTop = 20; // Top margin from window edge
        int livesRightMargin = 100; // Margin from right edge for lives text
        // Draw Score string
        g2d.drawString("Score: " + gameState.getScore(), uiMarginX, uiMarginYTop);
        // Draw Lives string (aligned towards the right)
        g2d.drawString("Lives: " + gameState.getLives(), AppConfig.GAME_WIDTH - livesRightMargin, uiMarginYTop);
    }

    /**
     * Draws overlay messages if the game is paused or over.
     * Renders "GAME OVER" text with score and instructions, or "PAUSED" text with instructions,
     * centered on the screen. Uses specific fonts and colors configured in {@link AppConfig}.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    private void drawOverlays(Graphics2D g2d) {
        // Define vertical offsets from the center for different lines of text
        int yOffsetLarge = -40; // Offset for the main "GAME OVER" / "PAUSED" text
        int yOffsetMedium = 20; // Offset for secondary text (like score)
        int yOffsetSmall = 60;  // Offset for tertiary text (like instructions)

        if (gameState.isGameOver()) {
            // Draw Game Over messages
            drawCenteredString(g2d, "GAME OVER", gameOverLargeFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetLarge);
            drawCenteredString(g2d, "Final Score: " + gameState.getScore(), gameOverMediumFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetMedium);
            drawCenteredString(g2d, "(R: Restart / ESC: Exit to Login)", gameOverSmallFont, AppConfig.GAME_UI_TEXT_COLOR, yOffsetSmall); // Adjusted instruction
        } else if (gameState.isPaused()) {
            // Draw Pause overlay and messages
            // 1. Draw translucent background overlay
            g2d.setColor(AppConfig.PAUSE_OVERLAY_COLOR);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            // 2. Draw "PAUSED" text
            drawCenteredString(g2d, "PAUSED", pauseLargeFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetLarge);
            // 3. Draw instructions
            drawCenteredString(g2d, "(Press 'P' to Resume)", pauseSmallFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetSmall);
        }
    }

    /**
     * Utility method to draw a string centered horizontally and vertically (with an offset) on the panel.
     *
     * @param g2d     The {@link Graphics2D} context to draw on.
     * @param text    The string to draw.
     * @param font    The {@link Font} to use for the string.
     * @param color   The {@link Color} to use for the string.
     * @param yOffset An additional vertical offset (positive moves down, negative moves up) from the vertical center.
     */
    private void drawCenteredString(Graphics2D g2d, String text, Font font, Color color, int yOffset) {
        g2d.setFont(font);
        g2d.setColor(color);
        // Get font metrics to calculate text dimensions
        FontMetrics metrics = g2d.getFontMetrics(font);
        // Calculate x coordinate for horizontal centering
        int x = (AppConfig.GAME_WIDTH - metrics.stringWidth(text)) / 2;
        // Calculate y coordinate for vertical centering, adjusting for font ascent and the yOffset
        int y = (AppConfig.GAME_HEIGHT / 2) - (metrics.getHeight() / 2) + metrics.getAscent() + yOffset;
        g2d.drawString(text, x, y);
    }
}

// --- Game Controller (Interacts with improved DatabaseManager) ---

/**
 * Acts as the central coordinator for the game. Manages the main game loop using a {@link Timer},
 * orchestrates updates between the {@link GameLogic} and {@link GameState}, handles game state transitions
 * (starting, pausing, game over, restarting), interacts with the {@link GamePanel} for rendering updates,
 * and uses the {@link DatabaseManager} to save scores. Also handles requests to exit or restart the game,
 * coordinating with the main {@link TerribleGame} application frame.
 */
class GameController implements ActionListener {
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GamePanel gamePanel; // To trigger repaints
    private final Timer gameTimer;     // Drives the game loop
    private final DatabaseManager dbManager; // Secure DB access for saving scores
    private final TerribleGame mainApp;     // To interact with the main application window (switch views, shutdown)

    /**
     * Constructs the GameController.
     *
     * @param state   The {@link GameState} instance for the game. Must not be null.
     * @param logic   The {@link GameLogic} instance containing game rules. Must not be null.
     * @param panel   The {@link GamePanel} instance for rendering. Must not be null.
     * @param dbMgr   The {@link DatabaseManager} instance for database operations. Must not be null.
     * @param app     The main {@link TerribleGame} application instance. Must not be null.
     * @throws IllegalArgumentException if any dependency is null.
     */
    public GameController(GameState state, GameLogic logic, GamePanel panel, DatabaseManager dbMgr, TerribleGame app) {
        if (state == null || logic == null || panel == null || dbMgr == null || app == null) {
            throw new IllegalArgumentException("All dependencies must be non-null for GameController");
        }
        this.gameState = state;
        this.gameLogic = logic;
        this.gamePanel = panel;
        this.dbManager = dbMgr;
        this.mainApp = app;

        // Setup the game loop timer
        this.gameTimer = new Timer(AppConfig.GAME_TICK_MS, this); // 'this' is the ActionListener
        this.gameTimer.setInitialDelay(0); // Start immediately
        this.gameTimer.setCoalesce(true); // Combine multiple pending events into one
    }

    /**
     * Starts a new game. Initializes game logic, sets the game state to running,
     * starts the game loop timer, and triggers an initial repaint.
     */
    public void startGame() {
        System.out.println("GameController: Starting game...");
        gameLogic.initializeNewGame(); // Reset state and logic
        gameState.setRunning(true);    // Mark game as actively running
        gameState.setPaused(false);    // Ensure game is not paused
        gameState.setGameOver(false); // Ensure game is not over
        if (!gameTimer.isRunning()) {
             gameTimer.start(); // Start the game loop timer if not already running
             System.out.println("Game Started. Timer running (" + AppConfig.GAME_TICK_MS + "ms interval).");
        }
        gamePanel.repaint(); // Initial render
    }

    /**
     * Stops the main game loop timer and sets the game state's running flag to false.
     * Does not reset the game state itself.
     */
    public void stopGameLoop() {
        if (gameTimer.isRunning()) {
            gameTimer.stop();
            System.out.println("Game Loop Stopped.");
        }
         // Always set running to false, even if timer wasn't running (defensive)
        gameState.setRunning(false);
    }

     /**
      * Toggles the paused state of the game.
      * Only effective if the game is currently running and not game over.
      * Triggers a repaint to show/hide the pause overlay.
      */
     public void togglePause() {
        // Check if pausing is allowed in the current state
        if (!gameState.isRunning() || gameState.isGameOver()) {
            System.out.println("Cannot toggle pause. Game running: " + gameState.isRunning() + ", Game over: " + gameState.isGameOver());
            return;
        }
        // Toggle the paused state via the GameState setter
        boolean currentPauseState = gameState.isPaused();
        gameState.setPaused(!currentPauseState); // Setter handles logic
        System.out.println(gameState.isPaused() ? "Game Paused" : "Game Resumed");
        gamePanel.repaint(); // Update the view to show/hide pause overlay
    }

    /**
     * Handles the game over sequence. Stops the game loop, saves the score
     * (if applicable and a user is logged in), and triggers a repaint to display
     * the game over screen.
     */
    private void handleGameOver() {
        System.out.println("GameController: Handling Game Over...");
        stopGameLoop(); // Stops timer and sets isRunning = false via GameState

        System.out.println("Game Over! Final Score: " + gameState.getScore());

        // Save score if conditions are met
        String username = mainApp.getCurrentUsername();
        if (username != null && !username.isEmpty() && gameState.getScore() > 0) {
            System.out.println("Saving score " + gameState.getScore() + " for user: " + username);
            dbManager.saveScore(username, gameState.getScore()); // Use the secure DB Manager
        } else {
             if (username == null || username.isEmpty()) {
                 System.out.println("Score not saved: No user logged in.");
             } else { // score is 0
                 System.out.println("Score not saved: Score is zero.");
             }
        }

        gamePanel.repaint(); // Request repaint to show the game over overlay
    }

    /**
     * Initiates the process of exiting the current game session and potentially the application.
     * Stops the game loop and calls the main application's shutdown method.
     */
    public void exitGame() {
        System.out.println("GameController: Exit requested.");
        stopGameLoop();
        // Delegate the actual application closing/view switching to the main app
        mainApp.switchToLoginScreen(); // Go back to login instead of full shutdown immediately
        // Alternatively, could call mainApp.shutdown() for full exit.
    }

    /**
     * Initiates the process of restarting the game.
     * Stops the current game loop and tells the main application to switch back
     * to the login screen (which effectively allows starting a new game after login).
     */
    public void requestRestart() {
        System.out.println("GameController: Restart requested.");
        stopGameLoop();
        // Delegate view switching back to the main app
        mainApp.switchToLoginScreen();
    }

    /**
     * The main action performed by the {@link #gameTimer}. Called on each timer tick.
     * If the game is running and not paused, it calls the {@link GameLogic#update()} method.
     * After the update, it checks if the game state has transitioned to game over and handles it.
     * Finally, it triggers a repaint of the {@link GamePanel}.
     *
     * @param e The {@link ActionEvent} generated by the game timer.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Only update logic if game is in active running state
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
            gameLogic.update(); // Execute one tick of game logic

            // Check if the update resulted in a game over state
            if (gameState.isGameOver()) {
                handleGameOver(); // Perform game over actions (stops loop, saves score, repaints)
                // No further repaint needed here as handleGameOver calls repaint
            } else {
                gamePanel.repaint(); // Standard repaint during active gameplay
            }
        } else if (gameState.isGameOver() || gameState.isPaused()) {
            // If timer fires while game is paused or over, ensure the panel still repaints
            // to keep the overlay visible, especially if repaint wasn't called elsewhere.
            // This is slightly defensive.
            gamePanel.repaint();
        }
        // If !isRunning && !isGameOver && !isPaused (e.g., stopped before starting), do nothing.
    }
}


// --- Main Application Class (JFrame) ---

/**
 * The main application class for the game, extending {@link JFrame}.
 * Sets up the main window, manages different UI panels (LoginScreen, GamePanel) using a {@link CardLayout},
 * initializes and coordinates core game components ({@link GameState}, {@link GameLogic}, {@link GameController},
 * {@link InputHandler}, {@link DatabaseManager}), handles transitions between login and game screens,
 * maintains the currently logged-in user's state, and manages the application lifecycle (startup, shutdown).
 * Uses the refactored, secure {@link DatabaseManager}.
 */
public class TerribleGame extends JFrame {

    // Core components
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GameController gameController;
    private final InputHandler inputHandler;
    /** The single instance of the secure DatabaseManager used throughout the application. */
    private final DatabaseManager dbManager;

    // UI Panels
    private final GamePanel gamePanel;
    private final LoginScreen loginScreen;

    // UI Management
    private CardLayout cardLayout; // Manages switching between login and game panels
    private final JPanel mainPanel; // The container panel holding the cards

    // Session State
    private String currentUsername = null; // Stores the username of the currently logged-in player

    /**
     * Constructs the main application window {@code TerribleGame}.
     * Initializes all core components (DatabaseManager, GameState, GameLogic, UI Panels, GameController, InputHandler),
     * sets up the JFrame properties, configures the CardLayout, adds panels,
     * sets up listeners, and displays the window, starting with the login screen.
     */
    public TerribleGame() {
        super(AppConfig.APP_TITLE); // Set window title from config

        // --- Initialize Core Components ---
        // 1. Database Manager (Secure Version)
        dbManager = new DatabaseManager();
        // 2. Game State
        gameState = new GameState();
        // 3. Game Logic (depends on GameState)
        gameLogic = new GameLogic(gameState);
        // 4. UI Panels (LoginScreen depends on this JFrame and dbManager, GamePanel on GameState)
        loginScreen = new LoginScreen(this, dbManager);
        gamePanel = new GamePanel(gameState);
        // 5. Game Controller (depends on State, Logic, Panel, DB Manager, and this JFrame)
        gameController = new GameController(gameState, gameLogic, gamePanel, dbManager, this);
        // 6. Input Handler (depends on GameState and GameController)
        inputHandler = new InputHandler(gameState, gameController);

        // --- Setup Window and Layout ---
        setupWindow(); // Configure JFrame properties (close operation, resizable, etc.)
        mainPanel = setupCardLayoutAndPanels(); // Create the CardLayout and add panels
        add(mainPanel, BorderLayout.CENTER); // Add the main panel to the JFrame

        // --- Add Listeners ---
        // Add the key listener specifically to the gamePanel, as it needs focus during gameplay.
        gamePanel.addKeyListener(inputHandler);

        // --- Finalize and Display ---
        pack(); // Size the window based on preferred sizes of components
        setLocationRelativeTo(null); // Center the window on the screen
        setVisible(true); // Make the window visible

        // --- Initial State ---
        switchToLoginScreen(); // Start the application on the login screen
    }

    /**
     * Configures the main JFrame properties, including the close operation
     * (delegating to {@link GameController#exitGame()}), resizability, and focusability.
     * Adds a WindowListener to handle the window closing event gracefully.
     */
    private void setupWindow() {
        // Ensure closing the window triggers the graceful shutdown logic
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // We handle closing manually
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                System.out.println("Window closing event received. Initiating shutdown...");
                // When clicking X, we want to fully shutdown the application
                shutdown();
            }
        });
        setResizable(false); // Prevent resizing the game window
        // The JFrame itself doesn't need focus; the panels inside will handle it.
        setFocusable(false);
    }

    /**
     * Creates the main content panel with a {@link CardLayout} and adds the
     * {@link LoginScreen} and {@link GamePanel} to it using identifiers from {@link AppConfig}.
     *
     * @return The configured {@link JPanel} containing the card layout and panels.
     */
    private JPanel setupCardLayoutAndPanels() {
        cardLayout = new CardLayout();
        JPanel panel = new JPanel(cardLayout); // Create a panel using CardLayout
        // Add the login and game screens to the card layout with unique names
        panel.add(loginScreen, AppConfig.LOGIN_PANEL_ID);
        panel.add(gamePanel, AppConfig.GAME_PANEL_ID);
        return panel;
    }

    // --- View Switching Methods ---

    /**
     * Switches the view to the {@link GamePanel}, requests focus for it,
     * and starts the game via the {@link GameController}.
     * Ensures focus request happens on the Event Dispatch Thread after the panel is shown.
     */
    public void switchToGameScreen() {
        System.out.println("Switching to Game Screen...");
        // Show the GamePanel using CardLayout
        cardLayout.show(mainPanel, AppConfig.GAME_PANEL_ID);
        // Request focus for the game panel *after* it's likely visible.
        // Use invokeLater to ensure this runs on the EDT after the layout change.
        SwingUtilities.invokeLater(() -> {
            boolean focused = gamePanel.requestFocusInWindow();
             if (focused) {
                 System.out.println("GamePanel focus requested successfully.");
             } else {
                 System.err.println("Warning: GamePanel failed to gain focus. Key input might not work.");
             }
             // Start the game logic and timer *after* switching panels and attempting focus.
             gameController.startGame();
        });
    }

     /**
      * Switches the view back to the {@link LoginScreen}. Stops the game loop,
      * clears the current user session, clears the login form, refreshes high scores,
      * and requests focus for the username field.
      * Ensures focus request happens on the Event Dispatch Thread.
      */
     public void switchToLoginScreen() {
         System.out.println("Switching to Login Screen...");
         gameController.stopGameLoop(); // Ensure the game loop is stopped
         this.currentUsername = null; // Clear the logged-in user state
         loginScreen.clearForm(); // Clear username/password fields and status message
         loginScreen.refreshHighScores(); // Update the high score display
         // Show the LoginPanel using CardLayout
         cardLayout.show(mainPanel, AppConfig.LOGIN_PANEL_ID);
         // Request focus for the username field *after* the panel is likely visible.
         SwingUtilities.invokeLater(() -> usernameFieldRequestFocus(loginScreen));
     }

     /**
      * Helper method to request focus for the username field within the LoginScreen.
      * Tries to find the JTextField associated with the "Username:" label, falling back
      * to the first JTextField found, or the LoginScreen panel itself.
      *
      * @param login The {@link LoginScreen} instance where focus should be set.
      */
     private void usernameFieldRequestFocus(LoginScreen login) {
         // Attempt to find the username JTextField more reliably
         Component[] components = login.getComponents();
         Component userField = null;

         // Look for a JTextField that immediately follows a JLabel with text "Username:"
         for (int i = 0; i < components.length - 1; i++) {
             if (components[i] instanceof JLabel && "Username:".equals(((JLabel)components[i]).getText()) &&
                 components[i+1] instanceof JTextField) {
                 userField = components[i+1];
                 break;
             }
         }

         // Fallback: If specific pattern not found, find the first JTextField available.
         if (userField == null) {
             for (Component comp : components) {
                 if (comp instanceof JTextField) {
                     userField = comp;
                     break;
                 }
             }
         }

         // Request focus on the determined component, or the panel as a last resort.
         if (userField != null) {
             userField.requestFocusInWindow();
         } else {
             login.requestFocusInWindow(); // Fallback to the panel itself
         }
     }

    // --- User Management Callback ---

    /**
     * Callback method invoked by {@link LoginScreen} upon successful user login.
     * Stores the username for the current session and initiates the switch to the game screen.
     *
     * @param username The username of the player who successfully logged in. Must not be null or empty.
     */
    public void userLoggedIn(String username) {
        if (username == null || username.trim().isEmpty()) {
            System.err.println("Login attempt with invalid username passed to userLoggedIn.");
            // Optionally switch back to login or show an error, but LoginScreen should prevent this.
            return;
        }
        this.currentUsername = username;
        System.out.println("User '" + username + "' logged in. Proceeding to game.");
        switchToGameScreen(); // Transition to the game view
    }

    /**
     * Gets the username of the currently logged-in player.
     *
     * @return The username string, or {@code null} if no user is currently logged in.
     */
    public String getCurrentUsername() {
        return currentUsername;
    }

    // --- Application Lifecycle ---

    /**
     * Handles the graceful shutdown sequence for the application.
     * Stops the game loop, disposes of the JFrame resources, and exits the JVM.
     * Note: Explicit database connection closing is no longer needed due to
     * the refactored {@link DatabaseManager} using try-with-resources.
     */
    public void shutdown() {
        System.out.println("MainApp: Initiating shutdown...");
        // 1. Stop game loop (important to prevent further updates)
        gameController.stopGameLoop();

        // 2. Database Connection Management:
        // No explicit global close needed here. DatabaseManager uses try-with-resources
        // for connections within its methods, ensuring they are closed after each operation.
        System.out.println("Database connections managed per-operation (try-with-resources). No global close needed.");

        // 3. Dispose of UI resources (releases windowing system resources)
        // This should be done on the Event Dispatch Thread if called from another thread,
        // but is often safe when called from windowClosing event or exit actions.
        dispose();
        System.out.println("JFrame disposed.");

        // 4. Terminate the Java Virtual Machine
        System.out.println("Shutdown complete. Exiting JVM.");
        System.exit(0);
    }

    // --- Main Entry Point ---

    /**
     * The main entry point of the application.
     * Prints startup messages and schedules the creation and display of the
     * {@link TerribleGame} window on the Swing Event Dispatch Thread (EDT).
     * Adds a JVM shutdown hook for cleanup messages (optional).
     *
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        System.out.println("Application starting with title: " + AppConfig.APP_TITLE);
        System.out.println("NOTE: Ensure the necessary libraries (e.g., jBCrypt, dotenv-java, database driver) are included in the classpath.");

        // --- Run GUI on Event Dispatch Thread ---
        // Crucial for Swing applications to ensure thread safety.
        SwingUtilities.invokeLater(() -> {
            // Create and show the main application window.
            new TerribleGame();

            // Optional: Add a JVM shutdown hook for logging or final cleanup actions.
            // Note: Shutdown hooks run during JVM termination, which might be abrupt.
            // Critical cleanup should ideally happen before System.exit().
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("JVM Shutdown Hook executing...");
                // Actions here run *during* JVM shutdown.
            }, "ShutdownCleanupThread"));
        });
    }
}