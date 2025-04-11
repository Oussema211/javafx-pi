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
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button signupButton;
    @FXML private Hyperlink loginLink;
    @FXML private Label messageLabel; // We'll need to add this to the FXML

    private AuthService authService = new AuthService();

    @FXML
    private void onSignupClicked() { // Changed from handleSignup to match FXML
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        // Split fullName into nom and prenom (assuming format "FirstName LastName")
        String[] nameParts = fullName.split("\\s+");
        String prenom = nameParts.length > 0 ? nameParts[0] : "";
        String nom = nameParts.length > 1 ? nameParts[1] : "";
        String travail = ""; // Not in FXML, set to empty or default
        String photoUrl = ""; // Not in FXML, set to empty or default
        String numTel = ""; // Not in FXML, set to empty or default

        // Validation: Full Name
        if (fullName.isEmpty()) {
            messageLabel.setText("Full Name is required");
            return;
        }
        if (!nom.matches("^[A-Za-z\\u00C0-\\u00FF\\s-]+$") || !prenom.matches("^[A-Za-z\\u00C0-\\u00FF\\s-]+$")) {
            messageLabel.setText("Full Name must contain only letters, accented letters, spaces, or hyphens");
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

        // Validation: Username
        if (username.isEmpty()) {
            messageLabel.setText("Username is required");
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

        // Validation: Confirm Password
        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Passwords do not match");
            return;
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
    private void onLoginClicked() { // Changed from switchToLogin to match FXML
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
                System.out.println("DEBUG: Could not find styles.css in onLoginClicked");
            }

            stage.setScene(scene);
            stage.setFullScreen(isFullScreen);
            stage.show();
        } catch (IOException e) {
            System.out.println("DEBUG: Error switching to login screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearFields() {
        fullNameField.clear();
        emailField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        messageLabel.setText("");
    }
}