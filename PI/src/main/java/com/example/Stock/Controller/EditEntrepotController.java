package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.service.EntrepotService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public class EditEntrepotController {
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
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label titleLabel;
    @FXML private Text errorText;

    // Services
    private final EntrepotService entrepotService = new EntrepotService();
    private final BooleanProperty mapReady = new SimpleBooleanProperty(false);

    // États
    private boolean isUpdatingFields = false;
    private boolean isUpdatingMap = false;
    private Entrepot entrepot;
    private EntrepotController parentController;

    // Patterns de validation
    private static final Pattern DOUBLE_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ0-9\\s\\-']+$");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^[a-zA-ZÀ-ÿ0-9\\s\\-',.]+$");

    @FXML
    public void initialize() {
        // Configuration du WebView pour éviter les conflits
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

    public void setEntrepot(Entrepot entrepot) {
        this.entrepot = entrepot;
        populateFields();
    }

    public void setParentController(EntrepotController parentController) {
        this.parentController = parentController;
    }

    private void populateFields() {
        titleLabel.setText("Modifier Entrepôt: " + entrepot.getNom());
        nomField.setText(entrepot.getNom());
        adresseField.setText(entrepot.getAdresse());
        villeField.setText(entrepot.getVille());
        espaceField.setText(String.valueOf(entrepot.getEspace()));

        if (entrepot.getLatitude() != null) {
            latitudeField.setText(String.valueOf(entrepot.getLatitude()));
            if (entrepot.getLongitude() != null) {
                longitudeField.setText(String.valueOf(entrepot.getLongitude()));
                mapView.setLocation(entrepot.getLatitude(), entrepot.getLongitude());
                mapView.setZoom(15);
            }
        }
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

    private boolean validateInput() {
        boolean isValid = true;
        StringBuilder errors = new StringBuilder();

        // Validation du nom
        if (nomField.getText().isEmpty() || !NAME_PATTERN.matcher(nomField.getText()).matches()) {
            nomField.setStyle("-fx-border-color: red;");
            errors.append("- Le nom est obligatoire\n");
            isValid = false;
        }

        // Validation de l'adresse
        if (adresseField.getText().isEmpty() || !ADDRESS_PATTERN.matcher(adresseField.getText()).matches()) {
            adresseField.setStyle("-fx-border-color: red;");
            errors.append("- L'adresse est obligatoire\n");
            isValid = false;
        }

        // Validation de la ville
        if (villeField.getText().isEmpty() || !NAME_PATTERN.matcher(villeField.getText()).matches()) {
            villeField.setStyle("-fx-border-color: red;");
            errors.append("- La ville est obligatoire\n");
            isValid = false;
        }

        // Validation de l'espace
        if (espaceField.getText().isEmpty() || !isValidDouble(espaceField.getText())) {
            espaceField.setStyle("-fx-border-color: red;");
            errors.append("- L'espace doit être un nombre valide\n");
            isValid = false;
        } else {
            try {
                double espace = Double.parseDouble(espaceField.getText());
                if (espace <= 0) {
                    espaceField.setStyle("-fx-border-color: red;");
                    errors.append("- L'espace doit être supérieur à 0\n");
                    isValid = false;
                }
            } catch (NumberFormatException e) {
                espaceField.setStyle("-fx-border-color: red;");
                errors.append("- L'espace doit être un nombre valide\n");
                isValid = false;
            }
        }

        // Validation optionnelle des coordonnées
        if (!latitudeField.getText().isEmpty()) {
            if (!isValidDouble(latitudeField.getText()) || !isValidLatitude(Double.parseDouble(latitudeField.getText()))) {
                latitudeField.setStyle("-fx-border-color: red;");
                errors.append("- La latitude doit être entre -90 et 90\n");
                isValid = false;
            }
        }

        if (!longitudeField.getText().isEmpty()) {
            if (!isValidDouble(longitudeField.getText()) || !isValidLongitude(Double.parseDouble(longitudeField.getText()))) {
                longitudeField.setStyle("-fx-border-color: red;");
                errors.append("- La longitude doit être entre -180 et 180\n");
                isValid = false;
            }
        }

        if (!isValid) {
            showAlert("Erreur de validation", "Veuillez corriger les erreurs suivantes:\n", Alert.AlertType.ERROR);
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

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            showError("Veuillez corriger les erreurs dans le formulaire");
            return;
        }

        try {
            // Mettre à jour l'objet Entrepot
            entrepot.setNom(nomField.getText());
            entrepot.setAdresse(adresseField.getText());
            entrepot.setVille(villeField.getText());
            entrepot.setEspace(Double.parseDouble(espaceField.getText()));

            if (!latitudeField.getText().isEmpty()) {
                entrepot.setLatitude(Double.parseDouble(latitudeField.getText()));
            } else {
                entrepot.setLatitude(null);
            }

            if (!longitudeField.getText().isEmpty()) {
                entrepot.setLongitude(Double.parseDouble(longitudeField.getText()));
            } else {
                entrepot.setLongitude(null);
            }

            // Sauvegarder dans la base de données
            boolean success = entrepotService.updateEntrepot(entrepot);

            if (success) {
                showAlert("Succès", "Entrepôt mis à jour avec succès", Alert.AlertType.INFORMATION);
                if (parentController != null) {
                    parentController.refreshEntrepotData();
                }
                closeWindow();
            } else {
                showAlert("Erreur", "Échec de la mise à jour de l'entrepôt", Alert.AlertType.ERROR);
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur", "Valeur numérique invalide", Alert.AlertType.ERROR);
        } catch (Exception e) {
            showAlert("Erreur", "Erreur inattendue: " + e.getMessage(), Alert.AlertType.ERROR);
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
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }
}