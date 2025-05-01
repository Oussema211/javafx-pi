package com.example.cart.controller;

import com.example.cart.OrderHistoryManager;
import com.example.cart.model.OrderSummary;
import com.example.cart.model.ProduitCommande;
import com.example.cart.service.ProduitCommandeDAO;
import com.example.cart.view.OrderDetailsDialog;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
    private int currentPage = 1;
    private final int pageSize = 5;
    private List<OrderSummary> allOrders;
    private List<OrderSummary> filteredOrders;
    @FXML private Label pageInfoLabel;

    private void afficherPage(int page) {
        orderContainer.getChildren().clear();

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredOrders.size());

        List<OrderSummary> pageItems = filteredOrders.subList(fromIndex, toIndex);

        for (OrderSummary order : pageItems) {
            VBox card = createOrderCard(order);
            orderContainer.getChildren().add(card);
        }

        pageInfoLabel.setText("Page " + page + " / " + Math.max(1, getTotalPages()));
        if (pageItems.isEmpty()) {
            Label emptyLabel = new Label("Aucune commande trouvée.");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
            orderContainer.getChildren().add(emptyLabel);
        }

    }

    @FXML
    private void initialize() {
        allOrders = OrderHistoryManager.getOrderHistory();
        filteredOrders = allOrders;
        updateStats();
        afficherPage(currentPage);



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
    private int getTotalPages() {
        return (int) Math.ceil((double) filteredOrders.size() / pageSize);
    }

    @FXML
    private void handleNextPage() {
        if (currentPage < getTotalPages()) {
            currentPage++;
            afficherPage(currentPage);
        }
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            afficherPage(currentPage);
        }
    }
    @FXML
    private TextField searchField;

    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();

        if (query.isEmpty()) {
            filteredOrders = allOrders;
        } else {
            filteredOrders = allOrders.stream()
                    .filter(order -> order.getUserId().toLowerCase().contains(query)
                            || order.getDateAchat().toLowerCase().contains(query)
                            || String.valueOf(order.getPrixTotal()).contains(query))
                    .toList();
        }


        currentPage = 1;
        afficherPage(currentPage);
    }
    private VBox createOrderCard(OrderSummary order) {
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

        card.setOnMouseClicked(e -> {
            // Récupération des produits de la commande
            List<ProduitCommande> produits = ProduitCommandeDAO.getProduitsParCommande(order.getId());
            order.setProduitsCommandes(produits);

            // Affichage dans le Dialog
            new OrderDetailsDialog(order).showAndWait();
        });


        return card;
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
