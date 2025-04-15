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

    public void initialize() {
        try {
            // Load products
            List<Produit> allProduits = produitService.getAllProducts();
            System.out.println("All products: " + (allProduits != null ? allProduits.size() : "null"));
            if (allProduits == null || allProduits.isEmpty()) {
                statusLabel.setText("Aucun produit disponible");
                return;
            }

            // Load stocks
            List<UUID> produitsInStock = stockService.getAllStocks().stream()
                    .map(Stock::getProduitId)
                    .collect(Collectors.toList());
            System.out.println("Products in stock: " + produitsInStock.size());

            // Filter available products
            List<Produit> availableProduits = allProduits; // Temporarily remove filter for testing
            // List<Produit> availableProduits = allProduits.stream()
            //     .filter(p -> !produitsInStock.contains(p.getId()))
            //     .collect(Collectors.toList());
            System.out.println("Available products: " + availableProduits.size());

            if (availableProduits.isEmpty()) {
                statusLabel.setText("Aucun produit disponible après filtrage");
            } else {
                produitCombo.setItems(FXCollections.observableArrayList(availableProduits));
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

            // Load entrepots
            entrepotList.setItems(FXCollections.observableArrayList(entrepotService.getAllEntrepots()));
            entrepotList.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Entrepot item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? null : item.getNom());
                }
            });

            dateEntreePicker.setValue(LocalDate.now());
            saveBtn.setOnAction(e -> saveStock());
            cancelBtn.setOnAction(e -> closeWindow());
        } catch (Exception e) {
            statusLabel.setText("Erreur: " + e.getMessage());
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

            if (entrepotList.getSelectionModel().getSelectedItems().isEmpty()) {
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

            // Conversion des entrepôts sélectionnés
            Set<UUID> selectedEntrepotIds = entrepotList.getSelectionModel().getSelectedItems().stream()
                    .map(Entrepot::getId)
                    .collect(Collectors.toSet());
            newStock.setEntrepotIds(selectedEntrepotIds);

            // Sauvegarde
            boolean success = stockService.addStock(newStock);
            if (success) {
                showAlert("Succès", "Stock ajouté avec succès", Alert.AlertType.INFORMATION);
                parentController.refreshStockData();
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