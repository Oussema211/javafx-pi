package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.IOException;

public class ProfileController {
    @FXML private ImageView profilePicture;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label workLabel;
    @FXML private Label phoneLabel;
    @FXML private TextField emailField;
    @FXML private TextField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button backButton;
    @FXML private Button saveSettingsButton;

    private SessionManager sessionManager = SessionManager.getInstance();
    private AuthService authService = new AuthService();
    private User currentUser;

    @FXML
    public void initialize() {
        currentUser = sessionManager.getLoggedInUser();
        if (currentUser == null) {
            messageLabel.setText("No user logged in");
            return;
        }

        // Set profile details
        nameLabel.setText("Name: " + currentUser.getNom() + " " + currentUser.getPrenom());
        emailLabel.setText("Email: " + currentUser.getEmail());
        workLabel.setText("Work: " + (currentUser.getTravail() != null ? currentUser.getTravail() : "N/A"));
        phoneLabel.setText("Phone: " + (currentUser.getNumTel() != null ? currentUser.getNumTel() : "N/A"));

        // Load profile picture as a resource
        String profilePhotoPath = currentUser.getProfilePhotoPath();
        if (profilePhotoPath != null && !profilePhotoPath.isEmpty()) {
            System.out.println("Attempting to load profile picture from resource: " + profilePhotoPath);
            try {
                // Load the image as a resource from the classpath
                Image image = new Image(getClass().getResourceAsStream(profilePhotoPath));
                if (!image.isError()) {
                    profilePicture.setImage(image);
                } else {
                    System.err.println("Error loading profile image: Image is corrupted or invalid.");
                    loadFallbackImage();
                }
            } catch (Exception e) {
                System.err.println("Error loading profile image: " + e.getMessage());
                loadFallbackImage();
            }
        } else {
            System.err.println("Profile photo path is null or empty.");
            loadFallbackImage();
        }

        // Set form fields
        emailField.setText(currentUser.getEmail());
        passwordField.setText("");
    }

    // Load a fallback image if the user's profile picture fails to load
    private void loadFallbackImage() {
        try {
            // Attempt to load the fallback image as a resource
            Image fallbackImage = new Image(getClass().getResourceAsStream("/com/example/images/default_profile.jpg"));
            if (!fallbackImage.isError()) {
                profilePicture.setImage(fallbackImage);
            } else {
                System.err.println("Fallback image is corrupted or invalid.");
                messageLabel.setText("Unable to load profile picture.");
                // Use CSS fallback
                profilePicture.setStyle("-fx-background-color: #cccccc; -fx-border-radius: 50%; -fx-background-radius: 50%;");
            }
        } catch (Exception e) {
            System.err.println("Error loading fallback image: " + e.getMessage());
            messageLabel.setText("Unable to load profile picture.");
            // Use CSS fallback
            profilePicture.setStyle("-fx-background-color: #cccccc; -fx-border-radius: 50%; -fx-background-radius: 50%;");
        }
    }

    @FXML
    private void goToDashboard() throws IOException {
        Stage stage = (Stage) backButton.getScene().getWindow();
        boolean isFullScreen = stage.isFullScreen();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/pages/dashboard.fxml"));
        Scene scene = new Scene(root, 400, 500);
        scene.getStylesheets().add(getClass().getClassLoader().getResource("com/example/auth/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(isFullScreen);
        stage.show();
    }

    @FXML
    private void saveSettings() {
        String newEmail = emailField.getText().trim();
        String newPassword = passwordField.getText().trim();

        if (newEmail.isEmpty()) {
            messageLabel.setText("Email cannot be empty");
            return;
        }

        currentUser.setEmail(newEmail);
        boolean emailUpdated = authService.updateUserEmail(currentUser.getEmail(), newEmail);
        if (!emailUpdated) {
            messageLabel.setText("Failed to update email");
            return;
        }

        if (!newPassword.isEmpty()) {
            boolean passwordUpdated = authService.updateUserPassword(currentUser.getEmail(), newPassword);
            if (!passwordUpdated) {
                messageLabel.setText("Failed to update password");
                return;
            }
        }

        emailLabel.setText("Email: " + newEmail);
        messageLabel.setText("Settings updated successfully");
    }
}