package com.terriblegame.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import com.terriblegame.config.AppConfig;
import com.terriblegame.db.DatabaseManager;
import com.terriblegame.TerribleGame;

/**
 * Represents the Login Screen as a {@link JPanel}.
 * Provides UI elements for username and password input, login and register buttons,
 * a status message label, and a text area to display high scores.
 * Interacts with the {@link DatabaseManager} for user authentication and registration,
 * ensuring secure handling of credentials.
 */
public class LoginScreen extends JPanel implements ActionListener {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel statusLabel;
    private JTextArea highScoreTextArea;
    private final TerribleGame mainApp;
    private final DatabaseManager dbManager;

    /**
     * Constructs the LoginScreen panel.
     *
     * @param mainApp   A reference to the main {@link TerribleGame} application instance, used for callbacks. Must not be null.
     * @param dbManager The {@link DatabaseManager} instance used for user authentication and registration. Must not be null.
     * @throws IllegalArgumentException if either {@code mainApp} or {@code dbManager} is null.
     */
    public LoginScreen(TerribleGame mainApp, DatabaseManager dbManager) {
        if (mainApp == null || dbManager == null) {
            throw new IllegalArgumentException("MainApp and DatabaseManager cannot be null for LoginScreen");
        }
        this.mainApp = mainApp;
        this.dbManager = dbManager;
        setupUI();
    }

    /**
     * Initializes and lays out the Swing components for the login screen.
     * Uses absolute positioning (setLayout(null)). Configures component appearance
     * based on values from {@link AppConfig}.
     */
    private void setupUI() {
        setLayout(null);
        setBackground(AppConfig.LOGIN_BACKGROUND_COLOR);
        setPreferredSize(new Dimension(AppConfig.GAME_WIDTH, AppConfig.GAME_HEIGHT));

        // Define common layout variables
        int centerX = AppConfig.GAME_WIDTH / 2;
        int fieldWidth = 160;
        int labelWidth = 80;
        int startY = 150;
        int fieldSpacing = 40;
        int buttonWidth = 120;
        int buttonSpacing = 20;

        // Username Label and Field
        JLabel usernameLabel = createLabel("Username:", centerX - fieldWidth / 2 - labelWidth, startY);
        usernameField = new JTextField();
        usernameField.setBounds(centerX - fieldWidth / 2, startY, fieldWidth, 25);
        add(usernameLabel);
        add(usernameField);

        // Password Label and Field
        JLabel passwordLabel = createLabel("Password:", centerX - fieldWidth / 2 - labelWidth, startY + fieldSpacing);
        passwordField = new JPasswordField();
        passwordField.setBounds(centerX - fieldWidth / 2, startY + fieldSpacing, fieldWidth, 25);
        add(passwordLabel);
        add(passwordField);

        // Login and Register Buttons
        int totalButtonWidth = buttonWidth * 2 + buttonSpacing;
        loginButton = createButton("Login", centerX - totalButtonWidth / 2, startY + 2 * fieldSpacing, buttonWidth, 30);
        registerButton = createButton("Register", centerX - totalButtonWidth / 2 + buttonWidth + buttonSpacing, startY + 2 * fieldSpacing, buttonWidth, 30);
        add(loginButton);
        add(registerButton);

        // Status Label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setBounds(centerX - (fieldWidth + labelWidth) / 2, startY + 3 * fieldSpacing, fieldWidth + labelWidth, 25);
        statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
        add(statusLabel);

        // High Score Display Area
        int hsX = 50;
        int hsY = startY + 4 * fieldSpacing + 20;
        int hsWidth = AppConfig.GAME_WIDTH - 2 * hsX;
        int hsHeight = AppConfig.GAME_HEIGHT - hsY - 30;

        highScoreTextArea = new JTextArea();
        highScoreTextArea.setEditable(false);
        highScoreTextArea.setForeground(AppConfig.LOGIN_HIGHSCORE_FG_COLOR);
        highScoreTextArea.setBackground(AppConfig.LOGIN_HIGHSCORE_BG_COLOR);
        highScoreTextArea.setFont(new Font(AppConfig.LOGIN_HIGHSCORE_FONT_NAME,
                                         AppConfig.LOGIN_HIGHSCORE_FONT_STYLE,
                                         AppConfig.LOGIN_HIGHSCORE_FONT_SIZE));
        JScrollPane scrollPane = new JScrollPane(highScoreTextArea);
        scrollPane.setBounds(hsX, hsY, hsWidth, hsHeight);
        add(scrollPane);

        setFocusable(true);
    }

    /**
     * Helper method to create and configure a {@link JLabel} for the login form.
     */
    private JLabel createLabel(String text, int x, int y) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, 80, 25);
        label.setForeground(AppConfig.LOGIN_LABEL_COLOR);
        return label;
    }

    /**
     * Helper method to create and configure a {@link JButton} for the login form.
     */
    private JButton createButton(String text, int x, int y, int w, int h) {
        JButton button = new JButton(text);
        button.setBounds(x, y, w, h);
        button.addActionListener(this);
        return button;
    }

    /**
     * Fetches the latest high scores from the {@link DatabaseManager} and updates
     * the {@code highScoreTextArea}.
     */
    public void refreshHighScores() {
        List<String> scores = dbManager.getHighScores();
        highScoreTextArea.setText("--- High Scores (Top " + AppConfig.HIGH_SCORE_LIMIT + ") ---\n");
        if (scores != null) {
            for (String scoreLine : scores) {
                highScoreTextArea.append(scoreLine + "\n");
            }
        } else {
            highScoreTextArea.append("Error loading scores.\n");
        }
        highScoreTextArea.setCaretPosition(0);
    }

    /**
     * Clears the username field, password field, and status label text.
     */
    public void clearForm() {
        usernameField.setText("");
        passwordField.setText("");
        statusLabel.setText("");
        statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
    }

    /**
     * Handles action events for login and register buttons.
     * Uses secure practices for password handling and displays appropriate status messages.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        String username = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        String plainTextPassword = new String(passwordChars);

        try {
            if (username.isEmpty() || plainTextPassword.isEmpty()) {
                statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                statusLabel.setText("Username and password cannot be empty.");
                java.util.Arrays.fill(passwordChars, ' ');
                return;
            }

            if (e.getSource() == loginButton) {
                if (dbManager.validateUser(username, plainTextPassword)) {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                    statusLabel.setText("Login Successful!");
                    Timer switchTimer = new Timer(500, ae -> mainApp.userLoggedIn(username));
                    switchTimer.setRepeats(false);
                    switchTimer.start();
                } else {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                    statusLabel.setText("Login failed. Check credentials.");
                    passwordField.setText("");
                }
            } else if (e.getSource() == registerButton) {
                if (dbManager.registerUser(username, plainTextPassword)) {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_SUCCESS_COLOR);
                    statusLabel.setText("Registration successful! Please log in.");
                    usernameField.setText("");
                    passwordField.setText("");
                } else {
                    statusLabel.setForeground(AppConfig.LOGIN_STATUS_ERROR_COLOR);
                    statusLabel.setText("Registration failed (username might exist or error occurred).");
                    passwordField.setText("");
                }
            }
        } finally {
            java.util.Arrays.fill(passwordChars, ' ');
        }
    }
}