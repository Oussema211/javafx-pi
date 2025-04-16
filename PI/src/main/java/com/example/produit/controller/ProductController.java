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
    @FXML private ComboBox<String> productTypeComboBox;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button addProductButton;
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
        imagePreviewColumn.setCellValueFactory(new PropertyValueFactory<>("imageName"));
    }

    private void initializeComboBoxes() {
        productTypeComboBox.getItems().addAll("All", "Physical", "Digital");
        categoryComboBox.setItems(FXCollections.observableArrayList(
                categoryList.stream().map(Categorie::getNom).toList()
        ));
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
            System.out.println("bitch it works");
        } else {
            System.err.println("Warning: CSS file /com/example/css/produits.css not found. Using default styles.");
            dialogPane.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-padding: 10;");
        }
        dialogPane.getStyleClass().add("dialog-pane");
        TextField nameField = new TextField();
        TextArea descriptionField = new TextArea();
        ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(
                categoryList.stream().map(Categorie::getNom).toList()
        ));
        TextField priceField = new TextField();
        TextField quantityField = new TextField();
        TextField imagePathField = new TextField();
        imagePathField.setEditable(false); // Make it read-only
        Button chooseImageButton = new Button("Choose Image");
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(100);
        imagePreview.setFitHeight(100);
        imagePreview.setPreserveRatio(true);

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

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Description:"), descriptionField);
        grid.addRow(2, new Label("Category:"), categoryCombo);
        grid.addRow(3, new Label("Price:"), priceField);
        grid.addRow(4, new Label("Quantity:"), quantityField);
        grid.addRow(5, new Label("Image:"), imagePathField);
        grid.addRow(6, new Label(""), chooseImageButton);
        grid.addRow(7, new Label("Preview:"), imagePreview);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                try {
                    Produit newProduct = product != null ? product : new Produit();
                    newProduct.setNom(nameField.getText());
                    newProduct.setDescription(descriptionField.getText());

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
                        ProduitDAO.saveProduct(newProduct);
                        productList.add(newProduct);
                    } else {
                        ProduitDAO.updateProduct(newProduct);
                        productTableView.refresh();
                    }
                    return newProduct;
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid number format for price or quantity.");
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