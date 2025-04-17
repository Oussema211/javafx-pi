package com.example.Evenement.Controller;

import com.example.Evenement.Dao.RegionDAO;
import com.example.Evenement.Model.Region;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Optional;

public class RegionListController {
    @FXML
    private ListView<Region> regionTable; // Changé de TableView à ListView

    private final RegionDAO regionDAO = new RegionDAO();
    private final ObservableList<Region> regionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        regionTable.setCellFactory(param -> new ListCell<Region>() {
            private final HBox content = new HBox();
            private final Text nom = new Text();
            private final Text ville = new Text();
            private final Text description = new Text();
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final Button detailsBtn = new Button("Détails");
            private final HBox buttons = new HBox(editBtn, deleteBtn, detailsBtn);

            {
                VBox textContent = new VBox(nom, ville, description);
                textContent.setSpacing(5);
                content.getChildren().addAll(textContent, buttons);
                content.setSpacing(20);

                buttons.setSpacing(5);
                buttons.setStyle("-fx-padding: 5;");

                editBtn.setStyle("-fx-background-color: #93441A; -fx-text-fill: white; -fx-padding: 5 10;");
                deleteBtn.setStyle("-fx-background-color: #B67332; -fx-text-fill: white; -fx-padding: 5 10;");
                detailsBtn.setStyle("-fx-background-color: #DAAB3A; -fx-text-fill: white; -fx-padding: 5 10;");

                // Actions des boutons
                editBtn.setOnAction(event -> handleEdit(getItem()));
                deleteBtn.setOnAction(event -> handleDelete(getItem()));
                detailsBtn.setOnAction(event -> showDetails(getItem()));
            }

            @Override
            protected void updateItem(Region region, boolean empty) {
                super.updateItem(region, empty);
                if (region != null && !empty) {
                    nom.setText("Nom: " + region.getNom());
                    ville.setText("Ville: " + region.getVille());
                    description.setText("Description: " + (region.getDescription() != null ? region.getDescription() : ""));
                    setGraphic(content);
                } else {
                    setGraphic(null);
                }
            }
        });

        loadRegions();
    }

    private void loadRegions() {
        regionList.setAll(regionDAO.getAllRegions());
        regionTable.setItems(regionList);
    }

    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/AjouterRegion.fxml"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/EvenementForm.fxml"));
            Parent root = loader.load();

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/AjouterRegion.fxml"));
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
