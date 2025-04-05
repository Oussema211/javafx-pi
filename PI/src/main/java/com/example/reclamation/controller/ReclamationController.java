package com.example.reclamation.controller;

import com.example.reclamation.model.Reclamation; // Correct import
import com.example.reclamation.model.Tag;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.reclamation.service.ReclamationService;
import com.example.reclamation.service.TagService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ReclamationController {
   UUID CURRENT_USER_ID = SessionManager.getInstance().getLoggedInUser().getId();

    private final AuthService authService = new AuthService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final TagService tagService = new TagService();

    @FXML private BorderPane root;
    @FXML private GridPane contentContainer;
    @FXML private VBox mainContainer;
    @FXML private VBox sidebar;

    private Stage primaryStage;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    public void initialize() {
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #f5f7fa, #c3cfe2);");

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(75);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(25);
        contentContainer.getColumnConstraints().addAll(col1, col2);

        setupMainContainer();
        setupSidebar();
    }

    private void setupMainContainer() {
        mainContainer.getChildren().clear();
        mainContainer.setPadding(new Insets(10));

        HBox header = createHeader();
        List<Reclamation> reclamations = reclamationService.getAllReclamations();
        VBox reclamationList = reclamations.isEmpty() ? createEmptyState() : createReclamationList(reclamations);
        mainContainer.getChildren().addAll(header, reclamationList);
    }

    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #6C983B, #7AAE49); " +
                "-fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 4);");

        Label headerText = new Label("Reclamation Discussions");
        headerText.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        List<Reclamation> reclamations = reclamationService.getAllReclamations();
        Label headerCount = new Label(reclamations.size() + " active");
        headerCount.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-background-color: rgba(255,255,255,0.2); " +
                "-fx-padding: 5 10; -fx-background-radius: 20;");

        header.getChildren().addAll(headerText, headerCount);
        HBox.setHgrow(headerText, Priority.ALWAYS);
        return header;
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(10);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setPadding(new Insets(40));
        emptyState.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5);");

        Label icon = new Label("📪");
        icon.setStyle("-fx-font-size: 48px;");
        Label text = new Label("No discussions yet. Be the first to start one!");
        text.setStyle("-fx-font-size: 18px; -fx-text-fill: #666;");

        emptyState.getChildren().addAll(icon, text);
        return emptyState;
    }

    private VBox createReclamationList(List<Reclamation> reclamations) {
        VBox reclamationContainer = new VBox(10);
        reclamationContainer.setStyle("-fx-animation: fadeInUp 0.5s ease forwards;");

        for (Reclamation rec : reclamations) {
            User user = authService.getUserById(rec.getUserId());
            Tag tag = rec.getTagId() != null ? tagService.getTagById(rec.getTagId()) : null;
            HBox card = createReclamationCard(rec, user, tag);
            reclamationContainer.getChildren().add(card);
        }
        return reclamationContainer;
    }

    private HBox createReclamationCard(Reclamation rec, User user, Tag tag) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5); " +
                "-fx-cursor: hand;");
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-translate-y: -5; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 25, 0, 0, 8);"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5);"));

        StackPane profileContainer = new StackPane();
        String photoUrl = user != null && user.getPhotoUrl() != null ? "file:" + user.getPhotoUrl() : "file:images/admin.jpg";
        ImageView avatar = new ImageView(new Image(photoUrl, true));
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setClip(new Circle(25, 25, 23));
        avatar.setStyle("-fx-border-color: white; -fx-border-width: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Circle status = new Circle(7.5);
        status.setStyle("-fx-fill: " + (rec.getStatut().equals("fermee") ? "#ff5555" : rec.getStatut().equals("resolue") ? "#4CAF50" : "#999") + "; " +
                "-fx-stroke: white; -fx-stroke-width: 2;");
        StackPane.setAlignment(status, Pos.BOTTOM_RIGHT);
        profileContainer.getChildren().addAll(avatar, status);

        VBox contentWrapper = new VBox(5);
        HBox titleBar = new HBox(10);
        Label title = new Label(rec.getTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label tagLabel = new Label(tag != null ? "#" + tag.getName() : "#NoTag");
        tagLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: white; -fx-background-color: " + getTagColor(rec.getTagId()) + "; " +
                "-fx-padding: 3 8; -fx-background-radius: 20;");
        titleBar.getChildren().addAll(title, tagLabel);

        Label desc = new Label(rec.getDescription());
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-wrap-text: true;");

        HBox metaInfo = new HBox(10);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
        Label date = new Label(dateFormat.format(rec.getDateReclamation()));
        date.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");
        Label author = new Label("by " + (user != null ? user.getPrenom() : "Unknown"));
        author.setStyle("-fx-font-size: 12px; -fx-text-fill: #6C983B; -fx-font-weight: bold;");
        metaInfo.getChildren().addAll(date, author);

        contentWrapper.getChildren().addAll(titleBar, desc, metaInfo);

        HBox actionButtons = new HBox(5);
        if (rec.getUserId().toString().equals(CURRENT_USER_ID)) {
            Button editBtn = new Button("Edit"); // Replaced ✏️
            editBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 10;");
            editBtn.setOnAction(e -> handleEdit(rec.getId()));
            editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 50%; -fx-padding: 10;"));
            editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 10;"));

            Button deleteBtn = new Button("Delete"); // Replaced 🗑️
            deleteBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 10;");
            deleteBtn.setOnAction(e -> handleDelete(rec.getId()));
            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: #ff5555; -fx-text-fill: white; -fx-background-radius: 50%; -fx-padding: 10;"));
            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 10;"));

            actionButtons.getChildren().addAll(editBtn, deleteBtn);
        }

        card.getChildren().addAll(profileContainer, contentWrapper, actionButtons);
        HBox.setHgrow(contentWrapper, Priority.ALWAYS);

        if (rec.getStatut().equals("fermee")) {
            card.setStyle(card.getStyle() + "-fx-background-color: #fff5f5; -fx-border-color: #ff5555; -fx-border-width: 0 0 0 5;");
        } else if (rec.getStatut().equals("resolue")) {
            card.setStyle(card.getStyle() + "-fx-background-color: #f5fff5; -fx-border-color: #4CAF50; -fx-border-width: 0 0 0 5;");
        }

        return card;
    }

    private void setupSidebar() {
        // ... (unchanged, no encoding issues here)
    }

    private String getTagColor(UUID tagId) {
        if (tagId == null) return "#999999";
        String tagIdStr = tagId.toString();
        switch (tagIdStr) {
            case "11d2c8d3-f48e-11ef-a0dc-8c8caa96b2fa": return "#7AAE49"; // General Info
            case "20743b83-f48e-11ef-a0dc-8c8caa96b2fa": return "#FF5555"; // Wrong product
            case "294a4472-f48e-11ef-a0dc-8c8caa96b2fa": return "#FF9800"; // Illegal activity
            default: return "#999999"; // Default gray
        }
    }

    private void handleEdit(UUID reclamationId) {
        Dialog<Reclamation> dialog = new Dialog<>();
        dialog.setTitle("Edit Reclamation");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("ouverte", "resolue", "fermee");
        statusCombo.setPromptText("Status");

        Reclamation rec = reclamationService.getReclamationById(reclamationId);
        if (rec != null) {
            titleField.setText(rec.getTitle());
            descField.setText(rec.getDescription());
            statusCombo.setValue(rec.getStatut());
        }

        content.getChildren().addAll(new Label("Title:"), titleField, new Label("Description:"), descField, new Label("Status:"), statusCombo);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && rec != null) {
                rec.setTitle(titleField.getText());
                rec.setDescription(descField.getText());
                rec.setStatut(statusCombo.getValue());
                return rec;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedRec -> {
            if (reclamationService.updateReclamation(updatedRec)) {
                setupMainContainer(); // Refresh UI
            }
        });
    }

    private void handleDelete(UUID reclamationId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this reclamation?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && reclamationService.deleteReclamation(reclamationId)) {
                setupMainContainer(); // Refresh UI
            }
        });
    }

    private void handleNewDiscussion() {
        Dialog<Reclamation> dialog = new Dialog<>();
        dialog.setTitle("New Discussion");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        ComboBox<String> tagCombo = new ComboBox<>();
        tagCombo.getItems().addAll(tagService.getAllTags().stream().map(Tag::getName).toList());
        tagCombo.setPromptText("Tag");

        content.getChildren().addAll(new Label("Title:"), titleField, new Label("Description:"), descField, new Label("Tag:"), tagCombo);
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                Tag selectedTag = tagService.getTagByName(tagCombo.getValue());
                UUID tagId = selectedTag != null ? selectedTag.getId() : null;
                return new Reclamation(UUID.randomUUID(), CURRENT_USER_ID, tagId, new Date(), 0, 
                                       titleField.getText(), descField.getText(), "ouverte");
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newRec -> {
            if (reclamationService.addReclamation(newRec.getUserId(), newRec.getTagId(), newRec.getRate(), 
                                                  newRec.getTitle(), newRec.getDescription(), newRec.getStatut())) {
                setupMainContainer(); // Refresh UI
            }
        });
    }

    private void handleWriteReview() {
        System.out.println("Opening write review form - not implemented yet.");
    }
}