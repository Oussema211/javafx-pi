package com.example.cart.controller;


import com.example.auth.utils.AuthManager;
import com.example.cart.service.CarteVirtuelleManager;
import com.example.cart.service.RecommendationService;
import com.example.produit.model.Produit;
import com.example.produit.service.ProduitDAO;      // si besoin d’accéder à l’image
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;                 // <-- nouveau
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import com.example.cart.service.RecommendationService;
import com.example.produit.model.Produit;
import com.example.produit.service.ProduitDAO;      // déjà ok
// si ton projet possède une classe pour l'utilisateur connecté : // adapte le package si besoin

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.example.cart.service.CarteVirtuelleManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class CartController {
    @FXML private HBox recoBox;   // conteneur ajouté dans le FXML

    @FXML
    private TableView<CartItem> cartTable;
    @FXML
    private Button checkoutButton;
    @FXML
    private Button carteVirtuelleButton; // Nouveau bouton pour ouvrir la carte virtuelle
    @FXML
    private Button payerAvecCarteVirtuelleButton; // Le bouton payer
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
        loadRecommendations();   // <= nouvelles suggestions

        FadeTransition fadeBtn1 = new FadeTransition(Duration.millis(800), checkoutButton);
        fadeBtn1.setFromValue(0);
        fadeBtn1.setToValue(1);
        fadeBtn1.play();

        FadeTransition fadeBtn2 = new FadeTransition(Duration.millis(800), totalLabel);
        fadeBtn2.setFromValue(0);
        fadeBtn2.setToValue(1);
        fadeBtn2.play();


        payerAvecCarteVirtuelleButton.setDisable(!CarteVirtuelleManager.isCarteActive());


        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(
                        javafx.util.Duration.seconds(2),
                        event -> {
                            if (CarteVirtuelleManager.isCarteActive()) {
                                payerAvecCarteVirtuelleButton.setDisable(false);
                            }
                        }
                )
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }
    private void loadRecommendations() {
        System.out.println("📦 Chargement des recommandations globales...");

        List<Produit> recos = RecommendationService.getMostFrequentRecommendations(5);
        System.out.println("🔎 Produits recommandés trouvés : " + recos.size());

        recoBox.getChildren().clear();

        if (recos == null || recos.isEmpty()) {
            Label noRecos = new Label("Aucune recommandation disponible.");
            noRecos.setStyle("-fx-text-fill: white; -fx-font-size: 14;");
            recoBox.getChildren().add(noRecos);
            return;
        }

        for (Produit p : recos) {
            VBox card = new VBox(5);
            card.setAlignment(Pos.CENTER);
            card.setPrefWidth(110);
            card.setStyle("""
            -fx-background-color: #ffffff;
            -fx-background-radius: 10;
            -fx-padding: 8;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 2);
            -fx-cursor: hand;
        """);

            ImageView img = new ImageView();
            img.setFitWidth(80);
            img.setFitHeight(80);
            img.setPreserveRatio(true);

            boolean imageLoaded = false;

            try {
                String imgPath = p.getImageName();
                if (imgPath != null && !imgPath.isBlank()) {
                    File imageFile = new File(imgPath);
                    if (imageFile.exists()) {
                        img.setImage(new Image(imageFile.toURI().toString()));
                        imageLoaded = true;
                    } else {
                        System.out.println("⚠️ Image introuvable : " + imageFile.getAbsolutePath());
                    }
                } else {
                    System.out.println("⚠️ Aucun chemin d'image défini pour le produit : " + p.getNom());
                }
            } catch (Exception e) {
                System.out.println("❌ Erreur lors du chargement de l'image : " + e.getMessage());
            }

            Label fallbackEmoji = new Label("📦");
            fallbackEmoji.setStyle("-fx-font-size: 30px;");

            Label name = new Label(p.getNom());
            name.setWrapText(true);
            name.setStyle("-fx-font-size: 11px; -fx-text-fill: #333333;");

            if (imageLoaded) {
                card.getChildren().addAll(img, name);
            } else {
                card.getChildren().addAll(fallbackEmoji, name);
            }

            card.setOnMouseClicked(e -> {
                CartManager.addProduct(p);
                cartTable.refresh();
                updateTotal();
                loadRecommendations();
            });

            recoBox.getChildren().add(card);
        }
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
    private void showAlert(Alert.AlertType type, String titre, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleCarteVirtuelle() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontPages/pages/CarteVirtuelle.fxml"));
            Parent cartePage = loader.load();

            BorderPane root = (BorderPane) cartTable.getScene().lookup("#borderPane");
            if (root != null) {
                root.setCenter(cartePage);
            } else {
                Stage stage = (Stage) cartTable.getScene().getWindow();
                stage.getScene().setRoot(cartePage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    private void handlePayerAvecCarteVirtuelle() {
        if (!CarteVirtuelleManager.isCarteActive()) {
            showAlert(Alert.AlertType.ERROR, "Carte Inactive", "Veuillez activer votre carte virtuelle avant de payer !");
            return;
        }

        TextInputDialog passwordDialog = new TextInputDialog();
        passwordDialog.setTitle("Authentification Requise");
        passwordDialog.setHeaderText("Entrez le mot de passe de votre carte virtuelle pour payer :");
        passwordDialog.setContentText("Mot de passe :");

        passwordDialog.showAndWait().ifPresent(inputPassword -> {
            if (CarteVirtuelleManager.verifierMotDePasse(inputPassword)) {
                if (CartManager.getCartItems().isEmpty()) {
                    showAlert(Alert.AlertType.WARNING, "Panier Vide", "Ajoutez des produits avant de payer.");
                    return;
                }
                double totalAmount = CartManager.getCartItems().stream()
                        .mapToDouble(item -> item.getProduit().getPrixUnitaire() * item.getQuantite())
                        .sum();


                totalAmount = Math.round(totalAmount * 100.0) / 100.0;



                boolean paymentSuccess = CarteVirtuelleManager.effectuerPaiement(totalAmount);

                if (paymentSuccess) {
                    CartManager.clearCart();
                    updateTotal();
                    showAlert(Alert.AlertType.INFORMATION, "Paiement Réussi", "Merci pour votre achat !");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Échec du Paiement", "Solde insuffisant pour effectuer ce paiement.");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Mot de passe incorrect", "Le mot de passe saisi est invalide.");
            }
        });
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
