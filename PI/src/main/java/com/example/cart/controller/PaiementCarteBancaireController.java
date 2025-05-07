package com.example.cart.controller;

import com.example.cart.service.CarteVirtuelleManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

import java.io.IOException;

public class PaiementCarteBancaireController {

    @FXML
    private TextField numeroCarteField;

    @FXML
    private TextField dateExpirationField;

    @FXML
    private TextField cvvField;

    @FXML
    private TextField nomTitulaireField;

    @FXML
    private void handleValiderPaiement() {
        String numeroCarte = numeroCarteField.getText().trim();
        String dateExpiration = dateExpirationField.getText().trim();
        String cvv = cvvField.getText().trim();
        String nomTitulaire = nomTitulaireField.getText().trim();

        if (numeroCarte.length() != 16 || !numeroCarte.matches("\\d{16}")) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Numéro de carte invalide.");
            return;
        }
        if (!dateExpiration.matches("\\d{2}/\\d{2}")) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Format date expiration incorrect. (MM/AA)");
            return;
        }
        if (cvv.length() != 3 || !cvv.matches("\\d{3}")) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "CVV invalide.");
            return;
        }
        if (nomTitulaire.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Nom du titulaire vide.");
            return;
        }

        // ✅ Simulation : Recharger la carte virtuelle avec un montant par défaut (ex: 50DT)
        CarteVirtuelleManager.chargerCarte(50.0);

        showAlert(Alert.AlertType.INFORMATION, "Succès", "Paiement validé. Carte virtuelle rechargée de 50 DT.");

        // ✅ Retour à la page CarteVirtuelle après paiement réussi
        retournerPageCarteVirtuelle();
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void retournerPageCarteVirtuelle() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontPages/pages/CarteVirtuelle.fxml"));
            Parent cartePage = loader.load();

            Stage stage = (Stage) numeroCarteField.getScene().getWindow();
            stage.getScene().setRoot(cartePage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
