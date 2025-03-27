package com.terriblegame.model;

import java.awt.Graphics2D;
import com.terriblegame.config.AppConfig;

/**
 * Represents a projectile fired by the player. Moves upwards.
 */
public class Bullet extends GameObject {
    private int speed;

    /**
     * Constructs a new Bullet object at the specified position.
     * Initializes speed and dimensions based on {@link AppConfig}.
     *
     * @param x The initial x-coordinate of the bullet.
     * @param y The initial y-coordinate of the bullet.
     */
    public Bullet(int x, int y) {
        super(x, y, AppConfig.BULLET_WIDTH, AppConfig.BULLET_HEIGHT);
        this.speed = AppConfig.BULLET_SPEED;
    }

    /**
     * Updates the bullet's position, moving it upwards based on its speed.
     * Updates collision bounds.
     */
    @Override
    public void update() {
        y -= speed; // Move upwards
        updateBounds();
    }

    /**
     * Draws the bullet on the screen using its configured color.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.BULLET_COLOR);
        g2d.fillRect(x, y, width, height);
    }
}