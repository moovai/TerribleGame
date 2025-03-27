package com.terriblegame.model;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Base implementation of {@link IGameObject} that provides common functionality
 * for game entities. This implementation uses a rectangular boundary for collision
 * detection and maintains position/size data.
 *
 * Implementation Notes:
 * - Uses a {@link Rectangle} for efficient collision detection
 * - Position updates require manual bounds synchronization via {@link #updateBounds()}
 * - Subclasses must implement {@link #update()} and {@link #draw(Graphics2D)}
 */
public abstract class GameObject implements IGameObject {
    /** The x-coordinate of the top-left corner of the object. */
    protected int x;
    /** The y-coordinate of the top-left corner of the object. */
    protected int y;
    /** The width of the object in pixels. */
    protected int width;
    /** The height of the object in pixels. */
    protected int height;
    /** The rectangular boundary used for collision detection. */
    protected Rectangle bounds;

    /**
     * Constructs a new GameObject with specified position and dimensions.
     * Initializes the collision bounds based on these parameters.
     *
     * @param x      The initial x-coordinate.
     * @param y      The initial y-coordinate.
     * @param width  The width of the object.
     * @param height The height of the object.
     */
    public GameObject(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.bounds = new Rectangle(x, y, width, height);
    }

    /**
     * Gets the current x-coordinate of the object.
     * @return The x-coordinate.
     */
    public int getX() { return x; }

    /**
     * Gets the current y-coordinate of the object.
     * @return The y-coordinate.
     */
    public int getY() { return y; }

    /**
     * Gets the width of the object.
     * @return The width in pixels.
     */
    public int getWidth() { return width; }

    /**
     * Gets the height of the object.
     * @return The height in pixels.
     */
    public int getHeight() { return height; }

    /**
     * Updates the position of the internal {@link Rectangle} bounds to match the object's current x and y coordinates.
     * This should be called after the object's position changes.
     */
    protected void updateBounds() {
        bounds.setLocation(x, y);
    }

    /**
     * Gets the rectangular bounds of this object, used for collision detection.
     * Note: The returned rectangle's position might be slightly out of sync if {@link #updateBounds()}
     * hasn't been called after the last position change.
     * @return A {@link Rectangle} representing the object's boundaries.
     */
    public Rectangle getBounds() {
        return bounds;
    }

    /**
     * Updates the state of the game object. This typically involves moving the object
     * based on its speed or player input. Must be implemented by subclasses.
     */
    public abstract void update();

    /**
     * Renders the game object onto the screen. Must be implemented by subclasses.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    public abstract void draw(Graphics2D g2d);

    /**
     * Checks if the object has moved completely outside the vertical bounds of the screen.
     *
     * @param screenHeight The height of the game screen.
     * @return {@code true} if the object is entirely above the top or below the bottom edge, {@code false} otherwise.
     */
    public boolean isOutOfBounds(int screenHeight) {
        return y > screenHeight || y + height < 0;
    }

    /**
     * Implementation of {@link IGameObject#intersects(IGameObject)}.
     * Uses rectangle intersection for efficient collision detection.
     *
     * @param other The other game object to check for intersection
     * @return true if the bounds intersect, false otherwise
     */
    @Override
    public boolean intersects(IGameObject other) {
        return this.bounds.intersects(other.getBounds());
    }
}