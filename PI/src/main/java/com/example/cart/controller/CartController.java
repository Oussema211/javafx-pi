package com.example.cart.controller;

import com.example.cart.model.CartItem;
import com.example.cart.CartManager;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;

public class CartController {

    @FXML
    private TableView<CartItem> cartTable;
    @FXML
    private Button checkoutButton;

    @FXML
    private TableColumn<CartItem, String> colProduit;

    @FXML
    private TableColumn<CartItem, Integer> colQuantite;

    @FXML
    private TableColumn<CartItem, Double> colPrix;

    @FXML
    private TableColumn<CartItem, Void> colActions;

    @FXML
    private TableColumn<CartItem, Void> colImage;

    @FXML
    private Label totalLabel;

    @FXML
    public void initialize() {
        checkoutButton.setDisable(CartManager.getCartItems().isEmpty());

        CartManager.getCartItems().addListener((javafx.collections.ListChangeListener<CartItem>) change -> {
            checkoutButton.setDisable(CartManager.getCartItems().isEmpty());
        });

        colProduit.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProduit().getNom()));
        colQuantite.setCellValueFactory(data -> data.getValue().quantiteProperty().asObject());
        colPrix.setCellValueFactory(data -> data.getValue().totalPriceProperty().asObject());

        colImage.setCellFactory(param -> new TableCell<>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(60);
                imageView.setFitHeight(60);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    CartItem cartItem = (CartItem) getTableRow().getItem();
                    if (cartItem != null && cartItem.getProduit().getImageName() != null) {
                        try {
                            String path = new File(cartItem.getProduit().getImageName()).toURI().toString();
                            imageView.setImage(new Image(path));
                        } catch (Exception e) {
                            imageView.setImage(new Image("file:src/main/resources/images/default.png"));
                        }
                    }
                    setGraphic(imageView);
                }
            }
        });

        addActionButtonsToTable();
        cartTable.setItems(CartManager.getCartItems());
        updateTotal();
        animateCartTable();

        FadeTransition fadeBtn1 = new FadeTransition(Duration.millis(800), checkoutButton);
        fadeBtn1.setFromValue(0);
        fadeBtn1.setToValue(1);
        fadeBtn1.play();

        FadeTransition fadeBtn2 = new FadeTransition(Duration.millis(800), totalLabel);
        fadeBtn2.setFromValue(0);
        fadeBtn2.setToValue(1);
        fadeBtn2.play();
    }

    private void addActionButtonsToTable() {
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("❌ Supprimer");

            {
                deleteBtn.setOnAction(event -> {
                    CartItem item = getTableView().getItems().get(getIndex());
                    CartManager.removeProduct(item.getProduit());
                    updateTotal();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteBtn);
                }
            }
        });
    }

    private void updateTotal() {
        totalLabel.setText(String.format("Total : %.2f DT", CartManager.getTotalPrice()));
    }

    @FXML
    private void handleClearCart() {
        CartManager.clearCart();
        updateTotal();
    }

    @FXML
    private void handleCheckout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontPages/pages/checkout.fxml"));
            Parent checkoutPage = loader.load();

            BorderPane root = (BorderPane) cartTable.getScene().lookup("#borderPane");
            if (root != null) {
                root.setCenter(checkoutPage);
            } else {
                Stage stage = (Stage) cartTable.getScene().getWindow();
                stage.getScene().setRoot(checkoutPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOrderHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontPages/pages/OrderHistory.fxml"));
            Parent orderHistoryPage = loader.load();

            BorderPane root = (BorderPane) cartTable.getScene().lookup("#borderPane");
            if (root != null) {
                root.setCenter(orderHistoryPage);
            } else {
                Stage stage = (Stage) cartTable.getScene().getWindow();
                stage.getScene().setRoot(orderHistoryPage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void animateCartTable() {
        javafx.application.Platform.runLater(() -> {
            cartTable.lookupAll(".table-row-cell").forEach(row -> {
                FadeTransition fade = new FadeTransition(Duration.millis(600), row);
                fade.setFromValue(0);
                fade.setToValue(1);

                TranslateTransition slide = new TranslateTransition(Duration.millis(600), row);
                slide.setFromX(-50);
                slide.setToX(0);

                fade.play();
                slide.play();
            });
        });
    }
}
