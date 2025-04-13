package com.example.Evenement.Controller;
import com.example.Evenement.Dao.EvenementDAO;
import com.example.Evenement.Dao.RegionDAO;
import com.example.Evenement.Model.Evenement;
import com.example.Evenement.Model.Region;
import com.example.Evenement.Model.TypeEvenement;
import com.example.Evenement.Model.StatutEvenement;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class EvenementController {
    // Composants FXML
    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<TypeEvenement> typeCombo;
    @FXML private ComboBox<StatutEvenement> statutCombo;
    @FXML private DatePicker dateDebutPicker;
    @FXML private Spinner<Integer> heureDebutSpinner;
    @FXML private DatePicker dateFinPicker;
    @FXML private Spinner<Integer> heureFinSpinner;
    @FXML private ImageView photoView;
    @FXML private TextField regionSearchField;
    @FXML private ListView<Region> selectedRegionsListView;
    @FXML private Button addRegionBtn;
    @FXML private Button removeRegionBtn;
    @FXML private Button uploadPhotoBtn;
    @FXML private Button saveBtn;

    // Données
    private Evenement currentEvent;
    private String photoPath;
    private final ObservableList<Region> allRegions = FXCollections.observableArrayList();
    private final ObservableList<Region> selectedRegions = FXCollections.observableArrayList();
    private FilteredList<Region> filteredRegions;
    private ContextMenu regionSuggestionsMenu;

    @FXML
    public void initialize() {
        configureComboBoxes();
        configureTimeSpinners();
        loadAllRegions();
        setupRegionsSelection();
    }

    private void configureComboBoxes() {
        typeCombo.getItems().setAll(TypeEvenement.values());
        statutCombo.getItems().setAll(StatutEvenement.values());
    }

    private void configureTimeSpinners() {
        heureDebutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 8));
        heureFinSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 18));
    }

    private void loadAllRegions() {
        try {
            allRegions.clear();
            allRegions.addAll(new RegionDAO().getAllRegions());
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des régions: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupRegionsSelection() {
        // Configurer la liste des régions sélectionnées
        selectedRegionsListView.setItems(selectedRegions);
        selectedRegionsListView.setCellFactory(lv -> new ListCell<Region>() {
            @Override
            protected void updateItem(Region item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getNom() + " (" + item.getVille() + ")");
            }
        });

        // Configurer la recherche de régions
        filteredRegions = new FilteredList<>(allRegions, s -> true);

        // Créer le menu contextuel pour les suggestions
        regionSuggestionsMenu = new ContextMenu();
        regionSuggestionsMenu.setAutoHide(true);

        // Ajouter un écouteur pour le champ de recherche
        regionSearchField.textProperty().addListener((obs, oldText, newText) -> {
            updateRegionSuggestions(newText);
        });

        // Ajouter un écouteur pour la touche Entrée dans le champ de recherche
        regionSearchField.setOnAction(event -> {
            addSelectedRegion();
        });

        // Ajouter un écouteur pour la touche Tab dans le champ de recherche
        regionSearchField.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.TAB) {
                if (!regionSuggestionsMenu.getItems().isEmpty()) {
                    regionSuggestionsMenu.getItems().get(0).fire();
                    event.consume();
                }
            }
        });
    }

    private void updateRegionSuggestions(String searchText) {
        regionSuggestionsMenu.getItems().clear();
        
        if (searchText == null || searchText.isEmpty()) {
            regionSuggestionsMenu.hide();
            return;
        }

        String lowerCaseFilter = searchText.toLowerCase();
        List<Region> matchingRegions = allRegions.stream()
            .filter(region -> region.getNom().toLowerCase().contains(lowerCaseFilter) || 
                             region.getVille().toLowerCase().contains(lowerCaseFilter))
            .collect(Collectors.toList());

        if (matchingRegions.isEmpty()) {
            regionSuggestionsMenu.hide();
            return;
        }

        // Limiter à 5 suggestions maximum
        int count = 0;
        for (Region region : matchingRegions) {
            if (count >= 5) break;
            
            MenuItem item = new MenuItem(region.getNom() + " (" + region.getVille() + ")");
            item.setOnAction(e -> {
                regionSearchField.setText(region.getNom() + " (" + region.getVille() + ")");
                addRegion(region);
            });
            regionSuggestionsMenu.getItems().add(item);
            count++;
        }

        // Afficher le menu sous le champ de recherche
        regionSuggestionsMenu.show(regionSearchField, 
            javafx.geometry.Side.BOTTOM, 
            0, 
            0);
    }

    private void addSelectedRegion() {
        String searchText = regionSearchField.getText().toLowerCase();
        if (!searchText.isEmpty()) {
            // Trouver la première région correspondante
            Region matchingRegion = allRegions.stream()
                .filter(region -> region.getNom().toLowerCase().contains(searchText) || 
                                 region.getVille().toLowerCase().contains(searchText))
                .findFirst()
                .orElse(null);
            
            if (matchingRegion != null) {
                addRegion(matchingRegion);
            }
        }
    }

    private void addRegion(Region region) {
        if (region != null && !selectedRegions.contains(region)) {
            selectedRegions.add(region);
            regionSearchField.clear();
            selectedRegionsListView.refresh();
        }
    }

    @FXML
    private void handleAddRegion() {
        addSelectedRegion();
    }

    @FXML
    private void handleRemoveRegion() {
        Region selectedRegion = selectedRegionsListView.getSelectionModel().getSelectedItem();
        if (selectedRegion != null) {
            selectedRegions.remove(selectedRegion);
            selectedRegionsListView.refresh();
        }
    }

    @FXML
    private void handleShowRegions() {
        try {
            // Charger la vue de la liste des régions
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Region/RegionList.fxml"));
            Parent root = loader.load();
            
            // Créer une nouvelle fenêtre pour afficher la liste des régions
            Stage stage = new Stage();
            stage.setTitle("Liste des régions");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir la liste des régions: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    public void setEvenement(Evenement event) {
        this.currentEvent = event;
        if (event != null) {
            populateFormFields(event);
        }
    }

    private void populateFormFields(Evenement event) {
        titreField.setText(event.getTitre());
        descriptionArea.setText(event.getDescription());
        typeCombo.setValue(event.getType());
        statutCombo.setValue(event.getStatut());

        setDateTimeFields(event);
        setPhoto(event);
        setSelectedRegions(event);
    }

    private void setDateTimeFields(Evenement event) {
        dateDebutPicker.setValue(event.getDateDebut().toLocalDate());
        heureDebutSpinner.getValueFactory().setValue(event.getDateDebut().getHour());
        dateFinPicker.setValue(event.getDateFin().toLocalDate());
        heureFinSpinner.getValueFactory().setValue(event.getDateFin().getHour());
    }

    private void setPhoto(Evenement event) {
        if (event.getPhotoPath() != null && !event.getPhotoPath().isEmpty()) {
            photoPath = event.getPhotoPath();
            photoView.setImage(new Image(new File(photoPath).toURI().toString()));
        }
    }

    private void setSelectedRegions(Evenement event) {
        selectedRegions.clear();
        selectedRegions.addAll(event.getRegions());
    }

    @FXML
    private void handleUploadPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.gif")
        );

        File file = fileChooser.showOpenDialog(uploadPhotoBtn.getScene().getWindow());
        if (file != null) {
            photoPath = file.getAbsolutePath();
            photoView.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleSave() {
        if (validateForm()) {
            try {
                prepareEventForSave();
                saveEvent();
                showAlert("Succès", "Événement sauvegardé avec succès", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de la sauvegarde: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void prepareEventForSave() {
        if (currentEvent == null) {
            currentEvent = new Evenement();
        }

        currentEvent.setTitre(titreField.getText());
        currentEvent.setDescription(descriptionArea.getText());
        currentEvent.setType(typeCombo.getValue());
        currentEvent.setStatut(statutCombo.getValue());
        currentEvent.setDateDebut(getDateTimeFromFields(dateDebutPicker, heureDebutSpinner));
        currentEvent.setDateFin(getDateTimeFromFields(dateFinPicker, heureFinSpinner));
        currentEvent.setPhotoPath(photoPath);

        updateEventRegions();
    }

    private LocalDateTime getDateTimeFromFields(DatePicker datePicker, Spinner<Integer> hourSpinner) {
        return LocalDateTime.of(
                datePicker.getValue(),
                LocalTime.of(hourSpinner.getValue(), 0)
        );
    }

    private void updateEventRegions() {
        currentEvent.getRegions().clear();
        currentEvent.getRegions().addAll(selectedRegions);
    }

    private void saveEvent() throws Exception {
        EvenementDAO dao = new EvenementDAO();
        if (currentEvent.getId() == null) {
            int id = dao.create(currentEvent);
            currentEvent.setId(id);
            if (!currentEvent.getRegions().isEmpty()) {
                dao.linkRegionsToEvent(id, currentEvent.getRegions());
            }
        } else {
            dao.update(currentEvent);
        }
    }

    private boolean validateForm() {
        if (titreField.getText() == null || titreField.getText().trim().isEmpty()) {
            showAlert("Erreur", "Le titre est obligatoire", Alert.AlertType.ERROR);
            return false;
        }

        if (typeCombo.getValue() == null) {
            showAlert("Erreur", "Veuillez sélectionner un type", Alert.AlertType.ERROR);
            return false;
        }

        if (!areDatesValid()) {
            return false;
        }
        
        if (selectedRegions.isEmpty()) {
            showAlert("Erreur", "Veuillez sélectionner au moins une région", Alert.AlertType.ERROR);
            return false;
        }

        return true;
    }

    private boolean areDatesValid() {
        if (dateDebutPicker.getValue() == null || dateFinPicker.getValue() == null) {
            showAlert("Erreur", "Les dates sont obligatoires", Alert.AlertType.ERROR);
            return false;
        }

        LocalDateTime debut = getDateTimeFromFields(dateDebutPicker, heureDebutSpinner);
        LocalDateTime fin = getDateTimeFromFields(dateFinPicker, heureFinSpinner);

        if (fin.isBefore(debut)) {
            showAlert("Erreur", "La date de fin doit être après la date de début", Alert.AlertType.ERROR);
            return false;
        }

        return true;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}