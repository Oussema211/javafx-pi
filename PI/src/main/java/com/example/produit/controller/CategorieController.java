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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class CategorieController {

    @FXML private TableView<Categorie> categoryTableView;
    @FXML private TableColumn<Categorie, String> nameColumn;
    @FXML private TableColumn<Categorie, String> descriptionColumn;
    @FXML private TableColumn<Categorie, String> statusColumn;
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
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
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

        TextField nameField = new TextField();
        TextArea descriptionField = new TextArea();
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("Active", "Inactive", "Draft"));
        TextField tagsField = new TextField();

        if (category != null) {
            nameField.setText(category.getNom());
            descriptionField.setText(category.getDescription());
        } else {
            statusCombo.getSelectionModel().select("Active");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Description:"), descriptionField);
        grid.addRow(2, new Label("Status:"), statusCombo);
        grid.addRow(3, new Label("Tags:"), tagsField);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                try {
                    Categorie newCategory = category != null ? category : new Categorie();
                    newCategory.setNom(nameField.getText());
                    newCategory.setDescription(descriptionField.getText());

                    User currentUser = sessionManager.getLoggedInUser();
                    if (category == null) {
                        newCategory.setId(UUID.randomUUID());
                        CategorieDAO.saveCategory(newCategory);
                        categoryList.add(newCategory);
                    } else {
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