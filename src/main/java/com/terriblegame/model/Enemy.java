package com.terriblegame.model;

import java.awt.Graphics2D;
import com.terriblegame.config.AppConfig;

/**
 * Represents an enemy ship that moves downwards towards the player.
 */
public class Enemy extends GameObject {
    private int speed;

    /**
     * Constructs a new Enemy object at the specified position with given dimensions and speed.
     *
     * @param x      The initial x-coordinate of the enemy.
     * @param y      The initial y-coordinate of the enemy.
     * @param width  The width of the enemy.
     * @param height The height of the enemy.
     * @param speed  The downward speed of the enemy in pixels per tick.
     */
    public Enemy(int x, int y, int width, int height, int speed) {
        super(x, y, width, height);
        this.speed = speed;
    }

    /**
     * Updates the enemy's position, moving it downwards based on its speed.
     * Updates collision bounds.
     */
    @Override
    public void update() {
        y += speed; // Move downwards
        updateBounds();
    }

    /**
     * Draws the enemy ship on the screen using its configured color.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.ENEMY_COLOR);
        g2d.fillRect(x, y, width, height);
    }
}