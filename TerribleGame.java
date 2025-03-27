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

// --- Configuration Loading ---
// Remains largely unchanged as it deals with loading immutable configuration values,
// which is generally acceptable as static finals.
class AppConfig {
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
    public static final String APP_TITLE = loadStringEnv("APP_TITLE", "The Properly Encapsulated Space Game");
    public static final int GAME_WIDTH = loadIntEnv("GAME_WIDTH", 800);
    public static final int GAME_HEIGHT = loadIntEnv("GAME_HEIGHT", 600);
    public static final int GAME_TICK_MS = loadIntEnv("GAME_TICK_MS", 16);
    public static final int BOTTOM_UI_BUFFER = loadIntEnv("BOTTOM_UI_BUFFER", 30);

    // --- Database ---
    public static final String DATABASE_URL = loadStringEnv("DATABASE_URL", "jdbc:sqlite:game_data.db"); // Renamed DB file
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


// --- Core Data Models (Largely unchanged, already well-encapsulated) ---

/**
 * Abstract base class for all objects appearing in the game world.
 */
abstract class GameObject {
    // Protected allows subclasses direct access, could be private with getters/setters
    protected int x, y, width, height;
    protected Rectangle bounds; // Using Rectangle for bounds checking

    public GameObject(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new Rectangle(x, y, width, height);
    }

    // Public getters for read-only access from outside
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // Update bounds based on current x, y - should be called after position changes
    protected void updateBounds() {
        bounds.setLocation(x, y);
    }

    // Get a copy or the direct reference for collision detection
    public Rectangle getBounds() {
        // Return a copy if you want to prevent external modification of the internal bounds object
        // return new Rectangle(bounds);
        return bounds; // Return direct reference for performance, assuming callers are trusted
    }

    // Abstract methods for behaviour
    public abstract void update(); // Update object state (e.g., position)
    public abstract void draw(Graphics2D g2d); // Draw the object

    // Utility method for checking if object is off-screen
    public boolean isOutOfBounds(int screenHeight) {
        return y > screenHeight || y + height < 0;
    }

    // Utility method for collision detection
    public boolean intersects(GameObject other) {
        return this.bounds.intersects(other.getBounds());
    }
}

/**
 * Represents the player's spaceship.
 */
class Player extends GameObject {
    private int speed;
    // Internal state for movement control
    private boolean movingLeft, movingRight, movingUp, movingDown;
    private boolean wantsToShoot = false; // State representing player input intention

    public Player(int startX, int startY) {
        super(startX, startY, AppConfig.PLAYER_WIDTH, AppConfig.PLAYER_HEIGHT);
        this.speed = AppConfig.PLAYER_SPEED;
        // Initialize movement flags to false
        this.movingLeft = false;
        this.movingRight = false;
        this.movingUp = false;
        this.movingDown = false;
    }

    // Public setters to change movement state (called by InputHandler)
    public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
    public void setMovingUp(boolean movingUp) { this.movingUp = movingUp; }
    public void setMovingDown(boolean movingDown) { this.movingDown = movingDown; }
    public void setWantsToShoot(boolean wantsToShoot) { this.wantsToShoot = wantsToShoot; }
    public boolean isWantsToShoot() { return wantsToShoot; } // Getter for checking shoot intention

    @Override
    public void update() {
        int dx = 0;
        int dy = 0;

        if (movingLeft) dx -= speed;
        if (movingRight) dx += speed;
        if (movingUp) dy -= speed;
        if (movingDown) dy += speed;

        // Update position
        x += dx;
        y += dy;

        // Apply boundary checks using configuration
        x = Math.max(0, x);
        x = Math.min(AppConfig.GAME_WIDTH - width, x);
        y = Math.max(0, y);
        y = Math.min(AppConfig.GAME_HEIGHT - height - AppConfig.BOTTOM_UI_BUFFER, y);

        updateBounds(); // Update collision bounds after position change
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.PLAYER_COLOR);
        g2d.fillRect(x, y, width, height);
    }

    // Player-specific action: create a bullet at the correct position
    public Bullet shoot() {
         this.wantsToShoot = false; // Consume the trigger/intention
         int bulletX = this.x + this.width / 2 - AppConfig.BULLET_WIDTH / 2;
         int bulletY = this.y - AppConfig.BULLET_HEIGHT; // Position bullet above player
         return new Bullet(bulletX, bulletY);
    }

    // Method to reset player's position and state (used by GameState.reset)
    public void resetPosition(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.movingLeft = false;
        this.movingRight = false;
        this.movingUp = false;
        this.movingDown = false;
        this.wantsToShoot = false;
        updateBounds();
    }
}

/**
 * Represents a bullet fired by the player.
 */
class Bullet extends GameObject {
    private int speed;

    public Bullet(int x, int y) {
        super(x, y, AppConfig.BULLET_WIDTH, AppConfig.BULLET_HEIGHT);
        this.speed = AppConfig.BULLET_SPEED;
    }

    @Override
    public void update() {
        y -= speed; // Bullets move upwards
        updateBounds();
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.BULLET_COLOR);
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
        y += speed; // Enemies move downwards
        updateBounds();
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.ENEMY_COLOR);
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
        super(x, y, AppConfig.POWERUP_WIDTH, AppConfig.POWERUP_HEIGHT);
        this.speed = speed;
    }

    @Override
    public void update() {
        y += speed; // Power-ups move downwards
        updateBounds();
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.POWERUP_COLOR);
        g2d.fillOval(x, y, width, height); // Draw as an oval
    }

    /**
     * Applies the effect of this power-up.
     * @param gameState The current game state to modify (e.g., increase score).
     */
    public void applyEffect(GameState gameState) {
        // Modifies the GameState directly through its public methods
        gameState.increaseScore(AppConfig.POWERUP_SCORE_VALUE);
        // Example effects (add methods to GameState if needed):
        // gameState.increaseLives(1);
        // gameState.getPlayer().increaseFireRate(); // Might modify player state directly or via GameState
    }
}

// --- Database Management (Instance-based, remains unchanged) ---
// Encapsulates database operations.
class DatabaseManager {
    private Connection databaseConnection = null;
    private final String dbUrl;
    private final String jdbcDriver;

    public DatabaseManager() {
        this.dbUrl = AppConfig.DATABASE_URL;
        this.jdbcDriver = AppConfig.JDBC_DRIVER;
        getConnection();
        initializeDatabaseTables();
    }

    private Connection getConnection() {
        if (databaseConnection == null) {
            try {
                Class.forName(jdbcDriver);
                databaseConnection = DriverManager.getConnection(dbUrl);
                System.out.println("Database connection established (" + dbUrl + ").");
            } catch (ClassNotFoundException e) {
                handleError("JDBC Driver not found! Check classpath: " + jdbcDriver, e);
                System.exit(1);
            } catch (SQLException e) {
                handleError("Database connection failed (" + dbUrl + ")", e);
            }
        } else {
            try {
                if (databaseConnection.isClosed() || !databaseConnection.isValid(2)) {
                    System.out.println("Database connection was closed or invalid, reopening...");
                    databaseConnection = DriverManager.getConnection(dbUrl);
                    System.out.println("Database connection re-established.");
                }
            } catch (SQLException e) {
                handleError("Failed to check/reopen database connection", e);
                databaseConnection = null;
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
                                    " password TEXT NOT NULL" + // WARNING: Plain text password
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
                return false;
            }
            rs.close(); // Close even if no results
        } catch (SQLException e) {
            handleError("Error checking username during registration", e);
            return false;
        }

        try (PreparedStatement insertStmt = conn.prepareStatement(insertUserSql)) {
            insertStmt.setString(1, username);
            insertStmt.setString(2, password); // Store plain text (BAD PRACTICE!)
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
                // Plain text comparison (BAD PRACTICE!)
                return storedPassword.equals(password);
            } else {
                rs.close(); // Close even if no results
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

    public List<String> getHighScores() {
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
                scores.add(String.format("%d. %-15s : %d", rank++, username, scoreValue));
            }
            rs.close(); // Close result set
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

    private boolean isInputValid(String username, String password) {
        return username != null && !username.trim().isEmpty() && password != null && !password.isEmpty();
    }

    private void handleError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        // e.printStackTrace(); // Uncomment for debugging
    }
}

// --- NEW: Game State Encapsulation ---
/**
 * Encapsulates the mutable state of the game world and player status.
 * Provides controlled access and modification methods.
 */
class GameState {
    // Private fields to hold the state
    private Player player;
    private final List<Enemy> enemies; // Use final for the list reference itself
    private final List<PowerUp> powerUps;
    private final List<Bullet> bullets;
    private int score;
    private int lives;
    private boolean isGameOver;
    private boolean isPaused;
    private boolean isRunning; // Indicates if the game logic loop should run

    public GameState() {
        // Initialize player at a temporary spot, reset() will place correctly
        this.player = new Player(0, 0);
        // Initialize lists (use thread-safe lists like CopyOnWriteArrayList if concurrency expected)
        this.enemies = new ArrayList<>();
        this.powerUps = new ArrayList<>();
        this.bullets = new ArrayList<>();
        // Initialize status variables
        this.score = 0;
        this.lives = AppConfig.INITIAL_LIVES;
        this.isGameOver = false;
        this.isPaused = false;
        this.isRunning = false; // Game doesn't start running automatically
    }

    /**
     * Resets the game state to its initial configuration for a new game.
     */
    public void reset() {
        // Reset player position using config values
        player.resetPosition(
            AppConfig.GAME_WIDTH / 2 - AppConfig.PLAYER_WIDTH / 2,
            AppConfig.GAME_HEIGHT - AppConfig.BOTTOM_UI_BUFFER - AppConfig.PLAYER_HEIGHT - 10 // Start slightly higher
        );

        // Clear all dynamic game objects
        enemies.clear();
        powerUps.clear();
        bullets.clear();

        // Reset score, lives, and status flags
        score = 0;
        lives = AppConfig.INITIAL_LIVES;
        isGameOver = false;
        isPaused = false;
        isRunning = false; // Reset running state, controller will set it true when starting
    }

    // --- Getters for Read Access ---

    public Player getPlayer() { return player; }

    // Consider returning unmodifiable lists or copies if strict immutability is needed outside
    // public List<Enemy> getEnemies() { return Collections.unmodifiableList(enemies); }
    public List<Enemy> getEnemies() { return enemies; } // Direct access for now
    public List<PowerUp> getPowerUps() { return powerUps; }
    public List<Bullet> getBullets() { return bullets; }

    public int getScore() { return score; }
    public int getLives() { return lives; }
    public boolean isGameOver() { return isGameOver; }
    public boolean isPaused() { return isPaused; }
    public boolean isRunning() { return isRunning; }

    // --- Mutators for Controlled State Changes ---

    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
        if (gameOver) {
            this.isRunning = false; // Game logic stops if game over
        }
    }

    public void setPaused(boolean paused) {
         // Only allow pausing if the game is running and not already over
        if (this.isRunning && !this.isGameOver) {
            this.isPaused = paused;
        }
    }

    public void setRunning(boolean running) {
        // Can only set running if not game over
        if (!this.isGameOver) {
            this.isRunning = running;
            if (!running) {
                this.isPaused = false; // If stopped, cannot be paused
            }
        } else {
            this.isRunning = false; // Cannot run if game over
        }
    }

    /** Decreases lives by one and sets game over if lives reach zero. */
    public void decreaseLives() {
        if (lives > 0) {
            lives--;
            System.out.println("Life lost. Lives remaining: " + lives); // Debugging
        }
        if (lives <= 0 && !isGameOver) { // Prevent multiple game over triggers
            setGameOver(true); // Use the setter to ensure isRunning is also handled
            System.out.println("Lives reached zero. Game Over triggered.");
        }
    }

    /** Increases the score by the given amount. */
    public void increaseScore(int amount) {
        if (amount > 0 && isRunning) { // Only score while running
            score += amount;
        }
    }

    // --- Methods to Manage Game Object Collections ---

    public void addBullet(Bullet bullet) {
        if (bullet != null) {
            bullets.add(bullet);
        }
    }

    public void addEnemy(Enemy enemy) {
        if (enemy != null) {
            enemies.add(enemy);
        }
    }

    public void addPowerUp(PowerUp powerUp) {
        if (powerUp != null) {
            powerUps.add(powerUp);
        }
    }

    // Use Iterators or provide methods for safe removal
    // These methods allow bulk removal, often done after collision checks

    public void removeBullets(List<Bullet> bulletsToRemove) {
        if (bulletsToRemove != null) {
            bullets.removeAll(bulletsToRemove);
        }
    }

    public void removeEnemies(List<Enemy> enemiesToRemove) {
        if (enemiesToRemove != null) {
            enemies.removeAll(enemiesToRemove);
        }
    }

    public void removePowerUps(List<PowerUp> powerUpsToRemove) {
        if (powerUpsToRemove != null) {
            powerUps.removeAll(powerUpsToRemove);
        }
    }
}


// --- Game Logic (Operates on GameState and GameObjects) ---
// Now depends on the GameState object passed to it.
class GameLogic {
    private final Random randomGenerator = new Random();
    private final GameState gameState; // Holds the state this logic operates on

    public GameLogic(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("GameState cannot be null for GameLogic");
        }
        this.gameState = state;
    }

    /**
     * Initializes the game state for a new game session.
     * Calls GameState.reset() and potentially adds initial objects.
     */
    public void initializeNewGame() {
        gameState.reset(); // Reset all state variables, player position, lists, etc.

        // Optional: Spawn some initial objects (can be configured)
        // Example: Spawn a few enemies to start
        // for (int i = 0; i < 3; i++) {
        //     spawnEnemy();
        // }

        // Mark the game as ready to run (controller will actually start the timer)
        // gameState.setRunning(true); // Controller should manage this transition
        System.out.println("Game logic initialized for a new game.");
    }

    /**
     * Main update method called each game tick by the GameController.
     * Updates all game objects, handles collisions, spawning, and checks game over conditions.
     */
    public void update() {
        // Logic should only run if the game state allows it
        if (!gameState.isRunning() || gameState.isPaused() || gameState.isGameOver()) {
            return; // Do nothing if not in active play state
        }

        // 1. Update player
        gameState.getPlayer().update();
        handleShooting(); // Check if player wants to shoot and add bullet to state

        // 2. Update bullets and remove Out-Of-Bounds (OOB)
        updateGameObjects(gameState.getBullets());

        // 3. Update enemies and remove OOB
        updateGameObjects(gameState.getEnemies());

        // 4. Update powerups and remove OOB
        updateGameObjects(gameState.getPowerUps());

        // 5. Handle collisions between objects
        handleCollisions();

        // 6. Handle spawning new objects based on chance/limits
        handleSpawning();

        // 7. Check game over condition (redundant if decreaseLives handles it, but safe)
        // Note: decreaseLives now sets isGameOver directly in GameState
        // if (gameState.getLives() <= 0 && !gameState.isGameOver()) {
        //     gameState.setGameOver(true);
        // }
    }

    /**
     * Generic method to update a list of GameObjects and remove those out of bounds.
     * Uses an Iterator for safe removal during iteration.
     */
    private <T extends GameObject> void updateGameObjects(List<T> list) {
        Iterator<T> iterator = list.iterator();
        while (iterator.hasNext()) {
            T obj = iterator.next();
            obj.update();
            // Use configured height for bounds check
            if (obj.isOutOfBounds(AppConfig.GAME_HEIGHT)) {
                iterator.remove(); // Safely remove using iterator
            }
        }
    }

    /** Checks if the player intends to shoot and adds a bullet to the GameState. */
    private void handleShooting() {
        Player player = gameState.getPlayer();
        if (player.isWantsToShoot()) {
            // Player object creates the bullet, GameState manages the list
            gameState.addBullet(player.shoot());
        }
    }

    /** Detects and resolves collisions between different game objects. */
    private void handleCollisions() {
        Player player = gameState.getPlayer();
        // Create lists to collect objects that need removal after checking all collisions
        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<PowerUp> powerUpsToRemove = new ArrayList<>();

        // --- 1. Player vs Enemy collisions ---
        for (Enemy enemy : gameState.getEnemies()) {
            // Avoid re-colliding with an enemy already marked for removal in this frame
            if (!enemiesToRemove.contains(enemy) && player.intersects(enemy)) {
                gameState.decreaseLives(); // Let GameState handle life reduction and game over check
                enemiesToRemove.add(enemy);
                // Optimization: If game over is triggered, no need to check further collisions this frame
                if (gameState.isGameOver()) {
                    // Remove collected objects immediately before returning
                    gameState.removeEnemies(enemiesToRemove);
                    gameState.removeBullets(bulletsToRemove); // May be empty
                    gameState.removePowerUps(powerUpsToRemove); // May be empty
                    return;
                }
            }
        }

        // --- 2. Bullet vs Enemy collisions ---
        for (Bullet bullet : gameState.getBullets()) {
            // Skip bullets already marked for removal (e.g., hit multiple enemies in one frame if allowed)
             if (bulletsToRemove.contains(bullet)) continue;

            for (Enemy enemy : gameState.getEnemies()) {
                 // Skip enemies already marked for removal (e.g., hit by player or another bullet)
                 if (enemiesToRemove.contains(enemy)) continue;

                if (bullet.intersects(enemy)) {
                    bulletsToRemove.add(bullet);
                    enemiesToRemove.add(enemy);
                    gameState.increaseScore(AppConfig.ENEMY_SCORE_VALUE); // Use GameState method
                    break; // Crucial: prevent one bullet hitting multiple enemies
                }
            }
        }

        // --- 3. Player vs PowerUp collisions ---
        for (PowerUp powerUp : gameState.getPowerUps()) {
             // Avoid re-collecting a powerup already marked for removal
             if (!powerUpsToRemove.contains(powerUp) && player.intersects(powerUp)) {
                powerUp.applyEffect(gameState); // PowerUp modifies GameState via its methods
                powerUpsToRemove.add(powerUp);
            }
        }

        // --- Remove all collected objects from the GameState ---
        // Perform removal outside the iteration loops
        gameState.removeEnemies(enemiesToRemove);
        gameState.removeBullets(bulletsToRemove);
        gameState.removePowerUps(powerUpsToRemove);
    }

    /** Handles the spawning of new enemies and power-ups based on configured chances and limits. */
    private void handleSpawning() {
        // Spawn enemies
        if (gameState.getEnemies().size() < AppConfig.MAX_ENEMIES &&
            randomGenerator.nextDouble() < AppConfig.ENEMY_SPAWN_CHANCE) {
            spawnEnemy();
        }
        // Spawn power-ups
        if (gameState.getPowerUps().size() < AppConfig.MAX_POWERUPS &&
            randomGenerator.nextDouble() < AppConfig.POWERUP_SPAWN_CHANCE) {
            spawnPowerUp();
        }
    }

    /** Creates a new enemy with random properties based on config and adds it to the GameState. */
    private void spawnEnemy() {
        int size = AppConfig.ENEMY_BASE_SIZE + (AppConfig.ENEMY_SIZE_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SIZE_VARIATION) : 0);
        int enemyX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - size);
        int enemyY = -size; // Start just above screen
        int enemySpeed = AppConfig.ENEMY_BASE_SPEED + (AppConfig.ENEMY_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SPEED_VARIATION) : 0);

        Enemy newEnemy = new Enemy(enemyX, enemyY, size, size, enemySpeed);
        gameState.addEnemy(newEnemy); // Add to state
    }

    /** Creates a new power-up with random properties based on config and adds it to the GameState. */
    private void spawnPowerUp() {
        int powerUpX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - AppConfig.POWERUP_WIDTH);
        int powerUpY = -AppConfig.POWERUP_HEIGHT; // Start just above screen
        int powerUpSpeed = AppConfig.POWERUP_BASE_SPEED + (AppConfig.POWERUP_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.POWERUP_SPEED_VARIATION) : 0);

        PowerUp newPowerUp = new PowerUp(powerUpX, powerUpY, powerUpSpeed);
        gameState.addPowerUp(newPowerUp); // Add to state
    }
}


// --- Input Handling (Interacts with Player object within GameState and GameController) ---
// Now receives GameState to access the player and check game status.
class InputHandler extends KeyAdapter {
    private final GameState gameState; // Reference to access player and game status
    private final GameController gameController; // Reference for global actions (pause, exit, restart)

    public InputHandler(GameState state, GameController controller) {
        if (state == null || controller == null) {
            throw new IllegalArgumentException("GameState and GameController cannot be null for InputHandler");
        }
        this.gameState = state;
        this.gameController = controller;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // --- Global Keys (handled regardless of game state, but some actions depend on it) ---
        switch (keyCode) {
            case KeyEvent.VK_P: // Pause Key
                // Controller handles the logic of whether pausing is allowed
                gameController.togglePause();
                return; // Consume event
            case KeyEvent.VK_ESCAPE: // Exit Key
                gameController.exitGame();
                return; // Consume event
            case KeyEvent.VK_R: // Restart Key
                // Only allow restart if game is actually over
                if (gameState.isGameOver()) {
                    System.out.println("Restart key (R) pressed - Requesting restart.");
                    gameController.requestRestart();
                }
                return; // Consume event
        }

        // --- Game Input Keys (only process if game is running and not paused/over) ---
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
             Player player = gameState.getPlayer(); // Get player from state
             if (player == null) return; // Safety check

            switch (keyCode) {
                // Movement keys
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
                // Action keys
                case KeyEvent.VK_SPACE:
                case KeyEvent.VK_CONTROL: // Allow Ctrl as alternative shoot key
                    player.setWantsToShoot(true); // Signal intent, GameLogic will handle it
                    break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        Player player = gameState.getPlayer(); // Get player from state
        if (player == null) return; // Safety check

        // Reset movement flags on key release
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
            // No action needed on release for VK_SPACE/VK_CONTROL currently,
            // as shooting is triggered on press and consumed by GameLogic.
            // If charging was implemented, release would be handled here.
        }
    }
}


// --- UI Panels ---

/**
 * Login Screen Panel - Handles user login/registration UI.
 * Interacts with DatabaseManager and the main application frame. (Largely unchanged logic)
 */
class LoginScreen extends JPanel implements ActionListener {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JTextArea highScoreTextArea;
    private final TerribleGame mainApp; // Reference to the main application for callbacks
    private final DatabaseManager dbManager; // Instance of DatabaseManager

    public LoginScreen(TerribleGame mainApp, DatabaseManager dbManager) {
        if (mainApp == null || dbManager == null) {
            throw new IllegalArgumentException("MainApp and DatabaseManager cannot be null for LoginScreen");
        }
        this.mainApp = mainApp;
        this.dbManager = dbManager;
        setupUI();
        // Don't refresh high scores here automatically, let mainApp call it when switching TO login
    }

    private void setupUI() {
        setLayout(null); // Using absolute positioning
        setBackground(AppConfig.LOGIN_BACKGROUND_COLOR);
        setPreferredSize(new Dimension(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT));

        int centerX = AppConfig.GAME_WIDTH / 2;
        int fieldWidth = 160;
        int labelWidth = 80;
        int startY = 150;
        int fieldSpacing = 40;
        int buttonWidth = 120;
        int buttonSpacing = 20;

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

        // Buttons
        int totalButtonWidth = buttonWidth * 2 + buttonSpacing;
        loginButton = createButton("Login", centerX - totalButtonWidth / 2, startY + 2 * fieldSpacing, buttonWidth, 30);
        registerButton = createButton("Register", centerX - totalButtonWidth / 2 + buttonWidth + buttonSpacing, startY + 2 * fieldSpacing, buttonWidth, 30);
        add(loginButton);
        add(registerButton);

        // Status Label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setBounds(centerX - (fieldWidth + labelWidth) / 2, startY + 3 * fieldSpacing, fieldWidth + labelWidth, 25);
        statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
        add(statusLabel);

        // High Scores Area
        int hsX = 50;
        int hsY = startY + 4 * fieldSpacing + 20;
        int hsWidth = AppConfig.GAME_WIDTH - 2 * hsX;
        int hsHeight = AppConfig.GAME_HEIGHT - hsY - 30;

        highScoreTextArea = new JTextArea();
        highScoreTextArea.setEditable(false);
        highScoreTextArea.setForeground(AppConfig.LOGIN_HIGHSCORE_FG_COLOR);
        highScoreTextArea.setBackground(AppConfig.LOGIN_HIGHSCORE_BG_COLOR);
        highScoreTextArea.setFont(new Font(AppConfig.LOGIN_HIGHSCORE_FONT_NAME,
                                           AppConfig.LOGIN_HIGHSCORE_FONT_STYLE,
                                           AppConfig.LOGIN_HIGHSCORE_FONT_SIZE));
        JScrollPane scrollPane = new JScrollPane(highScoreTextArea);
        scrollPane.setBounds(hsX, hsY, hsWidth, hsHeight);
        add(scrollPane);
    }

    private JLabel createLabel(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, 80, 25);
        label.setForeground(AppConfig.LOGIN_LABEL_COLOR);
        return label;
    }

    private JButton createButton(String text, int x, int y, int w, int h) {
        JButton button = new JButton(text);
        button.setBounds(x, y, w, h);
        button.addActionListener(this);
        // Could configure button appearance further using AppConfig
        return button;
    }

     /** Reloads and displays high scores from the database. */
     public void refreshHighScores() {
        List<String> scores = dbManager.getHighScores();
        highScoreTextArea.setText("--- High Scores (Top " + AppConfig.HIGH_SCORE_LIMIT + ") ---\n");
        if (scores != null) {
            for (String scoreLine : scores) {
                highScoreTextArea.append(scoreLine + "\n");
            }
        }
        highScoreTextArea.setCaretPosition(0); // Scroll to top
     }

     /** Clears input fields and the status message. */
     public void clearForm() {
         usernameField.setText("");
         passwordField.setText("");
         statusLabel.setText("");
         statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR); // Reset color
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText().trim();
        // Securely handle password (char array is better than String)
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);
        // Clear the password char array immediately after use
        java.util.Arrays.fill(passwordChars, ' ');

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        if (e.getSource() == loginButton) {
            if (dbManager.validateUser(username, password)) {
                statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                statusLabel.setText("Login Successful!");
                // Use Swing Timer for a slight delay before switching panel
                Timer switchTimer = new Timer(500, ae -> mainApp.userLoggedIn(username));
                switchTimer.setRepeats(false);
                switchTimer.start();
            } else {
                statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                statusLabel.setText("Login failed. Check credentials.");
                passwordField.setText(""); // Clear password field on failure
            }
        } else if (e.getSource() == registerButton) {
            if (dbManager.registerUser(username, password)) {
                statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                statusLabel.setText("Registration successful! Please log in.");
                usernameField.setText(""); // Clear fields after successful registration
                passwordField.setText("");
                // Optionally refresh high scores immediately, though login screen refresh handles it too
                // refreshHighScores();
            } else {
                statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                statusLabel.setText("Registration failed (username might exist).");
                passwordField.setText(""); // Clear password field on failure
            }
        }
    }
}

/**
 * Game Screen Panel - Renders the game state provided by GameState.
 * Reads data via GameState getters and uses Graphics2D for drawing.
 */
class GamePanel extends JPanel {
    private final GameState gameState; // Reference to the state to render

    // Pre-create Font objects based on config for efficiency
    private final Font uiFont = new Font(AppConfig.GAME_UI_FONT_NAME, AppConfig.GAME_UI_FONT_STYLE, AppConfig.GAME_UI_FONT_SIZE);
    private final Font gameOverLargeFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_LARGE_FONT_STYLE, AppConfig.GAME_OVER_LARGE_FONT_SIZE);
    private final Font gameOverMediumFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_MEDIUM_FONT_STYLE, AppConfig.GAME_OVER_MEDIUM_FONT_SIZE);
    private final Font gameOverSmallFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_SMALL_FONT_STYLE, AppConfig.GAME_OVER_SMALL_FONT_SIZE);
    private final Font pauseLargeFont = new Font(AppConfig.PAUSE_FONT_NAME, AppConfig.PAUSE_LARGE_FONT_STYLE, AppConfig.PAUSE_LARGE_FONT_SIZE);
    private final Font pauseSmallFont = new Font(AppConfig.PAUSE_FONT_NAME, AppConfig.PAUSE_SMALL_FONT_STYLE, AppConfig.PAUSE_SMALL_FONT_SIZE);


    public GamePanel(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("GameState cannot be null for GamePanel");
        }
        this.gameState = state;
        setPreferredSize(new Dimension(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT));
        setBackground(AppConfig.GAME_BACKGROUND_COLOR);
        setDoubleBuffered(true); // Enable double buffering for smoother rendering
        setFocusable(true);      // Panel needs focus to receive key events
        setRequestFocusEnabled(true);
        // Prevent default focus traversal keys (like Tab) from interfering
        setFocusTraversalKeysEnabled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Clears the panel
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for potentially smoother graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Rendering quality hint (can affect performance)
        // g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw Background elements first
        drawBackground(g2d);

        // Draw Game Objects (Player, Enemies, Bullets, PowerUps)
        // Only draw if game is considered active (running or game over state)
        // Or potentially draw static elements even if not running (e.g., a title screen overlay)
        // For simplicity, we draw if running OR game over to show final state.
        // A more complex state machine could control this better.
        if (gameState.isRunning() || gameState.isGameOver()) {
            drawGameObjects(g2d);
        }

        // Draw UI elements (Score, Lives) - always drawn if game has started/ended
        // Could be conditional based on gameState.isRunning() || gameState.isGameOver()
        drawUI(g2d);

        // Draw Overlays (Pause, Game Over) - drawn on top
        drawOverlays(g2d);

        // Dispose graphics context if manually created (not needed here as it's provided by Swing)
        // g2d.dispose();

        // Toolkit.getDefaultToolkit().sync(); // Sometimes used to reduce tearing, often not needed
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_BACKGROUND_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        // TODO: Add more interesting background elements? (e.g., stars)
    }

    private void drawGameObjects(Graphics2D g2d) {
        // Use try-catch for safety during rendering, though synchronization issues
        // should ideally be handled elsewhere (e.g., using concurrent collections or EDT safety).
        try {
            // Draw Player (if exists and game not strictly 'before start')
            Player player = gameState.getPlayer();
            if (player != null) {
                 player.draw(g2d);
            }

            // Draw Bullets - Iterate over a copy to avoid ConcurrentModificationException
            // if the game logic modifies the list while rendering (less likely on EDT but safer)
            List<Bullet> bulletsToDraw = new ArrayList<>(gameState.getBullets());
            for (Bullet bullet : bulletsToDraw) {
                bullet.draw(g2d);
            }

            // Draw Enemies - Iterate over a copy
            List<Enemy> enemiesToDraw = new ArrayList<>(gameState.getEnemies());
            for (Enemy enemy : enemiesToDraw) {
                enemy.draw(g2d);
            }

            // Draw Power-ups - Iterate over a copy
            List<PowerUp> powerUpsToDraw = new ArrayList<>(gameState.getPowerUps());
            for (PowerUp powerUp : powerUpsToDraw) {
                powerUp.draw(g2d);
            }
        } catch (Exception e) {
            // Log error if rendering fails for some reason
            System.err.println("Error during rendering game objects: " + e.getMessage());
            e.printStackTrace(); // For detailed debugging
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_UI_TEXT_COLOR);
        g2d.setFont(uiFont);

        // Use configured positions or calculate dynamically
        int uiMarginX = 10;
        int uiMarginYTop = 20; // Position from top
        int livesRightMargin = 100; // Position from right edge

        // Draw Score and Lives using data from GameState
        g2d.drawString("Score: " + gameState.getScore(), uiMarginX, uiMarginYTop);
        g2d.drawString("Lives: " + gameState.getLives(), AppConfig.GAME_WIDTH - livesRightMargin, uiMarginYTop);

        // Optionally display current player username (would need access or be passed in)
        // String username = mainApp.getCurrentUsername(); // If GamePanel had access to mainApp
        // if (username != null) g2d.drawString("Player: " + username, AppConfig.GAME_WIDTH / 2 - 50, uiMarginYTop);
    }

    private void drawOverlays(Graphics2D g2d) {
        // Vertical offsets for centered text lines
        int yOffsetLarge = -40;
        int yOffsetMedium = 20;
        int yOffsetSmall = 60;

        // Draw Game Over overlay if applicable
        if (gameState.isGameOver()) {
            // Optional semi-transparent background for game over text
            // g2d.setColor(new Color(0, 0, 0, 180));
            // g2d.fillRect(0, 0, getWidth(), getHeight());

            drawCenteredString(g2d, "GAME OVER", gameOverLargeFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetLarge);
            drawCenteredString(g2d, "Final Score: " + gameState.getScore(), gameOverMediumFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetMedium);
            drawCenteredString(g2d, "(R: Restart / ESC: Exit)", gameOverSmallFont, AppConfig.GAME_UI_TEXT_COLOR, yOffsetSmall);

        // Draw Pause overlay if applicable (and not game over)
        } else if (gameState.isPaused()) {
            // Draw semi-transparent overlay across the screen
            g2d.setColor(AppConfig.PAUSE_OVERLAY_COLOR);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Draw pause text on top
            drawCenteredString(g2d, "PAUSED", pauseLargeFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetLarge);
            drawCenteredString(g2d, "(Press 'P' to Resume)", pauseSmallFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetSmall);
        }
        // Could add other overlays for 'Get Ready', 'Level Complete', etc. based on GameState
    }

    /** Helper method to draw horizontally centered text at a specified vertical offset. */
    private void drawCenteredString(Graphics2D g2d, String text, Font font, Color color, int yOffset) {
        g2d.setFont(font);
        g2d.setColor(color);
        FontMetrics metrics = g2d.getFontMetrics(font);
        // Center horizontally based on configured game width
        int x = (AppConfig.GAME_WIDTH - metrics.stringWidth(text)) / 2;
        // Center vertically based on configured game height, adjusting for font metrics and offset
        int y = (AppConfig.GAME_HEIGHT / 2) - (metrics.getHeight() / 2) + metrics.getAscent() + yOffset;
        g2d.drawString(text, x, y);
    }
}

// --- Game Controller (Manages Game Loop, State Transitions, Interactions) ---
// Orchestrates the game flow using GameState, GameLogic, and UI interactions.
class GameController implements ActionListener {
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GamePanel gamePanel; // To trigger repaints
    private final Timer gameTimer; // Swing Timer for EDT-safe updates
    private final DatabaseManager dbManager;
    private final TerribleGame mainApp; // Reference to main app for global actions

    public GameController(GameState state, GameLogic logic, GamePanel panel, DatabaseManager dbMgr, TerribleGame app) {
        // Null checks for dependencies
        if (state == null || logic == null || panel == null || dbMgr == null || app == null) {
            throw new IllegalArgumentException("All dependencies must be non-null for GameController");
        }
        this.gameState = state;
        this.gameLogic = logic;
        this.gamePanel = panel;
        this.dbManager = dbMgr;
        this.mainApp = app;

        // Initialize Swing Timer - calls actionPerformed on the EDT
        this.gameTimer = new Timer(AppConfig.GAME_TICK_MS, this);
        this.gameTimer.setInitialDelay(0); // Start immediately when timer.start() is called
        this.gameTimer.setCoalesce(true); // Combine multiple pending events if updates are slow
    }

    /** Starts a new game session. */
    public void startGame() {
        System.out.println("GameController: Starting game...");
        gameLogic.initializeNewGame(); // Reset state and prepare logic
        gameState.setRunning(true);    // Mark game as actively running
        gameState.setPaused(false);    // Ensure not paused initially
        // gameState.setGameOver(false); // reset() already handles this
        gameTimer.start();             // Start the game loop timer
        System.out.println("Game Started. Timer running (" + AppConfig.GAME_TICK_MS + "ms interval).");
        gamePanel.repaint(); // Initial paint
    }

    /** Stops the game loop timer and marks the game as not running. */
    public void stopGameLoop() {
        if (gameTimer.isRunning()) {
            gameTimer.stop();
            gameState.setRunning(false); // Mark state as not running
            System.out.println("Game Loop Stopped.");
        }
    }

     /** Toggles the paused state of the game. */
     public void togglePause() {
        // Check if pausing/resuming is valid in the current state
        if (!gameState.isRunning() || gameState.isGameOver()) {
            System.out.println("Cannot toggle pause. Game running: " + gameState.isRunning() + ", Game over: " + gameState.isGameOver());
            return; // Can't pause if not running or already over
        }

        boolean currentPauseState = gameState.isPaused();
        gameState.setPaused(!currentPauseState); // Delegate toggling to GameState

        if (gameState.isPaused()) {
            // gameTimer.stop(); // Stopping timer might be too abrupt, maybe just skip logic update?
            // For simplicity, let timer run but logic won't execute due to isPaused check
            System.out.println("Game Paused");
        } else {
            // gameTimer.start(); // Only needed if timer was stopped
            System.out.println("Game Resumed");
        }
        gamePanel.repaint(); // Repaint immediately to show/hide pause overlay
    }

    /** Handles the sequence of events when the game reaches a 'Game Over' state. */
    private void handleGameOver() {
        System.out.println("GameController: Handling Game Over...");
        stopGameLoop(); // Stop updates

        // GameState should already have isGameOver = true and isRunning = false
        // gameState.setGameOver(true); // Already done by logic/state

        System.out.println("Game Over! Final Score: " + gameState.getScore());

        // Save score if a user is logged in
        String username = mainApp.getCurrentUsername();
        if (username != null && gameState.getScore() > 0) { // Only save if score > 0?
            System.out.println("Saving score for user: " + username);
            dbManager.saveScore(username, gameState.getScore());
        } else {
            System.out.println("Score not saved (no user logged in or score is zero).");
        }

        gamePanel.repaint(); // Ensure game over screen is drawn immediately
    }

    /** Initiates the application shutdown process via the main application class. */
    public void exitGame() {
        System.out.println("GameController: Exit requested.");
        stopGameLoop(); // Ensure game loop is stopped first
        mainApp.shutdown(); // Tell main application to handle shutdown
    }

    /** Handles the request to restart the game (usually goes back to login). */
    public void requestRestart() {
        System.out.println("GameController: Restart requested.");
        stopGameLoop();
        // Don't reset gameState here, happens when starting a new game
        mainApp.switchToLoginScreen(); // Tell main app to switch view
    }

    /**
     * Called by the Swing Timer on the EDT. Executes one game tick.
     * Updates logic, checks for game over, and requests a repaint.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        // Primary game loop executed on timer tick

        // Check if the game logic should update based on the current state
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
            // --- Update Game Logic ---
            gameLogic.update();

            // --- Check for State Changes (e.g., Game Over) ---
            // The update() might have triggered a game over condition inside GameState
            if (gameState.isGameOver()) {
                // If game over was set during the update, handle it now
                handleGameOver();
                // Repaint is implicitly called by handleGameOver's repaint call.
            } else {
                // --- Render Frame ---
                // If game is still running normally, request repaint
                gamePanel.repaint();
            }
        } else if (gameState.isGameOver() || gameState.isPaused()) {
            // If paused or game over, we still might want to repaint to show the overlay correctly
            // (e.g., if something else invalidated the panel)
            // Repainting here ensures the overlay stays visible even if the timer fires.
            gamePanel.repaint();
        }
        // If !isRunning and not paused/game over (e.g., on login screen), do nothing.
    }
}


// --- Main Application Class (JFrame) ---
// Sets up the window, manages panels via CardLayout, and orchestrates components.
// Holds the central instances of GameState, Controllers, etc.
public class TerribleGame extends JFrame {

    // Core game components, instantiated here and passed to others
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GameController gameController;
    private final InputHandler inputHandler;
    private final DatabaseManager dbManager;

    // UI Panels
    private final GamePanel gamePanel;
    private final LoginScreen loginScreen;

    // UI Management
    private CardLayout cardLayout;
    private final JPanel mainPanel;

    // Session State (simple version)
    private String currentUsername = null;

    public TerribleGame() {
        super(AppConfig.APP_TITLE); // Set window title from config

        // ---- Initialization Order ----
        // 1. Non-UI components first
        dbManager = new DatabaseManager();       // Manages database interactions
        gameState = new GameState();           // Holds all runtime game state
        gameLogic = new GameLogic(gameState);    // Contains game rules, operates on GameState

        // 2. UI Panels (depend on GameState or DatabaseManager)
        // Note: Pass 'this' (mainApp) for callbacks like userLoggedIn or switching panels
        loginScreen = new LoginScreen(this, dbManager);
        gamePanel = new GamePanel(gameState);      // Renders the GameState

        // 3. Controller and Input Handler (depend on state, logic, panels, mainApp)
        gameController = new GameController(gameState, gameLogic, gamePanel, dbManager, this);
        inputHandler = new InputHandler(gameState, gameController); // Translates key events

        // 4. Setup JFrame and Panel Management
        setupWindow();
        mainPanel = setupCardLayoutAndPanels(); // Create main container with CardLayout
        add(mainPanel, BorderLayout.CENTER);   // Add container panel to the frame

        // 5. Add KeyListener AFTER panel is added and focusable
        // KeyListener should be added to the component that needs to receive key events (GamePanel)
        gamePanel.addKeyListener(inputHandler);

        // 6. Finalize Window
        pack(); // Adjust window size to preferred sizes of components (alternative to setSize)
        // setSize(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT); // Or use fixed size from config
        setLocationRelativeTo(null); // Center window on screen
        setVisible(true);            // Make the window visible

        // 7. Show the initial screen (Login Panel)
        switchToLoginScreen();
    }

    /** Configures the main JFrame window properties. */
    private void setupWindow() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle closing manually
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                System.out.println("Window closing event received.");
                gameController.exitGame(); // Trigger clean shutdown via controller
            }
        });
        setResizable(false); // Prevent resizing which might break layout
        // Frame should not steal focus from its panels
        setFocusable(false);
    }

    /** Creates the main JPanel with CardLayout and adds the login and game panels. */
    private JPanel setupCardLayoutAndPanels() {
        cardLayout = new CardLayout();
        JPanel panel = new JPanel(cardLayout);

        // Add panels with their designated IDs from AppConfig
        panel.add(loginScreen, AppConfig.LOGIN_PANEL_ID);
        panel.add(gamePanel, AppConfig.GAME_PANEL_ID);

        return panel;
    }

    // --- View Switching Methods ---

    /** Switches the view to the Game Panel and starts the game. */
    public void switchToGameScreen() {
        System.out.println("Switching to Game Screen...");
        cardLayout.show(mainPanel, AppConfig.GAME_PANEL_ID);

        // Request focus for the game panel *after* it's made visible
        // Use invokeLater to ensure it happens after the layout change is processed
        SwingUtilities.invokeLater(() -> {
            boolean focused = gamePanel.requestFocusInWindow();
             if (focused) {
                System.out.println("GamePanel focus requested successfully.");
             } else {
                System.err.println("Warning: GamePanel failed to gain focus.");
                // As a fallback, sometimes focusing the content pane helps, but panel focus is preferred
                // getContentPane().requestFocusInWindow();
             }
             // Start the game logic AFTER switching view and attempting focus
             gameController.startGame();
        });
    }

     /** Switches the view to the Login Panel, stopping the game if running. */
     public void switchToLoginScreen() {
         System.out.println("Switching to Login Screen...");
         gameController.stopGameLoop(); // Ensure game stops before showing login
         this.currentUsername = null;   // Clear current user session
         loginScreen.clearForm();      // Clear login fields
         loginScreen.refreshHighScores(); // Update high score display
         cardLayout.show(mainPanel, AppConfig.LOGIN_PANEL_ID);

         // Request focus for the login screen (specifically, maybe the username field)
         SwingUtilities.invokeLater(() -> {
             // loginScreen.requestFocusInWindow(); // Focus the panel itself
             usernameFieldRequestFocus(loginScreen); // Try focusing the username field
         });
     }

     // Helper to request focus on the username field within LoginScreen
     private void usernameFieldRequestFocus(LoginScreen login) {
         // Find the username field (this is a bit fragile, relies on knowing component order/type)
         // A better way would be for LoginScreen to expose a method like requestUsernameFocus()
         Component userField = null;
         for (Component comp : login.getComponents()) {
             if (comp instanceof JTextField) { // Assuming first JTextField is username
                 userField = comp;
                 break;
             }
         }
         if (userField != null) {
             boolean focused = userField.requestFocusInWindow();
              if (focused) {
                 System.out.println("Username field focus requested successfully.");
             } else {
                 System.err.println("Warning: Username field failed to gain focus.");
                 // Fallback to panel focus if field focus fails
                 login.requestFocusInWindow();
             }
         } else {
             login.requestFocusInWindow(); // Fallback if field not found
         }
     }


    // --- User Management Callback ---

    /** Called by LoginScreen upon successful user authentication. */
    public void userLoggedIn(String username) {
        if (username == null || username.trim().isEmpty()) {
            System.err.println("Login attempt with invalid username.");
            return;
        }
        this.currentUsername = username;
        System.out.println("User '" + username + "' logged in. Proceeding to game.");
        switchToGameScreen(); // Switch to the game panel
    }

    /** Returns the username of the currently logged-in user, or null if none. */
    public String getCurrentUsername() {
        return currentUsername;
    }

    // --- Application Lifecycle ---

    /** Handles graceful shutdown of the application. */
    public void shutdown() {
        System.out.println("MainApp: Initiating shutdown...");
        // 1. Stop game loop (should already be done if called from controller.exitGame)
        gameController.stopGameLoop();
        // 2. Close database connection
        dbManager.closeConnection();
        // 3. Dispose of the main window resources
        dispose();
        System.out.println("Shutdown complete. Exiting JVM.");
        // 4. Terminate the Java Virtual Machine
        System.exit(0);
    }

    // --- Main Entry Point ---
    public static void main(String[] args) {
        // Ensure AppConfig is loaded (happens automatically on first access)
        System.out.println("Application starting with title: " + AppConfig.APP_TITLE);

        // Best practice: Create and show Swing GUI components on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            // Create the main application window instance
            TerribleGame game = new TerribleGame();

            // Optional: Add a JVM shutdown hook for cleanup on abnormal termination.
            // Note: This hook is not guaranteed to run in all circumstances (e.g., kill -9).
            // Actions within the hook should be minimal and fast.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("JVM Shutdown Hook executing...");
                // Avoid complex operations. Database closing etc. should ideally
                // happen via the normal window closing/exit process.
                // If game instance is available, could try a last-ditch cleanup.
                // if (game != null && game.dbManager != null) {
                //    game.dbManager.closeConnection(); // Risky if already closed
                // }
            }, "ShutdownCleanupThread"));
        });
    }
}