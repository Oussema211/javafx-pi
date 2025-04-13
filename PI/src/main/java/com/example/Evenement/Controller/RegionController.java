package com.example.Evenement.Controller;

import com.example.Evenement.Dao.RegionDAO;
import com.example.Evenement.Model.Region;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class RegionController {
    private Region currentRegion = new Region();

    @FXML private TextField nomField;
    @FXML private TextField villeField;
    @FXML private TextField descriptionField;

    @FXML
    public void initialize() {
        bindFieldsToModel();
    }

    private void bindFieldsToModel() {
        nomField.textProperty().bindBidirectional(currentRegion.nomProperty());
        villeField.textProperty().bindBidirectional(currentRegion.villeProperty());
        descriptionField.textProperty().bindBidirectional(currentRegion.descriptionProperty());
    }

    public void setRegionToEdit(Region region) {
        this.currentRegion = region;
        bindFieldsToModel();
    }

    @FXML
    private void handleSave() {
        if (validateFields()) {
            try {
                RegionDAO dao = new RegionDAO();
                if (currentRegion.getId() == 0) {
                    dao.addRegion(currentRegion);
                    showAlert("Succès", "Région ajoutée avec succès", AlertType.INFORMATION);
                } else {
                    dao.updateRegion(currentRegion);
                    showAlert("Succès", "Région mise à jour avec succès", AlertType.INFORMATION);
                }
                clearFields();
            } catch (Exception e) {
                showAlert("Erreur", "Échec de l'opération: " + e.getMessage(), AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleShowRegions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/RegionList.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Liste des Régions");
            stage.setScene(new Scene(root));
            stage.show();

            Stage currentStage = (Stage) nomField.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir la liste des régions: " + e.getMessage(), AlertType.ERROR);
        }
    }

    private void clearFields() {
        currentRegion = new Region();
        bindFieldsToModel();
    }

    private boolean validateFields() {
        // Validation du titre
        String titre = nomField.getText();
        if (titre == null || titre.trim().isEmpty()) {
            showAlert("Erreur", "Le titre est obligatoire", AlertType.ERROR);
            nomField.requestFocus();
            return false;
        }
        
        // Vérification de la longueur du titre (3-100 caractères)
        if (titre.length() < 3 || titre.length() > 100) {
            showAlert("Erreur", "Le titre doit contenir entre 3 et 100 caractères", AlertType.ERROR);
            nomField.requestFocus();
            return false;
        }
        
        // Vérification du format du titre (lettres, espaces et tirets uniquement)
        if (!titre.matches("^[A-Za-zÀ-ÿ\\s-]+$")) {
            showAlert("Erreur", "Le titre ne doit contenir que des lettres, espaces et tirets", AlertType.ERROR);
            nomField.requestFocus();
            return false;
        }
        
        // Vérification de la majuscule en début de titre
        if (!Character.isUpperCase(titre.charAt(0))) {
            showAlert("Erreur", "Le titre doit commencer par une majuscule", AlertType.ERROR);
            nomField.requestFocus();
            return false;
        }
        
        // Vérification de l'unicité du titre
        try {
            RegionDAO dao = new RegionDAO();
            if (currentRegion.getId() == 0) {
                // Nouvelle région
                if (dao.titreExists(titre)) {
                    showAlert("Erreur", "Ce titre existe déjà", AlertType.ERROR);
                    nomField.requestFocus();
                    return false;
                }
            } else {
                // Modification d'une région existante
                if (dao.titreExistsForOtherEvent(titre, currentRegion.getId())) {
                    showAlert("Erreur", "Ce titre existe déjà pour une autre région", AlertType.ERROR);
                    nomField.requestFocus();
                    return false;
                }
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la vérification de l'unicité du titre: " + e.getMessage(), AlertType.ERROR);
            return false;
        }

        // Validation de la ville
        String ville = villeField.getText();
        if (ville == null || ville.trim().isEmpty()) {
            showAlert("Erreur", "La ville est obligatoire", AlertType.ERROR);
            villeField.requestFocus();
            return false;
        }
        
        // Vérification de la longueur de la ville (3-50 caractères)
        if (ville.length() < 3 || ville.length() > 50) {
            showAlert("Erreur", "La ville doit contenir entre 3 et 50 caractères", AlertType.ERROR);
            villeField.requestFocus();
            return false;
        }
        
        // Vérification du format de la ville (pas de chiffres ou caractères spéciaux)
        if (!ville.matches("^[A-Za-zÀ-ÿ\\s-]+$")) {
            showAlert("Erreur", "La ville ne doit contenir que des lettres, espaces et tirets", AlertType.ERROR);
            villeField.requestFocus();
            return false;
        }

        // Validation de la description
        String description = descriptionField.getText();
        if (description == null || description.trim().isEmpty()) {
            showAlert("Erreur", "La description est obligatoire", AlertType.ERROR);
            descriptionField.requestFocus();
            return false;
        }
        
        // Vérification de la longueur de la description (10-500 caractères)
        if (description.length() < 10 || description.length() > 500) {
            showAlert("Erreur", "La description doit contenir entre 10 et 500 caractères", AlertType.ERROR);
            descriptionField.requestFocus();
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}