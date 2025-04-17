package com.example.produit.controller;

import com.example.auth.model.User;
import com.example.auth.utils.SessionManager;
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
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class CategorieController {

    @FXML private TableView<Categorie> categoryTableView;
    @FXML private TableColumn<Categorie, String> nameColumn;
    @FXML private TableColumn<Categorie, String> descriptionColumn;
    @FXML private TableColumn<Categorie, LocalDateTime> dateCreationColumn;
    @FXML private TableColumn<Categorie, Void> actionsColumn;

    @FXML private Label resultsCountLabel;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private ObservableList<Categorie> categoryList = FXCollections.observableArrayList();
    private FilteredList<Categorie> filteredList;

    @FXML
    public void initialize() {
        configureTableColumns();
        loadInitialData();
        setupContextMenu();
        setupActionsColumn();
        setupResultsCountListener();
    }

    private void configureTableColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
    }

    private void loadInitialData() {
        categoryList.addAll(CategorieDAO.getAllCategories());
        filteredList = new FilteredList<>(categoryList, c -> true);
        categoryTableView.setItems(filteredList);
        updateResultsCount();
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<Categorie, Void>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox hbox = new HBox(10);

            {
                Image editImage = new Image(getClass().getResourceAsStream("/icons/edit.png"));
                Image deleteImage = new Image(getClass().getResourceAsStream("/icons/delete.png"));
                editButton.setGraphic(new ImageView(editImage));
                deleteButton.setGraphic(new ImageView(deleteImage));
                editButton.getStyleClass().add("action-button");
                deleteButton.getStyleClass().add("action-button");

                editButton.setOnAction(event -> {
                    Categorie category = getTableView().getItems().get(getIndex());
                    handleEditCategory(category);
                });

                deleteButton.setOnAction(event -> {
                    Categorie category = getTableView().getItems().get(getIndex());
                    handleDeleteCategory(category);
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
        filteredList.addListener((javafx.collections.ListChangeListener<Categorie>) c -> updateResultsCount());
        updateResultsCount();
    }

    private void updateResultsCount() {
        int count = filteredList.size();
        resultsCountLabel.setText("SHOWING " + count + " CATEGORIES");
    }

    @FXML
    private void handleAddCategory() {
        showCategoryDialog(null);
    }

    private void handleEditCategory(Categorie category) {
        if (category != null) {
            showCategoryDialog(category);
        }
    }

    private void handleDeleteCategory(Categorie category) {
        if (category != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Delete Category");
            confirmation.setHeaderText("Delete " + category.getNom());
            confirmation.setContentText("Are you sure you want to delete this category?");
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                CategorieDAO.deleteCategory(category.getId());
                categoryList.remove(category);
            }
        }
    }

    private void showCategoryDialog(Categorie category) {
        Dialog<Categorie> dialog = new Dialog<>();
        dialog.setTitle(category == null ? "New Category" : "Edit Category");

        DialogPane dialogPane = dialog.getDialogPane();
        URL cssUrl = getClass().getResource("/com/example/css/categoriedialog.css");
        if (cssUrl != null) {
            dialogPane.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Warning: CSS file /com/example/css/categoriedialog.css not found. Using default styles.");
            dialogPane.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-padding: 10;");
        }
        dialogPane.getStyleClass().add("dialog-pane");

        // Form fields
        TextField nameField = new TextField();
        nameField.getStyleClass().add("modal-text-field");
        TextArea descriptionField = new TextArea();
        descriptionField.getStyleClass().add("modal-text-field");

        // Error labels (small font, minimal space)
        Label nameError = new Label();
        Label descriptionError = new Label();
        nameError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        descriptionError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

        // Populate fields for edit
        if (category != null) {
            nameField.setText(category.getNom());
            descriptionField.setText(category.getDescription());
        }

        // Validation patterns and rules
        Pattern namePattern = Pattern.compile("^[a-zA-Z0-9\\s-]{3,50}$");

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

        // Grid layout with error labels in same column
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5); // Reduced gap to minimize space
        grid.addRow(0, new Label("Name:"), nameField);
        grid.add(nameError, 1, 1);
        grid.addRow(2, new Label("Description:"), descriptionField);
        grid.add(descriptionError, 1, 3);

        grid.getChildren().forEach(node -> {
            if (node instanceof Label && !node.getStyle().contains("-fx-font-size: 10px")) {
                node.getStyleClass().add("modal-label");
            }
        });

        VBox modalContent = new VBox(grid);
        modalContent.getStyleClass().add("modal-vbox");
        dialog.getDialogPane().setContent(modalContent);

        // Add buttons with styling
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Style the buttons
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.getStyleClass().add("modal-save-button");
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).getStyleClass().add("modal-cancel-button");

        // Disable save button until all validations pass
        saveButton.setDisable(true);

        // Validation check for save button
        Runnable validateForm = () -> {
            boolean isValid =
                    !nameField.getText().trim().isEmpty() &&
                            namePattern.matcher(nameField.getText()).matches() &&
                            !descriptionField.getText().trim().isEmpty() &&
                            descriptionField.getText().length() <= 200;
            saveButton.setDisable(!isValid);
        };

        nameField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        descriptionField.textProperty().addListener((obs, old, newVal) -> validateForm.run());

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                try {
                    Categorie newCategory = category != null ? category : new Categorie();
                    newCategory.setNom(nameField.getText().trim());
                    newCategory.setDescription(descriptionField.getText().trim());
                    if (category == null) {
                        newCategory.setDateCreation(LocalDateTime.now());
                        newCategory.setId(UUID.randomUUID());
                        CategorieDAO.saveCategory(newCategory);
                        categoryList.add(newCategory);
                    } else {
                        // Preserve existing dateCreation for edits
                        CategorieDAO.updateCategory(newCategory);
                        categoryTableView.refresh();
                    }
                    return newCategory;
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error saving category: " + ex.getMessage());
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
}