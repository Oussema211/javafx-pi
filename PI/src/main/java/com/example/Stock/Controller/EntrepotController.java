package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.service.EntrepotService;
import com.example.auth.utils.SessionManager;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EntrepotController {

    // Références FXML
    @FXML private TableView<Entrepot> entrepotTable;
    @FXML private TableColumn<Entrepot, String> colId;
    @FXML private TableColumn<Entrepot, String> colNom;
    @FXML private TableColumn<Entrepot, String> colAdresse;
    @FXML private TableColumn<Entrepot, String> colVille;
    @FXML private TableColumn<Entrepot, Double> colEspace;
    @FXML private TableColumn<Entrepot, Void> colActions;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> villeFilter;
    @FXML private ComboBox<String> espaceFilter;
    @FXML private Button resetBtn;

    @FXML private Button ajouterBtn;
    @FXML private Button excelBtn;
    @FXML private Button pdfBtn;

    private final EntrepotService entrepotService = new EntrepotService();
    private final ObservableList<Entrepot> entrepotData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        loadRealData();
        configureTable();
        configureFilters();
        applyFilters();
    }

    private void loadRealData() {
        entrepotData.clear();
        entrepotData.addAll(entrepotService.getAllEntrepots());
    }

    public void refreshEntrepotData() {
        entrepotData.clear();
        entrepotData.addAll(entrepotService.getAllEntrepots());
        entrepotTable.refresh();
        configureFilters(); // Recharger les filtres après mise à jour
    }

    private void configureTable() {
        // Colonne ID
        colId.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getId().toString().substring(0, 8)));

        // Colonne Nom
        colNom.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getNom()));

        // Colonne Adresse
        colAdresse.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAdresse()));

        // Colonne Ville
        colVille.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getVille()));

        // Colonne Espace
        colEspace.setCellValueFactory(cellData ->
                new SimpleDoubleProperty(cellData.getValue().getEspace()).asObject());

        // Colonne Actions
        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Entrepot, Void> call(TableColumn<Entrepot, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = new Button("✏️");
                    private final Button deleteBtn = new Button("🗑️");
                    private final HBox box = new HBox(5, editBtn, deleteBtn);

                    {
                        box.setAlignment(Pos.CENTER);
                        editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

                        editBtn.setOnAction(e -> {
                            Entrepot entrepot = getTableView().getItems().get(getIndex());
                            editEntrepot(entrepot);
                        });

                        deleteBtn.setOnAction(e -> {
                            Entrepot entrepot = getTableView().getItems().get(getIndex());
                            deleteEntrepot(entrepot);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : box);
                    }
                };
            }
        });

        entrepotTable.setItems(entrepotData);
    }

    private Callback<TableColumn<Entrepot, Void>, TableCell<Entrepot, Void>> createActionCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Entrepot, Void> call(TableColumn<Entrepot, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = new Button("✏️");
                    private final Button deleteBtn = new Button("🗑️");
                    private final HBox box = new HBox(5, editBtn, deleteBtn);

                    {
                        box.setAlignment(javafx.geometry.Pos.CENTER);
                        editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

                        editBtn.setOnAction(e -> {
                            Entrepot entrepot = getTableView().getItems().get(getIndex());
                            editEntrepot(entrepot);
                        });

                        deleteBtn.setOnAction(e -> {
                            Entrepot entrepot = getTableView().getItems().get(getIndex());
                            deleteEntrepot(entrepot);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : box);
                    }
                };
            }
        };
    }

    private void configureFilters() {
        // Récupérer les villes distinctes
        Set<String> villes = entrepotData.stream()
                .map(Entrepot::getVille)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Configurer les ComboBox
        villeFilter.getItems().clear();
        villeFilter.getItems().add("Toutes les villes");
        villeFilter.getItems().addAll(villes);
        villeFilter.setValue("Toutes les villes");

        espaceFilter.getItems().clear();
        espaceFilter.getItems().addAll("Tous", "< 100 m²", "100-500 m²", "500-1000 m²", "> 1000 m²");
        espaceFilter.setValue("Tous");

        // Ajouter les listeners
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        villeFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        espaceFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String selectedVille = villeFilter.getValue();
        String selectedEspace = espaceFilter.getValue();

        ObservableList<Entrepot> filteredList = entrepotData.filtered(entrepot -> {
            // Filtre par texte
            if (!searchText.isEmpty() &&
                    !entrepot.getNom().toLowerCase().contains(searchText) &&
                    !entrepot.getAdresse().toLowerCase().contains(searchText) &&
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
                    case "< 100 m²": if (espace >= 100) return false; break;
                    case "100-500 m²": if (espace < 100 || espace > 500) return false; break;
                    case "500-1000 m²": if (espace < 500 || espace > 1000) return false; break;
                    case "> 1000 m²": if (espace <= 1000) return false; break;
                }
            }

            return true;
        });

        entrepotTable.setItems(filteredList);
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
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre de modification", Alert.AlertType.ERROR);
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
        // Implémentez l'export Excel
        System.out.println("Export vers Excel");
    }

    @FXML
    private void handleExportPDF() {
        // Implémentez l'export PDF
        System.out.println("Export vers PDF");
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}