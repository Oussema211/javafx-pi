package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button toggleFullScreenButton;

    private AuthService authService = new AuthService();
    private SessionManager sessionManager = SessionManager.getInstance();

    @FXML
    private void handleLogin() throws IOException {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter email and password");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            messageLabel.setText("Invalid email format");
            return;
        }

        User user = authService.login(email, password);
        if (user == null) {
            messageLabel.setText("Invalid email or password");
            return;
        }

        sessionManager.setLoggedInUser(user);

        // Redirect based on user role
        String fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/reclamation/Reclamation.fxml";
        Stage stage = (Stage) emailField.getScene().getWindow();
        boolean isFullScreen = stage.isFullScreen();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Scene scene = new Scene(root, 400, 500);

        // Load stylesheet
        java.net.URL stylesheetUrl = getClass().getClassLoader().getResource("com/example/auth/styles.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            System.out.println("DEBUG: Could not find styles.css in handleLogin");
        }

        stage.setScene(scene);
        stage.setFullScreen(isFullScreen);
        stage.show();
    }

    @FXML
    private void switchToSignup() {
        try {
            System.out.println("DEBUG: Switching to signup screen");
            Stage stage = (Stage) emailField.getScene().getWindow();
            boolean isFullScreen = stage.isFullScreen();
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/signup.fxml"));
            if (root == null) {
                System.out.println("DEBUG: Failed to load signup.fxml - root is null");
                return;
            }
            Scene scene = new Scene(root, 400, 500);

            // Load stylesheet
            java.net.URL stylesheetUrl = getClass().getClassLoader().getResource("com/example/auth/styles.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            } else {
                System.out.println("DEBUG: Could not find styles.css in switchToSignup");
            }

            stage.setScene(scene);
            stage.setFullScreen(isFullScreen);
            stage.show();
        } catch (IOException e) {
            System.out.println("DEBUG: Error switching to signup screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void toggleFullScreen() {
        Stage stage = (Stage) emailField.getScene().getWindow();
        boolean isFullScreen = stage.isFullScreen();
        stage.setFullScreen(!isFullScreen); // Toggle full-screen state
        toggleFullScreenButton.setText(isFullScreen ? "Toggle Full Screen" : "Exit Full Screen");
        System.out.println("DEBUG: Toggled full-screen mode to: " + !isFullScreen);
    }
}