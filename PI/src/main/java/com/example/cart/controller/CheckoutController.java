package com.example.cart.controller;

import com.example.cart.CartManager;
import com.example.cart.OrderHistoryManager;
import com.example.cart.model.OrderSummary;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

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
        nameField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("[a-zA-Z\\s]*")) {
                nameField.setText(oldText);
            }
        });

        cardNumberField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d*") || newText.length() > 16) {
                cardNumberField.setText(oldText);
            }
        });

        expiryDateField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.matches("\\d{0,2}(/\\d{0,2})?")) {
                expiryDateField.setText(oldText);
            } else if (newText.length() == 2 && !oldText.endsWith("/")) {
                expiryDateField.setText(newText + "/");
            }
        });

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

        // 📦 Créer la commande
        String commandeId = UUID.randomUUID().toString();
        String userId = com.example.auth.utils.SessionManager.getInstance().getLoggedInUser().getId().toString();
        String dateAchat = java.time.LocalDateTime.now().toString();
        double total = CartManager.getTotalPrice();

        OrderSummary orderSummary = new OrderSummary(commandeId, userId, dateAchat, total);

        OrderHistoryManager.addOrder(orderSummary);
        CartManager.clearCart();

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Paiement Réussi");
        alert.setHeaderText(null);
        alert.setContentText("🎉 Votre commande a été validée avec succès !");
        alert.showAndWait();

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
