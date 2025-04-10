package com.example.auth.controller;

import com.example.auth.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ResetPasswordController {
    @FXML private TextField emailField;
    @FXML private TextField numTelField;
    @FXML private Label messageLabel;

    private AuthService authService = new AuthService();

    @FXML
    private void handleResetRequest() {
        String email = emailField.getText().trim();
        String numTel = numTelField.getText().trim();

        if (email.isEmpty() || numTel.isEmpty()) {
            messageLabel.setText("Please enter both email and phone number");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            messageLabel.setText("Invalid email format");
            return;
        }

        boolean success = authService.requestPasswordReset(email, numTel);
        if (success) {
            messageLabel.setText("A reset link has been sent to your email");
        } else {
            messageLabel.setText("Invalid email or phone number");
        }
    }
}