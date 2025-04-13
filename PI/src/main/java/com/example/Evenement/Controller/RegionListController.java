package com.example.Evenement.Controller;

import com.example.Evenement.Dao.RegionDAO;
import javafx.scene.layout.HBox;
import com.example.Evenement.Model.Region;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.util.Optional;

public class RegionListController {
    @FXML private TableView<Region> regionTable;
    @FXML private TableColumn<Region, String> colNom;
    @FXML private TableColumn<Region, String> colVille;
    @FXML private TableColumn<Region, String> colDescription;
    @FXML private TableColumn<Region, Void> colActions;

    private final RegionDAO regionDAO = new RegionDAO();
    private final ObservableList<Region> regionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colVille.setCellValueFactory(new PropertyValueFactory<>("ville"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        setupActionColumn();
        loadRegions();
    }

    private void setupActionColumn() {
        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Region, Void> call(TableColumn<Region, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn = new Button("Modifier");
                    private final Button deleteBtn = new Button("Supprimer");
                    private final Button detailsBtn = new Button("Détails");
                    private final HBox pane = new HBox(editBtn, deleteBtn, detailsBtn);

                    {
                        pane.setSpacing(5);
                        editBtn.setStyle("-fx-background-color: #93441A; -fx-text-fill: white;");
                        deleteBtn.setStyle("-fx-background-color: #B67332; -fx-text-fill: white;");
                        detailsBtn.setStyle("-fx-background-color: #DAAB3A; -fx-text-fill: white;");

                        editBtn.setOnAction(event -> handleEdit(getTableView().getItems().get(getIndex())));
                        deleteBtn.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
                        detailsBtn.setOnAction(event -> showDetails(getTableView().getItems().get(getIndex())));
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        });
    }

    private void loadRegions() {
        regionList.setAll(regionDAO.getAllRegions());
        regionTable.setItems(regionList);
    }

    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterRegion.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter une région");
            stage.setScene(new Scene(root));
            stage.show();

            Stage currentStage = (Stage) regionTable.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddEvent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EvenementForm.fxml"));
            Parent root = loader.load();

            // Passer la région sélectionnée si nécessaire
            Region selectedRegion = regionTable.getSelectionModel().getSelectedItem();
            if (selectedRegion != null) {
                EvenementController controller = loader.getController();
                // controller.setSelectedRegion(selectedRegion); // À implémenter si besoin
            }

            Stage stage = new Stage();
            stage.setTitle("Ajouter un événement");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'événement: " + e.getMessage());
        }
    }

    private void handleEdit(Region region) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/AjouterRegion.fxml"));
            Parent root = loader.load();

            RegionController controller = loader.getController();
            controller.setRegionToEdit(region);

            Stage stage = new Stage();
            stage.setTitle("Modifier la région");
            stage.setScene(new Scene(root));
            stage.show();

            Stage currentStage = (Stage) regionTable.getScene().getWindow();
            currentStage.close();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification: " + e.getMessage());
        }
    }

    private void handleDelete(Region region) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer la région");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer la région " + region.getNom() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (regionDAO.deleteRegion(region.getId())) {
                regionList.remove(region);
                showAlert("Succès", "Région supprimée avec succès.");
            } else {
                showAlert("Erreur", "Erreur lors de la suppression.");
            }
        }
    }

    private void showDetails(Region region) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la région");
        alert.setHeaderText(region.getNom());
        alert.setContentText(
                "Ville: " + region.getVille() + "\n" +
                        "Description: " + (region.getDescription() != null ? region.getDescription() : "")
        );
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}