package com.example.produit.controller;

import com.example.auth.model.User;
import com.example.auth.utils.SessionManager;
import com.example.produit.model.Categorie;
import com.example.produit.model.Produit;
import com.example.produit.service.CategorieDAO;
import com.example.produit.service.ProduitDAO;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProductController {

    @FXML private TableView<Produit> productTableView;
    @FXML private TableColumn<Produit, Boolean> selectColumn;
    @FXML private TableColumn<Produit, String> productColumn;
    @FXML private TableColumn<Produit, String> descriptionColumn;
    @FXML private TableColumn<Produit, String> categoryColumn;
    @FXML private TableColumn<Produit, Number> priceColumn;
    @FXML private TableColumn<Produit, Number> quantityColumn;
    @FXML private TableColumn<Produit, LocalDateTime> dateCreationColumn;
    @FXML private TableColumn<Produit, Void> actionsColumn;
    @FXML private TableColumn<Produit, String> imagePreviewColumn;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextField minPriceField;
    @FXML private TextField maxPriceField;
    @FXML private TextField minQuantityField;
    @FXML private TextField maxQuantityField;
    @FXML private ComboBox<String> rateComboBox;
    @FXML private DatePicker datePicker;
    @FXML private Button addProductButton;
    @FXML private Button addScriptButton;
    @FXML private Button researchButton;
    @FXML private Button deleteSelectedButton;
    @FXML private Button exportSelectedButton;
    @FXML private Label resultsCountLabel;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private ObservableList<Produit> productList = FXCollections.observableArrayList();
    private ObservableList<Categorie> categoryList = FXCollections.observableArrayList();
    private FilteredList<Produit> filteredList;
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String IMAGE_DIR = "Uploads/images/";
    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String FLUX_API_URL = "https://queue.fal.run/fal-ai/flux-pro/v1.1-ultra";
    private static final String FLUX_AUTHORIZATION_KEY = "Key 3939b927-68c8-4ade-8095-d70b7c000476:db8c23f6d7f3d18803a231eddb8a6a8c";

    @FXML
    public void initialize() {
        try {
            createImagesDirectory();
            productTableView.setEditable(true);
            configureTableColumns();
            loadInitialData();
            initializeComboBoxes();
            setupContextMenu();
            setupActionsColumn();
            setupResultsCountListener();
            setupSearchListeners();
            setupInputValidation();
            setupSelectionListener();
        } catch (Exception e) {
            showError("Initialization Error", "Initialization failed: " + e.getMessage());
        }
    }

    private void createImagesDirectory() {
        File dir = new File(IMAGE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void configureTableColumns() {
        selectColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);

        productColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        categoryColumn.setCellValueFactory(cellData -> {
            Produit produit = cellData.getValue();
            Categorie category = produit.getCategory();
            return new SimpleStringProperty(category != null ? category.getNom() : "No Category");
        });
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        dateCreationColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        imagePreviewColumn.setCellFactory(param -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(String imageName, boolean empty) {
                super.updateItem(imageName, empty);
                if (empty || imageName == null || imageName.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        Image image = new Image(imageName.startsWith("http") ? imageName : "file:" + imageName);
                        imageView.setImage(image);
                        setGraphic(imageView);
                    } catch (Exception ex) {
                        setGraphic(null);
                    }
                }
            }
        });
        imagePreviewColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getImageName()));
    }

    private void initializeComboBoxes() {
        ObservableList<String> categoryNames = FXCollections.observableArrayList("All");
        categoryNames.addAll(categoryList.stream()
                .filter(c -> c.getNom() != null)
                .map(Categorie::getNom)
                .collect(Collectors.toList()));
        categoryComboBox.setItems(categoryNames);
        categoryComboBox.getSelectionModel().selectFirst();

        rateComboBox.getItems().addAll("All", "4+", "3+", "2+", "1+");
        rateComboBox.getSelectionModel().selectFirst();
    }

    private void loadInitialData() {
        try {
            categoryList.addAll(CategorieDAO.getAllCategories());
            productList.addAll(ProduitDAO.getAllProducts());
            filteredList = new FilteredList<>(productList, p -> true);
            productTableView.setItems(filteredList);
            updateResultsCount();
        } catch (Exception e) {
            showError("Data Loading Error", "Error loading data: " + e.getMessage());
        }
    }

    private void setupSearchListeners() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> handleResearch());
        categoryComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleResearch());
        minPriceField.textProperty().addListener((obs, oldVal, newVal) -> handleResearch());
        maxPriceField.textProperty().addListener((obs, oldVal, newVal) -> handleResearch());
        minQuantityField.textProperty().addListener((obs, oldVal, newVal) -> handleResearch());
        maxQuantityField.textProperty().addListener((obs, oldVal, newVal) -> handleResearch());
        rateComboBox.valueProperty().addListener((obs, oldVal, newVal) -> handleResearch());
        datePicker.valueProperty().addListener((obs, oldVal, newVal) -> handleResearch());
    }

    private void setupInputValidation() {
        Pattern pricePattern = Pattern.compile("^\\d*\\.?\\d*$");
        Pattern quantityPattern = Pattern.compile("^[0-9]*$");

        minPriceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !pricePattern.matcher(newVal).matches()) {
                minPriceField.setStyle("-fx-border-color: red;");
            } else {
                try {
                    if (!newVal.isEmpty()) {
                        float price = Float.parseFloat(newVal);
                        if (price < 0 || price > 1000000) {
                            minPriceField.setStyle("-fx-border-color: red;");
                            return;
                        }
                    }
                    minPriceField.setStyle("");
                } catch (NumberFormatException e) {
                    minPriceField.setStyle("-fx-border-color: red;");
                }
            }
        });

        maxPriceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !pricePattern.matcher(newVal).matches()) {
                maxPriceField.setStyle("-fx-border-color: red;");
            } else {
                try {
                    if (!newVal.isEmpty()) {
                        float price = Float.parseFloat(newVal);
                        if (price < 0 || price > 1000000) {
                            maxPriceField.setStyle("-fx-border-color: red;");
                            return;
                        }
                    }
                    maxPriceField.setStyle("");
                } catch (NumberFormatException e) {
                    maxPriceField.setStyle("-fx-border-color: red;");
                }
            }
        });

        minQuantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !quantityPattern.matcher(newVal).matches()) {
                minQuantityField.setStyle("-fx-border-color: red;");
            } else {
                try {
                    if (!newVal.isEmpty()) {
                        int quantity = Integer.parseInt(newVal);
                        if (quantity < 0 || quantity > 10000) {
                            minQuantityField.setStyle("-fx-border-color: red;");
                            return;
                        }
                    }
                    minQuantityField.setStyle("");
                } catch (NumberFormatException e) {
                    minQuantityField.setStyle("-fx-border-color: red;");
                }
            }
        });

        maxQuantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty() && !quantityPattern.matcher(newVal).matches()) {
                maxQuantityField.setStyle("-fx-border-color: red;");
            } else {
                try {
                    if (!newVal.isEmpty()) {
                        int quantity = Integer.parseInt(newVal);
                        if (quantity < 0 || quantity > 10000) {
                            maxQuantityField.setStyle("-fx-border-color: red;");
                            return;
                        }
                    }
                    maxQuantityField.setStyle("");
                } catch (NumberFormatException e) {
                    maxQuantityField.setStyle("-fx-border-color: red;");
                }
            }
        });
    }

    private void setupSelectionListener() {
        productList.forEach(this::addSelectionListener);
        productList.addListener((javafx.collections.ListChangeListener<Produit>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    c.getAddedSubList().forEach(this::addSelectionListener);
                }
            }
            updateBulkButtons();
        });
        filteredList.addListener((javafx.collections.ListChangeListener<Produit>) c -> updateBulkButtons());
    }

    private void addSelectionListener(Produit product) {
        product.selectedProperty().addListener((obs, oldVal, newVal) -> updateBulkButtons());
    }

    private void updateBulkButtons() {
        boolean hasSelection = productList.stream().anyMatch(Produit::isSelected);
        deleteSelectedButton.setDisable(!hasSelection);
        exportSelectedButton.setDisable(!hasSelection);
    }

    @FXML
    private void handleResearch() {
        filteredList.setPredicate(product -> {
            boolean match = true;
            String searchText = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
            if (!searchText.isEmpty()) {
                match &= product.getNom().toLowerCase().contains(searchText) ||
                        product.getDescription().toLowerCase().contains(searchText);
            }
            String category = categoryComboBox.getValue();
            if (category != null && !category.equals("All")) {
                match &= product.getCategory() != null && product.getCategory().getNom().equals(category);
            }
            try {
                if (!minPriceField.getText().isEmpty()) {
                    float minPrice = Float.parseFloat(minPriceField.getText());
                    match &= product.getPrixUnitaire() >= minPrice;
                }
                if (!maxPriceField.getText().isEmpty()) {
                    float maxPrice = Float.parseFloat(maxPriceField.getText());
                    match &= product.getPrixUnitaire() <= maxPrice;
                }
            } catch (NumberFormatException ignored) {
            }
            try {
                if (!minQuantityField.getText().isEmpty()) {
                    int minQuantity = Integer.parseInt(minQuantityField.getText());
                    match &= product.getQuantite() >= minQuantity;
                }
                if (!maxQuantityField.getText().isEmpty()) {
                    int maxQuantity = Integer.parseInt(maxQuantityField.getText());
                    match &= product.getQuantite() <= maxQuantity;
                }
            } catch (NumberFormatException ignored) {
            }
            String rateFilter = rateComboBox.getValue();
            if (rateFilter != null && !rateFilter.equals("All")) {
                float minRate = Float.parseFloat(rateFilter.replace("+", ""));
                match &= product.getRate() != null && product.getRate() >= minRate;
            }
            LocalDate selectedDate = datePicker.getValue();
            if (selectedDate != null) {
                LocalDateTime startOfDay = selectedDate.atStartOfDay();
                LocalDateTime endOfDay = selectedDate.plusDays(1).atStartOfDay();
                match &= product.getDateCreation() != null &&
                        !product.getDateCreation().isBefore(startOfDay) &&
                        product.getDateCreation().isBefore(endOfDay);
            }
            return match;
        });
        productTableView.refresh();
        updateResultsCount();
    }

    @FXML
    private void handleBulkDelete() {
        List<Produit> selectedProducts = productList.stream()
                .filter(Produit::isSelected)
                .collect(Collectors.toList());
        if (selectedProducts.isEmpty()) {
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Selected Products");
        confirmation.setHeaderText("Delete " + selectedProducts.size() + " Product(s)");
        confirmation.setContentText("Are you sure you want to delete the selected products?");
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            List<String> ids = selectedProducts.stream().map(Produit::getId).collect(Collectors.toList());
            boolean deleted = ProduitDAO.deleteProducts(ids);
            if (deleted) {
                productList.removeAll(selectedProducts);
                handleResearch();
                showInfo("Success", "Successfully deleted " + selectedProducts.size() + " product(s).");
            } else {
                showError("Delete Failed", "Failed to delete selected products. Check logs for details.");
            }
        }
    }

    @FXML
    private void exportToPDF() {
        List<Produit> selectedProducts = productList.stream()
                .filter(Produit::isSelected)
                .collect(Collectors.toList());
        if (selectedProducts.isEmpty()) {
            showWarning("No Selection", "Please select at least one product to export.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save PDF File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showSaveDialog(productTableView.getScene().getWindow());
        if (file != null) {
            try {
                PdfWriter writer = new PdfWriter(file);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf);
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                Paragraph title = new Paragraph("Products Report")
                        .setFont(boldFont)
                        .setFontSize(20)
                        .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(33, 150, 243))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10)
                        .setPadding(10)
                        .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(240, 248, 255));
                document.add(title);
                Paragraph timestamp = new Paragraph("Generated on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm")))
                        .setFont(font)
                        .setFontSize(10)
                        .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(100, 100, 100))
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20);
                document.add(timestamp);
                float[] columnWidths = {150, 200, 100, 80, 80, 100};
                Table table = new Table(UnitValue.createPointArray(columnWidths));
                table.setWidth(UnitValue.createPercentValue(100));
                table.setMarginTop(10);
                String[] headers = {"Name", "Description", "Category", "Price", "Qte", "Date Created"};
                for (String header : headers) {
                    table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(header)
                                    .setFont(boldFont)
                                    .setFontSize(11)
                                    .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255)))
                            .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(33, 150, 243))
                            .setTextAlignment(TextAlignment.CENTER)
                            .setPadding(8)
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 1)));
                }
                boolean alternate = false;
                for (Produit product : selectedProducts) {
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getNom())
                                    .setFont(font)
                                    .setFontSize(10))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setPadding(6)
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 1)));
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getDescription())
                                    .setFont(font)
                                    .setFontSize(10))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setPadding(6)
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 1)));
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getCategory() != null ? product.getCategory().getNom() : "No Category")
                                    .setFont(font)
                                    .setFontSize(10))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setPadding(6)
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 1)));
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(String.format("%.2f", product.getPrixUnitaire()))
                                    .setFont(font)
                                    .setFontSize(10))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setPadding(6)
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 1)));
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(String.valueOf(product.getQuantite()))
                                    .setFont(font)
                                    .setFontSize(10))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setPadding(6)
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 1)));
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getDateCreation().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                                    .setFont(font)
                                    .setFontSize(10))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(245, 245, 245) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setPadding(6)
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(200, 200, 200), 1)));
                    alternate = !alternate;
                }
                document.add(table);
                document.close();
                showInfo("Export Successful", "Selected products exported to " + file.getName());
            } catch (Exception e) {
                showError("Export Failed", "Failed to export PDF: " + e.getMessage());
            }
        }
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox hbox = new HBox(10);

            {
                InputStream editStream = getClass().getResourceAsStream("/icons/edit.png");
                InputStream deleteStream = getClass().getResourceAsStream("/icons/delete.png");
                if (editStream != null) {
                    Image editImage = new Image(editStream);
                    editButton.setGraphic(new ImageView(editImage));
                } else {
                    System.err.println("Resource /icons/edit.png not found in classpath.");
                    editButton.setText("Edit");
                }
                if (deleteStream != null) {
                    Image deleteImage = new Image(deleteStream);
                    deleteButton.setGraphic(new ImageView(deleteImage));
                } else {
                    System.err.println("Resource /icons/delete.png not found in classpath.");
                    deleteButton.setText("Delete");
                }
                editButton.getStyleClass().add("action-button");
                deleteButton.getStyleClass().add("action-button");
                editButton.setOnAction(event -> {
                    Produit product = getTableView().getItems().get(getIndex());
                    handleEditProduct(product);
                });
                deleteButton.setOnAction(event -> {
                    Produit product = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(product);
                });
                hbox.getChildren().addAll(editButton, deleteButton);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void setupResultsCountListener() {
        filteredList.addListener((javafx.collections.ListChangeListener<Produit>) c -> updateResultsCount());
        updateResultsCount();
    }

    private void updateResultsCount() {
        int count = filteredList.size();
        resultsCountLabel.setText("SHOWING " + count + " PRODUCTS");
    }

    @FXML
    private void handleAddProduct() {
        showProductDialog(null);
    }

    @FXML
    private void handleAddScript() {
        showScriptDialog();
    }

    private void handleEditProduct(Produit product) {
        if (product != null) {
            showProductDialog(product);
        }
    }

    private void handleDeleteProduct(Produit product) {
        if (product != null) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Delete Product");
            confirmation.setHeaderText("Delete " + product.getNom());
            confirmation.setContentText("Are you sure you want to delete this product?");
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean deleted = ProduitDAO.deleteProduct(product.getId());
                if (deleted) {
                    productList.remove(product);
                    handleResearch();
                    showInfo("Success", "Product deleted successfully.");
                } else {
                    showError("Delete Failed", "Failed to delete product. Check logs for details.");
                }
            }
        }
    }

    private String generateProductDescription(String brand, String category, String name) {
        try {
            String prompt = "Generate an attractive description in English, with a maximum of 200 characters, for an agricultural product named '" + name + "' in the category '" + category + "' intended for a farmers' market.";
            String jsonBody = """
                {
                  "messages": [
                    {
                      "role": "user",
                      "content": "%s"
                    }
                  ],
                  "model": "llama3-8b-8192",
                  "temperature": 1,
                  "max_tokens": 100
                }
                """.formatted(prompt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + GROQ_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String description = jsonResponse.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                return description.length() > 200 ? description.substring(0, 200) : description;
            } else {
                showError("API Error", "Failed to generate description: HTTP " + response.statusCode());
            }
        } catch (Exception e) {
            showError("API Error", "Failed to generate description: " + e.getMessage());
        }
        return "";
    }

    private String uploadImageToCloud(File imageFile) throws Exception {
        String extension = imageFile.getName().substring(imageFile.getName().lastIndexOf(".") + 1).toLowerCase();
        if (!List.of("jpg", "jpeg", "png", "bmp", "gif", "webp").contains(extension)) {
            throw new IllegalArgumentException("Invalid image format. Allowed: jpg, jpeg, png, bmp, gif, webp.");
        }

        try {
            String boundary = "----WebKitFormBoundary" + UUID.randomUUID().toString();
            String contentType = "multipart/form-data; boundary=" + boundary;

            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"filename\"; filename=\"")
                    .append(imageFile.getName()).append("\"\r\n")
                    .append("Content-Type: image/").append(extension.equals("jpeg") ? "jpeg" : extension).append("\r\n\r\n");

            byte[] headerBytes = bodyBuilder.toString().getBytes("UTF-8");
            byte[] fileBytes = Files.readAllBytes(imageFile.toPath());
            byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes("UTF-8");

            byte[] bodyBytes = new byte[headerBytes.length + fileBytes.length + footerBytes.length];
            System.arraycopy(headerBytes, 0, bodyBytes, 0, headerBytes.length);
            System.arraycopy(fileBytes, 0, bodyBytes, headerBytes.length, fileBytes.length);
            System.arraycopy(footerBytes, 0, bodyBytes, headerBytes.length + fileBytes.length, footerBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://prod.api.market/api/v1/magicapi/image-upload/upload"))
                    .header("accept", "application/json")
                    .header("x-magicapi-key", "cmaocal040001jm046r2mztky")
                    .header("Content-Type", contentType)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                if (jsonResponse.has("url")) {
                    return jsonResponse.getString("url");
                } else {
                    throw new Exception("No image URL found in response: " + response.body());
                }
            } else {
                throw new Exception("MagicAPI upload failed with status: " + response.statusCode() + ", response: " + response.body());
            }
        } catch (Exception e) {
            throw new Exception("Failed to upload image to MagicAPI: " + e.getMessage(), e);
        }
    }

    private void generateProductImage(String productName, TextField imageUrlField, ImageView imagePreview, Label statusLabel) {
        statusLabel.setText("Generating image...");
        new Thread(() -> {
            try {
                String sanitizedName = productName.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
                String imagePath = IMAGE_DIR + sanitizedName + ".jpg";
                String requestBody = """
                    {
                        "prompt": "%s, simple style",
                        "image_size": "square",
                        "num_inference_steps": 10,
                        "guidance_scale": 3.5,
                        "num_images": 1,
                        "output_format": "jpeg",
                        "safety_tolerance": 2
                    }
                    """.formatted(productName);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(FLUX_API_URL))
                        .header("Authorization", FLUX_AUTHORIZATION_KEY)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    updateStatus(statusLabel, "POST Error: " + response.statusCode() + " - " + response.body());
                    return;
                }
                JSONObject jsonResponse = new JSONObject(response.body());
                if (jsonResponse.has("images")) {
                    String imageUrl = jsonResponse.getJSONArray("images").getJSONObject(0).getString("url");
                    downloadAndSaveImage(imageUrl, imagePath, imageUrlField, imagePreview, statusLabel);
                    return;
                }
                String status = jsonResponse.optString("status", "UNKNOWN");
                String statusUrl = jsonResponse.optString("status_url", "");
                String responseUrl = jsonResponse.optString("response_url", "");
                if (statusUrl.isEmpty() || responseUrl.isEmpty()) {
                    updateStatus(statusLabel, "Error: Missing status_url or response_url in initial response.");
                    return;
                }
                if (!status.equals("IN_QUEUE") && !status.equals("IN_PROGRESS")) {
                    updateStatus(statusLabel, "Unexpected status: " + status);
                    return;
                }
                int maxAttempts = 10;
                int attempt = 0;
                do {
                    Thread.sleep(2000);
                    HttpRequest statusRequest = HttpRequest.newBuilder()
                            .uri(URI.create(statusUrl))
                            .header("Authorization", FLUX_AUTHORIZATION_KEY)
                            .header("Content-Type", "application/json")
                            .GET()
                            .build();
                    HttpResponse<String> statusResponse = client.send(statusRequest, HttpResponse.BodyHandlers.ofString());
                    JSONObject statusJson = new JSONObject(statusResponse.body());
                    status = statusJson.optString("status", "UNKNOWN");
                    updateStatus(statusLabel, "Status Check [" + attempt + "]: " + status);
                    attempt++;
                } while ((status.equals("IN_QUEUE") || status.equals("IN_PROGRESS")) && attempt < maxAttempts);
                if (!status.equals("COMPLETED")) {
                    updateStatus(statusLabel, "Task failed or timed out. Status: " + status);
                    return;
                }
                HttpRequest resultRequest = HttpRequest.newBuilder()
                        .uri(URI.create(responseUrl))
                        .header("Authorization", FLUX_AUTHORIZATION_KEY)
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();
                HttpResponse<String> resultResponse = client.send(resultRequest, HttpResponse.BodyHandlers.ofString());
                if (resultResponse.statusCode() != 200) {
                    updateStatus(statusLabel, "GET Result Error: " + resultResponse.statusCode() + " - " + resultResponse.body());
                    return;
                }
                JSONObject resultJson = new JSONObject(resultResponse.body());
                if (!resultJson.has("images")) {
                    updateStatus(statusLabel, "Error: No images in final result.");
                    return;
                }
                String imageUrl = resultJson.getJSONArray("images").getJSONObject(0).getString("url");
                downloadAndSaveImage(imageUrl, imagePath, imageUrlField, imagePreview, statusLabel);
            } catch (Exception ex) {
                updateStatus(statusLabel, "Exception: " + ex.getMessage());
                ex.printStackTrace();
            }
        }).start();
    }

    private void downloadAndSaveImage(String imageUrl, String imagePath, TextField imageUrlField, ImageView imagePreview, Label statusLabel) {
        try {
            HttpRequest imageRequest = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .build();
            HttpResponse<InputStream> imageResponse = client.send(imageRequest, HttpResponse.BodyHandlers.ofInputStream());
            if (imageResponse.statusCode() != 200) {
                updateStatus(statusLabel, "Image Download Error: " + imageResponse.statusCode());
                return;
            }
            File tempFile = new File(imagePath);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                InputStream inputStream = imageResponse.body();
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            String cloudImageUrl = uploadImageToCloud(tempFile);
            tempFile.delete();
            Platform.runLater(() -> {
                imageUrlField.setText(cloudImageUrl);
                try {
                    Image image = new Image(cloudImageUrl);
                    imagePreview.setImage(image);
                    updateStatus(statusLabel, "Image generated and uploaded successfully to: " + cloudImageUrl);
                } catch (Exception e) {
                    updateStatus(statusLabel, "Error loading image preview: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            updateStatus(statusLabel, "Error processing image: " + e.getMessage());
        }
    }

    private void updateStatus(Label statusLabel, String text) {
        Platform.runLater(() -> statusLabel.setText(text));
    }

    private String uploadImage(File imageFile) {
        try {
            String extension = imageFile.getName().substring(imageFile.getName().lastIndexOf(".") + 1).toLowerCase();
            if (!List.of("jpg", "jpeg", "png").contains(extension)) {
                throw new IllegalArgumentException("Invalid image format. Allowed: jpg, jpeg, png.");
            }
            String newFilename = UUID.randomUUID().toString() + "." + extension;
            Path targetPath = Paths.get(IMAGE_DIR, newFilename);
            Files.copy(imageFile.toPath(), targetPath);
            return IMAGE_DIR + newFilename;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload image: " + e.getMessage());
        }
    }

    private String fetchImageFromApi(String productName, String category) {
        try {
            String prompt = String.format("Image of %s in category %s", productName, category);
            String jsonBody = """
                {
                  "query": "%s",
                  "limit": 1
                }
                """.formatted(prompt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.imagefetch.example.com/v1/images"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer your_image_api_key_here")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String imageUrl = jsonResponse.getJSONArray("results").getJSONObject(0).getString("url");
                String extension = imageUrl.substring(imageUrl.lastIndexOf(".") + 1).toLowerCase();
                if (!List.of("jpg", "jpeg", "png").contains(extension)) {
                    throw new IllegalArgumentException("Invalid image format from API. Allowed: jpg, jpeg, png.");
                }
                String newFilename = UUID.randomUUID().toString() + "." + extension;
                Path targetPath = Paths.get(IMAGE_DIR, newFilename);
                try (InputStream in = new URL(imageUrl).openStream();
                     FileOutputStream out = new FileOutputStream(targetPath.toFile())) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                return IMAGE_DIR + newFilename;
            } else {
                showError("API Error", "Failed to fetch image: HTTP " + response.statusCode());
                return null;
            }
        } catch (Exception e) {
            showError("API Error", "Failed to fetch image from API: " + e.getMessage());
            return null;
        }
    }

    private void showProductDialog(Produit product) {
        Dialog<Produit> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "New Product" : "Edit Product");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/css/produitdialog.css").toExternalForm());

        TextField nameField = new TextField();
        TextArea descriptionField = new TextArea();
        ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(
                categoryList.stream().map(Categorie::getNom).collect(Collectors.toList())
        ));
        TextField priceField = new TextField();
        TextField quantityField = new TextField();
        TextField imageUrlField = new TextField();
        imageUrlField.setEditable(false);
        CheckBox useApiCheckBox = new CheckBox("Fetch Image via API");
        Button chooseImageButton = new Button("Choose Local Image");
        Button fetchImageButton = new Button("Fetch Image from API");
        Button generateImageButton = new Button("Generate Image");
        Button generateDescriptionButton = new Button("Generate Description");
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(100);
        imagePreview.setFitHeight(100);
        imagePreview.setPreserveRatio(true);
        Label imageStatusLabel = new Label();
        Label nameError = new Label();
        Label descriptionError = new Label();
        Label categoryError = new Label();
        Label priceError = new Label();
        Label quantityError = new Label();
        nameError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        descriptionError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        categoryError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        priceError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        quantityError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        chooseImageButton.setOnAction(e -> {
            File file = fileChooser.showOpenDialog(dialog.getOwner());
            if (file != null) {
                try {
                    imageStatusLabel.setText("Uploading image...");
                    String imagePath = uploadImage(file);
                    String cloudImageUrl = uploadImageToCloud(file);
                    imageUrlField.setText(cloudImageUrl);
                    Image image = new Image(cloudImageUrl);
                    imagePreview.setImage(image);
                    imageStatusLabel.setText("Image uploaded successfully to: " + cloudImageUrl);
                } catch (Exception ex) {
                    imageStatusLabel.setText("Failed to upload image: " + ex.getMessage());
                    showError("Image Upload Failed", "Failed to upload image: " + ex.getMessage());
                }
            }
        });

        fetchImageButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String category = categoryCombo.getValue();
            if (name.isEmpty() || category == null) {
                showWarning("Input Required", "Please enter a product name and select a category to fetch an image.");
                return;
            }
            imageStatusLabel.setText("Fetching image...");
            String imagePath = fetchImageFromApi(name, category);
            if (imagePath != null) {
                try {
                    String cloudImageUrl = uploadImageToCloud(new File(imagePath));
                    imageUrlField.setText(cloudImageUrl);
                    Image image = new Image(cloudImageUrl);
                    imagePreview.setImage(image);
                    imageStatusLabel.setText("Image fetched and uploaded successfully to: " + cloudImageUrl);
                } catch (Exception ex) {
                    imageStatusLabel.setText("Failed to upload fetched image: " + ex.getMessage());
                    showError("Image Upload Failed", "Failed to upload fetched image: " + ex.getMessage());
                }
            }
        });

        generateImageButton.setOnAction(e -> {
            String productName = nameField.getText().trim();
            if (productName.isEmpty()) {
                showWarning("Input Required", "Please enter a product name before generating an image.");
                return;
            }
            generateProductImage(productName, imageUrlField, imagePreview, imageStatusLabel);
        });

        generateDescriptionButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String category = categoryCombo.getValue();
            if (name.isEmpty() || category == null) {
                showWarning("Input Required", "Please enter a product name and select a category.");
                return;
            }
            String description = generateProductDescription("", category, name);
            if (!description.isEmpty()) {
                descriptionField.setText(description);
            }
        });

        useApiCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            chooseImageButton.setDisable(newVal);
            fetchImageButton.setDisable(!newVal);
            generateImageButton.setDisable(newVal);
        });

        if (product != null) {
            nameField.setText(product.getNom());
            descriptionField.setText(product.getDescription());
            categoryCombo.getSelectionModel().select(product.getCategory() != null ? product.getCategory().getNom() : null);
            priceField.setText(String.valueOf(product.getPrixUnitaire()));
            quantityField.setText(String.valueOf(product.getQuantite()));
            imageUrlField.setText(product.getImageName() != null ? product.getImageName() : "");
            if (product.getImageName() != null && !product.getImageName().isEmpty()) {
                try {
                    Image image = new Image(product.getImageName().startsWith("http") ? product.getImageName() : "file:" + product.getImageName());
                    imagePreview.setImage(image);
                } catch (Exception ex) {
                    imagePreview.setImage(null);
                }
            }
        }

        Pattern namePattern = Pattern.compile("^[a-zA-Z0-9\\s-]{3,50}$");
        Pattern pricePattern = Pattern.compile("^\\d*\\.?\\d+$");
        Pattern quantityPattern = Pattern.compile("^[0-9]+$");

        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                nameError.setText("Name cannot be empty");
                nameField.setStyle("-fx-border-color: red;");
            } else if (!namePattern.matcher(newVal).matches()) {
                nameError.setText("3-50 chars, letters, numbers, spaces, hyphens");
                nameField.setStyle("-fx-border-color: red;");
            } else {
                nameError.setText("");
                nameField.setStyle("");
            }
        });

        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                descriptionError.setText("Description cannot be empty");
                descriptionField.setStyle("-fx-border-color: red;");
            } else if (newVal.length() > 200) {
                descriptionError.setText("Max 200 characters");
                descriptionField.setStyle("-fx-border-color: red;");
            } else {
                descriptionError.setText("");
                descriptionField.setStyle("");
            }
        });

        categoryCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                categoryError.setText("Select a category");
                categoryCombo.setStyle("-fx-border-color: red;");
            } else {
                categoryError.setText("");
                categoryCombo.setStyle("");
            }
        });

        priceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                priceError.setText("Price cannot be empty");
                priceField.setStyle("-fx-border-color: red;");
            } else if (!pricePattern.matcher(newVal).matches()) {
                priceError.setText("Positive number required");
                priceField.setStyle("-fx-border-color: red;");
            } else {
                try {
                    float price = Float.parseFloat(newVal);
                    if (price <= 0) {
                        priceError.setText("Price must be > 0");
                        priceField.setStyle("-fx-border-color: red;");
                    } else if (price > 1000000) {
                        priceError.setText("Price max 1,000,000");
                        priceField.setStyle("-fx-border-color: red;");
                    } else {
                        priceError.setText("");
                        priceField.setStyle("");
                    }
                } catch (NumberFormatException e) {
                    priceError.setText("Invalid price format");
                    priceField.setStyle("-fx-border-color: red;");
                }
            }
        });

        quantityField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                quantityError.setText("Quantity cannot be empty");
                quantityField.setStyle("-fx-border-color: red;");
            } else if (!quantityPattern.matcher(newVal).matches()) {
                quantityError.setText("Positive integer required");
                quantityField.setStyle("-fx-border-color: red;");
            } else {
                try {
                    int quantity = Integer.parseInt(newVal);
                    if (quantity <= 0) {
                        quantityError.setText("Quantity must be > 0");
                        quantityField.setStyle("-fx-border-color: red;");
                    } else if (quantity > 10000) {
                        quantityError.setText("Quantity max 10,000");
                        quantityField.setStyle("-fx-border-color: red;");
                    } else {
                        quantityError.setText("");
                        quantityField.setStyle("");
                    }
                } catch (NumberFormatException e) {
                    quantityError.setText("Invalid quantity format");
                    quantityField.setStyle("-fx-border-color: red;");
                }
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.addRow(0, new Label("Name:"), nameField);
        grid.add(nameError, 1, 1);
        grid.addRow(2, new Label("Description:"), descriptionField);
        grid.add(generateDescriptionButton, 1, 3);
        grid.add(descriptionError, 1, 4);
        grid.addRow(5, new Label("Category:"), categoryCombo);
        grid.add(categoryError, 1, 6);
        grid.addRow(7, new Label("Price:"), priceField);
        grid.add(priceError, 1, 8);
        grid.addRow(9, new Label("Quantity:"), quantityField);
        grid.add(quantityError, 1, 10);
        grid.addRow(11, new Label("Image URL:"), imageUrlField);
        grid.addRow(12, new Label(""), useApiCheckBox);
        HBox imageButtons = new HBox(10);
        imageButtons.getChildren().addAll(chooseImageButton, fetchImageButton, generateImageButton);
        grid.addRow(13, new Label(""), imageButtons);
        grid.addRow(14, new Label(""), imageStatusLabel);
        grid.addRow(15, new Label("Preview:"), imagePreview);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        Runnable validateForm = () -> {
            boolean isValid =
                    !nameField.getText().trim().isEmpty() &&
                            namePattern.matcher(nameField.getText()).matches() &&
                            !descriptionField.getText().trim().isEmpty() &&
                            descriptionField.getText().length() <= 200 &&
                            categoryCombo.getValue() != null &&
                            !priceField.getText().trim().isEmpty() &&
                            pricePattern.matcher(priceField.getText()).matches() &&
                            Float.parseFloat(priceField.getText()) > 0 &&
                            Float.parseFloat(priceField.getText()) <= 1000000 &&
                            !quantityField.getText().trim().isEmpty() &&
                            quantityPattern.matcher(quantityField.getText()).matches() &&
                            Integer.parseInt(quantityField.getText()) > 0 &&
                            Integer.parseInt(quantityField.getText()) <= 10000;
            saveButton.setDisable(!isValid);
        };

        nameField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        descriptionField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        categoryCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        priceField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        quantityField.textProperty().addListener((obs, old, newVal) -> validateForm.run());

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                try {
                    Produit newProduct = product != null ? product : new Produit();
                    newProduct.setNom(nameField.getText().trim());
                    User currentUser = sessionManager.getLoggedInUser();
                    if (currentUser != null && currentUser.getId() != null) {
                        newProduct.setUserId(currentUser.getId());
                    } else {
                        throw new IllegalStateException("No valid user logged in.");
                    }
                    newProduct.setDescription(descriptionField.getText().trim());
                    String selectedCategoryName = categoryCombo.getValue();
                    Categorie selectedCategory = categoryList.stream()
                            .filter(c -> c.getNom().equals(selectedCategoryName))
                            .findFirst()
                            .orElse(null);
                    if (selectedCategory == null) {
                        throw new IllegalArgumentException("Selected category does not exist.");
                    }
                    newProduct.setCategory(selectedCategory);
                    newProduct.setPrixUnitaire(Float.parseFloat(priceField.getText()));
                    newProduct.setQuantite(Integer.parseInt(quantityField.getText()));
                    newProduct.setImageName(imageUrlField.getText());

                    if (product == null) {
                        newProduct.setId(UUID.randomUUID().toString());
                        newProduct.setDateCreation(LocalDateTime.now());
                        boolean saved = ProduitDAO.saveProduct(newProduct);
                        if (saved) {
                            productList.add(newProduct);
                            showInfo("Success", "Product added successfully.");
                        } else {
                            throw new RuntimeException("Failed to save product to the database. Check logs for details.");
                        }
                    } else {
                        boolean updated = ProduitDAO.updateProduct(newProduct);
                        if (updated) {
                            productTableView.refresh();
                            showInfo("Success", "Product updated successfully.");
                        } else {
                            throw new RuntimeException("Failed to update product in the database. Check logs for details.");
                        }
                    }
                    handleResearch();
                    return newProduct;
                } catch (Exception ex) {
                    showError("Save Error", "Failed to save product: " + ex.getMessage());
                    return null;
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void showScriptDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Add Products via Script");
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/example/css/produitdialog.css").toExternalForm());

        TextArea scriptField = new TextArea();
        scriptField.setPromptText("Enter JSON array of products, e.g., [{\"nom\": \"Apple\", \"description\": \"Fresh apples\", \"category\": \"Fruits\", \"prixUnitaire\": 2.99, \"quantite\": 100, \"imageName\": \"Uploads/images/apple.jpg\"}]");
        scriptField.setPrefRowCount(10);
        scriptField.setPrefColumnCount(50);
        Label scriptError = new Label();
        scriptError.setStyle("-fx-text-fill: red; -fx-font-size: 10px;");
        Button clearButton = new Button("Clear");
        clearButton.setOnAction(e -> scriptField.clear());

        Pattern namePattern = Pattern.compile("^[a-zA-Z0-9\\s-]{3,50}$");
        Pattern pricePattern = Pattern.compile("^\\d*\\.?\\d+$");
        Pattern quantityPattern = Pattern.compile("^[0-9]+$");

        scriptField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                scriptError.setText("Script cannot be empty");
                scriptField.setStyle("-fx-border-color: red;");
                return;
            }
            try {
                new JSONArray(newVal);
                scriptError.setText("");
                scriptField.setStyle("");
            } catch (Exception e) {
                scriptError.setText("Invalid JSON format");
                scriptField.setStyle("-fx-border-color: red;");
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.addRow(0, new Label("JSON Script:"), scriptField);
        grid.add(scriptError, 1, 1);
        grid.addRow(2, new Label(""), clearButton);
        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Parse and Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        scriptField.textProperty().addListener((obs, old, newVal) -> {
            boolean isValid = !newVal.trim().isEmpty();
            try {
                new JSONArray(newVal);
                saveButton.setDisable(false);
            } catch (Exception e) {
                saveButton.setDisable(true);
            }
        });

        dialog.setResultConverter(buttonType -> {
            if (buttonType == saveButtonType) {
                try {
                    User currentUser = sessionManager.getLoggedInUser();
                    if (currentUser == null || currentUser.getId() == null) {
                        showError("Authentication Error", "No user is logged in. Please log in and try again.");
                        return null;
                    }
                    JSONArray jsonArray = new JSONArray(scriptField.getText().trim());
                    if (jsonArray.length() > 100) {
                        showError("Too Many Products", "Cannot add more than 100 products at once.");
                        return null;
                    }
                    int successCount = 0;
                    StringBuilder errorMessages = new StringBuilder();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject json = jsonArray.getJSONObject(i);
                        String nom = json.optString("nom", "").trim();
                        String description = json.optString("description", "").trim();
                        String categoryName = json.optString("category", "").trim();
                        float prixUnitaire = json.optFloat("prixUnitaire", -1);
                        int quantite = json.optInt("quantite", -1);
                        String imageName = json.optString("imageName", "").trim();
                        if (nom.isEmpty() || !namePattern.matcher(nom).matches()) {
                            errorMessages.append(String.format("Product %d: Invalid name (3-50 chars, letters, numbers, spaces, hyphens)\n", i + 1));
                            continue;
                        }
                        if (description.isEmpty() || description.length() > 200) {
                            errorMessages.append(String.format("Product %d: Invalid description (1-200 chars)\n", i + 1));
                            continue;
                        }
                        Categorie category = categoryList.stream()
                                .filter(c -> c.getNom().equals(categoryName))
                                .findFirst()
                                .orElse(null);
                        if (category == null) {
                            errorMessages.append(String.format("Product %d: Invalid category '%s'\n", i + 1, categoryName));
                            continue;
                        }
                        if (prixUnitaire <= 0 || prixUnitaire > 1000000 || !pricePattern.matcher(String.valueOf(prixUnitaire)).matches()) {
                            errorMessages.append(String.format("Product %d: Invalid price (0 < price <= 1,000,000)\n", i + 1));
                            continue;
                        }
                        if (quantite <= 0 || quantite > 10000 || !quantityPattern.matcher(String.valueOf(quantite)).matches()) {
                            errorMessages.append(String.format("Product %d: Invalid quantity (0 < quantity <= 10,000)\n", i + 1));
                            continue;
                        }
                        Produit newProduct = new Produit();
                        newProduct.setId(UUID.randomUUID().toString());
                        newProduct.setNom(nom);
                        newProduct.setDescription(description);
                        newProduct.setCategory(category);
                        newProduct.setPrixUnitaire(prixUnitaire);
                        newProduct.setQuantite(quantite);
                        newProduct.setImageName(imageName.isEmpty() ? null : imageName);
                        newProduct.setDateCreation(LocalDateTime.now());
                        newProduct.setUserId(currentUser.getId());
                        boolean saved = ProduitDAO.saveProduct(newProduct);
                        if (saved) {
                            productList.add(newProduct);
                            successCount++;
                        } else {
                            errorMessages.append(String.format("Product %d: Failed to save to database. Check logs for details.\n", i + 1));
                        }
                    }
                    handleResearch();
                    if (successCount == jsonArray.length()) {
                        showInfo("Success", String.format("Successfully added %d product(s).", successCount));
                    } else {
                        showWarning("Partial Success", String.format("Added %d product(s). Errors:\n%s", successCount, errorMessages.toString()));
                    }
                } catch (Exception e) {
                    showError("Script Error", "Failed to parse and save products: " + e.getMessage());
                }
            }
            return null;
        });
        dialog.showAndWait();
    }

    private void setupContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit");
        MenuItem deleteItem = new MenuItem("Delete");
        editItem.setOnAction(event -> {
            Produit selected = productTableView.getSelectionModel().getSelectedItem();
            handleEditProduct(selected);
        });
        deleteItem.setOnAction(event -> {
            Produit selected = productTableView.getSelectionModel().getSelectedItem();
            handleDeleteProduct(selected);
        });
        contextMenu.getItems().addAll(editItem, deleteItem);
        productTableView.setContextMenu(contextMenu);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}