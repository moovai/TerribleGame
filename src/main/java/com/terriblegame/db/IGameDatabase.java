package com.terriblegame.db;

import java.util.List;

/**
 * Defines the contract for game data persistence operations.
 * Provides user authentication, registration, and high score management.
 *
 * Security Requirements:
 * - Must implement secure password storage
 * - Must protect against SQL injection
 * - Must handle sensitive data appropriately
 *
 * Limitations:
 * - Assumes single database connection
 * - Not suitable for high-concurrency environments
 * - Requires proper database setup before use
 */
public interface IGameDatabase {

    /**
     * Registers a new user in the system with the provided credentials.
     *
     * Side Effects:
     * - Creates new user record in database
     * - Generates and stores password hash
     * - Logs registration attempt
     *
     * Security Considerations:
     * - Password must be hashed before storage
     * - Username must be unique in the system
     * - Input must be sanitized to prevent injection
     *
     * @param username The desired username for the new user
     * @param plainTextPassword The password in plain text (will be hashed)
     * @return true if registration successful, false if username exists or error occurs
     */
    boolean registerUser(String username, String plainTextPassword);

    /**
     * Validates user credentials against stored data.
     *
     * Side Effects:
     * - Logs authentication attempts
     * - No modifications to stored data
     *
     * Security Considerations:
     * - Must use secure password comparison
     * - Should implement rate limiting (implementation specific)
     * - Must not expose password hash information
     *
     * @param username The username to validate
     * @param plainTextPassword The password to validate
     * @return true if credentials are valid, false otherwise
     */
    boolean validateUser(String username, String plainTextPassword);

    /**
     * Saves a player's score to the high score table.
     *
     * Side Effects:
     * - Creates new score record in database
     * - Updates high score rankings
     * - Logs score submission
     *
     * Validation:
     * - Username must exist in the system
     * - Score must be non-negative
     * - Timestamp is automatically added
     *
     * @param username The username of the player
     * @param score The score to save
     */
    void saveScore(String username, int score);

    /**
     * Retrieves the list of high scores from the database.
     *
     * Side Effects:
     * - None, read-only operation
     * - May create database connection
     *
     * Performance Note:
     * - Results are typically limited to top N scores
     * - Order is from highest to lowest score
     *
     * @return List of formatted score entries, or error message if database unavailable
     */
    List<String> getHighScores();
}