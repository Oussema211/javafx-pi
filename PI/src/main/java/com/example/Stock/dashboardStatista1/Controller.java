package com.example.Stock.dashboardStatista1;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.Model.Stock;
import com.example.Stock.service.EntrepotService;
import com.example.Stock.service.StockService;
import com.example.produit.model.Produit;
import com.example.produit.service.ProduitDAO;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Controller {
  private static final Logger LOGGER = Logger.getLogger(Controller.class.getName());

  @FXML
  private VBox dashboardContainer;
  @FXML
  private WebView mapView;
  @FXML
  private ProgressIndicator mapLoadingIndicator;
  @FXML
  private BarChart<String, Number> quantityChart;
  @FXML
  private PieChart categoryChart;
  @FXML
  private AreaChart<Number, Number> stockEvolutionChart;
  @FXML
  private Label totalProductsLabel;
  @FXML
  private Label totalWarehousesLabel;
  @FXML
  private Label lowStockLabel;
  @FXML
  private ComboBox<String> filterComboBox;
  @FXML
  private DatePicker startDatePicker;
  @FXML
  private DatePicker endDatePicker;
  @FXML
  private Button applyFilterButton;
  @FXML
  private Button refreshButton;
  @FXML
  private Button exportButton;
  @FXML
  private Button themeToggle;
  @FXML
  private TextField mapSearchField;

  private final EntrepotService entrepotService = new EntrepotService();
  private final StockService stockService = new StockService();
  private boolean isDarkMode = false;
  private ListView<String> alertesList;
  private Label produitPlusDemandeLabel;

  @FXML
  public void initialize() {
    // Ajouter ListView pour les alertes
    alertesList = new ListView<>();
    alertesList.setPrefHeight(100);
    dashboardContainer.getChildren().add(2, new Label("Alertes de Rupture de Stock"));
    dashboardContainer.getChildren().add(3, alertesList);

    // Ajouter Label pour le produit le plus demandé
    produitPlusDemandeLabel = new Label("Produit le plus demandé: Aucun");
    dashboardContainer.getChildren().add(4, new Label("Produit le Plus Demandé"));
    dashboardContainer.getChildren().add(5, produitPlusDemandeLabel);

    // Configurer les animations
    configureChartAnimations();

    // Initialiser les filtres
    initializeFilterComboBox();
    setupDatePickers();

    // Configurer les autres fonctionnalités
    setupThemeToggle();
    setupExportFunctionality();
    setupMapSearch();
    setupResponsiveBehavior();

    // Exécuter le script Python et charger les données
    executerScriptPython();
    applyFilters();
  }

  private void configureChartAnimations() {
    FadeTransition fadeQuantity = new FadeTransition(Duration.millis(800), quantityChart);
    fadeQuantity.setFromValue(0);
    fadeQuantity.setToValue(1);
    fadeQuantity.play();

    FadeTransition fadeCategory = new FadeTransition(Duration.millis(800), categoryChart);
    fadeCategory.setFromValue(0);
    fadeCategory.setToValue(1);
    fadeCategory.play();

    FadeTransition fadeEvolution = new FadeTransition(Duration.millis(800), stockEvolutionChart);
    fadeEvolution.setFromValue(0);
    fadeEvolution.setToValue(1);
    fadeEvolution.play();

    FadeTransition fadeMap = new FadeTransition(Duration.millis(800), mapView);
    fadeMap.setFromValue(0);
    fadeMap.setToValue(1);
    fadeMap.play();
  }

  private void initializeFilterComboBox() {
    filterComboBox.getItems().addAll("Semences", "Engrais", "Fruits", "Toutes");
    filterComboBox.setValue("Toutes");
    filterComboBox.setOnAction(e -> applyFilters());
  }

  private void initializeDashboardCards() {
    try (FileReader reader = new FileReader("rapport_stock.json")) {
      JSONObject rapport = new JSONObject(new JSONTokener(reader));
      totalProductsLabel.setText(String.valueOf(rapport.getInt("total_produits")));
      totalWarehousesLabel.setText(String.valueOf(rapport.getInt("total_entrepots")));
      lowStockLabel.setText(String.valueOf(rapport.getInt("produits_stock_faible")));
    } catch (IOException e) {
      LOGGER.severe("Error loading dashboard cards from JSON: " + e.getMessage());
      showAlert("Erreur", "Erreur lors du chargement des statistiques", Alert.AlertType.ERROR);
    }
  }

  private void initializeQuantityChart() {
    try (FileReader reader = new FileReader("rapport_stock.json")) {
      JSONObject rapport = new JSONObject(new JSONTokener(reader));
      JSONArray alertes = rapport.getJSONArray("alertes");

      XYChart.Series<String, Number> series = new XYChart.Series<>();
      series.setName("Jours avant Rupture");

      for (int i = 0; i < alertes.length(); i++) {
        JSONObject alerte = alertes.getJSONObject(i);
        String produitId = alerte.getString("produit_id").substring(0, 8);
        double joursAvantRupture = alerte.getDouble("jours_avant_rupture");
        double quantiteActuelle = alerte.getDouble("quantite_actuelle");
        String entrepotId = alerte.getString("entrepot_id").substring(0, 8);

        XYChart.Data<String, Number> data = new XYChart.Data<>(produitId, joursAvantRupture);
        data.nodeProperty().addListener((obs, old, newNode) -> {
          if (newNode != null) {
            String color = joursAvantRupture < 3 ? "#F44336" : joursAvantRupture < 7 ? "#FFC107" : "#4CAF50";
            newNode.setStyle("-fx-bar-fill: " + color + ";");
            Tooltip tooltip = new Tooltip(
                String.format("Produit: %s\nEntrepôt: %s\nJours avant rupture: %.2f\nQuantité: %.2f",
                    produitId, entrepotId, joursAvantRupture, quantiteActuelle));
            Tooltip.install(newNode, tooltip);
          }
        });
        series.getData().add(data);
      }

      Platform.runLater(() -> {
        quantityChart.getData().clear();
        quantityChart.getData().add(series);
        quantityChart.setTitle("Prédictions de Rupture de Stock");
        LOGGER.info("Bar chart updated with " + series.getData().size() + " rupture predictions");
      });

      NumberAxis yAxis = (NumberAxis) quantityChart.getYAxis();
      yAxis.setLabel("Jours avant Rupture");
      yAxis.setTickLabelFormatter(new StringConverter<Number>() {
        @Override
        public String toString(Number object) {
          return String.format("%.1f", object.doubleValue());
        }

        @Override
        public Number fromString(String string) {
          return Double.parseDouble(string);
        }
      });
    } catch (IOException e) {
      LOGGER.severe("Error initializing rupture prediction chart: " + e.getMessage());
      showAlert("Erreur", "Erreur lors de l'initialisation du graphique de prédiction", Alert.AlertType.ERROR);
    }
  }

  private void initializeCategoryChart() {
    try {
      String selectedCategory = filterComboBox.getValue();
      LocalDate startDate = startDatePicker.getValue();
      LocalDate endDate = endDatePicker.getValue();

      List<Produit> filteredProducts = ProduitDAO.getAllProducts().stream()
          .filter(p -> selectedCategory.equals("Toutes") ||
              (p.getCategory() != null && selectedCategory.equals(p.getCategory().getNom())))
          .filter(p -> p.getDateCreation() != null &&
              !p.getDateCreation().toLocalDate().isBefore(startDate) &&
              !p.getDateCreation().toLocalDate().isAfter(endDate))
          .collect(Collectors.toList());

      if (filteredProducts.isEmpty()) {
        LOGGER.warning("No products found for category chart");
        showAlert("Avertissement", "Aucun produit trouvé pour le graphique par catégorie", Alert.AlertType.WARNING);
        return;
      }

      Map<String, Long> produitsParCategorie = filteredProducts.stream()
          .collect(Collectors.groupingBy(
              p -> (p.getCategory() != null && p.getCategory().getNom() != null) ? p.getCategory().getNom()
                  : "Non catégorisé",
              Collectors.counting()));

      ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
      String[] colors = new String[10];
      colors[0] = "#4285F4";
      colors[1] = "#EA4335";
      colors[2] = "#FBBC05";
      colors[3] = "#34A853";
      colors[4] = "#673AB7";
      colors[5] = "#FF5722";
      colors[6] = "#009688";
      colors[7] = "#E91E63";
      colors[8] = "#3F51B5";
      colors[9] = "#795548";
      int colorIndex = 0;
      long total = produitsParCategorie.values().stream().mapToLong(Long::longValue).sum();

      for (Map.Entry<String, Long> entry : produitsParCategorie.entrySet()) {
        double percentage = (entry.getValue() * 100.0) / total;
        PieChart.Data slice = new PieChart.Data(String.format("%s (%.1f%%)", entry.getKey(), percentage),
            entry.getValue());
        final int currentColorIndex = colorIndex % colors.length;
        final String categoryKey = entry.getKey();

        slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
          if (newNode != null) {
            newNode.setStyle("-fx-pie-color: " + colors[currentColorIndex] + ";");
            Tooltip tooltip = new Tooltip(
                String.format("%s\nProduits: %d (%.1f%%)", categoryKey, entry.getValue().longValue(), percentage));
            Tooltip.install(newNode, tooltip);
            newNode.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> filterComboBox.setValue(categoryKey));
          }
        });

        pieChartData.add(slice);
        colorIndex++;
      }

      Platform.runLater(() -> {
        categoryChart.getData().clear();
        categoryChart.setData(pieChartData);
        categoryChart.setLabelsVisible(true);
        categoryChart.setLegendVisible(true);
        categoryChart.setTitle("Répartition par catégorie");
        LOGGER.info("Pie chart updated with " + pieChartData.size() + " slices");
      });
    } catch (Exception e) {
      LOGGER.severe("Error initializing pie chart: " + e.getMessage());
      showAlert("Erreur", "Erreur lors de l'initialisation du graphique en camembert", Alert.AlertType.ERROR);
    }
  }

  private void loadStockEvolutionData() {
    try {
      String selectedCategory = filterComboBox.getValue();
      LocalDate startDate = startDatePicker.getValue();
      LocalDate endDate = endDatePicker.getValue();

      if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
        LOGGER.warning("Invalid date range for area chart");
        showAlert("Erreur", "Veuillez sélectionner une plage de dates valide", Alert.AlertType.WARNING);
        return;
      }

      Map<Date, Integer> productsData = ProduitDAO.getNewProductsOverTime(startDate, endDate, selectedCategory);
      Map<Date, Integer> stocksData = stockService.getNewStocksOverTime(startDate, endDate, selectedCategory);

      XYChart.Series<Number, Number> productsSeries = new XYChart.Series<>();
      productsSeries.setName("Nouveaux produits");

      XYChart.Series<Number, Number> stocksSeries = new XYChart.Series<>();
      stocksSeries.setName("Nouveaux stocks");

      if (productsData.isEmpty() && stocksData.isEmpty()) {
        LOGGER.warning("No data available for area chart with category: " + selectedCategory);
        showAlert("Avertissement", "Aucune donnée disponible pour la période et catégorie sélectionnées",
            Alert.AlertType.WARNING);
      }

      productsData.forEach((date, count) -> {
        long epochDay = date.toLocalDate().toEpochDay();
        productsSeries.getData().add(new XYChart.Data<>(epochDay, count));
        LOGGER.info("Product data point: " + date + ", count: " + count);
      });

      stocksData.forEach((date, count) -> {
        long epochDay = date.toLocalDate().toEpochDay();
        stocksSeries.getData().add(new XYChart.Data<>(epochDay, count));
        LOGGER.info("Stock data point: " + date + ", count: " + count);
      });

      Platform.runLater(() -> {
        stockEvolutionChart.getData().clear();
        stockEvolutionChart.getData().addAll(productsSeries, stocksSeries);
        applyAreaChartStyles();
        LOGGER.info("Area chart updated with " + productsSeries.getData().size() + " product points and "
            + stocksSeries.getData().size() + " stock points");
      });
    } catch (Exception e) {
      LOGGER.severe("Error loading area chart data: " + e.getMessage());
      showAlert("Erreur", "Erreur lors du chargement des données du graphique d'évolution", Alert.AlertType.ERROR);
    }
  }

  private void applyAreaChartStyles() {
    for (XYChart.Series<Number, Number> series : stockEvolutionChart.getData()) {
      for (XYChart.Data<Number, Number> data : series.getData()) {
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
          if (newNode != null) {
            String style = series.getName().equals("Nouveaux produits") ? "-fx-background-color: #4285F4, transparent;"
                : "-fx-background-color: #34A853, transparent;";
            newNode.setStyle(style);
          }
        });
      }
    }
    Platform.runLater(() -> {
      for (XYChart.Series<Number, Number> series : stockEvolutionChart.getData()) {
        series.getNode().lookup(".chart-series-area-fill").setStyle(
            series.getName().equals("Nouveaux produits") ? "-fx-fill: #4285F4;" : "-fx-fill: #34A853;");
      }
    });
  }

  private void executerScriptPython() {
    try {
      ProcessBuilder pb = new ProcessBuilder("python", "scripts/stock_rupture_prediction.py");
      pb.redirectErrorStream(true);
      Process process = pb.start();
      process.waitFor();
    } catch (IOException | InterruptedException e) {
      LOGGER.severe("Error executing Python script: " + e.getMessage());
      showAlert("Erreur", "Erreur lors de l'exécution du script Python: " + e.getMessage(), Alert.AlertType.ERROR);
    }
  }

  private void chargerRapport() {
    try (FileReader reader = new FileReader("rapport_stock.json")) {
      JSONObject rapport = new JSONObject(new JSONTokener(reader));

      // Mettre à jour les alertes
      alertesList.getItems().clear();
      JSONArray alertes = rapport.getJSONArray("alertes");
      for (int i = 0; i < alertes.length(); i++) {
        JSONObject alerte = alertes.getJSONObject(i);
        String texte = String.format("Produit: %s, Entrepôt: %s, Jours avant rupture: %.2f, Quantité: %.2f",
            alerte.getString("produit_id").substring(0, 8),
            alerte.getString("entrepot_id").substring(0, 8),
            alerte.getDouble("jours_avant_rupture"),
            alerte.getDouble("quantite_actuelle"));
        alertesList.getItems().add(texte);
      }

      // Mettre à jour le produit le plus demandé
      JSONObject produitPlusDemande = rapport.getJSONObject("produit_plus_demande");
      produitPlusDemandeLabel.setText(String.format("Produit: %s, Quantité totale: %.2f",
          produitPlusDemande.getString("produit_id").substring(0, 8),
          produitPlusDemande.getDouble("quantite_totale")));
    } catch (IOException e) {
      LOGGER.severe("Error loading JSON report: " + e.getMessage());
      showAlert("Erreur", "Erreur lors du chargement du rapport: " + e.getMessage(), Alert.AlertType.ERROR);
    }
  }

  private void setupDatePickers() {
    startDatePicker.setValue(LocalDate.now().minusMonths(6));
    endDatePicker.setValue(LocalDate.now());
    applyFilterButton.setOnAction(e -> applyFilters());
  }

  private void applyFilters() {
    executerScriptPython();
    initializeDashboardCards();
    initializeQuantityChart();
    initializeCategoryChart();
    loadStockEvolutionData();
    showWarehouseMap();
    chargerRapport();
  }

  @FXML
  private void refreshChartData() {
    applyFilters();
  }

  @FXML
  private void showWarehouseMap() {
    try {
      mapLoadingIndicator.setVisible(true);
      String selectedCategory = filterComboBox.getValue();
      LocalDate startDate = startDatePicker.getValue();
      LocalDate endDate = endDatePicker.getValue();

      List<Entrepot> filteredEntrepots = getFilteredEntrepots(selectedCategory, startDate, endDate);
      String mapHtml = getMapHtmlWithMarkers(filteredEntrepots);
      WebEngine engine = mapView.getEngine();
      engine.setJavaScriptEnabled(true);
      engine.loadContent(mapHtml);

      engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
        if (newState == Worker.State.SUCCEEDED) {
          Platform.runLater(() -> {
            mapLoadingIndicator.setVisible(false);
            engine.executeScript("if (typeof map !== 'undefined') map.invalidateSize(true);");
            LOGGER.info("Map loaded with " + filteredEntrepots.size() + " markers");
          });
        } else if (newState == Worker.State.FAILED) {
          Platform.runLater(() -> {
            mapLoadingIndicator.setVisible(false);
            showAlert("Erreur", "Échec du chargement de la carte.", Alert.AlertType.ERROR);
          });
        }
      });

      mapView.widthProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal.doubleValue() > 0) {
          engine.executeScript("if (typeof map !== 'undefined') map.invalidateSize(true);");
        }
      });
      mapView.heightProperty().addListener((obs, oldVal, newVal) -> {
        if (newVal.doubleValue() > 0) {
          engine.executeScript("if (typeof map !== 'undefined') map.invalidateSize(true);");
        }
      });
    } catch (Exception e) {
      LOGGER.severe("Error loading map: " + e.getMessage());
      mapLoadingIndicator.setVisible(false);
      showAlert("Erreur", "Impossible d'afficher la carte", Alert.AlertType.ERROR);
    }
  }

  private List<Entrepot> getFilteredEntrepots(String selectedCategory, LocalDate startDate, LocalDate endDate) {
    try {
      List<Entrepot> allEntrepots = entrepotService.getAllEntrepots();
      if (selectedCategory.equals("Toutes") && startDate != null && endDate != null && !startDate.isAfter(endDate)) {
        return allEntrepots;
      }

      List<Stock> filteredStocks = stockService.getAllStocksWithDetails().stream()
          .filter(stock -> {
            Produit p = stock.getProduit();
            if (p == null)
              return false;
            if (!selectedCategory.equals("Toutes")) {
              if (p.getCategory() == null || !p.getCategory().getNom().equals(selectedCategory)) {
                return false;
              }
            }
            if (startDate != null && endDate != null && !startDate.isAfter(endDate)) {
              LocalDate entryDate = stock.getDateEntree().toLocalDate();
              return !entryDate.isBefore(startDate) && !entryDate.isAfter(endDate);
            }
            return true;
          })
          .collect(Collectors.toList());

      Set<UUID> relevantEntrepotIds = filteredStocks.stream()
          .flatMap(stock -> stock.getEntrepotIds().stream())
          .collect(Collectors.toSet());

      return allEntrepots.stream()
          .filter(entrepot -> relevantEntrepotIds.contains(entrepot.getId()))
          .collect(Collectors.toList());
    } catch (Exception e) {
      LOGGER.severe("Error filtering entrepots: " + e.getMessage());
      return Collections.emptyList();
    }
  }

  private String getMapHtmlWithMarkers(List<Entrepot> entrepots) {
    StringBuilder markersSb = new StringBuilder();
    markersSb.append("var markers = [\n");
    for (Entrepot entrepot : entrepots) {
      if (entrepot.getLatitude() != null && entrepot.getLongitude() != null) {
        String nom = entrepot.getNom() != null ? entrepot.getNom().replace("'", "\\'") : "Sans nom";
        String adresse = entrepot.getAdresse() != null ? entrepot.getAdresse().replace("'", "\\'") : "Adresse inconnue";
        String ville = entrepot.getVille() != null ? entrepot.getVille().replace("'", "\\'") : "Ville inconnue";
        markersSb.append("    {lat: ").append(entrepot.getLatitude())
            .append(", lng: ").append(entrepot.getLongitude())
            .append(", nom: '").append(nom)
            .append("', adresse: '").append(adresse)
            .append("', ville: '").append(ville)
            .append("'},\n");
      }
    }
    markersSb.append("];\n");

    String markersScript = markersSb.toString();

    String mapHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <title>Carte des Entrepôts</title>
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css"/>
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    overflow: hidden;
                }
                #map {
                    background: #f4f4f4;
                    position: absolute;
                    top: 0;
                    left: 0;
                    right: 0;
                    bottom: 0;
                }
                .leaflet-tile {
                    filter: brightness(0.99) contrast(1.01);
                }
                .leaflet-tooltip {
                    font-size: 14px;
                    background-color: #fff;
                    border: 1px solid #ccc;
                    padding: 5px;
                    border-radius: 4px;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
            <script>
                var map = L.map('map', {
                    center: [36.8065, 10.1815],
                    zoom: 7,
                    zoomControl: true,
                    attributionControl: false,
                    preferCanvas: true,
                    fadeAnimation: false,
                    zoomAnimation: true,
                    markerZoomAnimation: false,
                    renderer: L.canvas()
                });

                var osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19,
                    minZoom: 3,
                    subdomains: ['a', 'b', 'c'],
                    attribution: '© OpenStreetMap',
                    crossOrigin: true,
                    tileSize: 256,
                    updateWhenIdle: false,
                    unloadInvisibleTiles: true,
                    reuseTiles: true
                }).addTo(map);

                """
        + markersScript + """
                    markers.forEach(function(markerData) {
                        var marker = L.marker([markerData.lat, markerData.lng]).addTo(map);
                        marker.bindTooltip(markerData.nom, {
                            offset: [0, -10],
                            direction: 'top'
                        });
                        marker.bindPopup(
                            '<b>' + markerData.nom + '</b><br>' +
                            'Adresse: ' + markerData.adresse + '<br>' +
                            'Ville: ' + markerData.ville
                        );
                        marker.on('click', function() {
                            map.flyTo([markerData.lat, markerData.lng], 15, {
                                animate: true,
                                duration: 1
                            });
                        });
                    });

                    if (markers.length > 0) {
                        var bounds = L.latLngBounds(markers.map(function(m) { return [m.lat, m.lng]; }));
                        map.fitBounds(bounds, { padding: [20, 20] });
                    } else {
                        map.setView([36.8065, 10.1815], 7);
                    }

                    function forceMapResize() {
                        setTimeout(function() {
                            map.invalidateSize(true);
                        }, 300);
                    }

                    forceMapResize();
                    window.addEventListener('resize', forceMapResize);
                    document.addEventListener('visibilitychange', forceMapResize);
                </script>
            </body>
            </html>
            """;

    return mapHtml;
  }

  private void setupMapSearch() {
    mapSearchField.setPromptText("Rechercher un entrepôt...");
    mapSearchField.textProperty().addListener((obs, old, newValue) -> {
      if (!newValue.trim().isEmpty()) {
        WebEngine engine = mapView.getEngine();
        engine.executeScript(
            "if (typeof searchMarkers !== 'undefined') searchMarkers('" + newValue.replace("'", "\\'") + "');");
      }
    });
  }

  private void setupThemeToggle() {
    themeToggle.setOnAction(e -> {
      isDarkMode = !isDarkMode;
      dashboardContainer.getStylesheets().clear();
      dashboardContainer.getStylesheets()
          .add(getClass().getResource(isDarkMode ? "/dark-theme.css" : "/styles.css").toExternalForm());
      themeToggle.setText(isDarkMode ? "Mode Clair" : "Mode Sombre");
    });
  }

  private void setupExportFunctionality() {
    exportButton.setOnAction(e -> {
      try {
        BufferedImage barChartImage = new BufferedImage((int) quantityChart.getWidth(), (int) quantityChart.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        javafx.embed.swing.SwingFXUtils.fromFXImage(quantityChart.snapshot(null, null), barChartImage);
        ImageIO.write(barChartImage, "png", new File("rupture_chart_" + UUID.randomUUID() + ".png"));

        BufferedImage pieChartImage = new BufferedImage((int) categoryChart.getWidth(), (int) categoryChart.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        javafx.embed.swing.SwingFXUtils.fromFXImage(categoryChart.snapshot(null, null), pieChartImage);
        ImageIO.write(pieChartImage, "png", new File("category_chart_" + UUID.randomUUID() + ".png"));

        BufferedImage evolutionChartImage = new BufferedImage((int) stockEvolutionChart.getWidth(),
            (int) stockEvolutionChart.getHeight(), BufferedImage.TYPE_INT_ARGB);
        javafx.embed.swing.SwingFXUtils.fromFXImage(stockEvolutionChart.snapshot(null, null), evolutionChartImage);
        ImageIO.write(evolutionChartImage, "png", new File("evolution_chart_" + UUID.randomUUID() + ".png"));

        showAlert("Succès", "Graphiques exportés avec succès", Alert.AlertType.INFORMATION);
      } catch (Exception ex) {
        LOGGER.severe("Error exporting charts: " + ex.getMessage());
        showAlert("Erreur", "Échec de l'exportation des graphiques", Alert.AlertType.ERROR);
      }
    });
  }

  private void setupResponsiveBehavior() {
    dashboardContainer.widthProperty().addListener((obs, oldVal, newVal) -> {
      double width = newVal.doubleValue();
      if (width < 800) {
        dashboardContainer.setSpacing(10);
        mapView.setPrefHeight(200);
        quantityChart.setPrefHeight(200);
        categoryChart.setPrefHeight(200);
        stockEvolutionChart.setPrefHeight(200);
      } else {
        dashboardContainer.setSpacing(15);
        mapView.setPrefHeight(300);
        quantityChart.setPrefHeight(300);
        categoryChart.setPrefHeight(300);
        stockEvolutionChart.setPrefHeight(300);
      }
    });
  }

  private void showAlert(String title, String message, Alert.AlertType alertType) {
    Platform.runLater(() -> {
      Alert alert = new Alert(alertType);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.getDialogPane().setStyle("-fx-background-color: " + (isDarkMode ? "#424242" : "white") + ";");
      alert.showAndWait();
    });
  }
}