package com.example.auth.controller;

import com.example.auth.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class CodeVerificationController {
    @FXML private TextField codeField;
    @FXML private Label messageLabel;

    private AuthService authService = new AuthService();
    private String email;

    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    private void onVerifyClicked() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            messageLabel.setText("Please enter the verification code");
            return;
        }
        if (authService.verifyUser(code, email)) {
            messageLabel.setText("Verification successful!");
            try {
                Stage stage = (Stage) codeField.getScene().getWindow();
                boolean isFullScreen = stage.isFullScreen();
                Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
                Scene scene = new Scene(root, 400, 500);
                java.net.URL cssResource = getClass().getResource("/com/example/auth/styles.css");
                if (cssResource != null) {
                    scene.getStylesheets().add(cssResource.toExternalForm());
                } else {
                    System.err.println("Warning: styles.css not found");
                }
                stage.setScene(scene);
                stage.setFullScreen(isFullScreen);
                stage.show();
            } catch (IOException e) {
                messageLabel.setText("Error loading login page");
                System.err.println("Error loading login page: " + e.getMessage());
            }
        } else {
            messageLabel.setText("Invalid or expired code");
        }
    }
}