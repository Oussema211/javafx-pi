package com.example.Stock.Controller;

import com.example.Stock.Model.Stock;
import com.example.Stock.service.StockService;
import com.example.Stock.service.EntrepotService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;

public class DashboardController {

    @FXML private Label totalStocksLabel;
    @FXML private Label activeWarehousesLabel;
    @FXML private Label alertsCountLabel;
    @FXML private Label stockTrendLabel;
    @FXML private PieChart stockPieChart;
    @FXML private BarChart<String, Number> stockMovementChart;
    @FXML private HBox timeFilterBox;
    @FXML private VBox mainContent;
    @FXML private ComboBox<String> filterCombo;
    @FXML private ToggleGroup timeFilterGroup;

    private final StockService stockService = new StockService();
    private final EntrepotService entrepotService = new EntrepotService();
    private int timeRange = 7; // Default: 7 days
    private LocalDate currentPeriodStart = LocalDate.now().minusDays(30);




    public void initialize() {
        timeFilterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                RadioButton selected = (RadioButton) newVal;
                switch (selected.getText()) {
                    case "📅 7 jours": timeRange = 7; break;
                    case "🗓️ 30 jours": timeRange = 30; break;
                    case "📆 90 jours": timeRange = 90; break;
                    case "⏱️ Personnalisé":

                        break;
                }
                loadDashboardData();
            }
        });
    }

    private void setupTimeFilter() {
        timeFilterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                RadioButton selected = (RadioButton) newVal;
                switch (selected.getText()) {
                    case "📅 7 jours": timeRange = 7; break;
                    case "🗓️ 30 jours": timeRange = 30; break;
                    case "📆 90 jours": timeRange = 90; break;
                    case "⏱️ Personnalisé":
                        // Implémenter un sélecteur de date personnalisé
                        break;
                }
                loadDashboardData();
            }
        });
    }

    private void setupFilterCombo() {
        filterCombo.getItems().addAll(
                "Tous",
                "Alertes seulement",
                "Par catégorie",
                "Par entrepôt"
        );
        filterCombo.getSelectionModel().selectFirst();
    }

    private void loadDashboardData() {
        int totalStocks = stockService.getTotalStocks(timeRange);
        int activeWarehouses = entrepotService.getActiveWarehousesCount();
        int alertsCount = stockService.getLowStockItemsCount(timeRange);
        double stockTrend = stockService.getStockTrend(timeRange);

        totalStocksLabel.setText(String.format("%,d", totalStocks));
        activeWarehousesLabel.setText(String.valueOf(activeWarehouses));
        alertsCountLabel.setText(String.valueOf(alertsCount));

        String trendText = String.format("%s %.1f%%",
                stockTrend >= 0 ? "↑" : "↓",
                Math.abs(stockTrend));
        stockTrendLabel.setText(trendText);
        stockTrendLabel.getStyleClass().add(stockTrend >= 0 ? "trend-up" : "trend-down");

        updatePieChart();
        updateMovementChart();
    }

    // Méthodes de navigation temporelle
    @FXML
    private void nextPeriod(ActionEvent event) {
        currentPeriodStart = currentPeriodStart.plusDays(timeRange);
        updateMovementChart();
    }

    @FXML
    private void previousPeriod(ActionEvent event) {
        currentPeriodStart = currentPeriodStart.minusDays(timeRange);
        updateMovementChart();
    }

    // Méthodes d'affichage des détails
    @FXML
    private void showStockHistory(ActionEvent event) {
        // Implémentez la logique pour afficher l'historique complet
        System.out.println("Afficher l'historique des stocks");
    }

    @FXML
    private void showCategoryDetails(ActionEvent event) {
        // Implémentez la logique pour afficher les détails par catégorie
        System.out.println("Afficher les détails par catégorie");
    }

    @FXML
    private void showAlerts() {
        // Implémentez la logique pour afficher les alertes
        System.out.println("Afficher les alertes de stock");
    }

    @FXML
    private void showWarehouseMap() {
        // Implémentez la logique pour afficher la carte des entrepôts
        System.out.println("Afficher la carte des entrepôts");
    }

    // Méthodes d'actions
    @FXML
    private void handleRefresh(ActionEvent event) {
        loadDashboardData();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Actualisation");
        alert.setHeaderText(null);
        alert.setContentText("Les données ont été actualisées avec succès !");
        alert.showAndWait();
    }

    @FXML
    private void handleExport(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter les données");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"),
                new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx")
        );

        Stage stage = (Stage) mainContent.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            // Implémentez la logique d'export ici
            System.out.println("Export vers: " + file.getAbsolutePath());
        }
    }

    @FXML
    private void addStock(ActionEvent event) {
        // Implémentez la logique pour ajouter un stock
        System.out.println("Ajouter un nouveau stock");
    }

    @FXML
    private void addWarehouse(ActionEvent event) {
        // Implémentez la logique pour ajouter un entrepôt
        System.out.println("Ajouter un nouvel entrepôt");
    }

    @FXML
    private void generateReport(ActionEvent event) {
        // Implémentez la logique pour générer un rapport
        System.out.println("Générer un rapport");
    }

    private void updatePieChart() {
        stockPieChart.getData().clear();
        Map<String, Integer> distribution = stockService.getStockDistributionByCategory(timeRange);
        distribution.forEach((category, count) -> {
            PieChart.Data slice = new PieChart.Data(category, count);
            stockPieChart.getData().add(slice);
        });
    }

    private void updateMovementChart() {
        stockMovementChart.getData().clear();
        Map<String, Integer> movement = stockService.getStockMovement(timeRange);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Mouvement");
        movement.forEach((date, count) -> series.getData().add(new XYChart.Data<>(date, count)));
        stockMovementChart.getData().add(series);
    }
}