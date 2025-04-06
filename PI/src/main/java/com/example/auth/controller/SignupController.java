package com.example.auth.controller;

import com.example.auth.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

public class SignupController {
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField travailField;
    @FXML private TextField photoUrlField;
    @FXML private TextField numTelField;
    @FXML private Label messageLabel;
    @FXML private Button toggleFullScreenButton;

    private AuthService authService = new AuthService();

    @FXML
    private void handleSignup() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String travail = travailField.getText().trim();
        String photoUrl = photoUrlField.getText().trim();
        String numTel = numTelField.getText().trim();

        // Validation: Nom (Last Name)
        if (nom.isEmpty()) {
            messageLabel.setText("Nom (Last Name) is required");
            return;
        }
        if (!nom.matches("^[A-Za-zÀ-ÿ\\s-]+$")) {
            messageLabel.setText("Nom must contain only letters, spaces, or hyphens");
            return;
        }

        // Validation: Prénom (First Name)
        if (prenom.isEmpty()) {
            messageLabel.setText("Prénom (First Name) is required");
            return;
        }
        if (!prenom.matches("^[A-Za-zÀ-ÿ\\s-]+$")) {
            messageLabel.setText("Prénom must contain only letters, spaces, or hyphens");
            return;
        }

        // Validation: Email
        if (email.isEmpty()) {
            messageLabel.setText("Email is required");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            messageLabel.setText("Invalid email format");
            return;
        }

        // Validation: Password
        if (password.isEmpty()) {
            messageLabel.setText("Password is required");
            return;
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            messageLabel.setText("Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one digit");
            return;
        }

        // Validation: Numéro de Téléphone
        if (numTel.isEmpty()) {
            messageLabel.setText("Numéro de Téléphone is required");
            return;
        }
        if (!numTel.matches("^(\\+\\d{1,3}[- ]?)?\\d{10}$|^\\d{3}-\\d{3}-\\d{4}$")) {
            messageLabel.setText("Invalid phone number format (e.g., +1234567890 or 123-456-7890)");
            return;
        }

        // Validation: Photo URL (optional, but if provided, must be a valid URL)
        if (!photoUrl.isEmpty()) {
            try {
                new URL(photoUrl).toURI();
            } catch (Exception e) {
                messageLabel.setText("Invalid Photo URL format");
                return;
            }
        }

        // Show confirmation dialog
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Signup");
        confirmation.setHeaderText("Confirm Your Details");
        confirmation.setContentText("Are you sure you want to sign up with these details?\nEmail: " + email);
        if (confirmation.showAndWait().get() != ButtonType.OK) {
            return;
        }

        // If all validations pass, proceed with signup
        if (authService.signup(email, password, travail, photoUrl, nom, prenom, numTel, Collections.singletonList("ROLE_USER"))) {
            messageLabel.setText("Signup successful! Please login.");
            clearFields();
        } else {
            messageLabel.setText("Email already exists");
        }
    }

    @FXML
    private void switchToLogin() {
        try {
            System.out.println("DEBUG: Switching to login screen");
            Stage stage = (Stage) emailField.getScene().getWindow();
            boolean isFullScreen = stage.isFullScreen();
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
            if (root == null) {
                System.out.println("DEBUG: Failed to load login.fxml - root is null");
                return;
            }
            Scene scene = new Scene(root, 400, 500);

            // Load stylesheet
            java.net.URL stylesheetUrl = getClass().getClassLoader().getResource("com/example/auth/styles.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            } else {
                System.out.println("DEBUG: Could not find styles.css in switchToLogin");
            }

            stage.setScene(scene);
            stage.setFullScreen(isFullScreen);
            stage.show();
        } catch (IOException e) {
            System.out.println("DEBUG: Error switching to login screen: " + e.getMessage());
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

    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwordField.clear();
        travailField.clear();
        photoUrlField.clear();
        numTelField.clear();
        messageLabel.setText("");
    }
}