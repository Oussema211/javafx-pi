package com.example.reclamation.controller;

import com.example.reclamation.model.Reclamation;
import com.example.reclamation.model.Status;
import com.example.reclamation.service.MessageReclamationService;
import com.example.reclamation.service.ReclamationService;
import com.example.auth.utils.SessionManager;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReclamationDashboardController {

    
    @FXML private FlowPane reclamationsFlowPane;
    @FXML private TextField searchField;
    @FXML private Button clearSearchButton;
    @FXML private Button retrainButton;  // new button to retrain model


    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ReclamationService reclamationService = new ReclamationService();
    private final AuthService authService = new AuthService();
    private final MessageReclamationService messageReclamationService = new MessageReclamationService();
    private static final DropShadow CARD_SHADOW = new DropShadow(15, Color.gray(0.4, 0.6));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private ObservableList<Reclamation> reclamationsList;
    private ObservableList<Reclamation> filteredReclamations;
    private Stage primaryStage;

    @FXML
    public void initialize() {
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            System.err.println("No user logged in; should have been redirected to login");
            return;
        }


        setupFlowPane();
        setupSearch();
        setupRetrain();
        loadReclamations();
    }

    private void setupRetrain() {
        retrainButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 6 12;");
        retrainButton.setOnMouseEntered(e -> retrainButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 6 12;"));
        retrainButton.setOnMouseExited(e -> retrainButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 6 12;"));
        retrainButton.setOnAction(e -> {
            try {
                String result = messageReclamationService.retrainModel();
                showAlert("Model Retrained", result, Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                showAlert("Retrain Error", "Failed to retrain model: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void setupFlowPane() {
        reclamationsFlowPane.setStyle("-fx-background-color: transparent;");
        reclamationsFlowPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) reclamationsFlowPane.prefWidthProperty().bind(newScene.widthProperty());
        });
        reclamationsFlowPane.setPrefWidth(800);
    }

    private void setupSearch() {
        filteredReclamations = FXCollections.observableArrayList();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> filterReclamations(newValue.trim()));
        clearSearchButton.setOnAction(e -> {
            searchField.clear();
            filterReclamations("");
        });
    }
    private void filterReclamations(String query) {
        if (query.isEmpty()) filteredReclamations.setAll(reclamationsList);
        else {
            String lower = query.toLowerCase();
            filteredReclamations.setAll(reclamationsList.stream()
                .filter(r -> r.getTitle().toLowerCase().contains(lower) ||
                             r.getStatut().toString().toLowerCase().contains(lower))
                .collect(Collectors.toList()));
        }
        updateFlowPane();
    }

    private VBox createReclamationCard(Reclamation reclamation) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefSize(320, 280);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: linear-gradient(to bottom, #ffffff, #f1f5f9); " +
                      "-fx-border-color: #e2e8f0; -fx-border-radius: 15; -fx-background-radius: 15; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
    
        // Profile image
        AuthService authService = new AuthService();
        User user = authService.getUserById(reclamation.getUserId());
    
        ImageView profilePicture = new ImageView();
        profilePicture.setFitWidth(60);
        profilePicture.setFitHeight(60);
        profilePicture.setClip(new Circle(30, 30, 28));
    
        String profilePhotoPath = (user != null && user.getPhotoUrl() != null) ? user.getPhotoUrl() : null;
        if (profilePhotoPath != null && !profilePhotoPath.isEmpty()) {
            try {
                Image image = new Image(getClass().getResourceAsStream(profilePhotoPath));
                if (!image.isError()) {
                    profilePicture.setImage(image);
                } else {
                    loadFallbackImage(profilePicture, user);
                }
            } catch (Exception e) {
                loadFallbackImage(profilePicture, user);
            }
        } else {
            loadFallbackImage(profilePicture, user);
        }
    
        // Hover animation
        ScaleTransition hover = new ScaleTransition(Duration.millis(200), profilePicture);
        hover.setToX(1.1); hover.setToY(1.1);
        profilePicture.setOnMouseEntered(e -> {
            profilePicture.setEffect(new DropShadow(10, Color.gray(0.4, 0.6)));
            hover.play();
        });
        profilePicture.setOnMouseExited(e -> {
            profilePicture.setEffect(null);
            hover.setToX(1.0); hover.setToY(1.0);
            hover.play();
        });
    
        // Labels
        Label nameLabel = new Label((user != null ? user.getPrenom() + " " + user.getNom() : "Unknown"));
        nameLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b;");
    
        Label titleLabel = new Label(reclamation.getTitle());
        titleLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #1e3a8a; -fx-font-weight: bold;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(280);
    
        Label descLabel = new Label("Context: " + reclamation.getDescription());
        descLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #000000; -fx-font-weight: bold;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(280);
        descLabel.setMaxHeight(40);
        descLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
    
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(reclamation.getDateReclamation().getTime()), ZoneId.systemDefault());
        Label dateLabel = new Label("Date: " + DATE_FORMATTER.format(dateTime));
        dateLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b;");
    
        Label statusLabel = new Label(reclamation.getStatut().toString());
        statusLabel.setStyle(getStatusStyle(reclamation.getStatut().toString()));
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setPadding(new Insets(6, 12, 6, 12));
    
        Button editBtn = createIconButton("/icons/edit.png", "#3b82f6", "Edit Reclamation");
    editBtn.setOnAction(e -> showEditReclamationForm(reclamation));

    Button delBtn = createIconButton("/icons/delete.png", "#ef4444", "Delete Reclamation");
    delBtn.setOnAction(e -> deleteReclamation(reclamation));

    Button msgBtn = createIconButton("/icons/m.png", "#8b5cf6", "View Messages");
    msgBtn.setOnAction(e -> showMessagesWindow(reclamation));

    Button autoBtn = createIconButton("/icons/a.png", "#059669", "Generate Auto-Reply");
    autoBtn.setOnAction(e -> {
        try {
            String rep = messageReclamationService.generateAutoReply(sessionManager.getLoggedInUser().getId(), reclamation.getId());
            loadReclamations();
            showAlert("Auto-Reply Generated", rep, Alert.AlertType.INFORMATION);
        } catch (Exception ex) {
            showAlert("Error", "Failed to generate auto-reply: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    });

    Button csvBtn = createIconButton("/icons/c.png", "#10b981", "Export to CSV");
    csvBtn.setOnAction(e -> {
        try {
            String res = messageReclamationService.addReclamationToCsv(reclamation.getId());
            showAlert("CSV Export", res, Alert.AlertType.INFORMATION);
        } catch (Exception ex) {
            showAlert("CSV Error", "Failed to export CSV: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    });
    
        // Layouts
        HBox imageAndTitleBox = new HBox(12, profilePicture, new VBox(6, titleLabel, nameLabel));
        imageAndTitleBox.setAlignment(Pos.CENTER_LEFT);
    
        HBox topButtonBox = new HBox(8, autoBtn, csvBtn);
        topButtonBox.setAlignment(Pos.CENTER);
    
        HBox bottomButtonBox = new HBox(8, editBtn, delBtn, msgBtn);
        bottomButtonBox.setAlignment(Pos.CENTER);
    
        card.getChildren().addAll(imageAndTitleBox, descLabel, dateLabel, statusLabel, topButtonBox, bottomButtonBox);
        applyCardAnimation(card);
    
        return card;
    }
    
    
    private void loadFallbackImage(ImageView avatar, User user) {
    String initials = (user != null && user.getPrenom() != null && user.getNom() != null)
        ? (user.getPrenom().charAt(0) + "" ).toUpperCase()
        : "UU";
    StackPane textAvatar = new StackPane();
    Circle circle = new Circle(30);
    circle.setStyle("-fx-fill: #78909c;");
    Text text = new Text(initials);
    text.setStyle("-fx-font-size: 24px; -fx-fill: white; -fx-font-weight: bold;");
    textAvatar.getChildren().addAll(circle, text);
    textAvatar.setPrefSize(60, 60);
    textAvatar.setMaxSize(60, 60);

    avatar.setImage(null);
    avatar.setStyle("-fx-border-color: #6C983B; -fx-border-width: 2; -fx-border-radius: 50;");
    // Convert StackPane to Image for ImageView
    avatar.setImage(textAvatar.snapshot(null, null));
}

private Button createIconButton(String iconPath, String bgColor, String tooltipText) {
    Button button = new Button();
    try {
        Image image = new Image(getClass().getResourceAsStream(iconPath));
        ImageView icon = new ImageView(image);
        icon.setFitWidth(18);
        icon.setFitHeight(18);
        button.setGraphic(icon);
    } catch (Exception e) {
        System.err.println("Error loading icon: " + iconPath);
        button.setText("?");
    }

    button.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 50%; -fx-padding: 8; -fx-cursor: hand;");
    button.setPrefSize(32, 32);
    button.setEffect(new DropShadow(5, Color.gray(0.3)));

    String darkerColor = darkenColor(bgColor);
    button.setOnMouseEntered(e -> {
        button.setStyle("-fx-background-color: " + darkerColor + "; -fx-background-radius: 50%; -fx-padding: 8; -fx-cursor: hand;");
        ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
        st.setToX(1.1);
        st.setToY(1.1);
        st.play();
    });
    button.setOnMouseExited(e -> {
        button.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 50%; -fx-padding: 8; -fx-cursor: hand;");
        ScaleTransition st = new ScaleTransition(Duration.millis(200), button);
        st.setToX(1.0);
        st.setToY(1.0);
        st.play();
    });

    Tooltip tooltip = new Tooltip(tooltipText);
    tooltip.setStyle("-fx-font-size: 12px; -fx-background-color: rgba(44, 62, 80, 0.95); -fx-text-fill: white;");
    button.setTooltip(tooltip);

    return button;
}

    private void applyCardAnimation(VBox card) {
        ScaleTransition hover = new ScaleTransition(Duration.millis(200), card);
        hover.setFromX(1.0);
        hover.setFromY(1.0);
        hover.setToX(1.02);
        hover.setToY(1.02);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), card);
        fadeIn.setFromValue(0.3);
        fadeIn.setToValue(1.0);
        fadeIn.play();

        card.setOnMouseEntered(e -> {
            card.setEffect(CARD_SHADOW);
            hover.playFromStart();
        });
        card.setOnMouseExited(e -> {
            card.setEffect(null);
            hover.setRate(-1);
            hover.play();
        });
    }

    private String getStatusStyle(String status) {
        return switch (status.toUpperCase()) {
            case "WAITING" -> "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-font-family: 'Arial'; -fx-font-size: 12; -fx-background-radius: 12;";
            case "CLOSED" -> "-fx-background-color: #d1d5db; -fx-text-fill: #374151; -fx-font-weight: bold; -fx-font-family: 'Arial'; -fx-font-size: 12; -fx-background-radius: 12;";
            case "RESOLVED" -> "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-font-family: 'Arial'; -fx-font-size: 12; -fx-background-radius: 12;";
            default -> "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-family: 'Arial'; -fx-font-size: 12; -fx-background-radius: 12;";
        };
    }

    private Button createModernButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-family: 'Arial'; " +
                "-fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 6 12; -fx-font-size: 12;");
        button.setEffect(new DropShadow(5, Color.gray(0.3)));
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: " + darkenColor(color) +
                "; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 6 12; -fx-font-size: 12;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + color +
                "; -fx-text-fill: white; -fx-font-family: 'Arial'; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 6 12; -fx-font-size: 12;"));
        return button;
    }

    private String darkenColor(String color) {
        return switch (color) {
            case "#3b82f6" -> "#2563eb";
            case "#ef4444" -> "#dc2626";
            case "#8b5cf6" -> "#7c3aed";
            default -> color;
        };
    }

    private void loadReclamations() {
        var reclamations = reclamationService.getAllReclamations();
        reclamationsList = FXCollections.observableArrayList(reclamations);
        filteredReclamations.setAll(reclamationsList);
        updateFlowPane();
        applyFadeIn(reclamationsFlowPane);
    }

    private void updateFlowPane() {
        reclamationsFlowPane.getChildren().clear();
        for (Reclamation reclamation : filteredReclamations) {
            VBox card = createReclamationCard(reclamation);
            reclamationsFlowPane.getChildren().add(card);
        }
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
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

        Button saveButton = createModernButton("Save", "#10b981");
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

        stage.setScene(new Scene(form, 400, 500));
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

        Button saveButton = createModernButton("Update", "#10b981");
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

        stage.setScene(new Scene(form, 400, 450));
        stage.showAndWait();
    }

    private void deleteReclamation(Reclamation reclamation) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete: " + reclamation.getTitle());
        confirm.setContentText("Are you sure?");
        confirm.getDialogPane().setStyle("-fx-background-color: #fee2e2; -fx-border-color: #ef4444; -fx-border-width: 2;");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (reclamationService.deleteReclamation(reclamation.getId())) {
                loadReclamations();
                showAlert("Success", "Reclamation deleted successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to delete reclamation", Alert.AlertType.ERROR);
            }
        }
    }
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }
    private void showMessagesWindow(Reclamation reclamation) {
        try {
            // Stage currentStage = (Stage) reclamationsFlowPane.getScene().getWindow();
            // currentStage.close();
    
            Stage messagesStage = new Stage();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/reclamation/ReclamationMessages.fxml"));
            Parent root = loader.load();
    
    
            ReclamationMessagesController controller = loader.getController();
            controller.setSelectedReclamation(reclamation);
            controller.setPrimaryStage(messagesStage); 
    
            messagesStage.setTitle("Messages - " + reclamation.getTitle());
            messagesStage.setScene(new Scene(root, 800, 600));
            messagesStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to open messages window", Alert.AlertType.ERROR);
        }
    }


    private Stage createStyledStage(String title) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        return stage;
    }

    private VBox createStyledForm() {
        VBox form = new VBox(15);
        form.setPadding(new Insets(25));
        form.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e2e8f0; -fx-border-radius: 15; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        applyFadeIn(form);
        return form;
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#1e3a8a"));
        return label;
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'Arial'; -fx-font-size: 14; -fx-padding: 8;");
        return field;
    }

    private TextArea createStyledTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefHeight(120);
        area.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'Arial'; -fx-font-size: 14; -fx-padding: 8;");
        return area;
    }

    private ComboBox<String> createStyledComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #cbd5e1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'Arial'; -fx-font-size: 14;");
        return combo;
    }

    private void applyFadeIn(Region node) {
        FadeTransition fade = new FadeTransition(Duration.millis(600), node);
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
                (type == Alert.AlertType.ERROR ? "#fee2e2" : "#d1fae5") +
                "; -fx-border-color: " + (type == Alert.AlertType.ERROR ? "#ef4444" : "#10b981") + "; -fx-border-width: 2; -fx-font-family: 'Arial';");
        alert.showAndWait();
    }
}