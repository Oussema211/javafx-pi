package com.example.auth.controller;

import com.example.auth.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
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

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please fill in all required fields (Nom, Prénom, Email, Password)");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            messageLabel.setText("Invalid email format");
            return;
        }

        // Assign default role: ROLE_USER
        if (authService.signup(email, password, travail, photoUrl, nom, prenom, numTel, Collections.singletonList("ROLE_USER"))) {
            messageLabel.setText("Signup successful! Please login.");
            clearFields();
        } else {
            messageLabel.setText("Email already exists");
        }
    }

    @FXML
    private void switchToLogin() throws IOException {
        Stage stage = (Stage) emailField.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
        stage.setScene(new Scene(root));
    }

    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwordField.clear();
        travailField.clear();
        photoUrlField.clear();
        numTelField.clear();
    }
}