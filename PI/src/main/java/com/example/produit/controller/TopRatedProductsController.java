package com.example.produit.controller;

import com.example.auth.utils.SessionManager;
import com.example.cart.CartManager;
import com.example.produit.model.Commentaire;
import com.example.produit.model.Produit;
import com.example.produit.service.CommentaireDAO;
import com.example.produit.service.FavoriteDAO;
import com.example.produit.service.ProduitDAO;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.json.JSONObject;

import java.io.InputStream;
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
    private static final String API_URL = "https://payments-terms-mechanical-kingdom.trycloudflare.com/predict";
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            configureScrollPane();
            loadTopProducts();
            configureFastBuyButton();
            updatePage();
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur d'initialisation", "Échec du chargement des produits les mieux notés : " + e.getMessage());
        }
    }

    private void configureScrollPane() {
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    private void configureFastBuyButton() {
        fastBuyButton.setOnAction(e -> {
            try {
                FastBuyController fastBuyController = new FastBuyController();
                fastBuyController.showFastBuyDialog();
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Erreur d'achat rapide", "Échec de l'ouverture de la boîte de dialogue d'achat rapide : " + ex.getMessage());
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
        System.out.println("getAllProducts: Retrieved " + allProducts.size() + " products");
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
        boolean apiFailed = false;

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
                System.out.println("API Response for review '" + review + "': " + response.body());

                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    if (!responseBody.trim().startsWith("{")) {
                        System.err.println("Invalid JSON response for review '" + review + "': Response does not start with '{'. Response: " + responseBody);
                        apiFailed = true;
                        continue;
                    }

                    JSONObject responseJson = new JSONObject(responseBody);
                    String sentiment = responseJson.optString("sentiment", "Unknown");
                    double confidence = responseJson.optDouble("confidence", 0.0);

                    if ("Positive".equalsIgnoreCase(sentiment)) {
                        totalPositiveConfidence += confidence;
                        positiveCount++;
                    }
                } else {
                    System.err.println("API error for review '" + review + "': Status " + response.statusCode() + ", Response: " + response.body());
                    apiFailed = true;
                }
            } catch (Exception e) {
                System.err.println("Error processing review '" + review + "': " + e.getMessage());
                apiFailed = true;
            }
        }

        if (apiFailed || positiveCount == 0) {
            // Fallback to average rating if API fails or no positive sentiments
            double averageRating = getAverageRating(commentaires);
            System.out.println("Falling back to average rating for product '" + product.getNom() + "': " + averageRating);
            return averageRating / 5.0; // Normalize to 0-1 scale
        }

        return totalPositiveConfidence / commentaires.size();
    }

    private VBox createProductCard(Produit product) {
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setPadding(new Insets(8));

        // Discount Badge and Favorite
        HBox topBar = new HBox(8);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        Label discountBadge = new Label("10% OFF");
        discountBadge.getStyleClass().add("discount-badge");
        Button favoriteButton = createFavoriteButton(product);
        topBar.getChildren().addAll(discountBadge, favoriteButton);
        HBox.setHgrow(favoriteButton, Priority.ALWAYS); // Push favorite to right

        // Image
        ImageView imageView = createProductImage(product, 120, 120);
        imageView.getStyleClass().add("product-image");

        // Name
        Label nameLabel = new Label(product.getNom() != null ? product.getNom() : "Inconnu");
        nameLabel.getStyleClass().add("product-name");

        // Rating
        HBox ratingBox = createStarRating(getAverageRating(product.getCommentaires()));

        // Price and Quantity
        HBox priceBox = new HBox(6);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label(String.format("$%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("product-price");
        Label quantityLabel = new Label("Qté : " + product.getQuantite());
        quantityLabel.getStyleClass().add("product-quantity");
        priceBox.getChildren().addAll(priceLabel, quantityLabel);

        // Category
        Label categoryLabel = new Label(product.getCategory() != null ? product.getCategory().getNom() : "Aucune");
        categoryLabel.getStyleClass().add("product-category");

        // Positivity Score
        double positivityScore = calculatePositivityScore(product);
        Label positivityLabel = new Label(String.format("Score de positivité : %.2f%%", positivityScore * 100));
        positivityLabel.getStyleClass().add("product-positivity");

        // Add to Cart Button
        Button addToCartButton = new Button("Ajouter au panier");
        addToCartButton.getStyleClass().add("add-to-cart-button");
        addToCartButton.setOnAction(e -> {
            CartManager.addProduct(product);
            showAddedNotification(product.getNom());
        });

        // Card click shows product dialog
        card.setOnMouseClicked(e -> showProductDialog(product));

        card.getChildren().addAll(topBar, imageView, nameLabel, ratingBox, priceBox, categoryLabel, positivityLabel, addToCartButton);
        return card;
    }

    private Button createFavoriteButton(Produit product) {
        Button favoriteButton = new Button();
        favoriteButton.getStyleClass().add("favorite-button");

        // Use Unicode heart symbol as a fallback instead of relying on heart.png
        Label heartLabel = new Label("♥");
        heartLabel.getStyleClass().add("favorite-fallback");
        favoriteButton.setGraphic(heartLabel);

        updateFavoriteButtonStyle(favoriteButton, product.isFavoritedByCurrentUser());

        favoriteButton.setOnAction(e -> {
            UUID userId = sessionManager.getLoggedInUser() != null ? sessionManager.getLoggedInUser().getId() : null;
            if (userId == null) {
                showAlert(Alert.AlertType.WARNING, "Connexion requise", "Veuillez vous connecter pour ajouter des favoris.");
                return;
            }
            if (product.isFavoritedByCurrentUser()) {
                FavoriteDAO.removeFavorite(userId, product.getId());
                updateFavoriteButtonStyle(favoriteButton, false);
                showAlert(Alert.AlertType.INFORMATION, "Favori supprimé", product.getNom() + " a été supprimé des favoris.");
            } else {
                FavoriteDAO.addFavorite(userId, product.getId());
                updateFavoriteButtonStyle(favoriteButton, true);
                showAlert(Alert.AlertType.INFORMATION, "Favori ajouté", product.getNom() + " a été ajouté aux favoris.");
            }
        });

        return favoriteButton;
    }

    private void updateFavoriteButtonStyle(Button favoriteButton, boolean isFavorited) {
        if (isFavorited) {
            favoriteButton.getStyleClass().remove("favorite-empty");
            favoriteButton.getStyleClass().add("favorite-filled");
        } else {
            favoriteButton.getStyleClass().remove("favorite-filled");
            favoriteButton.getStyleClass().add("favorite-empty");
        }
    }

    private ImageView createProductImage(Produit product, double width, double height) {
        ImageView imageView = new ImageView();
        Image image = null;

        // Try loading the product's image from its URL
        String imageUrl = product.getImageName();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            try {
                image = new Image(imageUrl, width, height, true, true, true);
                if (image.isError()) {
                    System.err.println("Erreur lors du chargement de l'image du produit depuis l'URL : " + imageUrl);
                    image = null;
                }
            } catch (Exception e) {
                System.err.println("Échec du chargement de l'image du produit depuis l'URL " + imageUrl + " : " + e.getMessage());
                image = null;
            }
        } else {
            System.err.println("L'URL de l'image du produit est nulle ou vide pour le produit : " + product.getNom());
        }

        // Fallback to default image if the product image fails to load
        if (image == null) {
            InputStream defaultStream = getClass().getResourceAsStream("/images/default.png");
            if (defaultStream != null) {
                image = new Image(defaultStream, width, height, true, true);
            } else {
                System.err.println("Avertissement : default.png non trouvé à /images/default.png");
                image = null; // No fallback image available
            }
        }

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

        ButtonType submitButtonType = new ButtonType("Soumettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/frontPages/pages/products.css").toExternalForm());

        HBox content = new HBox(15);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_LEFT);

        // Left Section: Image and Details
        VBox leftBox = new VBox(10);
        leftBox.setPrefWidth(320);
        leftBox.setAlignment(Pos.TOP_CENTER);

        // Image
        ImageView imageView = createProductImage(product, 200, 200);
        imageView.getStyleClass().add("dialog-image");

        // Details
        VBox detailsBox = new VBox(8);
        HBox titleBar = new HBox(8);
        Label nameLabel = new Label(product.getNom());
        nameLabel.getStyleClass().add("dialog-title");
        Button favoriteButton = createFavoriteButton(product);
        titleBar.getChildren().addAll(nameLabel, favoriteButton);

        HBox ratingBox = createStarRating(getAverageRating(product.getCommentaires()));
        Label priceLabel = new Label(String.format("$%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("dialog-price");
        Label quantityLabel = new Label("Stock : " + product.getQuantite());
        quantityLabel.getStyleClass().add("dialog-quantity");
        Label categoryLabel = new Label("Catégorie : " + (product.getCategory() != null ? product.getCategory().getNom() : "Aucune"));
        categoryLabel.getStyleClass().add("dialog-category");
        Label positivityLabel = new Label(String.format("Score de positivité : %.2f%%", calculatePositivityScore(product) * 100));
        positivityLabel.getStyleClass().add("dialog-positivity");

        HBox actionBox = new HBox(8);
        Spinner<Integer> qtySpinner = new Spinner<>(1, Math.max(1, product.getQuantite()), 1);
        qtySpinner.setPrefWidth(100);
        qtySpinner.getStyleClass().add("qty-spinner");
        actionBox.getChildren().addAll(new Label("Qté :"), qtySpinner);

        Button addToCartButton = new Button("Ajouter au panier");
        addToCartButton.getStyleClass().add("add-to-cart-button");
        addToCartButton.setOnAction(e -> {
            CartManager.addProduct(product);
            showAddedNotification(product.getNom());
        });

        detailsBox.getChildren().addAll(titleBar, ratingBox, priceLabel, quantityLabel, categoryLabel, positivityLabel, actionBox, addToCartButton);
        leftBox.getChildren().addAll(imageView, detailsBox);

        // Right Section: Description, Reviews, Add Review
        VBox rightBox = new VBox(10);
        rightBox.setPrefWidth(465);
        rightBox.setAlignment(Pos.TOP_LEFT);

        // Description
        Text descriptionText = new Text(product.getDescription() != null ? product.getDescription() : "Aucune description disponible.");
        descriptionText.getStyleClass().add("dialog-description");
        descriptionText.setWrappingWidth(400);

        // Reviews with ScrollPane
        ScrollPane reviewsScroll = new ScrollPane();
        reviewsScroll.getStyleClass().add("reviews-scroll");
        reviewsScroll.setPrefHeight(200);
        reviewsScroll.setFitToWidth(true);
        reviewsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox reviewsBox = new VBox(8);
        reviewsBox.getStyleClass().add("reviews-box");
        List<Commentaire> commentaires = product.getCommentaires();
        populateComments(reviewsBox, commentaires != null ? commentaires : new ArrayList<>());
        reviewsScroll.setContent(reviewsBox);

        // Add Review
        VBox addReviewBox = new VBox(8);
        addReviewBox.getStyleClass().add("add-review-box");
        Label addReviewLabel = new Label("Ajouter un avis");
        addReviewLabel.getStyleClass().add("add-review-label");
        ComboBox<Integer> ratingCombo = new ComboBox<>();
        ratingCombo.getItems().addAll(1, 2, 3, 4, 5);
        ratingCombo.setPromptText("Note");
        ratingCombo.getStyleClass().add("rating-combo");
        TextArea commentTextArea = new TextArea();
        commentTextArea.setPromptText("Votre avis...");
        commentTextArea.setPrefRowCount(3);
        commentTextArea.setPrefHeight(80);
        commentTextArea.getStyleClass().add("input-textarea");
        addReviewBox.getChildren().addAll(addReviewLabel, ratingCombo, commentTextArea);

        rightBox.getChildren().addAll(descriptionText, reviewsScroll, addReviewBox);
        content.getChildren().addAll(leftBox, rightBox);
        dialog.getDialogPane().setContent(content);

        // Validation
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
                    showAlert(Alert.AlertType.ERROR, "Erreur de commentaire", "Échec de l'enregistrement du commentaire : " + e.getMessage());
                }
            }
        });
    }

    private void populateComments(VBox reviewsBox, List<Commentaire> commentaires) {
        reviewsBox.getChildren().clear();
        if (commentaires.isEmpty()) {
            reviewsBox.getChildren().add(new Label("Aucun avis pour le moment."));
            return;
        }

        for (Commentaire c : commentaires) {
            VBox commentBox = new VBox(5);
            commentBox.getStyleClass().add("comment-box");
            Label authorLabel = new Label(c.getAuteur() != null ? c.getAuteur() : "Anonyme");
            authorLabel.getStyleClass().add("comment-author");
            HBox stars = createStarRating(c.getNote() != null ? c.getNote() : 0);
            Text contentText = new Text(truncateText(c.getContenu() != null ? c.getContenu() : "", 100));
            contentText.getStyleClass().add("comment-text");
            commentBox.getChildren().addAll(authorLabel, stars, contentText);
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