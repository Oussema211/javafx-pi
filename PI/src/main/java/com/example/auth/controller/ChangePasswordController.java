package com.example.auth.controller;

import com.example.auth.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;

public class ChangePasswordController {
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label messageLabel;

    private AuthService authService = new AuthService();
    private String token;

    public void setToken(String token) {
        this.token = token;
    }

    @FXML
    private void handleChangePassword() {
        String newPassword = newPasswordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            messageLabel.setText("Please fill in both fields");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match");
            return;
        }

        boolean success = authService.resetPasswordWithToken(token, newPassword);
        if (success) {
            messageLabel.setText("Password changed successfully! Please log in.");
        } else {
            messageLabel.setText("Invalid or expired token");
        }
    }
}