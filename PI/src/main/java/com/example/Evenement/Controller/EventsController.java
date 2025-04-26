package com.example.Evenement.Controller;

import com.example.Evenement.Dao.EvenementDAO;
import com.example.Evenement.Model.Evenement;
import com.example.Evenement.Model.TypeEvenement;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

public class EventsController {

    // FXML Components
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane eventsFlowPane;
    @FXML private Button previousPageBtn;
    @FXML private Button nextPageBtn;

    // Data and State
    private final ObservableList<Evenement> eventList = FXCollections.observableArrayList();
    private final FilteredList<Evenement> filteredEvents = new FilteredList<>(eventList);
    private int currentPage = 1;
    private final int itemsPerPage = 6;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        setupSortComboBox();
        setupSearchFilter();
        loadEvents();
        updatePaginationButtons();
    }

    private void setupSortComboBox() {
        ObservableList<String> options = FXCollections.observableArrayList(
                "Par défaut", "Foire", "Formation", "Conférence"
        );
        sortCombo.setItems(options);
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setOnAction(event -> handleSort());
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
            currentPage = 1; // Reset to first page on search
            loadEvents();
            updatePaginationButtons();
        });
    }

    private void loadEvents() {
        try {
            eventList.setAll(new EvenementDAO().getAll());
            displayEvents();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des événements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void displayEvents() {
        eventsFlowPane.getChildren().clear();
        String sortType = sortCombo.getSelectionModel().getSelectedItem().toLowerCase();
        ObservableList<Evenement> sortedEvents = FXCollections.observableArrayList(filteredEvents);

        // Apply sorting
        if (!sortType.equals("par défaut")) {
            sortedEvents = sortedEvents.filtered(e -> e.getType().getLabel().toLowerCase().equals(sortType));
        }

        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, sortedEvents.size());

        for (int i = startIndex; i < endIndex; i++) {
            eventsFlowPane.getChildren().add(createEventCard(sortedEvents.get(i)));
        }
    }

    private VBox createEventCard(Evenement event) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-border-radius: 8; -fx-background-radius: 8; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 5, 0, 0, 0);");
        card.setPadding(new Insets(15));
        card.setPrefWidth(300);

        // Image
        ImageView eventImage = new ImageView();
        eventImage.setFitWidth(270);
        eventImage.setFitHeight(180);
        eventImage.setPreserveRatio(true);
        if (event.getPhotoPath() != null && !event.getPhotoPath().isEmpty()) {
            File file = new File(event.getPhotoPath());
            if (file.exists()) {
                eventImage.setImage(new Image(file.toURI().toString()));
            } else {
                eventImage.setImage(new Image(getClass().getResourceAsStream(
                        "/com/example/Evenement/images/default-event.jpg")));
            }
        } else {
            eventImage.setImage(new Image(getClass().getResourceAsStream(
                    "/com/example/Evenement/images/default-event.jpg")));
        }

        // Title
        Label titleLabel = new Label(event.getTitre());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);

        // Dates
        Label dateLabel = new Label("Du " + event.getDateDebut().format(DATE_FORMATTER) +
                " au " + event.getDateFin().format(DATE_FORMATTER));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Regions
        Label regionLabel = new Label("Régions : " + event.getRegions().stream()
                .map(r -> r.getNom()).collect(Collectors.joining(", ")));
        regionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Type and Status
        Label typeLabel = new Label("Type : " + event.getType().getLabel() +
                " | Statut : " + event.getStatut().getLabel());
        typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");

        // Details Button
        Button detailsBtn = new Button("Voir détails");
        detailsBtn.setStyle("-fx-background-color: #689f38; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 8;");
        detailsBtn.setOnAction(e -> showEventDetails(event));

        // Layout
        VBox infoBox = new VBox(8, titleLabel, dateLabel, regionLabel, typeLabel, detailsBtn);
        card.getChildren().addAll(eventImage, infoBox);

        return card;
    }

    private void showEventDetails(Evenement event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails de l'événement");
        alert.setHeaderText(event.getTitre());

        String content = String.format(
                "Description: %s\n" +
                        "Type: %s\n" +
                        "Statut: %s\n" +
                        "Date de début: %s\n" +
                        "Date de fin: %s\n" +
                        "Régions: %s\n" +
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

    @FXML
    private void handleSort() {
        currentPage = 1; // Reset to first page on sort
        displayEvents();
        updatePaginationButtons();
    }

    @FXML
    private void filterByFoire() {
        sortCombo.getSelectionModel().select("Foire");
        handleSort();
    }

    @FXML
    private void filterByFormation() {
        sortCombo.getSelectionModel().select("Formation");
        handleSort();
    }

    @FXML
    private void filterByConference() {
        sortCombo.getSelectionModel().select("Conférence");
        handleSort();
    }

    @FXML
    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            displayEvents();
            updatePaginationButtons();
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage * itemsPerPage < filteredEvents.size()) {
            currentPage++;
            displayEvents();
            updatePaginationButtons();
        }
    }

    @FXML
    private void goToPage(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        currentPage = Integer.parseInt(source.getText());
        displayEvents();
        updatePaginationButtons();
    }

    private void updatePaginationButtons() {
        previousPageBtn.setDisable(currentPage <= 1);
        nextPageBtn.setDisable(currentPage * itemsPerPage >= filteredEvents.size());
    }
}