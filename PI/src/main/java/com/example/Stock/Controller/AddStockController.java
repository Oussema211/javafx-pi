package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.Model.Stock;
import com.example.Stock.service.StockService;
import com.example.Stock.service.EntrepotService;
import com.example.produit.model.Produit;
import com.example.produit.service.ProduitDAO;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AddStockController {

    @FXML private ComboBox<Produit> produitCombo;
    @FXML private DatePicker dateEntreePicker;
    @FXML private TextField seuilAlertField;
    @FXML private ListView<Entrepot> entrepotList;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label statusLabel;

    private final StockService stockService = new StockService();
    private final ProduitDAO produitService = new ProduitDAO();
    private final EntrepotService entrepotService = new EntrepotService();

    private StockController parentController;

    public void setParentController(StockController parentController) {
        this.parentController = parentController;
    }

    @FXML
    public void initialize() {
        try {
            // Charger tous les produits
            List<Produit> allProduits = produitService.getAllProducts();

            if (allProduits == null || allProduits.isEmpty()) {
                statusLabel.setText("Aucun produit disponible");
                return;
            }

            // Charger les stocks existants
            List<Stock> existingStocks = stockService.getAllStocks();
            Set<String> produitsDejaEnStock = existingStocks.stream()
                    .map(Stock::getProduitId)
                    .collect(Collectors.toSet());

            // Filtrer les produits - seulement ceux qui ne sont pas déjà en stock
            List<Produit> produitsDisponibles = allProduits.stream()
                    .filter(p -> !produitsDejaEnStock.contains(p.getId()))
                    .collect(Collectors.toList());

            if (produitsDisponibles.isEmpty()) {
                statusLabel.setText("Tous les produits sont déjà en stock");
                produitCombo.setDisable(true);
                saveBtn.setDisable(true);
            } else {
                // Configurer la ComboBox des produits
                produitCombo.setItems(FXCollections.observableArrayList(produitsDisponibles));
                produitCombo.setCellFactory(param -> new ListCell<>() {
                    @Override
                    protected void updateItem(Produit item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? null : item.getNom());
                    }
                });
                produitCombo.setButtonCell(new ListCell<>() {
                    @Override
                    protected void updateItem(Produit item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty || item == null ? "Sélectionner un produit" : item.getNom());
                    }
                });
            }

            // Configurer la ListView des entrepôts (sélection multiple)
            entrepotList.setItems(FXCollections.observableArrayList(entrepotService.getAllEntrepots()));
            entrepotList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            entrepotList.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Entrepot item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNom());
                }
            });

            // Valeurs par défaut
            dateEntreePicker.setValue(LocalDate.now());

            // Gestion des événements
            saveBtn.setOnAction(e -> saveStock());
            cancelBtn.setOnAction(e -> closeWindow());

        } catch (Exception e) {
            statusLabel.setText("Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void saveStock() {
        try {
            // Validation des données
            if (produitCombo.getValue() == null) {
                showAlert("Erreur", "Veuillez sélectionner un produit", Alert.AlertType.ERROR);
                return;
            }

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
                showAlert("Erreur", "Le seuil d'alerte doit être un nombre valide", Alert.AlertType.ERROR);
                return;
            }

            List<Entrepot> selectedEntrepots = entrepotList.getSelectionModel().getSelectedItems();
            if (selectedEntrepots.isEmpty()) {
                showAlert("Erreur", "Veuillez sélectionner au moins un entrepôt", Alert.AlertType.ERROR);
                return;
            }

            // Création du nouveau stock
            Stock newStock = new Stock();
            newStock.setId(UUID.randomUUID());
            newStock.setProduitId(produitCombo.getValue().getId());
            newStock.setDateEntree(dateEntreePicker.getValue().atStartOfDay());
            newStock.setSeuilAlert(seuilAlert);
            newStock.setUserId(parentController.getCurrentUserId());

            // Conversion des entrepôts sélectionnés en Set d'IDs
            Set<UUID> selectedEntrepotIds = selectedEntrepots.stream()
                    .map(Entrepot::getId)
                    .collect(Collectors.toSet());
            newStock.setEntrepotIds(selectedEntrepotIds);

            // Sauvegarde
            boolean success = stockService.addStock(newStock);
            if (success) {
                showAlert("Succès", "Stock ajouté avec succès", Alert.AlertType.INFORMATION);
                if (parentController != null) {
                    parentController.refreshStockData();
                }
                closeWindow();
            } else {
                showAlert("Erreur", "Échec de l'ajout du stock", Alert.AlertType.ERROR);
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