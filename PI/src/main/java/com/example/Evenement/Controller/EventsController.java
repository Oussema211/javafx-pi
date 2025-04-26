package com.example.Evenement.Controller;

import com.example.Evenement.Dao.EvenementDAO;
import com.example.Evenement.Model.Evenement;
import com.example.Evenement.Model.TypeEvenement;
import com.example.Evenement.Service.GoogleCalendarService;
import com.google.api.services.calendar.model.Event;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class EventsController {

    // FXML Components
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private FlowPane eventsFlowPane;
    @FXML private Button previousPageBtn;
    @FXML private Button nextPageBtn;

    // Nouveaux composants FXML pour le calendrier
    @FXML private Button prevMonthBtn;
    @FXML private Button nextMonthBtn;
    @FXML private Label currentMonthLabel;
    @FXML private GridPane calendarGrid;
    @FXML private VBox eventDetailsPanel;
    @FXML private Label eventTitleLabel;
    @FXML private Label eventDateLabel;
    @FXML private Label eventDescriptionLabel;

    // Data and State
    private final ObservableList<Evenement> eventList = FXCollections.observableArrayList();
    private final FilteredList<Evenement> filteredEvents = new FilteredList<>(eventList);
    private int currentPage = 1;
    private final int itemsPerPage = 6;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // État du calendrier
    private YearMonth currentYearMonth = YearMonth.now();
    private Map<LocalDate, Evenement> eventMap = new HashMap<>();

    private GoogleCalendarService googleCalendarService;
    private Map<String, String> eventIdMap = new HashMap<>(); // Maps local event IDs to Google Calendar event IDs

    @FXML
    public void initialize() {
        try {
            googleCalendarService = new GoogleCalendarService();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'initialisation de Google Calendar: " + e.getMessage());
            e.printStackTrace();
        }
        
        setupSortComboBox();
        setupSearchFilter();
        loadEvents();
        updatePaginationButtons();
        setupCalendar();
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
            // Charger les événements locaux
            eventList.setAll(new EvenementDAO().getAll());
            
            // Synchroniser avec Google Calendar
            syncWithGoogleCalendar();
            
            displayEvents();
            loadEventsIntoCalendar();
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des événements: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncWithGoogleCalendar() {
        try {
            // Obtenir les événements du mois en cours
            LocalDateTime startOfMonth = currentYearMonth.atDay(1).atStartOfDay();
            LocalDateTime endOfMonth = currentYearMonth.atEndOfMonth().atTime(23, 59, 59);
            
            List<Event> googleEvents = googleCalendarService.getEvents(startOfMonth, endOfMonth);
            
            // Synchroniser chaque événement local avec Google Calendar
            for (Evenement localEvent : eventList) {
                String googleEventId = eventIdMap.get(localEvent.getId().toString());
                
                if (googleEventId == null) {
                    // Créer un nouvel événement dans Google Calendar
                    Event newGoogleEvent = googleCalendarService.createEvent(
                        localEvent.getTitre(),
                        localEvent.getDescription(),
                        localEvent.getDateDebut(),
                        localEvent.getDateFin()
                    );
                    eventIdMap.put(localEvent.getId().toString(), newGoogleEvent.getId());
                } else {
                    // Mettre à jour l'événement existant dans Google Calendar
                    googleCalendarService.updateEvent(
                        googleEventId,
                        localEvent.getTitre(),
                        localEvent.getDescription(),
                        localEvent.getDateDebut(),
                        localEvent.getDateFin()
                    );
                }
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la synchronisation avec Google Calendar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createEventInGoogleCalendar(Evenement event) {
        try {
            Event googleEvent = googleCalendarService.createEvent(
                event.getTitre(),
                event.getDescription(),
                event.getDateDebut(),
                event.getDateFin()
            );
            eventIdMap.put(event.getId().toString(), googleEvent.getId());
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la création de l'événement dans Google Calendar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateEventInGoogleCalendar(Evenement event) {
        try {
            String googleEventId = eventIdMap.get(event.getId().toString());
            if (googleEventId != null) {
                googleCalendarService.updateEvent(
                    googleEventId,
                    event.getTitre(),
                    event.getDescription(),
                    event.getDateDebut(),
                    event.getDateFin()
                );
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la mise à jour de l'événement dans Google Calendar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteEventFromGoogleCalendar(Evenement event) {
        try {
            String googleEventId = eventIdMap.get(event.getId().toString());
            if (googleEventId != null) {
                googleCalendarService.deleteEvent(googleEventId);
                eventIdMap.remove(event.getId().toString());
            }
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la suppression de l'événement dans Google Calendar: " + e.getMessage());
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
        try {
            // Charger le FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/event-details.fxml"));
            Parent root = loader.load();

            // Configurer le contrôleur
            EventDetailsController controller = loader.getController();
            controller.setEvent(event);

            // Créer et configurer la nouvelle fenêtre
            Stage detailsStage = new Stage();
            detailsStage.setTitle("Détails de l'événement");
            detailsStage.initModality(Modality.APPLICATION_MODAL);
            controller.setStage(detailsStage);

            // Configurer la scène
            Scene scene = new Scene(root);
            detailsStage.setScene(scene);
            detailsStage.setMinWidth(900);
            detailsStage.setMinHeight(700);

            // Afficher la fenêtre
            detailsStage.showAndWait();

        } catch (IOException e) {
            showAlert("Erreur", "Erreur lors de l'affichage des détails: " + e.getMessage());
            e.printStackTrace();
        }
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

    private void setupCalendar() {
        updateCalendar();
        loadEventsIntoCalendar();
    }

    private void updateCalendar() {
        // Mettre à jour le label du mois
        currentMonthLabel.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        
        // Nettoyer la grille existante
        calendarGrid.getChildren().clear();
        
        // Ajouter les en-têtes des jours de la semaine (déjà dans le FXML)
        
        // Obtenir le premier jour du mois
        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // Ajuster pour commencer le dimanche
        
        // Ajouter les jours du mois
        for (int i = 0; i < currentYearMonth.lengthOfMonth(); i++) {
            LocalDate date = firstOfMonth.plusDays(i);
            VBox dayCell = createDayCell(date);
            calendarGrid.add(dayCell, (dayOfWeek + i) % 7, (dayOfWeek + i) / 7 + 1);
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(2);
        cell.setStyle("-fx-background-color: #ffffff; -fx-padding: 5; -fx-background-radius: 4; -fx-border-radius: 4;");
        cell.setPrefWidth(80);
        cell.setPrefHeight(80);
        
        // Label du jour
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #2e7d32;");
        
        // Vérifier s'il y a des événements pour cette date
        Evenement event = eventMap.get(date);
        if (event != null) {
            // Créer un conteneur pour l'événement avec une couleur de fond basée sur le type
            VBox eventBox = new VBox(2);
            String backgroundColor = getEventTypeColor(event.getType().getLabel());
            eventBox.setStyle(String.format("-fx-background-color: %s; -fx-padding: 3; -fx-background-radius: 3; " +
                    "-fx-border-radius: 3; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 0);", backgroundColor));
            
            // Titre de l'événement
            Label eventLabel = new Label(event.getTitre());
            eventLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #ffffff; -fx-font-weight: bold;");
            eventLabel.setWrapText(true);
            
            // Heures de l'événement
            Label timeLabel = new Label(formatEventTime(event));
            timeLabel.setStyle("-fx-font-size: 8; -fx-text-fill: #ffffff;");
            
            // Indicateur si c'est le premier ou dernier jour
            if (date.isEqual(event.getDateDebut().toLocalDate())) {
                Label startLabel = new Label("▼ Début");
                startLabel.setStyle("-fx-font-size: 8; -fx-text-fill: #ffffff;");
                eventBox.getChildren().add(startLabel);
            }
            if (date.isEqual(event.getDateFin().toLocalDate())) {
                Label endLabel = new Label("▲ Fin");
                endLabel.setStyle("-fx-font-size: 8; -fx-text-fill: #ffffff;");
                eventBox.getChildren().add(endLabel);
            }
            
            eventBox.getChildren().addAll(eventLabel, timeLabel);
            
            // Ajouter un effet de survol
            eventBox.setOnMouseEntered(e -> {
                eventBox.setStyle(String.format("-fx-background-color: derive(%s, 10%%); -fx-padding: 3; " +
                        "-fx-background-radius: 3; -fx-border-radius: 3; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 0);", backgroundColor));
            });
            eventBox.setOnMouseExited(e -> {
                eventBox.setStyle(String.format("-fx-background-color: %s; -fx-padding: 3; -fx-background-radius: 3; " +
                        "-fx-border-radius: 3; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 0);", backgroundColor));
            });
            
            // Gestionnaire de clic
            eventBox.setOnMouseClicked(e -> showEventDetailsInPanel(event));
            
            cell.getChildren().addAll(dayLabel, eventBox);
            
            // Marquer visuellement si c'est dans l'intervalle
            if (isDateInEventInterval(date, event)) {
                cell.setStyle(cell.getStyle() + String.format("; -fx-border-color: %s; -fx-border-width: 1;", backgroundColor));
            }
        } else {
            cell.getChildren().add(dayLabel);
        }
        
        return cell;
    }

    private String formatEventTime(Evenement event) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        return event.getDateDebut().format(timeFormatter) + " - " + event.getDateFin().format(timeFormatter);
    }

    private boolean isDateInEventInterval(LocalDate date, Evenement event) {
        LocalDate startDate = event.getDateDebut().toLocalDate();
        LocalDate endDate = event.getDateFin().toLocalDate();
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    private String getEventTypeColor(String type) {
        switch (type.toLowerCase()) {
            case "foire":
                return "#2196F3"; // Bleu
            case "formation":
                return "#4CAF50"; // Vert
            case "conférence":
                return "#9C27B0"; // Violet
            default:
                return "#FF9800"; // Orange par défaut
        }
    }

    private void loadEventsIntoCalendar() {
        eventMap.clear();
        for (Evenement event : eventList) {
            LocalDate startDate = event.getDateDebut().toLocalDate();
            LocalDate endDate = event.getDateFin().toLocalDate();
            
            // Ajouter l'événement pour chaque jour de sa durée
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                eventMap.put(date, event);
            }
        }
        updateCalendar();
    }

    private void showEventDetailsInPanel(Evenement event) {
        eventTitleLabel.setText(event.getTitre());
        eventDateLabel.setText("Du " + event.getDateDebut().format(DATE_FORMATTER) + 
                             " au " + event.getDateFin().format(DATE_FORMATTER));
        eventDescriptionLabel.setText(event.getDescription());
        eventDetailsPanel.setVisible(true);
    }

    @FXML
    private void previousMonth() {
        currentYearMonth = currentYearMonth.minusMonths(1);
        updateCalendar();
    }

    @FXML
    private void nextMonth() {
        currentYearMonth = currentYearMonth.plusMonths(1);
        updateCalendar();
    }
}