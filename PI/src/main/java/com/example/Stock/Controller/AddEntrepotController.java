package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.service.EntrepotService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.UUID;

public class AddEntrepotController {
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField espaceField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label titleLabel;

    private EntrepotController parentController;
    private final EntrepotService entrepotService = new EntrepotService();

    public void setParentController(EntrepotController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleSave() {
        if (validateInput()) {
            Entrepot newEntrepot = new Entrepot();
            newEntrepot.setId(UUID.randomUUID());
            newEntrepot.setNom(nomField.getText());
            newEntrepot.setAdresse(adresseField.getText());
            newEntrepot.setVille(villeField.getText());
            newEntrepot.setEspace(Double.parseDouble(espaceField.getText()));

            if (!latitudeField.getText().isEmpty()) {
                newEntrepot.setLatitude(Double.parseDouble(latitudeField.getText()));
            }

            if (!longitudeField.getText().isEmpty()) {
                newEntrepot.setLongitude(Double.parseDouble(longitudeField.getText()));
            }

            boolean success = entrepotService.addEntrepot(newEntrepot);

            if (success) {
                if (parentController != null) {
                    parentController.refreshEntrepotData();
                }
                closeWindow();
            } else {
                showAlert("Erreur", "Échec de l'ajout du nouvel entrepôt");
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
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }
}