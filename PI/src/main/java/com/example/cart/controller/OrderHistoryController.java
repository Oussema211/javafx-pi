package com.example.cart.controller;

import com.example.cart.model.OrderSummary;
import com.example.cart.OrderHistoryManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.layout.Region;

public class OrderHistoryController {

    @FXML
    private VBox orderContainer;


    @FXML
    private ScrollPane orderScrollPane; // (ça doit être connecté à ton ScrollPane)

    @FXML
    private void initialize() {
        ObservableList<OrderSummary> orders = FXCollections.observableArrayList(OrderHistoryManager.getOrderHistory());

        for (OrderSummary order : orders) {
            VBox card = new VBox(10);
            card.getStyleClass().add("order-card");

            Label titleLabel = new Label("Commande de l'utilisateur : " + order.getUserId());
            titleLabel.getStyleClass().add("order-title");

            Label dateLabel = new Label("Date d'achat : " + order.getDateAchat());
            dateLabel.getStyleClass().add("order-date");

            Label priceLabel = new Label(String.format("Prix total : %.2f DT", order.getPrixTotal()));
            priceLabel.getStyleClass().add("order-price");

            card.getChildren().addAll(titleLabel, dateLabel, priceLabel);
            card.setPrefWidth(500);

            orderContainer.getChildren().add(card);

        }

        // 🆕 Charger automatiquement le CSS
        orderScrollPane.getStylesheets().add(getClass().getResource("/com/example/css/orderhistory.css").toExternalForm());
    }
}
