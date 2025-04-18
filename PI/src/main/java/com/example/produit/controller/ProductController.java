package com.example.produit.controller;

import com.example.auth.model.User;
import com.example.auth.utils.SessionManager;
import com.example.produit.model.Categorie;
import com.example.produit.model.Produit;
import com.example.produit.service.CategorieDAO;
import com.example.produit.service.ProduitDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class ProductController {

    @FXML private TableView<Produit> productTableView;
    @FXML private TableColumn<Produit, String> productColumn;
    @FXML private TableColumn<Produit, String> descriptionColumn;
    @FXML private TableColumn<Produit, String> categoryColumn;
    @FXML private TableColumn<Produit, Number> priceColumn;
    @FXML private TableColumn<Produit, Number> quantityColumn;
    @FXML private TableColumn<Produit, LocalDateTime> dateCreationColumn;
    @FXML private TableColumn<Produit, Void> actionsColumn;
    @FXML private TableColumn<Produit, String> imagePreviewColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button addProductButton;
    @FXML private Button researchButton;
    @FXML private Label resultsCountLabel;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private ObservableList<Produit> productList = FXCollections.observableArrayList();
    private ObservableList<Categorie> categoryList = FXCollections.observableArrayList();
    private FilteredList<Produit> filteredList;

    @FXML
    public void initialize() {
        configureTableColumns();
        initializeComboBoxes();
        loadInitialData();
        setupContextMenu();
        setupActionsColumn();
        setupResultsCountListener();
        setupSearchListeners();
    }

    private void configureTableColumns() {
        productColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryColumn.setCellValueFactory(cellData -> {
            Produit produit = cellData.getValue();
            Categorie category = produit.getCategory();
            return new SimpleStringProperty(category != null ? category.getNom() : "No Category");
        });
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        imagePreviewColumn.setCellFactory(param -> new TableCell<Produit, String>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        Image image = new Image(new File(imagePath).toURI().toString());
                        imageView.setImage(image);
                        setGraphic(imageView);
                    } catch (Exception ex) {
                        setGraphic(null);
                    }
                }
            }
        });
        imagePreviewColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getImageName()));
    }

    private void initializeComboBoxes() {
        ObservableList<String> categoryNames = FXCollections.observableArrayList("All");
        categoryNames.addAll(categoryList.stream().map(Categorie::getNom).toList());
        categoryComboBox.setItems(categoryNames);
        categoryComboBox.getSelectionModel().selectFirst();
    }

    private void loadInitialData() {
        categoryList.addAll(CategorieDAO.getAllCategories());
        initializeComboBoxes();
        productList.addAll(ProduitDAO.getAllProducts());
        filteredList = new FilteredList<>(productList, p -> true);
        productTableView.setItems(filteredList);
        updateResultsCount();
    }

    private void setupSearchListeners() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> handleResearch());
        categoryComboBox.valueProperty().addListener((observable, oldValue, newValue) -> handleResearch());
    }

    @FXML
    private void handleResearch() {
        filteredList.setPredicate(product -> {
            boolean match = true;

            String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
            String category = categoryComboBox.getValue();

            // Search text filter (name or description)
            if (!searchText.isEmpty()) {
                match &= product.getNom().toLowerCase().contains(searchText) ||
                        product.getDescription().toLowerCase().contains(searchText);
            }

            // Category filter
            if (category != null && !category.equals("All")) {
                match &= product.getCategory() != null && product.getCategory().getNom().equals(category);
            }

            return match;
        });

        productTableView.refresh();
        updateResultsCount();
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<Produit, Void>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox hbox = new HBox(10);

            {
                Image editImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/edit.png")));
                Image deleteImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/delete.png")));
                editButton.setGraphic(new ImageView(editImage));
                deleteButton.setGraphic(new ImageView(deleteImage));
                editButton.getStyleClass().add("action-button");
                deleteButton.getStyleClass().add("action-button");

                editButton.setOnAction(event -> {
                    Produit product = getTableView().getItems().get(getIndex());
                    handleEditProduct(product);
                });

                deleteButton.setOnAction(event -> {
                    Produit product = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(product);
                });

                hbox.getChildren().addAll(editButton, deleteButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void setupResultsCountListener() {
        filteredList.addListener((javafx.collections.ListChangeListener<Produit>) c -> updateResultsCount());
        updateResultsCount();
    }

    private void updateResultsCount() {
        int count = filteredList.size();
        resultsCountLabel.setText("SHOWING " + count + " PRODUCTS");
    }

    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }

    private void handleEditProduct(Produit product) {
        if (product != null) {
            showProductDialog(product);
        }
    }

    private void handleDeleteProduct(Produit product) {
        if (product != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Delete Product");
            confirmation.setHeaderText("Delete " + product.getNom());
            confirmation.setContentText("Are you sure you want to delete this product?");
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                ProduitDAO.deleteProduct(product.getId());
                productList.remove(product);
                handleResearch(); // Refresh table after deletion
            }
        }
    }

    private void showProductDialog(Produit product) {
        Dialog<Produit> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "New Product" : "Edit Product");

        DialogPane dialogPane = dialog.getDialogPane();
        URL cssUrl = getClass().getResource("/com/example/css/produitdialog.css");
        if (cssUrl != null) {
            dialogPane.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Warning: CSS file /com/example/css/produitdialog.css not found. Using default styles.");
            dialogPane.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-padding: 10;");
        }
        dialogPane.getStyleClass().add("dialog-pane");

        // Form fields
        TextField nameField = new TextField();
        TextArea descriptionField = new TextArea();
        ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(
                categoryList.stream().map(Categorie::getNom).toList()
        ));
        TextField priceField = new TextField();
        TextField quantityField = new TextField();
        TextField imagePathField = new TextField();
        imagePathField.setEditable(false);
        Button chooseImageButton = new Button("Choose Image");
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(100);
        imagePreview.setFitHeight(100);
        imagePreview.setPreserveRatio(true);

        // Error labels (small font, minimal space)
        Label nameError = new Label();
        Label descriptionError = new Label();
        Label categoryError = new Label();
        Label priceError = new Label();
        Label quantityError = new Label();
        nameError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        descriptionError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        categoryError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        priceError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        quantityError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

        // File chooser setup
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        chooseImageButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                imagePathField.setText(file.getAbsolutePath());
                try {
                    Image image = new Image(file.toURI().toString());
                    imagePreview.setImage(image);
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load image.");
                    alert.showAndWait();
                }
            }
        });

        // Populate fields for edit
        if (product != null) {
            nameField.setText(product.getNom());
            descriptionField.setText(product.getDescription());
            categoryCombo.getSelectionModel().select(product.getCategory() != null ? product.getCategory().getNom() : null);
            priceField.setText(String.valueOf(product.getPrixUnitaire()));
            quantityField.setText(String.valueOf(product.getQuantite()));
            imagePathField.setText(product.getImageName() != null ? product.getImageName() : "");
            if (product.getImageName() != null && !product.getImageName().isEmpty()) {
                try {
                    Image image = new Image(new File(product.getImageName()).toURI().toString());
                    imagePreview.setImage(image);
                } catch (Exception ex) {
                    imagePreview.setImage(null);
                }
            }
        }

        // Validation patterns and rules
        Pattern namePattern = Pattern.compile("^[a-zA-Z0-9\\s-]{3,50}$");
        Pattern pricePattern = Pattern.compile("^\\d*\\.?\\d+$");
        Pattern quantityPattern = Pattern.compile("^[0-9]+$");

        // Real-time validation
        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                nameError.setText("Name cannot be empty");
                nameField.setStyle("-fx-border-color: red;");
            } else if (!namePattern.matcher(newVal).matches()) {
                nameError.setText("3-50 chars, letters, numbers, spaces, hyphens");
                nameField.setStyle("-fx-border-color: red;");
            } else {
                nameError.setText("");
                nameField.setStyle("");
            }
        });

        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                descriptionError.setText("Description cannot be empty");
                descriptionField.setStyle("-fx-border-color: red;");
            } else if (newVal.length() > 200) {
                descriptionError.setText("Max 200 characters");
                descriptionField.setStyle("-fx-border-color: red;");
            } else {
                descriptionError.setText("");
                descriptionField.setStyle("");
            }
        });

        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                categoryError.setText("Select a category");
                categoryCombo.setStyle("-fx-border-color: red;");
            } else {
                categoryError.setText("");
                categoryCombo.setStyle("");
            }
        });

        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                priceError.setText("Price cannot be empty");
                priceField.setStyle("-fx-border-color: red;");
            } else if (!pricePattern.matcher(newVal).matches()) {
                priceError.setText("Positive number required");
                priceField.setStyle("-fx-border-color: red;");
            } else {
                try {
                    float price = Float.parseFloat(newVal);
                    if (price <= 0) {
                        priceError.setText("Price must be > 0");
                        priceField.setStyle("-fx-border-color: red;");
                    } else if (price > 1000000) {
                        priceError.setText("Price max 1,000,000");
                        priceField.setStyle("-fx-border-color: red;");
                    } else {
                        priceError.setText("");
                        priceField.setStyle("");
                    }
                } catch (NumberFormatException e) {
                    priceError.setText("Invalid price format");
                    priceField.setStyle("-fx-border-color: red;");
                }
            }
        });

        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                quantityError.setText("Quantity cannot be empty");
                quantityField.setStyle("-fx-border-color: red;");
            } else if (!quantityPattern.matcher(newVal).matches()) {
                quantityError.setText("Positive integer required");
                quantityField.setStyle("-fx-border-color: red;");
            } else {
                try {
                    int quantity = Integer.parseInt(newVal);
                    if (quantity <= 0) {
                        quantityError.setText("Quantity must be > 0");
                        quantityField.setStyle("-fx-border-color: red;");
                    } else if (quantity > 10000) {
                        quantityError.setText("Quantity max 10,000");
                        quantityField.setStyle("-fx-border-color: red;");
                    } else {
                        quantityError.setText("");
                        quantityField.setStyle("");
                    }
                } catch (NumberFormatException e) {
                    quantityError.setText("Invalid quantity format");
                    quantityField.setStyle("-fx-border-color: red;");
                }
            }
        });

        // Grid layout with error labels in same row
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.addRow(0, new Label("Name:"), nameField);
        grid.add(nameError, 1, 1);
        grid.addRow(2, new Label("Description:"), descriptionField);
        grid.add(descriptionError, 1, 3);
        grid.addRow(4, new Label("Category:"), categoryCombo);
        grid.add(categoryError, 1, 5);
        grid.addRow(6, new Label("Price:"), priceField);
        grid.add(priceError, 1, 7);
        grid.addRow(8, new Label("Quantity:"), quantityField);
        grid.add(quantityError, 1, 9);
        grid.addRow(10, new Label("Image:"), imagePathField);
        grid.addRow(11, new Label(""), chooseImageButton);
        grid.addRow(12, new Label("Preview:"), imagePreview);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Disable save button until all validations pass
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Validation check for save button
        Runnable validateForm = () -> {
            boolean isValid =
                    !nameField.getText().trim().isEmpty() &&
                            namePattern.matcher(nameField.getText()).matches() &&
                            !descriptionField.getText().trim().isEmpty() &&
                            descriptionField.getText().length() <= 200 &&
                            categoryCombo.getValue() != null &&
                            !priceField.getText().trim().isEmpty() &&
                            pricePattern.matcher(priceField.getText()).matches() &&
                            Float.parseFloat(priceField.getText()) > 0 &&
                            Float.parseFloat(priceField.getText()) <= 1000000 &&
                            !quantityField.getText().trim().isEmpty() &&
                            quantityPattern.matcher(quantityField.getText()).matches() &&
                            Integer.parseInt(quantityField.getText()) > 0 &&
                            Integer.parseInt(quantityField.getText()) <= 10000;
            saveButton.setDisable(!isValid);
        };

        nameField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        descriptionField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        categoryCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        priceField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        quantityField.textProperty().addListener((obs, old, newVal) -> validateForm.run());

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                try {
                    Produit newProduct = product != null ? product : new Produit();
                    newProduct.setNom(nameField.getText().trim());
                    newProduct.setDescription(descriptionField.getText().trim());

                    String selectedCategoryName = categoryCombo.getValue();
                    Categorie selectedCategory = categoryList.stream()
                            .filter(c -> c.getNom().equals(selectedCategoryName))
                            .findFirst()
                            .orElse(null);

                    newProduct.setCategory(selectedCategory);
                    newProduct.setPrixUnitaire(Float.parseFloat(priceField.getText()));
                    newProduct.setQuantite(Integer.parseInt(quantityField.getText()));
                    newProduct.setImageName(imagePathField.getText());

                    User currentUser = sessionManager.getLoggedInUser();
                    if (product == null) {
                        newProduct.setId(UUID.randomUUID());
                        newProduct.setDateCreation(LocalDateTime.now());
                        newProduct.setUserId(currentUser != null ? currentUser.getId() : null);
                        ProduitDAO.saveProduct(newProduct);
                        productList.add(newProduct);
                    } else {
                        ProduitDAO.updateProduct(newProduct);
                        productTableView.refresh();
                    }
                    handleResearch(); // Refresh table after save
                    return newProduct;
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid input format.");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");

        editItem.setOnAction(event -> {
            Produit selected = productTableView.getSelectionModel().getSelectedItem();
            handleEditProduct(selected);
        });

        deleteItem.setOnAction(event -> {
            Produit selected = productTableView.getSelectionModel().getSelectedItem();
            handleDeleteProduct(selected);
        });

        contextMenu.getItems().addAll(editItem, deleteItem);
        productTableView.setContextMenu(contextMenu);
    }
}