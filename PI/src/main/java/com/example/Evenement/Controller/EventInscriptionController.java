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
    @FXML private VBox formulaireManuel;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;

    private Evenement currentEvent;
    private Stage stage;
    private SessionManager sessionManager = SessionManager.getInstance();

    public void setEvent(Evenement event) {
        this.currentEvent = event;
        eventTitle.setText(event.getTitre());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        // Ne garder que la sélection de la session actuelle
        sessionActuelle.setSelected(true);
        sessionActuelle.setDisable(true);
        // Cacher tout formulaire manuel s'il existe
        if (formulaireManuel != null) {
            formulaireManuel.setVisible(false);
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
        inscrireUtilisateurConnecte();
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