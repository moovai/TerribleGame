package com.terriblegame.model;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Defines the contract for all interactive entities within the game world.
 * Every game object must provide position information, handle updates,
 * support rendering, and participate in collision detection.
 *
 * Implementations include players, enemies, bullets, and power-ups.
 *
 * Side Effects:
 * - Collision bounds may be modified during position updates
 * - Visual state changes when rendered to the graphics context
 * - Internal state modifications during updates
 */
public interface IGameObject {
    /**
     * Gets the current x-coordinate of the object.
     * This position represents the left edge of the object.
     *
     * @return The x-coordinate in pixels
     */
    int getX();

    /**
     * Gets the current y-coordinate of the object.
     * This position represents the top edge of the object.
     *
     * @return The y-coordinate in pixels
     */
    int getY();

    /**
     * Gets the width of the object.
     * Used for collision detection and rendering boundaries.
     *
     * @return The width in pixels
     */
    int getWidth();

    /**
     * Gets the height of the object.
     * Used for collision detection and rendering boundaries.
     *
     * @return The height in pixels
     */
    int getHeight();

    /**
     * Gets the rectangular bounds used for collision detection.
     * The bounds should always reflect the current position and size of the object.
     *
     * Side Effects:
     * - None, but the returned Rectangle may be affected by previous position updates
     *
     * @return A Rectangle representing the object's current boundaries
     */
    Rectangle getBounds();

    /**
     * Updates the object's state for the current frame.
     * This includes position updates, internal state changes, and any game logic
     * specific to the object type.
     *
     * Side Effects:
     * - May modify position (x, y coordinates)
     * - May update internal state (speed, direction, etc.)
     * - May trigger collision bound updates
     */
    void update();

    /**
     * Renders the object to the screen using the provided graphics context.
     * Implementation details like color, shape, and visual effects are left
     * to the concrete classes.
     *
     * Side Effects:
     * - Modifies the provided graphics context by drawing to it
     * - No modification to the object's internal state
     *
     * @param g2d The graphics context to draw on
     */
    void draw(Graphics2D g2d);

    /**
     * Checks if the object has moved completely outside the vertical bounds of the screen.
     * Used for cleanup of off-screen objects to maintain game performance.
     *
     * Side Effects:
     * - None, purely a state query
     *
     * @param screenHeight The height of the game screen in pixels
     * @return true if the object is entirely above or below the screen bounds
     */
    boolean isOutOfBounds(int screenHeight);

    /**
     * Checks if this object's bounds intersect with another game object's bounds.
     * Used for collision detection between game objects.
     *
     * Side Effects:
     * - None, purely a geometric calculation
     *
     * @param other The other game object to check collision with
     * @return true if the objects' bounds intersect
     */
    boolean intersects(IGameObject other);
}