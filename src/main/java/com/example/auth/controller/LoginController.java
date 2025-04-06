package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private AuthService authService = new AuthService();
    private SessionManager sessionManager = SessionManager.getInstance();

    @FXML
    private void handleLogin() throws IOException {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter email and password");
            return;
        }

        User user = authService.login(email, password);
        if (user == null) {
            messageLabel.setText("Invalid email or password");
            return;
        }

        sessionManager.setLoggedInUser(user);

        // Redirect based on user role
        String fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/adminDashboard.fxml" : "/com/example/auth/dashboard.fxml";
        Stage stage = (Stage) emailField.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        stage.setScene(new Scene(root));
    }

    @FXML
    private void switchToSignup() throws IOException {
        Stage stage = (Stage) emailField.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/signup.fxml"));
        stage.setScene(new Scene(root));
    }
}