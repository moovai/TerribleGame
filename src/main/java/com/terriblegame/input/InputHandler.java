package com.terriblegame.input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import com.terriblegame.game.GameState;
import com.terriblegame.controller.GameController;
import com.terriblegame.model.Player;

/**
 * AWT-based implementation of {@link IInputHandler} that maps keyboard events
 * to game actions.
 *
 * Implementation Details:
 * - Uses AWT KeyAdapter for event handling
 * - Supports both WASD and arrow key controls
 * - Maintains boolean state for each input
 * - Updates Player state directly for movement
 * - Delegates game control actions to GameController
 *
 * Technical Notes:
 * - All event handling occurs on AWT event dispatch thread
 * - Input state is maintained even when game is paused
 * - Movement flags are cleared on key release
 * - Shoot flag is consumed by game logic
 */
public class InputHandler extends KeyAdapter implements IInputHandler {
    // Input state tracking
    private boolean upPressed;
    private boolean downPressed;
    private boolean leftPressed;
    private boolean rightPressed;
    private boolean shootPressed;
    private boolean pausePressed;
    
    private final GameState gameState;
    private final GameController gameController;

    /**
     * Constructs a new InputHandler with dependencies.
     *
     * Implementation Note: All input states are initialized to false.
     *
     * @param state The game state for player updates
     * @param controller The game controller for system actions
     * @throws IllegalArgumentException if dependencies are null
     */
    public InputHandler(GameState state, GameController controller) {
        if (state == null || controller == null) {
            throw new IllegalArgumentException("GameState and GameController cannot be null for InputHandler");
        }
        this.gameState = state;
        this.gameController = controller;
        reset(); // Initialize all input states to false
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // Update input state flags
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                leftPressed = true;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                rightPressed = true;
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = true;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = true;
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_CONTROL:
                shootPressed = true;
                break;
            case KeyEvent.VK_P:
                pausePressed = true;
                gameController.togglePause();
                break;
            case KeyEvent.VK_ESCAPE:
                gameController.exitGame();
                break;
            case KeyEvent.VK_R:
                if (gameState.isGameOver()) {
                    gameController.requestRestart();
                }
                break;
        }

        // Update player state if game is active
        updatePlayerState();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int keyCode = e.getKeyCode();

        // Update input state flags
        switch (keyCode) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                leftPressed = false;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                rightPressed = false;
                break;
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = false;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = false;
                break;
            case KeyEvent.VK_SPACE:
            case KeyEvent.VK_CONTROL:
                shootPressed = false;
                break;
            case KeyEvent.VK_P:
                pausePressed = false;
                break;
        }

        // Update player state if game is active
        updatePlayerState();
    }

    @Override
    public boolean isUpPressed() {
        return upPressed;
    }

    @Override
    public boolean isDownPressed() {
        return downPressed;
    }

    @Override
    public boolean isLeftPressed() {
        return leftPressed;
    }

    @Override
    public boolean isRightPressed() {
        return rightPressed;
    }

    @Override
    public boolean isShootPressed() {
        return shootPressed;
    }

    @Override
    public boolean isPausePressed() {
        return pausePressed;
    }

    @Override
    public void reset() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
        shootPressed = false;
        pausePressed = false;
        
        // Also reset player movement state if available
        if (gameState != null && gameState.getPlayer() != null) {
            Player player = gameState.getPlayer();
            player.setMovingUp(false);
            player.setMovingDown(false);
            player.setMovingLeft(false);
            player.setMovingRight(false);
            player.setWantsToShoot(false);
        }
    }

    /**
     * Updates the player's movement and action states based on current input flags.
     * Only applies updates if the game is running and not paused.
     *
     * Implementation Note: This method consolidates all player state updates
     * to ensure consistent behavior between key press and release events.
     */
    private void updatePlayerState() {
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
            Player player = gameState.getPlayer();
            if (player != null) {
                player.setMovingUp(upPressed);
                player.setMovingDown(downPressed);
                player.setMovingLeft(leftPressed);
                player.setMovingRight(rightPressed);
                player.setWantsToShoot(shootPressed);
            }
        }
    }
}