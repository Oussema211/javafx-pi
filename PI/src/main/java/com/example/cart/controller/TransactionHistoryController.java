package com.example.cart.controller;

import com.example.cart.model.TransactionBlockchain;
import com.example.cart.service.CarteVirtuelleManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;

import java.util.List;

public class TransactionHistoryController {

    @FXML
    private VBox transactionContainer;

    @FXML
    private void initialize() {
        chargerHistorique();
    }

    private void chargerHistorique() {
        List<TransactionBlockchain> transactions = CarteVirtuelleManager.getHistoriqueTransactions();

        for (TransactionBlockchain transaction : transactions) {
            HBox card = creerCarteTransaction(transaction);
            transactionContainer.getChildren().add(card);
        }
    }

    private HBox creerCarteTransaction(TransactionBlockchain transaction) {
        HBox card = new HBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0.3, 0, 0);");
        card.setPadding(new Insets(10));

        Label montantLabel = new Label(String.format("%.2f DT", transaction.getMontant()));
        montantLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #4caf50; -fx-font-weight: bold;");

        Label descriptionLabel = new Label(transaction.getDescription());
        descriptionLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");

        card.getChildren().addAll(montantLabel, descriptionLabel);
        return card;
    }
}
