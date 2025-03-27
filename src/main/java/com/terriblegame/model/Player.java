package com.terriblegame.model;

import java.awt.Graphics2D;
import com.terriblegame.config.AppConfig;

/**
 * Represents the player-controlled spaceship. Handles movement based on input flags
 * and provides the ability to shoot bullets.
 */
public class Player extends GameObject {
    private int speed;
    private boolean movingLeft, movingRight, movingUp, movingDown;
    private boolean wantsToShoot = false;

    /**
     * Constructs a new Player object at the specified starting position.
     * Initializes speed and dimensions based on {@link AppConfig}.
     *
     * @param startX The initial x-coordinate for the player.
     * @param startY The initial y-coordinate for the player.
     */
    public Player(int startX, int startY) {
        super(startX, startY, AppConfig.PLAYER_WIDTH, AppConfig.PLAYER_HEIGHT);
        this.speed = AppConfig.PLAYER_SPEED;
        this.movingLeft = false;
        this.movingRight = false;
        this.movingUp = false;
        this.movingDown = false;
    }

    /**
     * Sets the flag indicating whether the player should move left.
     * @param movingLeft {@code true} to move left, {@code false} to stop.
     */
    public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }

    /**
     * Sets the flag indicating whether the player should move right.
     * @param movingRight {@code true} to move right, {@code false} to stop.
     */
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }

    /**
     * Sets the flag indicating whether the player should move up.
     * @param movingUp {@code true} to move up, {@code false} to stop.
     */
    public void setMovingUp(boolean movingUp) { this.movingUp = movingUp; }

    /**
     * Sets the flag indicating whether the player should move down.
     * @param movingDown {@code true} to move down, {@code false} to stop.
     */
    public void setMovingDown(boolean movingDown) { this.movingDown = movingDown; }

    /**
     * Sets the flag indicating whether the player intends to shoot on the next update cycle.
     * @param wantsToShoot {@code true} if the player wants to shoot, {@code false} otherwise.
     */
    public void setWantsToShoot(boolean wantsToShoot) { this.wantsToShoot = wantsToShoot; }

    /**
     * Checks if the player currently intends to shoot.
     * @return {@code true} if the shoot action is requested, {@code false} otherwise.
     */
    public boolean isWantsToShoot() { return wantsToShoot; }

    /**
     * Updates the player's position based on the current movement flags.
     * Ensures the player stays within the game boundaries. Updates collision bounds.
     */
    @Override
    public void update() {
        int dx = 0; // Change in x
        int dy = 0; // Change in y

        // Calculate delta based on movement flags and speed
        if (movingLeft) dx -= speed;
        if (movingRight) dx += speed;
        if (movingUp) dy -= speed;
        if (movingDown) dy += speed;

        // Update position
        x += dx;
        y += dy;

        // Clamp position to stay within screen bounds, considering UI buffer at the bottom
        x = Math.max(0, x); // Prevent moving past left edge
        x = Math.min(AppConfig.GAME_WIDTH - width, x); // Prevent moving past right edge
        y = Math.max(0, y); // Prevent moving past top edge
        y = Math.min(AppConfig.GAME_HEIGHT - height - AppConfig.BOTTOM_UI_BUFFER, y); // Prevent moving past bottom edge (above UI)

        // Update the collision bounds rectangle
        updateBounds();
    }

    /**
     * Draws the player's spaceship on the screen using its configured color.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.PLAYER_COLOR);
        g2d.fillRect(x, y, width, height);
    }

    /**
     * Creates and returns a new {@link Bullet} object originating from the player's current position.
     * Resets the {@code wantsToShoot} flag to {@code false}.
     *
     * @return A new {@link Bullet} instance ready to be added to the game state.
     */
    public Bullet shoot() {
        this.wantsToShoot = false; // Consume the shoot request
        // Calculate bullet starting position (centered above the player)
        int bulletX = this.x + this.width / 2 - AppConfig.BULLET_WIDTH / 2;
        int bulletY = this.y - AppConfig.BULLET_HEIGHT;
        return new Bullet(bulletX, bulletY);
    }

    /**
     * Resets the player's position to a specified starting point and clears all movement/shooting flags.
     * Typically used when starting a new game or after losing a life.
     *
     * @param startX The x-coordinate to reset the player to.
     * @param startY The y-coordinate to reset the player to.
     */
    public void resetPosition(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.movingLeft = false;
        this.movingRight = false;
        this.movingUp = false;
        this.movingDown = false;
        this.wantsToShoot = false;
        updateBounds(); // Ensure bounds match the new position
    }
}