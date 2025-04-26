package com.example.Evenement.Controller;

import com.example.Evenement.Model.Evenement;
import com.example.auth.utils.SessionManager;
import com.example.auth.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.example.Evenement.Model.Inscription;
import com.example.Evenement.Dao.InscriptionDAO;

public class EventInscriptionController {

    @FXML private Button backButton;
    @FXML private Label eventTitle;
    @FXML private RadioButton sessionActuelle;
    @FXML private RadioButton nouveauParticipant;
    @FXML private VBox formulaireManuel;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;

    private Evenement currentEvent;
    private Stage stage;
    private SessionManager sessionManager = SessionManager.getInstance();
    private ToggleGroup inscriptionToggle;

    public void setEvent(Evenement event) {
        this.currentEvent = event;
        eventTitle.setText(event.getTitre());
        initializeInscriptionOptions();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        // Initialiser le ToggleGroup pour les RadioButtons
        inscriptionToggle = new ToggleGroup();
        sessionActuelle.setToggleGroup(inscriptionToggle);
        nouveauParticipant.setToggleGroup(inscriptionToggle);

        // Écouter les changements de sélection
        inscriptionToggle.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            formulaireManuel.setVisible(newValue == nouveauParticipant);
        });
    }

    private void initializeInscriptionOptions() {
        if (sessionManager.isLoggedIn()) {
            sessionActuelle.setSelected(true);
            formulaireManuel.setVisible(false);
            
            // Pré-remplir les champs avec les informations de l'utilisateur connecté
            User user = sessionManager.getLoggedInUser();
            if (user != null) {
                nomField.setText(user.getNom());
                prenomField.setText(user.getPrenom());
                emailField.setText(user.getEmail());
                telephoneField.setText(user.getNumTel() != null ? user.getNumTel() : "");
            }
        } else {
            nouveauParticipant.setSelected(true);
            formulaireManuel.setVisible(true);
        }
    }

    @FXML
    private void handleBack() {
        stage.close();
    }

    @FXML
    private void handleCancelInscription() {
        stage.close();
    }

    @FXML
    private void handleConfirmInscription() {
        if (sessionActuelle.isSelected()) {
            inscrireUtilisateurConnecte();
        } else {
            if (validateFormulaire()) {
                inscrireNouveauParticipant();
            }
        }
    }

    private void inscrireUtilisateurConnecte() {
        try {
            User user = sessionManager.getLoggedInUser();
            if (user == null) {
                showErrorMessage("Erreur", "Aucun utilisateur connecté");
                return;
            }
            InscriptionDAO dao = new InscriptionDAO();
            dao.createSimple(user.getId().toString(), currentEvent.getId());
            showSuccessMessage("Inscription réussie", 
                String.format("Vous (%s %s) êtes maintenant inscrit à l'événement : %s", 
                    user.getPrenom(), user.getNom(), currentEvent.getTitre()));
            stage.close();
        } catch (Exception e) {
            showErrorMessage("Erreur d'inscription", 
                "Une erreur est survenue lors de l'inscription : " + e.getMessage());
        }
    }

    private void inscrireNouveauParticipant() {
        showErrorMessage("Inscription impossible", "Vous devez avoir un compte utilisateur pour vous inscrire à un événement.");
    }

    private boolean validateFormulaire() {
        StringBuilder errors = new StringBuilder();

        if (nomField.getText().trim().isEmpty()) {
            errors.append("Le nom est requis.\n");
        }
        if (prenomField.getText().trim().isEmpty()) {
            errors.append("Le prénom est requis.\n");
        }
        if (emailField.getText().trim().isEmpty() || !emailField.getText().contains("@")) {
            errors.append("Une adresse email valide est requise.\n");
        }
        if (telephoneField.getText().trim().isEmpty()) {
            errors.append("Le numéro de téléphone est requis.\n");
        }

        if (errors.length() > 0) {
            showErrorMessage("Formulaire incomplet", errors.toString());
            return false;
        }
        return true;
    }

    private void showSuccessMessage(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorMessage(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
} 