package com.example.Evenement.Controller;

import com.example.Evenement.Dao.RegionDAO;
import com.example.Evenement.Model.Region;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import java.io.IOException;
import java.util.Optional;

public class RegionListController {
    @FXML private ListView<Region> regionTable;
    @FXML private TextField searchField;
    @FXML private Label totalLabel;

    private final RegionDAO regionDAO = new RegionDAO();
    private final ObservableList<Region> regionList = FXCollections.observableArrayList();
    private final FilteredList<Region> filteredRegions = new FilteredList<>(regionList);

    @FXML
    public void initialize() {
        setupSearchFilter();
        loadRegions();
        setupCellFactory();
        updateTotalLabel();
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredRegions.setPredicate(region -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();
                return region.getNom().toLowerCase().contains(lowerCaseFilter) ||
                        region.getVille().toLowerCase().contains(lowerCaseFilter) ||
                        (region.getDescription() != null &&
                                region.getDescription().toLowerCase().contains(lowerCaseFilter));
            });
            updateTotalLabel();
        });
    }

    private void loadRegions() {
        regionList.setAll(regionDAO.getAllRegions());
        regionTable.setItems(filteredRegions);
        updateTotalLabel();
    }

    private void setupCellFactory() {
        regionTable.setCellFactory(param -> new ListCell<Region>() {
            private final HBox content = new HBox();
            private final VBox textContent = new VBox(5);
            private final Text nom = new Text();
            private final Text ville = new Text();
            private final Text description = new Text();
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final Button detailsBtn = new Button("Détails");
            private final HBox buttons = new HBox(10, editBtn, deleteBtn, detailsBtn);
            private final Pane spacer = new Pane(); // Utilisation de Pane pour éviter tout conflit

            {
                // Configuration du spacer pour pousser les boutons à droite
                HBox.setHgrow(spacer, Priority.ALWAYS);
                spacer.setMinWidth(10); // Valeur arbitraire pour éviter que le spacer ne disparaisse

                // Configuration de la mise en page
                content.setAlignment(Pos.CENTER_LEFT);
                content.setPadding(new Insets(15));
                content.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
                content.setMaxWidth(Double.MAX_VALUE);

                // Style des textes
                nom.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-fill: #2c3e50;");
                ville.setStyle("-fx-font-size: 14px; -fx-fill: #666;");
                description.setStyle("-fx-font-size: 14px; -fx-fill: #666;");

                // Style des boutons
                String buttonStyle = "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;";
                editBtn.setStyle("-fx-background-color: #93441A; " + buttonStyle);
                deleteBtn.setStyle("-fx-background-color: #B67332; " + buttonStyle);
                detailsBtn.setStyle("-fx-background-color: #DAAB3A; " + buttonStyle);

                // Actions des boutons
                editBtn.setOnAction(event -> handleEdit(getItem()));
                deleteBtn.setOnAction(event -> handleDelete(getItem()));
                detailsBtn.setOnAction(event -> showDetails(getItem()));

                // Assemblage des composants
                textContent.getChildren().addAll(nom, ville, description);
                content.getChildren().addAll(textContent, spacer, buttons);
            }

            @Override
            protected void updateItem(Region region, boolean empty) {
                super.updateItem(region, empty);
                if (empty || region == null) {
                    setGraphic(null);
                } else {
                    nom.setText("Nom: " + region.getNom());
                    ville.setText("Ville: " + region.getVille());
                    description.setText("Description: " +
                            (region.getDescription() != null ? region.getDescription() : ""));
                    setGraphic(content);
                }
            }
        });
    }

    // ... (les autres méthodes restent inchangées)
    @FXML
    private void handleAdd() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/AjouterRegion.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter une région");
            stage.setScene(new Scene(root));
            stage.show();
            stage.setOnHidden(e -> loadRegions());
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddEvent() {
        Region selectedRegion = regionTable.getSelectionModel().getSelectedItem();
        if (selectedRegion == null) {
            showAlert("Aucune sélection", "Veuillez sélectionner une région avant d'ajouter un événement.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/EvenementForm.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Ajouter un événement pour " + selectedRegion.getNom());
            stage.setScene(new Scene(root));
            stage.show();
            stage.setOnHidden(e -> loadRegions());
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'événement: " + e.getMessage());
        }
    }

    private void handleEdit(Region region) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/AjouterRegion.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Modifier la région: " + region.getNom());
            stage.setScene(new Scene(root));
            stage.show();
            stage.setOnHidden(e -> loadRegions());
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire de modification: " + e.getMessage());
        }
    }

    private void handleDelete(Region region) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer la région " + region.getNom());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette région?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (regionDAO.deleteRegion(region.getId())) {
                regionList.remove(region);
                updateTotalLabel();
                showAlert("Succès", "La région a été supprimée avec succès.");
            } else {
                showAlert("Erreur", "La suppression a échoué.");
            }
        }
    }

    private void showDetails(Region region) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de la région");
        alert.setHeaderText(region.getNom());

        String details = String.format(
                "Ville: %s\n\nDescription: %s",
                region.getVille(),
                region.getDescription() != null ? region.getDescription() : "Aucune description"
        );

        alert.setContentText(details);
        alert.showAndWait();
    }

    @FXML
    private void handleRefresh() {
        loadRegions();
    }

    private void updateTotalLabel() {
        totalLabel.setText("Total: " + filteredRegions.size() + " régions");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}