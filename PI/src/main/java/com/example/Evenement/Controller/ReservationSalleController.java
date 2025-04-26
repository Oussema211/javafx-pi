package com.example.Evenement.Controller;

import com.example.Evenement.Dao.PlaceDAO;
import com.example.Evenement.Model.Place;
import com.example.auth.model.User;
import com.example.auth.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.text.Text;

public class ReservationSalleController {
    @FXML private GridPane gridSalle;
    private int evenementId;
    private Stage stage;
    private Place selectedPlace;
    private Map<Button, Place> buttonPlaceMap = new HashMap<>();
    private ReservationCallback callback;

    public interface ReservationCallback {
        void onReservationConfirmee(int placeId);
    }

    public void setOnReservationConfirmee(ReservationCallback callback) {
        this.callback = callback;
    }

    public void setEvenementId(int evenementId) {
        System.out.println("setEvenementId appelé avec ID: " + evenementId);
        this.evenementId = evenementId;
        afficherPlaces();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void afficherPlaces() {
        try {
            System.out.println("Début afficherPlaces()");
            PlaceDAO dao = new PlaceDAO();
            List<Place> places = dao.getPlacesByEvenement(evenementId);
            System.out.println("Nombre de places trouvées: " + places.size());

            gridSalle.getChildren().clear();
            buttonPlaceMap.clear();

            // Ajouter les numéros de colonnes
            for (int col = 0; col < 10; col++) {
                Label colLabel = new Label(String.valueOf(col + 1));
                colLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-font-size: 14;");
                colLabel.setAlignment(Pos.CENTER);
                colLabel.setMinWidth(60);
                gridSalle.add(colLabel, col + 1, 0);
            }

            // Ajouter les lettres des lignes
            for (int row = 0; row < 8; row++) {
                Label rowLabel = new Label(String.valueOf((char)('A' + row)));
                rowLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32; -fx-font-size: 14;");
                rowLabel.setAlignment(Pos.CENTER);
                rowLabel.setMinHeight(60);
                gridSalle.add(rowLabel, 0, row + 1);
            }

            for (Place place : places) {
                Button btn = createSiegeButton(place);
                // Ajuster les indices pour correspondre à la grille
                gridSalle.add(btn, place.getNumeroColonne(), place.getNumeroLigne());
                buttonPlaceMap.put(btn, place);
            }

            System.out.println("Fin afficherPlaces()");
        } catch (Exception e) {
            System.err.println("Erreur dans afficherPlaces: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors du chargement des places : " + e.getMessage());
        }
    }

    private Button createSiegeButton(Place place) {
        try {
            Button btn = new Button(String.format("%c-%d", (char)('A' + place.getNumeroLigne() - 1), place.getNumeroColonne()));
            btn.setPrefSize(60, 60);
            btn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            
            // Style de base commun
            String baseStyle = "-fx-background-radius: 10;" +
                             "-fx-border-radius: 10;" +
                             "-fx-border-width: 2;" +
                             "-fx-font-size: 13;" +
                             "-fx-font-weight: bold;" +
                             "-fx-background-insets: 0,1,2,3;" +
                             "-fx-padding: 10;" +
                             "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0.0, 0, 2);" +
                             "-fx-border-style: solid inside;";

            // Appliquer le style selon le statut
            switch (place.getStatut()) {
                case "occupee":
                    btn.setStyle(baseStyle + 
                        "-fx-background-color: linear-gradient(to bottom right, #EEEEEE, #E0E0E0, #BDBDBD);" +
                        "-fx-border-color: #9E9E9E;" +
                        "-fx-text-fill: #757575;" +
                        "-fx-opacity: 0.7;");
                    btn.setDisable(true);
                    break;
                case "reservee":
                    btn.setStyle(baseStyle + 
                        "-fx-background-color: linear-gradient(to bottom right, #FFEBEE, #FFCDD2, #EF9A9A);" +
                        "-fx-border-color: #E57373;" +
                        "-fx-text-fill: #D32F2F;");
                    btn.setDisable(true);
                    break;
                default:
                    btn.setStyle(baseStyle + 
                        "-fx-background-color: linear-gradient(to bottom right, #F9FBE7, #F1F8E9, #DCEDC8);" +
                        "-fx-border-color: #7CB342;" +
                        "-fx-text-fill: #33691E;");
                    btn.setCursor(Cursor.HAND);

                    // Effet de survol uniquement si pas sélectionné
                    btn.setOnMouseEntered(e -> {
                        if (selectedPlace == null || selectedPlace.getId() != place.getId()) {
                            btn.setStyle(baseStyle + 
                                "-fx-background-color: linear-gradient(to bottom right, #F1F8E9, #DCEDC8, #C5E1A5);" +
                                "-fx-border-color: #558B2F;" +
                                "-fx-text-fill: #33691E;" +
                                "-fx-effect: dropshadow(three-pass-box, rgba(67,160,71,0.6), 8, 0.0, 0, 3);");
                        }
                    });

                    btn.setOnMouseExited(e -> {
                        if (selectedPlace == null || selectedPlace.getId() != place.getId()) {
                            btn.setStyle(baseStyle + 
                                "-fx-background-color: linear-gradient(to bottom right, #F9FBE7, #F1F8E9, #DCEDC8);" +
                                "-fx-border-color: #7CB342;" +
                                "-fx-text-fill: #33691E;");
                        }
                    });

                    // Effet de pression
                    btn.setOnMousePressed(e -> {
                        if (selectedPlace == null || selectedPlace.getId() != place.getId()) {
                            btn.setStyle(baseStyle + 
                                "-fx-background-color: linear-gradient(to bottom right, #DCEDC8, #C5E1A5, #AED581);" +
                                "-fx-border-color: #558B2F;" +
                                "-fx-effect: dropshadow(three-pass-box, rgba(67,160,71,0.4), 2, 0.0, 0, 1);" +
                                "-fx-translate-y: 1;");
                        }
                    });

                    btn.setOnMouseReleased(e -> {
                        if (selectedPlace == null || selectedPlace.getId() != place.getId()) {
                            btn.setStyle(baseStyle + 
                                "-fx-background-color: linear-gradient(to bottom right, #F9FBE7, #F1F8E9, #DCEDC8);" +
                                "-fx-border-color: #7CB342;" +
                                "-fx-text-fill: #33691E;" +
                                "-fx-translate-y: 0;");
                        }
                    });

                    // Action de sélection
                    btn.setOnAction(e -> {
                        if (selectedPlace != null && selectedPlace.getId() == place.getId()) {
                            // Désélection
                            selectedPlace = null;
                            btn.setStyle(baseStyle + 
                                "-fx-background-color: linear-gradient(to bottom right, #F9FBE7, #F1F8E9, #DCEDC8);" +
                                "-fx-border-color: #7CB342;" +
                                "-fx-text-fill: #33691E;");
                        } else {
                            // Nouvelle sélection
                            deselectAll();
                            selectedPlace = place;
                            btn.setStyle(baseStyle + 
                                "-fx-background-color: linear-gradient(to bottom right, #FFEBEE, #FFCDD2, #EF5350);" +
                                "-fx-border-color: #D32F2F;" +
                                "-fx-text-fill: white;" +
                                "-fx-effect: dropshadow(three-pass-box, rgba(211,47,47,0.6), 8, 0.0, 0, 3);");
                        }
                    });
            }
            
            return btn;
        } catch (Exception e) {
            System.err.println("Erreur dans createSiegeButton: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void deselectAll() {
        String baseStyle = "-fx-background-radius: 10;" +
                         "-fx-border-radius: 10;" +
                         "-fx-border-width: 2;" +
                         "-fx-font-size: 13;" +
                         "-fx-font-weight: bold;" +
                         "-fx-background-insets: 0,1,2,3;" +
                         "-fx-padding: 10;" +
                         "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0.0, 0, 2);" +
                         "-fx-border-style: solid inside;";

        for (Map.Entry<Button, Place> entry : buttonPlaceMap.entrySet()) {
            Button btn = entry.getKey();
            Place place = entry.getValue();
            
            if ("libre".equals(place.getStatut())) {
                btn.setStyle(baseStyle + 
                    "-fx-background-color: linear-gradient(to bottom right, #F9FBE7, #F1F8E9, #DCEDC8);" +
                    "-fx-border-color: #7CB342;" +
                    "-fx-text-fill: #33691E;");
            }
        }
        selectedPlace = null;
    }

    @FXML
    private void handleValider() {
        if (selectedPlace == null) {
            showError("Veuillez sélectionner une place.");
            return;
        }
        try {
            User user = SessionManager.getInstance().getLoggedInUser();
            if (user == null) {
                showError("Vous devez être connecté.");
                return;
            }
            PlaceDAO dao = new PlaceDAO();
            dao.reserverPlace(selectedPlace.getId(), user.getId().toString());
            if (callback != null) {
                callback.onReservationConfirmee(selectedPlace.getId());
            }
            showInfo("Votre place a été réservée avec succès !");
            stage.close();
        } catch (Exception e) {
            showError("Erreur lors de la réservation : " + e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        stage.close();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
} 