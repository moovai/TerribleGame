package com.terriblegame.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.mindrot.jbcrypt.BCrypt;
import com.terriblegame.config.AppConfig;

/**
 * Encapsulates all database interactions for the game, including user authentication
 * and high score management. Implements security best practices like using
 * parameterized queries (PreparedStatements) to prevent SQL injection and
 * BCrypt for password hashing. Manages database connections using try-with-resources
 * for automatic closure.
 */
public class DatabaseManager {
    private final String dbUrl;
    private final String jdbcDriver;

    /**
     * Static initializer to load the configured JDBC driver once when the class is loaded.
     * Throws a RuntimeException if the driver cannot be found, as the application cannot function without it.
     */
    static {
        try {
            Class.forName(AppConfig.JDBC_DRIVER);
            System.out.println("JDBC Driver loaded: " + AppConfig.JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
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
        this.jdbcDriver = AppConfig.JDBC_DRIVER;
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
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                " username TEXT UNIQUE NOT NULL," +
                                " password_hash TEXT NOT NULL" +
                                ");";
        String createHighScoresTable = "CREATE TABLE IF NOT EXISTS high_scores (" +
                                    " id INTEGER PRIMARY KEY AUTOINCREMENT," +
                                    " username TEXT NOT NULL," +
                                    " score INTEGER NOT NULL," +
                                    " timestamp DATETIME DEFAULT CURRENT_TIMESTAMP" +
                                    ");";

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
        if (!isInputValid(username, plainTextPassword)) {
            System.err.println("Registration failed: Invalid username or password provided.");
            return false;
        }

        String checkUserSql = "SELECT id FROM users WHERE username = ?";
        String insertUserSql = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (Connection conn = getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement(checkUserSql)) {
                checkStmt.setString(1, username);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Registration failed: Username '" + username + "' already exists.");
                        return false;
                    }
                }
            }

            String hashedPassword = BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(AppConfig.BCRYPT_LOG_ROUNDS));

            try (PreparedStatement insertStmt = conn.prepareStatement(insertUserSql)) {
                insertStmt.setString(1, username);
                insertStmt.setString(2, hashedPassword);
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
        } catch (Exception e) {
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
     *         stored hash, {@code false} otherwise.
     */
    public boolean validateUser(String username, String plainTextPassword) {
        if (!isInputValid(username, plainTextPassword)) {
            System.err.println("Validation failed: Invalid username or password provided.");
            return false;
        }

        String querySql = "SELECT password_hash FROM users WHERE username = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(querySql)) {

            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    if (storedHash != null) {
                        try {
                            return BCrypt.checkpw(plainTextPassword, storedHash);
                        } catch (IllegalArgumentException ex) {
                            handleError("Invalid hash format encountered during validation for user: " + username, ex);
                            return false;
                        }
                    } else {
                        System.err.println("Warning: Retrieved null password hash for user: " + username);
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            handleError("Error validating user: " + username, e);
            return false;
        }
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
        if (username == null || username.trim().isEmpty() || score < 0) {
            System.err.println("Invalid data for saving score (User: " + username + ", Score: " + score + ")");
            return;
        }

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
     * Retrieves a list of the top high scores from the database, ordered from highest to lowest.
     * The number of scores retrieved is limited by {@link AppConfig#HIGH_SCORE_LIMIT}.
     * Uses a parameterized query (PreparedStatement) for the limit parameter.
     *
     * @return A {@link List} of strings, each formatted to display a ranked high score entry.
     */
    public List<String> getHighScores() {
        int limit = AppConfig.HIGH_SCORE_LIMIT;
        List<String> scores = new ArrayList<>();
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
                if (rank == 1) {
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
     * This method is deprecated as database connections are now managed automatically
     * within each method using try-with-resources, eliminating the need for manual closure
     * at the DatabaseManager level. Calling this method has no effect.
     *
     * @deprecated Connections are managed per-operation via try-with-resources.
     */
    @Deprecated
    public void closeConnection() {
        System.out.println("DatabaseManager.closeConnection() is deprecated. Connection management is automatic via try-with-resources.");
    }

    /**
     * Performs basic validation on username and password strings.
     * Checks if they are non-null and not empty/whitespace-only.
     *
     * @param username The username string to validate.
     * @param password The password string to validate.
     * @return {@code true} if both username and password are considered valid.
     */
    private boolean isInputValid(String username, String password) {
        return username != null && !username.trim().isEmpty() &&
               password != null && !password.isEmpty();
    }

    /**
     * Centralized handler for logging database and security-related errors.
     * Prints an error message to standard error, including the original exception message.
     *
     * @param message A descriptive message indicating the context of the error.
     * @param e       The exception that occurred.
     */
    private void handleError(String message, Exception e) {
        System.err.println("DATABASE/SECURITY ERROR: " + message + " - " + e.getMessage());
    }
}