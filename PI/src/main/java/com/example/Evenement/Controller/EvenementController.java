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
import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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
    @FXML private ListView<Region> regionsListView;
    @FXML private Button uploadPhotoBtn;
    @FXML private Button saveBtn;

    // Données
    private Evenement currentEvent;
    private String photoPath;
    private final List<Region> allRegions = new ArrayList<>();

    @FXML
    public void initialize() {
        configureComboBoxes();
        configureTimeSpinners();
        loadAllRegions();
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
            regionsListView.getItems().setAll(allRegions);
            regionsListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des régions: " + e.getMessage(), Alert.AlertType.ERROR);
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
        regionsListView.getSelectionModel().clearSelection();
        event.getRegions().forEach(region -> {
            int index = allRegions.indexOf(region);
            if (index >= 0) {
                regionsListView.getSelectionModel().select(index);
            }
        });
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
        currentEvent.getRegions().addAll(
                regionsListView.getSelectionModel().getSelectedItems()
        );
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

    public void setSelectedRegion(Region region) {
        int index = allRegions.indexOf(region);
        if (index >= 0) {
            regionsListView.getSelectionModel().select(index);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}