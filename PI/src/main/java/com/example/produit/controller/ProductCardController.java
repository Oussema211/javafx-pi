package com.example.produit.controller;

import com.example.produit.model.Produit;
import com.example.produit.service.ProduitDAO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ProductCardController implements Initializable {

    @FXML private VBox productContainer;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;

    private List<Produit> allProducts;
    private int currentPage = 1;
    private final int PRODUCTS_PER_PAGE = 12; // 3x4 grid
    private final int CARDS_PER_ROW = 4;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Load all products once
        allProducts = ProduitDAO.getAllProducts();
        updatePage();
    }

    private void updatePage() {
        productContainer.getChildren().clear();

        // Calculate start and end indices for the current page
        int startIndex = (currentPage - 1) * PRODUCTS_PER_PAGE;
        int endIndex = Math.min(startIndex + PRODUCTS_PER_PAGE, allProducts.size());

        // Create rows of cards
        HBox currentRow = null;
        for (int i = startIndex; i < endIndex; i++) {
            if (i % CARDS_PER_ROW == 0) {
                if (currentRow != null) {
                    productContainer.getChildren().add(currentRow);
                }
                currentRow = new HBox(20);
                currentRow.setPadding(new Insets(0, 10, 0, 10));
            }
            currentRow.getChildren().add(createProductCard(allProducts.get(i)));
        }
        if (currentRow != null && !currentRow.getChildren().isEmpty()) {
            productContainer.getChildren().add(currentRow);
        }

        // Update pagination controls
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
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.getStyleClass().add("product-card");

        // Image Container
        VBox imageContainer = new VBox();
        imageContainer.getStyleClass().add("image-container");
        imageContainer.setAlignment(javafx.geometry.Pos.CENTER);

        // Image
        ImageView imageView = new ImageView();
        imageView.getStyleClass().add("image-view");
        try {
            String imagePath = product.getImageName() != null && !product.getImageName().isEmpty()
                    ? new File(product.getImageName()).toURI().toString()
                    : "file:src/main/resources/images/default.png";
            Image image = new Image(imagePath, 150, 150, true, true);
            imageView.setImage(image);
        } catch (Exception e) {
            Image defaultImage = new Image("file:src/main/resources/images/default.png", 150, 150, true, true);
            imageView.setImage(defaultImage);
        }
        imageView.setFitWidth(150);
        imageView.setFitHeight(150);

        // Product Details
        Label nameLabel = new Label(product.getNom() != null ? product.getNom() : "Unknown");
        nameLabel.getStyleClass().add("product-name");

        Text descriptionText = new Text(product.getDescription() != null ? product.getDescription() : "No description");
        descriptionText.getStyleClass().add("product-description");
        descriptionText.setWrappingWidth(230);

        Label priceLabel = new Label(String.format("Price: $%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("product-price");

        Label quantityLabel = new Label("Quantity: " + product.getQuantite());
        quantityLabel.getStyleClass().add("product-quantity");

        Label categoryLabel = new Label("Category: " +
                (product.getCategory() != null && product.getCategory().getNom() != null ? product.getCategory().getNom() : "None"));
        categoryLabel.getStyleClass().add("product-category");

        // Add to Cart Button
        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.getStyleClass().add("add-to-cart-button");
        addToCartButton.setOnAction(event -> {
            System.out.println("Added to cart: " + product.getNom());
        });

        card.getChildren().addAll(imageView, nameLabel, descriptionText, priceLabel, quantityLabel, categoryLabel, addToCartButton);
        return card;
    }
}