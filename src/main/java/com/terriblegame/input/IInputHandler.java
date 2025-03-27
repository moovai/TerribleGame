package com.terriblegame.input;

import java.awt.event.KeyEvent;

/**
 * Defines the contract for processing user input in the game.
 * Responsible for translating keyboard events into game actions
 * and maintaining the input state.
 *
 * Key Responsibilities:
 * - Process keyboard input events
 * - Maintain current input state
 * - Convert input to game actions
 * - Support simultaneous key presses
 *
 * Limitations:
 * - Keyboard input only (no mouse/gamepad)
 * - Cannot distinguish between physical keys
 * - Limited to basic directional and action inputs
 */
public interface IInputHandler {

    /**
     * Processes a key press event from the keyboard.
     * Updates the internal input state to reflect the pressed key.
     *
     * Side Effects:
     * - Updates internal key state tracking
     * - May trigger game state changes through bound actions
     *
     * Threading Considerations:
     * - Must be thread-safe for AWT event dispatch thread
     *
     * @param e The key event containing the pressed key information
     */
    void keyPressed(KeyEvent e);

    /**
     * Processes a key release event from the keyboard.
     * Updates the internal input state to reflect the released key.
     *
     * Side Effects:
     * - Updates internal key state tracking
     * - May affect ongoing actions (e.g., stop movement)
     *
     * Threading Considerations:
     * - Must be thread-safe for AWT event dispatch thread
     *
     * @param e The key event containing the released key information
     */
    void keyReleased(KeyEvent e);

    /**
     * Checks if movement in the upward direction is currently active.
     *
     * Side Effects:
     * - None, pure state query
     *
     * @return true if up movement is active
     */
    boolean isUpPressed();

    /**
     * Checks if movement in the downward direction is currently active.
     *
     * Side Effects:
     * - None, pure state query
     *
     * @return true if down movement is active
     */
    boolean isDownPressed();

    /**
     * Checks if movement to the left is currently active.
     *
     * Side Effects:
     * - None, pure state query
     *
     * @return true if left movement is active
     */
    boolean isLeftPressed();

    /**
     * Checks if movement to the right is currently active.
     *
     * Side Effects:
     * - None, pure state query
     *
     * @return true if right movement is active
     */
    boolean isRightPressed();

    /**
     * Checks if the shoot action is currently active.
     *
     * Side Effects:
     * - None, pure state query
     *
     * @return true if shoot action is active
     */
    boolean isShootPressed();

    /**
     * Checks if the pause action is currently active.
     *
     * Side Effects:
     * - None, pure state query
     *
     * @return true if pause action is active
     */
    boolean isPausePressed();

    /**
     * Resets all input states to their default (inactive) values.
     * Useful when transitioning game states or handling focus changes.
     *
     * Side Effects:
     * - Clears all active input states
     * - May affect ongoing actions
     */
    void reset();
}