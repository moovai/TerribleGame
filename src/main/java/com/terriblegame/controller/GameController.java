package com.terriblegame.controller;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.terriblegame.config.AppConfig;
import com.terriblegame.db.DatabaseManager;
import com.terriblegame.game.GameLogic;
import com.terriblegame.game.GameState;
import com.terriblegame.ui.GamePanel;
import com.terriblegame.TerribleGame;

/**
 * Acts as the central coordinator for the game. Manages the main game loop using a {@link Timer},
 * orchestrates updates between the {@link GameLogic} and {@link GameState}, handles game state transitions
 * (starting, pausing, game over, restarting), interacts with the {@link GamePanel} for rendering updates,
 * and uses the {@link DatabaseManager} to save scores.
 */
public class GameController implements ActionListener {
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GamePanel gamePanel;
    private final Timer gameTimer;
    private final DatabaseManager dbManager;
    private final TerribleGame mainApp;

    /**
     * Constructs the GameController.
     *
     * @param state   The {@link GameState} instance for the game. Must not be null.
     * @param logic   The {@link GameLogic} instance containing game rules. Must not be null.
     * @param panel   The {@link GamePanel} instance for rendering. Must not be null.
     * @param dbMgr   The {@link DatabaseManager} instance for database operations. Must not be null.
     * @param app     The main {@link TerribleGame} application instance. Must not be null.
     * @throws IllegalArgumentException if any dependency is null.
     */
    public GameController(GameState state, GameLogic logic, GamePanel panel, DatabaseManager dbMgr, TerribleGame app) {
        if (state == null || logic == null || panel == null || dbMgr == null || app == null) {
            throw new IllegalArgumentException("All dependencies must be non-null for GameController");
        }
        this.gameState = state;
        this.gameLogic = logic;
        this.gamePanel = panel;
        this.dbManager = dbMgr;
        this.mainApp = app;

        this.gameTimer = new Timer(AppConfig.GAME_TICK_MS, this);
        this.gameTimer.setInitialDelay(0);
        this.gameTimer.setCoalesce(true);
    }

    /**
     * Starts a new game. Initializes game logic, sets the game state to running,
     * starts the game loop timer, and triggers an initial repaint.
     */
    public void startGame() {
        System.out.println("GameController: Starting game...");
        gameLogic.initializeNewGame();
        gameState.setRunning(true);
        gameState.setPaused(false);
        gameState.setGameOver(false);
        if (!gameTimer.isRunning()) {
            gameTimer.start();
            System.out.println("Game Started. Timer running (" + AppConfig.GAME_TICK_MS + "ms interval).");
        }
        gamePanel.repaint();
    }

    /**
     * Stops the main game loop timer and sets the game state's running flag to false.
     */
    public void stopGameLoop() {
        if (gameTimer.isRunning()) {
            gameTimer.stop();
            System.out.println("Game Loop Stopped.");
        }
        gameState.setRunning(false);
    }

    /**
     * Toggles the paused state of the game.
     * Only effective if the game is currently running and not game over.
     */
    public void togglePause() {
        if (!gameState.isRunning() || gameState.isGameOver()) {
            System.out.println("Cannot toggle pause. Game running: " + gameState.isRunning() + ", Game over: " + gameState.isGameOver());
            return;
        }
        boolean currentPauseState = gameState.isPaused();
        gameState.setPaused(!currentPauseState);
        System.out.println(gameState.isPaused() ? "Game Paused" : "Game Resumed");
        gamePanel.repaint();
    }

    /**
     * Handles the game over sequence. Stops the game loop, saves the score
     * (if applicable and a user is logged in), and triggers a repaint.
     */
    private void handleGameOver() {
        System.out.println("GameController: Handling Game Over...");
        stopGameLoop();

        System.out.println("Game Over! Final Score: " + gameState.getScore());

        String username = mainApp.getCurrentUsername();
        if (username != null && !username.isEmpty() && gameState.getScore() > 0) {
            System.out.println("Saving score " + gameState.getScore() + " for user: " + username);
            dbManager.saveScore(username, gameState.getScore());
        } else {
            if (username == null || username.isEmpty()) {
                System.out.println("Score not saved: No user logged in.");
            } else {
                System.out.println("Score not saved: Score is zero.");
            }
        }

        gamePanel.repaint();
    }

    /**
     * Initiates the process of exiting the current game session.
     */
    public void exitGame() {
        System.out.println("GameController: Exit requested.");
        stopGameLoop();
        mainApp.switchToLoginScreen();
    }

    /**
     * Initiates the process of restarting the game.
     */
    public void requestRestart() {
        System.out.println("GameController: Restart requested.");
        stopGameLoop();
        mainApp.switchToLoginScreen();
    }

    /**
     * The main action performed by the {@link Timer}. Called on each timer tick.
     * Updates game logic if the game is running and not paused, checks for game over,
     * and triggers repaints.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState.isRunning() && !gameState.isPaused() && !gameState.isGameOver()) {
            gameLogic.update();

            if (gameState.isGameOver()) {
                handleGameOver();
            } else {
                gamePanel.repaint();
            }
        } else if (gameState.isGameOver() || gameState.isPaused()) {
            gamePanel.repaint();
        }
    }
}