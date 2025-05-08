package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.service.EntrepotService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class AddEntrepotController {
    // Champs du formulaire
    @FXML private TextField nomField;
    @FXML private TextField adresseField;
    @FXML private TextField villeField;
    @FXML private TextField espaceField;
    @FXML private TextField latitudeField;
    @FXML private TextField longitudeField;

    // Éléments UI
    @FXML private Button searchLocationBtn;
    @FXML private StackPane mapContainer;
    @FXML private MapView mapView;
    @FXML private ComboBox<String> mapTypeSelector;
    @FXML private Button zoomMaxButton;
    @FXML private Button saveBtn;  // Correspond à fx:id="saveBtn" dans FXML
    @FXML private Button cancelBtn; // Correspond à fx:id="cancelBtn" dans FXML
    @FXML private Text errorText;
    @FXML private Button describe3DButton;

    // Services
    private final EntrepotService entrepotService = new EntrepotService();
    private final BooleanProperty mapReady = new SimpleBooleanProperty(false);

    private EntrepotController parentController;

    // États
    private boolean isUpdatingFields = false;
    private boolean isUpdatingMap = false;

    // Patterns de validation
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ0-9\\s\\-']+$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ0-9\\s\\-',.]+$");

    @FXML
    public void initialize() {
        // Configuration du WebView pour éviter les conflirs
        System.setProperty("javafx.webview.userDataDir",
                System.getProperty("java.io.tmpdir") + "/webview_" + System.currentTimeMillis());

        if (mapView == null) {
            showError("Erreur: MapView n'est pas initialisée correctement!");
            return;
        }

        configureMap();
        setupBindings();
        setupListeners();
        setupValidations();
    }

    private void configureMap() {
        mapTypeSelector.getItems().addAll("OpenStreetMap", "Satellite", "Terrain");
        mapTypeSelector.getSelectionModel().selectFirst();
        mapView.setMapType("OpenStreetMap");
    }

    private void setupBindings() {
        // Liaison des champs latitude/longitude avec la carte
        latitudeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFields && !newVal.isEmpty() && isValidDouble(newVal)) {
                try {
                    double lat = Double.parseDouble(newVal);
                    if (isValidLatitude(lat)) {
                        isUpdatingMap = true;
                        mapView.setLocation(lat, mapView.getLongitude());
                        isUpdatingMap = false;
                        clearError();
                    } else {
                        showError("Latitude doit être entre -90 et 90");
                    }
                } catch (NumberFormatException ignored) {
                    showError("Format de latitude invalide");
                }
            }
        });

        longitudeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFields && !newVal.isEmpty() && isValidDouble(newVal)) {
                try {
                    double lng = Double.parseDouble(newVal);
                    if (isValidLongitude(lng)) {
                        isUpdatingMap = true;
                        mapView.setLocation(mapView.getLatitude(), lng);
                        isUpdatingMap = false;
                        clearError();
                    } else {
                        showError("Longitude doit être entre -180 et 180");
                    }
                } catch (NumberFormatException ignored) {
                    showError("Format de longitude invalide");
                }
            }
        });

        // Mise à jour des champs depuis la carte
        mapView.latitudeProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFields && newVal != null) {
                isUpdatingFields = true;
                latitudeField.setText(String.format(Locale.US, "%.6f", newVal));
                isUpdatingFields = false;
            }
        });

        mapView.longitudeProperty().addListener((obs, oldVal, newVal) -> {
            if (!isUpdatingFields && newVal != null) {
                isUpdatingFields = true;
                longitudeField.setText(String.format(Locale.US, "%.6f", newVal));
                isUpdatingFields = false;
            }
        });
    }

    private void setupListeners() {
        // Changement de type de carte
        mapTypeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mapView != null) {
                mapView.setMapType(newVal);
            }
        });

        // Prêt de la carte
        mapView.mapReadyProperty().addListener((obs, oldVal, isReady) -> {
            if (isReady) {
                Platform.runLater(() -> mapContainer.setVisible(true));
            }
        });

        // Boutons
        searchLocationBtn.setOnAction(e -> searchLocation());
        zoomMaxButton.setOnAction(e -> mapView.setZoom(18));
        saveBtn.setOnAction(e -> handleSave());
        cancelBtn.setOnAction(e -> handleCancel());
    }

    private void setupValidations() {
        // Validation en temps réel
        nomField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !NAME_PATTERN.matcher(newVal).matches()) {
                nomField.setStyle("-fx-border-color: red;");
            } else {
                nomField.setStyle("");
            }
        });

        adresseField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !ADDRESS_PATTERN.matcher(newVal).matches()) {
                adresseField.setStyle("-fx-border-color: red;");
            } else {
                adresseField.setStyle("");
            }
        });

        villeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !NAME_PATTERN.matcher(newVal).matches()) {
                villeField.setStyle("-fx-border-color: red;");
            } else {
                villeField.setStyle("");
            }
        });

        espaceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !isValidDouble(newVal)) {
                espaceField.setStyle("-fx-border-color: red;");
            } else {
                espaceField.setStyle("");
            }
        });
    }

    // Méthodes de validation
    private boolean isValidDouble(String value) {
        return DOUBLE_PATTERN.matcher(value).matches();
    }

    private boolean isValidLatitude(double lat) {
        return lat >= -90 && lat <= 90;
    }

    private boolean isValidLongitude(double lng) {
        return lng >= -180 && lng <= 180;
    }

    private boolean validateAllFields() {
        boolean isValid = true;

        // Validation du nom
        if (nomField.getText().isEmpty() || !NAME_PATTERN.matcher(nomField.getText()).matches()) {
            nomField.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        // Validation de l'adresse
        if (adresseField.getText().isEmpty() || !ADDRESS_PATTERN.matcher(adresseField.getText()).matches()) {
            adresseField.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        // Validation de la ville
        if (villeField.getText().isEmpty() || !NAME_PATTERN.matcher(villeField.getText()).matches()) {
            villeField.setStyle("-fx-border-color: red;");
            isValid = false;
        }

        // Validation de l'espace
        if (espaceField.getText().isEmpty() || !isValidDouble(espaceField.getText())) {
            espaceField.setStyle("-fx-border-color: red;");
            isValid = false;
        } else {
            try {
                double espace = Double.parseDouble(espaceField.getText());
                if (espace <= 0) {
                    espaceField.setStyle("-fx-border-color: red;");
                    showError("L'espace doit être supérieur à 0");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                espaceField.setStyle("-fx-border-color: red;");
                isValid = false;
            }
        }

        // Validation optionnelle des coordonnées
        if (!latitudeField.getText().isEmpty()) {
            if (!isValidDouble(latitudeField.getText()) || !isValidLatitude(Double.parseDouble(latitudeField.getText()))) {
                latitudeField.setStyle("-fx-border-color: red;");
                isValid = false;
            }
        }

        if (!longitudeField.getText().isEmpty()) {
            if (!isValidDouble(longitudeField.getText()) || !isValidLongitude(Double.parseDouble(longitudeField.getText()))) {
                longitudeField.setStyle("-fx-border-color: red;");
                isValid = false;
            }
        }

        return isValid;
    }

    @FXML
    private void searchLocation() {
        String adresse = adresseField.getText().trim();
        String ville = villeField.getText().trim();

        if (adresse.isEmpty() || ville.isEmpty()) {
            showAlert("Erreur", "Veuillez remplir l'adresse et la ville", Alert.AlertType.ERROR);
            return;
        }

        if (!ADDRESS_PATTERN.matcher(adresse).matches()) {
            showAlert("Erreur", "Adresse contient des caractères invalides", Alert.AlertType.ERROR);
            return;
        }

        if (!NAME_PATTERN.matcher(ville).matches()) {
            showAlert("Erreur", "Nom de ville invalide", Alert.AlertType.ERROR);
            return;
        }

        geocodeAddress(adresse, ville);
    }

    private void geocodeAddress(String address, String city) {
        try {
            String query = java.net.URLEncoder.encode(address + ", " + city, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=json&q=" + query
                    + "&countrycodes=tn&limit=1";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "StockManagement/1.0")
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(response -> processGeocodeResponse(response))
                    .exceptionally(e -> {
                        Platform.runLater(() -> {
                            showAlert("Erreur", "Échec de la recherche: " + e.getMessage(), Alert.AlertType.ERROR);
                            showError("Erreur lors de la recherche de localisation");
                        });
                        return null;
                    });
        } catch (Exception e) {
            Platform.runLater(() -> {
                showAlert("Erreur", "Erreur lors de la recherche: " + e.getMessage(), Alert.AlertType.ERROR);
                showError("Erreur lors de la recherche de localisation");
            });
        }
    }

    private void processGeocodeResponse(String response) {
        try {
            JSONArray results = new JSONArray(response);
            if (results.length() > 0) {
                JSONObject location = results.getJSONObject(0);
                double lat = location.getDouble("lat");
                double lon = location.getDouble("lon");

                Platform.runLater(() -> {
                    isUpdatingFields = true;
                    latitudeField.setText(String.format(Locale.US, "%.6f", lat));
                    longitudeField.setText(String.format(Locale.US, "%.6f", lon));
                    isUpdatingFields = false;

                    mapView.setLocation(lat, lon);
                    mapView.setZoom(15);
                    clearError();
                });
            } else {
                Platform.runLater(() -> {
                    showAlert("Erreur", "Aucun résultat trouvé pour l'adresse", Alert.AlertType.ERROR);
                    showError("Aucun résultat trouvé pour l'adresse");
                });
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                showAlert("Erreur", "Erreur de traitement: " + e.getMessage(), Alert.AlertType.ERROR);
                showError("Erreur lors du traitement des résultats");
            });
        }
    }

    public void setParentController(EntrepotController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleSave() {
        if (validateAllFields()) {
            Entrepot newEntrepot = new Entrepot();
            newEntrepot.setId(UUID.randomUUID());
            newEntrepot.setNom(nomField.getText());
            newEntrepot.setAdresse(adresseField.getText());
            newEntrepot.setVille(villeField.getText());
            newEntrepot.setEspace(Double.parseDouble(espaceField.getText()));

            if (!latitudeField.getText().isEmpty()) {
                newEntrepot.setLatitude(Double.parseDouble(latitudeField.getText()));
            }

            if (!longitudeField.getText().isEmpty()) {
                newEntrepot.setLongitude(Double.parseDouble(longitudeField.getText()));
            }

            boolean success = entrepotService.addEntrepot(newEntrepot);

            if (success) {
                if (parentController != null) {
                    parentController.refreshEntrepotData();
                }
                closeWindow();
            } else {
                showAlert("Erreur", "Échec de l'ajout du nouvel entrepôt", Alert.AlertType.ERROR);
            }
        }
    }
    @FXML
    private void navigateToDescriptionPage() {
        try {
            // Charge la nouvelle vue
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/Entrepot/view/Description3D.fxml"));

            // Configure la nouvelle scène
            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) describe3DButton).getScene().getWindow();
            stage.setScene(scene);

        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la page de description 3D", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }




    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showError(String message) {
        errorText.setText(message);
        errorText.setFill(Color.RED);
        errorText.setVisible(true);
    }

    private void clearError() {
        errorText.setText("");
        errorText.setVisible(false);
    }

    private void closeWindow() {
        nomField.getScene().getWindow().hide();
    }


}