// TerribleGame.java - A poorly coded game for educational purposes

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.sql.*;

public class TerribleGame extends JFrame implements KeyListener, ActionListener {

    public static final int W_WIDTH = 800;
    public static final int W_HEIGHT = 600;
    private static String currentLoggedInUser = null;
    private static Connection dbConn = null;
    private static final String DB_URL = "jdbc:sqlite:terrible_game_data.db";

    private GamePanel gamePanel;
    private javax.swing.Timer gameTimer;

    public int player_x_coord;
    public int player_y_coord;
    public int player_w = 30;
    public int player_h = 15;
    public int pSpeed = 5;

    public ArrayList<int[]> badThings;
    public ArrayList<int[]> goodThings;
    public ArrayList<int[]> bullets;

    public int score = 0;
    public int lives = 3;
    public boolean gameOver = false;
    public boolean gamePaused = false;
    public boolean gameRunning = false;

    private boolean keyLeft = false;
    private boolean keyRight = false;
    private boolean keyUp = false;
    private boolean keyDown = false;
    private boolean keyFire = false;

    private Random randomGenerator = new Random();

    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JPanel loginPanel;

    public TerribleGame() {
        super("The Terrible Space Game");

        setupDatabaseConnection();
        initializeDatabaseTables();

        setSize(W_WIDTH, W_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        loginPanel = new JPanel();
        loginPanel.setLayout(null);
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

        JTextArea highScoreArea = new JTextArea();
        highScoreArea.setBounds(50, 350, W_WIDTH - 100, 200);
        highScoreArea.setEditable(false);
        highScoreArea.setForeground(Color.CYAN);
        highScoreArea.setBackground(Color.BLACK);
        highScoreArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        loginPanel.add(highScoreArea);
        displayHighScores(highScoreArea);

        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(W_WIDTH, W_HEIGHT));
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
            if (dbConn == null || dbConn.isClosed()) {
                dbConn = DriverManager.getConnection(DB_URL);
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
         if (dbConn == null) {
             System.err.println("Cannot initialize DB tables, connection is null.");
             return;
         }
        String createUserTableSql = "CREATE TABLE IF NOT EXISTS users (" +
                                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    " username TEXT UNIQUE NOT NULL," +
                                    " password TEXT NOT NULL" +
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
            e.printStackTrace();
        }
    }

    private boolean registerNewUser(String user, String pass) {
        if (user == null || user.trim().isEmpty() || pass == null || pass.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return false;
        }
        String sql = "INSERT INTO users (username, password) VALUES ('" + user + "', '" + pass + "')";

        try (Statement stmt = dbConn.createStatement()) {
            String checkSql = "SELECT id FROM users WHERE username = '" + user + "'";
            ResultSet rs = stmt.executeQuery(checkSql);
            if (rs.next()) {
                statusLabel.setText("Username already exists.");
                rs.close();
                return false;
            }
            rs.close();

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
    }

    private boolean checkLogin(String user, String pass) {
         if (user == null || user.trim().isEmpty() || pass == null || pass.isEmpty()) {
            statusLabel.setText("Username and password cannot be empty.");
            return false;
        }
        String sql = "SELECT password FROM users WHERE username = '" + user + "'";

        try (Statement stmt = dbConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                if (storedPassword.equals(pass)) {
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

    private void savePlayerScore(String username, int finalScore) {
        if (username == null || finalScore <= 0) {
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
        } catch (NullPointerException e) {
            System.err.println("Database connection is likely null. Cannot save score.");
             e.printStackTrace();
        }
    }

     private void displayHighScores(JTextArea targetArea) {
        String sql = "SELECT username, score FROM high_scores ORDER BY score DESC LIMIT 10";
        StringBuilder scoreText = new StringBuilder("--- High Scores ---\n");

        try (Statement stmt = dbConn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int rank = 1;
            while (rs.next()) {
                String user = rs.getString("username");
                int scr = rs.getInt("score");
                scoreText.append(String.format("%d. %-15s : %d\n", rank++, user, scr));
            }
            if (rank == 1) {
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

    private void initializeGame() {
        player_x_coord = W_WIDTH / 2 - player_w / 2;
        player_y_coord = W_HEIGHT - 50 - player_h;
        score = 0;
        lives = 3;
        gameOver = false;
        gamePaused = false;
        gameRunning = true;

        badThings = new ArrayList<>();
        goodThings = new ArrayList<>();
        bullets = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            spawnBadThing();
        }
         for (int i = 0; i < 3; i++) {
            spawnGoodThing();
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

    private void updateGameLogic() {
        if (gameOver || gamePaused || !gameRunning) {
            return;
        }

        if (keyLeft) {
            player_x_coord -= pSpeed;
        }
        if (keyRight) {
            player_x_coord += pSpeed;
        }
         if (keyUp) {
            player_y_coord -= pSpeed;
        }
        if (keyDown) {
            player_y_coord += pSpeed;
        }

        if (player_x_coord < 0) {
            player_x_coord = 0;
        }
        if (player_x_coord > W_WIDTH - player_w) {
            player_x_coord = W_WIDTH - player_w;
        }
         if (player_y_coord < 0) {
            player_y_coord = 0;
        }
        if (player_y_coord > W_HEIGHT - player_h - 30) {
            player_y_coord = W_HEIGHT - player_h - 30;
        }

        if (keyFire) {
              int bulletW = 5;
              int bulletH = 10;
              int bulletSpeed = 8;
              int[] newBullet = {player_x_coord + player_w / 2 - bulletW / 2, player_y_coord - bulletH, bulletW, bulletH, bulletSpeed};
              bullets.add(newBullet);
              keyFire = false;
        }

        ArrayList<int[]> bulletsToRemove = new ArrayList<>();
        for (int i = 0; i < bullets.size(); i++) {
            int[] b = bullets.get(i);
            b[1] -= b[4];
            if (b[1] + b[3] < 0) {
                bulletsToRemove.add(b);
            }
        }
        bullets.removeAll(bulletsToRemove);

        ArrayList<int[]> badThingsToRemove = new ArrayList<>();
        Rectangle playerRect = new Rectangle(player_x_coord, player_y_coord, player_w, player_h);

        for (int i = 0; i < badThings.size(); i++) {
            int[] thing = badThings.get(i);
            thing[1] += thing[4];

            Rectangle thingRect = new Rectangle(thing[0], thing[1], thing[2], thing[3]);
            if (playerRect.intersects(thingRect)) {
                lives--;
                badThingsToRemove.add(thing);
                if (lives <= 0) {
                    gameOver = true;
                    gameTimer.stop();
                     handleGameOver();
                }
                continue;
            }

              ArrayList<int[]> bulletsHit = new ArrayList<>();
              boolean thingHit = false;
              for (int[] bullet : bullets) {
                  Rectangle bulletRect = new Rectangle(bullet[0], bullet[1], bullet[2], bullet[3]);
                  if (bulletRect.intersects(thingRect)) {
                      badThingsToRemove.add(thing);
                      bulletsHit.add(bullet);
                      score += 10;
                      thingHit = true;
                      break;
                  }
              }
              bullets.removeAll(bulletsHit);

            if (!thingHit && thing[1] > W_HEIGHT) {
                badThingsToRemove.add(thing);
            }
        }
        badThings.removeAll(badThingsToRemove);

          ArrayList<int[]> goodThingsToRemove = new ArrayList<>();
         for (int i = 0; i < goodThings.size(); i++) {
             int[] thing = goodThings.get(i);
             thing[1] += thing[4];

             Rectangle thingRect = new Rectangle(thing[0], thing[1], thing[2], thing[3]);
             if (playerRect.intersects(thingRect)) {
                 score += 50;
                 goodThingsToRemove.add(thing);
             } else if (thing[1] > W_HEIGHT) {
                 goodThingsToRemove.add(thing);
             }
         }
         goodThings.removeAll(goodThingsToRemove);

        if (randomGenerator.nextInt(100) < 5) {
             if (badThings.size() < 15) {
                spawnBadThing();
             }
        }
         if (randomGenerator.nextInt(100) < 2) {
             if (goodThings.size() < 5) {
                 spawnGoodThing();
             }
        }
    }

     private void spawnBadThing() {
         int width = 20 + randomGenerator.nextInt(30);
         int height = width;
         int x = randomGenerator.nextInt(W_WIDTH - width);
         int y = -height;
         int speed = 2 + randomGenerator.nextInt(3);
         badThings.add(new int[]{x, y, width, height, speed});
     }

     private void spawnGoodThing() {
         int width = 15;
         int height = 15;
         int x = randomGenerator.nextInt(W_WIDTH - width);
         int y = -height;
         int speed = 3 + randomGenerator.nextInt(2);
         goodThings.add(new int[]{x, y, width, height, speed});
     }

      private void handleGameOver() {
        gameRunning = false;
        System.out.println("Game Over! Final Score: " + score);
        if (currentLoggedInUser != null) {
             savePlayerScore(currentLoggedInUser, score);
        } else {
            System.out.println("Score not saved - user not logged in.");
        }
     }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == gameTimer) {
            updateGameLogic();
            gamePanel.repaint();
        } else if (src == loginButton) {
            String user = userField.getText();
            String pass = new String(passField.getPassword());
            if (checkLogin(user, pass)) {
                statusLabel.setText("Login Successful!");
                currentLoggedInUser = user;
                initializeGame();
            } else {
                passField.setText("");
            }
        } else if (src == registerButton) {
              String user = userField.getText();
              String pass = new String(passField.getPassword());
              if (registerNewUser(user, pass)) {
                  userField.setText("");
                  passField.setText("");
              } else {
                   passField.setText("");
              }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) {
            keyLeft = true;
        }
        if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) {
            keyRight = true;
        }
        if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) {
            keyUp = true;
        }
        if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) {
            keyDown = true;
        }
         if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_CONTROL) {
            if (gameRunning && !gameOver && !gamePaused) {
                 keyFire = true;
            }
        }
        if (keyCode == KeyEvent.VK_P) {
             if (gameRunning && !gameOver) {
                 gamePaused = !gamePaused;
                 if (gamePaused) {
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
                 if (dbConn != null && !dbConn.isClosed()) {
                     dbConn.close();
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
    }

    private class GamePanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, W_WIDTH, W_HEIGHT);

            if (!gameRunning && !gameOver) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 20));
                g2d.drawString("Waiting to start...", W_WIDTH/2 - 100, W_HEIGHT/2);
                return;
            }

            g2d.setColor(Color.CYAN);
            g2d.fillRect(player_x_coord, player_y_coord, player_w, player_h);

            g2d.setColor(Color.YELLOW);
            try {
                ArrayList<int[]> currentBullets = new ArrayList<>(bullets);
                for (int[] b : currentBullets) {
                    g2d.fillRect(b[0], b[1], b[2], b[3]);
                }
            } catch (Exception e) { }

            g2d.setColor(Color.RED);
             try {
                 ArrayList<int[]> currentBadThings = new ArrayList<>(badThings);
                 for (int[] thing : currentBadThings) {
                     g2d.fillRect(thing[0], thing[1], thing[2], thing[3]);
                 }
            } catch (Exception e) { }

            g2d.setColor(Color.GREEN);
             try {
                 ArrayList<int[]> currentGoodThings = new ArrayList<>(goodThings);
                for (int[] thing : currentGoodThings) {
                    g2d.fillOval(thing[0], thing[1], thing[2], thing[3]);
                }
            } catch (Exception e) { }

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Consolas", Font.BOLD, 16));

            g2d.drawString("Score: " + score, 10, 20);

            g2d.drawString("Lives: " + lives, W_WIDTH - 100, 20);

            if (gameOver) {
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                String msg = "GAME OVER";
                FontMetrics fm = g2d.getFontMetrics();
                int msgWidth = fm.stringWidth(msg);
                g2d.drawString(msg, (W_WIDTH - msgWidth) / 2, W_HEIGHT / 2 - 50);

                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                String scoreMsg = "Final Score: " + score;
                int scoreMsgWidth = fm.stringWidth(scoreMsg);
                fm = g2d.getFontMetrics();
                scoreMsgWidth = fm.stringWidth(scoreMsg);
                g2d.drawString(scoreMsg, (W_WIDTH - scoreMsgWidth) / 2, W_HEIGHT / 2);

                g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                String restartMsg = "(Press ESC to exit)";
                int restartMsgWidth = fm.stringWidth(restartMsg);
                 fm = g2d.getFontMetrics();
                 restartMsgWidth = fm.stringWidth(restartMsg);
                g2d.drawString(restartMsg, (W_WIDTH - restartMsgWidth) / 2, W_HEIGHT / 2 + 50);
            }

             if (gamePaused && !gameOver) {
                 g2d.setColor(new Color(0, 0, 0, 150));
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
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TerribleGame theGame = new TerribleGame();
            }
        });

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

}