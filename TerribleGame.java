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
                    // Attempt to parse known color names (case-insensitive)
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
    public static final String APP_TITLE = loadStringEnv("APP_TITLE", "Secure Space Game"); // Updated title
    public static final int GAME_WIDTH = loadIntEnv("GAME_WIDTH", 800);
    public static final int GAME_HEIGHT = loadIntEnv("GAME_HEIGHT", 600);
    public static final int GAME_TICK_MS = loadIntEnv("GAME_TICK_MS", 16);
    public static final int BOTTOM_UI_BUFFER = loadIntEnv("BOTTOM_UI_BUFFER", 30);

    // --- Database ---
    public static final String DATABASE_URL = loadStringEnv("DATABASE_URL", "jdbc:sqlite:secure_game_data.db"); // Renamed DB file
    public static final String JDBC_DRIVER = loadStringEnv("JDBC_DRIVER", "org.sqlite.JDBC");
    public static final int HIGH_SCORE_LIMIT = loadIntEnv("HIGH_SCORE_LIMIT", 10);
    public static final int BCRYPT_LOG_ROUNDS = loadIntEnv("BCRYPT_LOG_ROUNDS", 12); // Configurable BCrypt work factor

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


// --- Core Data Models (Unchanged) ---

/**
 * Abstract base class for all objects appearing in the game world.
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

    protected void updateBounds() {
        bounds.setLocation(x, y);
    }

    public Rectangle getBounds() {
        return bounds;
    }

    public abstract void update();
    public abstract void draw(Graphics2D g2d);

    public boolean isOutOfBounds(int screenHeight) {
        return y > screenHeight || y + height < 0;
    }

    public boolean intersects(GameObject other) {
        return this.bounds.intersects(other.getBounds());
    }
}

/**
 * Represents the player's spaceship.
 */
class Player extends GameObject {
    private int speed;
    private boolean movingLeft, movingRight, movingUp, movingDown;
    private boolean wantsToShoot = false;

    public Player(int startX, int startY) {
        super(startX, startY, AppConfig.PLAYER_WIDTH, AppConfig.PLAYER_HEIGHT);
        this.speed = AppConfig.PLAYER_SPEED;
        this.movingLeft = false;
        this.movingRight = false;
        this.movingUp = false;
        this.movingDown = false;
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

        x = Math.max(0, x);
        x = Math.min(AppConfig.GAME_WIDTH - width, x);
        y = Math.max(0, y);
        y = Math.min(AppConfig.GAME_HEIGHT - height - AppConfig.BOTTOM_UI_BUFFER, y);

        updateBounds();
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.PLAYER_COLOR);
        g2d.fillRect(x, y, width, height);
    }

    public Bullet shoot() {
         this.wantsToShoot = false;
         int bulletX = this.x + this.width / 2 - AppConfig.BULLET_WIDTH / 2;
         int bulletY = this.y - AppConfig.BULLET_HEIGHT;
         return new Bullet(bulletX, bulletY);
    }

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
        y -= speed;
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
        y += speed;
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

    public PowerUp(int x, int y, int speed) {
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
        g2d.setColor(AppConfig.POWERUP_COLOR);
        g2d.fillOval(x, y, width, height);
    }

    public void applyEffect(GameState gameState) {
        gameState.increaseScore(AppConfig.POWERUP_SCORE_VALUE);
    }
}

// --- Database Management (REFACTORED FOR SECURITY) ---
// Encapsulates database operations with parameterized queries and password hashing.
class DatabaseManager {
    private final String dbUrl;
    private final String jdbcDriver;

    // Load JDBC driver once when the class is loaded.
    static {
        try {
            Class.forName(AppConfig.JDBC_DRIVER);
            System.out.println("JDBC Driver loaded: " + AppConfig.JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("FATAL: JDBC Driver not found! Check classpath: " + AppConfig.JDBC_DRIVER);
            throw new RuntimeException("Failed to load JDBC Driver", e);
        }
    }

    public DatabaseManager() {
        this.dbUrl = AppConfig.DATABASE_URL;
        this.jdbcDriver = AppConfig.JDBC_DRIVER;
        initializeDatabaseTables();
    }

    /**
     * Establishes and returns a NEW database connection.
     * The caller is responsible for closing this connection (typically via try-with-resources).
     * @return A new Connection object.
     * @throws SQLException if a database access error occurs.
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Initializes the necessary database tables if they don't exist.
     * The `password` column now stores a hash, not plaintext.
     */
    private void initializeDatabaseTables() {
        // SECURITY: Password column now stores a hash (TEXT is suitable for BCrypt hashes)
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    " username TEXT UNIQUE NOT NULL," +
                                    " password_hash TEXT NOT NULL" + // Changed column name
                                    ");";
        String createHighScoresTable = "CREATE TABLE IF NOT EXISTS high_scores (" +
                                      " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                      " username TEXT NOT NULL," +
                                      " score INTEGER NOT NULL," +
                                      " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                                      ");";

        // Use try-with-resources for Connection and Statement
        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {

            statement.execute(createUsersTable);
            statement.execute(createHighScoresTable);
            System.out.println("Database tables checked/created successfully (Password stored as hash).");

        } catch (SQLException e) {
            handleError("CRITICAL: Error initializing database tables. Application might not function correctly.", e);
        }
    }

    /**
     * Registers a new user, hashing the password before storage.
     * Uses try-with-resources for connection and parameterized queries.
     * @param username The username.
     * @param plainTextPassword The user's chosen password (will be hashed).
     * @return true if registration is successful, false otherwise.
     */
    public boolean registerUser(String username, String plainTextPassword) {
        if (!isInputValid(username, plainTextPassword)) return false;

        // SQL Injection Prevention: Using PreparedStatement
        String checkUserSql = "SELECT id FROM users WHERE username = ?";
        // SECURITY: Store password_hash, not password
        String insertUserSql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = getConnection()) {
            // Check if user exists first (using PreparedStatement)
            try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Registration failed: Username '" + username + "' already exists.");
                        return false; // User exists
                    }
                }
            }

            // SECURITY: Hash the password using BCrypt with a generated salt
            String hashedPassword = BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(AppConfig.BCRYPT_LOG_ROUNDS));

            // Insert new user with hashed password (using PreparedStatement)
            try (PreparedStatement insertStmt = conn.prepareStatement(insertUserSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword); // Store the hash
                int result = insertStmt.executeUpdate();
                if (result > 0) {
                     System.out.println("User '" + username + "' registered successfully.");
                     return true;
                } else {
                    System.err.println("Registration failed: Insert returned 0 rows affected.");
                    return false;
                }
            }

        } catch (SQLException e) {
            handleError("Error during user registration for username: " + username, e);
            return false;
        } catch (Exception e) { // Catch potential errors from BCrypt (though less likely here)
            handleError("Error during password hashing for user: " + username, e);
            return false;
        }
    }

    /**
     * Validates user credentials by comparing the provided password against the stored hash.
     * Uses try-with-resources for connection and parameterized queries.
     * @param username The username.
     * @param plainTextPassword The password attempt to check.
     * @return true if the username exists and the password matches the stored hash, false otherwise.
     */
    public boolean validateUser(String username, String plainTextPassword) {
        if (!isInputValid(username, plainTextPassword)) return false;

        // SQL Injection Prevention: Using PreparedStatement
        // SECURITY: Retrieve password_hash
        String querySql = "SELECT password_hash FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    // SECURITY: Use BCrypt.checkpw to compare plaintext against the hash
                    if (storedHash != null) {
                        try {
                            return BCrypt.checkpw(plainTextPassword, storedHash);
                        } catch (IllegalArgumentException ex) {
                             // Handle cases where the stored hash might be invalid/corrupted
                             handleError("Invalid hash format encountered for user: " + username, ex);
                             return false;
                        }
                    } else {
                         // Should not happen if NOT NULL constraint is enforced, but good to check
                         System.err.println("Warning: Retrieved null password hash for user: " + username);
                         return false;
                    }
                } else {
                    return false; // Username not found
                }
            }
        } catch (SQLException e) {
            handleError("Error validating user: " + username, e);
            return false;
        }
    }

    /**
     * Saves a high score for a user. Uses try-with-resources for connection and parameterized query.
     * (No change needed here for security issues addressed, already uses PreparedStatement)
     * @param username The user who achieved the score.
     * @param score The score value.
     */
    public void saveScore(String username, int score) {
        if (username == null || username.trim().isEmpty() || score < 0) {
            System.err.println("Invalid data for saving score (User: " + username + ", Score: " + score + ")");
            return;
        }

        // SQL Injection Prevention: Using PreparedStatement
        String insertScoreSql = "INSERT INTO high_scores (username, score) VALUES (?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertScoreSql)) {

            pstmt.setString(1, username);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();
            System.out.println("Score " + score + " for user " + username + " saved.");

        } catch (SQLException e) {
            handleError("Error saving score for user: " + username, e);
        }
    }

    /**
     * Retrieves the top high scores. Uses try-with-resources for connection, parameterized query, and result set.
     * (No change needed here for security issues addressed, already uses PreparedStatement)
     * @return A list of formatted strings representing the high scores. Returns a list containing an error message on failure.
     */
    public List<String> getHighScores() {
        int limit = AppConfig.HIGH_SCORE_LIMIT;
        List<String> scores = new ArrayList<>();
        // SQL Injection Prevention: Using PreparedStatement (for limit)
        String queryHighScores = "SELECT username, score FROM high_scores ORDER BY score DESC LIMIT ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(queryHighScores)) {

            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    String username = rs.getString("username");
                    int scoreValue = rs.getInt("score");
                    scores.add(String.format("%d. %-15s : %d", rank++, username, scoreValue));
                }
                if (rank == 1) { // No scores found
                    scores.add("No scores recorded yet.");
                }
            }

        } catch (SQLException e) {
            handleError("Error fetching high scores", e);
            scores.clear();
            scores.add("Error loading scores due to database issue.");
        }

        return scores;
    }

    /**
     * @deprecated Connections are now managed automatically per operation via try-with-resources.
     */
    @Deprecated
    public void closeConnection() {
        System.out.println("DatabaseManager.closeConnection() is deprecated. Connection management is automatic.");
    }

    /**
     * Basic input validation for username and password (before hashing).
     */
    private boolean isInputValid(String username, String password) {
        // Basic checks, could be enhanced (e.g., length, characters)
        return username != null && !username.trim().isEmpty() && password != null && !password.isEmpty();
    }

    /**
     * Centralized error logging for database operations.
     */
    private void handleError(String message, Exception e) {
        System.err.println("DATABASE/SECURITY ERROR: " + message + " - " + e.getMessage());
        // Consider more robust logging in a real application (e.g., using a logging framework)
        // e.printStackTrace(); // Uncomment for detailed debugging if needed
    }
}


// --- NEW: Game State Encapsulation (Unchanged) ---
/**
 * Encapsulates the mutable state of the game world and player status.
 * Provides controlled access and modification methods.
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
    private boolean isRunning;

    public GameState() {
        this.player = new Player(0, 0);
        this.enemies = new ArrayList<>();
        this.powerUps = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.score = 0;
        this.lives = AppConfig.INITIAL_LIVES;
        this.isGameOver = false;
        this.isPaused = false;
        this.isRunning = false;
    }

    public void reset() {
        player.resetPosition(
            AppConfig.GAME_WIDTH / 2 - AppConfig.PLAYER_WIDTH / 2,
            AppConfig.GAME_HEIGHT - AppConfig.BOTTOM_UI_BUFFER - AppConfig.PLAYER_HEIGHT - 10
        );
        enemies.clear();
        powerUps.clear();
        bullets.clear();
        score = 0;
        lives = AppConfig.INITIAL_LIVES;
        isGameOver = false;
        isPaused = false;
        isRunning = false;
    }

    public Player getPlayer() { return player; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<PowerUp> getPowerUps() { return powerUps; }
    public List<Bullet> getBullets() { return bullets; }
    public int getScore() { return score; }
    public int getLives() { return lives; }
    public boolean isGameOver() { return isGameOver; }
    public boolean isPaused() { return isPaused; }
    public boolean isRunning() { return isRunning; }

    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
        if (gameOver) {
            this.isRunning = false;
        }
    }

    public void setPaused(boolean paused) {
        if (this.isRunning && !this.isGameOver) {
            this.isPaused = paused;
        }
    }

    public void setRunning(boolean running) {
        if (!this.isGameOver) {
            this.isRunning = running;
            if (!running) {
                this.isPaused = false;
            }
        } else {
            this.isRunning = false;
        }
    }

    public void decreaseLives() {
        if (lives > 0) {
            lives--;
            System.out.println("Life lost. Lives remaining: " + lives);
        }
        if (lives <= 0 && !isGameOver) {
            setGameOver(true);
            System.out.println("Lives reached zero. Game Over triggered.");
        }
    }

    public void increaseScore(int amount) {
        if (amount > 0 && isRunning) {
            score += amount;
        }
    }

    public void addBullet(Bullet bullet) {
        if (bullet != null) bullets.add(bullet);
    }

    public void addEnemy(Enemy enemy) {
        if (enemy != null) enemies.add(enemy);
    }

    public void addPowerUp(PowerUp powerUp) {
        if (powerUp != null) powerUps.add(powerUp);
    }

    public void removeBullets(List<Bullet> bulletsToRemove) {
        if (bulletsToRemove != null) bullets.removeAll(bulletsToRemove);
    }

    public void removeEnemies(List<Enemy> enemiesToRemove) {
        if (enemiesToRemove != null) enemies.removeAll(enemiesToRemove);
    }

    public void removePowerUps(List<PowerUp> powerUpsToRemove) {
        if (powerUpsToRemove != null) powerUps.removeAll(powerUpsToRemove);
    }
}


// --- Game Logic (Unchanged) ---
// Now depends on the GameState object passed to it.
class GameLogic {
    private final Random randomGenerator = new Random();
    private final GameState gameState;

    public GameLogic(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("GameState cannot be null for GameLogic");
        }
        this.gameState = state;
    }

    public void initializeNewGame() {
        gameState.reset();
        System.out.println("Game logic initialized for a new game.");
    }

    public void update() {
        if (!gameState.isRunning() || gameState.isPaused() || gameState.isGameOver()) {
            return;
        }

        gameState.getPlayer().update();
        handleShooting();

        updateGameObjects(gameState.getBullets());
        updateGameObjects(gameState.getEnemies());
        updateGameObjects(gameState.getPowerUps());

        handleCollisions();
        handleSpawning();
    }

    private <T extends GameObject> void updateGameObjects(List<T> list) {
        Iterator<T> iterator = list.iterator();
        while (iterator.hasNext()) {
            T obj = iterator.next();
            obj.update();
            if (obj.isOutOfBounds(AppConfig.GAME_HEIGHT)) {
                iterator.remove();
            }
        }
    }

    private void handleShooting() {
        Player player = gameState.getPlayer();
        if (player.isWantsToShoot()) {
            gameState.addBullet(player.shoot());
        }
    }

    private void handleCollisions() {
        Player player = gameState.getPlayer();
        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<PowerUp> powerUpsToRemove = new ArrayList<>();

        // Player vs Enemy
        for (Enemy enemy : gameState.getEnemies()) {
            if (!enemiesToRemove.contains(enemy) && player.intersects(enemy)) {
                gameState.decreaseLives();
                enemiesToRemove.add(enemy);
                if (gameState.isGameOver()) {
                    gameState.removeEnemies(enemiesToRemove);
                    gameState.removeBullets(bulletsToRemove);
                    gameState.removePowerUps(powerUpsToRemove);
                    return;
                }
            }
        }

        // Bullet vs Enemy
        for (Bullet bullet : gameState.getBullets()) {
             if (bulletsToRemove.contains(bullet)) continue;
            for (Enemy enemy : gameState.getEnemies()) {
                 if (enemiesToRemove.contains(enemy)) continue;
                if (bullet.intersects(enemy)) {
                    bulletsToRemove.add(bullet);
                    enemiesToRemove.add(enemy);
                    gameState.increaseScore(AppConfig.ENEMY_SCORE_VALUE);
                    break; // One bullet hits one enemy
                }
            }
        }

        // Player vs PowerUp
        for (PowerUp powerUp : gameState.getPowerUps()) {
             if (!powerUpsToRemove.contains(powerUp) && player.intersects(powerUp)) {
                powerUp.applyEffect(gameState);
                powerUpsToRemove.add(powerUp);
            }
        }

        gameState.removeEnemies(enemiesToRemove);
        gameState.removeBullets(bulletsToRemove);
        gameState.removePowerUps(powerUpsToRemove);
    }

    private void handleSpawning() {
        if (gameState.getEnemies().size() < AppConfig.MAX_ENEMIES &&
            randomGenerator.nextDouble() < AppConfig.ENEMY_SPAWN_CHANCE) {
            spawnEnemy();
        }
        if (gameState.getPowerUps().size() < AppConfig.MAX_POWERUPS &&
            randomGenerator.nextDouble() < AppConfig.POWERUP_SPAWN_CHANCE) {
            spawnPowerUp();
        }
    }

    private void spawnEnemy() {
        int size = AppConfig.ENEMY_BASE_SIZE + (AppConfig.ENEMY_SIZE_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SIZE_VARIATION) : 0);
        int enemyX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - size);
        int enemyY = -size;
        int enemySpeed = AppConfig.ENEMY_BASE_SPEED + (AppConfig.ENEMY_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SPEED_VARIATION) : 0);
        gameState.addEnemy(new Enemy(enemyX, enemyY, size, size, enemySpeed));
    }

    private void spawnPowerUp() {
        int powerUpX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - AppConfig.POWERUP_WIDTH);
        int powerUpY = -AppConfig.POWERUP_HEIGHT;
        int powerUpSpeed = AppConfig.POWERUP_BASE_SPEED + (AppConfig.POWERUP_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.POWERUP_SPEED_VARIATION) : 0);
        gameState.addPowerUp(new PowerUp(powerUpX, powerUpY, powerUpSpeed));
    }
}


// --- Input Handling (Unchanged) ---
// Now receives GameState to access the player and check game status.
class InputHandler extends KeyAdapter {
    private final GameState gameState;
    private final GameController gameController;

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

        switch (keyCode) {
            case KeyEvent.VK_P:
                gameController.togglePause();
                return;
            case KeyEvent.VK_ESCAPE:
                gameController.exitGame();
                return;
            case KeyEvent.VK_R:
                if (gameState.isGameOver()) {
                    System.out.println("Restart key (R) pressed - Requesting restart.");
                    gameController.requestRestart();
                }
                return;
        }

        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
             Player player = gameState.getPlayer();
             if (player == null) return;

            switch (keyCode) {
                case KeyEvent.VK_LEFT: case KeyEvent.VK_A: player.setMovingLeft(true); break;
                case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: player.setMovingRight(true); break;
                case KeyEvent.VK_UP: case KeyEvent.VK_W: player.setMovingUp(true); break;
                case KeyEvent.VK_DOWN: case KeyEvent.VK_S: player.setMovingDown(true); break;
                case KeyEvent.VK_SPACE: case KeyEvent.VK_CONTROL: player.setWantsToShoot(true); break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        Player player = gameState.getPlayer();
        if (player == null) return;

        switch (keyCode) {
            case KeyEvent.VK_LEFT: case KeyEvent.VK_A: player.setMovingLeft(false); break;
            case KeyEvent.VK_RIGHT: case KeyEvent.VK_D: player.setMovingRight(false); break;
            case KeyEvent.VK_UP: case KeyEvent.VK_W: player.setMovingUp(false); break;
            case KeyEvent.VK_DOWN: case KeyEvent.VK_S: player.setMovingDown(false); break;
        }
    }
}


// --- UI Panels ---

/**
 * Login Screen Panel - Handles user login/registration UI.
 * Interacts with the REFACTORED DatabaseManager for secure operations.
 */
class LoginScreen extends JPanel implements ActionListener {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JTextArea highScoreTextArea;
    private final TerribleGame mainApp;
    private final DatabaseManager dbManager; // Uses the refactored, secure DB Manager

    public LoginScreen(TerribleGame mainApp, DatabaseManager dbManager) {
        if (mainApp == null || dbManager == null) {
            throw new IllegalArgumentException("MainApp and DatabaseManager cannot be null for LoginScreen");
        }
        this.mainApp = mainApp;
        this.dbManager = dbManager; // Injected secure manager
        setupUI();
    }

    private void setupUI() {
        setLayout(null);
        setBackground(AppConfig.LOGIN_BACKGROUND_COLOR);
        setPreferredSize(new Dimension(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT));

        int centerX = AppConfig.GAME_WIDTH / 2;
        int fieldWidth = 160;
        int labelWidth = 80;
        int startY = 150;
        int fieldSpacing = 40;
        int buttonWidth = 120;
        int buttonSpacing = 20;

        JLabel usernameLabel = createLabel("Username:", centerX - fieldWidth / 2 - labelWidth, startY);
        usernameField = new JTextField();
        usernameField.setBounds(centerX - fieldWidth / 2, startY, fieldWidth, 25);
        add(usernameLabel);
        add(usernameField);

        JLabel passwordLabel = createLabel("Password:", centerX - fieldWidth / 2 - labelWidth, startY + fieldSpacing);
        passwordField = new JPasswordField();
        passwordField.setBounds(centerX - fieldWidth / 2, startY + fieldSpacing, fieldWidth, 25);
        add(passwordLabel);
        add(passwordField);

        int totalButtonWidth = buttonWidth * 2 + buttonSpacing;
        loginButton = createButton("Login", centerX - totalButtonWidth / 2, startY + 2 * fieldSpacing, buttonWidth, 30);
        registerButton = createButton("Register", centerX - totalButtonWidth / 2 + buttonWidth + buttonSpacing, startY + 2 * fieldSpacing, buttonWidth, 30);
        add(loginButton);
        add(registerButton);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setBounds(centerX - (fieldWidth + labelWidth) / 2, startY + 3 * fieldSpacing, fieldWidth + labelWidth, 25);
        statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
        add(statusLabel);

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
        return button;
    }

     public void refreshHighScores() {
        List<String> scores = dbManager.getHighScores(); // Uses secure DB Manager
        highScoreTextArea.setText("--- High Scores (Top " + AppConfig.HIGH_SCORE_LIMIT + ") ---\n");
        if (scores != null) {
            for (String scoreLine : scores) {
                highScoreTextArea.append(scoreLine + "\n");
            }
        }
        highScoreTextArea.setCaretPosition(0);
     }

     public void clearForm() {
         usernameField.setText("");
         passwordField.setText("");
         statusLabel.setText("");
         statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        // SECURITY: Handle password carefully. Convert to String only when needed by BCrypt.
        String plainTextPassword = new String(passwordChars);

        if (username.isEmpty() || plainTextPassword.isEmpty()) {
            statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
            statusLabel.setText("Username and password cannot be empty.");
            // SECURITY: Clear the char array even on validation failure
            java.util.Arrays.fill(passwordChars, ' ');
            return;
        }

        try {
            if (e.getSource() == loginButton) {
                // SECURITY: Use validateUser which uses BCrypt.checkpw
                if (dbManager.validateUser(username, plainTextPassword)) {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                    statusLabel.setText("Login Successful!");
                    Timer switchTimer = new Timer(500, ae -> mainApp.userLoggedIn(username));
                    switchTimer.setRepeats(false);
                    switchTimer.start();
                } else {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                    statusLabel.setText("Login failed. Check credentials.");
                    passwordField.setText(""); // Clear password field on failure
                }
            } else if (e.getSource() == registerButton) {
                 // SECURITY: Use registerUser which uses BCrypt.hashpw
                if (dbManager.registerUser(username, plainTextPassword)) {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                    statusLabel.setText("Registration successful! Please log in.");
                    usernameField.setText(""); // Clear fields after successful registration
                    passwordField.setText("");
                    // Optionally refresh high scores if needed: refreshHighScores();
                } else {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                    // Give a slightly more generic error message for security
                    statusLabel.setText("Registration failed (username might exist or error occurred).");
                    passwordField.setText(""); // Clear password field on failure
                }
            }
        } finally {
             // SECURITY: Always clear the plaintext password from memory immediately after use
             java.util.Arrays.fill(passwordChars, ' ');
             // Note: The 'plainTextPassword' String object will be garbage collected later.
             // Clearing the char array is the most critical step for immediate removal.
        }
    }
}

/**
 * Game Screen Panel - Renders the game state. (Unchanged)
 */
class GamePanel extends JPanel {
    private final GameState gameState;

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
        setDoubleBuffered(true);
        setFocusable(true);
        setRequestFocusEnabled(true);
        setFocusTraversalKeysEnabled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2d);

        if (gameState.isRunning() || gameState.isGameOver()) {
            drawGameObjects(g2d);
        }

        drawUI(g2d);
        drawOverlays(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_BACKGROUND_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    private void drawGameObjects(Graphics2D g2d) {
        try {
            Player player = gameState.getPlayer();
            if (player != null) player.draw(g2d);

            // Draw using copies or synchronized lists if concurrency were an issue
            List<Bullet> bulletsToDraw = new ArrayList<>(gameState.getBullets());
            for (Bullet bullet : bulletsToDraw) bullet.draw(g2d);

            List<Enemy> enemiesToDraw = new ArrayList<>(gameState.getEnemies());
            for (Enemy enemy : enemiesToDraw) enemy.draw(g2d);

            List<PowerUp> powerUpsToDraw = new ArrayList<>(gameState.getPowerUps());
            for (PowerUp powerUp : powerUpsToDraw) powerUp.draw(g2d);
        } catch (Exception e) {
            System.err.println("Error during rendering game objects: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_UI_TEXT_COLOR);
        g2d.setFont(uiFont);
        int uiMarginX = 10;
        int uiMarginYTop = 20;
        int livesRightMargin = 100;
        g2d.drawString("Score: " + gameState.getScore(), uiMarginX, uiMarginYTop);
        g2d.drawString("Lives: " + gameState.getLives(), AppConfig.GAME_WIDTH - livesRightMargin, uiMarginYTop);
    }

    private void drawOverlays(Graphics2D g2d) {
        int yOffsetLarge = -40;
        int yOffsetMedium = 20;
        int yOffsetSmall = 60;

        if (gameState.isGameOver()) {
            drawCenteredString(g2d, "GAME OVER", gameOverLargeFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetLarge);
            drawCenteredString(g2d, "Final Score: " + gameState.getScore(), gameOverMediumFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetMedium);
            drawCenteredString(g2d, "(R: Restart / ESC: Exit)", gameOverSmallFont, AppConfig.GAME_UI_TEXT_COLOR, yOffsetSmall);
        } else if (gameState.isPaused()) {
            g2d.setColor(AppConfig.PAUSE_OVERLAY_COLOR);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            drawCenteredString(g2d, "PAUSED", pauseLargeFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetLarge);
            drawCenteredString(g2d, "(Press 'P' to Resume)", pauseSmallFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetSmall);
        }
    }

    private void drawCenteredString(Graphics2D g2d, String text, Font font, Color color, int yOffset) {
        g2d.setFont(font);
        g2d.setColor(color);
        FontMetrics metrics = g2d.getFontMetrics(font);
        int x = (AppConfig.GAME_WIDTH - metrics.stringWidth(text)) / 2;
        int y = (AppConfig.GAME_HEIGHT / 2) - (metrics.getHeight() / 2) + metrics.getAscent() + yOffset;
        g2d.drawString(text, x, y);
    }
}

// --- Game Controller (Interacts with improved DatabaseManager) ---
class GameController implements ActionListener {
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GamePanel gamePanel;
    private final Timer gameTimer;
    private final DatabaseManager dbManager; // Uses refactored, secure DB Manager
    private final TerribleGame mainApp;

    public GameController(GameState state, GameLogic logic, GamePanel panel, DatabaseManager dbMgr, TerribleGame app) {
        if (state == null || logic == null || panel == null || dbMgr == null || app == null) {
            throw new IllegalArgumentException("All dependencies must be non-null for GameController");
        }
        this.gameState = state;
        this.gameLogic = logic;
        this.gamePanel = panel;
        this.dbManager = dbMgr; // Injected secure manager
        this.mainApp = app;

        this.gameTimer = new Timer(AppConfig.GAME_TICK_MS, this);
        this.gameTimer.setInitialDelay(0);
        this.gameTimer.setCoalesce(true);
    }

    public void startGame() {
        System.out.println("GameController: Starting game...");
        gameLogic.initializeNewGame();
        gameState.setRunning(true);
        gameState.setPaused(false);
        gameTimer.start();
        System.out.println("Game Started. Timer running (" + AppConfig.GAME_TICK_MS + "ms interval).");
        gamePanel.repaint();
    }

    public void stopGameLoop() {
        if (gameTimer.isRunning()) {
            gameTimer.stop();
            gameState.setRunning(false);
            System.out.println("Game Loop Stopped.");
        }
    }

     public void togglePause() {
        if (!gameState.isRunning() || gameState.isGameOver()) {
            System.out.println("Cannot toggle pause. Game running: " + gameState.isRunning() + ", Game over: " + gameState.isGameOver());
            return;
        }
        boolean currentPauseState = gameState.isPaused();
        gameState.setPaused(!currentPauseState);
        System.out.println(gameState.isPaused() ? "Game Paused" : "Game Resumed");
        gamePanel.repaint();
    }

    private void handleGameOver() {
        System.out.println("GameController: Handling Game Over...");
        stopGameLoop(); // Stops timer and sets isRunning = false

        System.out.println("Game Over! Final Score: " + gameState.getScore());

        String username = mainApp.getCurrentUsername();
        if (username != null && gameState.getScore() > 0) {
            System.out.println("Saving score for user: " + username);
            dbManager.saveScore(username, gameState.getScore()); // Uses secure DB Manager
        } else {
            System.out.println("Score not saved (no user logged in or score is zero).");
        }

        gamePanel.repaint(); // Show game over screen
    }

    public void exitGame() {
        System.out.println("GameController: Exit requested.");
        stopGameLoop();
        mainApp.shutdown(); // Triggers main app shutdown process
    }

    public void requestRestart() {
        System.out.println("GameController: Restart requested.");
        stopGameLoop();
        mainApp.switchToLoginScreen();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
            gameLogic.update();
            if (gameState.isGameOver()) {
                handleGameOver(); // Handles stopping loop, saving score, repaint
            } else {
                gamePanel.repaint(); // Standard repaint during gameplay
            }
        } else if (gameState.isGameOver() || gameState.isPaused()) {
            // Ensure overlays are painted even if timer fires while paused/over
            gamePanel.repaint();
        }
    }
}


// --- Main Application Class (JFrame) ---
// Uses the refactored DatabaseManager. No global DB connection closing needed.
public class TerribleGame extends JFrame {

    // Core components
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GameController gameController;
    private final InputHandler inputHandler;
    private final DatabaseManager dbManager; // *** Uses the refactored, secure DatabaseManager ***

    // UI Panels
    private final GamePanel gamePanel;
    private final LoginScreen loginScreen;

    // UI Management
    private CardLayout cardLayout;
    private final JPanel mainPanel;

    // Session State
    private String currentUsername = null;

    public TerribleGame() {
        super(AppConfig.APP_TITLE);

        // Initialize components (DB Manager, State, Logic, Panels, Controller, Input)
        // *** Instantiates the refactored DatabaseManager ***
        dbManager = new DatabaseManager(); // Secure version is created
        gameState = new GameState();
        gameLogic = new GameLogic(gameState);
        // *** LoginScreen receives the secure DatabaseManager ***
        loginScreen = new LoginScreen(this, dbManager);
        gamePanel = new GamePanel(gameState);
        // *** GameController receives the secure DatabaseManager ***
        gameController = new GameController(gameState, gameLogic, gamePanel, dbManager, this);
        inputHandler = new InputHandler(gameState, gameController);

        // Setup window and layout
        setupWindow();
        mainPanel = setupCardLayoutAndPanels();
        add(mainPanel, BorderLayout.CENTER);

        // Add key listener to the game panel
        gamePanel.addKeyListener(inputHandler);

        // Finalize and show window
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Start on login screen
        switchToLoginScreen();
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                System.out.println("Window closing event received.");
                gameController.exitGame();
            }
        });
        setResizable(false);
        setFocusable(false);
    }

    private JPanel setupCardLayoutAndPanels() {
        cardLayout = new CardLayout();
        JPanel panel = new JPanel(cardLayout);
        panel.add(loginScreen, AppConfig.LOGIN_PANEL_ID);
        panel.add(gamePanel, AppConfig.GAME_PANEL_ID);
        return panel;
    }

    // --- View Switching Methods ---

    public void switchToGameScreen() {
        System.out.println("Switching to Game Screen...");
        cardLayout.show(mainPanel, AppConfig.GAME_PANEL_ID);
        SwingUtilities.invokeLater(() -> {
            boolean focused = gamePanel.requestFocusInWindow();
             if (focused) System.out.println("GamePanel focus requested successfully.");
             else System.err.println("Warning: GamePanel failed to gain focus.");
             gameController.startGame();
        });
    }

     public void switchToLoginScreen() {
         System.out.println("Switching to Login Screen...");
         gameController.stopGameLoop();
         this.currentUsername = null;
         loginScreen.clearForm();
         loginScreen.refreshHighScores(); // Uses secure DB Manager
         cardLayout.show(mainPanel, AppConfig.LOGIN_PANEL_ID);
         SwingUtilities.invokeLater(() -> usernameFieldRequestFocus(loginScreen));
     }

     private void usernameFieldRequestFocus(LoginScreen login) {
         // Attempt to find and focus the username field
         Component[] components = login.getComponents();
         Component userField = null;
         
         // Look for a JTextField that comes after a "Username:" JLabel
         for (int i = 1; i < components.length; i++) {
             if (components[i] instanceof JTextField &&
                 components[i-1] instanceof JLabel &&
                 "Username:".equals(((JLabel)components[i-1]).getText())) {
                 userField = components[i];
                 break;
             }
         }
         
         // If not found, fall back to the first JTextField
         if (userField == null) {
             for (Component comp : components) {
                 if (comp instanceof JTextField) {
                     userField = comp;
                     break;
                 }
             }
         }
         
         // Request focus on the found field or fall back to panel
         if (userField != null) {
             userField.requestFocusInWindow();
         } else {
             login.requestFocusInWindow();
         }
     }

    // --- User Management Callback ---

    public void userLoggedIn(String username) {
        if (username == null || username.trim().isEmpty()) {
            System.err.println("Login attempt with invalid username.");
            return;
        }
        this.currentUsername = username;
        System.out.println("User '" + username + "' logged in. Proceeding to game.");
        switchToGameScreen();
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    // --- Application Lifecycle ---

    /** Handles graceful shutdown of the application. */
    public void shutdown() {
        System.out.println("MainApp: Initiating shutdown...");
        // 1. Stop game loop
        gameController.stopGameLoop();

        // 2. *** No explicit DB connection closing needed ***
        // Connections are managed via try-with-resources within DatabaseManager methods.
        System.out.println("Database connections are managed per-operation (try-with-resources).");

        // 3. Dispose UI resources
        dispose();
        System.out.println("Shutdown complete. Exiting JVM.");
        // 4. Terminate JVM
        System.exit(0);
    }

    // --- Main Entry Point ---
    public static void main(String[] args) {
        System.out.println("Application starting with title: " + AppConfig.APP_TITLE);
        System.out.println("NOTE: Ensure the jBCrypt library is included in the classpath.");

        // Ensure GUI creation happens on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            new TerribleGame(); // Creates and shows the main application window

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("JVM Shutdown Hook executing...");
            }, "ShutdownCleanupThread"));
        });
    }
}