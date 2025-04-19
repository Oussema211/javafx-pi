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
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
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
    @FXML private Button researchButton;
    @FXML private Button deleteSelectedButton;
    @FXML private Button exportSelectedButton;
    @FXML private Label resultsCountLabel;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private ObservableList<Produit> productList = FXCollections.observableArrayList();
    private ObservableList<Categorie> categoryList = FXCollections.observableArrayList();
    private FilteredList<Produit> filteredList;
    private static final String TEXTCORTEX_API_KEY = "gAAAAABoAtEETm6B9AD03NeVrbiZO8f9DUqIIohjLqf08F2j1kXt5dV7f4srb6uyv26bQcaj1-KIA997E6h87YXWWHYaMN9iIAz3MVJg1kJKOcpAxaqhEjlyrDZ6e5aQo-QjtVeVWIIaQntdMPKqDPBsRWyxjtOhC-Z9Yuf37sungaGnwp-oBUE=";
    private static final String TEXTCORTEX_API_URL = "https://api.textcortex.com/v1/texts/products/descriptions";

    @FXML
    public void initialize() {
        try {
            productTableView.setEditable(true);
            configureTableColumns();
            initializeComboBoxes();
            loadInitialData();
            setupContextMenu();
            setupActionsColumn();
            setupResultsCountListener();
            setupSearchListeners();
            setupInputValidation();
            setupSelectionListener();
            System.out.println("ProductController initialized successfully");
        } catch (Exception e) {
            System.err.println("Error in initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void configureTableColumns() {
        selectColumn.setCellValueFactory(new PropertyValueFactory<>("selected"));
        selectColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectColumn));
        selectColumn.setEditable(true);
        System.out.println("selectColumn configured with CheckBoxTableCell");

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

        imagePreviewColumn.setCellFactory(param -> new TableCell<Produit, String>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        Image image = new Image(new File(imagePath).toURI().toString());
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
        categoryNames.addAll(categoryList.stream().map(Categorie::getNom).toList());
        categoryComboBox.setItems(categoryNames);
        categoryComboBox.getSelectionModel().selectFirst();

        rateComboBox.getItems().addAll("All", "4+", "3+", "2+", "1+");
        rateComboBox.getSelectionModel().selectFirst();
    }

    private void loadInitialData() {
        categoryList.addAll(CategorieDAO.getAllCategories());
        initializeComboBoxes();
        productList.addAll(ProduitDAO.getAllProducts());
        filteredList = new FilteredList<>(productList, p -> true);
        productTableView.setItems(filteredList);
        updateResultsCount();
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
        productList.forEach(product -> {
            product.selectedProperty().addListener((obs, oldVal, newVal) -> {
                System.out.println("Checkbox for " + product.getNom() + " changed to " + newVal);
                updateBulkButtons();
            });
        });
        filteredList.addListener((javafx.collections.ListChangeListener<Produit>) c -> updateBulkButtons());
    }

    private void updateBulkButtons() {
        boolean hasSelection = productList.stream().anyMatch(Produit::isSelected);
        deleteSelectedButton.setDisable(!hasSelection);
        exportSelectedButton.setDisable(!hasSelection);
        System.out.println("Bulk buttons updated. Has selection: " + hasSelection);
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
            } catch (NumberFormatException e) {
                // Invalid input; skip price filter
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
            } catch (NumberFormatException e) {
                // Invalid input; skip quantity filter
            }

            String rateFilter = rateComboBox.getValue();
            if (rateFilter != null && !rateFilter.equals("All")) {
                float minRate = Float.parseFloat(rateFilter.replace("+", ""));
                match &= product.getRate() != null && product.getRate() >= minRate;
            }

            LocalDate selectedDate = datePicker.getValue();
            if (selectedDate != null) {
                LocalDateTime startOfDay = selectedDate.atStartOfDay();
                match &= product.getDateCreation() != null && !product.getDateCreation().isBefore(startOfDay);
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
            selectedProducts.forEach(product -> {
                ProduitDAO.deleteProduct(product.getId());
                productList.remove(product);
            });
            handleResearch();
        }
    }

    @FXML
    private void exportToPDF() {
        List<Produit> selectedProducts = productList.stream()
                .filter(Produit::isSelected)
                .collect(Collectors.toList());

        if (selectedProducts.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select at least one product to export.");
            alert.showAndWait();
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

                // Load Inter font
                PdfFont font;
                try {
                    font = PdfFontFactory.createFont(getClass().getResource("/com/example/fonts/Inter-Regular.ttf").toExternalForm());
                } catch (Exception e) {
                    System.err.println("Inter font not found, falling back to Helvetica");
                    font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                }

                PdfFont boldFont;
                try {
                    boldFont = PdfFontFactory.createFont(getClass().getResource("/com/example/fonts/Inter-Bold.ttf").toExternalForm());
                } catch (Exception e) {
                    boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
                }

                // Title
                Paragraph title = new Paragraph("Selected Products Report")
                        .setFont(boldFont)
                        .setFontSize(18)
                        .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(99, 102, 241)) // #6366f1
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(10);
                document.add(title);

                // Timestamp
                Paragraph timestamp = new Paragraph("Generated on " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .setFont(font)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setMarginBottom(20);
                document.add(timestamp);

                // Table
                float[] columnWidths = {80, 100, 150, 80, 60, 60, 100, 50, 100};
                Table table = new Table(UnitValue.createPointArray(columnWidths));
                table.setWidth(UnitValue.createPercentValue(100));

                // Headers
                String[] headers = {"ID", "Name", "Description", "Category", "Price", "Quantity", "Date Created", "Rate", "Image Name"};
                for (String header : headers) {
                    table.addHeaderCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(header)
                                    .setFont(boldFont)
                                    .setFontSize(10)
                                    .setFontColor(new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255)))
                            .setBackgroundColor(new com.itextpdf.kernel.colors.DeviceRgb(99, 102, 241)) // #6366f1
                            .setTextAlignment(TextAlignment.CENTER)
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1)) // #e2e8f0
                    );
                }

                // Rows
                boolean alternate = false;
                for (Produit product : selectedProducts) {
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getId().toString())
                                    .setFont(font)
                                    .setFontSize(9))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252) : // #f8fafc
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255)) // #ffffff
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1))
                    );
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getNom())
                                    .setFont(font)
                                    .setFontSize(9))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1))
                    );
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getDescription())
                                    .setFont(font)
                                    .setFontSize(9))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1))
                    );
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getCategory() != null ? product.getCategory().getNom() : "No Category")
                                    .setFont(font)
                                    .setFontSize(9))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1))
                    );
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(String.format("%.2f", product.getPrixUnitaire()))
                                    .setFont(font)
                                    .setFontSize(9))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1))
                    );
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(String.valueOf(product.getQuantite()))
                                    .setFont(font)
                                    .setFontSize(9))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1))
                    );
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getDateCreation().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                                    .setFont(font)
                                    .setFontSize(9))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1))
                    );
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getRate() != null ? String.format("%.1f", product.getRate()) : "")
                                    .setFont(font)
                                    .setFontSize(9))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1))
                    );
                    table.addCell(new com.itextpdf.layout.element.Cell()
                            .add(new Paragraph(product.getImageName() != null ? product.getImageName() : "")
                                    .setFont(font)
                                    .setFontSize(9))
                            .setBackgroundColor(alternate ?
                                    new com.itextpdf.kernel.colors.DeviceRgb(248, 250, 252) :
                                    new com.itextpdf.kernel.colors.DeviceRgb(255, 255, 255))
                            .setBorder(new com.itextpdf.layout.borders.SolidBorder(
                                    new com.itextpdf.kernel.colors.DeviceRgb(226, 232, 240), 1))
                    );
                    alternate = !alternate;
                }

                document.add(table);
                document.close();

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Export Successful");
                success.setHeaderText(null);
                success.setContentText("Selected products exported to " + file.getName());
                success.showAndWait();
            } catch (Exception e) {
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Export Failed");
                error.setHeaderText(null);
                error.setContentText("Failed to export products to PDF: " + e.getMessage());
                error.showAndWait();
                e.printStackTrace();
            }
        }
    }

    private void setupActionsColumn() {
        actionsColumn.setCellFactory(param -> new TableCell<Produit, Void>() {
            private final Button editButton = new Button();
            private final Button deleteButton = new Button();
            private final HBox hbox = new HBox(10);

            {
                Image editImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/edit.png")));
                Image deleteImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/delete.png")));
                editButton.setGraphic(new ImageView(editImage));
                deleteButton.setGraphic(new ImageView(deleteImage));
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
                ProduitDAO.deleteProduct(product.getId());
                productList.remove(product);
                handleResearch();
            }
        }
    }

    private String generateProductDescription(String brand, String category, String name) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            JSONObject payload = new JSONObject();
            payload.put("name", name);
            payload.put("category", category);
            payload.put("formality", "default");
            payload.put("max_tokens", 200);
            payload.put("n", 1);
            payload.put("source_lang", "fr");
            payload.put("target_lang", "fr");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TEXTCORTEX_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + TEXTCORTEX_API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("TextCortex API Response: " + response.body()); // Debug
            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                JSONArray outputs = jsonResponse.getJSONObject("data").getJSONArray("outputs");
                if (!outputs.isEmpty()) {
                    String description = outputs.getJSONObject(0).getString("text");
                    return description.length() > 200 ? description.substring(0, 200) : description;
                }
            } else {
                System.err.println("TextCortex API Error: " + response.statusCode() + " - " + response.body());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("API Error");
                alert.setHeaderText(null);
                alert.setContentText("Failed to generate description: HTTP " + response.statusCode());
                alert.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("API Error");
            alert.setHeaderText(null);
            alert.setContentText("Failed to generate description: " + e.getMessage());
            alert.showAndWait();
        }
        return "";
    }

    private void showProductDialog(Produit product) {
        Dialog<Produit> dialog = new Dialog<>();
        dialog.setTitle(product == null ? "New Product" : "Edit Product");

        DialogPane dialogPane = dialog.getDialogPane();
        URL cssUrl = getClass().getResource("/com/example/css/produitdialog.css");
        if (cssUrl != null) {
            dialogPane.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Warning: CSS file /com/example/css/produitdialog.css not found. Using default styles.");
            dialogPane.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-padding: 10;");
        }
        dialogPane.getStyleClass().add("dialog-pane");

        TextField nameField = new TextField();
        TextArea descriptionField = new TextArea();
        ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(
                categoryList.stream().map(Categorie::getNom).toList()
        ));
        TextField priceField = new TextField();
        TextField quantityField = new TextField();
        TextField imagePathField = new TextField();
        imagePathField.setEditable(false);
        Button chooseImageButton = new Button("Choose Image");
        Button generateDescriptionButton = new Button("Generate Description");
        generateDescriptionButton.getStyleClass().add("secondary-button");
        ImageView imagePreview = new ImageView();
        imagePreview.setFitWidth(100);
        imagePreview.setFitHeight(100);
        imagePreview.setPreserveRatio(true);

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
                imagePathField.setText(file.getAbsolutePath());
                try {
                    Image image = new Image(file.toURI().toString());
                    imagePreview.setImage(image);
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load image.");
                    alert.showAndWait();
                }
            }
        });

        generateDescriptionButton.setOnAction(e -> {
            String name = nameField.getText().trim();
            String category = categoryCombo.getValue();
            if (name.isEmpty() || category == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Input Required");
                alert.setHeaderText(null);
                alert.setContentText("Please enter a product name and select a category.");
                alert.showAndWait();
                return;
            }
            String description = generateProductDescription(name, category, name);
            if (!description.isEmpty()) {
                descriptionField.setText(description);
            }
        });

        if (product != null) {
            nameField.setText(product.getNom());
            descriptionField.setText(product.getDescription());
            categoryCombo.getSelectionModel().select(product.getCategory() != null ? product.getCategory().getNom() : null);
            priceField.setText(String.valueOf(product.getPrixUnitaire()));
            quantityField.setText(String.valueOf(product.getQuantite()));
            imagePathField.setText(product.getImageName() != null ? product.getImageName() : "");
            if (product.getImageName() != null && !product.getImageName().isEmpty()) {
                try {
                    Image image = new Image(new File(product.getImageName()).toURI().toString());
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
        grid.addRow(11, new Label("Image:"), imagePathField);
        grid.addRow(12, new Label(""), chooseImageButton);
        grid.addRow(13, new Label("Preview:"), imagePreview);

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
                    newProduct.setDescription(descriptionField.getText().trim());

                    String selectedCategoryName = categoryCombo.getValue();
                    Categorie selectedCategory = categoryList.stream()
                            .filter(c -> c.getNom().equals(selectedCategoryName))
                            .findFirst()
                            .orElse(null);

                    newProduct.setCategory(selectedCategory);
                    newProduct.setPrixUnitaire(Float.parseFloat(priceField.getText()));
                    newProduct.setQuantite(Integer.parseInt(quantityField.getText()));
                    newProduct.setImageName(imagePathField.getText());

                    User currentUser = sessionManager.getLoggedInUser();
                    if (product == null) {
                        newProduct.setId(UUID.randomUUID());
                        newProduct.setDateCreation(LocalDateTime.now());
                        newProduct.setUserId(currentUser != null ? currentUser.getId() : null);
                        ProduitDAO.saveProduct(newProduct);
                        productList.add(newProduct);
                    } else {
                        ProduitDAO.updateProduct(newProduct);
                        productTableView.refresh();
                    }
                    handleResearch();
                    return newProduct;
                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid input format.");
                    alert.showAndWait();
                    return null;
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
}
