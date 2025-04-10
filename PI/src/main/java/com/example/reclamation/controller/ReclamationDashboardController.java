package com.example.reclamation.controller;

import com.example.reclamation.model.Reclamation;
import com.example.reclamation.model.Status;
import com.example.reclamation.service.ReclamationService;
import utils.SessionManager;
import com.example.auth.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.function.Function;

public class ReclamationDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private TableView<Reclamation> reclamationsTable;
    @FXML private TableColumn<Reclamation, String> userIdColumn;
    @FXML private TableColumn<Reclamation, java.util.Date> dateColumn;
    @FXML private TableColumn<Reclamation, Integer> rateColumn;
    @FXML private TableColumn<Reclamation, String> titleColumn;
    @FXML private TableColumn<Reclamation, String> statusColumn;
    @FXML private TableColumn<Reclamation, Void> actionsColumn;

    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ReclamationService reclamationService = new ReclamationService();
    private static final DropShadow CELL_SHADOW = new DropShadow(10, Color.gray(0.4, 0.5));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            System.err.println("No user logged in; should have been redirected to login");
            return;
        }

        welcomeLabel.setText("Welcome, " + user.getPrenom() + " " + user.getNom() + "!");
        welcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        welcomeLabel.setTextFill(Color.web("#34495e"));
        applyFadeIn(welcomeLabel);

        setupModernTable();
        loadReclamations();
    }

    private void setupModernTable() {
        reclamationsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        URL cssResource = getClass().getResource("/com/example/reclamation/modern-table.css");
        if (cssResource != null) {
            reclamationsTable.getStylesheets().add(cssResource.toExternalForm());
        } else {
            System.err.println("CSS resource not found!");
        }
        reclamationsTable.setFixedCellSize(40);


        configureColumn(userIdColumn, r -> r.getUserId().toString());
        configureColumn(dateColumn, Reclamation::getDateReclamation);
        configureColumn(rateColumn, Reclamation::getRate);
        configureColumn(titleColumn, Reclamation::getTitle);

        // Status column
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatut().toString()));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    setGraphic(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER);
                    setPadding(new Insets(8));
                    setStyle(getStatusStyle(item));
                    applyCellAnimation(this);
                }
            }
        });

        // Actions column with edit/delete/messages buttons
        actionsColumn.setCellFactory(col -> {
            TableCell<Reclamation, Void> cell = new TableCell<>() {
                private final Button editButton = createModernButton("Edit", "#3498db");
                private final Button deleteButton = createModernButton("Delete", "#e74c3c");
                private final Button messagesButton = createModernButton("Messages", "#9b59b6");
                private final HBox buttonBox = new HBox(10, editButton, deleteButton, messagesButton);

                {
                    buttonBox.setAlignment(Pos.CENTER);
                    editButton.setOnAction(e -> showEditReclamationForm(getTableView().getItems().get(getIndex())));
                    deleteButton.setOnAction(e -> deleteReclamation(getTableView().getItems().get(getIndex())));
                    messagesButton.setOnAction(e -> showMessagesWindow(getTableView().getItems().get(getIndex())));
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : buttonBox);
                    if (!empty) applyCellAnimation(this);
                }
            };
            return cell;
        });
    }

    private void showMessagesWindow(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pages/ReclamationMessages.fxml"));
            Parent root = loader.load();

            MessagesRecController controller = loader.getController();
            controller.initData(reclamation, sessionManager.getLoggedInUser().getId());

            Stage messagesStage = new Stage();
            messagesStage.initModality(Modality.WINDOW_MODAL);
            messagesStage.initOwner(reclamationsTable.getScene().getWindow());
            messagesStage.setTitle("Messages - " + reclamation.getTitle());
            messagesStage.setScene(new Scene(root, 800, 600));
            messagesStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open messages window", Alert.AlertType.ERROR);
        }
    }

    /**
     * Generic method to configure a TableColumn with any data type.
     * @param column The table column to configure.
     * @param valueExtractor Function to extract the value from a Reclamation object.
     * @param <T> The type of data the column displays.
     */
    private <T> void configureColumn(TableColumn<Reclamation, T> column, Function<Reclamation, T> valueExtractor) {
        column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(valueExtractor.apply(cellData.getValue())));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    // Enhanced date formatting
                    if (item instanceof java.util.Date date) {
                        LocalDateTime dateTime = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
                        setText(DATE_FORMATTER.format(dateTime));
                    } else {
                        setText(String.valueOf(item));
                    }
                    setAlignment(Pos.CENTER);
                    setPadding(new Insets(8));
                    // Updated style with explicit text color
                    setStyle("-fx-background-color: #ffffff; -fx-text-fill: #2c3e50; -fx-font-family: 'Arial'; -fx-font-size: 14;");
                    applyCellAnimation(this);
                }
            }
        });
    }

    private void applyCellAnimation(TableCell<?, ?> cell) {
        cell.setEffect(null);
        ScaleTransition hover = new ScaleTransition(Duration.millis(200), cell);
        hover.setFromX(1.0);
        hover.setFromY(1.0);
        hover.setToX(1.05);
        hover.setToY(1.05);

        cell.setOnMouseEntered(e -> {
            cell.setEffect(CELL_SHADOW);
            hover.playFromStart();
        });
        cell.setOnMouseExited(e -> {
            cell.setEffect(null);
            hover.setRate(-1);
            hover.play();
        });
    }

    private String getStatusStyle(String status) {
        return switch (status.toUpperCase()) {  // Changed to uppercase to match your enum
            case "WAITING" -> "-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold;";
            case "CLOSED" -> "-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;";
            case "RESOLVED" -> "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;";
            default -> "-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50;";
        };
    }

    private Button createModernButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-family: 'Arial'; " +
                "-fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 15;");
        button.setEffect(new DropShadow(5, Color.gray(0.5)));
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: " + darkenColor(color) +
                "; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 15;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + color +
                "; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 5 15;"));
        return button;
    }

    private String darkenColor(String color) {
        return switch (color) {
            case "#3498db" -> "#2980b9";
            case "#e74c3c" -> "#c0392b";
            case "#2ecc71" -> "#27ae60";
            default -> color;
        };
    }

    private void loadReclamations() {
        var reclamations = reclamationService.getAllReclamations();
        reclamationsTable.setItems(FXCollections.observableArrayList(reclamations));
        applyFadeIn(reclamationsTable);
    }

    @FXML
    private void showAddReclamationForm() {
        Stage stage = createStyledStage("Add New Reclamation");
        VBox form = createStyledForm();

        TextField userIdField = createStyledTextField("User ID");
        TextField rateField = createStyledTextField("Rate (1-5)");
        TextField titleField = createStyledTextField("Title");
        TextArea descriptionField = createStyledTextArea("Description");
        ComboBox<String> statusCombo = createStyledComboBox();
        statusCombo.getItems().addAll("WAITING", "CLOSED", "RESOLVED");
        statusCombo.setValue("WAITING");

        Button saveButton = createModernButton("Save", "#2ecc71");
        saveButton.setOnAction(e -> {
            try {
                UUID userId = UUID.fromString(userIdField.getText());
                int rate = Integer.parseInt(rateField.getText());
                if (rate < 1 || rate > 5) throw new IllegalArgumentException("Rate must be between 1 and 5");
                reclamationService.addReclamation(
                    userId, null, rate, titleField.getText(),
                    descriptionField.getText(), Status.valueOf(statusCombo.getValue())
                );
                loadReclamations();
                stage.close();
                showAlert("Success", "Reclamation added successfully!", Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                showAlert("Error", "Failed to add: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        form.getChildren().addAll(
            createStyledLabel("User ID:"), userIdField,
            createStyledLabel("Rate:"), rateField,
            createStyledLabel("Title:"), titleField,
            createStyledLabel("Description:"), descriptionField,
            createStyledLabel("Status:"), statusCombo,
            saveButton
        );

        stage.setScene(new Scene(form, 350, 450));
        stage.showAndWait();
    }

    private void showEditReclamationForm(Reclamation reclamation) {
        Stage stage = createStyledStage("Edit Reclamation");
        VBox form = createStyledForm();

        TextField rateField = createStyledTextField("Rate (1-5)");
        rateField.setText(String.valueOf(reclamation.getRate()));
        TextField titleField = createStyledTextField("Title");
        titleField.setText(reclamation.getTitle());
        TextArea descriptionField = createStyledTextArea("Description");
        descriptionField.setText(reclamation.getDescription());
        ComboBox<String> statusCombo = createStyledComboBox();
        statusCombo.getItems().addAll("WAITING", "CLOSED", "RESOLVED");
        statusCombo.setValue(reclamation.getStatut().toString());

        Button saveButton = createModernButton("Update", "#2ecc71");
        saveButton.setOnAction(e -> {
            try {
                int rate = Integer.parseInt(rateField.getText());
                if (rate < 1 || rate > 5) throw new IllegalArgumentException("Rate must be between 1 and 5");
                reclamation.setRate(rate);
                reclamation.setTitle(titleField.getText());
                reclamation.setDescription(descriptionField.getText());
                reclamation.setStatut(Status.valueOf(statusCombo.getValue()));
                if (reclamationService.updateReclamation(reclamation)) {
                    loadReclamations();
                    stage.close();
                    showAlert("Success", "Reclamation updated successfully!", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Error", "Failed to update reclamation", Alert.AlertType.ERROR);
                }
            } catch (Exception ex) {
                showAlert("Error", "Failed to update: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        form.getChildren().addAll(
            createStyledLabel("Rate:"), rateField,
            createStyledLabel("Title:"), titleField,
            createStyledLabel("Description:"), descriptionField,
            createStyledLabel("Status:"), statusCombo,
            saveButton
        );

        stage.setScene(new Scene(form, 350, 400));
        stage.showAndWait();
    }

    private void deleteReclamation(Reclamation reclamation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete: " + reclamation.getTitle());
        confirm.setContentText("Are you sure?");
        confirm.getDialogPane().setStyle("-fx-background-color: #ffebee; -fx-border-color: #e74c3c; -fx-border-width: 2;");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (reclamationService.deleteReclamation(reclamation.getId())) {
                loadReclamations();
                showAlert("Success", "Reclamation deleted successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to delete reclamation", Alert.AlertType.ERROR);
            }
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        sessionManager.clearSession();
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
        stage.setScene(new Scene(root));
    }

    // Styling Helpers
    private Stage createStyledStage(String title) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        return stage;
    }

    private VBox createStyledForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(20));
        form.setStyle("-fx-background-color: #ffffff; -fx-border-color: #3498db; -fx-border-radius: 10; -fx-background-radius: 10;");
        form.setEffect(new DropShadow(10, Color.gray(0.3)));
        applyFadeIn(form);
        return form;
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#2c3e50"));
        return label;
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-family: 'Arial';");
        return field;
    }

    private TextArea createStyledTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefHeight(100);
        area.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-family: 'Arial';");
        return area;
    }

    private ComboBox<String> createStyledComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-family: 'Arial';");
        return combo;
    }

    private void applyFadeIn(Region node) {
        FadeTransition fade = new FadeTransition(Duration.millis(800), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.getDialogPane().setStyle("-fx-background-color: " +
                (type == Alert.AlertType.ERROR ? "#ffebee" : "#e8f5e9") +
                "; -fx-border-color: " + (type == Alert.AlertType.ERROR ? "#e74c3c" : "#2ecc71") + "; -fx-border-width: 2; -fx-font-family: 'Arial';");
        alert.showAndWait();
    }
}