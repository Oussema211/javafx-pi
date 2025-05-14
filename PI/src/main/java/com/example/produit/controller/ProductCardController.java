package com.example.produit.controller;

import com.example.auth.model.User;
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
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProductCardController implements Initializable {

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
    private List<Produit> allProducts = new ArrayList<>();
    private ObservableList<Categorie> categoryList = FXCollections.observableArrayList();
    private int currentPage = 1;
    private static final int PRODUCTS_PER_PAGE = 8;
    private static final int MAX_REVIEWS = 3;
    private static final String IMAGE_DIR = "Uploads/images/";
    private static final String DEFAULT_IMAGE_PATH = "/images/default.png";
    private static final String DEFAULT_ICON_PATH = "/images/default_icon.png";

    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Map<String, String> descriptionCache = new HashMap<>();
    private static final int MAX_API_RETRIES = 3;
    private static final long API_RETRY_DELAY_MS = 1000;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            configureScrollPane();
            loadCategories();
            initializeComboBox();
            loadProducts();
            updatePage();

            if (fastBuyButton != null) {
                fastBuyButton.setOnAction(e -> handleFastBuy());
            } else {
                logWarning("fastBuyButton is null");
            }

            if (refreshButton != null) {
                refreshButton.setOnAction(e -> {
                    currentPage = 1;
                    loadProducts();
                    updatePage();
                });
            } else {
                logWarning("refreshButton is null");
            }

            if (categoryFilterComboBox != null) {
                categoryFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                    currentPage = 1;
                    loadProducts();
                    updatePage();
                });
            } else {
                logWarning("categoryFilterComboBox is null");
            }
        } catch (Exception e) {
            logError("Initialization error", e);
            showAlert(Alert.AlertType.ERROR, "Initialization Error", "Failed to initialize. Please try again.");
        }
    }

    private void handleFastBuy() {
        try {
            FastBuyController fastBuyController = new FastBuyController();
            fastBuyController.showFastBuyDialog();
        } catch (Exception ex) {
            logError("Error opening Fast Buy dialog", ex);
            showAlert(Alert.AlertType.ERROR, "Fast Buy Error", "Failed to open Fast Buy dialog. Please try again.");
        }
    }

    private void loadCategories() {
        try {
            categoryList.clear();
            List<Categorie> categories = CategorieDAO.getAllCategories();
            if (categories != null) {
                categoryList.addAll(categories);
            }
        } catch (Exception e) {
            logError("Error loading categories", e);
            showAlert(Alert.AlertType.ERROR, "Category Load Error", "Failed to load categories.");
            categoryList.clear();
        }
    }

    private void initializeComboBox() {
        if (categoryFilterComboBox == null) {
            logWarning("Cannot initialize null categoryFilterComboBox");
            return;
        }
        ObservableList<String> categoryNames = FXCollections.observableArrayList("All");
        categoryNames.addAll(categoryList.stream()
                .filter(c -> c != null && c.getNom() != null)
                .map(c -> c.getNom() + (c.getParent() != null && c.getParent().getNom() != null ? " (Parent: " + c.getParent().getNom() + ")" : ""))
                .collect(Collectors.toList()));
        categoryFilterComboBox.setItems(categoryNames);
        categoryFilterComboBox.getSelectionModel().selectFirst();
    }

    private void loadProducts() {
        try {
            allProducts.clear();
            List<Produit> products = ProduitDAO.getAllProducts();
            if (products != null) {
                allProducts.addAll(products);
                allProducts.forEach(product -> {
                    try {
                        if (product != null) {
                            product.setCommentaires(CommentaireDAO.getCommentairesByProduit(product));
                        }
                    } catch (Exception e) {
                        logError("Failed to load comments for product " + (product != null && product.getNom() != null ? product.getNom() : "Unknown"), e);
                    }
                });
            }

            String selectedCategory = categoryFilterComboBox != null ? categoryFilterComboBox.getValue() : null;
            String categoryName = selectedCategory != null && !selectedCategory.equals("All") ?
                    selectedCategory.contains(" (Parent:") ?
                            selectedCategory.substring(0, selectedCategory.indexOf(" (Parent:")) : selectedCategory : null;

            if (categoryName != null) {
                allProducts = allProducts.stream()
                        .filter(product -> product != null && product.getCategory() != null &&
                                categoryName.equals(product.getCategory().getNom()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            logError("Error loading products", e);
            showAlert(Alert.AlertType.ERROR, "Load Error", "Failed to load products.");
            allProducts.clear();
        }
    }

    private void configureScrollPane() {
        if (scrollPane != null) {
            scrollPane.setFitToWidth(true);
            scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        } else {
            logWarning("scrollPane is null");
        }
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
        if ((currentPage * PRODUCTS_PER_PAGE) < allProducts.size()) {
            currentPage++;
            updatePage();
        }
    }

    private void updatePage() {
        if (productContainer == null) {
            logWarning("productContainer is null");
            return;
        }
        productContainer.getChildren().clear();
        if (allProducts.isEmpty()) {
            Label emptyLabel = new Label("No products found.");
            emptyLabel.getStyleClass().add("empty-label");
            productContainer.getChildren().add(emptyLabel);
            if (pageLabel != null) {
                pageLabel.setText("Page 0");
            }
            if (prevButton != null) {
                prevButton.setDisable(true);
            }
            if (nextButton != null) {
                nextButton.setDisable(true);
            }
            return;
        }

        int startIndex = (currentPage - 1) * PRODUCTS_PER_PAGE;
        int endIndex = Math.min(startIndex + PRODUCTS_PER_PAGE, allProducts.size());
        HBox row = createRow();
        for (int i = startIndex; i < endIndex; i++) {
            try {
                if (allProducts.get(i) != null) {
                    row.getChildren().add(createProductCard(allProducts.get(i)));
                    if ((i - startIndex + 1) % 4 == 0 || i == endIndex - 1) {
                        productContainer.getChildren().add(row);
                        row = createRow();
                    }
                }
            } catch (Exception e) {
                logError("Error creating card for product " + (allProducts.get(i) != null && allProducts.get(i).getNom() != null ? allProducts.get(i).getNom() : "Unknown"), e);
            }
        }

        if (pageLabel != null) {
            pageLabel.setText("Page " + currentPage);
        }
        if (prevButton != null) {
            prevButton.setDisable(currentPage == 1);
        }
        if (nextButton != null) {
            nextButton.setDisable(endIndex >= allProducts.size());
        }
    }

    private HBox createRow() {
        HBox row = new HBox(10);
        row.setPadding(new Insets(8));
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private VBox createProductCard(Produit product) {
        if (product == null) {
            logWarning("Attempted to create product card for null product");
            return new VBox();
        }
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card");
        card.setPadding(new Insets(16));

        HBox topBar = new HBox(10);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_LEFT);
        Label discountBadge = new Label("10% OFF");
        discountBadge.getStyleClass().add("discount-badge");
        Button favoriteButton = createFavoriteButton(product);
        topBar.getChildren().addAll(discountBadge, favoriteButton);
        HBox.setHgrow(favoriteButton, Priority.ALWAYS);

        ImageView imageView = createProductImage(product, 140, 140);
        imageView.getStyleClass().add("product-image");

        VBox detailsBox = new VBox(8);
        Label nameLabel = new Label(product.getNom() != null ? product.getNom() : "Unknown");
        nameLabel.getStyleClass().add("product-name");
        HBox ratingBox = createStarRating(getAverageRating(product.getCommentaires()));
        HBox priceBox = new HBox(8);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceLabel = new Label(String.format("$%.2f", product.getPrixUnitaire()));
        priceLabel.getStyleClass().add("product-price");
        Label quantityLabel = new Label("Stock: " + product.getQuantite());
        quantityLabel.getStyleClass().add("product-quantity");
        Label categoryLabel = new Label(product.getCategory() != null ? product.getCategory().getNom() : "None");
        categoryLabel.getStyleClass().add("product-category");
        priceBox.getChildren().addAll(priceLabel, quantityLabel);
        detailsBox.getChildren().addAll(nameLabel, ratingBox, priceBox, categoryLabel);

        Button addToCartButton = new Button("Add to Cart");
        addToCartButton.getStyleClass().add("add-to-cart-button");
        addToCartButton.setOnAction(e -> {
            if (product.getQuantite() > 0) {
                CartManager.addProduct(product, 1);
                showAddedNotification(product.getNom() != null ? product.getNom() : "Product");
            } else {
                showAlert(Alert.AlertType.WARNING, "Out of Stock", (product.getNom() != null ? product.getNom() : "Product") + " is out of stock.");
            }
        });

        card.setOnMouseClicked(e -> showProductDialog(product));

        card.getChildren().addAll(topBar, imageView, detailsBox, addToCartButton);
        return card;
    }

    private Button createFavoriteButton(Produit product) {
        if (product == null || product.getId() == null) {
            logWarning("Attempted to create favorite button for null product or product ID");
            return new Button();
        }
        Button favoriteButton = new Button();
        favoriteButton.getStyleClass().add("favorite-button");

        var imageUrl = getClass().getResource("/com/example/frontPages/icons/heart.png");
        if (imageUrl == null) {
            imageUrl = getClass().getResource(DEFAULT_ICON_PATH);
            logWarning("heart.png not found, using default_icon.png");
        }
        if (imageUrl != null) {
            ImageView heartIcon = new ImageView(new Image(imageUrl.toExternalForm(), 28, 28, true, true));
            heartIcon.getStyleClass().add("heart-icon");
            favoriteButton.setGraphic(heartIcon);
        } else {
            logWarning("Default icon not found at " + DEFAULT_ICON_PATH);
            Label heartLabel = new Label("♥");
            heartLabel.getStyleClass().add("favorite-fallback");
            favoriteButton.setGraphic(heartLabel);
        }

        UUID userId = sessionManager.getLoggedInUser() != null ? sessionManager.getLoggedInUser().getId() : null;
        boolean isFavorited = userId != null && FavoriteDAO.isFavorite(userId, product.getId());
        updateFavoriteButtonStyle(favoriteButton, isFavorited);

        favoriteButton.setOnAction(e -> {
            if (userId == null) {
                showAlert(Alert.AlertType.WARNING, "Login Required", "Please log in to manage favorites.");
                return;
            }
            try {
                if (FavoriteDAO.isFavorite(userId, product.getId())) {
                    FavoriteDAO.removeFavorite(userId, product.getId());
                    updateFavoriteButtonStyle(favoriteButton, false);
                    showAlert(Alert.AlertType.INFORMATION, "Favorite Removed", (product.getNom() != null ? product.getNom() : "Product") + " removed from favorites.");
                } else {
                    FavoriteDAO.addFavorite(userId, product.getId());
                    updateFavoriteButtonStyle(favoriteButton, true);
                    showAlert(Alert.AlertType.INFORMATION, "Favorite Added", (product.getNom() != null ? product.getNom() : "Product") + " added to favorites.");
                }
            } catch (Exception ex) {
                logError("Error updating favorite for product " + product.getId(), ex);
                showAlert(Alert.AlertType.ERROR, "Favorite Error", "Failed to update favorite. Please try again.");
            }
        });

        return favoriteButton;
    }

    private void updateFavoriteButtonStyle(Button favoriteButton, boolean isFavorited) {
        if (favoriteButton != null) {
            favoriteButton.getStyleClass().removeAll("favorite-empty", "favorite-filled");
            favoriteButton.getStyleClass().add(isFavorited ? "favorite-filled" : "favorite-empty");
        }
    }

    private ImageView createProductImage(Produit product, double width, double height) {
        ImageView imageView = new ImageView();
        String imagePath = product != null && product.getImageName() != null && !product.getImageName().isEmpty() &&
                !product.getImageName().startsWith("http") ? IMAGE_DIR + product.getImageName() : (product != null ? product.getImageName() : null);
        Image image = null;

        if (imagePath != null) {
            try {
                image = new Image(imagePath.startsWith("http") ? imagePath : "file:" + imagePath,
                        width, height, true, true, true);
                if (image.isError()) {
                    logError("Failed to load image: " + imagePath, image.getException());
                    image = null;
                }
            } catch (Exception e) {
                logError("Error loading image for product " + (product != null && product.getNom() != null ? product.getNom() : "Unknown"), e);
            }
        }

        if (image == null || image.isError()) {
            var defaultResource = getClass().getResource(DEFAULT_IMAGE_PATH);
            if (defaultResource != null) {
                image = new Image(defaultResource.toExternalForm(), width, height, true, true);
                if (image.isError()) {
                    logError("Failed to load default image", image.getException());
                }
            } else {
                logWarning("Default image not found at " + DEFAULT_IMAGE_PATH);
            }
        }
        imageView.setImage(image != null && !image.isError() ? image : null);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        return imageView;
    }

    private void showProductDialog(Produit product) {
        if (product == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Cannot display null product.");
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(product.getNom() != null ? product.getNom() : "Product");
        dialog.getDialogPane().getStyleClass().add("product-dialog");
        dialog.getDialogPane().setPrefSize(800, 600);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/com/example/css/produitdialog.css").toExternalForm());

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        HBox content = createDialogContent(product);
        dialog.getDialogPane().setContent(content);

        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submitButtonType);
        TextArea commentTextArea = (TextArea) content.lookup(".input-textarea");
        ComboBox<Integer> ratingCombo = (ComboBox<Integer>) content.lookup(".rating-combo");
        submitButton.setDisable(true);
        if (commentTextArea != null && ratingCombo != null) {
            commentTextArea.textProperty().addListener((obs, old, newValue) ->
                    submitButton.setDisable(newValue.trim().isEmpty() || ratingCombo.getValue() == null));
            ratingCombo.valueProperty().addListener((obs, old, newValue) ->
                    submitButton.setDisable(commentTextArea.getText().trim().isEmpty() || newValue == null));
        }

        dialog.showAndWait().ifPresent(result -> {
            if (result == submitButtonType && commentTextArea != null && ratingCombo != null) {
                try {
                    User currentUser = sessionManager.getLoggedInUser();
                    if (currentUser == null) {
                        showAlert(Alert.AlertType.WARNING, "Login Required", "Please log in to add a review.");
                        return;
                    }
                    String contentText = commentTextArea.getText().trim();
                    Integer rating = ratingCombo.getValue();
                    if (contentText.isEmpty() || rating == null) {
                        showAlert(Alert.AlertType.WARNING, "Invalid Input", "Review content and rating are required.");
                        return;
                    }
                    Commentaire commentaire = new Commentaire();
                    commentaire.setId(UUID.randomUUID());
                    commentaire.setAuteur(currentUser.getNom() != null ? currentUser.getNom() : "Anonymous");
                    commentaire.setContenu(contentText);
                    commentaire.setNote(rating.floatValue());
                    commentaire.setProduit(product);
                    commentaire.setDateCreation(ZonedDateTime.now(ZoneId.of("Europe/Paris")).toLocalDateTime());
                    CommentaireDAO.saveCommentaire(commentaire);
                    product.addCommentaire(commentaire);
                    VBox reviewsBox = (VBox) content.lookup(".reviews-box");
                    if (reviewsBox != null) {
                        reviewsBox.getChildren().clear();
                        populateComments(reviewsBox, product.getCommentaires() != null ? product.getCommentaires() : new ArrayList<>());
                    }
                    showAlert(Alert.AlertType.INFORMATION, "Review Added", "Your review has been saved.");
                } catch (Exception e) {
                    logError("Error saving comment for product " + product.getId(), e);
                    showAlert(Alert.AlertType.ERROR, "Comment Error", "Failed to save comment. Please try again.");
                }
            }
        });
    }

    private HBox createDialogContent(Produit product) {
        HBox content = new HBox(15);
        content.setPadding(new Insets(15));
        content.setAlignment(Pos.TOP_LEFT);

        VBox leftBox = new VBox(10);
        leftBox.setPrefWidth(320);
        leftBox.setAlignment(Pos.TOP_CENTER);

        ImageView imageView = createProductImage(product, 200, 200);
        imageView.getStyleClass().add("dialog-image");

        VBox detailsBox = createDetailsBox(product);
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
        List<Commentaire> commentaires = product.getCommentaires() != null ? product.getCommentaires() : new ArrayList<>();
        populateComments(reviewsBox, commentaires);
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
        return content;
    }

    private VBox createDetailsBox(Produit product) {
        VBox detailsBox = new VBox(8);
        HBox titleBar = new HBox(8);
        Label nameLabel = new Label(product.getNom() != null ? product.getNom() : "Unknown");
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
            String description = generateProductDescription(product.getId(), product.getNom() != null ? product.getNom() : "Unknown",
                    product.getCategory() != null ? product.getCategory().getNom() : "Unknown");
            if (!description.isEmpty()) {
                product.setDescription(description);
                try {
                    ProduitDAO.updateProduct(product);
                    showAlert(Alert.AlertType.INFORMATION, "Description Updated", "New description generated and saved.");
                } catch (Exception ex) {
                    logError("Error saving description for product " + product.getId(), ex);
                    showAlert(Alert.AlertType.ERROR, "Update Error", "Failed to save description. Please try again.");
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
            int qty = qtySpinner.getValue();
            if (product.getQuantite() >= qty) {
                CartManager.addProduct(product, qty);
                showAddedNotification(product.getNom() != null ? product.getNom() : "Product" + " (x" + qty + ")");
            } else {
                showAlert(Alert.AlertType.WARNING, "Out of Stock", "Only " + product.getQuantite() + " " + (product.getNom() != null ? product.getNom() : "Product") + " available.");
            }
        });

        detailsBox.getChildren().addAll(titleBar, ratingBox, priceLabel, quantityLabel, categoryLabel, generateDescriptionButton, actionBox, addToCartButton);
        return detailsBox;
    }

    private String generateProductDescription(String productId, String productName, String category) {
        String cacheKey = productId + "_" + productName + "_" + category;
        if (descriptionCache.containsKey(cacheKey)) {
            logInfo("Returning cached description for product " + productId);
            return descriptionCache.get(cacheKey);
        }

        for (int attempt = 1; attempt <= MAX_API_RETRIES; attempt++) {
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
                    description = description.length() > 200 ? description.substring(0, 200) : description;
                    descriptionCache.put(cacheKey, description);
                    logInfo("Generated description for product " + productId);
                    return description;
                } else {
                    logError("API error generating description for product " + productId + ": HTTP " + response.statusCode(), null);
                    if (attempt == MAX_API_RETRIES) {
                        showAlert(Alert.AlertType.ERROR, "API Error", "Failed to generate description: HTTP " + response.statusCode());
                    }
                }
            } catch (Exception e) {
                logError("Error generating description for product " + productId + " (attempt " + attempt + ")", e);
                if (attempt == MAX_API_RETRIES) {
                    showAlert(Alert.AlertType.ERROR, "API Error", "Failed to generate description. Please try again.");
                }
            }
            try {
                Thread.sleep(API_RETRY_DELAY_MS * attempt);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logError("Interrupted during API retry for product " + productId, ie);
                return "";
            }
        }
        return "";
    }

    private void populateComments(VBox reviewsBox, List<Commentaire> commentaires) {
        if (reviewsBox == null) {
            logWarning("reviewsBox is null");
            return;
        }
        reviewsBox.getChildren().clear();
        if (commentaires.isEmpty()) {
            reviewsBox.getChildren().add(new Label("No reviews yet."));
            return;
        }

        List<Commentaire> sortedComments = commentaires.stream()
                .filter(c -> c != null && c.getContenu() != null)
                .sorted((c1, c2) -> c2.getDateCreation().compareTo(c1.getDateCreation()))
                .limit(MAX_REVIEWS)
                .collect(Collectors.toList());

        for (Commentaire c : sortedComments) {
            VBox commentBox = new VBox(5);
            commentBox.getStyleClass().add("comment-box");
            Label authorLabel = new Label(c.getAuteur() != null ? c.getAuteur() : "Anonymous");
            authorLabel.getStyleClass().add("comment-author");
            HBox stars = createStarRating(c.getNote() != null ? c.getNote() : 0);
            Text contentText = new Text(truncateText(c.getContenu(), 100));
            contentText.getStyleClass().add("comment-text");
            commentBox.getChildren().addAll(authorLabel, stars, contentText);
            reviewsBox.getChildren().add(commentBox);
        }
    }

    private double getAverageRating(List<Commentaire> commentaires) {
        if (commentaires == null || commentaires.isEmpty()) {
            return 0;
        }
        return commentaires.stream()
                .filter(c -> c != null && c.getNote() != null)
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
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().getStyleClass().add("dialog");
        String css = getClass().getResource("/com/example/css/produitdialog.css") != null ?
                getClass().getResource("/com/example/css/produitdialog.css").toExternalForm() : "";
        if (!css.isEmpty()) {
            alert.getDialogPane().getStylesheets().add(css);
        }
        alert.showAndWait();
    }

    private void showAddedNotification(String productName) {
        showAlert(Alert.AlertType.INFORMATION, "Product Added", productName + " has been added to the cart!");
    }

    private void logInfo(String message) {
        System.out.println(message + " at " + ZonedDateTime.now(ZoneId.of("Europe/Paris")));
    }

    private void logWarning(String message) {
        System.err.println("WARNING: " + message + " at " + ZonedDateTime.now(ZoneId.of("Europe/Paris")));
    }

    private void logError(String message, Throwable e) {
        System.err.println("ERROR: " + message + " at " + ZonedDateTime.now(ZoneId.of("Europe/Paris")) + (e != null ? ": " + e.getMessage() : ""));
        if (e != null) {
            e.printStackTrace();
        }
    }
}