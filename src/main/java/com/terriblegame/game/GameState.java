package com.terriblegame.game;

import java.util.ArrayList;
import java.util.List;
import com.terriblegame.config.AppConfig;
import com.terriblegame.model.*;

/**
 * Encapsulates the dynamic state of the game, including player status,
 * lists of active game objects (enemies, bullets, power-ups), score, lives,
 * and game status flags (running, paused, game over).
 * Provides controlled methods for accessing and modifying this state.
 */
public class GameState {
    private Player player;
    private final List<Enemy> enemies;
    private final List<PowerUp> powerUps;
    private final List<Bullet> bullets;
    private int score;
    private int lives;
    private boolean isGameOver;
    private boolean isPaused;
    private boolean isRunning;

    /**
     * Constructs a new GameState object, initializing all game elements to their default starting values.
     * Creates empty lists for dynamic objects and sets initial score, lives, and status flags.
     */
    public GameState() {
        // Player is initialized but positioned during reset/start.
        this.player = new Player(0, 0);
        this.enemies = new ArrayList<>();
        this.powerUps = new ArrayList<>();
        this.bullets = new ArrayList<>();
        this.score = 0;
        this.lives = AppConfig.INITIAL_LIVES;
        this.isGameOver = false;
        this.isPaused = false;
        this.isRunning = false; // Game doesn't start running immediately
    }

    /**
     * Resets the game state to its initial configuration for starting a new game.
     * Clears all dynamic game objects, resets score and lives, positions the player,
     * and sets status flags to their default non-playing state.
     */
    public void reset() {
        // Reposition player to the center bottom area.
        player.resetPosition(
            AppConfig.GAME_WIDTH / 2 - AppConfig.PLAYER_WIDTH / 2,
            AppConfig.GAME_HEIGHT - AppConfig.BOTTOM_UI_BUFFER - AppConfig.PLAYER_HEIGHT - 10
        );
        // Clear all dynamic object lists.
        enemies.clear();
        powerUps.clear();
        bullets.clear();
        // Reset score and lives.
        score = 0;
        lives = AppConfig.INITIAL_LIVES;
        // Reset status flags.
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

    /**
     * Sets the game over state. If set to {@code true}, also sets {@code isRunning} to {@code false}.
     * @param gameOver {@code true} to mark the game as over, {@code false} otherwise.
     */
    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
        if (gameOver) {
            this.isRunning = false; // Game loop stops when game is over.
        }
    }

    /**
     * Sets the paused state. The game can only be paused if it is currently running and not game over.
     * @param paused {@code true} to pause the game, {@code false} to resume.
     */
    public void setPaused(boolean paused) {
        // Only allow pausing/resuming if the game is running and not over.
        if (this.isRunning && !this.isGameOver) {
            this.isPaused = paused;
        }
    }

    /**
     * Sets the running state, indicating whether the main game loop should process updates.
     * Cannot set to running if the game is already over. Setting to not running also clears the paused state.
     * @param running {@code true} to start or continue the game loop, {@code false} to stop it.
     */
    public void setRunning(boolean running) {
        if (!this.isGameOver) { // Can only set running if not game over
            this.isRunning = running;
            if (!running) {
                // If stopping the game, ensure it's not marked as paused.
                this.isPaused = false;
            }
        } else {
            // If game is over, ensure isRunning remains false.
            this.isRunning = false;
        }
    }

    /**
     * Decreases the player's lives by one. If lives reach zero or less,
     * sets the game over state. Logs the life loss.
     */
    public void decreaseLives() {
        if (lives > 0) {
            lives--;
            System.out.println("Life lost. Lives remaining: " + lives);
        }
        // Check if this life loss triggers game over.
        if (lives <= 0 && !isGameOver) {
            setGameOver(true); // Use the setter to ensure isRunning is also updated.
            System.out.println("Lives reached zero. Game Over triggered.");
        }
    }

    /**
     * Increases the player's score by a specified amount.
     * Score only increases if the amount is positive and the game is currently running.
     *
     * @param amount The positive amount to add to the score.
     */
    public void increaseScore(int amount) {
        if (amount > 0 && isRunning) { // Ensure score increases only during active gameplay.
            score += amount;
        }
    }

    /**
     * Adds a bullet to the list of active bullets. Ignores null bullets.
     * @param bullet The {@link Bullet} object to add.
     */
    public void addBullet(Bullet bullet) {
        if (bullet != null) bullets.add(bullet);
    }

    /**
     * Adds an enemy to the list of active enemies. Ignores null enemies.
     * @param enemy The {@link Enemy} object to add.
     */
    public void addEnemy(Enemy enemy) {
        if (enemy != null) enemies.add(enemy);
    }

    /**
     * Adds a power-up to the list of active power-ups. Ignores null power-ups.
     * @param powerUp The {@link PowerUp} object to add.
     */
    public void addPowerUp(PowerUp powerUp) {
        if (powerUp != null) powerUps.add(powerUp);
    }

    /**
     * Removes a collection of bullets from the active list.
     * Typically used after bullets hit enemies or go off-screen.
     * @param bulletsToRemove A {@link List} containing the {@link Bullet} objects to remove. If null, no action is taken.
     */
    public void removeBullets(List<Bullet> bulletsToRemove) {
        if (bulletsToRemove != null) bullets.removeAll(bulletsToRemove);
    }

    /**
     * Removes a collection of enemies from the active list.
     * Typically used after enemies are destroyed or go off-screen.
     * @param enemiesToRemove A {@link List} containing the {@link Enemy} objects to remove. If null, no action is taken.
     */
    public void removeEnemies(List<Enemy> enemiesToRemove) {
        if (enemiesToRemove != null) enemies.removeAll(enemiesToRemove);
    }

    /**
     * Removes a collection of power-ups from the active list.
     * Typically used after power-ups are collected or go off-screen.
     * @param powerUpsToRemove A {@link List} containing the {@link PowerUp} objects to remove. If null, no action is taken.
     */
    public void removePowerUps(List<PowerUp> powerUpsToRemove) {
        if (powerUpsToRemove != null) powerUps.removeAll(powerUpsToRemove);
    }
}