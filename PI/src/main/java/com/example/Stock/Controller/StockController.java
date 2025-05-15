package com.example.Stock.Controller;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.Model.Stock;
import com.example.Stock.service.EntrepotService;
import com.example.Stock.service.StockService;
import com.example.auth.utils.SessionManager;
import com.example.produit.model.Categorie;
import com.example.produit.model.Produit;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.Chart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.animation.FadeTransition;
import javafx.scene.web.WebEngine;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.google.gson.Gson;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class
StockController {

    // Constants
    private static final String ALL_CATEGORIES_TEXT = "Toutes les categories";
    private static final String ALL_WAREHOUSES_TEXT = "Tous les entrepots";

    // Services
    private final StockService stockService = new StockService();
    private final EntrepotService entrepotService = new EntrepotService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // FXML components

    @FXML private Button dashboardBtn, produitsBtn, entrepotsBtn, utilisateursBtn, parametresBtn, logoutBtn;
    @FXML private HBox topBar;
    @FXML private Button ajouterBtn, excelBtn, pdfBtn, mapBtn;
    @FXML private HBox filters;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categorieFilter;
    @FXML private DatePicker dateEntreeFilter;
    @FXML private ComboBox<String> entrepotFilter;
    @FXML private Button resetBtn;
    @FXML private ListView<Stock> stockList;
    @FXML private Button sortByNameBtn;
    @FXML private Button sortByQuantityBtn;
    @FXML private Button sortByDateBtn;
    @FXML private TabPane mainTabPane;
    private ObservableList<Stock> stockData = FXCollections.observableArrayList();
    private final Map<UUID, Entrepot> entrepotCache = new HashMap<>();

    @FXML
    public void initialize() {
        loadRealData();
        configureFilters();
        configureList();
        applyFilters();

        // Sorting listeners
        sortByNameBtn.setOnAction(e -> sortListByName());
        sortByQuantityBtn.setOnAction(e -> sortListByQuantity());
        sortByDateBtn.setOnAction(e -> sortListByDate());

        // Animation de démarrage
        FadeTransition fade = new FadeTransition(Duration.millis(1000), stockList);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();

        // Select the stock list tab by default
        mainTabPane.getSelectionModel().select(0);
    }

    private void loadRealData() {
        stockData.clear();
        stockData.addAll(stockService.getAllStocksWithDetails());
    }

    public void refreshStockData() {
        // Afficher un indicateur de chargement
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(50, 50);
        stockList.setPlaceholder(progress);

        // Exécuter en arrière-plan
        new Thread(() -> {
            List<Stock> freshData = stockService.getAllStocksWithDetails();

            // Mettre à jour sur le thread UI
            Platform.runLater(() -> {
                stockData.setAll(freshData);
                applyFilters();
            });
        }).start();
    }

    public UUID getCurrentUserId() {
        return SessionManager.getInstance().getLoggedInUser().getId();
    }

    private void configureList() {
        stockList.setCellFactory(param -> new ListCell<Stock>() {
            @Override
            protected void updateItem(Stock stock, boolean empty) {
                super.updateItem(stock, empty);

                if (empty || stock == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle(""); // Reset style
                } else {
                    // Conteneur principal
                    HBox mainContainer = new HBox(15);
                    mainContainer.setAlignment(Pos.CENTER_LEFT);
                    mainContainer.setPadding(new Insets(15));
                    mainContainer.getStyleClass().add("stock-cell");

                    // Image du produit
                    ImageView imageView = new ImageView();
                    try {
                        if (stock.getProduit() != null && stock.getProduit().getImageName() != null) {
                            Image img = new Image("file:" + stock.getProduit().getImageName());
                            imageView.setImage(img);
                        }
                    } catch (Exception e) {
                        imageView.setImage(new Image("file:src/main/resources/icons/default_product.png")); // Fallback image
                    }
                    imageView.setFitHeight(60);
                    imageView.setFitWidth(60);
                    imageView.setPreserveRatio(true);
                    imageView.setSmooth(true);

                    // Conteneur des informations
                    VBox infoBox = new VBox(8);
                    infoBox.setMaxWidth(Double.MAX_VALUE);
                    HBox.setHgrow(infoBox, Priority.ALWAYS);

                    // Nom du produit et catégorie
                    HBox titleBox = new HBox(10);
                    Label nameLabel = new Label(stock.getProduit() != null ? stock.getProduit().getNom() : "Produit inconnu");
                    nameLabel.getStyleClass().add("stock-name");

                    Label categoryLabel = new Label(stock.getProduit() != null && stock.getProduit().getCategory() != null ?
                            stock.getProduit().getCategory().getNom() : "Sans catégorie");
                    categoryLabel.getStyleClass().add("stock-category");

                    titleBox.getChildren().addAll(nameLabel, categoryLabel);

                    // Détails
                    HBox detailsBox = new HBox(20);
                    detailsBox.setAlignment(Pos.CENTER_LEFT);

                    Label quantityLabel = new Label("📦 " + (stock.getProduit() != null ? stock.getProduit().getQuantite() : 0));
                    quantityLabel.getStyleClass().add("stock-detail");

                    Label dateLabel = new Label("📅 " + stock.getDateEntree().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    dateLabel.getStyleClass().add("stock-detail");

                    Label warehouseLabel = new Label("🏭 " + formatEntrepots(stock.getEntrepotIds()));
                    warehouseLabel.getStyleClass().add("stock-detail");

                    detailsBox.getChildren().addAll(quantityLabel, dateLabel, warehouseLabel);

                    // Seuil d'alerte
                    Label alertLabel = new Label();
                    if (stock.getProduit() != null && stock.getProduit().getQuantite() < stock.getSeuilAlert()) {
                        alertLabel.setText("⚠ Stock critique (" + stock.getSeuilAlert() + ")");
                        alertLabel.getStyleClass().add("stock-alert-critical");
                    } else {
                        alertLabel.setText("Seuil: " + stock.getSeuilAlert());
                        alertLabel.getStyleClass().add("stock-alert-normal");
                    }

                    infoBox.getChildren().addAll(titleBox, detailsBox, alertLabel);

                    // Conteneur des actions
                    HBox actionBox = new HBox(10);
                    actionBox.setAlignment(Pos.CENTER_RIGHT);

                    Button editBtn = new Button("✏ Modifier");
                    editBtn.getStyleClass().add("action-button");
                    editBtn.setOnAction(e -> editStock(stock));

                    Button deleteBtn = new Button("🗑 Supprimer");
                    deleteBtn.getStyleClass().add("action-button-danger");
                    deleteBtn.setOnAction(e -> deleteStock(stock));

                    actionBox.getChildren().addAll(editBtn, deleteBtn);

                    mainContainer.getChildren().addAll(imageView, infoBox, actionBox);

                    // Hover effect
                    mainContainer.setTranslateY(20);
                    mainContainer.setOpacity(0);
                    Timeline timeline = new Timeline(
                            new KeyFrame(Duration.millis(300), new KeyValue(mainContainer.translateYProperty(), 0)),
                            new KeyFrame(Duration.millis(300), new KeyValue(mainContainer.opacityProperty(), 1))
                    );
                    timeline.play();

                    // Tooltip
                    Tooltip tooltip = new Tooltip();
                    String tooltipText = String.format(
                            "Produit: %s\nQuantite: %s\nSeuil d'alerte: %d",
                            stock.getProduit() != null && stock.getProduit().getNom() != null ? stock.getProduit().getNom() : "Inconnu",
                            stock.getProduit() != null && stock.getProduit().getQuantite() != 0 ? stock.getProduit().getQuantite() : "Inconnu",
                            stock.getSeuilAlert() != null ? stock.getSeuilAlert() : 0
                    );
                    tooltip.setText(tooltipText);
                    tooltip.setStyle("-fx-text-fill:white; -fx-font-size: 14px;");
                    tooltip.setShowDelay(Duration.millis(200));
                    Tooltip.install(mainContainer, tooltip);

                    // Context Menu
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem editItem = new MenuItem("Modifier");
                    editItem.setOnAction(e -> editStock(stock));
                    MenuItem deleteItem = new MenuItem("Supprimer");
                    deleteItem.setOnAction(e -> deleteStock(stock));
                    MenuItem detailsItem = new MenuItem("Voir détails");
                    detailsItem.setOnAction(e -> showStockDetails(stock));
                    contextMenu.getItems().addAll(editItem, deleteItem, detailsItem);
                    setContextMenu(contextMenu);

                    // Double-click to edit
                    mainContainer.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2) {
                            editStock(stock);
                        }
                    });

                    // Accessibility
                    mainContainer.setAccessibleText(String.format(
                            "Produit: %s, Quantité: %d, Date Entrée: %s, Seuil: %d",
                            stock.getProduit() != null ? stock.getProduit().getNom() : "Inconnu",
                            stock.getProduit() != null ? stock.getProduit().getQuantite() : 0,
                            stock.getDateEntree().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            stock.getSeuilAlert()
                    ));

                    // Conditional styling for critical stock
                    if (stock.getProduit() != null && stock.getProduit().getQuantite() < stock.getSeuilAlert()) {
                        mainContainer.getStyleClass().add("stock-cell-critical");
                    }

                    setGraphic(mainContainer);
                }
            }
        });

        stockList.setItems(stockData);
        stockList.getStyleClass().add("stock-list");
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

        stockList.setItems(FXCollections.observableArrayList(filteredList));
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
            dialogStage.setOnHidden(e -> refreshStockData());
            dialogStage.setResizable(false);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la fenêtre de modification", Alert.AlertType.ERROR);
        }
    }

    private void deleteStock(Stock stock) {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirmation de suppression");
        confirmationAlert.setHeaderText("Supprimer le stock");
        confirmationAlert.setContentText("Êtes-vous sûr de vouloir supprimer le stock ?");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = stockService.deleteStock(stock.getId());

            Alert resultAlert = new Alert(
                    success ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR
            );
            resultAlert.setTitle(success ? "Succès" : "Erreur");
            resultAlert.setHeaderText(null);
            resultAlert.setContentText(
                    success
                            ? "Le stock a été supprimé avec succès."
                            : "Échec de la suppression du stock."
            );
            resultAlert.showAndWait();

            if (success) {
                refreshStockData();
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
            dialogStage.setOnHidden(e -> refreshStockData());
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'ajout", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleExportExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 1. Configuration initiale
            Sheet sheet = workbook.createSheet("Analyse Stocks");
            sheet.setDisplayGridlines(false);

            // 2. Création des styles
            Map<String, CellStyle> styles = createExcelStyles(workbook);

            // 3. En-tête avec logo et infos société
            createCompanyHeader(workbook, sheet, styles);

            // 4. Titre et période du rapport
            createReportTitle(sheet, styles);

            // 5. Tableau de données principal
            int dataStartRow = createMainDataTable(workbook, sheet, styles);

            // 6. Section d'analyse avancée
            createAdvancedAnalysisSection(workbook, sheet, dataStartRow);

            // 7. Graphique des stocks
            createStockChart(workbook, sheet, dataStartRow);

            // 8. Ajout des logos
            addCompanyLogos(workbook, sheet);

            // 9. Sauvegarde du fichier
            saveExcelFile(workbook);

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur d'export",
                    "Échec de la génération du fichier Excel: " + e.getMessage());
        }
    }

// Méthodes auxiliaires:

    private Map<String, CellStyle> createExcelStyles(XSSFWorkbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();

        // Style de fond
        CellStyle bgStyle = workbook.createCellStyle();
        bgStyle.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        bgStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("background", bgStyle);

        // Style de titre principal
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short)18);
        titleFont.setColor(IndexedColors.DARK_BLUE.getIndex());
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        styles.put("title", titleStyle);

        // Style d'en-tête de tableau
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        styles.put("header", headerStyle);

        // Style de cellule avec bordure
        CellStyle borderedStyle = workbook.createCellStyle();
        borderedStyle.setBorderBottom(BorderStyle.THIN);
        borderedStyle.setBorderTop(BorderStyle.THIN);
        borderedStyle.setBorderLeft(BorderStyle.THIN);
        borderedStyle.setBorderRight(BorderStyle.THIN);
        styles.put("bordered", borderedStyle);

        // Style de cellule alternée
        CellStyle altStyle = workbook.createCellStyle();
        altStyle.cloneStyleFrom(borderedStyle);
        altStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        altStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        styles.put("alt", altStyle);

        // Style d'alerte
        CellStyle alertStyle = workbook.createCellStyle();
        alertStyle.cloneStyleFrom(borderedStyle);
        Font alertFont = workbook.createFont();
        alertFont.setColor(IndexedColors.RED.getIndex());
        alertFont.setBold(true);
        alertStyle.setFont(alertFont);
        styles.put("alert", alertStyle);

        return styles;
    }

    private void createCompanyHeader(XSSFWorkbook workbook, Sheet sheet, Map<String, CellStyle> styles) throws IOException {
        // Logo gauche
        try (InputStream is = getClass().getResourceAsStream("/icons/logo.jpg")) {
            if (is != null) {
                byte[] bytes = IOUtils.toByteArray(is);
                int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);

                CreationHelper helper = workbook.getCreationHelper();
                Drawing drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(0);
                anchor.setRow1(0);
                drawing.createPicture(anchor, pictureIdx);
            }
        }

        // Titre société
        Row companyRow = sheet.createRow(0);
        companyRow.setHeightInPoints(30);
        Cell companyCell = companyRow.createCell(1);
        companyCell.setCellValue("AGRIPLANER");
        companyCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, 4));

        // Coordonnées
        Row contactRow = sheet.createRow(1);
        contactRow.createCell(1).setCellValue("Tél: +216 71 000 000 | Email: contact@agriplaner.tn");
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 4));
    }

    private void createReportTitle(Sheet sheet, Map<String, CellStyle> styles) {
        Row titleRow = sheet.createRow(3);
        titleRow.setHeightInPoints(25);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("RAPPORT D'ANALYSE DES STOCKS");
        titleCell.setCellStyle(styles.get("title"));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 5));

        Row dateRow = sheet.createRow(4);
        dateRow.createCell(0).setCellValue("Période: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        sheet.addMergedRegion(new CellRangeAddress(4, 4, 0, 2));
    }

    private int createMainDataTable(XSSFWorkbook workbook, Sheet sheet, Map<String, CellStyle> styles) {
        int startRow = 6;

        // En-têtes du tableau
        Row headerRow = sheet.createRow(startRow);
        String[] headers = {"Produit", "Quantité", "Prix Unitaire", "Valeur", "Catégorie", "Statut"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.get("header"));
            sheet.setColumnWidth(i, (i == 0 || i == 4) ? 25*256 : 15*256);
        }

        // Données
        boolean alternate = false;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int i = 0; i < stockList.getItems().size(); i++) {
            Stock stock = stockList.getItems().get(i);
            Row row = sheet.createRow(startRow + 1 + i);

            CellStyle style = alternate ? styles.get("alt") : styles.get("bordered");
            alternate = !alternate;

            // Produit
            row.createCell(0).setCellValue(stock.getProduit().getNom());
            row.getCell(0).setCellStyle(style);

            // Quantité
            row.createCell(1).setCellValue(stock.getProduit().getQuantite());
            row.getCell(1).setCellStyle(style);

            // Prix unitaire
            row.createCell(2).setCellValue(stock.getProduit().getPrixUnitaire());
            row.getCell(2).setCellStyle(style);

            // Valeur (formule quantité * prix)
            row.createCell(3).setCellFormula("B"+(startRow+2+i)+"*C"+(startRow+2+i));
            row.getCell(3).setCellStyle(style);

            // Catégorie
            row.createCell(4).setCellValue(stock.getProduit().getCategory().getNom());
            row.getCell(4).setCellStyle(style);

            // Statut (avec format conditionnel)
            Cell statusCell = row.createCell(5);
            if (stock.getProduit().getQuantite() < stock.getSeuilAlert()) {
                statusCell.setCellValue("CRITIQUE");
                statusCell.setCellStyle(styles.get("alert"));
            } else {
                statusCell.setCellValue("OK");
                statusCell.setCellStyle(style);
            }
        }

        return startRow;
    }

    private void createAdvancedAnalysisSection(XSSFWorkbook workbook, Sheet sheet, int dataStartRow) {
        int analysisRow = dataStartRow + stockList.getItems().size() + 3;

        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle boldStyle = createBoldStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle borderedStyle = createBorderedStyle(workbook);

        // Titre section
        Row sectionRow = sheet.createRow(analysisRow);
        Cell titleCell = sectionRow.createCell(0);
        titleCell.setCellValue("ANALYSE AVANCÉE");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(analysisRow, analysisRow, 0, 5));

        // Statistiques principales (KPIs)
        String[] labels = {"Stock total:", "Valeur totale:", "Produits critiques:", "Valeur moyenne:"};
        String[] formulas = {
                "SUM(B" + (dataStartRow + 2) + ":B" + (dataStartRow + 1 + stockList.getItems().size()) + ")",
                "SUM(D" + (dataStartRow + 2) + ":D" + (dataStartRow + 1 + stockList.getItems().size()) + ")",
                "COUNTIF(F" + (dataStartRow + 2) + ":F" + (dataStartRow + 1 + stockList.getItems().size()) + ",\"CRITIQUE\")",
                "AVERAGE(D" + (dataStartRow + 2) + ":D" + (dataStartRow + 1 + stockList.getItems().size()) + ")"
        };

        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(analysisRow + 1 + i);
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue(labels[i]);
            labelCell.setCellStyle(boldStyle);
            Cell formulaCell = row.createCell(1);
            formulaCell.setCellFormula(formulas[i]);
        }

        // Tableau de synthèse par catégorie
        createCategorySummaryTable(workbook, sheet, analysisRow + 6, headerStyle, borderedStyle, boldStyle);
    }


    private void createCategorySummaryTable(XSSFWorkbook workbook, Sheet sheet, int startRow, CellStyle headerStyle, CellStyle dataStyle, CellStyle boldStyle) {
        Row headerRow = sheet.createRow(startRow);
        String[] headers = {"Catégorie", "Nb Produits", "Stock Total", "Valeur Moyenne"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        Map<String, Double[]> categoryData = calculateCategoryStats();
        int currentRow = startRow + 1;

        for (Map.Entry<String, Double[]> entry : categoryData.entrySet()) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue()[0]);
            row.createCell(2).setCellValue(entry.getValue()[1]);
            row.createCell(3).setCellValue(entry.getValue()[2]);

            for (int i = 0; i < 4; i++) {
                row.getCell(i).setCellStyle(dataStyle);
            }
        }

        // Ligne total
        Row totalRow = sheet.createRow(currentRow);
        Cell totalCell = totalRow.createCell(0);
        totalCell.setCellValue("TOTAL");
        totalCell.setCellStyle(boldStyle);

        totalRow.createCell(1).setCellFormula("SUM(B" + (startRow + 2) + ":B" + currentRow + ")");
        totalRow.createCell(2).setCellFormula("SUM(C" + (startRow + 2) + ":C" + currentRow + ")");
        totalRow.createCell(3).setCellFormula("AVERAGE(D" + (startRow + 2) + ":D" + currentRow + ")");

        for (int i = 0; i < 4; i++) {
            totalRow.getCell(i).setCellStyle(dataStyle);
        }
    }
    private CellStyle createTitleStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 14);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createBoldStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBorderedStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }


    private Map<String, Double[]> calculateCategoryStats() {
            // Implémentation simplifiée - à adapter avec vos données réelles
            Map<String, Double[]> stats = new LinkedHashMap<>();

            // Exemple de données statiques
            stats.put("Electronique", new Double[]{15.0, 450.0, 125.5});
            stats.put("Alimentaire", new Double[]{32.0, 890.0, 45.8});
            stats.put("Textile", new Double[]{22.0, 320.0, 75.2});

            return stats;
        }

    private void createStockChart(XSSFWorkbook workbook, Sheet sheet, int dataStartRow) {
        try {
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 6, 1, 12, 15);

            XSSFChart chart = ((XSSFDrawing) drawing).createChart(anchor);

            // Légende du graphique
            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.BOTTOM);

            // Axes
            XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
            leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            // Données
            XDDFDataSource<String> products = XDDFDataSourcesFactory.fromStringCellRange(
                    (XSSFSheet) sheet,
                    new CellRangeAddress(dataStartRow + 1, dataStartRow + stockList.getItems().size(), 0, 0));

            XDDFNumericalDataSource<Double> quantities = XDDFDataSourcesFactory.fromNumericCellRange(
                    (XSSFSheet) sheet,
                    new CellRangeAddress(dataStartRow + 1, dataStartRow + stockList.getItems().size(), 1, 1));

            // Données de graphique
            XDDFLineChartData data = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) data.addSeries(products, quantities);
            series.setTitle("Quantité en stock", null);

            // Affichage du graphique
            chart.plot(data);

            // Définir un titre (ajout manuel avec paragraphes si vraiment nécessaire)
            chart.setTitleText("Niveaux de Stock");
            chart.setTitleOverlay(false);

        } catch (Exception e) {
            System.err.println("Erreur création graphique: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addCompanyLogos(XSSFWorkbook workbook, Sheet sheet) throws IOException {
        // Logo droit (ESPRIT)
        try (InputStream is = getClass().getResourceAsStream("/icons/esprit.jpg")) {
            if (is != null) {
                byte[] bytes = IOUtils.toByteArray(is);
                int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG);

                CreationHelper helper = workbook.getCreationHelper();
                Drawing drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = helper.createClientAnchor();
                anchor.setCol1(5);
                anchor.setRow1(0);
                drawing.createPicture(anchor, pictureIdx);
            }
        }
    }

    private void saveExcelFile(XSSFWorkbook workbook) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport Excel");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"));
        fileChooser.setInitialFileName("rapport_stocks_" + LocalDate.now() + ".xlsx");

        File file = fileChooser.showSaveDialog(excelBtn.getScene().getWindow());
        if (file != null) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }
            showAlert(Alert.AlertType.INFORMATION, "Export réussi",
                    "Le rapport Excel a été généré avec succès!");

            // Ouvrir le fichier automatiquement
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Méthodes utilitaires pour le PDF
    private void drawTableHeader(PDPageContentStream contentStream, float margin, float yStart,
                                 float rowHeight, String[] headers, float[] columnWidths) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, 11);
        contentStream.setNonStrokingColor(33, 33, 33);
        contentStream.setLineWidth(0.5f);

        float nextX = margin;
        for (int i = 0; i < headers.length; i++) {
            // Fond bleu pour l'en-tête
            contentStream.setNonStrokingColor(25, 118, 210);
            contentStream.addRect(nextX, yStart - rowHeight, columnWidths[i], rowHeight);
            contentStream.fill();

            // Texte en blanc
            contentStream.setNonStrokingColor(255, 255, 255);
            contentStream.beginText();
            contentStream.newLineAtOffset(nextX + 5, yStart - rowHeight + 5);
            contentStream.showText(headers[i]);
            contentStream.endText();

            nextX += columnWidths[i];
        }
    }
    private float getTotalWidth(float[] columnWidths) {
        float total = 0;
        for (float width : columnWidths) {
            total += width;
        }
        return total;
    }



    private void showStockDetails(Stock stock) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du Stock");
        alert.setHeaderText(stock.getProduit() != null ? stock.getProduit().getNom() : "Produit inconnu");
        String details = String.format(
                "Quantité: %d\nDate Entrée: %s\nEntrepôts: %s\nSeuil d'alerte: %d\nDescription: %s",
                stock.getProduit() != null ? stock.getProduit().getQuantite() : 0,
                stock.getDateEntree().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                formatEntrepots(stock.getEntrepotIds()),
                stock.getSeuilAlert(),
                stock.getProduit() != null && stock.getProduit().getDescription() != null ?
                        stock.getProduit().getDescription() : "Aucune"
        );
        alert.setContentText(details);
        alert.showAndWait();
    }

    private void sortListByName() {
        List<Stock> sortedList = stockData.stream()
                .sorted(Comparator.comparing(s -> s.getProduit() != null ? s.getProduit().getNom().toLowerCase() : ""))
                .collect(Collectors.toList());
        stockList.setItems(FXCollections.observableArrayList(sortedList));
    }

    private void sortListByQuantity() {
        List<Stock> sortedList = stockData.stream()
                .sorted(Comparator.comparingInt(s -> s.getProduit() != null ? s.getProduit().getQuantite() : 0))
                .collect(Collectors.toList());
        stockList.setItems(FXCollections.observableArrayList(sortedList));
    }

    private void sortListByDate() {
        List<Stock> sortedList = stockData.stream()
                .sorted(Comparator.comparing(Stock::getDateEntree))
                .collect(Collectors.toList());
        stockList.setItems(FXCollections.observableArrayList(sortedList));
    }

    @FXML
    private WebView mapView;

    @FXML
    private ProgressIndicator mapLoadingIndicator;

    @FXML
    private void showWarehouseMap() {
        try {
            // Récupérer la liste des entrepôts
            List<Entrepot> entrepots = entrepotService.getAllEntrepots();

            // Générer le contenu HTML avec les marqueurs
            String mapHtml = getMapHtmlWithMarkers(entrepots);

            // Configuration du WebView
            WebEngine engine = mapView.getEngine();
            engine.setJavaScriptEnabled(true);

            // Paramètres de dimensionnement
            mapView.setPrefSize(1200, 700);
            mapView.setMinSize(800, 500);

            // Charger le contenu HTML
            engine.loadContent(mapHtml);

            // Gestion du chargement
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    Platform.runLater(() -> {
                        // Redimensionner la carte après le chargement
                        engine.executeScript("if (typeof map !== 'undefined') map.invalidateSize(true);");
                    });
                } else if (newState == Worker.State.FAILED) {
                    showAlert("Erreur", "Échec du chargement de la carte.", Alert.AlertType.ERROR);
                }
            });

            // Gestion du redimensionnement de la WebView
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
            System.err.println("Erreur critique: " + e.getMessage());
            showAlert("Erreur", "Impossible d'afficher la carte", Alert.AlertType.ERROR);
        }
    }

    private String getMapHtmlWithMarkers(List<Entrepot> entrepots) {
        // Construire la liste des marqueurs en JavaScript
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

        // Générer le contenu HTML complet avec les marqueurs
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
                // Configuration avancée de la carte
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

                // Couche de tuiles avec paramètres optimisés
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

                // Ajouter les marqueurs avec tooltips, popups et événements de clic
                """ + markersScript + """
                markers.forEach(function(markerData) {
                    var marker = L.marker([markerData.lat, markerData.lng]).addTo(map);
                    // Tooltip affiché au survol
                    marker.bindTooltip(markerData.nom, {
                        offset: [0, -10],
                        direction: 'top'
                    });
                    // Popup détaillé sur clic
                    marker.bindPopup(
                        '<b>' + markerData.nom + '</b><br>' +
                        'Adresse: ' + markerData.adresse + '<br>' +
                        'Ville: ' + markerData.ville
                    );
                    // Zoom sur l'entrepôt lors du clic
                    marker.on('click', function() {
                        map.flyTo([markerData.lat, markerData.lng], 15, {
                            animate: true,
                            duration: 1
                        });
                    });
                });

                // Ajuster la vue pour englober tous les marqueurs si présents
                if (markers.length > 0) {
                    var bounds = L.latLngBounds(markers.map(function(m) { return [m.lat, m.lng]; }));
                    map.fitBounds(bounds, { padding: [20, 20] });
                } else {
                    map.setView([36.8065, 10.1815], 7); // Vue par défaut
                }

                // Fonction de redimensionnement forcé
                function forceMapResize() {
                    setTimeout(function() {
                        map.invalidateSize(true);
                    }, 300);
                }

                // Redimensionnement initial
                forceMapResize();

                // Écouteurs de redimensionnement
                window.addEventListener('resize', forceMapResize);
                document.addEventListener('visibilitychange', forceMapResize);
            </script>
        </body>
        </html>
        """;

        return mapHtml;
    }






        @FXML
        private void handleExportPDF() {
            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                // 1. Configuration initiale
                PDPageContentStream content = new PDPageContentStream(document, page);
                float margin = 30;
                float y = PDRectangle.A4.getHeight() - margin;
                float lineHeight = 15;
                float rowHeight = 20;

                // 2. En-tête avec logo
                y = drawHeader(document, content, margin, y);

                // 3. Titre du rapport
                y = drawTitle(content, margin, y);

                // 4. Tableau principal (6 colonnes)
                String[] headers = {"Produit", "Quantité", "Prix Unitaire", "Valeur", "Catégorie", "Statut"};
                float[] widths = {120f, 60f, 80f, 80f, 100f, 60f};
                y = drawMainTable(content, margin, y, rowHeight, headers, widths);

                // 5. Disposition en 2 colonnes
                float col1X = margin;
                float col2X = PDRectangle.A4.getWidth()/2 + 10;
                float colWidth = (PDRectangle.A4.getWidth() - 3*margin)/2;

                // 6. Analyse avancée (colonne gauche)
                y = drawAdvancedAnalysis(content, col1X, y - 30, colWidth);

                // 7. Graphique (colonne droite)
                float chartWidth = PDRectangle.A4.getWidth() - 2*margin;
                float chartHeight = 200;
                float chartY = y - chartHeight - 20; // Position sous l'analyse

                drawStockChart(document, page, stockList.getItems(), margin, chartY, chartWidth, chartHeight);

                // 8. Synthèse par catégorie sous le graphique
                y = chartY - 20;
                drawCategorySummary(content, col1X, y-20, colWidth);

                // 9. Pied de page
                drawFooter(content,margin);

                content.close();
                savePDF(document);
            } catch (Exception e) {
                showErrorAlert("Erreur lors de la génération PDF", e.getMessage());
            }
        }

        private float drawHeader(PDDocument doc, PDPageContentStream content, float margin, float y) throws IOException {
            // Logo gauche
            try {
                PDImageXObject logo = PDImageXObject.createFromFile("src/main/resources/icons/logo.jpg", doc);
                content.drawImage(logo, margin, y - 40, 40, 40);
            } catch (IOException e) {
                System.err.println("Logo gauche non trouvé");
            }

            // Logo droit
            try {
                PDImageXObject logo = PDImageXObject.createFromFile("src/main/resources/icons/esprit.jpg", doc);
                content.drawImage(logo, PDRectangle.A4.getWidth() - margin - 40, y - 40, 40, 40);
            } catch (IOException e) {
                System.err.println("Logo droit non trouvé");
            }

            // Titre entreprise
            content.setFont(PDType1Font.HELVETICA_BOLD, 18);
            content.setNonStrokingColor(25, 118, 210); // Bleu
            drawText(content, "AGRIPLANER", margin + 50, y - 20);

            // Coordonnées
            content.setFont(PDType1Font.HELVETICA, 10);
            content.setNonStrokingColor(66, 66, 66); // Gris
            drawText(content, "Tél: +216 71 000 000 | Email: contact@agriplaner.tn", margin + 50, y - 35);

            // Ligne de séparation
            content.setLineWidth(1f);
            content.setStrokingColor(25, 118, 210);
            content.moveTo(margin, y - 50);
            content.lineTo(PDRectangle.A4.getWidth() - margin, y - 50);
            content.stroke();

            return y - 70;
        }

        private float drawTitle(PDPageContentStream content, float margin, float y) throws IOException {
            content.setFont(PDType1Font.HELVETICA_BOLD, 20);
            content.setNonStrokingColor(0, 150, 136); // Vert-bleu
            drawText(content, "RAPPORT D'ANALYSE DES STOCKS", margin, y);

            content.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
            content.setNonStrokingColor(120, 120, 120);
            drawText(content, "Période: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), margin, y - 20);

            return y - 40;
        }

        private float drawMainTable(PDPageContentStream content, float margin, float y, float rowHeight,
                                    String[] headers, float[] widths) throws IOException {
            // En-tête
            drawTableHeader(content, margin, y, rowHeight, headers, widths);

            // Données
            float currentY = y - rowHeight;
            boolean alternate = false;

            for (Stock stock : stockList.getItems()) {
                Produit p = stock.getProduit();

                // Fond alterné
                content.setNonStrokingColor(alternate ? new Color(245, 247, 250) : Color.WHITE);
                content.addRect(margin, currentY, getTotalWidth(widths), rowHeight);
                content.fill();

                // Données
                content.setNonStrokingColor(Color.BLACK);
                content.setFont(PDType1Font.HELVETICA, 10);

                float x = margin + 5;
                drawText(content, p != null ? p.getNom() : "N/A", x, currentY + 5);
                x += widths[0];

                drawText(content, String.valueOf(p != null ? p.getQuantite() : 0), x, currentY + 5);
                x += widths[1];

                drawText(content, String.format("%.2f", p != null ? p.getPrixUnitaire() : 0), x, currentY + 5);
                x += widths[2];

                double value = p != null ? p.getQuantite() * p.getPrixUnitaire() : 0;
                drawText(content, String.format("%.2f", value), x, currentY + 5);
                x += widths[3];

                drawText(content, p != null && p.getCategory() != null ? p.getCategory().getNom() : "N/A", x, currentY + 5);
                x += widths[4];

                // Statut avec couleur conditionnelle
                if (p != null && p.getQuantite() < stock.getSeuilAlert()) {
                    content.setNonStrokingColor(Color.RED);
                    drawText(content, "CRITIQUE", x, currentY + 5);
                } else {
                    content.setNonStrokingColor(new Color(0, 128, 0));
                    drawText(content, "OK", x, currentY + 5);
                }

                // Bordures
                content.setStrokingColor(200, 200, 200);
                content.addRect(margin, currentY, getTotalWidth(widths), rowHeight);
                content.stroke();

                currentY -= rowHeight;
                alternate = !alternate;
            }

            return currentY;
        }

        private float drawAdvancedAnalysis(PDPageContentStream content, float x, float y, float width) throws IOException {
            // Titre section
            content.setFont(PDType1Font.HELVETICA_BOLD, 14);
            content.setNonStrokingColor(0, 150, 136);
            drawText(content, "ANALYSE AVANCÉE", x, y);
            y -= 25;

            // Calcul des stats
            double totalStock = stockList.getItems().stream()
                    .mapToDouble(s -> s.getProduit() != null ? s.getProduit().getQuantite() : 0)
                    .sum();

            double totalValue = stockList.getItems().stream()
                    .mapToDouble(s -> s.getProduit() != null ?
                            s.getProduit().getQuantite() * s.getProduit().getPrixUnitaire() : 0)
                    .sum();

            long critical = stockList.getItems().stream()
                    .filter(s -> s.getProduit() != null && s.getProduit().getQuantite() < s.getSeuilAlert())
                    .count();

            double avgValue = stockList.getItems().isEmpty() ? 0 : totalValue / stockList.getItems().size();

            // Affichage
            content.setFont(PDType1Font.HELVETICA, 10);
            content.setNonStrokingColor(Color.BLACK);

            drawText(content, String.format("Stock total: %.0f unités", totalStock), x, y);
            drawText(content, String.format("Valeur totale: %.2f DT", totalValue), x, y - 20);
            drawText(content, String.format("Produits critiques: %d", critical), x, y - 40);
            drawText(content, String.format("Valeur moyenne: %.2f DT", avgValue), x, y - 60);

            return y - 80;
        }

        private void drawStockChart(PDDocument doc, PDPage page, List<Stock> stocks,
                                    float x, float y, float w, float h) throws IOException {
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            stocks.forEach(s -> {
                if (s.getProduit() != null) {
                    dataset.addValue(s.getProduit().getQuantite(), "Stock", s.getProduit().getNom());
                    dataset.addValue(s.getSeuilAlert(), "Seuil", s.getProduit().getNom());
                }
            });

            JFreeChart chart = ChartFactory.createBarChart(
                    null, // Pas de titre pour gagner de la place
                    "Produits",
                    "Quantité",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, // Légende
                    false,
                    false
            );

            // Style
            CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeGridlinePaint(new Color(220, 220, 220));

            BarRenderer renderer = (BarRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, new Color(79, 129, 189)); // Bleu stock
            renderer.setSeriesPaint(1, new Color(192, 80, 77));  // Rouge seuil

            // Dessin
            BufferedImage img = new BufferedImage((int)w, (int)h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            chart.draw(g, new Rectangle2D.Double(0, 0, w, h));
            g.dispose();

            // Insertion PDF
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "PNG", baos);
            PDImageXObject pdImg = PDImageXObject.createFromByteArray(doc, baos.toByteArray(), "chart");

            try (PDPageContentStream content = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true)) {
                content.drawImage(pdImg, x, y, w, h);
            }
        }

        private float drawCategorySummary(PDPageContentStream content, float x, float y, float width) throws IOException {
            // Titre
            content.setFont(PDType1Font.HELVETICA_BOLD, 14);
            content.setNonStrokingColor(0, 150, 136);
            drawText(content, "SYNTHÈSE PAR CATÉGORIE", x, y);
            y -= 25;

            // En-têtes
            String[] headers = {"Catégorie", "Nb Produits", "Stock", "Moyenne"};
            float[] widths = {width*0.4f, width*0.2f, width*0.2f, width*0.2f};
            drawTableHeader(content, x, y, 20, headers, widths);
            y -= 20;

            // Données
            Map<String, Double[]> stats = calculateCategoryStats();
            boolean alternate = false;
            content.setFont(PDType1Font.HELVETICA, 10);

            for (Map.Entry<String, Double[]> entry : stats.entrySet()) {
                // Fond alterné
                content.setNonStrokingColor(alternate ? new Color(245, 247, 250) : Color.WHITE);
                content.addRect(x, y, getTotalWidth(widths), 20);
                content.fill();

                // Données
                content.setNonStrokingColor(Color.BLACK);
                float colX = x + 5;
                drawText(content, entry.getKey(), colX, y + 5);
                colX += widths[0];

                drawText(content, String.format("%.0f", entry.getValue()[0]), colX, y + 5);
                colX += widths[1];

                drawText(content, String.format("%.0f", entry.getValue()[1]), colX, y + 5);
                colX += widths[2];

                drawText(content, String.format("%.2f", entry.getValue()[2]), colX, y + 5);

                // Bordures
                content.setStrokingColor(200, 200, 200);
                content.addRect(x, y, getTotalWidth(widths), 20);
                content.stroke();

                y -= 20;
                alternate = !alternate;
            }

            return y;
        }

        private void drawFooter(PDPageContentStream content, float margin) throws IOException {
            content.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
            content.setNonStrokingColor(120, 120, 120);
            drawText(content, "© " + LocalDate.now().getYear() + " Agriplaner - Généré le " +
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    margin, margin);
        }



        private void drawText(PDPageContentStream content, String text, float x, float y) throws IOException {
            content.beginText();
            content.newLineAtOffset(x, y);
            content.showText(text);
            content.endText();
        }





        private void savePDF(PDDocument doc) throws IOException {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le rapport");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fileChooser.setInitialFileName("rapport_stocks_" +
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf");

            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                doc.save(file);
                Desktop.getDesktop().open(file);
            }
        }

        private void showErrorAlert(String title, String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
