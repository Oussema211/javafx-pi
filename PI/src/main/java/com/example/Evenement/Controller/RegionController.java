package com.example.Evenement.Controller;

import com.example.Evenement.Dao.RegionDAO;
import com.example.Evenement.Model.Region;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
                    showAlert("Succès", "Région ajoutée avec succès", Alert.AlertType.INFORMATION);
                } else {
                    dao.updateRegion(currentRegion);
                    showAlert("Succès", "Région mise à jour avec succès", Alert.AlertType.INFORMATION);
                }
                clearFields();
            } catch (Exception e) {
                showAlert("Erreur", "Échec de l'opération: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleCancel() {
        Stage currentStage = (Stage) nomField.getScene().getWindow();
        currentStage.close();
    }

    private void clearFields() {
        currentRegion = new Region();
        bindFieldsToModel();
    }

    private boolean validateFields() {
        String titre = nomField.getText();
        if (titre == null || titre.trim().isEmpty()) {
            showAlert("Erreur", "Le titre est obligatoire", Alert.AlertType.ERROR);
            nomField.requestFocus();
            return false;
        }

        if (titre.length() < 3 || titre.length() > 100) {
            showAlert("Erreur", "Le titre doit contenir entre 3 et 100 caractères", Alert.AlertType.ERROR);
            nomField.requestFocus();
            return false;
        }

        if (!titre.matches("^[A-Za-zÀ-ÿ\\s-]+$")) {
            showAlert("Erreur", "Le titre ne doit contenir que des lettres, espaces et tirets", Alert.AlertType.ERROR);
            nomField.requestFocus();
            return false;
        }

        if (!Character.isUpperCase(titre.charAt(0))) {
            showAlert("Erreur", "Le titre doit commencer par une majuscule", Alert.AlertType.ERROR);
            nomField.requestFocus();
            return false;
        }

        try {
            RegionDAO dao = new RegionDAO();
            if (currentRegion.getId() == 0) {
                if (dao.titreExists(titre)) {
                    showAlert("Erreur", "Ce titre existe déjà", Alert.AlertType.ERROR);
                    nomField.requestFocus();
                    return false;
                }
            } else {
                if (dao.titreExistsForOtherEvent(titre, currentRegion.getId())) {
                    showAlert("Erreur", "Ce titre existe déjà pour une autre région", Alert.AlertType.ERROR);
                    nomField.requestFocus();
                    return false;
                }
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la vérification de l'unicité du titre: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }

        String ville = villeField.getText();
        if (ville == null || ville.trim().isEmpty()) {
            showAlert("Erreur", "La ville est obligatoire", Alert.AlertType.ERROR);
            villeField.requestFocus();
            return false;
        }

        if (ville.length() < 3 || ville.length() > 50) {
            showAlert("Erreur", "La ville doit contenir entre 3 et 50 caractères", Alert.AlertType.ERROR);
            villeField.requestFocus();
            return false;
        }

        if (!ville.matches("^[A-Za-zÀ-ÿ\\s-]+$")) {
            showAlert("Erreur", "La ville ne doit contenir que des lettres, espaces et tirets", Alert.AlertType.ERROR);
            villeField.requestFocus();
            return false;
        }

        String description = descriptionField.getText();
        if (description == null || description.trim().isEmpty()) {
            showAlert("Erreur", "La description est obligatoire", Alert.AlertType.ERROR);
            descriptionField.requestFocus();
            return false;
        }

        if (description.length() < 10 || description.length() > 500) {
            showAlert("Erreur", "La description doit contenir entre 10 et 500 caractères", Alert.AlertType.ERROR);
            descriptionField.requestFocus();
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void handleShowList() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/RegionList.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Liste des régions");
            stage.setScene(new Scene(root));
            stage.show();

            // Fermer la fenêtre actuelle si nécessaire
            Stage currentStage = (Stage) nomField.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir la liste des régions: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
