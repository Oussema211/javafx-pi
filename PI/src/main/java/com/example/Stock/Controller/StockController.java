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
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.animation.FadeTransition;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
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
    @FXML private ListView<Stock> stockList;
    @FXML private Button sortByNameBtn;
    @FXML private Button sortByQuantityBtn;
    @FXML private Button sortByDateBtn;
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
                    // Structured tooltip content with product name, category, and alert threshold
                    String tooltipText = String.format(
                            "Produit: %s\nQuantite: %s\nSeuil d'alerte: %d",
                            stock.getProduit() != null && stock.getProduit().getNom() != null ? stock.getProduit().getNom() : "Inconnu",
                            stock.getProduit() != null && stock.getProduit().getQuantite() != 0 ? stock.getProduit().getQuantite() : "Inconnu",
                            stock.getSeuilAlert() != null ? stock.getSeuilAlert() : 0
                    );
                    tooltip.setText(tooltipText);

                    // Customize tooltip style for better readability
                    tooltip.setStyle(

                            "-fx-text-fill:white; " +       // Dark text for high contrast
                                    "-fx-font-size: 14px; "
                    );
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
                // Rafraîchir les données directement
                refreshStockData(); // Ajoutez cet appel
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
            Sheet sheet = workbook.createSheet("Stocks");

            // 1. Création du style d'en-tête avec POI
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 2. Création des en-têtes avec POI
            String[] headers = {"Produit", "Quantité", "Date Entrée", "Date Sortie", "Entrepôts", "Seuil"};
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 3. Remplissage des données
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (int i = 0; i < stockList.getItems().size(); i++) {
                Stock stock = stockList.getItems().get(i);
                if (stock == null) continue;

                Row row = sheet.createRow(i + 1);
                Produit p = stock.getProduit();

                // Produit
                org.apache.poi.ss.usermodel.Cell produitCell = row.createCell(0);
                produitCell.setCellValue(p != null ? p.getNom() : "Inconnu");

                // Quantité
                org.apache.poi.ss.usermodel.Cell quantiteCell = row.createCell(1);
                quantiteCell.setCellValue(p != null ? p.getQuantite() : 0);

                // Date Entrée
                org.apache.poi.ss.usermodel.Cell dateEntreeCell = row.createCell(2);
                dateEntreeCell.setCellValue(stock.getDateEntree().format(dateFormatter));

                // Date Sortie
                org.apache.poi.ss.usermodel.Cell dateSortieCell = row.createCell(3);
                dateSortieCell.setCellValue(stock.getDateSortie() != null ?
                        stock.getDateSortie().format(dateFormatter) : "N/A");

                // Entrepôts
                org.apache.poi.ss.usermodel.Cell entrepotCell = row.createCell(4);
                entrepotCell.setCellValue(formatEntrepots(stock.getEntrepotIds()));

                // Seuil
                org.apache.poi.ss.usermodel.Cell seuilCell = row.createCell(5);
                seuilCell.setCellValue(stock.getSeuilAlert());
            }

            // 4. Sauvegarde du fichier
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le rapport Excel");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"));
            fileChooser.setInitialFileName("export_stocks_" + LocalDate.now() + ".xlsx");

            File file = fileChooser.showSaveDialog(excelBtn.getScene().getWindow());
            if (file != null) {
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    workbook.write(outputStream);

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Export réussi");
                    alert.setHeaderText(null);
                    alert.setContentText("Le fichier Excel a été généré avec succès !");
                    alert.showAndWait();
                }
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur d'export");
            alert.setHeaderText("Échec de la génération du fichier Excel");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExportPDF() {
        try {
            // 1. Configuration du document
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // 2. Style d'entreprise
            String nomEntreprise = "Agriplaner";
            String telephone = "+216 71 000 000";
            String email = "ContacteInfo@Agriplaner.tn";

            // 3. Fond coloré (dégradé)
            contentStream.setNonStrokingColor(240, 248, 255); // Bleu clair
            contentStream.addRect(0, 0, page.getMediaBox().getWidth(), page.getMediaBox().getHeight());
            contentStream.fill();

            // 4. En-tête avec logos
            PDImageXObject logoLeft = PDImageXObject.createFromFile("src/main/resources/icons/logo.jpg", document);
            contentStream.drawImage(logoLeft, 30, 750, 40, 40);

            PDImageXObject logoRight = PDImageXObject.createFromFile("src/main/resources/icons/esprit.jpg", document);
            contentStream.drawImage(logoRight, 520, 750, 40, 40);

            // 5. Titres et informations
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.setNonStrokingColor(25, 118, 210); // Bleu
            contentStream.beginText();
            contentStream.newLineAtOffset(210, 780);
            contentStream.showText(nomEntreprise);
            contentStream.endText();

            contentStream.setFont(PDType1Font.HELVETICA, 10);
            contentStream.setNonStrokingColor(66, 66, 66); // Gris
            contentStream.beginText();
            contentStream.newLineAtOffset(210, 760);
            contentStream.showText("Téléphone: " + telephone);
            contentStream.endText();

            contentStream.beginText();
            contentStream.newLineAtOffset(210, 745);
            contentStream.showText("Email: " + email);
            contentStream.endText();

            // 6. Ligne décorative
            contentStream.setLineWidth(1f);
            contentStream.setStrokingColor(25, 118, 210);
            contentStream.moveTo(30, 730);
            contentStream.lineTo(560, 730);
            contentStream.stroke();

            // 7. Titre du rapport
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
            contentStream.setNonStrokingColor(0, 150, 136); // Vert-bleu
            contentStream.beginText();
            contentStream.newLineAtOffset(210, 700);
            contentStream.showText("Rapport des Stocks");
            contentStream.endText();

            contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 10);
            contentStream.setNonStrokingColor(120, 120, 120);
            contentStream.beginText();
            contentStream.newLineAtOffset(210, 680);
            contentStream.showText("Généré le: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
            contentStream.endText();

            // 8. En-tête du tableau
            float margin = 30;
            float yStart = 650;
            float rowHeight = 20;

            String[] headers = {"Produit", "Quantité", "Date Entrée", "Date Sortie", "Entrepôts", "Seuil"};
            float[] columnWidths = {150f, 50f, 80f, 80f, 120f, 50f};

            // Calcul de la largeur totale
            float totalWidth = 0;
            for (float width : columnWidths) {
                totalWidth += width;
            }

            // Dessiner l'en-tête
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

            // 9. Données du tableau
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            boolean fill = false;
            float yPosition = yStart - rowHeight * 2;

            for (Stock stock : stockList.getItems()) {
                // Alternance de couleurs
                if (fill) {
                    contentStream.setNonStrokingColor(245, 247, 250); // Gris clair
                } else {
                    contentStream.setNonStrokingColor(255, 255, 255); // Blanc
                }

                // Dessiner la cellule de fond
                contentStream.addRect(margin, yPosition, totalWidth, rowHeight);
                contentStream.fill();

                // Réinitialiser la position X
                nextX = margin;

                // Produit
                contentStream.setNonStrokingColor(33, 33, 33);
                contentStream.beginText();
                contentStream.newLineAtOffset(nextX + 5, yPosition + 5);
                Produit p = stock.getProduit();
                contentStream.showText(p != null ? p.getNom() : "Inconnu");
                contentStream.endText();
                nextX += columnWidths[0];

                // Quantité
                contentStream.beginText();
                contentStream.newLineAtOffset(nextX + 5, yPosition + 5);
                contentStream.showText(String.valueOf(p != null ? p.getQuantite() : 0));
                contentStream.endText();
                nextX += columnWidths[1];

                // Date Entrée
                contentStream.beginText();
                contentStream.newLineAtOffset(nextX + 5, yPosition + 5);
                contentStream.showText(stock.getDateEntree().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                contentStream.endText();
                nextX += columnWidths[2];

                // Date Sortie
                contentStream.beginText();
                contentStream.newLineAtOffset(nextX + 5, yPosition + 5);
                contentStream.showText(stock.getDateSortie() != null ?
                        stock.getDateSortie().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A");
                contentStream.endText();
                nextX += columnWidths[3];

                // Entrepôts
                String entrepots = formatEntrepots(stock.getEntrepotIds());

                contentStream.beginText();
                contentStream.newLineAtOffset(nextX + 5, yPosition + 5);
                contentStream.showText(entrepots);
                contentStream.endText();
                nextX += columnWidths[4];

                // Seuil (en rouge si dépassé)
                if (p != null && p.getQuantite() < stock.getSeuilAlert()) {
                    contentStream.setNonStrokingColor(255, 87, 34); // Orange
                }
                contentStream.beginText();
                contentStream.newLineAtOffset(nextX + 5, yPosition + 5);
                contentStream.showText(String.valueOf(stock.getSeuilAlert()));
                contentStream.endText();

                // Réinitialiser la couleur
                contentStream.setNonStrokingColor(33, 33, 33);

                // Dessiner les bordures
                contentStream.setStrokingColor(200, 200, 200);
                contentStream.addRect(margin, yPosition, totalWidth, rowHeight);
                contentStream.stroke();

                yPosition -= rowHeight;
                fill = !fill;
            }

            // 10. Pied de page
            contentStream.setFont(PDType1Font.HELVETICA_OBLIQUE, 8);
            contentStream.setNonStrokingColor(120, 120, 120);
            contentStream.beginText();
            contentStream.newLineAtOffset(210, 30);
            contentStream.showText("© 2025 Agriplaner - Généré automatiquement");
            contentStream.endText();

            contentStream.close();

            // 11. Sauvegarde du fichier
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le rapport PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            fileChooser.setInitialFileName("stock_export_" + LocalDate.now() + ".pdf");

            File file = fileChooser.showSaveDialog(pdfBtn.getScene().getWindow());
            if (file != null) {
                document.save(file);
                document.close();

                // Notification
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export réussi");
                alert.setHeaderText(null);
                alert.setContentText("Le rapport PDF a été généré avec succès!");
                alert.showAndWait();

                // Ouvrir le PDF après génération (optionnel)
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            } else {
                document.close();
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur d'export");
            alert.setHeaderText("Échec de la génération du PDF");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
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
}