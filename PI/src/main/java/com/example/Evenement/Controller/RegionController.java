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
        if (nomField.getText() == null || nomField.getText().trim().isEmpty()) {
            showAlert("Erreur", "Le nom est obligatoire", AlertType.ERROR);
            nomField.requestFocus();
            return false;
        }
        if (villeField.getText() == null || villeField.getText().trim().isEmpty()) {
            showAlert("Erreur", "La ville est obligatoire", AlertType.ERROR);
            villeField.requestFocus();
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