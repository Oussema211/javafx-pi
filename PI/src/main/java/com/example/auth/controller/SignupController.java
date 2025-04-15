package com.example.auth.controller;

import com.example.auth.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

public class SignupController {
    @FXML private TextField prenomField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField travailField;
    @FXML private TextField numTelField;
    @FXML private Button signupButton;
    @FXML private Hyperlink loginLink;
    @FXML private Label messageLabel;
    @FXML private Button choosePhotoButton;
    @FXML private ImageView photoPreview;
    @FXML private Label photoErrorLabel;

    private AuthService authService = new AuthService();
    private File selectedPhoto;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
    private static final String[] ALLOWED_EXTENSIONS = {".png", ".jpg", ".jpeg"};

    @FXML
    private void onChoosePhotoClicked() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Fichiers image", "*.png", "*.jpg", "*.jpeg")
        );
        Stage stage = (Stage) choosePhotoButton.getScene().getWindow();
        selectedPhoto = fileChooser.showOpenDialog(stage);

        if (selectedPhoto != null) {
            if (!validatePhoto(selectedPhoto)) {
                selectedPhoto = null;
                photoPreview.setImage(null);
                return;
            }

            try {
                Image image = new Image(selectedPhoto.toURI().toString(), 50, 50, true, true);
                photoPreview.setImage(image);
                photoErrorLabel.setVisible(false);
            } catch (Exception e) {
                photoErrorLabel.setText("Erreur lors du chargement de l’image : " + e.getMessage());
                photoErrorLabel.setVisible(true);
                selectedPhoto = null;
            }
        }
    }

    private boolean validatePhoto(File file) {
        String fileName = file.getName().toLowerCase();
        boolean isValidExtension = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                isValidExtension = true;
                break;
            }
        }
        if (!isValidExtension) {
            photoErrorLabel.setText("Seuls les fichiers PNG, JPG et JPEG sont autorisés");
            photoErrorLabel.setVisible(true);
            return false;
        }

        if (file.length() > MAX_FILE_SIZE) {
            photoErrorLabel.setText("La taille du fichier dépasse la limite de 5 Mo");
            photoErrorLabel.setVisible(true);
            return false;
        }

        return true;
    }

    @FXML
    private void onSignupClicked() {
        String prenom = prenomField.getText().trim();
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String travail = travailField.getText().trim();
        String numTel = numTelField.getText().trim();
        String photoUrl = "";

        if (prenom.isEmpty()) {
            messageLabel.setText("Le prénom est requis");
            return;
        }
        if (!prenom.matches("^[A-Za-z\\u00C0-\\u00FF\\s-]+$")) {
            messageLabel.setText("Le prénom ne doit contenir que des lettres, espaces ou tirets");
            return;
        }

        if (nom.isEmpty()) {
            messageLabel.setText("Le nom est requis");
            return;
        }
        if (!nom.matches("^[A-Za-z\\u00C0-\\u00FF\\s-]+$")) {
            messageLabel.setText("Le nom ne doit contenir que des lettres, espaces ou tirets");
            return;
        }

        if (email.isEmpty()) {
            messageLabel.setText("L'email est requis");
            return;
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            messageLabel.setText("Format de l'email invalide");
            return;
        }

        if (username.isEmpty()) {
            messageLabel.setText("Le nom d'utilisateur est requis");
            return;
        }

        if (password.isEmpty()) {
            messageLabel.setText("Le mot de passe est requis");
            return;
        }
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$")) {
            messageLabel.setText("Le mot de passe doit contenir au moins 8 caractères, dont une majuscule, une minuscule et un chiffre");
            return;
        }

        if (!password.equals(confirmPassword)) {
            messageLabel.setText("Les mots de passe ne correspondent pas");
            return;
        }

        if (travail.isEmpty()) {
            travail = "Non spécifié";
        }

        if (numTel.isEmpty()) {
            messageLabel.setText("Le numéro de téléphone est requis");
            return;
        }
        if (!numTel.matches("^\\d{8}$")) {
            messageLabel.setText("Le numéro de téléphone doit contenir exactement 8 chiffres");
            return;
        }

        if (selectedPhoto != null) {
            try {
                String targetDir = "src/main/resources/com/example/auth/images/users/";
                File dir = new File(targetDir);
                if (!dir.exists()) {
                    if (!dir.mkdirs()) {
                        messageLabel.setText("Erreur : Impossible de créer le dossier pour les images.");
                        return;
                    }
                }

                String fileExtension = selectedPhoto.getName().substring(selectedPhoto.getName().lastIndexOf("."));
                String newFileName = username + "_" + System.currentTimeMillis() + fileExtension;
                Path targetPath = Paths.get(targetDir, newFileName);

                Files.copy(selectedPhoto.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Profile photo saved at: " + targetPath.toAbsolutePath());

                photoUrl = "/com/example/auth/images/users/" + newFileName;
            } catch (IOException e) {
                messageLabel.setText("Erreur lors de l’enregistrement de la photo : " + e.getMessage());
                e.printStackTrace();
                return;
            }
        } else {
            photoUrl = "/com/example/images/default_profile.jpg";
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation d'inscription");
        confirmation.setHeaderText("Veuillez confirmer vos informations");
        confirmation.setContentText("Êtes-vous sûr de vouloir vous inscrire avec cet email :\n" + email);
        if (confirmation.showAndWait().get() != ButtonType.OK) {
            return;
        }

        if (authService.signup(email, password, travail, photoUrl, nom, prenom, numTel, Collections.singletonList("ROLE_USER"))) {
            messageLabel.setText("Inscription réussie ! Veuillez vous connecter.");
            clearFields();
        } else {
            messageLabel.setText("Cet email existe déjà");
        }
    }

    @FXML
    private void onLoginClicked() {
        try {
            Stage stage = (Stage) emailField.getScene().getWindow();
            boolean isFullScreen = stage.isFullScreen();
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
            if (root == null) {
                return;
            }
            Scene scene = new Scene(root, 400, 500);
            java.net.URL stylesheetUrl = getClass().getClassLoader().getResource("com/example/auth/styles.css");
            if (stylesheetUrl != null) {
                scene.getStylesheets().add(stylesheetUrl.toExternalForm());
            } else {
                System.err.println("Stylesheet modern-theme.css not found!");
            }

            stage.setScene(scene);
            stage.setFullScreen(isFullScreen);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        prenomField.clear();
        nomField.clear();
        emailField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        travailField.clear();
        numTelField.clear();
        messageLabel.setText("");
        selectedPhoto = null;
        photoPreview.setImage(null);
        photoErrorLabel.setVisible(false);
    }
}