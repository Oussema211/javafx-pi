package com.example.cart.controller;

import com.example.cart.OrderHistoryManager;
import com.example.cart.model.OrderSummary;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;

public class OrderHistoryController {

    @FXML
    private VBox orderContainer;
    @FXML
    private Label totalOrdersLabel;
    @FXML
    private Label totalSpentLabel;
    @FXML
    private Label averagePriceLabel;
    @FXML
    private ScrollPane orderScrollPane;

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

            // 🔥 Affichage des détails au clic
            card.setOnMouseClicked(e -> {
                new com.example.cart.view.OrderDetailsDialog(order).showAndWait();
            });

            orderContainer.getChildren().add(card);
        }

        updateStats();
        animateStatisticsCards();

        // 🔗 Appliquer le CSS
        orderScrollPane.getStylesheets().add(getClass().getResource("/com/example/css/orderhistory.css").toExternalForm());
    }

    private void updateStats() {
        ObservableList<OrderSummary> orders = OrderHistoryManager.getOrderHistory();

        int totalOrders = orders.size();
        double totalSpent = orders.stream().mapToDouble(OrderSummary::getPrixTotal).sum();
        double averagePrice = totalOrders > 0 ? totalSpent / totalOrders : 0.0;

        animateNumber(totalOrdersLabel, 0, totalOrders, "");
        animateNumber(totalSpentLabel, 0, totalSpent, " DT");
        animateNumber(averagePriceLabel, 0, averagePrice, " DT");
    }

    private void animateNumber(Label label, double start, double end, String suffix) {
        final int durationMillis = 1000;
        final long frameRate = 60;
        final double increment = (end - start) / (durationMillis / (1000.0 / frameRate));

        new Thread(() -> {
            double currentValue = start;
            long sleepTime = 1000 / frameRate;

            while ((increment > 0 && currentValue < end) || (increment < 0 && currentValue > end)) {
                currentValue += increment;
                double finalValue = currentValue;

                javafx.application.Platform.runLater(() -> {
                    if (suffix.contains("DT")) {
                        label.setText(String.format("%.2f%s", finalValue, suffix));
                    } else {
                        label.setText(String.format("%.0f%s", finalValue, suffix));
                    }
                });

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            javafx.application.Platform.runLater(() -> {
                if (suffix.contains("DT")) {
                    label.setText(String.format("%.2f%s", end, suffix));
                } else {
                    label.setText(String.format("%.0f%s", end, suffix));
                }
            });
        }).start();
    }

    @FXML
    private void handleCardHover(javafx.scene.input.MouseEvent event) {
        VBox card = (VBox) event.getSource();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 15;"
                + "-fx-effect: dropshadow(gaussian, #00c6ff, 30, 0.6, 0, 0); -fx-pref-width: 180;"
                + "-fx-translate-y: -5;");
    }

    @FXML
    private void handleCardExit(javafx.scene.input.MouseEvent event) {
        VBox card = (VBox) event.getSource();
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 15;"
                + "-fx-effect: dropshadow(gaussian, #00c6ff, 20, 0.5, 0, 0); -fx-pref-width: 180;"
                + "-fx-translate-y: 0;");
    }

    private void animateStatisticsCards() {
        List<Node> statCards = List.of(totalOrdersLabel, totalSpentLabel, averagePriceLabel);

        for (Node card : statCards) {
            FadeTransition fadeIn = new FadeTransition(Duration.millis(800), card);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition moveUp = new TranslateTransition(Duration.millis(800), card);
            moveUp.setFromY(20);
            moveUp.setToY(0);

            fadeIn.play();
            moveUp.play();
        }
    }
}
