package com.terriblegame.game;

/**
 * Defines the contract for managing core game mechanics and state updates.
 * This interface represents the central game loop logic, handling object
 * lifecycles, collisions, and game rules.
 *
 * Implementations are responsible for:
 * - Updating game object positions
 * - Managing collisions between objects
 * - Handling object spawning/removal
 * - Processing player actions
 * - Enforcing game rules
 *
 * Limitations:
 * - Assumes single-threaded execution
 * - Requires a valid GameState instance
 * - Does not handle rendering (separation of concerns)
 */
public interface IGameLogic {

    /**
     * Initializes the game state for a new game session.
     * Should be called before starting a new game.
     *
     * Side Effects:
     * - Resets the game state to initial values
     * - Clears all game objects
     * - Resets score and lives
     */
    void initializeNewGame();

    /**
     * Performs a single update tick of the game logic.
     * This is the core method that should be called repeatedly
     * by the main game loop to advance the game state.
     *
     * Side Effects:
     * - Updates positions of all game objects
     * - Processes player input (shooting)
     * - Checks and resolves collisions
     * - Spawns new objects (enemies, power-ups)
     * - Removes out-of-bounds objects
     * - Updates score and lives based on game events
     * - May trigger game over state
     *
     * Threading Considerations:
     * - Must be called from the game loop thread
     * - Should not be called if game is paused or over
     */
    void update();

    /**
     * Gets the current game state instance that this logic operates on.
     * Provides read-only access to game state for external components
     * (e.g., rendering, UI updates).
     *
     * Side Effects:
     * - None, purely a state query
     *
     * @return The current GameState instance
     */
    GameState getGameState();
}