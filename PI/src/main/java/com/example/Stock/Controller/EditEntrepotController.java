package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.service.EntrepotService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class EditEntrepotController {
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField espaceField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label titleLabel;

    private Entrepot entrepot;
    private EntrepotController parentController;

    public void setEntrepot(Entrepot entrepot) {
        this.entrepot = entrepot;
        populateFields();
    }

    public void setParentController(EntrepotController parentController) {
        this.parentController = parentController;
    }

    private void populateFields() {
        titleLabel.setText("Modifier Entrepôt: " + entrepot.getNom());
        nomField.setText(entrepot.getNom());
        adresseField.setText(entrepot.getAdresse());
        villeField.setText(entrepot.getVille());
        espaceField.setText(String.valueOf(entrepot.getEspace()));

        if (entrepot.getLatitude() != null) {
            latitudeField.setText(String.valueOf(entrepot.getLatitude()));
        }

        if (entrepot.getLongitude() != null) {
            longitudeField.setText(String.valueOf(entrepot.getLongitude()));
        }
    }

    private EntrepotService entrepotService = new EntrepotService();

    @FXML
    private void handleSave() {
        if (validateInput()) {
            // Mettre à jour l'objet Entrepot
            entrepot.setNom(nomField.getText());
            entrepot.setAdresse(adresseField.getText());
            entrepot.setVille(villeField.getText());
            entrepot.setEspace(Double.parseDouble(espaceField.getText()));

            if (!latitudeField.getText().isEmpty()) {
                entrepot.setLatitude(Double.parseDouble(latitudeField.getText()));
            } else {
                entrepot.setLatitude(null);
            }

            if (!longitudeField.getText().isEmpty()) {
                entrepot.setLongitude(Double.parseDouble(longitudeField.getText()));
            } else {
                entrepot.setLongitude(null);
            }

            // Sauvegarder dans la base de données
            boolean success = entrepotService.updateEntrepot(entrepot);

            if (success) {
                if (parentController != null) {
                    parentController.refreshEntrepotData();
                }
                closeWindow();
            } else {
                showAlert("Erreur", "Échec de la mise à jour de l'entrepôt dans la base de données");
            }
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        if (nomField.getText().isEmpty()) {
            errors.append("- Le nom est obligatoire\n");
        }

        if (adresseField.getText().isEmpty()) {
            errors.append("- L'adresse est obligatoire\n");
        }

        if (villeField.getText().isEmpty()) {
            errors.append("- La ville est obligatoire\n");
        }

        try {
            Double.parseDouble(espaceField.getText());
        } catch (NumberFormatException e) {
            errors.append("- L'espace doit être un nombre valide\n");
        }

        if (!latitudeField.getText().isEmpty()) {
            try {
                Double.parseDouble(latitudeField.getText());
            } catch (NumberFormatException e) {
                errors.append("- La latitude doit être un nombre valide\n");
            }
        }

        if (!longitudeField.getText().isEmpty()) {
            try {
                Double.parseDouble(longitudeField.getText());
            } catch (NumberFormatException e) {
                errors.append("- La longitude doit être un nombre valide\n");
            }
        }

        if (errors.length() > 0) {
            showAlert("Erreur de validation", "Veuillez corriger les erreurs suivantes:\n" + errors.toString());
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }
}