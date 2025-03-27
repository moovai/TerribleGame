package com.terriblegame;

import javax.swing.*;
import java.awt.*;
import com.terriblegame.config.AppConfig;
import com.terriblegame.controller.GameController;
import com.terriblegame.db.DatabaseManager;
import com.terriblegame.game.GameLogic;
import com.terriblegame.game.GameState;
import com.terriblegame.input.InputHandler;
import com.terriblegame.ui.GamePanel;
import com.terriblegame.ui.LoginScreen;

/**
 * The main application class for the game, extending {@link JFrame}.
 * Sets up the main window, manages different UI panels (LoginScreen, GamePanel) using a {@link CardLayout},
 * initializes and coordinates core game components, handles transitions between login and game screens,
 * maintains the currently logged-in user's state, and manages the application lifecycle.
 */
public class TerribleGame extends JFrame {
    // Core components
    private final GameState gameState;
    private final GameLogic gameLogic;
    private final GameController gameController;
    private final InputHandler inputHandler;
    private final DatabaseManager dbManager;

    // UI Panels
    private final GamePanel gamePanel;
    private final LoginScreen loginScreen;

    // UI Management
    private CardLayout cardLayout;
    private final JPanel mainPanel;

    // Session State
    private String currentUsername = null;

    /**
     * Constructs the main application window {@code TerribleGame}.
     * Initializes all core components, sets up the JFrame properties, configures the CardLayout,
     * adds panels, sets up listeners, and displays the window.
     */
    public TerribleGame() {
        super(AppConfig.APP_TITLE);

        // Initialize Core Components
        dbManager = new DatabaseManager();
        gameState = new GameState();
        gameLogic = new GameLogic(gameState);
        loginScreen = new LoginScreen(this, dbManager);
        gamePanel = new GamePanel(gameState);
        gameController = new GameController(gameState, gameLogic, gamePanel, dbManager, this);
        inputHandler = new InputHandler(gameState, gameController);

        // Setup Window and Layout
        setupWindow();
        mainPanel = setupCardLayoutAndPanels();
        add(mainPanel, BorderLayout.CENTER);

        // Add Listeners
        gamePanel.addKeyListener(inputHandler);

        // Finalize and Display
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        // Initial State
        switchToLoginScreen();
    }

    /**
     * Configures the main JFrame properties, including the close operation,
     * resizability, and focusability.
     */
    private void setupWindow() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                System.out.println("Window closing event received. Initiating shutdown...");
                shutdown();
            }
        });
        setResizable(false);
        setFocusable(false);
    }

    /**
     * Creates the main content panel with a {@link CardLayout} and adds the panels.
     */
    private JPanel setupCardLayoutAndPanels() {
        cardLayout = new CardLayout();
        JPanel panel = new JPanel(cardLayout);
        panel.add(loginScreen, AppConfig.LOGIN_PANEL_ID);
        panel.add(gamePanel, AppConfig.GAME_PANEL_ID);
        return panel;
    }

    /**
     * Switches the view to the {@link GamePanel} and starts the game.
     */
    public void switchToGameScreen() {
        System.out.println("Switching to Game Screen...");
        cardLayout.show(mainPanel, AppConfig.GAME_PANEL_ID);
        SwingUtilities.invokeLater(() -> {
            boolean focused = gamePanel.requestFocusInWindow();
            if (focused) {
                System.out.println("GamePanel focus requested successfully.");
            } else {
                System.err.println("Warning: GamePanel failed to gain focus. Key input might not work.");
            }
            gameController.startGame();
        });
    }

    /**
     * Switches the view back to the {@link LoginScreen}.
     */
    public void switchToLoginScreen() {
        System.out.println("Switching to Login Screen...");
        gameController.stopGameLoop();
        this.currentUsername = null;
        loginScreen.clearForm();
        loginScreen.refreshHighScores();
        cardLayout.show(mainPanel, AppConfig.LOGIN_PANEL_ID);
        SwingUtilities.invokeLater(() -> usernameFieldRequestFocus(loginScreen));
    }

    /**
     * Helper method to request focus for the username field within the LoginScreen.
     */
    private void usernameFieldRequestFocus(LoginScreen login) {
        Component[] components = login.getComponents();
        Component userField = null;

        for (int i = 0; i < components.length - 1; i++) {
            if (components[i] instanceof JLabel && "Username:".equals(((JLabel)components[i]).getText()) &&
                components[i+1] instanceof JTextField) {
                userField = components[i+1];
                break;
            }
        }

        if (userField == null) {
            for (Component comp : components) {
                if (comp instanceof JTextField) {
                    userField = comp;
                    break;
                }
            }
        }

        if (userField != null) {
            userField.requestFocusInWindow();
        } else {
            login.requestFocusInWindow();
        }
    }

    /**
     * Callback method invoked by {@link LoginScreen} upon successful user login.
     */
    public void userLoggedIn(String username) {
        if (username == null || username.trim().isEmpty()) {
            System.err.println("Login attempt with invalid username passed to userLoggedIn.");
            return;
        }
        this.currentUsername = username;
        System.out.println("User '" + username + "' logged in. Proceeding to game.");
        switchToGameScreen();
    }

    /**
     * Gets the username of the currently logged-in player.
     */
    public String getCurrentUsername() {
        return currentUsername;
    }

    /**
     * Handles the graceful shutdown sequence for the application.
     */
    public void shutdown() {
        System.out.println("MainApp: Initiating shutdown...");
        gameController.stopGameLoop();
        System.out.println("Database connections managed per-operation (try-with-resources). No global close needed.");
        dispose();
        System.out.println("JFrame disposed.");
        System.out.println("Shutdown complete. Exiting JVM.");
        System.exit(0);
    }

    /**
     * The main entry point of the application.
     */
    public static void main(String[] args) {
        System.out.println("Application starting with title: " + AppConfig.APP_TITLE);
        System.out.println("NOTE: Ensure the necessary libraries (e.g., jBCrypt, dotenv-java, database driver) are included in the classpath.");

        SwingUtilities.invokeLater(() -> {
            new TerribleGame();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("JVM Shutdown Hook executing...");
            }, "ShutdownCleanupThread"));
        });
    }
}