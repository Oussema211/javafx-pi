package com.example.Evenement.Controller;

import com.example.Evenement.Dao.EvenementDAO;
import com.example.Evenement.Model.Evenement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

public class EvenementListController {

    @FXML
    private ListView<Evenement> eventTable;
    @FXML
    private TextField searchField;
    @FXML
    private Label totalLabel;

    private final ObservableList<Evenement> eventList = FXCollections.observableArrayList();
    private final FilteredList<Evenement> filteredEvents = new FilteredList<>(eventList);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupSearchFilter();
        loadEvents();
        setupCellFactory();
        updateTotalLabel();
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredEvents.setPredicate(event -> {
                if (newValue == null || newValue.isEmpty()) {
                    return true;
                }

                String lowerCaseFilter = newValue.toLowerCase();
                return event.getTitre().toLowerCase().contains(lowerCaseFilter) ||
                        event.getDescription().toLowerCase().contains(lowerCaseFilter) ||
                        event.getType().getLabel().toLowerCase().contains(lowerCaseFilter) ||
                        event.getRegions().stream().anyMatch(r -> r.getNom().toLowerCase().contains(lowerCaseFilter));
            });
            updateTotalLabel();
        });
    }

    private void loadEvents() {
        try {
            eventList.setAll(new EvenementDAO().getAll());
            eventTable.setItems(filteredEvents);
            updateTotalLabel();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des événements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadEvents();
    }

    private void updateTotalLabel() {
        totalLabel.setText("Total: " + filteredEvents.size() + " événements");
    }

    private void setupCellFactory() {
        eventTable.setCellFactory(param -> new ListCell<Evenement>() {
            private final HBox root = new HBox(20);
            private final VBox infoBox = new VBox(8);
            private final ImageView imageView = new ImageView();
            private final Label titleLabel = new Label();
            private final Label dateLabel = new Label();
            private final Label regionLabel = new Label();
            private final Label typeLabel = new Label();
            private final HBox buttonBox = new HBox(10);

            {
                // Configuration de la mise en page
                root.setAlignment(Pos.CENTER_LEFT);
                root.setPadding(new Insets(15));
                root.setStyle("-fx-background-color: white; -fx-background-radius: 12;");

                // Style des éléments
                titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
                dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
                regionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
                typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

                // Configuration de l'image
                imageView.setFitWidth(100);
                imageView.setFitHeight(100);
                imageView.setPreserveRatio(true);
                imageView.setStyle("-fx-border-radius: 8; -fx-background-radius: 8;");

                // Configuration des boutons
                Button btnEdit = new Button("Modifier");
                Button btnDelete = new Button("Supprimer");
                Button btnDetails = new Button("Voir détails");

                btnEdit.setStyle("-fx-background-color: #93441A; -fx-text-fill: white; -fx-font-weight: bold;");
                btnDelete.setStyle("-fx-background-color: #B67332; -fx-text-fill: white; -fx-font-weight: bold;");
                btnDetails.setStyle("-fx-background-color: #DAAB3A; -fx-text-fill: white; -fx-font-weight: bold;");

                btnEdit.setOnAction(e -> openEventForm(getItem()));
                btnDelete.setOnAction(e -> handleDelete(getItem()));
                btnDetails.setOnAction(e -> showEventDetails(getItem()));

                buttonBox.getChildren().addAll(btnEdit, btnDelete, btnDetails);

                // Ajout des éléments à la VBox d'informations
                infoBox.getChildren().addAll(titleLabel, dateLabel, regionLabel, typeLabel, buttonBox);

                // Ajout des deux colonnes à la HBox racine
                root.getChildren().addAll(imageView, infoBox);
            }

            @Override
            protected void updateItem(Evenement event, boolean empty) {
                super.updateItem(event, empty);

                if (empty || event == null) {
                    setGraphic(null);
                    return;
                }

                // Mise à jour des informations
                titleLabel.setText(event.getTitre());
                dateLabel.setText("Du " + event.getDateDebut().format(DATE_FORMATTER) +
                        " au " + event.getDateFin().format(DATE_FORMATTER));
                regionLabel.setText("Régions : " + event.getRegions().stream()
                        .map(r -> r.getNom()).collect(Collectors.joining(", ")));
                typeLabel.setText("Type : " + event.getType().getLabel() +
                        " | Statut : " + event.getStatut().getLabel());

                // Mise à jour de l'image
                if (event.getPhotoPath() != null && !event.getPhotoPath().isEmpty()) {
                    File file = new File(event.getPhotoPath());
                    if (file.exists()) {
                        imageView.setImage(new Image(file.toURI().toString()));
                    } else {
                        imageView.setImage(null);
                    }
                } else {
                    imageView.setImage(null);
                }

                setGraphic(root);
            }
        });
    }

    @FXML
    private void handleAdd() {
        openEventForm(null);
    }

    private void openEventForm(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/EvenementForm.fxml"));
            Parent root = loader.load();

            EvenementController controller = loader.getController();
            if (event != null) {
                Evenement copy = new Evenement();
                copy.setId(event.getId());
                copy.setTitre(event.getTitre());
                copy.setDescription(event.getDescription());
                copy.setType(event.getType());
                copy.setStatut(event.getStatut());
                copy.setDateDebut(event.getDateDebut());
                copy.setDateFin(event.getDateFin());
                copy.setPhotoPath(event.getPhotoPath());
                copy.getRegions().addAll(event.getRegions());

                controller.setEvenement(copy);
            }

            Stage stage = new Stage();
            stage.setTitle(event == null ? "Nouvel événement" : "Modifier événement");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadEvents(); // Rafraîchir
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDelete(Evenement event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer l'événement");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer \"" + event.getTitre() + "\" ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                new EvenementDAO().delete(event.getId());
                eventList.remove(event);
                showAlert("Succès", "Événement supprimé avec succès");
                updateTotalLabel();
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage());
            }
        }
    }

    private void showEventDetails(Evenement event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'événement");
        alert.setHeaderText(event.getTitre());

        String content = String.format(
                "Description: %s%n" +
                        "Type: %s%n" +
                        "Statut: %s%n" +
                        "Date de début: %s%n" +
                        "Date de fin: %s%n" +
                        "Régions: %s%n" +
                        "Photo: %s",
                event.getDescription(),
                event.getType().getLabel(),
                event.getStatut().getLabel(),
                event.getDateDebut().format(DATE_FORMATTER),
                event.getDateFin().format(DATE_FORMATTER),
                event.getRegions().stream().map(r -> r.getNom()).collect(Collectors.joining(", ")),
                event.getPhotoPath() != null ? event.getPhotoPath() : "Aucune photo"
        );

        alert.setContentText(content);
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