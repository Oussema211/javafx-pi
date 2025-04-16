package com.example.Evenement.Controller;

import com.example.Evenement.Dao.EvenementDAO;
import com.example.Evenement.Model.Evenement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Collectors;

public class EvenementListController {

    @FXML
    private ListView<Evenement> eventTable;

    private final ObservableList<Evenement> eventList = FXCollections.observableArrayList();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        loadEvents();
        setupCellFactory();
    }

    private void loadEvents() {
        try {
            eventList.setAll(new EvenementDAO().getAll());
            eventTable.setItems(eventList);
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des événements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupCellFactory() {
        eventTable.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Evenement> call(ListView<Evenement> listView) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Evenement event, boolean empty) {
                        super.updateItem(event, empty);

                        if (empty || event == null) {
                            setGraphic(null);
                            return;
                        }

                        VBox box = new VBox(5);
                        box.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.07), 6, 0, 0, 3);");

                        // Affichage de l'image de l'événement
                        ImageView imageView = new ImageView();
                        if (event.getPhotoPath() != null && !event.getPhotoPath().isEmpty()) {
                            File file = new File(event.getPhotoPath());
                            if (file.exists()) {
                                imageView.setImage(new Image(file.toURI().toString()));
                                imageView.setFitWidth(100);
                                imageView.setFitHeight(100);
                                imageView.setPreserveRatio(true);
                            }
                        }

                        // Titre de l'événement
                        Label title = new Label(event.getTitre());
                        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

                        // Description de l'événement
                        Label description = new Label(event.getDescription());
                        description.setStyle("-fx-font-size: 13px; -fx-text-fill: #555;");

                        // Informations supplémentaires sur l'événement
                        Label infos = new Label(
                                "📅 Du " + event.getDateDebut().format(DATE_FORMATTER)
                                        + " au " + event.getDateFin().format(DATE_FORMATTER) + "\n" +
                                        "📍 Régions : " + event.getRegions().stream().map(r -> r.getNom()).collect(Collectors.joining(", ")) + "\n" +
                                        "🔖 Type : " + event.getType().getLabel() + " | Statut : " + event.getStatut().getLabel()
                        );
                        infos.setStyle("-fx-font-size: 12px; -fx-text-fill: #777;");

                        // Boutons d'action (modifier, supprimer, voir détails)
                        HBox buttons = new HBox(10);
                        Button btnEdit = new Button("Modifier");
                        Button btnDelete = new Button("Supprimer");
                        Button btnDetails = new Button("Voir détails");

                        btnEdit.setStyle("-fx-background-color: #93441A; -fx-text-fill: white; -fx-font-weight: bold;");
                        btnDelete.setStyle("-fx-background-color: #B67332; -fx-text-fill: white; -fx-font-weight: bold;");
                        btnDetails.setStyle("-fx-background-color: #DAAB3A; -fx-text-fill: white; -fx-font-weight: bold;");

                        btnEdit.setOnAction(e -> openEventForm(event));
                        btnDelete.setOnAction(e -> handleDelete(event));
                        btnDetails.setOnAction(e -> showEventDetails(event));

                        buttons.getChildren().addAll(btnEdit, btnDelete, btnDetails);

                        // Ajouter l'image et le texte à la boîte
                        box.getChildren().addAll(imageView, title, description, infos, buttons);
                        setGraphic(box);
                    }
                };
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
