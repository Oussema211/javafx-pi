package com.example.auth.controller;

import com.example.auth.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Hyperlink;
import javafx.stage.Stage;

import java.io.IOException;

public class VerificationController {
    @FXML private TextField codeField;
    @FXML private Label statusLabel;
    @FXML private Hyperlink loginLink;

    private AuthService authService = new AuthService();
    private String userEmail; // Set during signup

    public void initializeVerification(String email) {
        this.userEmail = email;
    }

    @FXML
    private void onVerifyClicked() {
        String code = codeField.getText().trim();
        if (code.length() != 6 || !code.matches("\\d+")) {
            statusLabel.setText("Please enter a valid 6-digit code.");
            statusLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        boolean verified = authService.verifyUser(userEmail, code);
        if (verified) {
            statusLabel.setText("Email verified successfully! You can now log in.");
            statusLabel.setStyle("-fx-text-fill: green;");
        } else {
            statusLabel.setText("Invalid or expired code.");
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    private void onLoginClicked() {
        try {
            Stage stage = (Stage) loginLink.getScene().getWindow();
            boolean isFullScreen = stage.isFullScreen();
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getClassLoader().getResource("com/example/auth/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreen(isFullScreen);
            stage.show();
        } catch (IOException e) {
            statusLabel.setText("Error loading login page");
            statusLabel.setStyle("-fx-text-fill: red;");
            System.err.println("Error loading login page: " + e.getMessage());
        }
    }
}