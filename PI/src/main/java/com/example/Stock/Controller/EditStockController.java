package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.Model.Stock;
import com.example.Stock.service.StockService;
import com.example.Stock.service.EntrepotService;
import com.example.produit.model.Produit;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class EditStockController {
    @FXML private Label titleLabel;
    @FXML private DatePicker dateEntreePicker;
    @FXML private DatePicker dateSortiePicker;
    @FXML private TextField seuilAlertField;
    @FXML private ListView<Entrepot> entrepotList;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    private Stock stock;
    private StockController parentController;
    private final StockService stockService = new StockService();
    private final EntrepotService entrepotService = new EntrepotService();

    public void setStock(Stock stock) {
        this.stock = stock;
        titleLabel.setText("Modifier Stock: " + stock.getProduit().getNom());
        initializeData();
    }

    public void setParentController(StockController parentController) {
        this.parentController = parentController;
    }

    @FXML
    public void initialize() {
        // Configurer la liste des entrepôts
        entrepotList.setItems(FXCollections.observableArrayList(entrepotService.getAllEntrepots()));
        entrepotList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        entrepotList.setCellFactory(param -> new ListCell<Entrepot>() {
            @Override
            protected void updateItem(Entrepot item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNom());
            }
        });

        // Configurer les boutons
        saveBtn.setOnAction(e -> updateStock());
        cancelBtn.setOnAction(e -> closeWindow());
    }

    private void initializeData() {
        // Configurer les dates
        dateEntreePicker.setValue(stock.getDateEntree().toLocalDate());
        if (stock.getDateSortie() != null) {
            dateSortiePicker.setValue(stock.getDateSortie().toLocalDate());
        }

        // Configurer le seuil d'alerte
        seuilAlertField.setText(String.valueOf(stock.getSeuilAlert()));

        // Sélectionner les entrepôts
        if (stock.getEntrepotIds() != null) {
            entrepotList.getItems().forEach(entrepot -> {
                if (stock.getEntrepotIds().contains(entrepot.getId())) {
                    entrepotList.getSelectionModel().select(entrepot);
                }
            });
        }
    }

    private void updateStock() {
        try {
            // Validation des données
            if (dateEntreePicker.getValue() == null) {
                showAlert("Erreur", "Veuillez sélectionner une date d'entrée", Alert.AlertType.ERROR);
                return;
            }

            int seuilAlert;
            try {
                seuilAlert = Integer.parseInt(seuilAlertField.getText());
                if (seuilAlert < 0) {
                    showAlert("Erreur", "Le seuil d'alerte doit être positif", Alert.AlertType.ERROR);
                    return;
                }
            } catch (NumberFormatException e) {
                showAlert("Erreur", "Le seuil d'alerte doit être un nombre positif", Alert.AlertType.ERROR);
                return;
            }

            // Mettre à jour l'objet Stock
            stock.setDateEntree(dateEntreePicker.getValue().atStartOfDay());

            if (dateSortiePicker.getValue() != null) {
                stock.setDateSortie(dateSortiePicker.getValue().atStartOfDay());
            } else {
                stock.setDateSortie(null);
            }

            stock.setSeuilAlert(seuilAlert);

            // Mettre à jour les entrepôts
            Set<UUID> selectedEntrepotIds = entrepotList.getSelectionModel().getSelectedItems().stream()
                    .map(Entrepot::getId)
                    .collect(Collectors.toSet());
            stock.setEntrepotIds(selectedEntrepotIds);

            // Sauvegarder les modifications
            boolean success = stockService.updateStock(stock);
            if (success) {
                showAlert("Succès", "Stock modifié avec succès", Alert.AlertType.INFORMATION);
                parentController.refreshStockData();
                closeWindow();
            } else {
                showAlert("Erreur", "Échec de la modification du stock", Alert.AlertType.ERROR);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Une erreur est survenue: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) cancelBtn.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}