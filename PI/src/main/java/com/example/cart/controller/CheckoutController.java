package com.example.cart.controller;

import com.example.cart.CartManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.example.cart.model.OrderSummary;
import com.example.cart.OrderHistoryManager;

import java.io.IOException;
import java.time.YearMonth;
import java.util.UUID;

public class CheckoutController {

    @FXML
    private TextField nameField;

    @FXML
    private TextField cardNumberField;

    @FXML
    private TextField expiryDateField;

    @FXML
    private TextField cvvField;

    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        setupInputRestrictions();
    }

    private void setupInputRestrictions() {
        // Nom : empêcher de taper des chiffres
        nameField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("[a-zA-Z\\s]*")) {
                nameField.setText(oldText);
            }
        });

        // Numéro de carte : uniquement chiffres, max 16 chiffres
        cardNumberField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*") || newText.length() > 16) {
                cardNumberField.setText(oldText);
            }
        });

        // Expiry date : format MM/YY avec ajout automatique du '/'
        expiryDateField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d{0,2}(/\\d{0,2})?")) {
                expiryDateField.setText(oldText);
            } else if (newText.length() == 2 && !oldText.endsWith("/")) {
                expiryDateField.setText(newText + "/");
            }
        });

        // CVV : uniquement chiffres, max 3 chiffres
        cvvField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d{0,3}")) {
                cvvField.setText(oldText);
            }
        });
    }

    @FXML
    private void handlePayment() {
        String name = nameField.getText().trim();
        String cardNumber = cardNumberField.getText().trim();
        String expiry = expiryDateField.getText().trim();
        String cvv = cvvField.getText().trim();

        errorLabel.setText("");
        errorLabel.setVisible(false);

        // 🚨 Vérifications
        if (name.isEmpty() || cardNumber.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
            errorLabel.setText("⚠️ Tous les champs doivent être remplis !");
            errorLabel.setVisible(true);
            return;
        }
        if (!name.matches("[a-zA-Z\\s]+")) {
            errorLabel.setText("⚠️ Le nom doit contenir uniquement des lettres !");
            errorLabel.setVisible(true);
            return;
        }
        if (!cardNumber.matches("\\d{16}")) {
            errorLabel.setText("⚠️ Numéro de carte invalide !");
            errorLabel.setVisible(true);
            return;
        }
        if (!expiry.matches("\\d{2}/\\d{2}")) {
            errorLabel.setText("⚠️ Format date invalide (MM/YY) !");
            errorLabel.setVisible(true);
            return;
        }

        try {
            String[] parts = expiry.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt(parts[1]) + 2000;

            if (month < 1 || month > 12) {
                errorLabel.setText("⚠️ Mois invalide !");
                errorLabel.setVisible(true);
                return;
            }

            YearMonth today = YearMonth.now();
            YearMonth enteredDate = YearMonth.of(year, month);

            if (enteredDate.isBefore(today)) {
                errorLabel.setText("⚠️ Carte expirée !");
                errorLabel.setVisible(true);
                return;
            }
        } catch (Exception e) {
            errorLabel.setText("⚠️ Erreur dans la date !");
            errorLabel.setVisible(true);
            return;
        }

        if (!cvv.matches("\\d{3}")) {
            errorLabel.setText("⚠️ CVV invalide !");
            errorLabel.setVisible(true);
            return;
        }

        // ✅ Paiement validé
        errorLabel.setVisible(false);

        // 📦 Créer la commande correctement
        String commandeId = UUID.randomUUID().toString();
        String userId = com.example.auth.utils.SessionManager.getInstance().getLoggedInUser().getId().toString();
        String dateAchat = java.time.LocalDateTime.now().toString();
        double total = CartManager.getTotalPrice();

        OrderSummary orderSummary = new OrderSummary(
                commandeId,
                userId,
                dateAchat,
                total
        );

        // ➡️ Ajouter à l'historique + sauvegarder dans la BDD
        OrderHistoryManager.addOrder(orderSummary);

        // 🛒 Vider le panier après ajout
        CartManager.clearCart();

        // 🎉 Message succès
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Paiement Réussi");
        alert.setHeaderText(null);
        alert.setContentText("🎉 Votre commande a été validée avec succès !");
        alert.showAndWait();

        // 🔄 Rediriger vers l'historique des commandes
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontPages/pages/OrderHistory.fxml"));
            Parent orderHistoryPage = loader.load();

            BorderPane root = (BorderPane) nameField.getScene().lookup("#borderPane");
            if (root != null) {
                root.setCenter(orderHistoryPage);
            } else {
                Stage stage = (Stage) nameField.getScene().getWindow();
                stage.getScene().setRoot(orderHistoryPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}