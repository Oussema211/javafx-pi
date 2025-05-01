package com.example.Evenement.Controller;

import com.example.Evenement.Model.Evenement;
import com.example.Evenement.Model.Region;
import com.example.Evenement.Service.GoogleCalendarService;
import com.example.auth.utils.SessionManager;
import com.example.auth.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.scene.Parent;
import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import com.example.Evenement.Service.WeatherService;
import org.json.JSONObject;
import java.util.Optional;

public class EventDetailsController {

    @FXML private Button backButton;
    @FXML private ImageView eventImage;
    @FXML private Label eventTitle;
    @FXML private Label eventType;
    @FXML private Label eventStatus;
    @FXML private Label startDate;
    @FXML private Label endDate;
    @FXML private Label duration;
    @FXML private Text descriptionText;
    @FXML private FlowPane regionsFlow;

    private Evenement currentEvent;
    private Stage stage;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");
    private GoogleCalendarService googleCalendarService;
    private SessionManager sessionManager = SessionManager.getInstance();
    private WeatherService weatherService = new WeatherService();

    public void setEvent(Evenement event) {
        this.currentEvent = event;
        populateEventDetails();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        // Suppression de l'initialisation du ToggleGroup et des listeners liés à l'inscription
    }

    private void populateEventDetails() {
        // Titre
        eventTitle.setText(currentEvent.getTitre());

        // Type et Statut
        eventType.setText(currentEvent.getType().getLabel());
        eventStatus.setText(currentEvent.getStatut().getLabel());

        // Dates
        startDate.setText(currentEvent.getDateDebut().format(DATE_FORMATTER));
        endDate.setText(currentEvent.getDateFin().format(DATE_FORMATTER));

        // Durée
        Duration eventDuration = Duration.between(currentEvent.getDateDebut(), currentEvent.getDateFin());
        long days = eventDuration.toDays();
        long hours = eventDuration.toHoursPart();
        long minutes = eventDuration.toMinutesPart();
        
        StringBuilder durationText = new StringBuilder();
        if (days > 0) durationText.append(days).append(" jour(s) ");
        if (hours > 0) durationText.append(hours).append(" heure(s) ");
        if (minutes > 0) durationText.append(minutes).append(" minute(s)");
        duration.setText(durationText.toString());

        // Description
        descriptionText.setText(currentEvent.getDescription());

        // Image
        if (currentEvent.getPhotoPath() != null && !currentEvent.getPhotoPath().isEmpty()) {
            File file = new File(currentEvent.getPhotoPath());
            if (file.exists()) {
                eventImage.setImage(new Image(file.toURI().toString()));
            } else {
                setDefaultImage();
            }
        } else {
            setDefaultImage();
        }

        // Régions
        regionsFlow.getChildren().clear();
        for (Region region : currentEvent.getRegions()) {
            Label regionLabel = new Label(region.getNom());
            regionLabel.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 5 15; -fx-background-radius: 15; " +
                               "-fx-text-fill: #333333; -fx-font-size: 12;");
            regionsFlow.getChildren().add(regionLabel);
        }
    }

    private void setDefaultImage() {
        eventImage.setImage(new Image(getClass().getResourceAsStream(
                "/com/example/Evenement/images/default-event.jpg")));
    }

    @FXML
    private void handleBack() {
        stage.close();
    }

    @FXML
    private void handleEdit() {
        // TODO: Implémenter la modification de l'événement
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Modification");
        alert.setHeaderText(null);
        alert.setContentText("Fonctionnalité de modification à venir");
        alert.showAndWait();
    }

    @FXML
    private void handleDelete() {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation de suppression");
        confirmation.setHeaderText("Êtes-vous sûr de vouloir supprimer cet événement ?");
        confirmation.setContentText("Cette action est irréversible.");

        if (confirmation.showAndWait().get() == ButtonType.OK) {
            // TODO: Implémenter la suppression de l'événement
            stage.close();
        }
    }

    @FXML
    private void handleAddToGoogleCalendar() {
        try {
            if (googleCalendarService == null) {
                googleCalendarService = new GoogleCalendarService();
            }
            
            googleCalendarService.createEvent(
                currentEvent.getTitre(),
                currentEvent.getDescription(),
                currentEvent.getDateDebut(),
                currentEvent.getDateFin()
            );

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Succès");
            success.setHeaderText(null);
            success.setContentText("L'événement a été ajouté à votre calendrier Google avec succès !");
            success.showAndWait();

        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText(null);
            error.setContentText("Erreur lors de l'ajout à Google Calendar : " + e.getMessage());
            error.showAndWait();
        }
    }

    @FXML
    private void handleWeather() {
        if (currentEvent == null || currentEvent.getRegions().isEmpty()) return;

        try {
            // Créer une boîte de dialogue pour choisir la région
            ChoiceDialog<Region> dialog = new ChoiceDialog<>(
                currentEvent.getRegions().get(0),
                currentEvent.getRegions()
            );
            dialog.setTitle("Choix de la région");
            dialog.setHeaderText("Choisissez la région pour voir la météo");
            dialog.setContentText("Région :");

            // Personnaliser l'affichage des régions dans la liste
            dialog.getItems().setAll(currentEvent.getRegions());
            
            // Attendre le choix de l'utilisateur
            Optional<Region> result = dialog.showAndWait();
            
            if (result.isPresent()) {
                Region selectedRegion = result.get();
                // Convertir le nom de la région en nom de ville pour l'API météo
                String city = convertRegionToCity(selectedRegion.getNom());

                // Obtenir les données météo
                JSONObject weatherData = weatherService.getWeatherForDate(city, currentEvent.getDateDebut());

                // Charger la vue météo
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/weather-view.fxml"));
                Parent root = loader.load();

                // Configurer le contrôleur
                WeatherViewController controller = loader.getController();
                Stage weatherStage = new Stage();
                controller.setStage(weatherStage);
                controller.setWeatherData(weatherData, selectedRegion.getNom(), currentEvent.getDateDebut());

                // Configurer et afficher la fenêtre
                weatherStage.setTitle("Météo pour " + selectedRegion.getNom());
                weatherStage.setScene(new Scene(root));
                weatherStage.show();
            }
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText(null);
            error.setContentText("Erreur lors de la récupération de la météo : " + e.getMessage());
            error.showAndWait();
        }
    }

    // Méthode pour convertir le nom de la région en nom de ville pour l'API météo
    private String convertRegionToCity(String region) {
        // Correspondance des régions avec les principales villes
        switch (region.toLowerCase()) {
            case "tunis":
                return "Tunis,TN";
            case "ariana":
                return "Ariana,TN";
            case "ben arous":
                return "Ben Arous,TN";
            case "manouba":
                return "Manouba,TN";
            case "nabeul":
                return "Nabeul,TN";
            case "zaghouan":
                return "Zaghouan,TN";
            case "bizerte":
                return "Bizerte,TN";
            case "béja":
                return "Beja,TN";
            case "jendouba":
                return "Jendouba,TN";
            case "kef":
                return "Le Kef,TN";
            case "siliana":
                return "Siliana,TN";
            case "sousse":
                return "Sousse,TN";
            case "monastir":
                return "Monastir,TN";
            case "mahdia":
                return "Mahdia,TN";
            case "sfax":
                return "Sfax,TN";
            case "kairouan":
                return "Kairouan,TN";
            case "kasserine":
                return "Kasserine,TN";
            case "sidi bouzid":
                return "Sidi Bouzid,TN";
            case "gabès":
                return "Gabes,TN";
            case "medenine":
                return "Medenine,TN";
            case "tataouine":
                return "Tataouine,TN";
            case "gafsa":
                return "Gafsa,TN";
            case "tozeur":
                return "Tozeur,TN";
            case "kebili":
                return "Kebili,TN";
            default:
                return "Tunis,TN"; // Par défaut, utiliser Tunis
        }
    }

    @FXML
    private void handleInscription() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/event-inscription.fxml"));
            Stage inscriptionStage = new Stage();
            inscriptionStage.setTitle("Inscription à l'événement");
            
            Parent root = loader.load();
            EventInscriptionController controller = loader.getController();
            controller.setEvent(currentEvent);
            controller.setStage(inscriptionStage);
            
            inscriptionStage.setScene(new javafx.scene.Scene(root));
            inscriptionStage.show();
            
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText(null);
            error.setContentText("Impossible d'ouvrir la page d'inscription : " + e.getMessage());
            error.showAndWait();
        }
    }
} 