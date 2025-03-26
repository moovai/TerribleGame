// RefactoredGame.java - A refactored game for educational purposes

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// --- Constants ---
class GameConstants {
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
}

// --- Data Transfer Objects / Simple Data Holders ---

// Represents a generic game entity with position, size, and speed
// Used as a base or structure for player, enemies, bullets, power-ups
// Using int[] for simplicity as in the original, though dedicated classes would be better OOP.
// [0]=x, [1]=y, [2]=width, [3]=height, [4]=speed (optional, depends on entity type)


// --- Database Management ---
class DatabaseManager {
    private static Connection databaseConnection = null;

    public static Connection getConnection() {
        // Establish connection if needed
        if (databaseConnection == null) {
            try {
                Class.forName("org.sqlite.JDBC");
                databaseConnection = DriverManager.getConnection(GameConstants.DATABASE_URL);
                System.out.println("Database connection established.");
                initializeDatabaseTables();
            } catch (ClassNotFoundException e) {
                System.err.println("SQLite JDBC Driver not found! Make sure the JAR is in the classpath.");
                JOptionPane.showMessageDialog(null, "Critical Error: SQLite JDBC Driver not found.\nPlease check application setup.", "Database Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            } catch (SQLException e) {
                System.err.println("Database connection failed: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Could not connect to the database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                // Optionally return null or throw a custom exception
            }
        }
        // Check if connection is still valid
        try {
             if (databaseConnection != null && databaseConnection.isClosed()) {
                 System.out.println("Database connection was closed, reopening...");
                 databaseConnection = DriverManager.getConnection(GameConstants.DATABASE_URL);
                 System.out.println("Database connection re-established.");
             }
        } catch(SQLException e) {
             System.err.println("Failed to check/reopen database connection: " + e.getMessage());
             databaseConnection = null; // Force re-establishment next time
        }
        return databaseConnection;
    }

    private static void initializeDatabaseTables() {
        Connection conn = getConnection();
        if (conn == null) {
            System.err.println("Cannot initialize DB tables, connection is null.");
            return;
        }
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    " username TEXT UNIQUE NOT NULL," +
                                    " password TEXT NOT NULL" +
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
            System.err.println("Error creating database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean registerUser(String username, String password, JLabel statusLabel) {
         if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return false;
        }
        Connection conn = getConnection();
        if (conn == null) {
            statusLabel.setText("Registration failed (database connection error).");
            return false;
        }

        // Use PreparedStatement to prevent SQL injection
        String checkUserSql = "SELECT id FROM users WHERE username = ?";
        String insertUserSql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
            checkStmt.setString(1, username);
            ResultSet resultSet = checkStmt.executeQuery();

            if (resultSet.next()) {
                statusLabel.setText("Username already exists.");
                resultSet.close();
                return false;
            }
            resultSet.close(); // Close result set promptly

            // If username doesn't exist, proceed with insertion
            try (PreparedStatement insertStmt = conn.prepareStatement(insertUserSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, password); // In a real app, hash the password!
                int result = insertStmt.executeUpdate();

                if (result > 0) {
                    statusLabel.setText("Registration successful!");
                    return true;
                } else {
                    statusLabel.setText("Registration failed (database error).");
                    return false;
                }
            }

        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            statusLabel.setText("Registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public static boolean loginUser(String username, String password, JLabel statusLabel) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return false;
        }
        Connection conn = getConnection();
        if (conn == null) {
            statusLabel.setText("Login failed (database connection error).");
            return false;
        }

        // Use PreparedStatement
        String querySql = "SELECT password FROM users WHERE username = ?";

        try (PreparedStatement preparedStatement = conn.prepareStatement(querySql)) {
            preparedStatement.setString(1, username);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                // IMPORTANT: In a real application, use password hashing and comparison
                if (storedPassword.equals(password)) {
                    statusLabel.setText("Login Successful!"); // Set success message here
                    resultSet.close();
                    return true;
                } else {
                    statusLabel.setText("Incorrect password.");
                    resultSet.close();
                    return false;
                }
            } else {
                statusLabel.setText("Username not found.");
                resultSet.close();
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
            statusLabel.setText("Login failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

     public static void saveScore(String username, int score) {
        if (username == null || score <= 0) {
            System.err.println("Invalid data for saving score (User: " + username + ", Score: " + score + ")");
            return;
        }
        Connection conn = getConnection();
         if (conn == null) {
             System.err.println("Database connection is null. Cannot save score.");
             return;
         }

        String insertScoreSql = "INSERT INTO high_scores (username, score) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = conn.prepareStatement(insertScoreSql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setInt(2, score);
            preparedStatement.executeUpdate();
            System.out.println("Score " + score + " for user " + username + " saved.");
        } catch (SQLException e) {
            System.err.println("Error saving score: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getHighScores() {
        Connection conn = getConnection();
        StringBuilder highScoreText = new StringBuilder("--- High Scores ---\n");
        if (conn == null) {
            System.err.println("Database connection is likely null. Cannot fetch scores.");
            highScoreText.append("Database connection error.\n");
            return highScoreText.toString();
        }

        String queryHighScores = "SELECT username, score FROM high_scores ORDER BY score DESC LIMIT 10";

        try (Statement statement = conn.createStatement();
             ResultSet resultSet = statement.executeQuery(queryHighScores)) {

            int rank = 1;
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                int scoreValue = resultSet.getInt("score");
                highScoreText.append(String.format("%d. %-15s : %d\n", rank++, username, scoreValue));
            }
            if (rank == 1) {
                highScoreText.append("No scores recorded yet.\n");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching high scores: " + e.getMessage());
            highScoreText.append("Error loading scores.\n");
            e.printStackTrace();
        }
        return highScoreText.toString();
    }

    public static void closeConnection() {
        try {
            if (databaseConnection != null && !databaseConnection.isClosed()) {
                System.out.println("Closing database connection.");
                databaseConnection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

// --- User Session Management ---
class UserSession {
    private static String currentUsername = null;

    public static void login(String username) {
        currentUsername = username;
    }

    public static void logout() {
        currentUsername = null;
    }

    public static String getCurrentUsername() {
        return currentUsername;
    }

    public static boolean isLoggedIn() {
        return currentUsername != null;
    }
}


// --- Game State ---
class GameState {
    public int playerX;
    public int playerY;

    public List<int[]> enemyList;
    public List<int[]> powerUpList;
    public List<int[]> bulletList;

    public int score = 0;
    public int lives = GameConstants.INITIAL_LIVES;
    public boolean gameOver = false;
    public boolean paused = false;
    public boolean gameRunning = false;

    // Input flags
    public boolean moveLeft = false;
    public boolean moveRight = false;
    public boolean moveUp = false;
    public boolean moveDown = false;
    public boolean fireTriggered = false;

    public GameState() {
        reset();
    }

    public void reset() {
        playerX = GameConstants.GAME_WIDTH / 2 - GameConstants.PLAYER_WIDTH / 2;
        playerY = GameConstants.GAME_HEIGHT - 50 - GameConstants.PLAYER_HEIGHT;
        score = 0;
        lives = GameConstants.INITIAL_LIVES;
        gameOver = false;
        paused = false;
        gameRunning = false; // Will be set to true when game actually starts

        enemyList = new ArrayList<>();
        powerUpList = new ArrayList<>();
        bulletList = new ArrayList<>();

        moveLeft = false;
        moveRight = false;
        moveUp = false;
        moveDown = false;
        fireTriggered = false;
    }
}


// --- Game Logic ---
class GameLogic {
    private Random randomGenerator = new Random();
    private GameState gameState;

    public GameLogic(GameState state) {
        this.gameState = state;
    }

     public void initializeNewGame() {
        gameState.reset(); // Reset state variables
        gameState.gameRunning = true;

        // Initial population
        for (int i = 0; i < 5; i++) {
            spawnEnemy();
        }
        for (int i = 0; i < 3; i++) {
            spawnPowerUp();
        }
    }

    public void update() {
        if (gameState.gameOver || gameState.paused || !gameState.gameRunning) {
            return;
        }

        updatePlayerPosition();
        handleFiring();
        updateBullets();
        updateEnemies();
        updatePowerUps();
        handleSpawning();
    }

    private void updatePlayerPosition() {
        if (gameState.moveLeft) gameState.playerX -= GameConstants.PLAYER_SPEED;
        if (gameState.moveRight) gameState.playerX += GameConstants.PLAYER_SPEED;
        if (gameState.moveUp) gameState.playerY -= GameConstants.PLAYER_SPEED;
        if (gameState.moveDown) gameState.playerY += GameConstants.PLAYER_SPEED;

        // Boundary checks
        gameState.playerX = Math.max(0, gameState.playerX);
        gameState.playerX = Math.min(GameConstants.GAME_WIDTH - GameConstants.PLAYER_WIDTH, gameState.playerX);
        gameState.playerY = Math.max(0, gameState.playerY);
        gameState.playerY = Math.min(GameConstants.GAME_HEIGHT - GameConstants.PLAYER_HEIGHT - 30, gameState.playerY); // Keep player above status bar area
    }

    private void handleFiring() {
        if (gameState.fireTriggered) {
            int[] newBullet = {
                gameState.playerX + GameConstants.PLAYER_WIDTH / 2 - GameConstants.BULLET_WIDTH / 2,
                gameState.playerY - GameConstants.BULLET_HEIGHT,
                GameConstants.BULLET_WIDTH,
                GameConstants.BULLET_HEIGHT,
                GameConstants.BULLET_SPEED
            };
            gameState.bulletList.add(newBullet);
            gameState.fireTriggered = false; // Consume the trigger
        }
    }

    private void updateBullets() {
        List<int[]> bulletsToRemove = new ArrayList<>();
        for (int[] bullet : gameState.bulletList) {
            bullet[1] -= bullet[4]; // Move bullet up
            if (bullet[1] + bullet[3] < 0) { // Check if off-screen
                bulletsToRemove.add(bullet);
            }
        }
        gameState.bulletList.removeAll(bulletsToRemove);
    }

    private void updateEnemies() {
        List<int[]> enemiesToRemove = new ArrayList<>();
        List<int[]> bulletsConsumed = new ArrayList<>();
        Rectangle playerRect = new Rectangle(gameState.playerX, gameState.playerY, GameConstants.PLAYER_WIDTH, GameConstants.PLAYER_HEIGHT);

        for (int[] enemy : gameState.enemyList) {
            enemy[1] += enemy[4]; // Move enemy down

            Rectangle enemyRect = new Rectangle(enemy[0], enemy[1], enemy[2], enemy[3]);

            // Check collision with player
            if (playerRect.intersects(enemyRect)) {
                gameState.lives--;
                enemiesToRemove.add(enemy);
                if (gameState.lives <= 0) {
                    gameState.gameOver = true;
                    // GameController will handle stopping timer and saving score
                }
                continue; // Skip bullet collision checks if hit player
            }

            // Check collision with bullets
            boolean bulletHit = false;
            for (int[] bullet : gameState.bulletList) {
                 if (bulletsConsumed.contains(bullet)) continue; // Don't check bullets already marked for removal

                Rectangle bulletRect = new Rectangle(bullet[0], bullet[1], bullet[2], bullet[3]);
                if (bulletRect.intersects(enemyRect)) {
                    enemiesToRemove.add(enemy);
                    bulletsConsumed.add(bullet);
                    gameState.score += 10;
                    bulletHit = true;
                    break; // One bullet hits one enemy
                }
            }

             // Check if enemy moved off-screen
            if (!bulletHit && enemy[1] > GameConstants.GAME_HEIGHT) {
                enemiesToRemove.add(enemy);
                // Optional: Penalize player for letting enemies pass
                // gameState.lives--;
            }
        }
        gameState.enemyList.removeAll(enemiesToRemove);
        gameState.bulletList.removeAll(bulletsConsumed); // Remove bullets that hit
    }

     private void updatePowerUps() {
        List<int[]> powerUpsToRemove = new ArrayList<>();
        Rectangle playerRect = new Rectangle(gameState.playerX, gameState.playerY, GameConstants.PLAYER_WIDTH, GameConstants.PLAYER_HEIGHT);

        for (int[] powerUp : gameState.powerUpList) {
            powerUp[1] += powerUp[4]; // Move power-up down

            Rectangle powerUpRect = new Rectangle(powerUp[0], powerUp[1], powerUp[2], powerUp[3]);

            // Check collision with player
            if (playerRect.intersects(powerUpRect)) {
                gameState.score += 50; // Apply power-up effect (e.g., score)
                powerUpsToRemove.add(powerUp);
                // Add other power-up effects here (e.g., extra life, weapon upgrade)
            } else if (powerUp[1] > GameConstants.GAME_HEIGHT) { // Check if off-screen
                powerUpsToRemove.add(powerUp);
            }
        }
        gameState.powerUpList.removeAll(powerUpsToRemove);
    }

    private void handleSpawning() {
        // Spawn enemies randomly
        if (randomGenerator.nextInt(100) < 5) { // 5% chance per frame
            if (gameState.enemyList.size() < 15) { // Limit max enemies
                spawnEnemy();
            }
        }
        // Spawn power-ups randomly
        if (randomGenerator.nextInt(100) < 2) { // 2% chance per frame
            if (gameState.powerUpList.size() < 5) { // Limit max power-ups
                spawnPowerUp();
            }
        }
    }

    private void spawnEnemy() {
        int size = 20 + randomGenerator.nextInt(30); // Varying size
        int enemyWidth = size;
        int enemyHeight = size;
        int enemyX = randomGenerator.nextInt(GameConstants.GAME_WIDTH - enemyWidth);
        int enemyY = -enemyHeight; // Start above screen
        int enemySpeed = 2 + randomGenerator.nextInt(3); // Varying speed
        gameState.enemyList.add(new int[]{enemyX, enemyY, enemyWidth, enemyHeight, enemySpeed});
    }

    private void spawnPowerUp() {
        int powerUpX = randomGenerator.nextInt(GameConstants.GAME_WIDTH - GameConstants.POWERUP_WIDTH);
        int powerUpY = -GameConstants.POWERUP_HEIGHT; // Start above screen
        int powerUpSpeed = 3 + randomGenerator.nextInt(2); // Varying speed
        gameState.powerUpList.add(new int[]{powerUpX, powerUpY, GameConstants.POWERUP_WIDTH, GameConstants.POWERUP_HEIGHT, powerUpSpeed});
    }
}


// --- Input Handling ---
class InputHandler implements KeyListener {
    private GameState gameState;
    private GameController gameController; // To handle pause

    public InputHandler(GameState state, GameController controller) {
        this.gameState = state;
        this.gameController = controller;
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not typically used in action games
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // Movement keys
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            gameState.moveLeft = true;
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            gameState.moveRight = true;
        }
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            gameState.moveUp = true;
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            gameState.moveDown = true;
        }

        // Action keys
        if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_CONTROL) {
            if (gameState.gameRunning && !gameState.gameOver && !gameState.paused) {
                gameState.fireTriggered = true;
            }
        }

        // Control keys
        if (keyCode == KeyEvent.VK_P) {
            if (gameState.gameRunning && !gameState.gameOver) {
                 gameController.togglePause();
            }
        }
        if (keyCode == KeyEvent.VK_ESCAPE) {
            System.out.println("Escape pressed, exiting.");
            DatabaseManager.closeConnection(); // Ensure DB connection is closed
            System.exit(0);
        }

        // Restart (Optional, basic implementation)
        if (gameState.gameOver && keyCode == KeyEvent.VK_R) {
             System.out.println("Restart key (R) pressed - Note: Requires full state reset and potentially view switching.");
             // A full restart might involve signaling the main application class
             // to switch back to login or re-initialize the game.
             // For now, just logs a message.
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // Movement keys
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            gameState.moveLeft = false;
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            gameState.moveRight = false;
        }
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            gameState.moveUp = false;
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            gameState.moveDown = false;
        }
        // Note: fireTriggered is usually consumed immediately, so no key release logic needed for it.
    }
}


// --- UI Panels ---

// Login Screen Panel
class LoginScreen extends JPanel implements ActionListener {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JTextArea highScoreTextArea;
    private RefactoredGame mainApp; // Reference to the main application to switch views

    public LoginScreen(RefactoredGame mainApp) {
        this.mainApp = mainApp;
        setLayout(null);
        setBackground(Color.DARK_GRAY);
        setPreferredSize(new Dimension(GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT));

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(300, 150, 80, 25);
        usernameLabel.setForeground(Color.WHITE);
        add(usernameLabel);

        usernameField = new JTextField();
        usernameField.setBounds(400, 150, 160, 25);
        add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(300, 190, 80, 25);
        passwordLabel.setForeground(Color.WHITE);
        add(passwordLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(400, 190, 160, 25);
        add(passwordField);

        loginButton = new JButton("Login");
        loginButton.setBounds(300, 230, 120, 30);
        loginButton.addActionListener(this);
        add(loginButton);

        registerButton = new JButton("Register");
        registerButton.setBounds(440, 230, 120, 30);
        registerButton.addActionListener(this);
        add(registerButton);

        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setBounds(300, 270, 260, 25);
        statusLabel.setForeground(Color.RED);
        add(statusLabel);

        highScoreTextArea = new JTextArea();
        highScoreTextArea.setBounds(50, 350, GameConstants.GAME_WIDTH - 100, 200);
        highScoreTextArea.setEditable(false);
        highScoreTextArea.setForeground(Color.CYAN);
        highScoreTextArea.setBackground(Color.BLACK);
        highScoreTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(highScoreTextArea);

        refreshHighScores(); // Initial load
    }

     public void refreshHighScores() {
        highScoreTextArea.setText(DatabaseManager.getHighScores());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (e.getSource() == loginButton) {
            if (DatabaseManager.loginUser(username, password, statusLabel)) {
                UserSession.login(username);
                mainApp.switchToGameScreen(); // Tell the main app to switch views
            } else {
                passwordField.setText(""); // Clear password field on failure
            }
        } else if (e.getSource() == registerButton) {
            if (DatabaseManager.registerUser(username, password, statusLabel)) {
                usernameField.setText(""); // Clear fields on successful registration
                passwordField.setText("");
                refreshHighScores(); // Refresh scores in case this affects display later
            } else {
                passwordField.setText(""); // Clear password field on failure
            }
        }
    }
}

// Game Screen Panel (Rendering)
class GamePanel extends JPanel {
    private GameState gameState;

    public GamePanel(GameState state) {
        this.gameState = state;
        setPreferredSize(new Dimension(GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT));
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT);

        // If game hasn't started yet (e.g., initial state before login)
        if (!gameState.gameRunning && !gameState.gameOver) {
             g2d.setColor(Color.WHITE);
             g2d.setFont(new Font("Arial", Font.BOLD, 20));
             // This message might not be seen if the login screen is active
             // Consider showing it only if login is skipped or after logout->restart
             // g2d.drawString("Waiting to start...", GameConstants.GAME_WIDTH / 2 - 100, GameConstants.GAME_HEIGHT / 2);
             return;
        }

        // Draw Player
        g2d.setColor(Color.CYAN);
        g2d.fillRect(gameState.playerX, gameState.playerY, GameConstants.PLAYER_WIDTH, GameConstants.PLAYER_HEIGHT);

        // Draw Bullets (use try-with-resources or defensive copy for potential concurrency issues)
        g2d.setColor(Color.YELLOW);
        try {
            List<int[]> bulletsCopy = new ArrayList<>(gameState.bulletList);
            for (int[] bullet : bulletsCopy) {
                g2d.fillRect(bullet[0], bullet[1], bullet[2], bullet[3]);
            }
        } catch (Exception e) {
            System.err.println("Error drawing bullets: " + e.getMessage());
        }

        // Draw Enemies
        g2d.setColor(Color.RED);
         try {
            List<int[]> enemiesCopy = new ArrayList<>(gameState.enemyList);
            for (int[] enemy : enemiesCopy) {
                g2d.fillRect(enemy[0], enemy[1], enemy[2], enemy[3]);
            }
        } catch (Exception e) {
            System.err.println("Error drawing enemies: " + e.getMessage());
        }

        // Draw Power-ups
        g2d.setColor(Color.GREEN);
        try {
            List<int[]> powerUpsCopy = new ArrayList<>(gameState.powerUpList);
            for (int[] powerUp : powerUpsCopy) {
                g2d.fillOval(powerUp[0], powerUp[1], powerUp[2], powerUp[3]);
            }
        } catch (Exception e) {
            System.err.println("Error drawing powerups: " + e.getMessage());
        }

        // Draw UI Overlays (Score, Lives)
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Consolas", Font.BOLD, 16));
        g2d.drawString("Score: " + gameState.score, 10, 20);
        g2d.drawString("Lives: " + gameState.lives, GameConstants.GAME_WIDTH - 100, 20);

        // Draw Game Over Message
        if (gameState.gameOver) {
            drawCenteredString(g2d, "GAME OVER", new Font("Arial", Font.BOLD, 48), Color.YELLOW, -50);
            drawCenteredString(g2d, "Final Score: " + gameState.score, new Font("Arial", Font.BOLD, 24), Color.YELLOW, 0);
            drawCenteredString(g2d, "(Press ESC to exit)", new Font("Arial", Font.PLAIN, 16), Color.WHITE, 50);
             // Consider adding "(Press R to Restart)" if restart implemented
        }

        // Draw Paused Message
        if (gameState.paused && !gameState.gameOver) {
            // Semi-transparent overlay
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT);

            drawCenteredString(g2d, "PAUSED", new Font("Arial", Font.BOLD, 48), Color.WHITE, 0);
            drawCenteredString(g2d, "(Press 'P' to resume)", new Font("Arial", Font.PLAIN, 16), Color.WHITE, 50);
        }
    }

     // Helper to draw centered text
    private void drawCenteredString(Graphics2D g2d, String text, Font font, Color color, int yOffset) {
        g2d.setFont(font);
        g2d.setColor(color);
        FontMetrics metrics = g2d.getFontMetrics(font);
        int x = (GameConstants.GAME_WIDTH - metrics.stringWidth(text)) / 2;
        int y = (GameConstants.GAME_HEIGHT / 2) + yOffset;
        g2d.drawString(text, x, y);
    }
}

// --- Game Controller (Manages Game Loop and State Transitions) ---
class GameController implements ActionListener {
    private GameState gameState;
    private GameLogic gameLogic;
    private GamePanel gamePanel;
    private javax.swing.Timer gameTimer;

    public GameController(GameState state, GameLogic logic, GamePanel panel) {
        this.gameState = state;
        this.gameLogic = logic;
        this.gamePanel = panel;
        this.gameTimer = new javax.swing.Timer(16, this); // Approx 60 FPS
    }

    public void startGame() {
        gameLogic.initializeNewGame(); // Sets up initial game elements
        gameState.gameRunning = true;
        gameState.gameOver = false;
        gameState.paused = false;
        gameTimer.start();
        System.out.println("Game Started");
    }

    public void stopGame() {
        gameState.gameRunning = false;
        gameTimer.stop();
        System.out.println("Game Stopped");
    }

     public void togglePause() {
        if (!gameState.gameOver) {
            gameState.paused = !gameState.paused;
            if (gameState.paused) {
                gameTimer.stop();
                System.out.println("Game Paused");
            } else {
                gameTimer.start();
                 System.out.println("Game Resumed");
            }
            gamePanel.repaint(); // Repaint to show/hide pause message
        }
    }

    private void endGame() {
        stopGame(); // Stops the timer and sets gameRunning to false
        gameState.gameOver = true;
        System.out.println("Game Over! Final Score: " + gameState.score);

        String username = UserSession.getCurrentUsername();
        if (username != null) {
            DatabaseManager.saveScore(username, gameState.score);
        } else {
            System.out.println("Score not saved - user not logged in.");
        }
        gamePanel.repaint(); // Ensure game over screen is drawn
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        // Only action source should be the gameTimer
        if (e.getSource() == gameTimer) {
            if (!gameState.gameOver && !gameState.paused && gameState.gameRunning) {
                gameLogic.update(); // Update game state
                if (gameState.gameOver) { // Check if update resulted in game over
                    endGame();
                } else {
                   gamePanel.repaint(); // Render the updated state
                }
            } else if (gameState.gameOver) {
                // If game is over, make sure timer is stopped (should be already by endGame)
                if(gameTimer.isRunning()) {
                    gameTimer.stop();
                }
                // No need to update or repaint constantly in game over state unless animations are needed
            }
        }
    }
}


// --- Main Application Class ---
public class RefactoredGame extends JFrame {

    private GameState gameState;
    private GameLogic gameLogic;
    private GamePanel gamePanel;
    private LoginScreen loginScreen;
    private GameController gameController;
    private InputHandler inputHandler;
    private CardLayout cardLayout;
    private JPanel mainPanel;

    private static final String LOGIN_PANEL = "LoginPanel";
    private static final String GAME_PANEL = "GamePanel";


    public RefactoredGame() {
        super("The Less Terrible Space Game");

        // Initialize core components
        DatabaseManager.getConnection(); // Initialize DB connection early
        gameState = new GameState();
        gameLogic = new GameLogic(gameState);

        // Initialize UI panels
        loginScreen = new LoginScreen(this); // Pass reference for view switching
        gamePanel = new GamePanel(gameState);

        // Initialize controllers and handlers
        gameController = new GameController(gameState, gameLogic, gamePanel);
        inputHandler = new InputHandler(gameState, gameController);

        // Setup JFrame
        setSize(GameConstants.GAME_WIDTH, GameConstants.GAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Use CardLayout to switch between Login and Game panels
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(loginScreen, LOGIN_PANEL);
        mainPanel.add(gamePanel, GAME_PANEL);

        add(mainPanel, BorderLayout.CENTER);

        // Input handling - Add KeyListener to the JFrame, ensure it's focusable
        addKeyListener(inputHandler);
        setFocusable(true);
        requestFocusInWindow(); // Request focus for key events

        setLocationRelativeTo(null);
        setVisible(true);

        // Show login screen initially
        cardLayout.show(mainPanel, LOGIN_PANEL);
    }

    // Method called by LoginScreen upon successful login
    public void switchToGameScreen() {
        cardLayout.show(mainPanel, GAME_PANEL);
        gameController.startGame();
        gamePanel.requestFocusInWindow(); // Ensure game panel gets key events after switch
        this.requestFocus(); // Redundant? Maybe helps ensure frame has focus.
        System.out.println("Switched to Game Screen");
    }

     // Optional: Method to switch back to login (e.g., after game over or logout)
     public void switchToLoginScreen() {
         gameController.stopGame(); // Make sure game loop is stopped
         UserSession.logout(); // Log out the user
         loginScreen.refreshHighScores(); // Update high scores display
         cardLayout.show(mainPanel, LOGIN_PANEL);
         loginScreen.requestFocusInWindow();
         System.out.println("Switched back to Login Screen");
     }

    public static void main(String[] args) {
        // Ensure Swing components are created on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            new RefactoredGame();
        });

        // Add shutdown hook to close database connection gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown hook triggered.");
            DatabaseManager.closeConnection();
        }));
    }
}