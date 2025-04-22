package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.service.EntrepotService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static javafx.util.Duration.seconds;



public class AddEntrepotController {
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField espaceField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;
    @FXML private Button searchLocationBtn;
    @FXML private StackPane mapContainer;
    @FXML private MapView mapView;
    @FXML private ComboBox<String> mapTypeSelector;


    private EntrepotController parentController;
    private EntrepotService entrepotService = new EntrepotService();
    private final OkHttpClient httpClient = new OkHttpClient();
    private final BooleanProperty mapReady = new SimpleBooleanProperty(false);


    @FXML
    public void initialize() {
        configureMapContainer();
        setupMapBindings();
        setupMapListeners();
        setupMapResizing();

        mapTypeSelector.getItems().addAll("OpenStreetMap", "Satellite", "Terrain");
        mapTypeSelector.getSelectionModel().selectFirst();
        mapView.setMapType("OpenStreetMap");


        mapContainer.setVisible(true);
        mapContainer.setManaged(true);

        mapTypeSelector.setVisible(false);
        setupMapResizing();

        // Écouteur pour le changement de type de carte
        mapTypeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mapView != null) {
                mapView.setMapType(newVal);
            }
        });

        // Écouteur pour le chargement de la carte
        mapView.mapReadyProperty().addListener((obs, oldVal, ready) -> {
            if (ready) {
                PauseTransition pt = new PauseTransition(Duration.millis(500));
                pt.setOnFinished(e -> {
                    mapView.setMapType("OpenStreetMap");
                    mapView.scheduleResize();
                });
                pt.play();
            }
        });

        // Lier les champs de texte à la carte
        setupLocationBindings();

        searchLocationBtn.setOnAction(e -> searchLocation());
        double lat = mapView.getLatitude();
        double lng = mapView.getLongitude();


    }

    private void setupLocationBindings() {
        // Quand la carte change, mettre à jour les champs
        mapView.latitudeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() != 0.0) {
                Platform.runLater(() -> {
                    latitudeField.setText(String.format("%.6f", newVal));
                });
            }
        });

        mapView.longitudeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() != 0.0) {
                Platform.runLater(() -> {
                    longitudeField.setText(String.format("%.6f", newVal));
                });
            }
        });

        // Quand les champs changent manuellement, mettre à jour la carte
        latitudeField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double lat = Double.parseDouble(newVal);
                double lng = mapView.getLongitude();
                if (mapView.getLatitude() != lat) {
                    mapView.setLocation(lat, lng);
                }
            } catch (NumberFormatException ignored) {}
        });

        longitudeField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double lng = Double.parseDouble(newVal);
                double lat = mapView.getLatitude();
                if (mapView.getLongitude() != lng) {
                    mapView.setLocation(lat, lng);
                }
            } catch (NumberFormatException ignored) {}
        });
    }

    @FXML
    private void searchLocation() {
        String adresse = adresseField.getText().trim();
        String ville = villeField.getText().trim();

        if(adresse.isEmpty() || ville.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir l'adresse et la ville", Alert.AlertType.ERROR);
            return;
        }

        Platform.runLater(() -> {
            mapContainer.setVisible(true);
            PauseTransition pause = new PauseTransition(seconds(0.5));
            pause.setOnFinished(e -> executeLocationSearch(adresse, ville));
            pause.play();
        });
    }

    private void configureMapContainer() {
        mapView.prefWidthProperty().bind(mapContainer.widthProperty());
        mapView.prefHeightProperty().bind(mapContainer.heightProperty());
    }

    private void setupMapBindings() {
        latitudeField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double lat = Double.parseDouble(newVal);
                if(mapView.getLatitude() != lat) {
                    mapView.setLocation(lat, mapView.getLongitude());
                }
            } catch (NumberFormatException ignored) {}
        });

        longitudeField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double lng = Double.parseDouble(newVal);
                if(mapView.getLongitude() != lng) {
                    mapView.setLocation(mapView.getLatitude(), lng);
                }
            } catch (NumberFormatException ignored) {}
        });
    }
    private void setupMapListeners() {
        mapView.mapReadyProperty().addListener((obs, oldVal, isReady) -> {
            if (isReady) {
                PauseTransition pt = new PauseTransition(Duration.millis(300));
                pt.setOnFinished(e -> {
                    mapView.setStyle("-fx-opacity: 1;");
                    mapView.setMapType("OpenStreetMap");
                });
                pt.play();
            }
        });
    }

    private void geocodeAddress(String address, String city) {
        String query = URLEncoder.encode(address + ", " + city, StandardCharsets.UTF_8);
        String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + query;

        // Implémentez la logique de géocodage ici
    }
    private void setupMapResizing() {
        // La carte s'adapte automatiquement au conteneurse
        mapView.prefWidthProperty().bind(mapContainer.widthProperty());
        mapView.prefHeightProperty().bind(mapContainer.heightProperty());
    }

    private void executeLocationSearch(String adresse, String ville) {
        try {
            String query = URLEncoder.encode(adresse + ", " + ville, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + query;

            HttpClient.Builder builder = HttpClient.newBuilder();
            HttpClient client = builder
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "JavaFX Map Application")
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(responseBody -> {
                        try {
                            JSONArray results = new JSONArray(responseBody);
                            if (results.length() > 0) {
                                JSONObject result = results.getJSONObject(0);
                                double lat = Double.parseDouble(result.getString("lat"));
                                double lon = Double.parseDouble(result.getString("lon"));

                                Platform.runLater(() -> {
                                    mapView.setLocation(lat, lon);
                                    mapView.setZoom(15);
                                    // No need to set text fields as the bindings will handle it
                                });
                            } else {
                                Platform.runLater(() ->
                                        showAlert("Aucun résultat",
                                                "Aucun emplacement trouvé pour cette adresse",
                                                Alert.AlertType.INFORMATION)
                                );
                            }
                        } catch (Exception e) {
                            Platform.runLater(() ->
                                    showAlert("Erreur",
                                            "Erreur lors du traitement des résultats: " + e.getMessage(),
                                            Alert.AlertType.ERROR)
                            );
                        }
                    })
                    .exceptionally(e -> {
                        Platform.runLater(() ->
                                showAlert("Erreur",
                                        "Impossible de se connecter au service de géocodage: " + e.getMessage(),
                                        Alert.AlertType.ERROR)
                        );
                        return null;
                    });
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de la recherche: " + e.getMessage(), Alert.AlertType.ERROR);
        }



    }

    @FXML
    private void handleSave() {
        try {
            // Validation des champs obligatoires
            if (nomField.getText().isEmpty() || adresseField.getText().isEmpty() ||
                    villeField.getText().isEmpty() || espaceField.getText().isEmpty()) {
                showAlert("Erreur", "Veuillez remplir tous les champs obligatoires", Alert.AlertType.ERROR);
                return;
            }

            Entrepot newEntrepot = new Entrepot();
            newEntrepot.setNom(nomField.getText());
            newEntrepot.setAdresse(adresseField.getText());
            newEntrepot.setVille(villeField.getText());
            newEntrepot.setEspace(Double.parseDouble(espaceField.getText()));

            // Récupérer les coordonnées seulement si la carte est visible
            if (mapContainer.isVisible()) {
                try {
                    newEntrepot.setLatitude(Double.parseDouble(latitudeField.getText()));
                    newEntrepot.setLongitude(Double.parseDouble(longitudeField.getText()));
                } catch (NumberFormatException e) {
                    showAlert("Erreur", "Coordonnées GPS invalides", Alert.AlertType.ERROR);
                    return;
                }
            }

            if (entrepotService.saveEntrepot(newEntrepot)) {
                if (parentController != null) {

                }
                closeWindow();
            } else {
                showAlert("Erreur", "Échec de la sauvegarde", Alert.AlertType.ERROR);
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Veuillez entrer une valeur valide pour l'espace", Alert.AlertType.ERROR);
        }
    }

    public void setParentController(EntrepotController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeWindow() {
        nomField.getScene().getWindow().hide();
    }

}