import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator; // Using Iterator for safe removal during iteration
import java.util.Random;

// --- Constants ---
// Constants remain largely the same, grouped for clarity.
class GameConfig {
    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;
    public static final String DATABASE_URL = "jdbc:sqlite:terrible_game_data.db";
    public static final int PLAYER_WIDTH = 30;
    public static final int PLAYER_HEIGHT = 15;
    public static final int PLAYER_SPEED = 5;
    public static final int BULLET_WIDTH = 5;
    public static final int BULLET_HEIGHT = 10;
    public static final int BULLET_SPEED = 8;
    public static final int POWERUP_WIDTH = 15;
    public static final int POWERUP_HEIGHT = 15;
    public static final int INITIAL_LIVES = 3;
    public static final int GAME_TICK_MS = 16; // Approx 60 FPS
    public static final int MAX_ENEMIES = 15;
    public static final int MAX_POWERUPS = 5;
    public static final double ENEMY_SPAWN_CHANCE = 0.05; // 5% per update tick
    public static final double POWERUP_SPAWN_CHANCE = 0.02; // 2% per update tick
    public static final int ENEMY_BASE_SPEED = 2;
    public static final int ENEMY_SPEED_VARIATION = 3;
    public static final int POWERUP_BASE_SPEED = 3;
    public static final int POWERUP_SPEED_VARIATION = 2;
    public static final int ENEMY_BASE_SIZE = 20;
    public static final int ENEMY_SIZE_VARIATION = 30;
    public static final int ENEMY_SCORE_VALUE = 10;
    public static final int POWERUP_SCORE_VALUE = 50;
    public static final int BOTTOM_UI_BUFFER = 30; // Space at the bottom
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
        return y > screenHeight || y + height < 0; // Check if fully below or fully above screen
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
        super(startX, startY, GameConfig.PLAYER_WIDTH, GameConfig.PLAYER_HEIGHT);
        this.speed = GameConfig.PLAYER_SPEED;
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

        // Apply boundary checks
        x = Math.max(0, x);
        x = Math.min(GameConfig.GAME_WIDTH - width, x);
        y = Math.max(0, y);
        y = Math.min(GameConfig.GAME_HEIGHT - height - GameConfig.BOTTOM_UI_BUFFER, y);

        updateBounds(); // Update bounds after moving
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.CYAN);
        g2d.fillRect(x, y, width, height);
    }

    // Player-specific action: create a bullet
    public Bullet shoot() {
         this.wantsToShoot = false; // Consume the trigger
         int bulletX = this.x + this.width / 2 - GameConfig.BULLET_WIDTH / 2;
         int bulletY = this.y - GameConfig.BULLET_HEIGHT;
         return new Bullet(bulletX, bulletY);
    }
}

/**
 * Represents a bullet fired by the player.
 */
class Bullet extends GameObject {
    private int speed;

    public Bullet(int x, int y) {
        super(x, y, GameConfig.BULLET_WIDTH, GameConfig.BULLET_HEIGHT);
        this.speed = GameConfig.BULLET_SPEED;
    }

    @Override
    public void update() {
        y -= speed;
        updateBounds();
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.YELLOW);
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
        g2d.setColor(Color.RED);
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
        super(x, y, GameConfig.POWERUP_WIDTH, GameConfig.POWERUP_HEIGHT);
        this.speed = speed;
    }

    @Override
    public void update() {
        y += speed;
        updateBounds();
    }

    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(Color.GREEN);
        g2d.fillOval(x, y, width, height); // Draw as oval
    }

    // Could have an applyEffect(Player player) method here
    public void applyEffect(GameState gameState) {
        gameState.increaseScore(GameConfig.POWERUP_SCORE_VALUE);
        // Example: gameState.increaseLives(1);
        // Example: player.increaseFireRate();
    }
}

// --- Database Management (Instance-based) ---
// Manages database connection and operations for users and scores.
class DatabaseManager {
    private Connection databaseConnection = null;
    private final String dbUrl;

    public DatabaseManager(String dbUrl) {
        this.dbUrl = dbUrl;
        // Initialize connection immediately or lazily later
        getConnection(); // Try initial connection
        initializeDatabaseTables();
    }

    private Connection getConnection() {
        if (databaseConnection == null) {
            try {
                Class.forName("org.sqlite.JDBC"); // Ensure driver is loaded
                databaseConnection = DriverManager.getConnection(dbUrl);
                System.out.println("Database connection established.");
            } catch (ClassNotFoundException e) {
                handleError("SQLite JDBC Driver not found! Check classpath.", e);
                System.exit(1); // Critical failure
            } catch (SQLException e) {
                handleError("Database connection failed", e);
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

    public List<String> getHighScores(int limit) {
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
        this.lives = GameConfig.INITIAL_LIVES;
        this.isGameOver = false;
        this.isPaused = false;
        this.isRunning = false; // Not running initially
    }

    // Resets the game state for a new game
    public void reset() {
        player.x = GameConfig.GAME_WIDTH / 2 - GameConfig.PLAYER_WIDTH / 2;
        player.y = GameConfig.GAME_HEIGHT - 50 - GameConfig.PLAYER_HEIGHT;
        player.updateBounds(); // Ensure bounds are correct after reset

        enemies.clear();
        powerUps.clear();
        bullets.clear();

        score = 0;
        lives = GameConfig.INITIAL_LIVES;
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
        // Initial population of game objects
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
            if (obj.isOutOfBounds(GameConfig.GAME_HEIGHT)) {
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
                    gameState.increaseScore(GameConfig.ENEMY_SCORE_VALUE);
                    break; // One bullet hits one enemy
                }
            }
        }

        // 3. Player vs PowerUp collisions
        for (PowerUp powerUp : gameState.getPowerUps()) {
             if (!powerUpsToRemove.contains(powerUp) && player.intersects(powerUp)) {
                powerUp.applyEffect(gameState); // Apply the power-up effect
                powerUpsToRemove.add(powerUp);
            }
        }

        // Remove collided objects from the game state
        gameState.removeEnemies(enemiesToRemove);
        gameState.removeBullets(bulletsToRemove);
        gameState.removePowerUps(powerUpsToRemove);
    }

    private void handleSpawning() {
        // Spawn enemies based on chance and count limit
        if (gameState.getEnemies().size() < GameConfig.MAX_ENEMIES && randomGenerator.nextDouble() < GameConfig.ENEMY_SPAWN_CHANCE) {
            spawnEnemy();
        }
        // Spawn power-ups based on chance and count limit
        if (gameState.getPowerUps().size() < GameConfig.MAX_POWERUPS && randomGenerator.nextDouble() < GameConfig.POWERUP_SPAWN_CHANCE) {
            spawnPowerUp();
        }
    }

    private void spawnEnemy() {
        int size = GameConfig.ENEMY_BASE_SIZE + randomGenerator.nextInt(GameConfig.ENEMY_SIZE_VARIATION);
        int enemyX = randomGenerator.nextInt(GameConfig.GAME_WIDTH - size);
        int enemyY = -size; // Start just above the screen
        int enemySpeed = GameConfig.ENEMY_BASE_SPEED + randomGenerator.nextInt(GameConfig.ENEMY_SPEED_VARIATION);
        gameState.addEnemy(new Enemy(enemyX, enemyY, size, size, enemySpeed));
    }

    private void spawnPowerUp() {
        int powerUpX = randomGenerator.nextInt(GameConfig.GAME_WIDTH - GameConfig.POWERUP_WIDTH);
        int powerUpY = -GameConfig.POWERUP_HEIGHT; // Start just above the screen
        int powerUpSpeed = GameConfig.POWERUP_BASE_SPEED + randomGenerator.nextInt(GameConfig.POWERUP_SPEED_VARIATION);
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
            case KeyEvent.VK_P:
                if (gameState.isRunning() && !gameState.isGameOver()) {
                    gameController.togglePause();
                }
                return;
            case KeyEvent.VK_ESCAPE:
                gameController.exitGame();
                return;
            case KeyEvent.VK_R:
                if (gameState.isGameOver()) {
                    System.out.println("Restart key (R) pressed - Triggering restart via controller.");
                    gameController.requestRestart();
                }
                return;
        }

        // Process game input if the game is running and not paused/over
        // Process game input if game is active (more permissive conditions)
        if (!gameState.isGameOver()) {
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

        // Only process movement keys
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
                // Shooting is handled as a one-time event in keyPressed
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
        setLayout(null); // Using null layout as in original
        setBackground(Color.DARK_GRAY);
        setPreferredSize(new Dimension(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT));

        // Username
        JLabel usernameLabel = createLabel("Username:", 300, 150);
        usernameField = new JTextField();
        usernameField.setBounds(400, 150, 160, 25);
        add(usernameLabel);
        add(usernameField);

        // Password
        JLabel passwordLabel = createLabel("Password:", 300, 190);
        passwordField = new JPasswordField();
        passwordField.setBounds(400, 190, 160, 25);
        add(passwordLabel);
        add(passwordField);

        // Buttons
        loginButton = createButton("Login", 300, 230, 120, 30);
        registerButton = createButton("Register", 440, 230, 120, 30);
        add(loginButton);
        add(registerButton);

        // Status Label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setBounds(300, 270, 260, 25);
        statusLabel.setForeground(Color.RED);
        add(statusLabel);

        // High Scores Area
        highScoreTextArea = new JTextArea();
        highScoreTextArea.setBounds(50, 350, GameConfig.GAME_WIDTH - 100, 200);
        highScoreTextArea.setEditable(false);
        highScoreTextArea.setForeground(Color.CYAN);
        highScoreTextArea.setBackground(Color.BLACK);
        highScoreTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(highScoreTextArea); // Add scroll pane
        scrollPane.setBounds(50, 350, GameConfig.GAME_WIDTH - 100, 200);
        add(scrollPane);
    }

    private JLabel createLabel(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, 80, 25);
        label.setForeground(Color.WHITE);
        return label;
    }

    private JButton createButton(String text, int x, int y, int w, int h) {
        JButton button = new JButton(text);
        button.setBounds(x, y, w, h);
        button.addActionListener(this);
        return button;
    }

     public void refreshHighScores() {
        List<String> scores = dbManager.getHighScores(10);
        highScoreTextArea.setText("--- High Scores ---\n");
        for (String scoreLine : scores) {
            highScoreTextArea.append(scoreLine + "\n");
        }
     }

     // Clear input fields and status
     public void clearForm() {
         usernameField.setText("");
         passwordField.setText("");
         statusLabel.setText("");
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return;
        }

        if (e.getSource() == loginButton) {
            if (dbManager.validateUser(username, password)) {
                statusLabel.setForeground(Color.GREEN);
                statusLabel.setText("Login Successful!");
                mainApp.userLoggedIn(username); // Notify main app of successful login
            } else {
                statusLabel.setForeground(Color.RED);
                statusLabel.setText("Login failed. Check username/password.");
                passwordField.setText(""); // Clear password on failure
            }
        } else if (e.getSource() == registerButton) {
            if (dbManager.registerUser(username, password)) {
                statusLabel.setForeground(Color.GREEN);
                statusLabel.setText("Registration successful! You can now log in.");
                usernameField.setText(""); // Clear fields after success
                passwordField.setText("");
                refreshHighScores(); // Potentially update scores if needed (though unlikely on register)
            } else {
                statusLabel.setForeground(Color.RED);
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

    public GamePanel(GameState state) {
        this.gameState = state;
        setPreferredSize(new Dimension(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true); // Improve rendering performance
        setFocusable(true);
        setRequestFocusEnabled(true);
        setFocusTraversalKeysEnabled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw Background
        drawBackground(g2d);

        // Draw Game Objects only if the game is considered running
        if (gameState.isRunning() || gameState.isGameOver()) { // Draw objects even on game over screen
            drawGameObjects(g2d);
            drawUI(g2d); // Draw score, lives
        }

        // Draw Overlays (Pause, Game Over)
        drawOverlays(g2d);
    }

    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        // Could add stars or other background elements here
    }

    private void drawGameObjects(Graphics2D g2d) {
        // Use defensive copies or synchronized blocks if concurrent modification is a risk
        // For Swing Timer on EDT, usually safe, but iterators preferred for modification
        try {
            // Draw Player
            gameState.getPlayer().draw(g2d);

            // Draw Bullets
            // Create copy to avoid ConcurrentModificationException if list is modified elsewhere (unlikely here)
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
            // Catch potential exceptions during drawing iteration
            System.err.println("Error during drawing game objects: " + e.getMessage());
            // e.printStackTrace();
        }
    }

    private void drawUI(Graphics2D g2d) {
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 16));
        g2d.drawString("Score: " + gameState.getScore(), 10, 20);
        g2d.drawString("Lives: " + gameState.getLives(), GameConfig.GAME_WIDTH - 100, 20);
        // Display current user?
        // g2d.drawString("Player: " + mainApp.getCurrentUsername(), GameConfig.GAME_WIDTH / 2 - 50, 20);
    }

    private void drawOverlays(Graphics2D g2d) {
        if (gameState.isGameOver()) {
            drawCenteredString(g2d, "GAME OVER", new Font("Arial", Font.BOLD, 48), Color.YELLOW, -50);
            drawCenteredString(g2d, "Final Score: " + gameState.getScore(), new Font("Arial", Font.BOLD, 24), Color.YELLOW, 0);
            drawCenteredString(g2d, "(Press R to Restart / ESC to Exit)", new Font("Arial", Font.PLAIN, 16), Color.WHITE, 50);
        } else if (gameState.isPaused()) {
            // Semi-transparent overlay for pause
            g2d.setColor(new Color(0, 0, 0, 150)); // Black with alpha
            g2d.fillRect(0, 0, getWidth(), getHeight());

            drawCenteredString(g2d, "PAUSED", new Font("Arial", Font.BOLD, 48), Color.WHITE, 0);
            drawCenteredString(g2d, "(Press 'P' to resume)", new Font("Arial", Font.PLAIN, 16), Color.WHITE, 50);
        } else if (!gameState.isRunning() && !gameState.isGameOver()) {
             // Optional: Message before game starts (e.g., if transitioning)
             // drawCenteredString(g2d, "Prepare for Battle!", new Font("Arial", Font.BOLD, 30), Color.WHITE, 0);
        }
    }

    // Helper method for drawing centered text
    private void drawCenteredString(Graphics2D g2d, String text, Font font, Color color, int yOffset) {
        g2d.setFont(font);
        g2d.setColor(color);
        FontMetrics metrics = g2d.getFontMetrics(font);
        int x = (GameConfig.GAME_WIDTH - metrics.stringWidth(text)) / 2;
        int y = (GameConfig.GAME_HEIGHT / 2) + yOffset;
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
        // Timer calls actionPerformed every GameConfig.GAME_TICK_MS milliseconds
        this.gameTimer = new Timer(GameConfig.GAME_TICK_MS, this);
    }

    public void startGame() {
        gameLogic.initializeNewGame(); // Sets up initial game state and objects
        gameState.setRunning(true);    // Mark game as running
        gameState.setPaused(false);    // Ensure not paused
        gameState.setGameOver(false);  // Ensure not game over
        gameTimer.start();
        System.out.println("Game Started. Timer running.");
    }

    // Stops the game loop but doesn't necessarily mean 'Game Over'
    public void stopGameLoop() {
        gameState.setRunning(false);
        gameTimer.stop();
        System.out.println("Game Loop Stopped.");
    }

     public void togglePause() {
        if (!gameState.isRunning() || gameState.isGameOver()) return; // Can't pause if not running or already over

        gameState.setPaused(!gameState.isPaused()); // Toggle pause state

        if (gameState.isPaused()) {
            gameTimer.stop();
            System.out.println("Game Paused");
        } else {
            gameTimer.start(); // Resume timer
            System.out.println("Game Resumed");
        }
        gamePanel.repaint(); // Repaint immediately to show/hide pause overlay
    }

    // Called when the game reaches a game over condition
    private void handleGameOver() {
        stopGameLoop(); // Stop the timer and updates
        gameState.setGameOver(true); // Mark as game over
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
        System.out.println("Exit requested.");
        stopGameLoop(); // Ensure game loop is stopped
        mainApp.shutdown(); // Ask main application to close gracefully
    }

    // Handles request to restart the game (typically back to login screen)
    public void requestRestart() {
        System.out.println("Restart requested.");
        stopGameLoop();
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
                handleGameOver();
            } else {
               gamePanel.repaint(); // Render the updated state
            }
        } else if (gameState.isGameOver()) {
            // If game is over, ensure timer is stopped (should be by handleGameOver)
            if(gameTimer.isRunning()) {
                gameTimer.stop();
                 System.err.println("Warning: Timer was still running in Game Over state.");
            }
            // No need to repaint constantly unless Game Over screen has animations
        }
        // If paused, do nothing in the timer tick (timer should be stopped anyway)
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

    private static final String LOGIN_PANEL_ID = "LoginPanel";
    private static final String GAME_PANEL_ID = "GamePanel";

    public TerribleGame() {
        super("The Less Terrible Space Game (OOP Refactor)");

        // 1. Initialize Core Components (non-UI first)
        dbManager = new DatabaseManager(GameConfig.DATABASE_URL);
        gameState = new GameState();
        gameLogic = new GameLogic(gameState);

        // 2. Initialize UI Panels
        loginScreen = new LoginScreen(this, dbManager); // Pass ref to self and dbManager
        gamePanel = new GamePanel(gameState);           // Pass ref to gameState

        // 3. Initialize Controller and Input Handler (need UI refs)
        gameController = new GameController(gameState, gameLogic, gamePanel, dbManager, this);
        inputHandler = new InputHandler(gameState, gameController); // Pass gameState and controller

        // 4. Setup JFrame and CardLayout
        setupWindow();
        setupCardLayout();

        // 5. Focus Management (remove frame-level key listener)
        setFocusable(false); // Frame should not be focusable
        setFocusTraversalKeysEnabled(false);

        // 6. Finalize Window Setup
        setLocationRelativeTo(null); // Center window
        setVisible(true);

        // 7. Show Initial Screen (Login)
        switchToLoginScreen(); // Use method to ensure consistency
    }

    private void setupWindow() {
        setSize(GameConfig.GAME_WIDTH, GameConfig.GAME_HEIGHT);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Handle close manually
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                shutdown(); // Ensure clean shutdown on window close
            }
        });
        setResizable(false);
    }

    private void setupCardLayout() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Set up login screen
        loginScreen.setFocusable(true);
        mainPanel.add(loginScreen, LOGIN_PANEL_ID);
        
        // Set up game panel with key listener
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(inputHandler);
        mainPanel.add(gamePanel, GAME_PANEL_ID);
        
        add(mainPanel, BorderLayout.CENTER);
    }

    // --- View Switching Methods ---

    public void switchToGameScreen() {
        cardLayout.show(mainPanel, GAME_PANEL_ID);
        gameController.startGame(); // Start the game logic and timer
        
        // Ensure proper focus for key events
        gamePanel.requestFocusInWindow();
        
        // Add small delay to ensure focus is properly set
        SwingUtilities.invokeLater(() -> {
            gamePanel.requestFocusInWindow();
        });
        
        System.out.println("Switched to Game Screen. Game started.");
    }

     public void switchToLoginScreen() {
         gameController.stopGameLoop(); // Make sure game loop is stopped
         this.currentUsername = null; // Log out the user conceptually
         loginScreen.clearForm(); // Clear username/password fields
         loginScreen.refreshHighScores(); // Update high scores display
         cardLayout.show(mainPanel, LOGIN_PANEL_ID);
         
         // Ensure proper focus for login screen
         SwingUtilities.invokeLater(() -> {
             loginScreen.requestFocusInWindow();
         });
         
         System.out.println("Switched back to Login Screen.");
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

    // Graceful shutdown procedure
    public void shutdown() {
        System.out.println("Shutting down application...");
        gameController.stopGameLoop(); // Ensure game loop is off
        dbManager.closeConnection(); // Close database connection
        System.out.println("Shutdown complete. Exiting.");
        System.exit(0); // Terminate the application
    }

    // --- Main Entry Point ---
    public static void main(String[] args) {
        // Ensure Swing components are created on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            TerribleGame game = new TerribleGame();

            // Add shutdown hook for JVM termination (e.g., Ctrl+C)
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown hook triggered.");
                // Accessing non-static 'game' or 'dbManager' here is tricky/unsafe.
                // Better rely on window closing listener or manual exit.
                // Forcing DB close statically if needed:
                // DatabaseManager.forceCloseStaticConnection(); // If a static method existed
                // But instance-based approach relies on 'shutdown()' being called.
            }));
        });
    }
}