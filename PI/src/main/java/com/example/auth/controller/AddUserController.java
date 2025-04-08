package com.example.auth.controller;

import com.example.auth.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class AddUserController {
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField travailField;
    @FXML private TextField photoUrlField;
    @FXML private TextField numTelField;
    @FXML private TextField rolesField;
    @FXML private Label messageLabel;

    private AuthService authService = new AuthService();

    @FXML
    private void handleAddUser() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();
        String travail = travailField.getText().trim();
        String photoUrl = photoUrlField.getText().trim();
        String numTel = numTelField.getText().trim();
        String rolesInput = rolesField.getText().trim();

        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || rolesInput.isEmpty()) {
            messageLabel.setText("Please fill in all required fields");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            messageLabel.setText("Invalid email format");
            return;
        }

        List<String> roles = Arrays.asList(rolesInput.split("\\s*,\\s*"));
        if (authService.signup(email, password, travail, photoUrl, nom, prenom, numTel, roles)) {
            messageLabel.setText("User added successfully!");
            clearFields();
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.close();
        } else {
            messageLabel.setText("Email already exists");
        }
    }

    private void clearFields() {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwordField.clear();
        travailField.clear();
        photoUrlField.clear();
        numTelField.clear();
        rolesField.clear();
    }
}