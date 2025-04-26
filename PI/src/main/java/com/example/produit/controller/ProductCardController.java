package com.example.produit.controller;

import com.example.auth.utils.SessionManager;
import com.example.cart.CartManager;
import com.example.produit.model.Commentaire;
import com.example.produit.model.Produit;
import com.example.produit.service.CommentaireDAO;
import com.example.produit.service.ProduitDAO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.File;
import java.net.URL;
import java.util.*;

public class ProductCardController implements Initializable {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox productContainer;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;
    private final SessionManager sessionManager = SessionManager.getInstance();

    private List<Produit> allProducts;
    private int currentPage = 1;
    private final int PRODUCTS_PER_PAGE = 6;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            allProducts = ProduitDAO.getAllProducts();
            for (Produit product : allProducts) {
                List<Commentaire> comments = CommentaireDAO.getCommentairesByProduit(product);
                product.setCommentaires(comments != null ? comments : new ArrayList<>());
            }
            updatePage();
        } catch (Exception e) {
            showErrorAlert("Initialization Error", "Failed to load products: " + e.getMessage());
        }
    }

    private void updatePage() {
        productContainer.getChildren().clear();
        int startIndex = (currentPage - 1) * PRODUCTS_PER_PAGE;
        int endIndex = Math.min(startIndex + PRODUCTS_PER_PAGE, allProducts.size());
        HBox row = new HBox(20);
        row.setPadding(new Insets(10));
        row.setAlignment(Pos.CENTER);

        for (int i = startIndex; i < endIndex; i++) {
            VBox productCard = createProductCard(allProducts.get(i));
            row.getChildren().add(productCard);
            if ((i - startIndex + 1) % 4 == 0 || i == endIndex - 1) {
                productContainer.getChildren().add(row);
                row = new HBox(20);
                row.setPadding(new Insets(10));
                row.setAlignment(Pos.CENTER);
            }
        }

        pageLabel.setText("Page " + currentPage);
        prevButton.setDisable(currentPage == 1);
        nextButton.setDisable(endIndex >= allProducts.size());
    }

    @FXML
    private void handlePrevious() {
        if (currentPage > 1) {
            currentPage--;
            updatePage();
        }
    }

    @FXML
    private void handleNext() {
        if (currentPage * PRODUCTS_PER_PAGE < allProducts.size()) {
            currentPage++;
            updatePage();
        }
    }

    private VBox createProductCard(Produit product) {
        VBox card = new VBox(20);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("product-card");

        Label discountBadge = new Label("10% OFF");
        discountBadge.getStyleClass().add("discount-badge");
        discountBadge.setVisible(false);
        VBox.setMargin(discountBadge, new Insets(-5, 0, 0, 0));

        ImageView imageView = new ImageView();
        imageView.getStyleClass().add("image-view");
        try {
            String imagePath = product.getImageName() != null && !product.getImageName().isEmpty()
                    ? new File(product.getImageName()).toURI().toString()
                    : "file:src/main/resources/images/default.png";
            Image image = new Image(imagePath, 180, 180, true, true);
            imageView.setImage(image);
        } catch (Exception e) {
            imageView.setImage(new Image("file:src/main/resources/images/default.png", 180, 180, true, true));
        }

        Label nameLabel = new Label(product.getNom() != null ? product.getNom() : "Unknown");
        nameLabel.getStyleClass().add("product-name");

        HBox ratingBox = createStarRating(getAverageRating(product.getCommentaires()));
        ratingBox.getStyleClass().add("rating-box");

        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label(String.format("$%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("product-price");
        Label quantityLabel = new Label("Qty: " + product.getQuantite());
        quantityLabel.getStyleClass().add("product-quantity");
        priceBox.getChildren().addAll(priceLabel, quantityLabel);

        Label categoryLabel = new Label(product.getCategory() != null && product.getCategory().getNom() != null ? product.getCategory().getNom() : "None");
        categoryLabel.getStyleClass().add("product-category");

        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.getStyleClass().add("add-to-cart-button");
        addToCartButton.setOnAction(event -> {
            CartManager.addProduct(product);
            showAddedNotification(product.getNom());
        });

        // Supprimer l'appel : ne rien faire au clic, ou afficher une simple notification
        card.setOnMouseClicked(event -> {
            // Exemple : afficher un petit message
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Produit Sélectionné");
            alert.setHeaderText(null);
            alert.setContentText("Produit: " + product.getNom());
            alert.showAndWait();
        });

        card.getChildren().addAll(discountBadge, imageView, nameLabel, ratingBox, priceBox, categoryLabel, addToCartButton);
        return card;
    }

    private HBox createStarRating(double rating) {
        HBox starBox = new HBox(3);
        starBox.setAlignment(Pos.CENTER_LEFT);
        int fullStars = (int) rating;
        boolean halfStar = rating - fullStars >= 0.5;
        for (int i = 0; i < 5; i++) {
            Label star = new Label();
            if (i < fullStars) {
                star.setText("★");
                star.getStyleClass().add("star-filled");
            } else if (i == fullStars && halfStar) {
                star.setText("★");
                star.getStyleClass().add("star-half");
            } else {
                star.setText("☆");
                star.getStyleClass().add("star-empty");
            }
            starBox.getChildren().add(star);
        }
        return starBox;
    }

    private double getAverageRating(List<Commentaire> commentaires) {
        if (commentaires == null || commentaires.isEmpty()) return 0;
        double total = 0;
        int count = 0;
        for (Commentaire c : commentaires) {
            if (c.getNote() != null) {
                total += c.getNote();
                count++;
            }
        }
        return count > 0 ? total / count : 0;
    }

    private void showAddedNotification(String productName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Produit ajouté");
        alert.setHeaderText(null);
        alert.setContentText(productName + " a été ajouté au panier !");
        alert.showAndWait();
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStyleClass().add("error-dialog");
        alert.showAndWait();
    }
}
