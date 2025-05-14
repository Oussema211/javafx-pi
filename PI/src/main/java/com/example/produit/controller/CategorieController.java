package com.example.produit.controller;

import com.example.produit.model.Categorie;
import com.example.produit.service.CategorieDAO;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CategorieController {

    @FXML private TableView<Categorie> categoryTableView;
    @FXML private TableColumn<Categorie, String> nameColumn;
    @FXML private TableColumn<Categorie, String> parentColumn;
    @FXML private TableColumn<Categorie, String> imagePreviewColumn;
    @FXML private TableColumn<Categorie, Void> actionsColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> parentFilterComboBox;
    @FXML private Button addCategoryButton;
    @FXML private Button deleteSelectedButton;
    @FXML private Label resultsCountLabel;

    private ObservableList<Categorie> categoryList = FXCollections.observableArrayList();
    private FilteredList<Categorie> filteredList;
    private static final String IMAGE_DIR = "Uploads/images/";

    @FXML
    public void initialize() {
        if (categoryTableView == null || nameColumn == null || parentColumn == null ||
                imagePreviewColumn == null || actionsColumn == null) {
            showError("Initialization error", "One or more TableColumn fields are null. Check FXML file and controller mapping.");
            return;
        }
        try {
            createImagesDirectory();
            categoryTableView.setEditable(true);
            configureTableColumns();
            loadInitialData();
            initializeComboBox();
            setupContextMenu();
            setupActionsColumn();
            setupResultsCountListener();
            setupSearchListener();
        } catch (Exception e) {
            showError("Initialization error", e.getMessage());
        }
    }

    private void createImagesDirectory() {
        File dir = new File(IMAGE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void configureTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        parentColumn.setCellValueFactory(cellData -> {
            Categorie categorie = cellData.getValue();
            Categorie parent = categorie.getParent();
            return new SimpleStringProperty(parent != null ? parent.getNom() : "None");
        });

        imagePreviewColumn.setCellFactory(param -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(String imgUrl, boolean empty) {
                super.updateItem(imgUrl, empty);
                if (empty || imgUrl == null || imgUrl.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        Image image = new Image("file:///" + imgUrl.replace("\\", "/"));
                        imageView.setImage(image);
                        setGraphic(imageView);
                    } catch (Exception ex) {
                        setGraphic(null);
                    }
                }
            }
        });
        imagePreviewColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getImgUrl()));
    }

    private void initializeComboBox() {
        ObservableList<String> parentNames = FXCollections.observableArrayList("All");
        parentNames.addAll(categoryList.stream()
                .filter(c -> c.getNom() != null)
                .map(Categorie::getNom)
                .collect(Collectors.toList()));
        parentFilterComboBox.setItems(parentNames);
        parentFilterComboBox.getSelectionModel().selectFirst();
    }

    private void loadInitialData() {
        try {
            categoryList.clear();
            List<Categorie> categories = CategorieDAO.getAllCategories();
            categoryList.addAll(categories);
            filteredList = new FilteredList<>(categoryList, p -> true);
            categoryTableView.setItems(filteredList);
            updateResultsCount();
        } catch (Exception e) {
            showError("Error loading data", e.getMessage());
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());
        parentFilterComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleSearch());
    }

    private void setupResultsCountListener() {
        filteredList.addListener((javafx.collections.ListChangeListener<Categorie>) c -> updateResultsCount());
        updateResultsCount();
    }

    private void updateResultsCount() {
        int count = filteredList.size();
        resultsCountLabel.setText("SHOWING " + count + " CATEGORIES");
    }

    @FXML
    private void handleSearch() {
        filteredList.setPredicate(category -> {
            boolean match = true;
            String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
            if (!searchText.isEmpty()) {
                match &= category.getNom().toLowerCase().contains(searchText);
            }
            String parentFilter = parentFilterComboBox.getValue();
            if (parentFilter != null && !parentFilter.equals("All")) {
                match &= category.getParent() != null && category.getParent().getNom().equals(parentFilter);
            }
            return match;
        });
        categoryTableView.refresh();
        updateResultsCount();
    }

    @FXML
    private void handleAddCategory() {
        showCategoryDialog(null);
    }

    @FXML
    private void handleBulkDelete() {
        List<Categorie> selectedCategories = categoryTableView.getSelectionModel().getSelectedItems();
        if (selectedCategories.isEmpty()) {
            showWarning("No Selection", "Please select at least one category to delete.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Selected Categories");
        confirmation.setHeaderText("Delete " + selectedCategories.size() + " Category(ies)");
        confirmation.setContentText("Are you sure you want to delete the selected categories?");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            for (Categorie categorie : selectedCategories) {
                if (categorie.getId() != null) {
                    CategorieDAO.deleteCategory(categorie.getId());
                    categoryList.remove(categorie);
                }
            }
            handleSearch();
        }
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox hbox = new HBox(10);
            {
                editButton.getStyleClass().add("action-button");
                deleteButton.getStyleClass().add("action-button");
                editButton.setOnAction(event -> {
                    Categorie categorie = getTableView().getItems().get(getIndex());
                    handleEditCategory(categorie);
                });
                deleteButton.setOnAction(event -> {
                    Categorie categorie = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(categorie);
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

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");
        editItem.setOnAction(event -> {
            Categorie selected = categoryTableView.getSelectionModel().getSelectedItem();
            handleEditCategory(selected);
        });
        deleteItem.setOnAction(event -> {
            Categorie selected = categoryTableView.getSelectionModel().getSelectedItem();
            handleDeleteCategory(selected);
        });
        contextMenu.getItems().addAll(editItem, deleteItem);
        categoryTableView.setContextMenu(contextMenu);
    }

    private void handleEditCategory(Categorie categorie) {
        if (categorie != null && categorie.getId() != null) {
            showCategoryDialog(categorie);
        }
    }

    private void handleDeleteCategory(Categorie categorie) {
        if (categorie != null && categorie.getId() != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Delete Category");
            confirmation.setHeaderText("Delete " + categorie.getNom());
            confirmation.setContentText("Are you sure you want to delete this category?");
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                CategorieDAO.deleteCategory(categorie.getId());
                categoryList.remove(categorie);
                handleSearch();
            }
        }
    }

    private String uploadImage(File imageFile) {
        try {
            String extension = imageFile.getName().substring(imageFile.getName().lastIndexOf(".") + 1).toLowerCase();
            if (!List.of("jpg", "jpeg", "png").contains(extension)) {
                throw new IllegalArgumentException("Invalid image format. Allowed: jpg, jpeg, png.");
            }
            String newFilename = java.util.UUID.randomUUID().toString() + "." + extension;
            Path targetPath = Paths.get(IMAGE_DIR, newFilename).toAbsolutePath();
            Files.copy(imageFile.toPath(), targetPath);
            return targetPath.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    private void showCategoryDialog(Categorie categorie) {
        Dialog<Categorie> dialog = new Dialog<>();
        dialog.setTitle(categorie == null ? "New Category" : "Edit Category");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/css/produitdialog.css").toExternalForm());

        TextField nameField = new TextField();
        ComboBox<String> parentCombo = new ComboBox<>();
        ObservableList<String> parentOptions = FXCollections.observableArrayList("None");
        parentOptions.addAll(categoryList.stream()
                .filter(c -> c.getNom() != null && (categorie == null || !c.getId().equals(categorie.getId())))
                .map(Categorie::getNom)
                .collect(Collectors.toList()));
        parentCombo.setItems(parentOptions);
        TextField imageUrlField = new TextField();
        imageUrlField.setEditable(false);
        Button chooseImageButton = new Button("Choose Image");
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(100);
        imagePreview.setFitHeight(100);
        imagePreview.setPreserveRatio(true);
        Label imageStatusLabel = new Label();
        Label nameError = new Label();
        Label parentError = new Label();
        nameError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        parentError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        chooseImageButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                try {
                    imageStatusLabel.setText("Uploading image...");
                    String imagePath = uploadImage(file);
                    imageUrlField.setText(imagePath);
                    Image image = new Image("file:///" + imagePath.replace("\\", "/"));
                    imagePreview.setImage(image);
                    imageStatusLabel.setText("Image uploaded successfully");
                } catch (Exception ex) {
                    imageStatusLabel.setText("Failed to upload image: " + ex.getMessage());
                    showError("Image Upload Failed", ex.getMessage());
                }
            }
        });

        if (categorie != null) {
            nameField.setText(categorie.getNom());
            parentCombo.getSelectionModel().select(categorie.getParent() != null ? categorie.getParent().getNom() : "None");
            imageUrlField.setText(categorie.getImgUrl() != null ? categorie.getImgUrl() : "");
            if (categorie.getImgUrl() != null && !categorie.getImgUrl().isEmpty()) {
                try {
                    Image image = new Image("file:///" + imageUrlField.getText().replace("\\", "/"));
                    imagePreview.setImage(image);
                } catch (Exception ex) {
                    imagePreview.setImage(null);
                }
            }
        }

        Pattern namePattern = Pattern.compile("^[a-zA-Z0-9\\s-]{3,50}$");

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

        parentCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                parentError.setText("Select a parent category or None");
                parentCombo.setStyle("-fx-border-color: red;");
            } else {
                parentError.setText("");
                parentCombo.setStyle("");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.addRow(0, new Label("Name:"), nameField);
        grid.add(nameError, 1, 1);
        grid.addRow(2, new Label("Parent Category:"), parentCombo);
        grid.add(parentError, 1, 3);
        grid.addRow(4, new Label("Image URL:"), imageUrlField);
        grid.addRow(5, new Label(""), chooseImageButton);
        grid.addRow(6, new Label(""), imageStatusLabel);
        grid.addRow(7, new Label("Preview:"), imagePreview);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        Runnable validateForm = () -> {
            boolean isValid =
                    !nameField.getText().trim().isEmpty() &&
                            namePattern.matcher(nameField.getText()).matches() &&
                            parentCombo.getValue() != null;
            saveButton.setDisable(!isValid);
        };

        nameField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        parentCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                Categorie newCategory = categorie != null ? categorie : new Categorie();
                newCategory.setNom(nameField.getText().trim());
                String selectedParentName = parentCombo.getValue();
                Categorie selectedParent = selectedParentName.equals("None") ? null :
                        categoryList.stream()
                                .filter(c -> c.getNom().equals(selectedParentName))
                                .findFirst()
                                .orElse(null);
                newCategory.setParent(selectedParent);
                newCategory.setImgUrl(imageUrlField.getText().isEmpty() ? null : imageUrlField.getText());
                if (categorie == null) {
                    CategorieDAO.saveCategory(newCategory);
                    categoryList.add(newCategory);
                } else {
                    CategorieDAO.updateCategory(newCategory);
                    categoryTableView.refresh();
                }
                initializeComboBox();
                handleSearch();
                return newCategory;
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}