package com.terriblegame.model;

import java.awt.Graphics2D;
import com.terriblegame.config.AppConfig;
import com.terriblegame.game.GameState;

/**
 * Represents a power-up item that moves downwards and provides a benefit (e.g., score) when collected by the player.
 */
public class PowerUp extends GameObject {
    private int speed;

    /**
     * Constructs a new PowerUp object at the specified position with a given speed.
     * Initializes dimensions based on {@link AppConfig}.
     *
     * @param x     The initial x-coordinate of the power-up.
     * @param y     The initial y-coordinate of the power-up.
     * @param speed The downward speed of the power-up in pixels per tick.
     */
    public PowerUp(int x, int y, int speed) {
        super(x, y, AppConfig.POWERUP_WIDTH, AppConfig.POWERUP_HEIGHT);
        this.speed = speed;
    }

    /**
     * Updates the power-up's position, moving it downwards based on its speed.
     * Updates collision bounds.
     */
    @Override
    public void update() {
        y += speed; // Move downwards
        updateBounds();
    }

    /**
     * Draws the power-up item on the screen using its configured color (as an oval).
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    @Override
    public void draw(Graphics2D g2d) {
        g2d.setColor(AppConfig.POWERUP_COLOR);
        g2d.fillOval(x, y, width, height); // Drawn as an oval
    }

    /**
     * Applies the effect of this power-up to the game state.
     * Currently, it increases the player's score.
     *
     * @param gameState The current {@link GameState} to modify.
     */
    public void applyEffect(GameState gameState) {
        gameState.increaseScore(AppConfig.POWERUP_SCORE_VALUE);
    }
}