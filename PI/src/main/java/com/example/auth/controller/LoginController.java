package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.utils.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField usernameField; // Changed from emailField to match FXML
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheckBox;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;
    @FXML private Label messageLabel; // We'll need to add this to the FXML

    private AuthService authService = new AuthService();
    private SessionManager sessionManager = SessionManager.getInstance();

    @FXML
    private void onLoginClicked() throws IOException { // Changed from handleLogin to match FXML
        String username = usernameField.getText().trim(); // Changed from email to username
        String password = passwordField.getText().trim();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter username and password");
            return;
        }

        // Since the FXML uses username instead of email, we'll assume the AuthService can handle username-based login
        // If AuthService strictly requires email, you'll need to modify the FXML to use emailField instead
        User user = authService.login(username, password);
        if (user == null) {
            messageLabel.setText("Invalid username or password");
            return;
        }

        sessionManager.setLoggedInUser(user);

        // Redirect based on user role
        String fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/frontPages/dashboard.fxml";
        Stage stage = (Stage) usernameField.getScene().getWindow();
        boolean isFullScreen = stage.isFullScreen();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        Scene scene = new Scene(root, 700, 700);

        // Load stylesheet
        java.net.URL stylesheetUrl = getClass().getClassLoader().getResource("com/example/auth/styles.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            System.out.println("DEBUG: Could not find styles.css in onLoginClicked");
        }

        stage.setScene(scene);
        stage.setFullScreen(isFullScreen);
        stage.show();
    }

    @FXML
    private void onForgotPasswordClicked() { // Changed from handleForgotPassword to match FXML
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auth/resetPassword.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Reset Password");
            stage.setScene(new Scene(root, 700, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRegisterClicked() { // Changed from switchToSignup to match FXML
        try {
            System.out.println("DEBUG: Switching to signup screen");
            Stage stage = (Stage) usernameField.getScene().getWindow();
            boolean isFullScreen = stage.isFullScreen();
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/signup.fxml"));
            if (root == null) {
                System.out.println("DEBUG: Failed to load signup.fxml - root is null");
                return;
            }
            Scene scene = new Scene(root, 700, 700);

            // Load stylesheet
            java.net.URL stylesheetUrl = getClass().getClassLoader().getResource("com/example/auth/styles.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            } else {
                System.out.println("DEBUG: Could not find styles.css in onRegisterClicked");
            }

            stage.setScene(scene);
            stage.setFullScreen(isFullScreen);
            stage.show();
        } catch (IOException e) {
            System.out.println("DEBUG: Error switching to signup screen: " + e.getMessage());
            e.printStackTrace();
        }
    }
}