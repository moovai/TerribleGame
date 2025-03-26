// TerribleGame.java - A poorly coded game for educational purposes
// WARNING: Contains deliberate bad practices and security vulnerabilities!

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.sql.*; // Import for JDBC

// Main class doing EVERYTHING - JFrame, game logic, db access... terrible!
public class TerribleGame extends JFrame implements KeyListener, ActionListener {

    // ---=== Global Static State Mess ===---
    // Often using static is bad, especially for instance-specific things
    public static final int W_WIDTH = 800; // Window width - magic numbers sometimes constant-ized poorly
    public static final int W_HEIGHT = 600; // Window height
    private static String currentLoggedInUser = null; // Track logged in user globally
    private static Connection dbConn = null; // Global DB connection - bad practice!
    private static final String DB_URL = "jdbc:sqlite:terrible_game_data.db"; // DB file name

    // ---=== Instance Variables (Many should be encapsulated better) ===---
    private GamePanel gamePanel; // The drawing canvas
    private javax.swing.Timer gameTimer; // Swing Timer for game loop

    // Game state variables - poor naming, public access maybe?
    public int player_x_coord; // player x
    public int player_y_coord; // player y
    public int player_w = 30;
    public int player_h = 15;
    public int pSpeed = 5; // player speed

    public ArrayList<int[]> badThings; // List of enemies/obstacles - using int[] is obscure
    public ArrayList<int[]> goodThings; // List of collectibles
    public ArrayList<int[]> bullets; // Player bullets

    public int score = 0;
    public int lives = 3;
    public boolean gameOver = false;
    public boolean gamePaused = false;
    public boolean gameRunning = false; // Is the game itself active? vs menu/login

    private boolean keyLeft = false;
    private boolean keyRight = false;
    private boolean keyUp = false; // Added movement
    private boolean keyDown = false; // Added movement
    private boolean keyFire = false; // Firing key

    private Random randomGenerator = new Random(); // Random number generator

    // UI Components - Should be in separate classes
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JPanel loginPanel; // Panel for login screen stuff

    // Constructor - Does way too much setup
    public TerribleGame() {
        super("The Terrible Space Game"); // Set window title

        // --- Database Initialization ---
        // Doing DB setup in constructor is questionable
        setupDatabaseConnection();
        initializeDatabaseTables(); // Idempotent table creation

        // --- UI Setup ---
        setSize(W_WIDTH, W_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false); // Keep it simple

        // Login Screen Setup - Mixing UI concerns directly
        loginPanel = new JPanel();
        loginPanel.setLayout(null); // Using null layout - generally bad practice!
        loginPanel.setBackground(Color.DARK_GRAY);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(300, 150, 80, 25);
        userLabel.setForeground(Color.WHITE);
        loginPanel.add(userLabel);

        userField = new JTextField();
        userField.setBounds(400, 150, 160, 25);
        loginPanel.add(userField);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setBounds(300, 190, 80, 25);
        passLabel.setForeground(Color.WHITE);
        loginPanel.add(passLabel);

        passField = new JPasswordField();
        passField.setBounds(400, 190, 160, 25);
        loginPanel.add(passField);

        loginButton = new JButton("Login");
        loginButton.setBounds(300, 230, 120, 30);
        loginButton.addActionListener(this); // Using 'this' as ActionListener for everything
        loginPanel.add(loginButton);

        registerButton = new JButton("Register");
        registerButton.setBounds(440, 230, 120, 30);
        registerButton.addActionListener(this);
        loginPanel.add(registerButton);

        statusLabel = new JLabel("");
        statusLabel.setBounds(300, 270, 260, 25);
        statusLabel.setForeground(Color.RED);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loginPanel.add(statusLabel);

        // High Score Display on Login Screen
        JTextArea highScoreArea = new JTextArea();
        highScoreArea.setBounds(50, 350, W_WIDTH - 100, 200);
        highScoreArea.setEditable(false);
        highScoreArea.setForeground(Color.CYAN);
        highScoreArea.setBackground(Color.BLACK);
        highScoreArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        loginPanel.add(highScoreArea);
        displayHighScores(highScoreArea); // Method to fetch and display scores

        // Game Panel Setup
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(W_WIDTH, W_HEIGHT));
        gamePanel.setBackground(Color.BLACK);
        gamePanel.setVisible(false); // Start hidden

        // Add panels to frame - Only show login initially
        add(loginPanel, BorderLayout.CENTER);
        // We will add gamePanel later when login is successful

        addKeyListener(this); // The JFrame listens to keys - sometimes better on the panel
        setFocusable(true); // Need this for KeyListener on JFrame
        requestFocusInWindow(); // Try to grab focus

        setLocationRelativeTo(null); // Center window
        setVisible(true); // Make window visible

        // Game Timer setup - but don't start it yet
        gameTimer = new javax.swing.Timer(16, this); // ~60 FPS target, 'this' is the ActionListener
    }

    // ---=== Database Methods (Poorly placed, potential issues) ===---

    private void setupDatabaseConnection() {
        try {
            // Ensure the JDBC driver is loaded (needed for older Java versions or specific setups)
            Class.forName("org.sqlite.JDBC");
            if (dbConn == null || dbConn.isClosed()) {
                dbConn = DriverManager.getConnection(DB_URL);
                System.out.println("Database connection established."); // Console logging isn't great
            }
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found! Make sure the JAR is in the classpath.");
            // In a real app, handle this much more gracefully (e.g., disable DB features, show error dialog)
            JOptionPane.showMessageDialog(this, "Critical Error: SQLite JDBC Driver not found.\nPlease check application setup.", "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1); // Harsh exit
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace(); // Leaking stack trace to console
            // Maybe show an error to the user
             JOptionPane.showMessageDialog(this, "Could not connect to the database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
             // Might want to disable DB functionality or exit
        }
    }

    private void initializeDatabaseTables() {
        // Using Statement, susceptible to issues if not careful, but okay for CREATE TABLE
        // Not checking if dbConn is valid here - potential NullPointerException
         if (dbConn == null) {
             System.err.println("Cannot initialize DB tables, connection is null.");
             return;
         }
        String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    " username TEXT UNIQUE NOT NULL," +
                                    " password TEXT NOT NULL" + // Storing plain text passwords! BAD!
                                    ");";
        String createScoresTableSql = "CREATE TABLE IF NOT EXISTS high_scores (" +
                                      " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                      " username TEXT NOT NULL," +
                                      " score INTEGER NOT NULL," +
                                      " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                                      ");";

        try (Statement stmt = dbConn.createStatement()) {
            stmt.execute(createUserTableSql);
            stmt.execute(createScoresTableSql);
            System.out.println("Database tables checked/created.");
        } catch (SQLException e) {
            System.err.println("Error creating database tables: " + e.getMessage());
            e.printStackTrace(); // Again, stack trace leak
        }
    }

    // SECURITY VULNERABILITY: SQL Injection is possible here!
    private boolean registerNewUser(String user, String pass) {
        if (user == null || user.trim().isEmpty() || pass == null || pass.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return false;
        }
        // !! VULNERABLE !! - Concatenating user input directly into SQL query
        String sql = "INSERT INTO users (username, password) VALUES ('" + user + "', '" + pass + "')"; // BAD PRACTICE!

        try (Statement stmt = dbConn.createStatement()) {
            // Check if user already exists first (another query, could be inefficient)
            String checkSql = "SELECT id FROM users WHERE username = '" + user + "'"; // Also vulnerable!
            ResultSet rs = stmt.executeQuery(checkSql);
            if (rs.next()) {
                statusLabel.setText("Username already exists.");
                rs.close(); // Close ResultSet
                return false;
            }
            rs.close(); // Close ResultSet

            // Now insert the new user
            int result = stmt.executeUpdate(sql);
            if (result > 0) {
                statusLabel.setText("Registration successful!");
                return true;
            } else {
                statusLabel.setText("Registration failed (database error).");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Registration error: " + e.getMessage());
            statusLabel.setText("Registration failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        // Using Statement for inserts/updates is generally discouraged vs PreparedStatement
    }

    // SECURITY VULNERABILITY: SQL Injection & Timing Attack potential
    private boolean checkLogin(String user, String pass) {
         if (user == null || user.trim().isEmpty() || pass == null || pass.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return false;
        }
        // !! VULNERABLE !! - Concatenating user input directly into SQL query
        String sql = "SELECT password FROM users WHERE username = '" + user + "'"; // BAD PRACTICE!

        try (Statement stmt = dbConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                // Direct string comparison - minor timing attack potential, but main issue is plain text
                if (storedPassword.equals(pass)) {
                    return true; // Login successful
                } else {
                    statusLabel.setText("Incorrect password.");
                    return false;
                }
            } else {
                statusLabel.setText("Username not found.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Login error: " + e.getMessage());
            statusLabel.setText("Login failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        // Note: PreparedStatement should be used here to prevent SQL injection.
        // Passwords should be hashed and salted.
    }

    // Saving score - slightly better using PreparedStatement, but mixed in main class
    private void savePlayerScore(String username, int finalScore) {
        if (username == null || finalScore <= 0) { // Don't save zero scores
             System.err.println("Invalid data for saving score (User: " + username + ", Score: " + finalScore + ")");
             return;
        }
        String sql = "INSERT INTO high_scores (username, score) VALUES (?, ?)";

        try (PreparedStatement pstmt = dbConn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, finalScore);
            pstmt.executeUpdate();
            System.out.println("Score " + finalScore + " for user " + username + " saved.");
        } catch (SQLException e) {
            System.err.println("Error saving score: " + e.getMessage());
            e.printStackTrace();
            // Maybe notify user?
            // statusLabel.setText("Could not save high score."); // Need access to statusLabel here
        } catch (NullPointerException e) {
            System.err.println("Database connection is likely null. Cannot save score.");
             e.printStackTrace();
        }
    }

     // Fetching high scores - Also mixed in main class
    private void displayHighScores(JTextArea targetArea) {
        String sql = "SELECT username, score FROM high_scores ORDER BY score DESC LIMIT 10"; // Get top 10
        StringBuilder scoreText = new StringBuilder("--- High Scores ---\n");

        try (Statement stmt = dbConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int rank = 1;
            while (rs.next()) {
                String user = rs.getString("username");
                int scr = rs.getInt("score");
                // Formatting could be better
                scoreText.append(String.format("%d. %-15s : %d\n", rank++, user, scr));
            }
            if (rank == 1) { // No scores found
                 scoreText.append("No scores recorded yet.\n");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching high scores: " + e.getMessage());
            scoreText.append("Error loading scores.\n");
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Database connection is likely null. Cannot fetch scores.");
            scoreText.append("Database connection error.\n");
            e.printStackTrace();
        }
        targetArea.setText(scoreText.toString());
    }


    // ---=== Game Logic Methods (Long, tangled, inefficient) ===---

    private void initializeGame() {
        // Reset game state variables
        player_x_coord = W_WIDTH / 2 - player_w / 2;
        player_y_coord = W_HEIGHT - 50 - player_h; // Start near bottom
        score = 0;
        lives = 3;
        gameOver = false;
        gamePaused = false;
        gameRunning = true; // Mark game as active

        // Clear lists and re-initialize
        badThings = new ArrayList<>();
        goodThings = new ArrayList<>();
        bullets = new ArrayList<>();

        // Initial population of game objects (example)
        for (int i = 0; i < 5; i++) { // Magic number 5
            spawnBadThing();
        }
         for (int i = 0; i < 3; i++) { // Magic number 3
            spawnGoodThing();
        }

        // Switch UI from login panel to game panel
        remove(loginPanel); // Remove login UI
        add(gamePanel, BorderLayout.CENTER); // Add game panel
        gamePanel.setVisible(true); // Make it visible
        revalidate(); // Tell layout manager to recalculate
        repaint(); // Redraw the frame contents

        // Ensure game panel has focus for input
        gamePanel.requestFocusInWindow(); // Request focus for the panel, if it handles keys
        requestFocus(); // Also request focus for the frame (where listener is currently attached)

        // Start the game loop timer
        gameTimer.start();
    }

    // This method is HUGE and does too much! Update logic, collision, spawning...
    private void updateGameLogic() {
        if (gameOver || gamePaused || !gameRunning) {
            return; // Don't update if game is not in active play state
        }

        // --- Player Movement ---
        // Lots of checks, could be simplified
        if (keyLeft) {
            player_x_coord -= pSpeed;
        }
        if (keyRight) {
            player_x_coord += pSpeed;
        }
         if (keyUp) { // Added vertical movement
            player_y_coord -= pSpeed;
        }
        if (keyDown) {
            player_y_coord += pSpeed;
        }

        // Boundary checks - repetitive code
        if (player_x_coord < 0) {
            player_x_coord = 0;
        }
        if (player_x_coord > W_WIDTH - player_w) {
            player_x_coord = W_WIDTH - player_w;
        }
         if (player_y_coord < 0) { // Added vertical bounds
            player_y_coord = 0;
        }
        if (player_y_coord > W_HEIGHT - player_h - 30) { // Keep player above bottom slightly maybe
            player_y_coord = W_HEIGHT - player_h - 30;
        }

        // --- Bullet Firing ---
        // Should have a cooldown mechanism - currently allows rapid fire
        if (keyFire) {
             // Create a new bullet at player's position
             // Using int[]: {x, y, width, height, speed} - obscure!
             int bulletW = 5;
             int bulletH = 10;
             int bulletSpeed = 8;
             int[] newBullet = {player_x_coord + player_w / 2 - bulletW / 2, player_y_coord - bulletH, bulletW, bulletH, bulletSpeed};
             bullets.add(newBullet);
             keyFire = false; // Consume the fire key press - simple way to prevent continuous fire
        }

        // --- Update Bullets ---
        // Iterating and modifying list - requires care (use iterator or backwards loop)
        // This loop is okay, but could be an iterator
        ArrayList<int[]> bulletsToRemove = new ArrayList<>(); // Temp list to avoid ConcurrentModificationException
        for (int i = 0; i < bullets.size(); i++) {
            int[] b = bullets.get(i);
            b[1] -= b[4]; // Move bullet up (y decreases) using speed at index 4
            if (b[1] + b[3] < 0) { // If bullet goes off screen top
                bulletsToRemove.add(b);
            }
        }
        bullets.removeAll(bulletsToRemove); // Remove off-screen bullets

        // --- Update Bad Things (Enemies/Obstacles) ---
        // This structure is asking for ConcurrentModificationException if we remove directly
        ArrayList<int[]> badThingsToRemove = new ArrayList<>();
        Rectangle playerRect = new Rectangle(player_x_coord, player_y_coord, player_w, player_h); // Create player bounds once

        for (int i = 0; i < badThings.size(); i++) {
            int[] thing = badThings.get(i);
            thing[1] += thing[4]; // Move down using speed at index 4

            // Check collision with player
            // int[] format: {x, y, width, height, speed, type?} - needs comments! Let's assume 0=x, 1=y, 2=w, 3=h, 4=speed
            Rectangle thingRect = new Rectangle(thing[0], thing[1], thing[2], thing[3]);
            if (playerRect.intersects(thingRect)) {
                lives--; // Ouch!
                badThingsToRemove.add(thing); // Remove the thing that hit player
                // Add explosion effect? Sound? Score penalty? None of that here.
                if (lives <= 0) {
                    gameOver = true;
                    gameTimer.stop(); // Stop game loop immediately
                     handleGameOver(); // Call game over logic
                    // Maybe break loop?
                }
                continue; // Skip other checks for this item if hit player
            }

            // Check collision with Bullets
            // Nested loops - can be inefficient if many bullets/enemies
             ArrayList<int[]> bulletsHit = new ArrayList<>();
             boolean thingHit = false;
             for (int[] bullet : bullets) {
                 Rectangle bulletRect = new Rectangle(bullet[0], bullet[1], bullet[2], bullet[3]);
                 if (bulletRect.intersects(thingRect)) {
                     badThingsToRemove.add(thing); // Mark enemy for removal
                     bulletsHit.add(bullet);       // Mark bullet for removal
                     score += 10;                 // Increase score
                     thingHit = true;
                     // Add explosion? Sound?
                     break; // One bullet kills one enemy max
                 }
             }
             bullets.removeAll(bulletsHit); // Remove bullets that hit

             // If thing goes off screen bottom
            if (!thingHit && thing[1] > W_HEIGHT) {
                badThingsToRemove.add(thing);
                // Maybe penalize score for missing?
            }
        }
        badThings.removeAll(badThingsToRemove); // Remove things that were hit or went off screen

        // --- Update Good Things (Collectibles) ---
        // Similar logic to bad things, but adds score on collision
         ArrayList<int[]> goodThingsToRemove = new ArrayList<>();
        for (int i = 0; i < goodThings.size(); i++) {
            int[] thing = goodThings.get(i);
            thing[1] += thing[4]; // Move down

            Rectangle thingRect = new Rectangle(thing[0], thing[1], thing[2], thing[3]);
            if (playerRect.intersects(thingRect)) {
                score += 50; // More points for good things!
                goodThingsToRemove.add(thing);
                // Play sound? Effect?
            } else if (thing[1] > W_HEIGHT) { // Off screen bottom
                goodThingsToRemove.add(thing);
            }
        }
        goodThings.removeAll(goodThingsToRemove);


        // --- Spawning New Things ---
        // Random spawning based on probability - could be frame rate dependent!
        // Lots of magic numbers for probability and types
        if (randomGenerator.nextInt(100) < 5) { // ~5% chance per frame to spawn bad
             if (badThings.size() < 15) { // Limit max enemies
                spawnBadThing();
             }
        }
         if (randomGenerator.nextInt(100) < 2) { // ~2% chance per frame to spawn good
             if (goodThings.size() < 5) { // Limit max collectibles
                 spawnGoodThing();
             }
        }

        // Add complexity: maybe increase speed over time?
        // if (score > 1000 && score % 500 == 0) { /* increase speed vars? */ } // Example
    }

    // Spawning methods - contain magic numbers and rely on obscure int[] structure
     private void spawnBadThing() {
         int width = 20 + randomGenerator.nextInt(30); // Random size
         int height = width;
         int x = randomGenerator.nextInt(W_WIDTH - width);
         int y = -height; // Start off screen top
         int speed = 2 + randomGenerator.nextInt(3); // Random speed
         // int[] format: {x, y, width, height, speed}
         badThings.add(new int[]{x, y, width, height, speed});
     }

     private void spawnGoodThing() {
         int width = 15;
         int height = 15;
         int x = randomGenerator.nextInt(W_WIDTH - width);
         int y = -height;
         int speed = 3 + randomGenerator.nextInt(2);
          // int[] format: {x, y, width, height, speed} - same format, differentiate by list or add type later?
         goodThings.add(new int[]{x, y, width, height, speed});
     }

     // Game Over handling
     private void handleGameOver() {
        gameRunning = false;
        System.out.println("Game Over! Final Score: " + score); // Log to console
        if (currentLoggedInUser != null) {
             savePlayerScore(currentLoggedInUser, score);
             // Update high scores on login screen for next time?
             // We need access to the text area... this is messy.
             // Let's assume the user sees the high scores when they restart/relogin.
        } else {
            System.out.println("Score not saved - user not logged in.");
        }

        // Display Game Over message on screen (drawn in paintComponent)
        // Maybe show a "Restart" button? Adds complexity. For now, just stops.
        // We should probably switch back to the login/menu screen here.
        // This requires removing the game panel and adding the login panel again.

        // Delay and switch back to login screen? Or just show message on game panel.
        // Let's keep the game panel visible with the game over message for now.
     }

    // ---=== Event Handling (ActionListener for Timer and Buttons, KeyListener) ===---

    @Override
    public void actionPerformed(ActionEvent e) {
        // Ugly: Check source of event using instanceof or getSource()
        Object src = e.getSource();

        if (src == gameTimer) {
            // This is the game loop tick
            updateGameLogic();
            gamePanel.repaint(); // Trigger redraw
        } else if (src == loginButton) {
            // Handle Login Button Press
            String user = userField.getText();
            String pass = new String(passField.getPassword()); // Get password securely-ish from JPasswordField
            if (checkLogin(user, pass)) {
                statusLabel.setText("Login Successful!");
                currentLoggedInUser = user; // Store logged-in user
                // Start the game!
                initializeGame();
            } else {
                // Error message is set within checkLogin
                passField.setText(""); // Clear password field on failure
            }
        } else if (src == registerButton) {
            // Handle Register Button Press
             String user = userField.getText();
             String pass = new String(passField.getPassword());
             if (registerNewUser(user, pass)) {
                 // Optionally clear fields or show success message differently
                 userField.setText("");
                 passField.setText("");
             } else {
                  // Error message set within registerNewUser
                  passField.setText("");
             }
        }
        // Add more button handlers here if needed
    }

    // --- KeyListener Methods ---
    // These methods track key state - could be improved (e.g., using InputMaps)
    @Override
    public void keyTyped(KeyEvent e) {
        // Generally not used for game movement
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        // Using multiple if statements - a switch might be cleaner but this is "worse"
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            keyLeft = true;
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            keyRight = true;
        }
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) { // Added vertical
            keyUp = true;
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) { // Added vertical
            keyDown = true;
        }
         if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_CONTROL) { // Fire key
            if (gameRunning && !gameOver && !gamePaused) { // Only allow fire if game is active
                 keyFire = true; // Set flag to be consumed in update loop
            }
        }
        if (keyCode == KeyEvent.VK_P) { // Pause toggle
             if (gameRunning && !gameOver) {
                 gamePaused = !gamePaused;
                 if (gamePaused) {
                     gameTimer.stop();
                 } else {
                     gameTimer.start();
                 }
                 gamePanel.repaint(); // Redraw to show pause status
             }
         }
        if (keyCode == KeyEvent.VK_ESCAPE) { // Exit / Back to menu?
            // For now, just exit the application entirely - crude.
             System.out.println("Escape pressed, exiting.");
             // Clean up DB connection?
             try {
                 if (dbConn != null && !dbConn.isClosed()) {
                     dbConn.close();
                     System.out.println("Database connection closed.");
                 }
             } catch (SQLException ex) {
                 ex.printStackTrace();
             }
             System.exit(0);
        }
        // Add restart key?
        if (gameOver && keyCode == KeyEvent.VK_R) {
            // Restart the game? Need to re-initialize everything.
            // For simplicity, let's not implement restart now. User must relaunch.
             System.out.println("Restart requested (Not implemented fully - requires relaunch or better state management)");
             // Tentative restart - might have issues
             // remove(gamePanel);
             // add(loginPanel);
             // loginPanel.setVisible(true);
             // gamePanel.setVisible(false);
             // currentLoggedInUser = null; // Log out?
             // statusLabel.setText("Logged out. Restarted.");
             // displayHighScores((JTextArea)loginPanel.getComponent(6)); // Hacky way to get text area
             // revalidate();
             // repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            keyLeft = false;
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            keyRight = false;
        }
         if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            keyUp = false;
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            keyDown = false;
        }
         // Note: keyFire is typically consumed in update, not released here,
         // unless you want continuous fire while held down (which requires different logic).
    }

    // ---=== Inner Class for Drawing Panel ===---
    // Inner class has implicit reference to outer class instance, allowing access
    // to all its fields (even private ones) - convenient but can lead to tight coupling.
    private class GamePanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // Clears the panel

            // Improve rendering quality (optional, but good practice usually)
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background - could be more interesting (stars, etc.)
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, W_WIDTH, W_HEIGHT);

            if (!gameRunning && !gameOver) {
                // This state shouldn't normally be drawn if login panel is visible
                // But if something goes wrong, show a message
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("Waiting to start...", W_WIDTH/2 - 100, W_HEIGHT/2);
                return; // Don't draw game elements if not running
            }

            // --- Draw Game Elements ---
            // Lots of drawing code mixed here. Could be separate methods.

            // Draw Player
            g2d.setColor(Color.CYAN); // Player color
            g2d.fillRect(player_x_coord, player_y_coord, player_w, player_h);
            // Maybe add more detail to player? Triangle shape?

            // Draw Bullets
            g2d.setColor(Color.YELLOW);
             // Need to copy list if iterating and modifying elsewhere? No, drawing is usually safe.
             // But for consistency with update loop issues, let's iterate safely (copy or index)
            try { // Protect against potential concurrent modification if state changes during paint
                ArrayList<int[]> currentBullets = new ArrayList<>(bullets); // Shallow copy
                for (int[] b : currentBullets) {
                    // Assuming b = {x, y, w, h, ...}
                    g2d.fillRect(b[0], b[1], b[2], b[3]);
                }
            } catch (Exception e) { /* Ignore drawing error - bad practice */ }


            // Draw Bad Things
            g2d.setColor(Color.RED);
             try {
                 ArrayList<int[]> currentBadThings = new ArrayList<>(badThings);
                 for (int[] thing : currentBadThings) {
                     // Draw as simple squares/rects
                     g2d.fillRect(thing[0], thing[1], thing[2], thing[3]);
                     // Could vary color or shape based on a 'type' if added
                 }
            } catch (Exception e) { /* Ignore drawing error */ }

            // Draw Good Things
            g2d.setColor(Color.GREEN);
             try {
                 ArrayList<int[]> currentGoodThings = new ArrayList<>(goodThings);
                for (int[] thing : currentGoodThings) {
                    // Draw as circles maybe?
                    g2d.fillOval(thing[0], thing[1], thing[2], thing[3]);
                }
            } catch (Exception e) { /* Ignore drawing error */ }


            // --- Draw UI Overlays (Score, Lives, Game Over) ---
            // Poor placement of UI drawing logic inside game object drawing
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 16)); // Use monospaced?

            // Draw Score
            g2d.drawString("Score: " + score, 10, 20);

            // Draw Lives
            g2d.drawString("Lives: " + lives, W_WIDTH - 100, 20);

            // Draw Game Over Message
            if (gameOver) {
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                String msg = "GAME OVER";
                FontMetrics fm = g2d.getFontMetrics();
                int msgWidth = fm.stringWidth(msg);
                g2d.drawString(msg, (W_WIDTH - msgWidth) / 2, W_HEIGHT / 2 - 50);

                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                String scoreMsg = "Final Score: " + score;
                int scoreMsgWidth = fm.stringWidth(scoreMsg); // Re-use fm? No, get new one for new font
                fm = g2d.getFontMetrics(); // Get metrics for the current font
                scoreMsgWidth = fm.stringWidth(scoreMsg);
                g2d.drawString(scoreMsg, (W_WIDTH - scoreMsgWidth) / 2, W_HEIGHT / 2);

                // Add restart instruction?
                 g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                 String restartMsg = "(Press ESC to exit)"; // No easy restart implemented yet
                 int restartMsgWidth = fm.stringWidth(restartMsg);
                  fm = g2d.getFontMetrics();
                  restartMsgWidth = fm.stringWidth(restartMsg);
                 g2d.drawString(restartMsg, (W_WIDTH - restartMsgWidth) / 2, W_HEIGHT / 2 + 50);
            }

             // Draw Paused Message
             if (gamePaused && !gameOver) {
                 g2d.setColor(new Color(0, 0, 0, 150)); // Semi-transparent overlay
                 g2d.fillRect(0, 0, W_WIDTH, W_HEIGHT);

                 g2d.setColor(Color.WHITE);
                 g2d.setFont(new Font("Arial", Font.BOLD, 48));
                 String msg = "PAUSED";
                 FontMetrics fm = g2d.getFontMetrics();
                 int msgWidth = fm.stringWidth(msg);
                 g2d.drawString(msg, (W_WIDTH - msgWidth) / 2, W_HEIGHT / 2);

                 g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                 String resumeMsg = "(Press 'P' to resume)";
                 int resumeMsgWidth = fm.stringWidth(resumeMsg);
                 fm = g2d.getFontMetrics();
                 resumeMsgWidth = fm.stringWidth(resumeMsg);
                 g2d.drawString(resumeMsg, (W_WIDTH - resumeMsgWidth) / 2, W_HEIGHT / 2 + 50);
             }
        }
    } // End of inner GamePanel class

    // ---=== Main Method ===---
    public static void main(String[] args) {
        // Entry point of the application

        // Check for SQLite Driver presence early? Maybe not, let constructor handle it.

        // Ensure GUI operations run on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Create the main game window instance
                TerribleGame theGame = new TerribleGame();
                // The constructor already makes it visible
            }
        });

         // Add a shutdown hook to close DB connection cleanly (good practice, but still global conn)
         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (dbConn != null && !dbConn.isClosed()) {
                    System.out.println("Shutdown hook closing database connection.");
                    dbConn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }

    // Add more ridiculously complex or poorly implemented features here to reach 3000 lines:
    // - Multiple enemy types with different behaviors (e.g., sine wave movement, shooting back)
    // - Power-ups (shield, rapid fire, score multiplier) with complex collision/duration logic
    // - Different levels with increasing difficulty (more enemies, faster speeds)
    // - Boss fights at end of levels
    // - More elaborate drawing (simple sprites instead of rectangles, background scrolling)
    // - Unnecessary calculations in loops
    // - More UI screens (options, credits) implemented directly in this file
    // - Convoluted state management using flags and complex conditional chains
    // - Adding sound effects using Clip/AudioSystem (adds complexity and potential issues)

} // End of TerribleGame class