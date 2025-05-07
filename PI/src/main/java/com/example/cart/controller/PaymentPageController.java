package com.example.cart.controller;

import com.example.cart.service.CarteVirtuelleManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.input.KeyEvent;

import java.io.IOException;

public class PaymentPageController {

    @FXML
    private TextField numeroCarteField;

    @FXML
    private TextField dateExpirationField;

    @FXML
    private TextField cvvField;

    @FXML
    private TextField montantField;

    @FXML
    private void initialize() {
        limiterSaisie();
    }

    private void limiterSaisie() {
        // Numéro carte : 16 chiffres max
        numeroCarteField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (!event.getCharacter().matches("\\d") || numeroCarteField.getText().length() >= 16) {
                event.consume();
            }
        });

        // CVV : 3 chiffres max
        cvvField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            if (!event.getCharacter().matches("\\d") || cvvField.getText().length() >= 3) {
                event.consume();
            }
        });

        // Date expiration avec format automatique MM/AA
        dateExpirationField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String character = event.getCharacter();
            String text = dateExpirationField.getText();

            if (!character.matches("\\d") || text.length() >= 5) {
                event.consume();
                return;
            }

            if (text.length() == 2) {
                dateExpirationField.setText(text + "/");
                dateExpirationField.positionCaret(dateExpirationField.getText().length());
            }
        });

        // Montant : chiffres et point seulement
        montantField.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            String c = event.getCharacter();
            if (!c.matches("[\\d\\.]") || (c.equals(".") && montantField.getText().contains("."))) {
                event.consume();
            }
        });
    }

    @FXML
    private void handlePayer() {
        try {
            if (numeroCarteField.getText().isEmpty() || dateExpirationField.getText().isEmpty() || cvvField.getText().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez remplir tous les champs.");
                return;
            }

            String date = dateExpirationField.getText();
            if (!date.matches("\\d{2}/\\d{2}")) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Date invalide (format attendu : MM/AA).");
                return;
            }
            int mois = Integer.parseInt(date.split("/")[0]);
            if (mois < 1 || mois > 12) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Mois d'expiration invalide.");
                return;
            }

            if (cvvField.getText().length() != 3) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "CVV invalide (3 chiffres).");
                return;
            }

            double montant = Double.parseDouble(montantField.getText());
            if (montant <= 0) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Montant invalide.");
                return;
            }

            // ✅ Simulation du paiement réussi
            CarteVirtuelleManager.chargerCarte(montant);

            showAlert(Alert.AlertType.INFORMATION, "Paiement réussi", "Votre carte virtuelle a été rechargée avec succès !");

            // Retour à la page Carte Virtuelle
            retournerCarteVirtuellePage();

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer un montant valide.");
        }
    }

    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void retournerCarteVirtuellePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontPages/pages/CarteVirtuelle.fxml"));
            Parent cartePage = loader.load();

            BorderPane root = (BorderPane) montantField.getScene().lookup("#borderPane");
            if (root != null) {
                root.setCenter(cartePage);
            } else {
                Stage stage = (Stage) montantField.getScene().getWindow();
                stage.getScene().setRoot(cartePage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
