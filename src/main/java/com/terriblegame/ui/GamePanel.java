package com.terriblegame.ui;

import javax.swing.JPanel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import com.terriblegame.config.AppConfig;
import com.terriblegame.game.GameState;
import com.terriblegame.model.*;

/**
 * Represents the main Game Screen as a {@link JPanel}.
 * Responsible for rendering the current {@link GameState}, including the background,
 * player, enemies, bullets, power-ups, UI elements (score, lives), and overlays (pause/game over messages).
 * Relies on the {@link GameState} object passed to it for all data to be drawn.
 */
public class GamePanel extends JPanel {
    private final GameState gameState;

    // Pre-create Font objects for performance
    private final Font uiFont;
    private final Font gameOverLargeFont;
    private final Font gameOverMediumFont;
    private final Font gameOverSmallFont;
    private final Font pauseLargeFont;
    private final Font pauseSmallFont;

    /**
     * Constructs the GamePanel.
     *
     * @param state The {@link GameState} instance containing the data to be rendered. Must not be null.
     * @throws IllegalArgumentException if the provided {@code state} is null.
     */
    public GamePanel(GameState state) {
        if (state == null) {
            throw new IllegalArgumentException("GameState cannot be null for GamePanel");
        }
        this.gameState = state;

        // Initialize fonts based on AppConfig values
        uiFont = new Font(AppConfig.GAME_UI_FONT_NAME, AppConfig.GAME_UI_FONT_STYLE, AppConfig.GAME_UI_FONT_SIZE);
        gameOverLargeFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_LARGE_FONT_STYLE, AppConfig.GAME_OVER_LARGE_FONT_SIZE);
        gameOverMediumFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_MEDIUM_FONT_STYLE, AppConfig.GAME_OVER_MEDIUM_FONT_SIZE);
        gameOverSmallFont = new Font(AppConfig.GAME_OVER_FONT_NAME, AppConfig.GAME_OVER_SMALL_FONT_STYLE, AppConfig.GAME_OVER_SMALL_FONT_SIZE);
        pauseLargeFont = new Font(AppConfig.PAUSE_FONT_NAME, AppConfig.PAUSE_LARGE_FONT_STYLE, AppConfig.PAUSE_LARGE_FONT_SIZE);
        pauseSmallFont = new Font(AppConfig.PAUSE_FONT_NAME, AppConfig.PAUSE_SMALL_FONT_STYLE, AppConfig.PAUSE_SMALL_FONT_SIZE);

        // Configure panel properties
        setPreferredSize(new Dimension(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT));
        setBackground(AppConfig.GAME_BACKGROUND_COLOR);
        setDoubleBuffered(true); // Use double buffering for smoother rendering
        setFocusable(true); // Panel needs focus to receive key events
        setRequestFocusEnabled(true); // Allow requesting focus programmatically
        setFocusTraversalKeysEnabled(false); // Prevent Tab/Shift-Tab from changing focus away from the panel
    }

    /**
     * Overrides the {@link JComponent#paintComponent(Graphics)} method to custom render the game state.
     * This method is called by Swing whenever the panel needs to be redrawn.
     * It orchestrates the drawing of the background, game objects, UI, and overlays.
     *
     * @param g The {@link Graphics} context provided by Swing for drawing.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable anti-aliasing for smoother shapes and text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw elements in layers: background -> objects -> UI -> overlays
        drawBackground(g2d);

        // Only draw game objects if the game is considered "running" or if it's game over
        if (gameState.isRunning() || gameState.isGameOver()) {
            drawGameObjects(g2d);
        }

        drawUI(g2d);
        drawOverlays(g2d);

        g2d.dispose();
    }

    /**
     * Fills the panel's background with the configured color.
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    private void drawBackground(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_BACKGROUND_COLOR);
        g2d.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Draws all active game objects (player, bullets, enemies, power-ups) onto the screen.
     * Iterates through the lists obtained from {@link GameState}.
     * Uses defensive copies of the lists to mitigate concurrency issues.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    private void drawGameObjects(Graphics2D g2d) {
        try {
            // Draw Player
            Player player = gameState.getPlayer();
            if (player != null) {
                player.draw(g2d);
            }

            // Draw Bullets (using a copy of the list)
            List<Bullet> bulletsToDraw = new ArrayList<>(gameState.getBullets());
            for (Bullet bullet : bulletsToDraw) {
                bullet.draw(g2d);
            }

            // Draw Enemies (using a copy of the list)
            List<Enemy> enemiesToDraw = new ArrayList<>(gameState.getEnemies());
            for (Enemy enemy : enemiesToDraw) {
                enemy.draw(g2d);
            }

            // Draw PowerUps (using a copy of the list)
            List<PowerUp> powerUpsToDraw = new ArrayList<>(gameState.getPowerUps());
            for (PowerUp powerUp : powerUpsToDraw) {
                powerUp.draw(g2d);
            }
        } catch (Exception e) {
            System.err.println("Error during rendering game objects: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Draws the static UI elements, such as the player's score and remaining lives,
     * at the top of the screen. Uses configured colors and fonts.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    private void drawUI(Graphics2D g2d) {
        g2d.setColor(AppConfig.GAME_UI_TEXT_COLOR);
        g2d.setFont(uiFont);
        int uiMarginX = 10;
        int uiMarginYTop = 20;
        int livesRightMargin = 100;
        g2d.drawString("Score: " + gameState.getScore(), uiMarginX, uiMarginYTop);
        g2d.drawString("Lives: " + gameState.getLives(), AppConfig.GAME_WIDTH - livesRightMargin, uiMarginYTop);
    }

    /**
     * Draws overlay messages if the game is paused or over.
     * Renders "GAME OVER" text with score and instructions, or "PAUSED" text with instructions,
     * centered on the screen. Uses specific fonts and colors configured in {@link AppConfig}.
     *
     * @param g2d The {@link Graphics2D} context to draw on.
     */
    private void drawOverlays(Graphics2D g2d) {
        int yOffsetLarge = -40;
        int yOffsetMedium = 20;
        int yOffsetSmall = 60;

        if (gameState.isGameOver()) {
            drawCenteredString(g2d, "GAME OVER", gameOverLargeFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetLarge);
            drawCenteredString(g2d, "Final Score: " + gameState.getScore(), gameOverMediumFont, AppConfig.GAME_OVER_TEXT_COLOR, yOffsetMedium);
            drawCenteredString(g2d, "(R: Restart / ESC: Exit to Login)", gameOverSmallFont, AppConfig.GAME_UI_TEXT_COLOR, yOffsetSmall);
        } else if (gameState.isPaused()) {
            g2d.setColor(AppConfig.PAUSE_OVERLAY_COLOR);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            drawCenteredString(g2d, "PAUSED", pauseLargeFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetLarge);
            drawCenteredString(g2d, "(Press 'P' to Resume)", pauseSmallFont, AppConfig.PAUSE_TEXT_COLOR, yOffsetSmall);
        }
    }

    /**
     * Utility method to draw a string centered horizontally and vertically (with an offset) on the panel.
     *
     * @param g2d     The {@link Graphics2D} context to draw on.
     * @param text    The string to draw.
     * @param font    The {@link Font} to use for the string.
     * @param color   The {@link Color} to use for the string.
     * @param yOffset An additional vertical offset from the vertical center.
     */
    private void drawCenteredString(Graphics2D g2d, String text, Font font, Color color, int yOffset) {
        g2d.setFont(font);
        g2d.setColor(color);
        FontMetrics metrics = g2d.getFontMetrics(font);
        int x = (AppConfig.GAME_WIDTH - metrics.stringWidth(text)) / 2;
        int y = (AppConfig.GAME_HEIGHT / 2) - (metrics.getHeight() / 2) + metrics.getAscent() + yOffset;
        g2d.drawString(text, x, y);
    }
}