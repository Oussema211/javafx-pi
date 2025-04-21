package com.example.cart.controller;

import com.example.cart.model.CartItem;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.beans.property.SimpleStringProperty;
import com.example.cart.CartManager;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.Parent;
import java.io.IOException;

import java.io.File;

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
        // Gestion activation/désactivation du bouton "Commander"
        checkoutButton.setDisable(CartManager.getCartItems().isEmpty());

// Écoute en live si le panier change
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
                            imageView.setImage(new javafx.scene.image.Image(path));
                        } catch (Exception e) {
                            // image par défaut si problème
                            imageView.setImage(new javafx.scene.image.Image("file:src/main/resources/images/default.png"));
                        }
                    }
                    setGraphic(imageView);
                }
            }
        });

        addActionButtonsToTable();

        cartTable.setItems(CartManager.getCartItems());
        updateTotal();
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

            // Cherche le BorderPane principal pour garder la navbar
            BorderPane root = (BorderPane) cartTable.getScene().lookup("#borderPane");
            if (root != null) {
                root.setCenter(checkoutPage);
            } else {
                // Si pas trouvé (théoriquement rare) : ouverture brute
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

}
