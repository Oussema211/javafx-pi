package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.Model.Stock;
import com.example.Stock.service.EntrepotService;
import com.example.Stock.service.StockService;
import com.example.auth.utils.SessionManager;
import com.example.produit.model.Categorie;
import com.example.produit.model.Produit;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.animation.FadeTransition;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class StockController {

    // Constants
    private static final String ALL_CATEGORIES_TEXT = "Toutes les categories";
    private static final String ALL_WAREHOUSES_TEXT = "Tous les entrepots";

    // Services
    private final StockService stockService = new StockService();
    private final EntrepotService entrepotService = new EntrepotService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // FXML components
    @FXML private VBox sidebar;
    @FXML private Button dashboardBtn, produitsBtn, entrepotsBtn, utilisateursBtn, parametresBtn, logoutBtn;
    @FXML private HBox topBar;
    @FXML private Button ajouterBtn, excelBtn, pdfBtn;
    @FXML private HBox filters;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private DatePicker dateEntreeFilter;
    @FXML private ComboBox<String> entrepotFilter;
    @FXML private Button resetBtn;
    @FXML private TableView<Stock> stockTable;
    @FXML private TableColumn<Stock, String> colId;
    @FXML private TableColumn<Stock, String> colProduit;
    @FXML private TableColumn<Stock, String> colImage;
    @FXML private TableColumn<Stock, Integer> colQuantite;
    @FXML private TableColumn<Stock, String> colDateEntree;
    @FXML private TableColumn<Stock, Integer> colSeuil;
    @FXML private TableColumn<Stock, String> colEntrepot;
    @FXML private TableColumn<Stock, String> colCategorie;
    @FXML private TableColumn<Stock, Void> colActions;

    private ObservableList<Stock> stockData = FXCollections.observableArrayList();
    private final Map<UUID, Entrepot> entrepotCache = new HashMap<>();

    @FXML
    public void initialize() {
        loadRealData();
        configureFilters();
        configureTable();
        applyFilters();
        configureTableColumns();
        // Animation de démarrage
        FadeTransition fade = new FadeTransition(Duration.millis(1000), stockTable);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void loadRealData() {
        stockData.clear();
        stockData.addAll(stockService.getAllStocksWithDetails());
    }

    public void refreshStockData() {
        stockData.clear();
        stockData.addAll(stockService.getAllStocksWithDetails());
        stockTable.refresh();
    }

    public UUID getCurrentUserId() {
        return SessionManager.getInstance().getLoggedInUser().getId();
    }

    private void configureTableColumns() {
        // Configuration des proportions des colonnes
        final Map<TableColumn<Stock, ?>, Double> columnProportions = new LinkedHashMap<>();
        columnProportions.put(colId, 0.08);
        columnProportions.put(colProduit, 0.20);
        columnProportions.put(colQuantite, 0.10);
        columnProportions.put(colDateEntree, 0.12);
        columnProportions.put(colSeuil, 0.08);
        columnProportions.put(colEntrepot, 0.15);
        columnProportions.put(colCategorie, 0.15);
        columnProportions.put(colActions, 0.12);

        // Listener pour le redimensionnement
        stockTable.widthProperty().addListener((obs, oldVal, newVal) -> {
            double tableWidth = newVal.doubleValue();
            double totalProportions = columnProportions.values().stream().mapToDouble(d -> d).sum();

            columnProportions.forEach((column, proportion) -> {
                double calculatedWidth = (proportion / totalProportions) * tableWidth;
                column.setPrefWidth(calculatedWidth);
            });
        });

        // Appliquer le style aux colonnes
        columnProportions.keySet().forEach(column -> {
            if (column == colId || column == colQuantite || column == colSeuil || column == colDateEntree || column == colActions) {
                column.setStyle("-fx-alignment: CENTER;");
            }
        });
    }

    private void configureFilters() {
        // Récupérer les catégories distinctes depuis les données
        Set<String> categories = stockData.stream()
                .map(stock -> stock.getProduit())
                .filter(Objects::nonNull)
                .map(produit -> produit.getCategory())
                .filter(Objects::nonNull)
                .map(Categorie::getNom)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        // Récupérer les noms d'entrepôts distincts
        Set<String> entrepotNames = stockData.stream()
                .flatMap(stock -> stock.getEntrepotIds().stream())
                .map(id -> {
                    Entrepot e = entrepotCache.computeIfAbsent(id, entrepotService::getEntrepotById);
                    return e != null ? e.getNom() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));

        // Configurer les ComboBox
        categorieFilter.getItems().setAll(ALL_CATEGORIES_TEXT);
        categorieFilter.getItems().addAll(categories);
        categorieFilter.setValue(ALL_CATEGORIES_TEXT);

        entrepotFilter.getItems().setAll(ALL_WAREHOUSES_TEXT);
        entrepotFilter.getItems().addAll(entrepotNames);
        entrepotFilter.setValue(ALL_WAREHOUSES_TEXT);

        // Ajouter les listeners
        searchField.textProperty().addListener((obs, old, val) -> applyFilters());
        categorieFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        entrepotFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
        dateEntreeFilter.valueProperty().addListener((obs, old, val) -> applyFilters());
    }

    private void applyFilters() {
        String searchText = searchField.getText().toLowerCase();
        String selectedCategory = categorieFilter.getValue();
        String selectedEntrepot = entrepotFilter.getValue();
        LocalDate selectedDate = dateEntreeFilter.getValue();

        List<Stock> filteredList = stockData.stream()
                .filter(stock -> filterBySearchText(stock, searchText))
                .filter(stock -> filterByCategory(stock, selectedCategory))
                .filter(stock -> filterByEntrepot(stock, selectedEntrepot))
                .filter(stock -> filterByDate(stock, selectedDate))
                .collect(Collectors.toList());

        stockTable.setItems(FXCollections.observableArrayList(filteredList));
    }

    private boolean filterBySearchText(Stock stock, String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            return true;
        }

        Produit produit = stock.getProduit();
        if (produit == null) {
            return false;
        }

        boolean matchesName = produit.getNom().toLowerCase().contains(searchText);
        boolean matchesDescription = produit.getDescription() != null &&
                produit.getDescription().toLowerCase().contains(searchText);

        return matchesName || matchesDescription;
    }

    private boolean filterByCategory(Stock stock, String category) {
        if (category == null || category.equals(ALL_CATEGORIES_TEXT)) {
            return true;
        }

        Produit produit = stock.getProduit();
        if (produit == null || produit.getCategory() == null) {
            return false;
        }

        return category.equals(produit.getCategory().getNom());
    }

    private boolean filterByEntrepot(Stock stock, String entrepot) {
        if (entrepot == null || entrepot.equals(ALL_WAREHOUSES_TEXT)) {
            return true;
        }

        return stock.getEntrepotIds().stream()
                .anyMatch(id -> {
                    Entrepot e = entrepotCache.computeIfAbsent(id, entrepotService::getEntrepotById);
                    return e != null && entrepot.equals(e.getNom());
                });
    }

    private boolean filterByDate(Stock stock, LocalDate date) {
        if (date == null) {
            return true;
        }
        return stock.getDateEntree().toLocalDate().equals(date);
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        categorieFilter.setValue(ALL_CATEGORIES_TEXT);
        entrepotFilter.setValue(ALL_WAREHOUSES_TEXT);
        dateEntreeFilter.setValue(null);
        applyFilters();
    }

    private void configureTable() {
        // Colonne ID
        colId.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getId().toString().substring(0, 8)));

        // Colonne Produit
        colProduit.setCellValueFactory(cellData -> {
            Produit p = cellData.getValue().getProduit();
            return new SimpleStringProperty(p != null ? p.getNom() : "Inconnu");
        });

        // Colonne Image
        colImage.setCellFactory(column -> new TableCell<Stock, String>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitHeight(40);
                imageView.setFitWidth(40);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null) {
                    setGraphic(null);
                } else {
                    try {
                        Image img = new Image("file:" + imagePath);
                        imageView.setImage(img);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(new Label("No Image"));
                    }
                }
            }
        });

        // Colonne Quantité
        colQuantite.setCellValueFactory(cellData -> {
            Produit p = cellData.getValue().getProduit();
            return new SimpleIntegerProperty(p != null ? p.getQuantite() : 0).asObject();
        });

        // Colonne Date Entrée
        colDateEntree.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDateEntree().format(dateFormatter)));

        // Colonne Seuil Alerte
        colSeuil.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getSeuilAlert()).asObject());

        // Colonne Entrepôt
        colEntrepot.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatEntrepots(cellData.getValue().getEntrepotIds())));

        // Colonne Catégorie
        colCategorie.setCellValueFactory(cellData -> {
            Produit p = cellData.getValue().getProduit();
            Categorie c = p != null ? p.getCategory() : null;
            return new SimpleStringProperty(c != null ? c.getNom() : "Inconnue");
        });

        // Colonne Actions
        colActions.setCellFactory(column -> new TableCell<Stock, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox box = new HBox(5, editBtn, deleteBtn);

            {
                box.setAlignment(Pos.CENTER);
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

                editBtn.setOnAction(e -> {
                    Stock stock = getTableView().getItems().get(getIndex());
                    editStock(stock);
                });
                deleteBtn.setOnAction(e -> {
                    Stock stock = getTableView().getItems().get(getIndex());
                    deleteStock(stock);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        stockTable.setItems(stockData);
    }

    private String formatEntrepots(Set<UUID> entrepotIds) {
        if (entrepotIds == null || entrepotIds.isEmpty()) {
            return "Aucun entrepôt";
        }
        return entrepotIds.stream()
                .map(id -> {
                    Entrepot e = entrepotCache.computeIfAbsent(id, entrepotService::getEntrepotById);
                    return e != null ? e.getNom() : "Entrepôt " + id.toString().substring(0, 4);
                })
                .collect(Collectors.joining(", "));
    }

    private void editStock(Stock stock) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Stock/view/EditStockForm.fxml"));
            Parent root = loader.load();

            EditStockController controller = loader.getController();
            controller.setStock(stock);
            controller.setParentController(this);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Modifier le Stock");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre de modification", Alert.AlertType.ERROR);
        }
    }

    private void deleteStock(Stock stock) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le stock");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce stock ?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = stockService.deleteStock(stock.getId());
            if (success) {
                stockData.remove(stock);
                showAlert("Succès", "Stock supprimé avec succès", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Erreur", "Échec de la suppression du stock", Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleAjouter() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/Stock/view/AddStockForm.fxml"));
            Parent root = loader.load();

            AddStockController addStockController = loader.getController();
            addStockController.setParentController(this);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Ajouter un nouveau stock");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleExportExcel() {
        System.out.println("Export vers Excel");
    }

    @FXML
    private void handleExportPDF() {
        System.out.println("Export vers PDF");
    }
}