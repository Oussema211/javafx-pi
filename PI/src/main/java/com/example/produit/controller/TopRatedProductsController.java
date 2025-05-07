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
import org.json.JSONObject;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;

public class TopRatedProductsController implements Initializable {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox productContainer;
    @FXML private HBox fastBuyContainer;
    @FXML private Button fastBuyButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Label pageLabel;
    @FXML private HBox paginationControls;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private List<Produit> topProducts = new ArrayList<>();
    private int currentPage = 1;
    private static final int PRODUCTS_PER_PAGE = 8;
    private static final String API_URL = "http://localhost:5000/predict";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            configureScrollPane();
            loadTopProducts();
            configureFastBuyButton();
            updatePage();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to load top products: " + e.getMessage());
        }
    }

    private void configureScrollPane() {
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    private void configureFastBuyButton() {
        fastBuyButton.setOnAction(e -> {
            try {
                FastBuyController fastBuyController = new FastBuyController();
                fastBuyController.showFastBuyDialog();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Fast Buy Error", "Failed to open Fast Buy dialog: " + ex.getMessage());
            }
        });
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
        if (currentPage * PRODUCTS_PER_PAGE < topProducts.size()) {
            currentPage++;
            updatePage();
        }
    }

    private void updatePage() {
        productContainer.getChildren().clear();
        int startIndex = (currentPage - 1) * PRODUCTS_PER_PAGE;
        int endIndex = Math.min(startIndex + PRODUCTS_PER_PAGE, topProducts.size());

        HBox row = createRow();
        for (int i = startIndex; i < endIndex; i++) {
            row.getChildren().add(createProductCard(topProducts.get(i)));
            if ((i - startIndex + 1) % 4 == 0 || i == endIndex - 1) {
                productContainer.getChildren().add(row);
                row = createRow();
            }
        }

        pageLabel.setText("Page " + currentPage);
        prevButton.setDisable(currentPage == 1);
        nextButton.setDisable(endIndex >= topProducts.size());
    }

    private HBox createRow() {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private void loadTopProducts() {
        List<Produit> allProducts = ProduitDAO.getAllProducts();
        if (allProducts == null) {
            allProducts = new ArrayList<>();
        }
        allProducts.forEach(product -> product.setCommentaires(
                CommentaireDAO.getCommentairesByProduit(product)));

        List<ProductWithScore> productsWithScores = allProducts.stream()
                .map(product -> new ProductWithScore(product, calculatePositivityScore(product)))
                .filter(pws -> pws.score > 0)
                .sorted((p1, p2) -> Double.compare(p2.score, p1.score))
                .limit(10)
                .collect(Collectors.toList());

        topProducts = productsWithScores.stream()
                .map(pws -> pws.product)
                .collect(Collectors.toList());
    }

    private double calculatePositivityScore(Produit product) {
        List<Commentaire> commentaires = product.getCommentaires();
        if (commentaires == null || commentaires.isEmpty()) {
            return 0.0;
        }

        double totalPositiveConfidence = 0.0;
        int positiveCount = 0;

        for (Commentaire commentaire : commentaires) {
            String review = commentaire.getContenu();
            if (review == null || review.trim().isEmpty()) {
                continue;
            }

            try {
                JSONObject json = new JSONObject();
                json.put("review", review);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(response.body());
                    String sentiment = responseJson.getString("sentiment");
                    double confidence = responseJson.getDouble("confidence");

                    if ("Positive".equals(sentiment)) {
                        totalPositiveConfidence += confidence;
                        positiveCount++;
                    }
                } else {
                    System.err.println("API error for review: " + review + ", Status: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("Error processing review: " + review + ", Error: " + e.getMessage());
            }
        }

        return positiveCount > 0 ? totalPositiveConfidence / commentaires.size() : 0.0;
    }

    private VBox createProductCard(Produit product) {
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setPadding(new Insets(8));

        Label discountBadge = new Label("10% OFF");
        discountBadge.getStyleClass().add("discount-badge");

        ImageView imageView = createProductImage(product, 120, 120);
        imageView.getStyleClass().add("product-image");

        Label nameLabel = new Label(product.getNom() != null ? product.getNom() : "Unknown");
        nameLabel.getStyleClass().add("product-name");

        HBox ratingBox = createStarRating(getAverageRating(product.getCommentaires()));

        HBox priceBox = new HBox(6);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label(String.format("$%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("product-price");
        Label quantityLabel = new Label("Qty: " + product.getQuantite());
        quantityLabel.getStyleClass().add("product-quantity");
        priceBox.getChildren().addAll(priceLabel, quantityLabel);

        Label categoryLabel = new Label(product.getCategory() != null ? product.getCategory().getNom() : "None");
        categoryLabel.getStyleClass().add("product-category");

        double positivityScore = calculatePositivityScore(product);
        Label positivityLabel = new Label(String.format("Positivity: %.2f%%", positivityScore * 100));
        positivityLabel.getStyleClass().add("product-positivity");

        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.getStyleClass().add("add-to-cart-button");
        addToCartButton.setOnAction(e -> {
            CartManager.addProduct(product);
            showAddedNotification(product.getNom());
        });

        card.setOnMouseClicked(e -> showProductDialog(product));

        card.getChildren().addAll(discountBadge, imageView, nameLabel, ratingBox, priceBox, categoryLabel, positivityLabel, addToCartButton);
        return card;
    }

    private ImageView createProductImage(Produit product, double width, double height) {
        ImageView imageView = new ImageView();
        String imagePath = product.getImageName() != null && !product.getImageName().isEmpty()
                ? new File(product.getImageName()).toURI().toString()
                : "file:src/main/resources/images/default.png";
        Image image = new Image(imagePath, width, height, true, true, true);
        imageView.setImage(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        return imageView;
    }

    private void showProductDialog(Produit product) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(product.getNom());
        dialog.getDialogPane().getStyleClass().add("product-dialog");
        dialog.getDialogPane().setPrefSize(800, 600);

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/frontPages/pages/products.css").toExternalForm());

        HBox content = new HBox(15);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_LEFT);

        // Left side: Product image and details
        VBox leftBox = new VBox(10);
        leftBox.setPrefWidth(320);
        leftBox.setAlignment(Pos.TOP_CENTER);

        ImageView imageView = createProductImage(product, 200, 200);
        imageView.getStyleClass().add("dialog-image");

        VBox detailsBox = new VBox(8);
        Label nameLabel = new Label(product.getNom());
        nameLabel.getStyleClass().add("dialog-title");
        HBox ratingBox = createStarRating(getAverageRating(product.getCommentaires()));
        Label priceLabel = new Label(String.format("$%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("dialog-price");
        Label quantityLabel = new Label("Stock: " + product.getQuantite());
        quantityLabel.getStyleClass().add("dialog-quantity");
        Label categoryLabel = new Label("Category: " + (product.getCategory() != null ? product.getCategory().getNom() : "None"));
        categoryLabel.getStyleClass().add("dialog-category");
        Label positivityLabel = new Label(String.format("Positivity Score: %.2f%%", calculatePositivityScore(product) * 100));
        positivityLabel.getStyleClass().add("dialog-positivity");

        HBox actionBox = new HBox(8);
        Spinner<Integer> qtySpinner = new Spinner<>(1, Math.max(1, product.getQuantite()), 1);
        qtySpinner.setPrefWidth(100);
        qtySpinner.getStyleClass().add("qty-spinner");
        actionBox.getChildren().addAll(new Label("Qty:"), qtySpinner);

        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.getStyleClass().add("add-to-cart-button");
        addToCartButton.setOnAction(e -> {
            CartManager.addProduct(product);
            showAddedNotification(product.getNom());
        });

        detailsBox.getChildren().addAll(nameLabel, ratingBox, priceLabel, quantityLabel, categoryLabel, positivityLabel, actionBox, addToCartButton);
        leftBox.getChildren().addAll(imageView, detailsBox);

        // Right side: Description, reviews, and add review
        VBox rightBox = new VBox(10);
        rightBox.setPrefWidth(465);
        rightBox.setAlignment(Pos.TOP_LEFT);

        Text descriptionText = new Text(product.getDescription() != null ? product.getDescription() : "No description available.");
        descriptionText.getStyleClass().add("dialog-description");
        descriptionText.setWrappingWidth(400);

        ScrollPane reviewsScroll = new ScrollPane();
        reviewsScroll.getStyleClass().add("reviews-scroll");
        reviewsScroll.setPrefHeight(200);
        reviewsScroll.setFitToWidth(true);
        reviewsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox reviewsBox = new VBox(8);
        reviewsBox.getStyleClass().add("reviews-box");
        reviewsScroll.setContent(reviewsBox);
        List<Commentaire> commentaires = product.getCommentaires();
        populateComments(reviewsBox, commentaires != null ? commentaires : new ArrayList<>());

        VBox addReviewBox = new VBox(8);
        addReviewBox.getStyleClass().add("add-review-box");
        Label addReviewLabel = new Label("Add Review");
        addReviewLabel.getStyleClass().add("add-review-label");
        ComboBox<Integer> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(1, 2, 3, 4, 5);
        ratingCombo.setPromptText("Rating");
        ratingCombo.getStyleClass().add("rating-combo");
        TextArea commentTextArea = new TextArea();
        commentTextArea.setPromptText("Your review...");
        commentTextArea.setPrefRowCount(3);
        commentTextArea.setPrefHeight(80);
        commentTextArea.getStyleClass().add("input-textarea");
        addReviewBox.getChildren().addAll(addReviewLabel, ratingCombo, commentTextArea);

        rightBox.getChildren().addAll(descriptionText, reviewsScroll, addReviewBox);
        content.getChildren().addAll(leftBox, rightBox);
        dialog.getDialogPane().setContent(content);

        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.setDisable(true);
        commentTextArea.textProperty().addListener((obs, old, newValue) ->
                submitButton.setDisable(newValue.trim().isEmpty() || ratingCombo.getValue() == null));
        ratingCombo.valueProperty().addListener((obs, old, newValue) ->
                submitButton.setDisable(commentTextArea.getText().trim().isEmpty() || newValue == null));

        dialog.showAndWait().ifPresent(result -> {
            if (result == submitButtonType) {
                try {
                    Commentaire commentaire = new Commentaire();
                    commentaire.setAuteur(sessionManager.getLoggedInUser().getNom());
                    commentaire.setContenu(commentTextArea.getText().trim());
                    commentaire.setNote(ratingCombo.getValue().floatValue());
                    commentaire.setProduit(product);
                    CommentaireDAO.saveCommentaire(commentaire);
                    product.addCommentaire(commentaire);
                    reviewsBox.getChildren().clear();
                    populateComments(reviewsBox, product.getCommentaires());
                } catch (Exception e) {
                    showAlert(Alert.AlertType.ERROR, "Comment Error", "Failed to save comment: " + e.getMessage());
                }
            }
        });
    }

    private void populateComments(VBox reviewsBox, List<Commentaire> commentaires) {
        reviewsBox.getChildren().clear();
        if (commentaires.isEmpty()) {
            reviewsBox.getChildren().add(new Label("No reviews yet."));
            return;
        }

        for (Commentaire c : commentaires) {
            VBox commentBox = new VBox(5);
            commentBox.getStyleClass().add("comment-box");
            Label authorLabel = new Label(c.getAuteur() != null ? c.getAuteur() : "Anonymous");
            authorLabel.getStyleClass().add("comment-author");
            HBox stars = createStarRating(c.getNote() != null ? c.getNote() : 0);
            Text commentText = new Text(truncateText(c.getContenu() != null ? c.getContenu() : "", 100));
            commentText.getStyleClass().add("comment-text");
            commentBox.getChildren().addAll(authorLabel, stars, commentText);
            reviewsBox.getChildren().add(commentBox);
        }
    }

    private double getAverageRating(List<Commentaire> commentaires) {
        if (commentaires == null || commentaires.isEmpty()) return 0;
        return commentaires.stream()
                .filter(c -> c.getNote() != null)
                .mapToDouble(Commentaire::getNote)
                .average()
                .orElse(0);
    }

    private HBox createStarRating(double rating) {
        HBox starBox = new HBox(3);
        int fullStars = (int) rating;
        boolean halfStar = rating - fullStars >= 0.5;

        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < fullStars ? "★" : (i == fullStars && halfStar ? "★" : "☆"));
            star.getStyleClass().add(i < fullStars ? "star-filled" : (i == fullStars && halfStar ? "star-half" : "star-empty"));
            starBox.getChildren().add(star);
        }
        return starBox;
    }

    private String truncateText(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStyleClass().add("dialog");
        alert.showAndWait();
    }

    private void showAddedNotification(String productName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Produit ajouté");
        alert.setHeaderText(null);
        alert.setContentText(productName + " a été ajouté au panier !");
        alert.showAndWait();
    }

    private static class ProductWithScore {
        Produit product;
        double score;

        ProductWithScore(Produit product, double score) {
            this.product = product;
            this.score = score;
        }
    }
}