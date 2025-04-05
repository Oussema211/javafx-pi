package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Arrays;

public class EditUserController {
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField travailField;
    @FXML private TextField photoUrlField;
    @FXML private TextField numTelField;
    @FXML private TextField rolesField;
    @FXML private CheckBox isVerifiedCheckBox;
    @FXML private Label messageLabel;

    private AuthService authService = new AuthService();
    private SessionManager sessionManager = SessionManager.getInstance();
    private User user;

    @FXML
    public void initialize() {
        User currentUser = sessionManager.getLoggedInUser();
        if (currentUser == null || !currentUser.hasRole("ROLE_ADMIN")) {
            messageLabel.setText("Admin access required");
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.close();
        }
    }

    public void setUser(User user) {
        this.user = user;
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        travailField.setText(user.getTravail());
        photoUrlField.setText(user.getPhotoUrl());
        numTelField.setText(user.getNumTel());
        rolesField.setText(String.join(",", user.getRoles()));
        isVerifiedCheckBox.setSelected(user.isVerified());
    }

    @FXML
    private void handleUpdateUser() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String travail = travailField.getText().trim();
        String photoUrl = photoUrlField.getText().trim();
        String numTel = numTelField.getText().trim();
        String rolesInput = rolesField.getText().trim();
        boolean isVerified = isVerifiedCheckBox.isSelected();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || rolesInput.isEmpty()) {
            messageLabel.setText("Please fill in all required fields");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            messageLabel.setText("Invalid email format");
            return;
        }

        user.setNom(nom);
        user.setPrenom(prenom);
        user.setEmail(email);
        user.setTravail(travail);
        user.setPhotoUrl(photoUrl);
        user.setNumTel(numTel);
        user.setRoles(Arrays.asList(rolesInput.split("\\s*,\\s*")));
        user.setVerified(isVerified);

        // Update password only if a new one is provided
        if (!password.isEmpty()) {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            user.setPassword(hashedPassword);
        }

        if (authService.updateUser(user)) {
            messageLabel.setText("User updated successfully!");
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.close();
        } else {
            messageLabel.setText("Failed to update user");
        }
    }
}