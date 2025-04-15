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
import javafx.scene.control.Button;
import javafx.scene.control.Cell;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.animation.FadeTransition;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Callback;
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

import javafx.scene.control.Alert;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;


import java.awt.*;
import java.awt.Font;
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
                // Utilisation explicite de la classe POI Cell
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);  // Méthode POI
                cell.setCellStyle(headerStyle);  // Méthode POI
            }

            // 3. Remplissage des données
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (int i = 0; i < stockTable.getItems().size(); i++) {
                Stock stock = stockTable.getItems().get(i);
                if (stock == null) continue;

                Row row = sheet.createRow(i + 1);
                Produit p = stock.getProduit();

                // Produit - en utilisant explicitement la classe POI Cell
                org.apache.poi.ss.usermodel.Cell produitCell = row.createCell(0);
                produitCell.setCellValue(p != null ? p.getNom() : "Inconnu");

                // Quantité
                org.apache.poi.ss.usermodel.Cell quantiteCell = row.createCell(1);
                quantiteCell.setCellValue(p != null ? p.getQuantite() : 0);

                // Date Entrée
                org.apache.poi.ss.usermodel.Cell dateEntreeCell = row.createCell(2);
                dateEntreeCell.setCellValue(stock.getDateEntree().format(dateFormatter));

                // ... (autres cellules de la même manière)
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
            // Logo gauche (remplacer par votre chemin)
            PDImageXObject logoLeft = PDImageXObject.createFromFile("src/main/resources/icons/logo.jpg", document);
            contentStream.drawImage(logoLeft, 30, 750, 40, 40);

            // Logo droit (remplacer par votre chemin)
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

            for (Stock stock : stockTable.getItems()) {
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

                // Quantité - Note: Changed to get from Produit as shown in your TableView config
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

    // Helper method to format entrepots (similar to what you have in your TableView)
    private String formatEntrepots(List<UUID> entrepotIds) {
        if (entrepotIds == null || entrepotIds.isEmpty()) {
            return "Aucun";
        }
        return entrepotIds.stream()
                .limit(3)
                .map(id -> "Entr." + id.toString().substring(0, 4))
                .collect(Collectors.joining(", "))
                + (entrepotIds.size() > 3 ? ", ..." : "");
    }
}