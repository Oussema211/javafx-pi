package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.service.EntrepotService;
import com.example.auth.utils.SessionManager;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EntrepotController {

    // FXML References
    @FXML private TextField searchField;
    @FXML private ComboBox<String> villeFilter;
    @FXML private ComboBox<String> espaceFilter;
    @FXML private Button resetBtn;
    @FXML private Button ajouterBtn;
    @FXML private Button excelBtn;
    @FXML private Button pdfBtn;
    @FXML private ListView<Entrepot> entrepotList;
    @FXML private Button sortByNameBtn;
    @FXML private Button sortByVilleBtn;
    @FXML private Button sortByEspaceBtn;

    private final EntrepotService entrepotService = new EntrepotService();
    private final ObservableList<Entrepot> entrepotData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            loadRealData();
            configureList();
            configureFilters();
            applyFilters();
            FadeTransition fade = new FadeTransition(Duration.millis(1000), entrepotList);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);
            fade.play();
            sortByNameBtn.setOnAction(e -> sortListByName());
            sortByVilleBtn.setOnAction(e -> sortListByVille());
            sortByEspaceBtn.setOnAction(e -> sortListByEspace());
        });
    }

    private void loadRealData() {
        entrepotData.clear();
        entrepotData.addAll(entrepotService.getAllEntrepots());
    }

    public void refreshEntrepotData() {
        loadRealData();
        entrepotList.refresh();
        configureFilters();
    }

    private void configureList() {
        entrepotList.setCellFactory(param -> new ListCell<Entrepot>() {
            @Override
            protected void updateItem(Entrepot entrepot, boolean empty) {
                super.updateItem(entrepot, empty);
                if (empty || entrepot == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("");
                    return;
                }

                HBox mainContainer = new HBox(15);
                mainContainer.setAlignment(Pos.CENTER_LEFT);
                mainContainer.setPadding(new Insets(15));
                mainContainer.getStyleClass().add("entrepot-cell");


                ImageView imageView = new ImageView();
                imageView.setFitHeight(60);
                imageView.setFitWidth(60);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);

                // Center ImageView in Pane
                imageView.setLayoutX(5);
                imageView.setLayoutY(5);

                VBox infoBox = new VBox(8);
                infoBox.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(infoBox, Priority.ALWAYS);

                Label nameLabel = new Label(entrepot.getNom());
                nameLabel.getStyleClass().add("entrepot-name");

                HBox detailsBox = new HBox(20);
                detailsBox.setAlignment(Pos.CENTER_LEFT);

                Label adresseLabel = new Label("📍 " + (entrepot.getAdresse() != null ? entrepot.getAdresse() : "Non spécifiée"));
                adresseLabel.getStyleClass().add("entrepot-detail");

                Label villeLabel = new Label("🏙️ " + (entrepot.getVille() != null ? entrepot.getVille() : "Non spécifiée"));
                villeLabel.getStyleClass().add("entrepot-detail");

                Label espaceLabel = new Label("📏 " + entrepot.getEspace() + " m²");
                espaceLabel.getStyleClass().add("entrepot-detail");

                detailsBox.getChildren().addAll(adresseLabel, villeLabel, espaceLabel);
                infoBox.getChildren().addAll(nameLabel, detailsBox);

                HBox actionBox = new HBox(10);
                actionBox.setAlignment(Pos.CENTER_RIGHT);

                Button editBtn = new Button("✏️ Modifier");
                editBtn.getStyleClass().add("action-button");

                Button deleteBtn = new Button("🗑️ Supprimer");
                deleteBtn.getStyleClass().add("action-button-danger");

                mainContainer.getChildren().addAll(imageView, infoBox, actionBox);
                actionBox.getChildren().addAll(editBtn, deleteBtn);

                // Event handlers after node construction
                editBtn.setOnAction(e -> editEntrepot(entrepot));
                deleteBtn.setOnAction(e -> deleteEntrepot(entrepot));

                // Animation
                mainContainer.setTranslateY(20);
                mainContainer.setOpacity(0);
                Platform.runLater(() -> {
                    Timeline timeline = new Timeline(
                            new KeyFrame(Duration.millis(300), new KeyValue(mainContainer.translateYProperty(), 0)),
                            new KeyFrame(Duration.millis(300), new KeyValue(mainContainer.opacityProperty(), 1))
                    );
                    timeline.play();
                });

                // Tooltip
                Tooltip tooltip = new Tooltip("Entrepôt: " + entrepot.getNom() + "\nAdresse: " +
                        (entrepot.getAdresse() != null ? entrepot.getAdresse() : "Non spécifiée"));
                tooltip.setStyle("-fx-text-fill:white; " + "-fx-font-size: 14px; ");
                tooltip.setShowDelay(Duration.millis(200));
                Tooltip.install(mainContainer, tooltip);

                // Context Menu
                ContextMenu contextMenu = new ContextMenu();
                MenuItem editItem = new MenuItem("Modifier");
                editItem.setOnAction(e -> editEntrepot(entrepot));
                MenuItem deleteItem = new MenuItem("Supprimer");
                deleteItem.setOnAction(e -> deleteEntrepot(entrepot));
                contextMenu.getItems().addAll(editItem, deleteItem);
                setContextMenu(contextMenu);

                mainContainer.setOnMouseClicked(e -> {
                    if (e.getClickCount() == 2) {
                        editEntrepot(entrepot);
                    }
                });

                setGraphic(mainContainer);
            }
        });
        entrepotList.setItems(entrepotData);
        entrepotList.getStyleClass().add("stock-list");
    }

    private void configureFilters() {
        Set<String> villes = entrepotData.stream()
                .map(Entrepot::getVille)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        villeFilter.getItems().clear();
        villeFilter.getItems().add("Toutes les villes");
        villeFilter.getItems().addAll(villes);
        villeFilter.setValue("Toutes les villes");

        espaceFilter.getItems().clear();
        espaceFilter.getItems().addAll("Tous", "< 100 m²", "100-500 m²", "500-1000 m²", "> 1000 m²");
        espaceFilter.setValue("Tous");

        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        villeFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        espaceFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String selectedVille = villeFilter.getValue();
        String selectedEspace = espaceFilter.getValue();

        List<Entrepot> filteredList = entrepotData.stream()
                .filter(entrepot -> {
                    // Filtre par texte
                    if (!searchText.isEmpty() &&
                            !entrepot.getNom().toLowerCase().contains(searchText) &&
                            (entrepot.getAdresse() == null || !entrepot.getAdresse().toLowerCase().contains(searchText)) &&
                            (entrepot.getVille() == null || !entrepot.getVille().toLowerCase().contains(searchText))) {
                        return false;
                    }

                    // Filtre par ville
                    if (!"Toutes les villes".equals(selectedVille) &&
                            (entrepot.getVille() == null || !entrepot.getVille().equals(selectedVille))) {
                        return false;
                    }

                    // Filtre par espace
                    if (!"Tous".equals(selectedEspace)) {
                        double espace = entrepot.getEspace();
                        switch (selectedEspace) {
                            case "< 100 m²":
                                return espace < 100;
                            case "100-500 m²":
                                return espace >= 100 && espace <= 500;
                            case "500-1000 m²":
                                return espace > 500 && espace <= 1000;
                            case "> 1000 m²":
                                return espace > 1000;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        entrepotList.setItems(FXCollections.observableArrayList(filteredList));
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        villeFilter.setValue("Toutes les villes");
        espaceFilter.setValue("Tous");
        applyFilters();
    }

    private void editEntrepot(Entrepot entrepot) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Entrepot/view/EditEntrepotForm.fxml"));
            if (loader.getLocation() == null) {
                showAlert("Erreur", "Fichier EditEntrepotForm.fxml introuvable", Alert.AlertType.ERROR);
                return;
            }
            Parent root = loader.load();

            EditEntrepotController controller = loader.getController();
            controller.setEntrepot(entrepot);
            controller.setParentController(this);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Modifier l'entrepôt");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre de modification: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void deleteEntrepot(Entrepot entrepot) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'entrepôt");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cet entrepôt ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = entrepotService.deleteEntrepot(entrepot.getId());
            if (success) {
                entrepotData.remove(entrepot);
                showAlert("Succès", "Entrepôt supprimé avec succès", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Erreur", "Échec de la suppression de l'entrepôt", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Entrepot/view/AddEntrepotForm.fxml"));
            if (loader.getLocation() == null) {
                showAlert("Erreur", "Fichier AddEntrepotForm.fxml introuvable", Alert.AlertType.ERROR);
                return;
            }
            Parent root = loader.load();

            AddEntrepotController controller = loader.getController();
            controller.setParentController(this);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Ajouter un nouvel entrepôt");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleExportExcel() {
        // TODO: Implement Excel export
        showAlert("Information", "Export vers Excel non implémenté", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleExportPDF() {
        // TODO: Implement PDF export
        showAlert("Information", "Export vers PDF non implémenté", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void sortListByName() {
        List<Entrepot> sortedList = entrepotData.stream()
                .sorted(Comparator.comparing(Entrepot::getNom, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        entrepotList.setItems(FXCollections.observableArrayList(sortedList));
    }

    private void sortListByVille() {
        List<Entrepot> sortedList = entrepotData.stream()
                .sorted(Comparator.comparing(entrepot -> entrepot.getVille() != null ? entrepot.getVille() : "", String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
        entrepotList.setItems(FXCollections.observableArrayList(sortedList));
    }

    private void sortListByEspace() {
        List<Entrepot> sortedList = entrepotData.stream()
                .sorted(Comparator.comparingDouble(Entrepot::getEspace))
                .collect(Collectors.toList());
        entrepotList.setItems(FXCollections.observableArrayList(sortedList));
    }
}