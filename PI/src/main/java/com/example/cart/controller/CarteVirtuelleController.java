package com.example.cart.controller;

import com.example.cart.service.CarteVirtuelleManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

public class CarteVirtuelleController {

    @FXML
    private Button historiqueButton;


    @FXML
    private Label numeroCarteLabel;

    @FXML
    private Label soldeLabel;
    @FXML
    private Label etatCarteLabel;

    @FXML
    private TextField montantField;

    @FXML
    private void initialize() {
        if (CarteVirtuelleManager.getCarte() == null) {
            CarteVirtuelleManager.creerNouvelleCarte();
        }
        afficherInfosCarte();
    }

    private void afficherInfosCarte() {
        numeroCarteLabel.setText("Numéro Carte : " + CarteVirtuelleManager.getNumeroCarte());
        soldeLabel.setText(String.format("Solde : %.2f DT", CarteVirtuelleManager.getSolde()));

        if (CarteVirtuelleManager.isCarteActive()) {
            etatCarteLabel.setText("État : ✅ Activée");
            etatCarteLabel.setStyle("-fx-text-fill: green; -fx-font-size: 18px;");
        } else {
            etatCarteLabel.setText("État : ❌ Désactivée");
            etatCarteLabel.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
        }
    }


    @FXML
    private void handleCharger() {
        if (!CarteVirtuelleManager.isCarteActive()) {
            showAlert(Alert.AlertType.ERROR, "Carte Inactive", "Veuillez activer votre carte avant de la recharger !");
            return;
        }

        try {
            double montant = Double.parseDouble(montantField.getText());
            if (montant <= 0) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Montant invalide.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontPages/pages/PaymentPage.fxml"));
            Parent paymentPage = loader.load();

            BorderPane root = (BorderPane) numeroCarteLabel.getScene().lookup("#borderPane");
            if (root != null) {
                root.setCenter(paymentPage);
            } else {
                Stage stage = (Stage) numeroCarteLabel.getScene().getWindow();
                stage.getScene().setRoot(paymentPage);
            }

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer un nombre valide.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handlePayer() {
        if (!CarteVirtuelleManager.isCarteActive()) {
            showAlert(Alert.AlertType.ERROR, "Carte Inactive", "Veuillez activer votre carte avant de payer !");
            return;
        }

        // Demander mot de passe AVANT le paiement
        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Vérification du Mot de Passe");
        passwordDialog.setHeaderText("Authentification Requise");
        passwordDialog.setContentText("Veuillez entrer votre mot de passe :");

        passwordDialog.showAndWait().ifPresent(inputPassword -> {
            if (CarteVirtuelleManager.verifierMotDePasse(inputPassword)) {
                try {
                    double montant = Double.parseDouble(montantField.getText());
                    if (montant <= 0) {
                        showAlert(Alert.AlertType.ERROR, "Erreur", "Montant invalide.");
                        return;
                    }
                    boolean success = CarteVirtuelleManager.effectuerPaiement(montant);
                    if (success) {
                        afficherInfosCarte();
                        showAlert(Alert.AlertType.INFORMATION, "Succès", "Paiement effectué !");
                        montantField.clear();
                    } else {
                        showAlert(Alert.AlertType.WARNING, "Échec", "Solde insuffisant.");
                    }
                } catch (NumberFormatException e) {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer un nombre valide.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Mot de passe incorrect !");
            }
        });
    }

    @FXML
    private void handleDesactiverCarte() {
        if (CarteVirtuelleManager.isCarteActive()) {
            CarteVirtuelleManager.desactiverCarte();
            showAlert(Alert.AlertType.INFORMATION, "Carte Désactivée", "Votre carte virtuelle est maintenant désactivée !");
            afficherInfosCarte(); // pour mettre à jour l'affichage
        } else {
            showAlert(Alert.AlertType.WARNING, "Déjà Désactivée", "Votre carte est déjà désactivée.");
        }
    }

    @FXML
    private void handleVoirHistorique() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontPages/pages/TransactionHistory.fxml"));
            Parent historiquePage = loader.load();

            BorderPane root = (BorderPane) numeroCarteLabel.getScene().lookup("#borderPane");
            if (root != null) {
                root.setCenter(historiquePage);
            } else {
                Stage stage = (Stage) numeroCarteLabel.getScene().getWindow();
                stage.getScene().setRoot(historiquePage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleActiverCarte() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Activer la Carte Virtuelle");
        dialog.setHeaderText("Protection de votre carte");
        dialog.setContentText("Entrez un mot de passe pour activer votre carte :");

        dialog.showAndWait().ifPresent(password -> {
            if (password.trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Mot de passe vide !");
            } else {
                boolean activated = CarteVirtuelleManager.activerCarte(password);
                if (activated) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre carte est activée !");
                    afficherInfosCarte(); // ➔ AJOUT ici
                } else {
                    showAlert(Alert.AlertType.WARNING, "Attention", "Votre carte est déjà activée !");
                }
            }
        });
    }



    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
