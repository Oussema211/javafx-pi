package com.example.produit.controller;

import com.example.auth.utils.SessionManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

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
        discountBadge.setVisible(false); // Show only if there's a discount
        VBox.setMargin(discountBadge, new Insets(-5, 0, 0, 0));

        // Image
        ImageView imageView = new ImageView();
        imageView.getStyleClass().add("image-view");
        try {
            String imagePath = product.getImageName() != null && !product.getImageName().isEmpty()
                    ? new File(product.getImageName()).toURI().toString()
                    : "file:src/main/resources/images/default.png";
            Image image = new Image(imagePath, 180, 180, true, true);
            imageView.setImage(image);
        } catch (Exception e) {
            Image defaultImage = new Image("file:src/main/resources/images/default.png", 180, 180, true, true);
            imageView.setImage(defaultImage);
        }
        imageView.setFitWidth(180);
        imageView.setFitHeight(180);

        // Name
        Label nameLabel = new Label(product.getNom() != null ? product.getNom() : "Unknown");
        nameLabel.getStyleClass().add("product-name");

        // Rating
        HBox ratingBox = createStarRating(getAverageRating(product.getCommentaires()));
        ratingBox.getStyleClass().add("rating-box");

        // Price and Quantity
        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label(String.format("$%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("product-price");
        Label quantityLabel = new Label("Qty: " + product.getQuantite());
        quantityLabel.getStyleClass().add("product-quantity");
        priceBox.getChildren().addAll(priceLabel, quantityLabel);

        // Category
        Label categoryLabel = new Label(product.getCategory() != null && product.getCategory().getNom() != null ? product.getCategory().getNom() : "None");
        categoryLabel.getStyleClass().add("product-category");

        // Add to Cart Button
        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.getStyleClass().add("add-to-cart-button");
        addToCartButton.setOnAction(event -> {
            System.out.println("Added to cart: " + product.getNom());
        });

        card.setOnMouseClicked(event -> showProductDialog(product));
        card.getChildren().addAll(discountBadge, imageView, nameLabel, ratingBox, priceBox, categoryLabel, addToCartButton);
        return card;
    }

    private void showProductDialog(Produit product) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Product Details");
        dialog.getDialogPane().getStyleClass().add("product-dialog");
        dialog.setHeaderText(null);

        DialogPane dialogPane = dialog.getDialogPane();
        URL cssUrl = getClass().getResource("/com/example/frontPages/pages/products.css");
        if (cssUrl != null) {
            dialogPane.getStylesheets().add(cssUrl.toExternalForm());
            System.out.println("css loaded");
        } else {
            System.err.println("Warning: CSS file /products.css not found. Using default styles.");
            dialogPane.setStyle("-fx-background-color: #F5F7F2; -fx-border-color: #A9CBA4; -fx-border-width: 1; -fx-border-radius: 15; -fx-padding: 10;");
        }

        ButtonType submitButtonType = new ButtonType("Submit Review", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("dialog-content");

        // Top: Image and Basic Info
        HBox topBox = new HBox(15);
        topBox.setAlignment(Pos.CENTER_LEFT);

        // Image Section
        VBox imageBox = new VBox(10);
        imageBox.setAlignment(Pos.CENTER);
        ImageView imageView = new ImageView();
        try {
            String imagePath = product.getImageName() != null && !product.getImageName().isEmpty()
                    ? new File(product.getImageName()).toURI().toString()
                    : "file:src/main/resources/images/default.png";
            Image image = new Image(imagePath, 200, 200, true, true);
            imageView.setImage(image);
        } catch (Exception e) {
            Image defaultImage = new Image("file:src/main/resources/images/default.png", 200, 200, true, true);
            imageView.setImage(defaultImage);
        }
        imageView.setFitWidth(200);
        imageView.setFitHeight(200);
        imageView.getStyleClass().add("dialog-image");
        imageBox.getChildren().add(imageView);

        VBox infoBox = new VBox(10);
        infoBox.setPrefWidth(300);

        Label nameLabel = new Label(product.getNom() != null ? product.getNom() : "Unknown");
        nameLabel.getStyleClass().add("dialog-title");

        // Fetch the latest comments to calculate the rating
        List<Commentaire> latestComments = CommentaireDAO.getCommentairesByProduit(product);
        product.setCommentaires(latestComments != null ? latestComments : new ArrayList<>());
        HBox ratingBox = createStarRating(getAverageRating(product.getCommentaires()));
        ratingBox.getStyleClass().add("dialog-rating");

        Label priceLabel = new Label(String.format("$%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("dialog-price");

        Label quantityLabel = new Label("In Stock: " + product.getQuantite());
        quantityLabel.getStyleClass().add("dialog-quantity");

        Label categoryLabel = new Label("Category: " +
                (product.getCategory() != null && product.getCategory().getNom() != null ? product.getCategory().getNom() : "None"));
        categoryLabel.getStyleClass().add("dialog-category");

        // Quantity Selector and Add to Cart
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);
        Label qtyLabel = new Label("Qty:");
        qtyLabel.getStyleClass().add("qty-label");
        Spinner<Integer> qtySpinner = new Spinner<>(1, Math.max(1, product.getQuantite()), 1);
        qtySpinner.getStyleClass().add("qty-spinner");
        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.getStyleClass().add("dialog-add-to-cart");
        addToCartButton.setOnAction(event -> {
            System.out.println("Added " + qtySpinner.getValue() + " of " + product.getNom() + " to cart");
        });
        actionBox.getChildren().addAll(qtyLabel, qtySpinner, addToCartButton);

        infoBox.getChildren().addAll(nameLabel, ratingBox, priceLabel, quantityLabel, categoryLabel, actionBox);
        topBox.getChildren().addAll(imageBox, infoBox);

        // Tabs: Description and Reviews
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("dialog-tabs");

        // Description Tab
        Tab descTab = new Tab("Description");
        descTab.setClosable(false);
        VBox descContent = new VBox(10);
        Text descriptionText = new Text(product.getDescription() != null ? product.getDescription() : "No description available.");
        descriptionText.getStyleClass().add("dialog-description");
        descriptionText.setWrappingWidth(450);
        descContent.getChildren().add(descriptionText);
        descTab.setContent(descContent);

        // Reviews Tab
        Tab reviewsTab = new Tab("Reviews");
        reviewsTab.setClosable(false);
        VBox reviewsContent = new VBox(15);

        ScrollPane commentsScroll = new ScrollPane();
        VBox commentsBox = new VBox(10);
        commentsScroll.setContent(commentsBox);
        commentsScroll.setFitToWidth(true);
        commentsScroll.setPrefHeight(120);
        commentsScroll.getStyleClass().add("comments-scroll");
        populateComments(commentsBox, product.getCommentaires());

        // Add Review Section
        Label addCommentLabel = new Label("Add Your Review");
        addCommentLabel.getStyleClass().add("add-comment-label");

        HBox ratingInputBox = new HBox(5);
        Label ratingLabel = new Label("Rating:");
        ratingLabel.getStyleClass().add("rating-label");
        ComboBox<Integer> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(1, 2, 3, 4, 5);
        ratingCombo.setPromptText("Select rating");
        ratingCombo.getStyleClass().add("rating-combo");
        HBox starRatingBox = new HBox(5);
        starRatingBox.getStyleClass().add("star-rating-input");
        ratingCombo.valueProperty().addListener((obs, old, newValue) -> {
            starRatingBox.getChildren().clear();
            starRatingBox.getChildren().add(createStarRating(newValue != null ? newValue : 0));
        });
        ratingInputBox.getChildren().addAll(ratingLabel, ratingCombo, starRatingBox);

        TextArea commentTextArea = new TextArea();
        commentTextArea.setPromptText("Write your review...");
        commentTextArea.setPrefRowCount(3);
        commentTextArea.setWrapText(true);
        commentTextArea.getStyleClass().add("input-textarea");

        reviewsContent.getChildren().addAll(commentsScroll, addCommentLabel, ratingInputBox, commentTextArea);
        reviewsTab.setContent(reviewsContent);

        tabPane.getTabs().addAll(descTab, reviewsTab);
        content.getChildren().addAll(topBox, tabPane);
        dialog.getDialogPane().setContent(content);

        // Validation
        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.setDisable(true);

        commentTextArea.textProperty().addListener((obs, old, newValue) -> {
            submitButton.setDisable(
                    newValue.trim().isEmpty() ||
                            ratingCombo.getValue() == null
            );
        });
        ratingCombo.valueProperty().addListener((obs, old, newValue) -> {
            submitButton.setDisable(
                    commentTextArea.getText().trim().isEmpty() ||
                            newValue == null
            );
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == submitButtonType) {
            try {
                String contenu = commentTextArea.getText().trim();
                float note = ratingCombo.getValue();

                Commentaire commentaire = new Commentaire();
                commentaire.setAuteur(sessionManager.getLoggedInUser().getNom());
                commentaire.setContenu(contenu);
                commentaire.setNote(note);
                commentaire.setProduit(product);
                commentaire.setUser(null);

                CommentaireDAO.saveCommentaire(commentaire);
                product.addCommentaire(commentaire);

                commentsBox.getChildren().clear();
                populateComments(commentsBox, product.getCommentaires());

                ratingBox.getChildren().clear();
                ratingBox.getChildren().add(createStarRating(getAverageRating(product.getCommentaires())));
                updatePage(); // Refresh cards to update rating
            } catch (Exception e) {
                showErrorAlert("Comment Error", "Failed to save comment: " + e.getMessage());
            }
        }
    }

    private void populateComments(VBox commentsBox, List<Commentaire> commentaires) {
        if (commentaires == null || commentaires.isEmpty()) {
            Label noCommentsLabel = new Label("No reviews yet.");
            noCommentsLabel.getStyleClass().add("comment-text");
            commentsBox.getChildren().add(noCommentsLabel);
        } else {
            for (Commentaire c : commentaires) {
                VBox commentBox = new VBox(5);
                commentBox.getStyleClass().add("comment-box");

                HBox authorRatingBox = new HBox(10);
                Label authorLabel = new Label(c.getAuteur() != null ? c.getAuteur() : "Anonymous");
                authorLabel.getStyleClass().add("comment-author");
                HBox commentStars = createStarRating(c.getNote() != null ? c.getNote() : 0);
                commentStars.getStyleClass().add("comment-rating");
                authorRatingBox.getChildren().addAll(authorLabel, commentStars);

                Text contentText = new Text(c.getContenu() != null ? truncateText(c.getContenu(), 200) : "");
                contentText.getStyleClass().add("comment-text");

                Label dateLabel = new Label(c.getDateCreation() != null ? c.getDateCreation().toString() : "Unknown");
                dateLabel.getStyleClass().add("comment-date");

                commentBox.getChildren().addAll(authorRatingBox, contentText, dateLabel);
                commentsBox.getChildren().add(commentBox);
            }
        }
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

    private boolean isValidRating(String rating) {
        try {
            float value = Float.parseFloat(rating);
            return value >= 0 && value <= 5;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
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