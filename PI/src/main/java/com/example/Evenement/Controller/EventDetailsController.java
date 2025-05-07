package com.example.Evenement.Controller;

import com.example.Evenement.Model.Evenement;
import com.example.Evenement.Model.Region;
import com.example.Evenement.Service.WeatherService;
import com.example.auth.service.GeminiChatService;
import com.example.auth.utils.SessionManager;
import javafx.fxml.FXML;
import java.util.Optional;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

public class EventDetailsController {

    @FXML private Button backButton;
    @FXML private ImageView eventImage;
    @FXML private Label eventTitle;
    @FXML private Label eventType;
    @FXML private Label eventStatus;
    @FXML private Label startDate;
    @FXML private Label endDate;
    @FXML private Label duration;
    @FXML private TextArea descriptionText;
    @FXML private FlowPane regionsFlow;
    @FXML private VBox chatbotContainer;
    @FXML private VBox chatMessages;
    @FXML private TextField userMessage;
    @FXML private ScrollPane chatScroll;
    @FXML private Button chatbotButton;

    private Evenement currentEvent;
    private Stage stage;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");
    private WeatherService weatherService = new WeatherService();
    private GeminiChatService geminiService;
    private boolean isChatbotVisible = false;

    public void setEvent(Evenement event) {
        this.currentEvent = event;
        populateEventDetails();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        String eventContext = "You are an assistant for an event management application. Provide helpful responses related to the event details.";
        if (currentEvent != null) {
            eventContext = String.format(
                    "You are an assistant for an event called '%s' (type: %s). Description: %s. " +
                            "It starts on %s and ends on %s. Regions: %s.",
                    currentEvent.getTitre(),
                    currentEvent.getType().getLabel(),
                    currentEvent.getDescription(),
                    currentEvent.getDateDebut().format(DATE_FORMATTER),
                    currentEvent.getDateFin().format(DATE_FORMATTER),
                    String.join(", ", currentEvent.getRegions().stream().map(Region::getNom).toList())
            );
        }
        geminiService = new GeminiChatService(eventContext);
    }

    private void populateEventDetails() {
        eventTitle.setText(currentEvent.getTitre());
        eventType.setText(currentEvent.getType().getLabel());
        eventStatus.setText(currentEvent.getStatut().getLabel());
        startDate.setText(currentEvent.getDateDebut().format(DATE_FORMATTER));
        endDate.setText(currentEvent.getDateFin().format(DATE_FORMATTER));

        Duration eventDuration = Duration.between(currentEvent.getDateDebut(), currentEvent.getDateFin());
        StringBuilder durationText = new StringBuilder();
        long days = eventDuration.toDays();
        long hours = eventDuration.toHoursPart();
        long minutes = eventDuration.toMinutesPart();
        if (days > 0) durationText.append(days).append(" jour(s) ");
        if (hours > 0) durationText.append(hours).append(" heure(s) ");
        if (minutes > 0) durationText.append(minutes).append(" minute(s)");
        duration.setText(durationText.toString());

        descriptionText.setText(currentEvent.getDescription());

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

        regionsFlow.getChildren().clear();
        for (Region region : currentEvent.getRegions()) {
            Label regionLabel = new Label(region.getNom());
            regionLabel.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 5 15; -fx-background-radius: 15; " +
                    "-fx-text-fill: #333333; -fx-font-size: 12;");
            regionsFlow.getChildren().add(regionLabel);
        }
    }

    private void setDefaultImage() {
        try {
            eventImage.setImage(new Image(getClass().getResourceAsStream(
                    "/com/example/Evenement/images/default-event.jpg")));
        } catch (Exception e) {
            System.err.println("Error loading default image: " + e.getMessage());
        }
    }

    @FXML
    private void toggleChatbot() {
        isChatbotVisible = !isChatbotVisible;
        chatbotContainer.setVisible(isChatbotVisible);
        if (isChatbotVisible && chatMessages.getChildren().isEmpty()) {
            addAIMessage("Bonjour ! Je suis votre assistant pour cet événement. Comment puis-je vous aider ?");
        }
    }

    @FXML
    private void sendMessage() {
        String message = userMessage.getText().trim();
        if (!message.isEmpty()) {
            addUserMessage(message);
            userMessage.clear();

            new Thread(() -> {
                try {
                    String response = geminiService.sendMessage(message);
                    javafx.application.Platform.runLater(() -> addAIMessage(response));
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() ->
                            addAIMessage("Désolé, une erreur s'est produite. Veuillez réessayer."));
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void addUserMessage(String message) {
        Label messageLabel = new Label("Vous: " + message);
        messageLabel.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 10; -fx-padding: 8; " +
                "-fx-wrap-text: true; -fx-max-width: 400; -fx-alignment: center-right;");
        messageLabel.setMaxWidth(400);
        chatMessages.getChildren().add(messageLabel);
        scrollToBottom();
    }

    private void addAIMessage(String message) {
        Label messageLabel = new Label("Assistant: " + message);
        messageLabel.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 10; -fx-padding: 8; " +
                "-fx-wrap-text: true; -fx-max-width: 400; -fx-alignment: center-left;");
        messageLabel.setMaxWidth(400);
        chatMessages.getChildren().add(messageLabel);
        scrollToBottom();
    }

    private void scrollToBottom() {
        javafx.application.Platform.runLater(() -> {
            chatScroll.setVvalue(1.0);
        });
    }

    @FXML
    private void handleBack() {
        stage.close();
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
            inscriptionStage.setScene(new Scene(root));
            inscriptionStage.show();
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Erreur");
            error.setHeaderText(null);
            error.setContentText("Impossible d'ouvrir la page d'inscription : " + e.getMessage());
            error.showAndWait();
        }
    }

    @FXML
    private void handleWeather() {
        if (currentEvent == null || currentEvent.getRegions().isEmpty()) return;

        try {
            ChoiceDialog<Region> dialog = new ChoiceDialog<>(currentEvent.getRegions().get(0), currentEvent.getRegions());
            dialog.setTitle("Choix de la région");
            dialog.setHeaderText("Choisissez la région pour voir la météo");
            dialog.setContentText("Région :");
            dialog.getItems().setAll(currentEvent.getRegions());

            Optional<Region> result = dialog.showAndWait();
            if (result.isPresent()) {
                Region selectedRegion = result.get();
                String city = convertRegionToCity(selectedRegion.getNom());
                JSONObject weatherData = weatherService.getWeatherForDate(city, currentEvent.getDateDebut());

                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Evenement/weather-view.fxml"));
                Parent root = loader.load();
                WeatherViewController controller = loader.getController();
                Stage weatherStage = new Stage();
                controller.setStage(weatherStage);
                controller.setWeatherData(weatherData, selectedRegion.getNom(), currentEvent.getDateDebut());
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

    private String convertRegionToCity(String region) {
        switch (region.toLowerCase()) {
            case "tunis": return "Tunis,TN";
            case "ariana": return "Ariana,TN";
            case "ben arous": return "Ben Arous,TN";
            case "manouba": return "Manouba,TN";
            case "nabeul": return "Nabeul,TN";
            case "zaghouan": return "Zaghouan,TN";
            case "bizerte": return "Bizerte,TN";
            case "béja": return "Beja,TN";
            case "jendouba": return "Jendouba,TN";
            case "kef": return "Le Kef,TN";
            case "siliana": return "Siliana,TN";
            case "sousse": return "Sousse,TN";
            case "monastir": return "Monastir,TN";
            case "mahdia": return "Mahdia,TN";
            case "sfax": return "Sfax,TN";
            case "kairouan": return "Kairouan,TN";
            case "kasserine": return "Kasserine,TN";
            case "sidi bouzid": return "Sidi Bouzid,TN";
            case "gabès": return "Gabes,TN";
            case "medenine": return "Medenine,TN";
            case "tataouine": return "Tataouine,TN";
            case "gafsa": return "Gafsa,TN";
            case "tozeur": return "Tozeur,TN";
            case "kebili": return "Kebili,TN";
            default: return "Tunis,TN";
        }
    }
}