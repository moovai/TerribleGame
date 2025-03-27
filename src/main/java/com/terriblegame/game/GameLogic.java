package com.terriblegame.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import com.terriblegame.config.AppConfig;
import com.terriblegame.model.*;

/**
 * Implementation of {@link IGameLogic} that manages game mechanics using a
 * simple update loop and direct object manipulation.
 *
 * Implementation Notes:
 * - Uses {@link java.util.Random} for spawn probability calculations
 * - Employs separate lists for collision tracking to avoid concurrent modification
 * - Object updates are processed in order: player, bullets, enemies, power-ups
 * - Collision detection uses rectangular bounds for efficiency
 *
 * Performance Considerations:
 * - O(n²) collision detection between bullets and enemies
 * - Uses iterators for safe object removal during updates
 * - Maintains separate lists for removal to avoid collection modification issues
 */
public class GameLogic implements IGameLogic {
    private final Random randomGenerator = new Random();
    private final GameState gameState; // Reference to the mutable game state

    /**
     * Constructs a new GameLogic instance associated with a specific {@link GameState}.
     *
     * @param state The {@link GameState} object that this logic will operate on. Must not be null.
     * @throws IllegalArgumentException if the provided {@code state} is null.
     */
    public GameLogic(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("GameState cannot be null for GameLogic");
        }
        this.gameState = state;
    }

    /**
     * Initializes the game state for a new game session by resetting it.
     * Should be called before starting the game loop for a new game.
     */
    public void initializeNewGame() {
        gameState.reset(); // Use GameState's reset method
        System.out.println("Game logic initialized for a new game.");
    }

    /**
     * Performs a single update tick of the game logic.
     * This method should be called repeatedly by the main game loop (e.g., via a Timer).
     * It only processes updates if the game is running, not paused, and not over.
     * Updates player, bullets, enemies, power-ups, handles shooting, collisions, and spawning.
     */
    public void update() {
        // Do nothing if the game is not in an active state
        if (!gameState.isRunning() || gameState.isPaused() || gameState.isGameOver()) {
            return;
        }

        // Update the player's state (movement)
        gameState.getPlayer().update();
        // Process player shooting request
        handleShooting();

        // Update all active game objects and remove those out of bounds
        updateGameObjects(gameState.getBullets());
        updateGameObjects(gameState.getEnemies());
        updateGameObjects(gameState.getPowerUps());

        // Detect and resolve collisions between game objects
        handleCollisions();
        // Handle spawning of new enemies and power-ups based on chance
        handleSpawning();
    }

    /**
     * Helper method to update a list of {@link GameObject}s.
     * Iterates through the list, calls the {@code update()} method on each object,
     * and removes objects that have gone out of the screen bounds.
     * Uses an Iterator to safely remove elements during iteration.
     *
     * @param list A {@link List} of {@link GameObject}s (or a subclass) to update.
     * @param <T> The specific type of GameObject in the list.
     */
    private <T extends GameObject> void updateGameObjects(List<T> list) {
        Iterator<T> iterator = list.iterator();
        while (iterator.hasNext()) {
            T obj = iterator.next();
            obj.update(); // Update the object's state (e.g., position)
            // Check if the object is out of the vertical screen bounds
            if (obj.isOutOfBounds(AppConfig.GAME_HEIGHT)) {
                iterator.remove(); // Remove the object from the list
            }
        }
    }

    /**
     * Checks if the player wants to shoot and, if so, creates a new bullet
     * and adds it to the game state. The player's {@code wantsToShoot} flag is
     * consumed in the {@link Player#shoot()} method.
     */
    private void handleShooting() {
        Player player = gameState.getPlayer();
        if (player.isWantsToShoot()) {
            // Player.shoot() creates the bullet and resets the flag.
            gameState.addBullet(player.shoot());
        }
    }

    /**
     * Detects and handles collisions between different types of game objects:
     * - Player vs Enemy: Decreases player lives, removes enemy.
     * - Bullet vs Enemy: Removes both bullet and enemy, increases score.
     * - Player vs PowerUp: Applies power-up effect, removes power-up.
     * Uses temporary lists to collect objects for removal to avoid ConcurrentModificationException.
     */
    private void handleCollisions() {
        Player player = gameState.getPlayer();
        // Lists to store objects that need to be removed after collision checks.
        List<Bullet> bulletsToRemove = new ArrayList<>();
        List<Enemy> enemiesToRemove = new ArrayList<>();
        List<PowerUp> powerUpsToRemove = new ArrayList<>();

        // 1. Player vs Enemy Collisions
        for (Enemy enemy : gameState.getEnemies()) {
            if (!enemiesToRemove.contains(enemy) && player.intersects(enemy)) {
                gameState.decreaseLives(); // Player loses a life
                enemiesToRemove.add(enemy); // Mark enemy for removal
                // If game is over due to life loss, stop further collision checks
                if (gameState.isGameOver()) {
                    gameState.removeEnemies(enemiesToRemove);
                    gameState.removeBullets(bulletsToRemove);
                    gameState.removePowerUps(powerUpsToRemove);
                    return;
                }
            }
        }

        // 2. Bullet vs Enemy Collisions
        for (Bullet bullet : gameState.getBullets()) {
            if (bulletsToRemove.contains(bullet)) continue;

            for (Enemy enemy : gameState.getEnemies()) {
                if (enemiesToRemove.contains(enemy)) continue;

                if (bullet.intersects(enemy)) {
                    bulletsToRemove.add(bullet);
                    enemiesToRemove.add(enemy);
                    gameState.increaseScore(AppConfig.ENEMY_SCORE_VALUE);
                    break; // A single bullet should only hit one enemy
                }
            }
        }

        // 3. Player vs PowerUp Collisions
        for (PowerUp powerUp : gameState.getPowerUps()) {
            if (!powerUpsToRemove.contains(powerUp) && player.intersects(powerUp)) {
                powerUp.applyEffect(gameState);
                powerUpsToRemove.add(powerUp);
            }
        }

        // Remove all collided objects from the game state
        gameState.removeEnemies(enemiesToRemove);
        gameState.removeBullets(bulletsToRemove);
        gameState.removePowerUps(powerUpsToRemove);
    }

    /**
     * Handles the random spawning of new enemies and power-ups based on configured
     * probabilities and maximum counts. Checks occur each game tick.
     */
    private void handleSpawning() {
        // Spawn Enemy? Check count and probability.
        if (gameState.getEnemies().size() < AppConfig.MAX_ENEMIES &&
            randomGenerator.nextDouble() < AppConfig.ENEMY_SPAWN_CHANCE) {
            spawnEnemy();
        }
        // Spawn PowerUp? Check count and probability.
        if (gameState.getPowerUps().size() < AppConfig.MAX_POWERUPS &&
            randomGenerator.nextDouble() < AppConfig.POWERUP_SPAWN_CHANCE) {
            spawnPowerUp();
        }
    }

    /**
     * Creates a new enemy instance with randomized size, position (spawning above the screen),
     * and speed, then adds it to the game state.
     */
    private void spawnEnemy() {
        // Calculate random size within configured range
        int sizeVar = AppConfig.ENEMY_SIZE_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SIZE_VARIATION) : 0;
        int size = AppConfig.ENEMY_BASE_SIZE + sizeVar;
        // Calculate random horizontal position ensuring enemy is fully within screen width
        int enemyX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - size);
        int enemyY = -size; // Start just above the screen
        // Calculate random speed within configured range
        int speedVar = AppConfig.ENEMY_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.ENEMY_SPEED_VARIATION) : 0;
        int enemySpeed = AppConfig.ENEMY_BASE_SPEED + speedVar;
        // Create and add the new enemy
        gameState.addEnemy(new Enemy(enemyX, enemyY, size, size, enemySpeed));
    }

    /**
     * Creates a new power-up instance with randomized position (spawning above the screen)
     * and speed, then adds it to the game state.
     */
    private void spawnPowerUp() {
        // Calculate random horizontal position ensuring power-up is fully within screen width
        int powerUpX = randomGenerator.nextInt(AppConfig.GAME_WIDTH - AppConfig.POWERUP_WIDTH);
        int powerUpY = -AppConfig.POWERUP_HEIGHT; // Start just above the screen
        // Calculate random speed within configured range
        int speedVar = AppConfig.POWERUP_SPEED_VARIATION > 0 ? randomGenerator.nextInt(AppConfig.POWERUP_SPEED_VARIATION) : 0;
        int powerUpSpeed = AppConfig.POWERUP_BASE_SPEED + speedVar;
        // Create and add the new power-up
        gameState.addPowerUp(new PowerUp(powerUpX, powerUpY, powerUpSpeed));
    }

    /**
     * Implementation Note: Returns direct reference to game state
     * for efficiency. Callers should treat it as read-only.
     */
    @Override
    public GameState getGameState() {
        return gameState;
    }
}