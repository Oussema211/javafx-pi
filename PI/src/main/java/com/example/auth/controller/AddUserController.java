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

        // Contrôles de saisie
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty() || rolesInput.isEmpty()) {
            messageLabel.setText("Champs requis manquants !");
            return;
        }

        if (!nom.matches("^[a-zA-ZéèàçêâÉÈÀÇÂÊ\\s-]+$")) {
            messageLabel.setText("Nom invalide !");
            return;
        }

        if (!prenom.matches("^[a-zA-ZéèàçêâÉÈÀÇÂÊ\\s-]+$")) {
            messageLabel.setText("Prénom invalide !");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[a-z]{2,6}$")) {
            messageLabel.setText("Format email invalide !");
            return;
        }

        if (password.length() < 6) {
            messageLabel.setText("Mot de passe trop court (min 6 caractères) !");
            return;
        }

        if (!numTel.isEmpty() && !numTel.matches("^\\d{8}$")) {
            messageLabel.setText("Numéro de téléphone invalide (8 chiffres) !");
            return;
        }

        if (!photoUrl.isEmpty() && !photoUrl.matches("^(http|https)://.*$")) {
            messageLabel.setText("URL photo invalide !");
            return;
        }

        List<String> roles = Arrays.asList(rolesInput.split("\\s*,\\s*"));
        if (authService.signup(email, password, travail, photoUrl, nom, prenom, numTel, roles)) {
            messageLabel.setText("Utilisateur ajouté avec succès !");
            clearFields();
            Stage stage = (Stage) nomField.getScene().getWindow();
            stage.close();
        } else {
            messageLabel.setText("Email déjà utilisé !");
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
