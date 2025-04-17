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

    private final SessionManager sessionManager = SessionManager.getInstance();
    private final AuthService authService = new AuthService();
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

        // Load profile picture
        loadProfilePicture();

        // Set form fields
        emailField.setText(currentUser.getEmail());
        passwordField.setText("");
    }

    private void loadProfilePicture() {
        String profilePhotoPath = currentUser.getProfilePhotoPath();
        System.out.println("Profile photo path: " + profilePhotoPath);

        if (profilePhotoPath != null && !profilePhotoPath.isEmpty()) {
            try {
                // Load image directly from path or file
                Image image;
                if (profilePhotoPath.startsWith("/")) {
                    // Resource path
                    if (getClass().getResource(profilePhotoPath) == null) {
                        System.err.println("Resource not found: " + profilePhotoPath);
                        loadFallbackImage();
                        return;
                    }
                    image = new Image(getClass().getResourceAsStream(profilePhotoPath));
                } else {
                    // File path
                    image = new Image("file:" + profilePhotoPath);
                }

                if (!image.isError()) {
                    profilePicture.setImage(image);
                    System.out.println("Profile picture loaded successfully");
                } else {
                    System.err.println("Error loading profile image: Image is corrupted or invalid");
                    loadFallbackImage();
                }
            } catch (Exception e) {
                System.err.println("Error loading profile image: " + e.getMessage());
                loadFallbackImage();
            }
        } else {
            System.err.println("Profile photo path is null or empty");
            loadFallbackImage();
        }
    }

    private void loadFallbackImage() {
        String fallbackPath = "/com/example/images/download.png";
        System.out.println("Attempting to load fallback image: " + fallbackPath);

        try {
            if (getClass().getResource(fallbackPath) == null) {
                System.err.println("Fallback resource not found: " + fallbackPath);
                applyCssFallback();
                return;
            }

            Image fallbackImage = new Image(getClass().getResourceAsStream(fallbackPath));
            if (!fallbackImage.isError()) {
                profilePicture.setImage(fallbackImage);
                System.out.println("Fallback image loaded successfully");
            } else {
                System.err.println("Fallback image is corrupted or invalid");
                applyCssFallback();
            }
        } catch (Exception e) {
            System.err.println("Error loading fallback image: " + e.getMessage());
            applyCssFallback();
        }
    }

    private void applyCssFallback() {
        messageLabel.setText("Unable to load profile picture");
        profilePicture.setImage(null);
        profilePicture.setStyle("-fx-background-color: #cccccc; -fx-border-radius: 50%; -fx-background-radius: 50%;");
        System.out.println("Applied CSS fallback for profile picture");
    }

    @FXML
    private void goToDashboard() throws IOException {
        System.out.println("Navigating to Dashboard");
        Stage stage = (Stage) backButton.getScene().getWindow();
        boolean isFullScreen = stage.isFullScreen();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auth/DashboardFront.fxml"));
        if (loader.getLocation() == null) {
            System.err.println("DashboardFront.fxml not found at /com/example/auth/DashboardFront.fxml");
            throw new IOException("DashboardFront.fxml resource not found");
        }

        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/example/auth/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Dashboard");
        stage.setFullScreen(isFullScreen);
        stage.show();
        System.out.println("Navigated to dashboard");
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