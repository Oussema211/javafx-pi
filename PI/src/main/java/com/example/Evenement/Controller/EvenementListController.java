package com.example.Evenement.Controller;

import com.example.Evenement.Dao.EvenementDAO;
import com.example.Evenement.Model.Evenement;
import com.example.Evenement.Model.TypeEvenement;
import com.example.Evenement.Model.StatutEvenement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import java.io.IOException;
import java.util.Optional;

public class EvenementListController {
    @FXML private TableView<Evenement> eventTable;
    @FXML private TableColumn<Evenement, String> colTitre;
    @FXML private TableColumn<Evenement, String> colType;
    @FXML private TableColumn<Evenement, String> colDateDebut;
    @FXML private TableColumn<Evenement, String> colStatut;
    @FXML private TableColumn<Evenement, Void> colActions;

    private final ObservableList<Evenement> eventList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configureColumns();
        loadEvents();
    }

    private void configureColumns() {
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));

        // Pour les enums, utilisez une lambda qui appelle getLabel()
        colType.setCellValueFactory(cellData ->
                cellData.getValue().getType() != null
                        ? javafx.beans.binding.Bindings.createStringBinding(() ->
                        cellData.getValue().getType().getLabel())
                        : null);

        colDateDebut.setCellValueFactory(new PropertyValueFactory<>("dateDebut"));

        colStatut.setCellValueFactory(cellData ->
                cellData.getValue().getStatut() != null
                        ? javafx.beans.binding.Bindings.createStringBinding(() ->
                        cellData.getValue().getStatut().getLabel())
                        : null);

        colActions.setCellFactory(createActionCellFactory());
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

    private Callback<TableColumn<Evenement, Void>, TableCell<Evenement, Void>> createActionCellFactory() {
        return param -> new TableCell<>() {
            private final Button editBtn = new Button("Modifier");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                editBtn.setOnAction(event -> openEventForm(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(event -> handleDelete(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        };
    }

    @FXML
    private void handleAdd() {
        openEventForm(null);
    }

    private void openEventForm(Evenement event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EvenementForm.fxml"));
            Parent root = loader.load();

            EvenementController controller = loader.getController();
            if (event != null) {
                controller.setEvenement(event);
            }

            Stage stage = new Stage();
            stage.setTitle(event == null ? "Nouvel événement" : "Modifier événement");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            loadEvents(); // Rafraîchir la liste
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}