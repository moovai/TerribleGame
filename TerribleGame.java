// TerribleGame.java - A poorly coded game for educational purposes (refactored)

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.sql.*;

public class TerribleGame extends JFrame implements KeyListener, ActionListener {

    public static final int GAME_WIDTH = 800;
    public static final int GAME_HEIGHT = 600;
    private static String currentUsername = null;
    private static Connection databaseConnection = null;
    private static final String DATABASE_URL = "jdbc:sqlite:terrible_game_data.db";

    private GamePanel gamePanel;
    private javax.swing.Timer gameTimer;

    public int playerX;
    public int playerY;
    public int playerWidth = 30;
    public int playerHeight = 15;
    public int playerSpeed = 5;

    public ArrayList<int[]> enemyList;
    public ArrayList<int[]> powerUpList;
    public ArrayList<int[]> bulletList;

    public int score = 0;
    public int lives = 3;
    public boolean gameOver = false;
    public boolean paused = false;
    public boolean gameRunning = false;

    private boolean moveLeft = false;
    private boolean moveRight = false;
    private boolean moveUp = false;
    private boolean moveDown = false;
    private boolean fireTriggered = false;

    private Random randomGenerator = new Random();

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JPanel loginPanel;

    public TerribleGame() {
        super("The Terrible Space Game");

        setupDatabaseConnection();
        initializeDatabaseTables();

        setSize(GAME_WIDTH, GAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        loginPanel = new JPanel();
        loginPanel.setLayout(null);
        loginPanel.setBackground(Color.DARK_GRAY);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setBounds(300, 150, 80, 25);
        usernameLabel.setForeground(Color.WHITE);
        loginPanel.add(usernameLabel);

        usernameField = new JTextField();
        usernameField.setBounds(400, 150, 160, 25);
        loginPanel.add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(300, 190, 80, 25);
        passwordLabel.setForeground(Color.WHITE);
        loginPanel.add(passwordLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(400, 190, 160, 25);
        loginPanel.add(passwordField);

        loginButton = new JButton("Login");
        loginButton.setBounds(300, 230, 120, 30);
        loginButton.addActionListener(this);
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

        JTextArea highScoreTextArea = new JTextArea();
        highScoreTextArea.setBounds(50, 350, GAME_WIDTH - 100, 200);
        highScoreTextArea.setEditable(false);
        highScoreTextArea.setForeground(Color.CYAN);
        highScoreTextArea.setBackground(Color.BLACK);
        highScoreTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        loginPanel.add(highScoreTextArea);
        displayHighScores(highScoreTextArea);

        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        gamePanel.setBackground(Color.BLACK);
        gamePanel.setVisible(false);

        add(loginPanel, BorderLayout.CENTER);

        addKeyListener(this);
        setFocusable(true);
        requestFocusInWindow();

        setLocationRelativeTo(null);
        setVisible(true);

        gameTimer = new javax.swing.Timer(16, this);
    }

    private void setupDatabaseConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            if (databaseConnection == null || databaseConnection.isClosed()) {
                databaseConnection = DriverManager.getConnection(DATABASE_URL);
                System.out.println("Database connection established.");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found! Make sure the JAR is in the classpath.");
            JOptionPane.showMessageDialog(this, "Critical Error: SQLite JDBC Driver not found.\nPlease check application setup.", "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Could not connect to the database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeDatabaseTables() {
        if (databaseConnection == null) {
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

        try (Statement statement = databaseConnection.createStatement()) {
            statement.execute(createUsersTable);
            statement.execute(createHighScoresTable);
            System.out.println("Database tables checked/created.");
        } catch (SQLException e) {
            System.err.println("Error creating database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean registerUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return false;
        }
        String insertUserSql = "INSERT INTO users (username, password) VALUES ('" + username + "', '" + password + "')";
        try (Statement statement = databaseConnection.createStatement()) {
            String checkUserSql = "SELECT id FROM users WHERE username = '" + username + "'";
            ResultSet resultSet = statement.executeQuery(checkUserSql);
            if (resultSet.next()) {
                statusLabel.setText("Username already exists.");
                resultSet.close();
                return false;
            }
            resultSet.close();

            int result = statement.executeUpdate(insertUserSql);
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
    }

    private boolean loginUser(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return false;
        }
        String querySql = "SELECT password FROM users WHERE username = '" + username + "'";
        try (Statement statement = databaseConnection.createStatement();
             ResultSet resultSet = statement.executeQuery(querySql)) {

            if (resultSet.next()) {
                String storedPassword = resultSet.getString("password");
                if (storedPassword.equals(password)) {
                    return true;
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
    }

    private void saveScore(String username, int score) {
        if (username == null || score <= 0) {
            System.err.println("Invalid data for saving score (User: " + username + ", Score: " + score + ")");
            return;
        }
        String insertScoreSql = "INSERT INTO high_scores (username, score) VALUES (?, ?)";
        try (PreparedStatement preparedStatement = databaseConnection.prepareStatement(insertScoreSql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setInt(2, score);
            preparedStatement.executeUpdate();
            System.out.println("Score " + score + " for user " + username + " saved.");
        } catch (SQLException e) {
            System.err.println("Error saving score: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.err.println("Database connection is likely null. Cannot save score.");
            e.printStackTrace();
        }
    }

    private void displayHighScores(JTextArea highScoreTextArea) {
        String queryHighScores = "SELECT username, score FROM high_scores ORDER BY score DESC LIMIT 10";
        StringBuilder highScoreText = new StringBuilder("--- High Scores ---\n");

        try (Statement statement = databaseConnection.createStatement();
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
        } catch (NullPointerException e) {
            System.err.println("Database connection is likely null. Cannot fetch scores.");
            highScoreText.append("Database connection error.\n");
            e.printStackTrace();
        }
        highScoreTextArea.setText(highScoreText.toString());
    }

    private void initializeGame() {
        playerX = GAME_WIDTH / 2 - playerWidth / 2;
        playerY = GAME_HEIGHT - 50 - playerHeight;
        score = 0;
        lives = 3;
        gameOver = false;
        paused = false;
        gameRunning = true;

        enemyList = new ArrayList<>();
        powerUpList = new ArrayList<>();
        bulletList = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            spawnEnemy();
        }
        for (int i = 0; i < 3; i++) {
            spawnPowerUp();
        }

        remove(loginPanel);
        add(gamePanel, BorderLayout.CENTER);
        gamePanel.setVisible(true);
        revalidate();
        repaint();

        gamePanel.requestFocusInWindow();
        requestFocus();

        gameTimer.start();
    }

    private void updateGame() {
        if (gameOver || paused || !gameRunning) {
            return;
        }

        if (moveLeft) {
            playerX -= playerSpeed;
        }
        if (moveRight) {
            playerX += playerSpeed;
        }
        if (moveUp) {
            playerY -= playerSpeed;
        }
        if (moveDown) {
            playerY += playerSpeed;
        }

        if (playerX < 0) {
            playerX = 0;
        }
        if (playerX > GAME_WIDTH - playerWidth) {
            playerX = GAME_WIDTH - playerWidth;
        }
        if (playerY < 0) {
            playerY = 0;
        }
        if (playerY > GAME_HEIGHT - playerHeight - 30) {
            playerY = GAME_HEIGHT - playerHeight - 30;
        }

        if (fireTriggered) {
            int bulletWidth = 5;
            int bulletHeight = 10;
            int bulletSpeed = 8;
            int[] newBullet = {playerX + playerWidth / 2 - bulletWidth / 2, playerY - bulletHeight, bulletWidth, bulletHeight, bulletSpeed};
            bulletList.add(newBullet);
            fireTriggered = false;
        }

        ArrayList<int[]> bulletsToRemove = new ArrayList<>();
        for (int[] bullet : bulletList) {
            bullet[1] -= bullet[4];
            if (bullet[1] + bullet[3] < 0) {
                bulletsToRemove.add(bullet);
            }
        }
        bulletList.removeAll(bulletsToRemove);

        ArrayList<int[]> enemiesToRemove = new ArrayList<>();
        Rectangle playerRect = new Rectangle(playerX, playerY, playerWidth, playerHeight);

        for (int[] enemy : enemyList) {
            enemy[1] += enemy[4];

            Rectangle enemyRect = new Rectangle(enemy[0], enemy[1], enemy[2], enemy[3]);
            if (playerRect.intersects(enemyRect)) {
                lives--;
                enemiesToRemove.add(enemy);
                if (lives <= 0) {
                    gameOver = true;
                    gameTimer.stop();
                    endGame();
                }
                continue;
            }

            ArrayList<int[]> collidedBullets = new ArrayList<>();
            boolean bulletHit = false;
            for (int[] bullet : bulletList) {
                Rectangle bulletRect = new Rectangle(bullet[0], bullet[1], bullet[2], bullet[3]);
                if (bulletRect.intersects(enemyRect)) {
                    enemiesToRemove.add(enemy);
                    collidedBullets.add(bullet);
                    score += 10;
                    bulletHit = true;
                    break;
                }
            }
            bulletList.removeAll(collidedBullets);

            if (!bulletHit && enemy[1] > GAME_HEIGHT) {
                enemiesToRemove.add(enemy);
            }
        }
        enemyList.removeAll(enemiesToRemove);

        ArrayList<int[]> powerUpsToRemove = new ArrayList<>();
        for (int[] powerUp : powerUpList) {
            powerUp[1] += powerUp[4];

            Rectangle powerUpRect = new Rectangle(powerUp[0], powerUp[1], powerUp[2], powerUp[3]);
            if (playerRect.intersects(powerUpRect)) {
                score += 50;
                powerUpsToRemove.add(powerUp);
            } else if (powerUp[1] > GAME_HEIGHT) {
                powerUpsToRemove.add(powerUp);
            }
        }
        powerUpList.removeAll(powerUpsToRemove);

        if (randomGenerator.nextInt(100) < 5) {
            if (enemyList.size() < 15) {
                spawnEnemy();
            }
        }
        if (randomGenerator.nextInt(100) < 2) {
            if (powerUpList.size() < 5) {
                spawnPowerUp();
            }
        }
    }

    private void spawnEnemy() {
        int size = 20 + randomGenerator.nextInt(30);
        int enemyWidth = size;
        int enemyHeight = size;
        int enemyX = randomGenerator.nextInt(GAME_WIDTH - enemyWidth);
        int enemyY = -enemyHeight;
        int enemySpeed = 2 + randomGenerator.nextInt(3);
        enemyList.add(new int[]{enemyX, enemyY, enemyWidth, enemyHeight, enemySpeed});
    }

    private void spawnPowerUp() {
        int powerUpWidth = 15;
        int powerUpHeight = 15;
        int powerUpX = randomGenerator.nextInt(GAME_WIDTH - powerUpWidth);
        int powerUpY = -powerUpHeight;
        int powerUpSpeed = 3 + randomGenerator.nextInt(2);
        powerUpList.add(new int[]{powerUpX, powerUpY, powerUpWidth, powerUpHeight, powerUpSpeed});
    }

    private void endGame() {
        gameRunning = false;
        System.out.println("Game Over! Final Score: " + score);
        if (currentUsername != null) {
            saveScore(currentUsername, score);
        } else {
            System.out.println("Score not saved - user not logged in.");
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == gameTimer) {
            updateGame();
            gamePanel.repaint();
        } else if (source == loginButton) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (loginUser(username, password)) {
                statusLabel.setText("Login Successful!");
                currentUsername = username;
                initializeGame();
            } else {
                passwordField.setText("");
            }
        } else if (source == registerButton) {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (registerUser(username, password)) {
                usernameField.setText("");
                passwordField.setText("");
            } else {
                passwordField.setText("");
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // No implementation needed for keyTyped
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            moveLeft = true;
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            moveRight = true;
        }
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            moveUp = true;
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            moveDown = true;
        }
        if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_CONTROL) {
            if (gameRunning && !gameOver && !paused) {
                fireTriggered = true;
            }
        }
        if (keyCode == KeyEvent.VK_P) {
            if (gameRunning && !gameOver) {
                paused = !paused;
                if (paused) {
                    gameTimer.stop();
                } else {
                    gameTimer.start();
                }
                gamePanel.repaint();
            }
        }
        if (keyCode == KeyEvent.VK_ESCAPE) {
            System.out.println("Escape pressed, exiting.");
            try {
                if (databaseConnection != null && !databaseConnection.isClosed()) {
                    databaseConnection.close();
                    System.out.println("Database connection closed.");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.exit(0);
        }
        if (gameOver && keyCode == KeyEvent.VK_R) {
            System.out.println("Restart requested (Not implemented fully - requires relaunch or better state management)");
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            moveLeft = false;
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            moveRight = false;
        }
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            moveUp = false;
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            moveDown = false;
        }
    }

    private class GamePanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D graphics2D = (Graphics2D) g;
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            graphics2D.setColor(Color.BLACK);
            graphics2D.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

            if (!gameRunning && !gameOver) {
                graphics2D.setColor(Color.WHITE);
                graphics2D.setFont(new Font("Arial", Font.BOLD, 20));
                graphics2D.drawString("Waiting to start...", GAME_WIDTH / 2 - 100, GAME_HEIGHT / 2);
                return;
            }

            graphics2D.setColor(Color.CYAN);
            graphics2D.fillRect(playerX, playerY, playerWidth, playerHeight);

            graphics2D.setColor(Color.YELLOW);
            try {
                ArrayList<int[]> bulletsCopy = new ArrayList<>(bulletList);
                for (int[] bullet : bulletsCopy) {
                    graphics2D.fillRect(bullet[0], bullet[1], bullet[2], bullet[3]);
                }
            } catch (Exception e) { }

            graphics2D.setColor(Color.RED);
            try {
                ArrayList<int[]> enemiesCopy = new ArrayList<>(enemyList);
                for (int[] enemy : enemiesCopy) {
                    graphics2D.fillRect(enemy[0], enemy[1], enemy[2], enemy[3]);
                }
            } catch (Exception e) { }

            graphics2D.setColor(Color.GREEN);
            try {
                ArrayList<int[]> powerUpsCopy = new ArrayList<>(powerUpList);
                for (int[] powerUp : powerUpsCopy) {
                    graphics2D.fillOval(powerUp[0], powerUp[1], powerUp[2], powerUp[3]);
                }
            } catch (Exception e) { }

            graphics2D.setColor(Color.WHITE);
            graphics2D.setFont(new Font("Consolas", Font.BOLD, 16));
            graphics2D.drawString("Score: " + score, 10, 20);
            graphics2D.drawString("Lives: " + lives, GAME_WIDTH - 100, 20);

            if (gameOver) {
                graphics2D.setColor(Color.YELLOW);
                graphics2D.setFont(new Font("Arial", Font.BOLD, 48));
                String gameOverMessage = "GAME OVER";
                FontMetrics metrics = graphics2D.getFontMetrics();
                int messageWidth = metrics.stringWidth(gameOverMessage);
                graphics2D.drawString(gameOverMessage, (GAME_WIDTH - messageWidth) / 2, GAME_HEIGHT / 2 - 50);

                graphics2D.setFont(new Font("Arial", Font.BOLD, 24));
                String finalScoreMessage = "Final Score: " + score;
                int scoreMessageWidth = graphics2D.getFontMetrics().stringWidth(finalScoreMessage);
                graphics2D.drawString(finalScoreMessage, (GAME_WIDTH - scoreMessageWidth) / 2, GAME_HEIGHT / 2);

                graphics2D.setFont(new Font("Arial", Font.PLAIN, 16));
                String exitMessage = "(Press ESC to exit)";
                int exitMessageWidth = graphics2D.getFontMetrics().stringWidth(exitMessage);
                graphics2D.drawString(exitMessage, (GAME_WIDTH - exitMessageWidth) / 2, GAME_HEIGHT / 2 + 50);
            }

            if (paused && !gameOver) {
                graphics2D.setColor(new Color(0, 0, 0, 150));
                graphics2D.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

                graphics2D.setColor(Color.WHITE);
                graphics2D.setFont(new Font("Arial", Font.BOLD, 48));
                String pauseMessage = "PAUSED";
                FontMetrics metrics = graphics2D.getFontMetrics();
                int pauseMessageWidth = metrics.stringWidth(pauseMessage);
                graphics2D.drawString(pauseMessage, (GAME_WIDTH - pauseMessageWidth) / 2, GAME_HEIGHT / 2);

                graphics2D.setFont(new Font("Arial", Font.PLAIN, 16));
                String resumeMessage = "(Press 'P' to resume)";
                int resumeMessageWidth = graphics2D.getFontMetrics().stringWidth(resumeMessage);
                graphics2D.drawString(resumeMessage, (GAME_WIDTH - resumeMessageWidth) / 2, GAME_HEIGHT / 2 + 50);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TerribleGame terribleGame = new TerribleGame();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (databaseConnection != null && !databaseConnection.isClosed()) {
                    System.out.println("Shutdown hook closing database connection.");
                    databaseConnection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing database connection during shutdown: " + e.getMessage());
                e.printStackTrace();
            }
        }));
    }

}