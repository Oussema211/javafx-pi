package com.example.auth.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.stage.Stage;

import java.io.IOException;

public class WaitingController {
    @FXML private Hyperlink verifyLink;
    @FXML private Hyperlink loginLink;

    private String userEmail; // Set during signup

    public void initialize(String email) {
        this.userEmail = email;
    }

    @FXML
    private void onVerifyClicked() {
        try {
            Stage stage = (Stage) verifyLink.getScene().getWindow();
            boolean isFullScreen = stage.isFullScreen();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auth/verify.fxml"));
            Parent root = loader.load();
            VerificationController controller = loader.getController();
            controller.initializeVerification(userEmail);
            Scene scene = new Scene(root, 400, 500);
            scene.getStylesheets().add(getClass().getClassLoader().getResource("com/example/auth/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setFullScreen(isFullScreen);
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading verification page: " + e.getMessage());
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
            System.err.println("Error loading login page: " + e.getMessage());
        }
    }
}