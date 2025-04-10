package com.example.reclamation.controller;

import com.example.reclamation.model.Reclamation;
import com.example.reclamation.model.Tag;
import com.example.reclamation.model.Status;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.utils.SessionManager;
import com.example.reclamation.service.ReclamationService;
import com.example.reclamation.service.TagService;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ReclamationController {

    private final AuthService authService = new AuthService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final TagService tagService = new TagService();
    private final SessionManager sessionManager = SessionManager.getInstance();

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

        // Add click handler to navigate to ReclamationMessages
        card.setOnMouseClicked(e -> handleReclamationClick(rec));

        StackPane profileContainer = new StackPane();
        String photoUrl = user != null && user.getPhotoUrl() != null ? "file:" + user.getPhotoUrl() : "file:images/admin.jpg";
        ImageView avatar = new ImageView(new Image(photoUrl, true));
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setClip(new Circle(25, 25, 23));
        avatar.setStyle("-fx-border-color: white; -fx-border-width: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        Circle status = new Circle(7.5);
        String statusColor = switch (rec.getStatut()) {
            case CLOSED -> "#ff5555";  // Red for closed
            case RESOLVED -> "#4CAF50"; // Green for resolved
            case REVIEW -> "#FF9800";   // Orange for review
            case WAITING -> "#999";     // Gray for waiting
        };
        status.setStyle("-fx-fill: " + statusColor + "; -fx-stroke: white; -fx-stroke-width: 2;");
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
        User currentUser = sessionManager.getLoggedInUser();
        if (currentUser != null && rec.getUserId().equals(currentUser.getId())) {
            Button editBtn = new Button("Edit");
            editBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 10;");
            editBtn.setOnAction(e -> handleEdit(rec.getId()));
            editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 50%; -fx-padding: 10;"));
            editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 10;"));

            Button deleteBtn = new Button("Delete");
            deleteBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 10;");
            deleteBtn.setOnAction(e -> handleDelete(rec.getId()));
            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: #ff5555; -fx-text-fill: white; -fx-background-radius: 50%; -fx-padding: 10;"));
            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 10;"));

            actionButtons.getChildren().addAll(editBtn, deleteBtn);
        }

        card.getChildren().addAll(profileContainer, contentWrapper, actionButtons);
        HBox.setHgrow(contentWrapper, Priority.ALWAYS);

        if (rec.getStatut() == Status.CLOSED) {
            card.setStyle(card.getStyle() + "-fx-background-color: #fff5f5; -fx-border-color: #ff5555; -fx-border-width: 0 0 0 5;");
        } else if (rec.getStatut() == Status.RESOLVED) {
            card.setStyle(card.getStyle() + "-fx-background-color: #f5fff5; -fx-border-color: #4CAF50; -fx-border-width: 0 0 0 5;");
        }

        return card;
    }

    private void handleReclamationClick(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/reclamation/ReclamationMessages.fxml"));
            Parent messagesRoot = loader.load();

            ReclamationMessagesController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);
            controller.setSelectedReclamation(reclamation);

            Scene messagesScene = new Scene(messagesRoot, primaryStage.getWidth(), primaryStage.getHeight());
            primaryStage.setScene(messagesScene);
            primaryStage.setTitle("Reclamation Messages - " + reclamation.getTitle());
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load messages page.");
            alert.showAndWait();
        }
    }

    // Rest of the methods remain unchanged...

    private void setupSidebar() {
        sidebar.getChildren().clear();
        sidebar.setPadding(new Insets(20));
        sidebar.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5);");

        HBox searchWrapper = new HBox(10);
        TextField searchInput = new TextField();
        searchInput.setPromptText("Search discussions...");
        searchInput.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 25; -fx-padding: 10 10 10 30; " +
                "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        searchInput.focusedProperty().addListener((obs, old, newVal) -> {
            if (newVal) {
                searchInput.setStyle("-fx-background-color: white; -fx-background-radius: 25; -fx-padding: 10 10 10 30; " +
                        "-fx-effect: dropshadow(gaussian, rgba(108,152,59,0.2), 0, 0, 0, 3);");
            } else {
                searchInput.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 25; -fx-padding: 10 10 10 30; " +
                        "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");
            }
        });
        searchWrapper.getChildren().add(searchInput);

        VBox sidebarActions = new VBox(10);
        Button newDiscussionBtn = new Button("New Discussion");
        newDiscussionBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(108,152,59,0.2), 15, 0, 0, 4);");
        newDiscussionBtn.setOnMouseEntered(e -> newDiscussionBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(108,152,59,0.4), 20, 0, 0, 6); -fx-translate-y: -2;"));
        newDiscussionBtn.setOnMouseExited(e -> newDiscussionBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(108,152,59,0.2), 15, 0, 0, 4);"));
        newDiscussionBtn.setOnAction(e -> handleNewDiscussion());

        Button writeReviewBtn = new Button("Write Review");
        writeReviewBtn.setStyle("-fx-background-color: #7AAE49; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(108,152,59,0.2), 15, 0, 0, 4);");
        writeReviewBtn.setOnMouseEntered(e -> writeReviewBtn.setStyle("-fx-background-color: #7AAE49; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(108,152,59,0.4), 20, 0, 0, 6); -fx-translate-y: -2;"));
        writeReviewBtn.setOnMouseExited(e -> writeReviewBtn.setStyle("-fx-background-color: #7AAE49; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(108,152,59,0.2), 15, 0, 0, 4);"));
        writeReviewBtn.setOnAction(e -> handleWriteReview());

        sidebarActions.getChildren().addAll(newDiscussionBtn, writeReviewBtn);

        VBox sidebarInfo = new VBox(10);
        sidebarInfo.setAlignment(Pos.CENTER);
        sidebarInfo.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 10;");
        Label infoText = new Label("Click any discussion to join the conversation");
        infoText.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        HBox stats = new HBox(20);
        stats.setAlignment(Pos.CENTER);
        Label members = new Label(authService.getAllUsers().size() + " Members");
        Label posts = new Label(reclamationService.getAllReclamations().size() + " Posts");
        stats.getChildren().addAll(members, posts);
        stats.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        sidebarInfo.getChildren().addAll(infoText, stats);

        sidebar.getChildren().addAll(searchWrapper, sidebarActions, sidebarInfo);
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
        ComboBox<Status> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(Status.values());
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
                setupMainContainer();
            }
        });
    }

    private void handleDelete(UUID reclamationId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this reclamation?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && reclamationService.deleteReclamation(reclamationId)) {
                setupMainContainer();
            }
        });
    }

    private void handleNewDiscussion() {
        User currentUser = sessionManager.getLoggedInUser();
        if (currentUser == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "You must be logged in to create a discussion.");
            alert.showAndWait();
            return;
        }

        Dialog<Reclamation> dialog = new Dialog<>();
        dialog.setTitle("New Discussion");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        VBox content = new VBox(25);
        content.setPadding(new Insets(40));
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(700);
        content.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, white, #f5f6fa);" +
            "-fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 30, 0, 0, 10);"
        );

        Label titleLabel = new Label("Submit Your Problem");
        titleLabel.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 2.2em;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1a1a1a;" +
            "-fx-padding: 0 0 15 0;"
        );

        Label subtitle = new Label("We value your feedback. Please describe your concern below.");
        subtitle.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1.1em;" +
            "-fx-text-fill: #666;" +
            "-fx-padding: 0 0 30 0;"
        );

        VBox titleGroup = new VBox(8);
        Label titleFieldLabel = new Label("Title:");
        titleFieldLabel.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1.1em;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1a1a1a;"
        );
        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        titleField.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1.2em;" +
            "-fx-font-weight: 500;" +
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: #2ecc71;" +
            "-fx-border-width: 2;" +
            "-fx-padding: 15;" +
            "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        );
        titleField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                titleField.setStyle(
                    "-fx-font-family: 'Poppins';" +
                    "-fx-font-size: 1.2em;" +
                    "-fx-font-weight: 500;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-color: #27ae60;" +
                    "-fx-border-width: 2;" +
                    "-fx-padding: 15;" +
                    "-fx-effect: dropshadow(gaussian, rgba(46,204,113,0.2), 4, 0, 0, 0);"
                );
            } else {
                titleField.setStyle(
                    "-fx-font-family: 'Poppins';" +
                    "-fx-font-size: 1.2em;" +
                    "-fx-font-weight: 500;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-color: #2ecc71;" +
                    "-fx-border-width: 2;" +
                    "-fx-padding: 15;" +
                    "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
                );
            }
        });
        titleGroup.getChildren().addAll(titleFieldLabel, titleField);

        VBox descGroup = new VBox(8);
        Label descFieldLabel = new Label("Describe Your Problem:");
        descFieldLabel.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1.1em;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1a1a1a;"
        );
        TextArea descField = new TextArea();
        descField.setPromptText("Description");
        descField.setPrefHeight(220);
        descField.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1em;" +
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-width: 0;" +
            "-fx-padding: 15;" +
            "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        );
        descField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                descField.setStyle(
                    "-fx-font-family: 'Poppins';" +
                    "-fx-font-size: 1em;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-width: 0;" +
                    "-fx-padding: 15;" +
                    "-fx-effect: dropshadow(gaussian, rgba(46,204,113,0.2), 4, 0, 0, 0);"
                );
            } else {
                descField.setStyle(
                    "-fx-font-family: 'Poppins';" +
                    "-fx-font-size: 1em;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-width: 0;" +
                    "-fx-padding: 15;" +
                    "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
                );
            }
        });
        descGroup.getChildren().addAll(descFieldLabel, descField);

        VBox tagGroup = new VBox(8);
        Label tagFieldLabel = new Label("Tag:");
        tagFieldLabel.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1.1em;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #1a1a1a;"
        );
        ComboBox<String> tagCombo = new ComboBox<>();
        tagCombo.getItems().addAll(tagService.getAllTags().stream().map(Tag::getName).toList());
        tagCombo.setPromptText("Tag (optional)");
        tagCombo.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1em;" +
            "-fx-background-color: white;" +
            "-fx-background-radius: 10;" +
            "-fx-border-width: 0;" +
            "-fx-padding: 15;" +
            "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
        );
        tagCombo.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                tagCombo.setStyle(
                    "-fx-font-family: 'Poppins';" +
                    "-fx-font-size: 1em;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-width: 0;" +
                    "-fx-padding: 15;" +
                    "-fx-effect: dropshadow(gaussian, rgba(46,204,113,0.2), 4, 0, 0, 0);"
                );
            } else {
                tagCombo.setStyle(
                    "-fx-font-family: 'Poppins';" +
                    "-fx-font-size: 1em;" +
                    "-fx-background-color: white;" +
                    "-fx-background-radius: 10;" +
                    "-fx-border-width: 0;" +
                    "-fx-padding: 15;" +
                    "-fx-effect: innershadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);"
                );
            }
        });
        tagGroup.getChildren().addAll(tagFieldLabel, tagCombo);

        Label messageLabel = new Label();
        messageLabel.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1em;" +
            "-fx-text-fill: #e74c3c;" +
            "-fx-font-weight: 500;"
        );

        content.getChildren().addAll(titleLabel, subtitle, titleGroup, descGroup, tagGroup, messageLabel);

        Button createButton = (Button) dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1.1em;" +
            "-fx-font-weight: bold;" +
            "-fx-background-color: #2ecc71;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 50;" +
            "-fx-padding: 15 30;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 4);"
        );
        createButton.setOnMouseEntered(e -> createButton.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1.1em;" +
            "-fx-font-weight: bold;" +
            "-fx-background-color: #27ae60;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 50;" +
            "-fx-padding: 15 30;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 20, 0, 0, 6);"
        ));
        createButton.setOnMouseExited(e -> createButton.setStyle(
            "-fx-font-family: 'Poppins';" +
            "-fx-font-size: 1.1em;" +
            "-fx-font-weight: bold;" +
            "-fx-background-color: #2ecc71;" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 50;" +
            "-fx-padding: 15 30;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 4);"
        ));

        createButton.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), createButton);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.05);
            st.setToY(1.05);
            st.setCycleCount(2);
            st.setAutoReverse(true);
            st.play();
        });

        dialog.getDialogPane().setContent(content);

        createButton.setOnAction(e -> {
            String title = titleField.getText().trim();
            String description = descField.getText().trim();
            String tagName = tagCombo.getValue();

            if (title.isEmpty() || description.isEmpty()) {
                messageLabel.setText("Please fill in all required fields (Title and Description)");
                messageLabel.setStyle("-fx-text-fill: #e74c3c;");
                return;
            }

            Tag selectedTag = tagName != null ? tagService.getTagByName(tagName) : null;
            UUID tagId = selectedTag != null ? selectedTag.getId() : null;
            UUID userId = currentUser.getId();

            boolean success = reclamationService.addReclamation(
                userId,
                tagId,
                1,
                title,
                description,
                Status.WAITING
            );

            if (success) {
                messageLabel.setText("Reclamation added successfully!");
                messageLabel.setStyle("-fx-text-fill: #2ecc71;");
                setupMainContainer();
                titleField.clear();
                descField.clear();
                tagCombo.getSelectionModel().clearSelection();

                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(Duration.seconds(1));
                delay.setOnFinished(event -> dialog.close());
                delay.play();
            } else {
                messageLabel.setText("Failed to add reclamation. Check user ID or database constraints.");
                messageLabel.setStyle("-fx-text-fill: #e74c3c;");
            }
        });

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        dialog.setOnShown(e -> fadeIn.play());

        dialog.show();
    }

    private void handleWriteReview() {
        System.out.println("Opening write review form - not implemented yet.");
    }
}