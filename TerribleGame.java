// Single file structure for demonstration. In a real project, these would be separate files.

// --- Dependency Notes ---
// This code requires the 'java-dotenv' library.
// Add it via Maven:
// <dependency>
//     <groupId>io.github.cdimascio</groupId>
//     <artifactId>java-dotenv</artifactId>
//     <version>5.2.2</version> <!-- Or the latest version -->
// </dependency>
// Or download the JAR and add it to your classpath.

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

// --- Configuration Loading ---
class AppConfig {
    private static final Dotenv dotenv;

    static {
        Dotenv loadedDotenv = null;
        try {
            // Load .env file from the project root (or classpath)
            // ignoreIfMissing() prevents errors if .env doesn't exist
            // systemProperties() allows overriding with system properties
            loadedDotenv = Dotenv.configure()
                                 .ignoreIfMissing()
                                 .systemProperties()
                                 .load();
            System.out.println(".env file loaded successfully (or ignored if missing).");
        } catch (DotenvException e) {
            System.err.println("Could not load .env file: " + e.getMessage());
            // Application can continue with default values
        }
        dotenv = loadedDotenv; // Assign the loaded (or null) dotenv instance
    }

    // Helper to load String environment variables with defaults
    private static String loadStringEnv(String varName, String defaultValue) {
        if (dotenv == null) return defaultValue;
        return dotenv.get(varName, defaultValue);
    }

    // Helper to load Integer environment variables with defaults
    private static int loadIntEnv(String varName, int defaultValue) {
        if (dotenv == null) return defaultValue;
        try {
            return Integer.parseInt(dotenv.get(varName, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid integer format for env var '" + varName + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    // Helper to load Double environment variables with defaults
    private static double loadDoubleEnv(String varName, double defaultValue) {
         if (dotenv == null) return defaultValue;
        try {
            return Double.parseDouble(dotenv.get(varName, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid double format for env var '" + varName + "'. Using default: " + defaultValue);
            return defaultValue;
        }
    }

    // Helper to load Color environment variables with defaults (supports hex #RRGGBB or #AARRGGBB)
    private static Color loadColorEnv(String varName, Color defaultValue) {
        if (dotenv == null) return defaultValue;
        String colorStr = dotenv.get(varName, "");
        if (colorStr.isEmpty()) {
            return defaultValue;
        }
        try {
            // Color.decode handles hex strings like "#RRGGBB" or "0xRRGGBB"
            // It doesn't handle alpha directly in #AARRGGBB format well, so we parse manually if needed
            if (colorStr.startsWith("#")) {
                if (colorStr.length() == 9) { // #AARRGGBB
                    long colorValue = Long.parseLong(colorStr.substring(1), 16);
                    int alpha = (int) ((colorValue >> 24) & 0xFF);
                    int red = (int) ((colorValue >> 16) & 0xFF);
                    int green = (int) ((colorValue >> 8) & 0xFF);
                    int blue = (int) (colorValue & 0xFF);
                    return new Color(red, green, blue, alpha);
                } else if (colorStr.length() == 7) { // #RRGGBB
                     return Color.decode(colorStr);
                } else {
                    throw new NumberFormatException("Invalid hex color format length.");
                }
            } else {
                // Try standard color names (case-insensitive) - less robust
                try {
                    return (Color) Color.class.getField(colorStr.toUpperCase()).get(null);
                } catch (Exception fieldEx) {
                     System.err.println("Warning: Could not parse color name '" + colorStr + "'. Using default.");
                     return defaultValue;
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid color format for env var '" + varName + "' (use #RRGGBB or #AARRGGBB). Using default.");
            return defaultValue;
        }
    }

     // Helper to load Font Style environment variables with defaults
    private static int loadFontStyleEnv(String varName, int defaultValue) {
        if (dotenv == null) return defaultValue;
        String styleStr = dotenv.get(varName, "").toUpperCase();
        switch (styleStr) {
            case "PLAIN": return Font.PLAIN;
            case "BOLD": return Font.BOLD;
            case "ITALIC": return Font.ITALIC;
            case "BOLD_ITALIC": return Font.BOLD | Font.ITALIC;
            default:
                System.err.println("Warning: Invalid font style for env var '" + varName + "' (use PLAIN, BOLD, ITALIC). Using default.");
                return defaultValue;
        }
    }


    // --- Game Window & Timing ---
    public static final String APP_TITLE = loadStringEnv("APP_TITLE", "The Less Terrible Space Game (Configured)");
    public static final int GAME_WIDTH = loadIntEnv("GAME_WIDTH", 800);
    public static final int GAME_HEIGHT = loadIntEnv("GAME_HEIGHT", 600);
    public static final int GAME_TICK_MS = loadIntEnv("GAME_TICK_MS", 16); // Approx 60 FPS default
    public static final int BOTTOM_UI_BUFFER = loadIntEnv("BOTTOM_UI_BUFFER", 30); // Space at the bottom for UI/boundary

    // --- Database ---
    public static final String DATABASE_URL = loadStringEnv("DATABASE_URL", "jdbc:sqlite:terrible_game_data.db");
    public static final String JDBC_DRIVER = loadStringEnv("JDBC_DRIVER", "org.sqlite.JDBC");
    public static final int HIGH_SCORE_LIMIT = loadIntEnv("HIGH_SCORE_LIMIT", 10);

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
    public static final double ENEMY_SPAWN_CHANCE = loadDoubleEnv("ENEMY_SPAWN_CHANCE", 0.05); // 5% per update tick
    public static final int ENEMY_BASE_SPEED = loadIntEnv("ENEMY_BASE_SPEED", 2);
    public static final int ENEMY_SPEED_VARIATION = loadIntEnv("ENEMY_SPEED_VARIATION", 3); // Speed = BASE + random(0 to VARIATION-1)
    public static final int ENEMY_BASE_SIZE = loadIntEnv("ENEMY_BASE_SIZE", 20);
    public static final int ENEMY_SIZE_VARIATION = loadIntEnv("ENEMY_SIZE_VARIATION", 30); // Size = BASE + random(0 to VARIATION-1)
    public static final int ENEMY_SCORE_VALUE = loadIntEnv("ENEMY_SCORE_VALUE", 10);
    public static final Color ENEMY_COLOR = loadColorEnv("ENEMY_COLOR", Color.RED);

    // --- PowerUp ---
    public static final int MAX_POWERUPS = loadIntEnv("MAX_POWERUPS", 5);
    public static final double POWERUP_SPAWN_CHANCE = loadDoubleEnv("POWERUP_SPAWN_CHANCE", 0.02); // 2% per update tick
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

    public static final Color PAUSE_OVERLAY_COLOR = loadColorEnv("PAUSE_OVERLAY_COLOR", new Color(0, 0, 0, 150)); // Default: Transparent black
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


// --- Core Data Models (Replacing int[]) ---

/**
 * Abstract base class for all objects appearing in the game world.
 * Encapsulates position, size, and provides a method for getting bounds.
 */
abstract class GameObject {
    protected int x, y, width, height;
    protected Rectangle bounds;

    public GameObject(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new Rectangle(x, y, width, height);
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // Update bounds based on current x, y
    protected void updateBounds() {
        bounds.setLocation(x, y);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    // Abstract methods to be implemented by subclasses
    public abstract void update(); // Update object state (e.g., position)
    public abstract void draw(Graphics2D g2d); // Draw the object

    public boolean isOutOfBounds(int screenHeight) {
        // Check if fully below or fully above screen (using y coordinate only for top-down)
        return y > screenHeight || y + height < 0;
    }

    public boolean intersects(GameObject other) {
        return this.bounds.intersects(other.getBounds());
    }
}

/**
 * Represents the player's spaceship.
 * Encapsulates position, movement state, and drawing logic.
 */
class Player extends GameObject {
    private int speed;
    private boolean movingLeft, movingRight, movingUp, movingDown;
    private boolean wantsToShoot = false;

    public Player(int startX, int startY) {
        // Use configuration values for size and speed
        super(startX, startY, AppConfig.PLAYER_WIDTH, AppConfig.PLAYER_HEIGHT);
        this.speed = AppConfig.PLAYER_SPEED;
    }

    public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
    public void setMovingUp(boolean movingUp) { this.movingUp = movingUp; }
    public void setMovingDown(boolean movingDown) { this.movingDown = movingDown; }
    public void setWantsToShoot(boolean wantsToShoot) { this.wantsToShoot = wantsToShoot; }
    public boolean isWantsToShoot() { return wantsToShoot; }

    @Override
    public void update() {
        int dx = 0;
        int dy = 0;

        if (movingLeft) dx -= speed;
        if (movingRight) dx += speed;
        if (movingUp) dy -= speed;
        if (movingDown) dy += speed;

        x += dx;
        y += dy;

        // Apply boundary checks using configuration values
        x = Math.max(0, x);
        x = Math.min(AppConfig.GAME_WIDTH - width, x);
        y = Math.max(0, y);
        y = Math.min(AppConfig.GAME_HEIGHT - height - AppConfig.BOTTOM_UI_BUFFER, y);

        updateBounds(); // Update bounds after moving
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.PLAYER_COLOR); // Use configured color
        g2d.fillRect(x, y, width, height);
    }

    // Player-specific action: create a bullet
    public Bullet shoot() {
         this.wantsToShoot = false; // Consume the trigger
         // Use configuration values for bullet size offset
         int bulletX = this.x + this.width / 2 - AppConfig.BULLET_WIDTH / 2;
         int bulletY = this.y - AppConfig.BULLET_HEIGHT;
         return new Bullet(bulletX, bulletY);
    }
}

/**
 * Represents a bullet fired by the player.
 */
class Bullet extends GameObject {
    private int speed;

    public Bullet(int x, int y) {
        // Use configuration values for size and speed
        super(x, y, AppConfig.BULLET_WIDTH, AppConfig.BULLET_HEIGHT);
        this.speed = AppConfig.BULLET_SPEED;
    }

    @Override
    public void update() {
        y -= speed;
        updateBounds();
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.BULLET_COLOR); // Use configured color
        g2d.fillRect(x, y, width, height);
    }
}

/**
 * Represents an enemy ship.
 */
class Enemy extends GameObject {
    private int speed;

    public Enemy(int x, int y, int width, int height, int speed) {
        super(x, y, width, height);
        this.speed = speed;
    }

    @Override
    public void update() {
        y += speed;
        updateBounds();
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.ENEMY_COLOR); // Use configured color
        g2d.fillRect(x, y, width, height);
    }
}

/**
 * Represents a power-up item.
 */
class PowerUp extends GameObject {
    private int speed;
    // Could add 'type' field for different power-up effects

    public PowerUp(int x, int y, int speed) {
        // Use configuration values for size
        super(x, y, AppConfig.POWERUP_WIDTH, AppConfig.POWERUP_HEIGHT);
        this.speed = speed;
    }

    @Override
    public void update() {
        y += speed;
        updateBounds();
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.POWERUP_COLOR); // Use configured color
        g2d.fillOval(x, y, width, height); // Draw as oval
    }

    // Could have an applyEffect(Player player) method here
    public void applyEffect(GameState gameState) {
        gameState.increaseScore(AppConfig.POWERUP_SCORE_VALUE); // Use configured score value
        // Example: gameState.increaseLives(1);
        // Example: player.increaseFireRate();
    }
}

// --- Database Management (Instance-based) ---
// Manages database connection and operations for users and scores.
class DatabaseManager {
    private Connection databaseConnection = null;
    private final String dbUrl;
    private final String jdbcDriver;

    public DatabaseManager() {
        this.dbUrl = AppConfig.DATABASE_URL; // Use configured URL
        this.jdbcDriver = AppConfig.JDBC_DRIVER; // Use configured driver
        getConnection(); // Try initial connection
        initializeDatabaseTables();
    }

    private Connection getConnection() {
        if (databaseConnection == null) {
            try {
                Class.forName(jdbcDriver); // Use configured driver class name
                databaseConnection = DriverManager.getConnection(dbUrl);
                System.out.println("Database connection established (" + dbUrl + ").");
            } catch (ClassNotFoundException e) {
                handleError("JDBC Driver not found! Check classpath: " + jdbcDriver, e);
                System.exit(1); // Critical failure
            } catch (SQLException e) {
                handleError("Database connection failed (" + dbUrl + ")", e);
                // Allow application to continue but DB features won't work
            }
        } else {
            // Check if the existing connection is still valid
            try {
                if (databaseConnection.isClosed() || !databaseConnection.isValid(2)) {
                    System.out.println("Database connection was closed or invalid, reopening...");
                    databaseConnection = DriverManager.getConnection(dbUrl);
                    System.out.println("Database connection re-established.");
                }
            } catch (SQLException e) {
                handleError("Failed to check/reopen database connection", e);
                databaseConnection = null; // Force re-establishment next time
            }
        }
        return databaseConnection;
    }

    private void initializeDatabaseTables() {
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("Cannot initialize DB tables, connection is null.");
            return;
        }
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    " username TEXT UNIQUE NOT NULL," +
                                    " password TEXT NOT NULL" + // Plain text - BAD for production!
                                    ");";
        String createHighScoresTable = "CREATE TABLE IF NOT EXISTS high_scores (" +
                                      " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                      " username TEXT NOT NULL," +
                                      " score INTEGER NOT NULL," +
                                      " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                                      ");";

        try (Statement statement = conn.createStatement()) {
            statement.execute(createUsersTable);
            statement.execute(createHighScoresTable);
            System.out.println("Database tables checked/created.");
        } catch (SQLException e) {
            handleError("Error creating database tables", e);
        }
    }

    // Use PreparedStatement for safety and clarity
    public boolean registerUser(String username, String password) {
        if (!isInputValid(username, password)) return false;
        Connection conn = getConnection();
        if (conn == null) return false;

        String checkUserSql = "SELECT id FROM users WHERE username = ?";
        String insertUserSql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("Registration failed: Username '" + username + "' already exists.");
                rs.close();
                return false; // Username exists
            }
            rs.close();
        } catch (SQLException e) {
            handleError("Error checking username during registration", e);
            return false;
        }

        try (PreparedStatement insertStmt = conn.prepareStatement(insertUserSql)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, password); // HASH THIS PASSWORD in a real app
            int result = insertStmt.executeUpdate();
            if (result > 0) {
                 System.out.println("User '" + username + "' registered successfully.");
                 return true;
            } else {
                System.err.println("Registration failed: Insert returned 0 rows affected.");
                return false;
            }
        } catch (SQLException e) {
            handleError("Error inserting new user", e);
            return false;
        }
    }

    public boolean validateUser(String username, String password) {
        if (!isInputValid(username, password)) return false;
        Connection conn = getConnection();
        if (conn == null) return false;

        String querySql = "SELECT password FROM users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(querySql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                rs.close();
                // COMPARE HASHES in a real app
                return storedPassword.equals(password);
            } else {
                rs.close();
                return false; // Username not found
            }
        } catch (SQLException e) {
            handleError("Error validating user", e);
            return false;
        }
    }

    public void saveScore(String username, int score) {
        if (username == null || username.trim().isEmpty() || score < 0) {
            System.err.println("Invalid data for saving score (User: " + username + ", Score: " + score + ")");
            return;
        }
        Connection conn = getConnection();
        if (conn == null) return;

        String insertScoreSql = "INSERT INTO high_scores (username, score) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertScoreSql)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();
            System.out.println("Score " + score + " for user " + username + " saved.");
        } catch (SQLException e) {
            handleError("Error saving score", e);
        }
    }

    public List<String> getHighScores() { // Use configured limit internally
        int limit = AppConfig.HIGH_SCORE_LIMIT;
        List<String> scores = new ArrayList<>();
        Connection conn = getConnection();
        if (conn == null) {
             scores.add("Database connection error.");
             return scores;
        }

        String queryHighScores = "SELECT username, score FROM high_scores ORDER BY score DESC LIMIT ?";
        try (PreparedStatement pstmt = conn.prepareStatement(queryHighScores)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            int rank = 1;
            while (rs.next()) {
                String username = rs.getString("username");
                int scoreValue = rs.getInt("score");
                // Simple formatting, could be made more sophisticated
                scores.add(String.format("%d. %-15s : %d", rank++, username, scoreValue));
            }
            rs.close();
            if (rank == 1) {
                scores.add("No scores recorded yet.");
            }
        } catch (SQLException e) {
            handleError("Error fetching high scores", e);
            scores.add("Error loading scores.");
        }
        return scores;
    }

    public void closeConnection() {
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                System.out.println("Closing database connection.");
                databaseConnection.close();
                databaseConnection = null;
            }
        } catch (SQLException e) {
            handleError("Error closing database connection", e);
        }
    }

    // Helper for input validation
    private boolean isInputValid(String username, String password) {
        return username != null && !username.trim().isEmpty() && password != null && !password.isEmpty();
    }

    // Centralized error handling
    private void handleError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        // e.printStackTrace(); // Optionally print stack trace for debugging
    }
}

// --- User Session Management (Now managed by the main application) ---
// The concept of a static UserSession is removed.
// The main application (TerribleGame) will hold the current username.


// --- Game State (Encapsulated) ---
// Holds the current state of the game, including objects and status.
class GameState {
    private Player player;
    private List<Enemy> enemies;
    private List<PowerUp> powerUps;
    private List<Bullet> bullets;
    private int score;
    private int lives;
    private boolean isGameOver;
    private boolean isPaused;
    private boolean isRunning; // Indicates if the game logic loop should run

    public GameState() {
        // Initialize with default values, call reset() for actual game start setup
        this.player = new Player(0, 0); // Temporary position, reset will fix
        this.enemies = new ArrayList<>();
        this.powerUps = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.score = 0;
        this.lives = AppConfig.INITIAL_LIVES; // Use configured initial lives
        this.isGameOver = false;
        this.isPaused = false;
        this.isRunning = false; // Not running initially
    }

    // Resets the game state for a new game
    public void reset() {
        // Use configured dimensions for initial player placement
        player.x = AppConfig.GAME_WIDTH / 2 - AppConfig.PLAYER_WIDTH / 2;
        player.y = AppConfig.GAME_HEIGHT - AppConfig.BOTTOM_UI_BUFFER - AppConfig.PLAYER_HEIGHT; // Place above buffer
        player.updateBounds(); // Ensure bounds are correct after reset

        enemies.clear();
        powerUps.clear();
        bullets.clear();

        score = 0;
        lives = AppConfig.INITIAL_LIVES; // Use configured initial lives
        isGameOver = false;
        isPaused = false;
        isRunning = false; // Set to true when game actually starts playing

        // Reset player movement state
        player.setMovingLeft(false);
        player.setMovingRight(false);
        player.setMovingUp(false);
        player.setMovingDown(false);
        player.setWantsToShoot(false);
    }

    // Getters for accessing state (provide controlled access)
    public Player getPlayer() { return player; }
    public List<Enemy> getEnemies() { return enemies; } // Consider returning immutable list or copy if needed
    public List<PowerUp> getPowerUps() { return powerUps; }
    public List<Bullet> getBullets() { return bullets; }
    public int getScore() { return score; }
    public int getLives() { return lives; }
    public boolean isGameOver() { return isGameOver; }
    public boolean isPaused() { return isPaused; }
    public boolean isRunning() { return isRunning; }

    // Setters/Modifiers for state changes
    public void setGameOver(boolean gameOver) { isGameOver = gameOver; }
    public void setPaused(boolean paused) { isPaused = paused; }
    public void setRunning(boolean running) { isRunning = running; }

    public void decreaseLives() {
        if (lives > 0) {
            lives--;
        }
        if (lives <= 0) {
            isGameOver = true;
        }
    }

    public void increaseScore(int amount) {
        score += amount;
    }

    // Methods to manage game objects
    public void addBullet(Bullet bullet) { bullets.add(bullet); }
    public void addEnemy(Enemy enemy) { enemies.add(enemy); }
    public void addPowerUp(PowerUp powerUp) { powerUps.add(powerUp); }

    // Use Iterators for safe removal during updates
    public void removeBullets(List<Bullet> bulletsToRemove) { bullets.removeAll(bulletsToRemove); }
    public void removeEnemies(List<Enemy> enemiesToRemove) { enemies.removeAll(enemiesToRemove); }
    public void removePowerUps(List<PowerUp> powerUpsToRemove) { powerUps.removeAll(powerUpsToRemove); }
}


// --- Game Logic (Operates on GameState and GameObjects) ---
// Contains the rules and updates for the game simulation.
class GameLogic {
    private Random randomGenerator = new Random();
    private GameState gameState; // Holds the state this logic operates on

    public GameLogic(GameState state) {
        this.gameState = state;
    }

    // Called when a new game starts
    public void initializeNewGame() {
        gameState.reset(); // Reset state variables
        // Initial population of game objects (can configure initial counts too if desired)
        // For now, keep the hardcoded initial spawn counts
        for (int i = 0; i < 5; i++) {
            spawnEnemy();
        }
        for (int i = 0; i < 3; i++) {
            spawnPowerUp();
        }
        gameState.setRunning(true); // Mark game as actively running
    }

    // Main update method called each game tick
    public void update() {
        // Only update if the game is running and not paused or over
        if (!gameState.isRunning() || gameState.isPaused() || gameState.isGameOver()) {
            return;
        }

        // Update player
        gameState.getPlayer().update();
        handleShooting();

        // Update bullets and remove OOB ones
        updateGameObjects(gameState.getBullets());

        // Update enemies and remove OOB ones
        updateGameObjects(gameState.getEnemies());

        // Update powerups and remove OOB ones
        updateGameObjects(gameState.getPowerUps());

        // Handle collisions
        handleCollisions();

        // Handle spawning new objects
        handleSpawning();

        // Check for game over condition (redundant if decreaseLives handles it, but safe)
        if (gameState.getLives() <= 0) {
            gameState.setGameOver(true);
        }
    }

    // Generic update and OOB removal for any GameObject list
    private <T extends GameObject> void updateGameObjects(List<T> list) {
        Iterator<T> iterator = list.iterator();
        while (iterator.hasNext()) {
            T obj = iterator.next();
            obj.update();
            if (obj.isOutOfBounds(AppConfig.GAME_HEIGHT)) { // Use configured height
                iterator.remove();
            }
        }
    }

    private void handleShooting() {
        Player player = gameState.getPlayer();
        if (player.isWantsToShoot()) {
            gameState.addBullet(player.shoot()); // Player creates and returns a new bullet
        }
    }

    private void handleCollisions() {
        Player player = gameState.getPlayer();
        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<PowerUp> powerUpsToRemove = new ArrayList<>();

        // 1. Player vs Enemy collisions
        for (Enemy enemy : gameState.getEnemies()) {
            if (!enemiesToRemove.contains(enemy) && player.intersects(enemy)) {
                gameState.decreaseLives();
                enemiesToRemove.add(enemy);
                if (gameState.isGameOver()) return; // Stop checking if game over
            }
        }

        // 2. Bullet vs Enemy collisions
        for (Bullet bullet : gameState.getBullets()) {
             if (bulletsToRemove.contains(bullet)) continue; // Skip already marked bullets

            for (Enemy enemy : gameState.getEnemies()) {
                 if (enemiesToRemove.contains(enemy)) continue; // Skip already hit enemies

                if (bullet.intersects(enemy)) {
                    bulletsToRemove.add(bullet);
                    enemiesToRemove.add(enemy);
                    gameState.increaseScore(AppConfig.ENEMY_SCORE_VALUE); // Use configured score
                    break; // One bullet hits one enemy
                }
            }
        }

        // 3. Player vs PowerUp collisions
        for (PowerUp powerUp : gameState.getPowerUps()) {
             if (!powerUpsToRemove.contains(powerUp) && player.intersects(powerUp)) {
                powerUp.applyEffect(gameState); // Apply the power-up effect (uses configured score internally)
                powerUpsToRemove.add(powerUp);
            }
        }

        // Remove collided objects from the game state
        gameState.removeEnemies(enemiesToRemove);
        gameState.removeBullets(bulletsToRemove);
        gameState.removePowerUps(powerUpsToRemove);
    }

    private void handleSpawning() {
        // Spawn enemies based on chance and count limit (using configured values)
        if (gameState.getEnemies().size() < AppConfig.MAX_ENEMIES && randomGenerator.nextDouble() < AppConfig.ENEMY_SPAWN_CHANCE) {
            spawnEnemy();
        }
        // Spawn power-ups based on chance and count limit (using configured values)
        if (gameState.getPowerUps().size() < AppConfig.MAX_POWERUPS && randomGenerator.nextDouble() < AppConfig.POWERUP_SPAWN_CHANCE) {
            spawnPowerUp();
        }
    }

    private void spawnEnemy() {
        // Use configured base size and variation
        int size = AppConfig.ENEMY_BASE_SIZE + (AppConfig.ENEMY_SIZE_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SIZE_VARIATION) : 0);
        int enemyX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - size); // Use configured width
        int enemyY = -size; // Start just above the screen
        // Use configured base speed and variation
        int enemySpeed = AppConfig.ENEMY_BASE_SPEED + (AppConfig.ENEMY_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SPEED_VARIATION) : 0);
        gameState.addEnemy(new Enemy(enemyX, enemyY, size, size, enemySpeed));
    }

    private void spawnPowerUp() {
        // Use configured width and height
        int powerUpX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - AppConfig.POWERUP_WIDTH);
        int powerUpY = -AppConfig.POWERUP_HEIGHT; // Start just above the screen
        // Use configured base speed and variation
        int powerUpSpeed = AppConfig.POWERUP_BASE_SPEED + (AppConfig.POWERUP_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.POWERUP_SPEED_VARIATION) : 0);
        gameState.addPowerUp(new PowerUp(powerUpX, powerUpY, powerUpSpeed));
    }
}


// --- Input Handling (Interacts with Player and GameController) ---
// Translates keyboard events into actions on the Player object or GameController.
class InputHandler extends KeyAdapter { // Using KeyAdapter for conciseness
    private GameState gameState;
    private GameController gameController; // To handle pause/exit actions

    public InputHandler(GameState state, GameController controller) {
        this.gameState = state;
        this.gameController = controller;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        Player player = gameState.getPlayer();

        // Handle global keys first, regardless of game state
        switch (keyCode) {
            case KeyEvent.VK_P: // Pause Key
                if (gameState.isRunning() && !gameState.isGameOver()) {
                    gameController.togglePause();
                }
                return;
            case KeyEvent.VK_ESCAPE: // Exit Key
                gameController.exitGame();
                return;
            case KeyEvent.VK_R: // Restart Key
                if (gameState.isGameOver()) {
                    System.out.println("Restart key (R) pressed - Triggering restart via controller.");
                    gameController.requestRestart();
                }
                return;
        }

        // Process game input if the game is running and not paused/over
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
            switch (keyCode) {
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    player.setMovingLeft(true);
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    player.setMovingRight(true);
                    break;
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    player.setMovingUp(true);
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    player.setMovingDown(true);
                    break;
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_CONTROL:
                    player.setWantsToShoot(true); // Signal intent to shoot
                    break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        Player player = gameState.getPlayer();

        // Only process movement/shooting keys that need a 'released' state
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                player.setMovingLeft(false);
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                player.setMovingRight(false);
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                player.setMovingUp(false);
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                player.setMovingDown(false);
                break;
             case KeyEvent.VK_SPACE:
             case KeyEvent.VK_CONTROL:
                // If shooting required holding, you'd set wantsToShoot = false here.
                // Currently it's a trigger on press, consumed immediately.
                break;
        }
    }
}


// --- UI Panels ---

/**
 * Login Screen Panel - Handles user login and registration UI.
 * Interacts with DatabaseManager and the main application frame.
 */
class LoginScreen extends JPanel implements ActionListener {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JTextArea highScoreTextArea;
    private TerribleGame mainApp; // Reference to the main application
    private DatabaseManager dbManager; // Instance of DatabaseManager

    public LoginScreen(TerribleGame mainApp, DatabaseManager dbManager) {
        this.mainApp = mainApp;
        this.dbManager = dbManager;
        setupUI();
        refreshHighScores(); // Initial load
    }

    private void setupUI() {
        setLayout(null); // Using null layout - consider alternatives for robustness
        setBackground(AppConfig.LOGIN_BACKGROUND_COLOR); // Use configured color
        setPreferredSize(new Dimension(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT));

        // --- Dynamic Layout (Example - adjust as needed if not using null layout) ---
        // Instead of hardcoded numbers, calculate positions based on GAME_WIDTH/HEIGHT
        int centerX = AppConfig.GAME_WIDTH / 2;
        int fieldWidth = 160;
        int labelWidth = 80;
        int startY = 150;
        int fieldSpacing = 40;
        int buttonWidth = 120;
        int buttonSpacing = 20; // Space between buttons

        // Username
        JLabel usernameLabel = createLabel("Username:", centerX - fieldWidth / 2 - labelWidth, startY);
        usernameField = new JTextField();
        usernameField.setBounds(centerX - fieldWidth / 2, startY, fieldWidth, 25);
        add(usernameLabel);
        add(usernameField);

        // Password
        JLabel passwordLabel = createLabel("Password:", centerX - fieldWidth / 2 - labelWidth, startY + fieldSpacing);
        passwordField = new JPasswordField();
        passwordField.setBounds(centerX - fieldWidth / 2, startY + fieldSpacing, fieldWidth, 25);
        add(passwordLabel);
        add(passwordField);

        // Buttons (Centered below fields)
        int totalButtonWidth = buttonWidth * 2 + buttonSpacing;
        loginButton = createButton("Login", centerX - totalButtonWidth / 2, startY + 2 * fieldSpacing, buttonWidth, 30);
        registerButton = createButton("Register", centerX - totalButtonWidth / 2 + buttonWidth + buttonSpacing, startY + 2 * fieldSpacing, buttonWidth, 30);
        add(loginButton);
        add(registerButton);

        // Status Label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setBounds(centerX - (fieldWidth + labelWidth) / 2, startY + 3 * fieldSpacing, fieldWidth + labelWidth, 25);
        statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR); // Default to error color
        add(statusLabel);

        // High Scores Area
        int hsX = 50;
        int hsY = startY + 4 * fieldSpacing + 20; // Position below status label
        int hsWidth = AppConfig.GAME_WIDTH - 2 * hsX;
        int hsHeight = AppConfig.GAME_HEIGHT - hsY - 30; // Fill remaining space approx

        highScoreTextArea = new JTextArea();
        highScoreTextArea.setEditable(false);
        highScoreTextArea.setForeground(AppConfig.LOGIN_HIGHSCORE_FG_COLOR);
        highScoreTextArea.setBackground(AppConfig.LOGIN_HIGHSCORE_BG_COLOR);
        highScoreTextArea.setFont(new Font(AppConfig.LOGIN_HIGHSCORE_FONT_NAME,
                                           AppConfig.LOGIN_HIGHSCORE_FONT_STYLE,
                                           AppConfig.LOGIN_HIGHSCORE_FONT_SIZE));
        JScrollPane scrollPane = new JScrollPane(highScoreTextArea); // Add scroll pane
        scrollPane.setBounds(hsX, hsY, hsWidth, hsHeight);
        add(scrollPane);
    }

    private JLabel createLabel(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, 80, 25); // Keep size fixed for now
        label.setForeground(AppConfig.LOGIN_LABEL_COLOR); // Use configured color
        return label;
    }

    private JButton createButton(String text, int x, int y, int w, int h) {
        JButton button = new JButton(text);
        button.setBounds(x, y, w, h);
        button.addActionListener(this);
        // Could configure button appearance here too
        return button;
    }

     public void refreshHighScores() {
        List<String> scores = dbManager.getHighScores(); // Uses configured limit internally
        highScoreTextArea.setText("--- High Scores (Top " + AppConfig.HIGH_SCORE_LIMIT + ") ---\n");
        for (String scoreLine : scores) {
            highScoreTextArea.append(scoreLine + "\n");
        }
        highScoreTextArea.setCaretPosition(0); // Scroll to top
     }

     // Clear input fields and status
     public void clearForm() {
         usernameField.setText("");
         passwordField.setText("");
         statusLabel.setText("");
         statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR); // Reset color
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        if (e.getSource() == loginButton) {
            if (dbManager.validateUser(username, password)) {
                statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                statusLabel.setText("Login Successful!");
                // Short delay before switching to show message (optional)
                Timer timer = new Timer(500, ae -> mainApp.userLoggedIn(username));
                timer.setRepeats(false);
                timer.start();
                // mainApp.userLoggedIn(username); // Notify main app of successful login
            } else {
                statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                statusLabel.setText("Login failed. Check username/password.");
                passwordField.setText(""); // Clear password on failure
            }
        } else if (e.getSource() == registerButton) {
            if (dbManager.registerUser(username, password)) {
                statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                statusLabel.setText("Registration successful! You can now log in.");
                usernameField.setText(""); // Clear fields after success
                passwordField.setText("");
                refreshHighScores(); // Update high scores display
            } else {
                statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                // More specific error might be possible if registerUser returned different codes
                statusLabel.setText("Registration failed (username might exist).");
                passwordField.setText(""); // Clear password on failure
            }
        }
    }
}

/**
 * Game Screen Panel - Responsible for rendering the game state.
 * Reads data from GameState and uses Graphics2D for drawing.
 */
class GamePanel extends JPanel {
    private GameState gameState; // Reference to the state to render

    // Pre-create Font objects based on config for efficiency
    private final Font uiFont = new Font(AppConfig.GAME_UI_FONT_NAME, AppConfig.GAME_UI_FONT_STYLE, AppConfig.GAME_UI_FONT_SIZE);
    private final Font gameOverLargeFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_LARGE_FONT_STYLE, AppConfig.GAME_OVER_LARGE_FONT_SIZE);
    private final Font gameOverMediumFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_MEDIUM_FONT_STYLE, AppConfig.GAME_OVER_MEDIUM_FONT_SIZE);
    private final Font gameOverSmallFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_SMALL_FONT_STYLE, AppConfig.GAME_OVER_SMALL_FONT_SIZE);
    private final Font pauseLargeFont = new Font(AppConfig.PAUSE_FONT_NAME, AppConfig.PAUSE_LARGE_FONT_STYLE, AppConfig.PAUSE_LARGE_FONT_SIZE);
    private final Font pauseSmallFont = new Font(AppConfig.PAUSE_FONT_NAME, AppConfig.PAUSE_SMALL_FONT_STYLE, AppConfig.PAUSE_SMALL_FONT_SIZE);


    public GamePanel(GameState state) {
        this.gameState = state;
        setPreferredSize(new Dimension(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT));
        setBackground(AppConfig.GAME_BACKGROUND_COLOR); // Use configured color
        setDoubleBuffered(true); // Improve rendering performance
        setFocusable(true);
        setRequestFocusEnabled(true);
        setFocusTraversalKeysEnabled(false); // Allow capturing keys like Tab
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        // Anti-aliasing can be configured too if desired
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Background
        drawBackground(g2d);

        // Draw Game Objects only if the game is considered running or game over
        if (gameState.isRunning() || gameState.isGameOver()) {
            drawGameObjects(g2d);
            drawUI(g2d); // Draw score, lives
        }

        // Draw Overlays (Pause, Game Over)
        drawOverlays(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_BACKGROUND_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        // Could add stars or other background elements here (potentially configurable?)
    }

    private void drawGameObjects(Graphics2D g2d) {
        // Using iterators or defensive copies is good practice, especially if
        // state could theoretically be modified concurrently (less likely with Swing Timer on EDT).
        try {
            // Draw Player
            if (gameState.getPlayer() != null) {
                 gameState.getPlayer().draw(g2d);
            }

            // Draw Bullets
            // Create copy to avoid ConcurrentModificationException
            List<Bullet> bulletsCopy = new ArrayList<>(gameState.getBullets());
            for (Bullet bullet : bulletsCopy) {
                bullet.draw(g2d);
            }

            // Draw Enemies
            List<Enemy> enemiesCopy = new ArrayList<>(gameState.getEnemies());
            for (Enemy enemy : enemiesCopy) {
                enemy.draw(g2d);
            }

            // Draw Power-ups
            List<PowerUp> powerUpsCopy = new ArrayList<>(gameState.getPowerUps());
            for (PowerUp powerUp : powerUpsCopy) {
                powerUp.draw(g2d);
            }
        } catch (Exception e) {
            // Catch potential exceptions during drawing iteration (e.g., null objects if state is inconsistent)
            System.err.println("Error during drawing game objects: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_UI_TEXT_COLOR);
        g2d.setFont(uiFont); // Use pre-created font

        // Positions could also be configured or calculated based on screen size
        int uiMarginX = 10;
        int uiMarginY = 20;
        int livesRightMargin = 100;

        g2d.drawString("Score: " + gameState.getScore(), uiMarginX, uiMarginY);
        g2d.drawString("Lives: " + gameState.getLives(), AppConfig.GAME_WIDTH - livesRightMargin, uiMarginY);
        // Display current user? Might need access to mainApp or pass username
        // String username = mainApp.getCurrentUsername(); // Requires access
        // if (username != null) g2d.drawString("Player: " + username, AppConfig.GAME_WIDTH / 2 - 50, uiMarginY);
    }

    private void drawOverlays(Graphics2D g2d) {
        int yOffsetLarge = -30; // Adjust vertical positioning of overlay text
        int yOffsetMedium = 20;
        int yOffsetSmall = 60;

        if (gameState.isGameOver()) {
            drawCenteredString(g2d, "GAME OVER", gameOverLargeFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetLarge);
            drawCenteredString(g2d, "Final Score: " + gameState.getScore(), gameOverMediumFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetMedium);
            drawCenteredString(g2d, "(Press R to Restart / ESC to Exit)", gameOverSmallFont, AppConfig.GAME_UI_TEXT_COLOR, yOffsetSmall);
        } else if (gameState.isPaused()) {
            // Semi-transparent overlay for pause
            g2d.setColor(AppConfig.PAUSE_OVERLAY_COLOR); // Use configured color (with alpha)
            g2d.fillRect(0, 0, getWidth(), getHeight());

            drawCenteredString(g2d, "PAUSED", pauseLargeFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetLarge);
            drawCenteredString(g2d, "(Press 'P' to resume)", pauseSmallFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetSmall);
        } else if (!gameState.isRunning() && !gameState.isGameOver()) {
             // Optional: Message before game starts (e.g., if transitioning)
             // drawCenteredString(g2d, "Get Ready!", someFont, someColor, 0);
        }
    }

    // Helper method for drawing centered text
    private void drawCenteredString(Graphics2D g2d, String text, Font font, Color color, int yOffset) {
        g2d.setFont(font);
        g2d.setColor(color);
        FontMetrics metrics = g2d.getFontMetrics(font);
        // Use configured width/height for centering
        int x = (AppConfig.GAME_WIDTH - metrics.stringWidth(text)) / 2;
        int y = (AppConfig.GAME_HEIGHT / 2) + metrics.getAscent() / 2 - metrics.getDescent() + yOffset; // Better vertical centering
        g2d.drawString(text, x, y);
    }
}

// --- Game Controller (Manages Game Loop, State Transitions, Interactions) ---
// Orchestrates the game flow, timer, and interactions between logic, state, and UI.
class GameController implements ActionListener {
    private GameState gameState;
    private GameLogic gameLogic;
    private GamePanel gamePanel;
    private Timer gameTimer; // Use javax.swing.Timer for GUI updates
    private DatabaseManager dbManager;
    private TerribleGame mainApp; // Reference to main application for global actions (exit, switch view)

    public GameController(GameState state, GameLogic logic, GamePanel panel, DatabaseManager dbManager, TerribleGame mainApp) {
        this.gameState = state;
        this.gameLogic = logic;
        this.gamePanel = panel;
        this.dbManager = dbManager;
        this.mainApp = mainApp;
        // Timer calls actionPerformed every configured tick milliseconds
        this.gameTimer = new Timer(AppConfig.GAME_TICK_MS, this);
    }

    public void startGame() {
        gameLogic.initializeNewGame(); // Sets up initial game state and objects
        gameState.setRunning(true);    // Mark game as running
        gameState.setPaused(false);    // Ensure not paused
        gameState.setGameOver(false);  // Ensure not game over
        gameTimer.start();
        System.out.println("Game Started. Timer running (" + AppConfig.GAME_TICK_MS + "ms interval).");
    }

    // Stops the game loop but doesn't necessarily mean 'Game Over'
    public void stopGameLoop() {
        if (gameTimer.isRunning()) {
            gameState.setRunning(false); // Mark as not actively running logic
            gameTimer.stop();
            System.out.println("Game Loop Stopped.");
        }
    }

     public void togglePause() {
        if (!gameState.isRunning() || gameState.isGameOver()) return; // Can't pause if not running or already over

        gameState.setPaused(!gameState.isPaused()); // Toggle pause state

        if (gameState.isPaused()) {
            gameTimer.stop(); // Stop updates
            System.out.println("Game Paused");
        } else {
            gameTimer.start(); // Resume timer and updates
            System.out.println("Game Resumed");
        }
        gamePanel.repaint(); // Repaint immediately to show/hide pause overlay
    }

    // Called when the game reaches a game over condition
    private void handleGameOver() {
        stopGameLoop(); // Stop the timer and updates
        // gameState.setGameOver(true); // This should already be set by GameLogic/GameState
        System.out.println("Game Over! Final Score: " + gameState.getScore());

        // Save score if a user is logged in
        String username = mainApp.getCurrentUsername();
        if (username != null) {
            dbManager.saveScore(username, gameState.getScore());
        } else {
            System.out.println("Score not saved - no user logged in.");
        }
        gamePanel.repaint(); // Ensure game over screen is drawn
    }

    // Handles request to exit the application
    public void exitGame() {
        System.out.println("Exit requested via ESC or window close.");
        stopGameLoop(); // Ensure game loop is stopped
        mainApp.shutdown(); // Ask main application to close gracefully
    }

    // Handles request to restart the game (typically back to login screen)
    public void requestRestart() {
        System.out.println("Restart requested via R key.");
        stopGameLoop();
        // gameState.reset(); // Reset happens in initializeNewGame or when switching view
        mainApp.switchToLoginScreen(); // Tell main app to go back to login
    }

    // Called by the Swing Timer
    @Override
    public void actionPerformed(ActionEvent e) {
        // Check if the game should be updating
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
            gameLogic.update(); // Update game state through logic

            // Check if the update resulted in game over
            if (gameState.isGameOver()) {
                handleGameOver(); // Handle game over logic (stop timer, save score)
                gamePanel.repaint(); // Ensure Game Over screen is drawn *after* handling
            } else {
               gamePanel.repaint(); // Render the updated state normally
            }
        }
        // No need for explicit checks for paused/game over here if the timer is managed correctly
        // by togglePause() and handleGameOver(). Repainting happens naturally or is forced when needed.
    }
}


// --- Main Application Class (JFrame) ---
// Sets up the window, manages panels (CardLayout), and orchestrates components.
public class TerribleGame extends JFrame {

    private GameState gameState;
    private GameLogic gameLogic;
    private GamePanel gamePanel;
    private LoginScreen loginScreen;
    private GameController gameController;
    private InputHandler inputHandler;
    private DatabaseManager dbManager;

    private CardLayout cardLayout;
    private JPanel mainPanel;

    private String currentUsername = null; // Holds the logged-in username

    // Using configured IDs
    // private static final String LOGIN_PANEL_ID = AppConfig.LOGIN_PANEL_ID;
    // private static final String GAME_PANEL_ID = AppConfig.GAME_PANEL_ID;
    // Note: Direct static access might happen before AppConfig fully initializes if not careful.
    // It's safer to access AppConfig fields after its static initializer has run.

    public TerribleGame() {
        super(AppConfig.APP_TITLE); // Use configured title

        // Ensure AppConfig is loaded (happens automatically via static block)
        System.out.println("Initializing game with Width=" + AppConfig.GAME_WIDTH + ", Height=" + AppConfig.GAME_HEIGHT);

        // 1. Initialize Core Components (non-UI first)
        dbManager = new DatabaseManager(); // Uses configured DB settings internally
        gameState = new GameState();       // Uses configured initial lives
        gameLogic = new GameLogic(gameState); // Uses various configured game parameters

        // 2. Initialize UI Panels
        loginScreen = new LoginScreen(this, dbManager); // Uses configured UI settings
        gamePanel = new GamePanel(gameState);           // Uses configured UI/game settings

        // 3. Initialize Controller and Input Handler (need UI refs)
        gameController = new GameController(gameState, gameLogic, gamePanel, dbManager, this); // Uses configured tick rate
        inputHandler = new InputHandler(gameState, gameController); // Listens for standard keys

        // 4. Setup JFrame and CardLayout
        setupWindow();
        setupCardLayout();

        // 5. Focus Management
        setFocusable(false); // Frame should not capture focus intended for panels
        setFocusTraversalKeysEnabled(false);

        // 6. Finalize Window Setup
        setLocationRelativeTo(null); // Center window
        setVisible(true);

        // 7. Show Initial Screen (Login)
        switchToLoginScreen(); // Use method to ensure consistency
    }

    private void setupWindow() {
        // Use configured dimensions
        setSize(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle close manually via controller->shutdown
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                gameController.exitGame(); // Trigger clean shutdown via controller
            }
        });
        setResizable(false); // Resizing not typically handled well in simple games
    }

    private void setupCardLayout() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add panels using configured IDs
        mainPanel.add(loginScreen, AppConfig.LOGIN_PANEL_ID);
        mainPanel.add(gamePanel, AppConfig.GAME_PANEL_ID);

        // Add key listener ONLY to the component that needs it (GamePanel)
        gamePanel.addKeyListener(inputHandler);

        add(mainPanel, BorderLayout.CENTER);
    }

    // --- View Switching Methods ---

    public void switchToGameScreen() {
        cardLayout.show(mainPanel, AppConfig.GAME_PANEL_ID);
        System.out.println("Switching to Game Screen...");

        // Request focus AFTER the panel is shown
        SwingUtilities.invokeLater(() -> {
            boolean focused = gamePanel.requestFocusInWindow();
             if (!focused) {
                System.err.println("Warning: GamePanel failed to gain focus.");
                // Fallback: Try requesting focus directly on the frame content pane sometimes works
                // getContentPane().requestFocusInWindow();
            } else {
                 System.out.println("GamePanel focus requested successfully.");
             }
             // Start the game logic AFTER focus is likely set
             gameController.startGame(); // Start the game logic and timer
        });
    }

     public void switchToLoginScreen() {
         gameController.stopGameLoop(); // Make sure game loop is stopped
         this.currentUsername = null; // Log out the user conceptually
         loginScreen.clearForm(); // Clear username/password fields
         loginScreen.refreshHighScores(); // Update high scores display
         cardLayout.show(mainPanel, AppConfig.LOGIN_PANEL_ID);
         System.out.println("Switching to Login Screen...");

         // Request focus AFTER the panel is shown
         SwingUtilities.invokeLater(() -> {
             boolean focused = loginScreen.requestFocusInWindow();
             if (!focused) {
                 System.err.println("Warning: LoginScreen failed to gain focus.");
             } else {
                 System.out.println("LoginScreen focus requested successfully.");
             }
         });
     }

    // --- User Management Callback ---

    // Called by LoginScreen upon successful login
    public void userLoggedIn(String username) {
        this.currentUsername = username;
        System.out.println("User '" + username + "' logged in.");
        switchToGameScreen(); // Proceed to the game
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    // --- Application Lifecycle ---

    // Graceful shutdown procedure triggered by controller or window closing
    public void shutdown() {
        System.out.println("Shutting down application...");
        // Controller already stops the game loop
        // gameController.stopGameLoop(); // Ensure game loop is off (redundant if called from exitGame)
        dbManager.closeConnection(); // Close database connection
        System.out.println("Shutdown complete. Exiting JVM.");
        dispose(); // Close the JFrame window
        System.exit(0); // Terminate the application
    }

    // --- Main Entry Point ---
    public static void main(String[] args) {
        // AppConfig loads automatically here when the class is first accessed
        // which happens when TerribleGame constructor runs or AppConfig constants are used.

        // Ensure Swing components are created on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new TerribleGame(); // Create and show the main game window

            // Optional: Add JVM shutdown hook for cleanup on unexpected termination
            // Note: Actions within shutdown hooks have limitations
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("JVM Shutdown Hook triggered.");
                // Avoid complex operations here. The windowClosing listener is preferred.
                // Potentially force DB close if needed, but DatabaseManager instance is gone.
            }));
        });
    }
}
