package com.example.produit.controller;/*package com.example.produit.controller;

import com.example.auth.utils.SessionManager;
import com.example.cart.CartManager;
import com.example.produit.model.Categorie;
import com.example.produit.model.Commentaire;
import com.example.produit.model.Produit;
import com.example.produit.service.CategorieDAO;
import com.example.produit.service.CommentaireDAO;
import com.example.produit.service.FavoriteDAO;
import com.example.produit.service.ProduitDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

public class FavoriteCardController implements Initializable {

    @FXML private ScrollPane scrollPane;
    @FXML private VBox productContainer;
    @FXML private HBox fastBuyContainer;
    @FXML private Button fastBuyButton;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button refreshButton;
    @FXML private Label pageLabel;
    @FXML private ComboBox<String> categoryFilterComboBox;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private List<Produit> favoriteProducts = new ArrayList<>();
    private ObservableList<Categorie> categoryList = FXCollections.observableArrayList();
    private int currentPage = 1;
    private static final int PRODUCTS_PER_PAGE = 8;
    private static final int MAX_REVIEWS = 3;
    private static final String IMAGE_DIR = "Uploads/images/";

    // Grok API configuration
    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            configureScrollPane();
            loadCategories();
            initializeComboBox();
            loadFavoriteProducts();
            updatePage();

            fastBuyButton.setOnAction(e -> {
                try {
                    FastBuyController fastBuyController = new FastBuyController();
                    fastBuyController.showFastBuyDialog();
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Fast Buy Error", "Failed to open Fast Buy dialog: " + ex.getMessage());
                }
            });

            refreshButton.setOnAction(e -> {
                loadFavoriteProducts();
                updatePage();
            });

            categoryFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                loadFavoriteProducts();
                updatePage();
            });
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCategories() {
        try {
            categoryList.clear();
            categoryList.addAll(CategorieDAO.getAllCategories());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Category Load Error", "Failed to load categories: " + e.getMessage());
        }
    }

    private void initializeComboBox() {
        ObservableList<String> categoryNames = FXCollections.observableArrayList("All");
        categoryNames.addAll(categoryList.stream()
                .filter(c -> c.getNom() != null)
                .map(c -> c.getNom() + (c.getParent() != null ? " (Parent: " + c.getParent().getNom() + ")" : ""))
                .collect(Collectors.toList()));
        categoryFilterComboBox.setItems(categoryNames);
        categoryFilterComboBox.getSelectionModel().selectFirst();
    }

    private void loadFavoriteProducts() {
        UUID userId = sessionManager.getLoggedInUser() != null ? sessionManager.getLoggedInUser().getId() : null;
        if (userId == null) {
            favoriteProducts.clear();
            showAlert(Alert.AlertType.WARNING, "Login Required", "Please log in to view your favorites.");
            return;
        }

        try {
            List<Produit> allProducts = ProduitDAO.getAllProducts();
            if (allProducts == null) {
                allProducts = new ArrayList<>();
            }
            allProducts.forEach(product -> {
                try {
                    product.setCommentaires(CommentaireDAO.getCommentairesByProduit(product));
                } catch (Exception e) {
                    System.err.println("Failed to load comments for product " + product.getNom() + ": " + e.getMessage());
                }
            });

            String selectedCategory = categoryFilterComboBox.getValue();
            String categoryName = selectedCategory != null && !selectedCategory.equals("All") ?
                    selectedCategory.contains(" (Parent:") ?
                            selectedCategory.substring(0, selectedCategory.indexOf(" (Parent:")) :
                            selectedCategory : null;

            favoriteProducts = allProducts.stream()
                    .filter(product -> FavoriteDAO.isFavorite(userId, product.getId()))
                    .filter(product -> categoryName == null ||
                            (product.getCategory() != null && product.getCategory().getNom().equals(categoryName)))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load favorite products: " + e.getMessage());
            favoriteProducts.clear();
        }
    }

    private void configureScrollPane() {
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
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
        if (currentPage * PRODUCTS_PER_PAGE < favoriteProducts.size()) {
            currentPage++;
            updatePage();
        }
    }

    private void updatePage() {
        productContainer.getChildren().clear();
        if (favoriteProducts.isEmpty()) {
            Label emptyLabel = new Label("No favorite products found.");
            emptyLabel.getStyleClass().add("empty-label");
            productContainer.getChildren().add(emptyLabel);
            pageLabel.setText("Page 0");
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            return;
        }

        int startIndex = (currentPage - 1) * PRODUCTS_PER_PAGE;
        int endIndex = Math.min(startIndex + PRODUCTS_PER_PAGE, favoriteProducts.size());

        HBox row = createRow();
        for (int i = startIndex; i < endIndex; i++) {
            try {
                row.getChildren().add(createProductCard(favoriteProducts.get(i)));
                if ((i - startIndex + 1) % 4 == 0 || i == endIndex - 1) {
                    productContainer.getChildren().add(row);
                    row = createRow();
                }
            } catch (Exception e) {
                System.err.println("Error creating card for product " + favoriteProducts.get(i).getNom() + ": " + e.getMessage());
            }
        }

        pageLabel.setText("Page " + currentPage);
        prevButton.setDisable(currentPage == 1);
        nextButton.setDisable(endIndex >= favoriteProducts.size());
    }

    private HBox createRow() {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private VBox createProductCard(Produit product) {
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setPadding(new Insets(8));

        HBox topBar = new HBox(8);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        Label discountBadge = new Label("10% OFF");
        discountBadge.getStyleClass().add("discount-badge");
        Button favoriteButton = createFavoriteButton(product);
        topBar.getChildren().addAll(discountBadge, favoriteButton);
        HBox.setHgrow(favoriteButton, Priority.ALWAYS);

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

        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.getStyleClass().add("add-to-cart-button");
        addToCartButton.setOnAction(e -> {
            CartManager.addProduct(product);
            showAddedNotification(product.getNom());
        });

        card.setOnMouseClicked(e -> showProductDialog(product));

        card.getChildren().addAll(topBar, imageView, nameLabel, ratingBox, priceBox, categoryLabel, addToCartButton);
        return card;
    }

    private Button createFavoriteButton(Produit product) {
        Button favoriteButton = new Button();
        favoriteButton.getStyleClass().add("favorite-button");

        var imageUrl = getClass().getResource("/com/example/frontPages/icons/heart.png");
        if (imageUrl != null) {
            ImageView heartIcon = new ImageView(new Image(imageUrl.toExternalForm(), 28, 28, true, true));
            heartIcon.getStyleClass().add("heart-icon");
            favoriteButton.setGraphic(heartIcon);
        } else {
            System.err.println("Warning: heart.png not found at /com/example/frontPages/icons/heart.png");
            Label heartLabel = new Label("♥");
            heartLabel.getStyleClass().add("favorite-fallback");
            favoriteButton.setGraphic(heartLabel);
        }

        updateFavoriteButtonStyle(favoriteButton, FavoriteDAO.isFavorite(
                sessionManager.getLoggedInUser() != null ? sessionManager.getLoggedInUser().getId() : null,
                product.getId()));

        favoriteButton.setOnAction(e -> {
            UUID userId = sessionManager.getLoggedInUser() != null ? sessionManager.getLoggedInUser().getId() : null;
            if (userId == null) {
                showAlert(Alert.AlertType.WARNING, "Login Required", "Please log in to manage favorites.");
                return;
            }
            try {
                if (FavoriteDAO.isFavorite(userId, product.getId())) {
                    FavoriteDAO.removeFavorite(userId, product.getId());
                    updateFavoriteButtonStyle(favoriteButton, false);
                    showAlert(Alert.AlertType.INFORMATION, "Favorite Removed", product.getNom() + " removed from favorites.");
                    loadFavoriteProducts();
                    updatePage();
                } else {
                    FavoriteDAO.addFavorite(userId, product.getId());
                    updateFavoriteButtonStyle(favoriteButton, true);
                    showAlert(Alert.AlertType.INFORMATION, "Favorite Added", product.getNom() + " added to favorites.");
                }
            } catch (Exception ex) {
                showAlert(Alert.AlertType.ERROR, "Favorite Error", "Failed to update favorite: " + ex.getMessage());
            }
        });

        return favoriteButton;
    }

    private void updateFavoriteButtonStyle(Button favoriteButton, boolean isFavorited) {
        favoriteButton.getStyleClass().removeAll("favorite-empty", "favorite-filled");
        favoriteButton.getStyleClass().add(isFavorited ? "favorite-filled" : "favorite-empty");
    }

    private ImageView createProductImage(Produit product, double width, double height) {
        ImageView imageView = new ImageView();
        String imagePath = product.getImageName() != null && !product.getImageName().isEmpty()
                ? IMAGE_DIR + product.getImageName()
                : null;
        Image image = null;
        if (imagePath != null) {
            try {
                System.out.println("Loading image for product '" + product.getNom() + "' from: " + imagePath);
                image = new Image("file:" + imagePath, width, height, true, true, true);
                if (image.isError()) {
                    System.err.println("Failed to load image: " + imagePath + ", Error: " + image.getException());
                    image = null;
                }
            } catch (Exception e) {
                System.err.println("Error loading image for '" + product.getNom() + "': " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (image == null) {
            var defaultResource = getClass().getResource("/images/default.png");
            if (defaultResource != null) {
                System.out.println("Loading default image from: " + defaultResource.toExternalForm());
                image = new Image(defaultResource.toExternalForm(), width, height, true, true);
                if (image.isError()) {
                    System.err.println("Failed to load default image: " + image.getException());
                }
            } else {
                System.err.println("Warning: default.png not found at /images/default.png");
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
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/css/produitdialog.css").toExternalForm());

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        HBox content = new HBox(15);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_LEFT);

        VBox leftBox = new VBox(10);
        leftBox.setPrefWidth(320);
        leftBox.setAlignment(Pos.TOP_CENTER);

        ImageView imageView = createProductImage(product, 200, 200);
        imageView.getStyleClass().add("dialog-image");

        VBox detailsBox = new VBox(8);
        HBox titleBar = new HBox(8);
        Label nameLabel = new Label(product.getNom());
        nameLabel.getStyleClass().add("dialog-title");
        Button favoriteButton = createFavoriteButton(product);
        titleBar.getChildren().addAll(nameLabel, favoriteButton);

        HBox ratingBox = createStarRating(getAverageRating(product.getCommentaires()));
        Label priceLabel = new Label(String.format("$%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("dialog-price");
        Label quantityLabel = new Label("Stock: " + product.getQuantite());
        quantityLabel.getStyleClass().add("dialog-quantity");
        Label categoryLabel = new Label("Category: " + (product.getCategory() != null ? product.getCategory().getNom() : "None"));
        categoryLabel.getStyleClass().add("dialog-category");

        Button generateDescriptionButton = new Button("Generate Description");
        generateDescriptionButton.getStyleClass().add("action-button");
        generateDescriptionButton.setOnAction(e -> {
            String description = generateProductDescription(product.getNom(), product.getCategory() != null ? product.getCategory().getNom() : "Unknown");
            if (!description.isEmpty()) {
                product.setDescription(description);
                try {
                    ProduitDAO.updateProduct(product);
                    showAlert(Alert.AlertType.INFORMATION, "Description Updated", "New description generated and saved.");
                } catch (Exception ex) {
                    showAlert(Alert.AlertType.ERROR, "Update Error", "Failed to save description: " + ex.getMessage());
                }
            }
        });

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

        detailsBox.getChildren().addAll(titleBar, ratingBox, priceLabel, quantityLabel, categoryLabel, generateDescriptionButton, actionBox, addToCartButton);
        leftBox.getChildren().addAll(imageView, detailsBox);

        VBox rightBox = new VBox(10);
        rightBox.setPrefWidth(465);
        rightBox.setAlignment(Pos.TOP_LEFT);

        HBox descriptionHeader = new HBox(8);
        Label descriptionLabel = new Label("Description:");
        descriptionLabel.getStyleClass().add("dialog-description-label");

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
        List<Commentaire> commentaires = product.getCommentaires();
        populateComments(reviewsBox, commentaires != null ? commentaires : new ArrayList<>());
        reviewsScroll.setContent(reviewsBox);

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

        rightBox.getChildren().addAll(descriptionHeader, descriptionText, reviewsScroll, addReviewBox);
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
                    e.printStackTrace();
                }
            }
        });
    }

    private String generateProductDescription(String productName, String category) {
        try {
            String prompt = "Generate an attractive description in English, with a maximum of 200 characters, for an agricultural product named '" + productName + "' in the category '" + category + "' intended for a farmers' market.";
            String jsonBody = """
                {
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "model": "llama3-8b-8192",
                  "temperature": 1,
                  "max_tokens": 100
                }
                """.formatted(prompt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + GROQ_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String description = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                return description.length() > 200 ? description.substring(0, 200) : description;
            } else {
                showAlert(Alert.AlertType.ERROR, "API Error", "Failed to generate description: HTTP " + response.statusCode());
                System.err.println("API response: " + response.body());
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "API Error", "Failed to generate description: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
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
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/css/produitdialog.css").toExternalForm());
        alert.showAndWait();
    }

    private void showAddedNotification(String productName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Product Added");
        alert.setHeaderText(null);
        alert.setContentText(productName + " has been added to the cart!");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/css/produitdialog.css").toExternalForm());
        alert.showAndWait();
    }
}*/