package com.terriblegame.input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import com.terriblegame.game.GameState;
import com.terriblegame.controller.GameController;
import com.terriblegame.model.Player;

/**
 * Handles keyboard input for the game. Extends {@link KeyAdapter} to listen for
 * key press and release events. Translates key events into actions on the
 * {@link Player} object within the {@link GameState} or triggers actions
 * on the {@link GameController} (like pausing, restarting, exiting).
 */
public class InputHandler extends KeyAdapter {
    private final GameState gameState;
    private final GameController gameController;

    /**
     * Constructs a new InputHandler.
     *
     * @param state      The {@link GameState} containing the player object to control. Must not be null.
     * @param controller The {@link GameController} to notify for game-level actions (pause, etc.). Must not be null.
     * @throws IllegalArgumentException if either {@code state} or {@code controller} is null.
     */
    public InputHandler(GameState state, GameController controller) {
        if (state == null || controller == null) {
            throw new IllegalArgumentException("GameState and GameController cannot be null for InputHandler");
        }
        this.gameState = state;
        this.gameController = controller;
    }

    /**
     * Handles key pressed events.
     * Maps specific keys (P, ESC, R) to game control actions (pause, exit, restart).
     * Maps movement keys (Arrows, WASD) and shooting keys (Space, Ctrl) to player actions,
     * but only if the game is actively running (not paused, not game over).
     *
     * @param e The {@link KeyEvent} associated with the key press.
     */
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // --- Game Control Keys (handled regardless of game running state, except restart) ---
        switch (keyCode) {
            case KeyEvent.VK_P: // Toggle Pause
                gameController.togglePause();
                return; // Consume event
            case KeyEvent.VK_ESCAPE: // Exit Game
                gameController.exitGame();
                return; // Consume event
            case KeyEvent.VK_R: // Restart Game (only if game is over)
                if (gameState.isGameOver()) {
                    System.out.println("Restart key (R) pressed - Requesting restart.");
                    gameController.requestRestart();
                }
                return; // Consume event
        }

        // --- Player Control Keys (only handled if game is running, not paused, not game over) ---
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
            Player player = gameState.getPlayer();
            // Defensive check, although player should exist if game is running.
            if (player == null) return;

            // Set movement/shooting flags based on key code
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
                    player.setWantsToShoot(true);
                    break;
            }
        }
    }

    /**
     * Handles key released events.
     * Clears the corresponding movement flags on the {@link Player} object when
     * movement keys (Arrows, WASD) are released. Shooting flag is not cleared here,
     * it's consumed when the bullet is fired.
     *
     * @param e The {@link KeyEvent} associated with the key release.
     */
    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();
        Player player = gameState.getPlayer();
        // Defensive check
        if (player == null) return;

        // Clear movement flags based on released key
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
            // No action needed for releasing shoot key (SPACE/CONTROL) as Player.shoot() resets the flag.
            // No action needed for releasing P, ESC, R.
        }
    }
}