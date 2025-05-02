package com.example.reclamation.controller;
import com.modernmt.text.profanity.ProfanityFilter;
import com.modernmt.text.profanity.dictionary.Profanity;
import com.example.reclamation.model.Reclamation;
import com.example.reclamation.model.Tag;
import com.example.reclamation.model.Status;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.reclamation.service.ReclamationService;
import com.example.reclamation.service.TagService;
import utils.SessionManager;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ReclamationController {

    private final AuthService authService = new AuthService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final TagService tagService = new TagService();
    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ProfanityFilter profanityFilter = new ProfanityFilter();

    @FXML private BorderPane root;
    @FXML private GridPane contentContainer;
    @FXML private VBox mainContainer;
    @FXML private VBox sidebar;
    @FXML private NavbarController navbarController;
    private Stage primaryStage;

    // Pagination variables
    private static final int ITEMS_PER_PAGE = 4;
    private int currentPage = 1;
    private int totalPages = 1;

    // Selected tag for filtering
    private UUID selectedTagId = null;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    @FXML
    public void initialize() {
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #f5f7fa");

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
        List<Reclamation> reclamations = selectedTagId == null 
            ? reclamationService.getAllReclamations() 
            : reclamationService.getReclamationsByTag(selectedTagId);
        totalPages = (int) Math.ceil((double) reclamations.size() / ITEMS_PER_PAGE);
        VBox reclamationList = reclamations.isEmpty() ? createEmptyState() : createPaginatedReclamationList(reclamations);
        HBox paginationControls = createPaginationControls();
        mainContainer.getChildren().addAll(header, reclamationList, paginationControls);
    }

    private HBox createHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setStyle("-fx-background-color: linear-gradient(to right, #6C983B, #7AAE49); " +
                "-fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 4);");

        Label headerText = new Label("Reclamation Discussions");
        headerText.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        List<Reclamation> reclamations = selectedTagId == null 
            ? reclamationService.getAllReclamations() 
            : reclamationService.getReclamationsByTag(selectedTagId);
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

    private VBox createPaginatedReclamationList(List<Reclamation> reclamations) {
        VBox reclamationContainer = new VBox(10);
        reclamationContainer.setStyle("-fx-animation: fadeInUp 0.5s ease forwards;");

        // Calculate start and end indices for current page
        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, reclamations.size());

        for (int i = startIndex; i < endIndex; i++) {
            Reclamation rec = reclamations.get(i);
            User user = authService.getUserById(rec.getUserId());
            Tag tag = rec.getTagId() != null ? tagService.getTagById(rec.getTagId()) : null;
            HBox card = createReclamationCard(rec, user, tag);
            reclamationContainer.getChildren().add(card);
        }
        return reclamationContainer;
    }

    private HBox createPaginationControls() {
        HBox paginationBox = new HBox(10);
        paginationBox.setAlignment(Pos.CENTER);
        paginationBox.setPadding(new Insets(10));
        paginationBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5);");

        Button prevButton = new Button("Previous");
        prevButton.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 8 15;");
        prevButton.setDisable(currentPage <= 1);
        prevButton.setOnAction(e -> {
            if (currentPage > 1) {
                currentPage--;
                setupMainContainer();
            }
        });

        Label pageLabel = new Label(String.format("Page %d of %d", currentPage, totalPages));
        pageLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");

        Button nextButton = new Button("Next");
        nextButton.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 25; -fx-padding: 8 15;");
        nextButton.setDisable(currentPage >= totalPages);
        nextButton.setOnAction(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                setupMainContainer();
            }
        });

        paginationBox.getChildren().addAll(prevButton, pageLabel, nextButton);
        return paginationBox;
    }

    private HBox createReclamationCard(Reclamation rec, User user, Tag tag) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5);");

        if (rec.getStatut() != Status.CLOSED) {
            card.setStyle(card.getStyle() + "-fx-cursor: hand;");
            card.setOnMouseClicked(e -> handleReclamationClick(rec));
            card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-translate-y: -5; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 25, 0, 0, 8);"));
            card.setOnMouseExited(e -> {
                String baseStyle = "-fx-background-color: white; -fx-background-radius: 15; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5); -fx-cursor: hand;";
                if (rec.getStatut() == Status.RESOLVED) {
                    card.setStyle(baseStyle.replace("-fx-background-color: white;", "-fx-background-color: #e6f5e6;"));
                } else {
                    card.setStyle(baseStyle);
                }
            });
        }

        StackPane profileContainer = new StackPane();
        ImageView avatar = new ImageView();
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setClip(new Circle(25, 25, 23));
        avatar.setStyle("-fx-border-color: white; -fx-border-width: 3; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        String profilePhotoPath = user != null ? user.getPhotoUrl() : null;
        if (profilePhotoPath != null && !profilePhotoPath.isEmpty()) {
            System.out.println("Attempting to load profile picture from resource: " + profilePhotoPath);
            try {
                Image image = new Image(getClass().getResourceAsStream(profilePhotoPath));
                if (!image.isError()) {
                    avatar.setImage(image);
                } else {
                    System.err.println("Error loading profile image: Image is corrupted or invalid.");
                    loadFallbackImage(avatar);
                }
            } catch (Exception e) {
                System.err.println("Error loading profile image: " + e.getMessage());
                loadFallbackImage(avatar);
            }
        } else {
            System.err.println("Profile photo path is null or empty.");
            loadFallbackImage(avatar);
        }

        Circle status = new Circle(7.5);
        String statusColor = switch (rec.getStatut()) {
            case CLOSED -> "#ff5555";
            case RESOLVED -> "#4CAF50";
            case REVIEW -> "#FF9800";
            case WAITING -> "#999";
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

        Label desc = rec.getDescription() != null ? new Label(rec.getDescription()) : new Label("No description provided.");
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
            Button editBtn = new Button();
            ImageView editIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/edit.png")));
            editIcon.setFitWidth(16);
            editIcon.setFitHeight(16);
            
            editBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 8;");
            editBtn.setOnAction(e -> handleEdit(rec.getId()));
            editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 50%; -fx-padding: 8;"));
            editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 8;"));
            Tooltip editTooltip = new Tooltip("Edit");
            editBtn.setTooltip(editTooltip);

            Button deleteBtn = new Button();
            ImageView deleteIcon = new ImageView(new Image(getClass().getResourceAsStream("/icons/delete.png")));
            deleteIcon.setFitWidth(16);
            deleteIcon.setFitHeight(16);
            deleteBtn.setGraphic(deleteIcon);
            deleteBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 8;");
            deleteBtn.setOnAction(e -> handleDelete(rec.getId()));
            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: #ff5555; -fx-background-radius: 50%; -fx-padding: 8;"));
            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 50%; -fx-padding: 8;"));
            Tooltip deleteTooltip = new Tooltip("Delete");
            deleteBtn.setTooltip(deleteTooltip);

            actionButtons.getChildren().addAll(editBtn, deleteBtn);
        }

        card.getChildren().addAll(profileContainer, contentWrapper, actionButtons);
        HBox.setHgrow(contentWrapper, Priority.ALWAYS);

        if (rec.getStatut() == Status.CLOSED) {
            card.setStyle("-fx-background-color: #ffe6e6; -fx-background-radius: 15; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5);");
        } else if (rec.getStatut() == Status.RESOLVED) {
            card.setStyle("-fx-background-color: #e6f5e6; -fx-background-radius: 15; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 15, 0, 0, 5); -fx-cursor: hand;");
        }

        return card;
    }

    private void loadFallbackImage(ImageView avatar) {
        try {
            Image fallbackImage = new Image(getClass().getResourceAsStream("/images/admin.jpg"));
            avatar.setImage(fallbackImage);
        } catch (Exception e) {
            System.err.println("Error loading fallback image: " + e.getMessage());
            avatar.setImage(new Image("file:images/admin.jpg"));
        }
    }

    private void handleReclamationClick(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/reclamation/ReclamationMessages.fxml"));
            Parent messagesRoot = loader.load();

            ReclamationMessagesController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);
            controller.setSelectedReclamation(reclamation);
            Stage stage = (Stage) root.getScene().getWindow();
            Scene messagesScene = new Scene(messagesRoot, stage.getWidth(), stage.getHeight());
            stage.setScene(messagesScene);
            stage.setTitle("Reclamation Messages - " + reclamation.getTitle());
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load messages page.");
            alert.showAndWait();
        }
    }

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

        // Add tag filter section
        VBox tagFilterSection = new VBox(10);
        Label tagFilterLabel = new Label("Filter by Tags");
        tagFilterLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        FlowPane tagContainer = new FlowPane(8, 8);
        tagContainer.setPadding(new Insets(10));
        tagContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;");

        // Add "All Tags" button to reset filter
        Button allTagsBtn = new Button("All Tags");
        allTagsBtn.setStyle("-fx-background-color: " + (selectedTagId == null ? "#6C983B" : "#999999") + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 5 10;");
        allTagsBtn.setOnAction(e -> {
            selectedTagId = null;
            currentPage = 1;
            setupMainContainer();
            // Update tag button styles
            tagContainer.getChildren().forEach(node -> {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    btn.setStyle("-fx-background-color: " + (btn == allTagsBtn ? "#6C983B" : "#999999") + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 5 10;");
                }
            });
        });
        tagContainer.getChildren().add(allTagsBtn);

        // Add tag buttons
        List<Tag> tags = tagService.getAllTags();
        for (Tag tag : tags) {
            Button tagBtn = new Button("#" + tag.getName());
            tagBtn.setStyle("-fx-background-color: " + getTagColor(tag.getId()) + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 5 10;");
            tagBtn.setOnAction(e -> {
                selectedTagId = tag.getId();
                currentPage = 1;
                setupMainContainer();
                // Update tag button styles
                tagContainer.getChildren().forEach(node -> {
                    if (node instanceof Button) {
                        Button btn = (Button) node;
                        btn.setStyle("-fx-background-color: " + (btn == tagBtn ? getTagColor(tag.getId()) : "#999999") + "; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 5 10;");
                    }
                });
                allTagsBtn.setStyle("-fx-background-color: #999999; -fx-text-fill: white; -fx-background-radius: 20; -fx-padding: 5 10;");
            });
            tagContainer.getChildren().add(tagBtn);
        }

        tagFilterSection.getChildren().addAll(tagFilterLabel, tagContainer);

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

        sidebar.getChildren().addAll(searchWrapper, sidebarActions, tagFilterSection, sidebarInfo);
    }

    private String getTagColor(UUID tagId) {
        if (tagId == null) return "#999999";
        String tagIdStr = tagId.toString();
        switch (tagIdStr) {
            case "11d2c8d3-f48e-11ef-a0dc-8c8caa96b2fa": return "#7AAE49";
            case "20743b83-f48e-11ef-a0dc-8c8caa96b2fa": return "#FF5555";
            case "294a4472-f48e-11ef-a0dc-8c8caa96b2fa": return "#FF9800";
            default: return "#999999";
        }
    }

    private void handleEdit(UUID reclamationId) {
        Dialog<Reclamation> dialog = new Dialog<>();
        dialog.setTitle("Edit Reclamation");
    
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/modern-dialog.css").toExternalForm());
    
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
    
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");
    
        // Title group
        VBox titleGroup = new VBox(8);
        Label titleLabel = new Label("Title:");
        titleLabel.getStyleClass().add("form-label");
        TextField titleField = new TextField();
        titleField.setPromptText("Enter title (min 5 letters)");
        titleField.getStyleClass().add("form-field");
        Label titleError = new Label();
        titleError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        titleGroup.getChildren().addAll(titleLabel, titleField, titleError);
    
        // Description group
        VBox descGroup = new VBox(8);
        Label descLabel = new Label("Description:");
        descLabel.getStyleClass().add("form-label");
        TextArea descField = new TextArea();
        descField.setPromptText("Enter description (min 6 words)");
        descField.setPrefRowCount(4);
        descField.getStyleClass().add("form-field");
        Label descError = new Label();
        descError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        descGroup.getChildren().addAll(descLabel, descField, descError);
    
        Button voiceBtn = new Button("🎙 Speak");
        AtomicBoolean isRecording = new AtomicBoolean(false);
        AtomicReference<AudioRecorder> recorderRef = new AtomicReference<>();
        voiceBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 5;");
        voiceBtn.setOnAction(evt -> {
            if (!isRecording.get()) {
                isRecording.set(true);
                voiceBtn.setText("🔴 Recording...");
                voiceBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
                AudioRecorder recorder = new AudioRecorder();
                recorderRef.set(recorder);
                new Thread(() -> {
                    try {
                        recorder.start();
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            voiceBtn.setText("🎙 Speak");
                            voiceBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 5;");
                            voiceBtn.setDisable(false);
                            showAlert("Recording Error", e.getMessage(), Alert.AlertType.ERROR);
                        });
                    }
                }).start();
            } else {
                isRecording.set(false);
                voiceBtn.setText("Processing...");
                voiceBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-background-radius: 5;");
                voiceBtn.setDisable(true);
                new Thread(() -> {
                    try {
                        File audioFile = recorderRef.get().stopAndSave();
                        AssemblyAIRecognizer recognizer = new AssemblyAIRecognizer();
                        String transcription = recognizer.transcribe(audioFile);
                        Platform.runLater(() -> {
                            descField.setText(transcription);
                            voiceBtn.setText("🎙 Speak");
                            voiceBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 5;");
                            voiceBtn.setDisable(false);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            voiceBtn.setText("Error");
                            voiceBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
                            voiceBtn.setDisable(false);
                        });
                    }
                }).start();
            }
        });
        descGroup.getChildren().add(voiceBtn);
    
        // Status group
        VBox statusGroup = new VBox(8);
        Label statusLabel = new Label("Status:");
        statusLabel.getStyleClass().add("form-label");
        ComboBox<Status> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(Status.values());
        statusCombo.setPromptText("Select status");
        statusCombo.getStyleClass().add("form-field");
        statusGroup.getChildren().addAll(statusLabel, statusCombo);
    
        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
    
        Reclamation rec = reclamationService.getReclamationById(reclamationId);
        if (rec != null) {
            titleField.setText(rec.getTitle());
            descField.setText(rec.getDescription());
            statusCombo.setValue(rec.getStatut());
        }
    
        content.getChildren().addAll(titleGroup, descGroup, statusGroup, messageLabel);
        dialog.getDialogPane().setContent(content);
    
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.getStyleClass().add("primary-button");
    
        saveButton.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), saveButton);
            st.setFromX(1.0); st.setFromY(1.0);
            st.setToX(1.05); st.setToY(1.05);
            st.setCycleCount(2);
            st.setAutoReverse(true);
            st.play();
        });
    
        // Validation and profanity filtering
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String title = titleField.getText().trim();
            String description = descField.getText().trim();
    
            titleError.setText("");
            descError.setText("");
            messageLabel.setText("");
    
            boolean hasErrors = false;
            String lang = "en";
    
            Profanity tProf = profanityFilter.find(lang, title);
            if (tProf != null) {
                titleError.setText("Disallowed word: " + tProf.text());
                hasErrors = true;
            }
    
            Profanity dProf = profanityFilter.find(lang, description);
            if (dProf != null) {
                descError.setText("Disallowed word: " + dProf.text());
                hasErrors = true;
            }
    
            if (title.length() < 5) {
                titleError.setText("Title must be at least 5 letters long.");
                hasErrors = true;
            }
    
            if (description.isEmpty() || description.split("\\s+").length < 6) {
                descError.setText("Description must contain at least 6 words.");
                hasErrors = true;
            }
    
            if (hasErrors) {
                event.consume();
            }
        });
    
        // Update on valid input
        saveButton.setOnAction(e -> {
            try {
                if (rec != null) {
                    rec.setTitle(titleField.getText().trim());
                    rec.setDescription(descField.getText().trim());
                    rec.setStatut(statusCombo.getValue());
    
                    boolean success = reclamationService.updateReclamation(rec);
                    if (success) {
                        titleError.setText("");
                        descError.setText("");
                        messageLabel.setText("");
                        setupMainContainer();
                        dialog.close();
                    } else {
                        messageLabel.setText("Failed to update reclamation. Check database constraints.");
                    }
                } else {
                    messageLabel.setText("Reclamation not found.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                messageLabel.setText("An unexpected error occurred: " + ex.getMessage());
            }
        });
    
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), content);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        dialog.setOnShown(e -> fadeIn.play());
    
        dialog.show();
    }
    

    private void handleDelete(UUID reclamationId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this reclamation?");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && reclamationService.deleteReclamation(reclamationId)) {
                currentPage = Math.min(currentPage, (int) Math.ceil((double) reclamationService.getAllReclamations().size() / ITEMS_PER_PAGE));
                setupMainContainer();
            }
        });
    }

   private void handleNewDiscussion() {
        User currentUser = sessionManager.getLoggedInUser();
        if (currentUser == null) {
            showAlert("Unauthorized", "You must be logged in to create a discussion.", Alert.AlertType.WARNING);
            return;
        }

        Dialog<Reclamation> dialog = new Dialog<>();
        dialog.setTitle("New Discussion");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/modern-dialog.css").toExternalForm());

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Build form UI
        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(700);

        Label titleLabel = new Label("Submit Your Problem");
        titleLabel.getStyleClass().add("header-label");

        Label subtitle = new Label("We value your feedback. Please describe your concern below.");
        subtitle.getStyleClass().add("subtitle-label");

        // Title field
        VBox titleGroup = new VBox(8);
        Label titleFieldLabel = new Label("Title:");
        titleFieldLabel.getStyleClass().add("form-label");
        TextField titleField = new TextField();
        titleField.setPromptText("Enter title (min 5 letters)");
        titleField.getStyleClass().add("form-field");
        Label titleError = new Label();
        titleError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        titleGroup.getChildren().addAll(titleFieldLabel, titleField, titleError);

        // Description field
        VBox descGroup = new VBox(8);
        Label descFieldLabel = new Label("Describe Your Problem:");
        descFieldLabel.getStyleClass().add("form-label");
        TextArea descField = new TextArea();
        descField.setPromptText("Enter description (min 6 words)");
        descField.setPrefRowCount(6);
        descField.getStyleClass().add("form-field");
        Label descError = new Label();
        descError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        descGroup.getChildren().addAll(descFieldLabel, descField, descError);

        // Voice input button (optional)
        Button voiceBtn = new Button("🎙 Speak");
        AtomicBoolean isRecording = new AtomicBoolean(false);
        AtomicReference<AudioRecorder> recorderRef = new AtomicReference<>();
        voiceBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 5;");
        voiceBtn.setOnAction(evt -> {
            if (!isRecording.get()) {
                isRecording.set(true);
                voiceBtn.setText("🔴 Recording...");
                voiceBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
                AudioRecorder recorder = new AudioRecorder();
                recorderRef.set(recorder);
                new Thread(() -> {
                    try {
                        recorder.start();
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            voiceBtn.setText("🎙 Speak");
                            voiceBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 5;");
                            voiceBtn.setDisable(false);
                            showAlert("Recording Error", e.getMessage(), Alert.AlertType.ERROR);
                        });
                    }
                }).start();
            } else {
                isRecording.set(false);
                voiceBtn.setText("Processing...");
                voiceBtn.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-background-radius: 5;");
                voiceBtn.setDisable(true);
                new Thread(() -> {
                    try {
                        File audioFile = recorderRef.get().stopAndSave();
                        AssemblyAIRecognizer recognizer = new AssemblyAIRecognizer();
                        String transcription = recognizer.transcribe(audioFile);
                        Platform.runLater(() -> {
                            descField.setText(transcription);
                            voiceBtn.setText("🎙 Speak");
                            voiceBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-background-radius: 5;");
                            voiceBtn.setDisable(false);
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            voiceBtn.setText("Error");
                            voiceBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 5;");
                            voiceBtn.setDisable(false);
                        });
                    }
                }).start();
            }
        });
        descGroup.getChildren().add(voiceBtn);

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        content.getChildren().addAll(titleLabel, subtitle, titleGroup, descGroup, messageLabel);

        dialogPane.setContent(content);

        Button createButton = (Button) dialogPane.lookupButton(createButtonType);
        createButton.getStyleClass().add("primary-button");

        // Button press animation
        createButton.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), createButton);
            st.setFromX(1.0); st.setFromY(1.0);
            st.setToX(1.05); st.setToY(1.05);
            st.setCycleCount(2);
            st.setAutoReverse(true);
            st.play();
        });

        // Fade-in dialog content
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), content);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        dialog.setOnShown(e -> fadeIn.play());

        // Validation & profanity check before submission
        createButton.addEventFilter(ActionEvent.ACTION, event -> {
            // Clear previous errors
            titleError.setText("");
            descError.setText("");
            messageLabel.setText("");
            boolean hasErrors = false;

            String title       = titleField.getText().trim();
            String description = descField.getText().trim();

            // 1) Profanity check (English)
            String lang = "en";
            Profanity tProf = profanityFilter.find(lang, title);
            if (tProf != null) {
                titleError.setText("Disallowed word: " + tProf.text());
                hasErrors = true;
            }
            Profanity dProf = profanityFilter.find(lang, description);
            if (dProf != null) {
                descError.setText("Disallowed word: " + dProf.text());
                hasErrors = true;
            }

            // 2) Title length
            if (title.length() < 5) {
                titleError.setText("Title must be at least 5 letters long.");
                hasErrors = true;
            }

            // 3) Description word count
            if (description.isEmpty() || description.split("\\s+").length < 6) {
                descError.setText("Description must contain at least 6 words.");
                hasErrors = true;
            }

            // Block if any errors
            if (hasErrors) {
                event.consume();
            }
        });

        // On successful click (after validation)
        createButton.setOnAction(e -> {
            String title       = titleField.getText().trim();
            String description = descField.getText().trim();
            try {
                boolean success = reclamationService.addReclamation(
                    currentUser.getId(),
                    null,
                    1,
                    title,
                    description,
                    Status.WAITING
                );
                if (success) {
                    currentPage = 1;
                    setupMainContainer();
                    dialog.close();
                } else {
                    messageLabel.setText("Failed to add reclamation. Check user ID or database constraints.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                messageLabel.setText("An unexpected error occurred: " + ex.getMessage());
            }
        });

        dialog.showAndWait();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void handleWriteReview() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Write Review");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/css/modern-dialog.css").toExternalForm());

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        VBox content = new VBox(15);
        content.setStyle("-fx-padding: 20;");
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(700);

        Label titleLabel = new Label("Write a Review");
        titleLabel.getStyleClass().add("header-label");

        Label subtitle = new Label("Please provide your feedback below.");
        subtitle.getStyleClass().add("subtitle-label");

        VBox reviewGroup = new VBox(8);
        Label reviewLabel = new Label("Your Review:");
        reviewLabel.getStyleClass().add("form-label");
        TextArea reviewField = new TextArea();
        reviewField.setPromptText("Enter your review (min 10 words)");
        reviewField.setPrefRowCount(6);
        reviewField.getStyleClass().add("form-field");
        Label reviewError = new Label();
        reviewError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
        reviewGroup.getChildren().addAll(reviewLabel, reviewField, reviewError);

        VBox tagGroup = new VBox(8);
        Label tagLabel = new Label("Select Tag:");
        tagLabel.getStyleClass().add("form-label");
        ComboBox<Tag> tagCombo = new ComboBox<>();
        tagCombo.getItems().addAll(tagService.getAllTags());
        tagCombo.setPromptText("Select a tag");
        tagCombo.getStyleClass().add("form-field");
        tagGroup.getChildren().addAll(tagLabel, tagCombo);

        Label messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

        content.getChildren().addAll(titleLabel, subtitle, reviewGroup, tagGroup, messageLabel);

        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.getStyleClass().add("primary-button");

        submitButton.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(200), submitButton);
            st.setFromX(1.0);
            st.setFromY(1.0);
            st.setToX(1.05);
            st.setToY(1.05);
            st.setCycleCount(2);
            st.setAutoReverse(true);
            st.play();
        });

        submitButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String review = reviewField.getText().trim();
            Tag selectedTag = tagCombo.getValue();

            reviewError.setText("");
            messageLabel.setText("");

            boolean hasErrors = false;

            String[] words = review.split("\\s+");
            if (review.isEmpty() || words.length < 10) {
                reviewError.setText("Review must contain at least 10 words.");
                hasErrors = true;
            }

            if (selectedTag == null) {
                messageLabel.setText("Please select a tag.");
                hasErrors = true;
            }

            if (hasErrors) {
                event.consume();
            }
        });

        submitButton.setOnAction(e -> {
            try {
                User currentUser = sessionManager.getLoggedInUser();
                if (currentUser == null) {
                    showAlert("Unauthorized", "You must be logged in to submit a review.", Alert.AlertType.WARNING);
                    dialog.close();
                    return;
                }

                String review = reviewField.getText().trim();
                Tag selectedTag = tagCombo.getValue();

                boolean success = reclamationService.addReclamation(
                    currentUser.getId(),
                    selectedTag.getId(),
                    1,
                    "Review: " + review.substring(0, Math.min(review.length(), 20)),
                    review,
                    Status.WAITING
                );

                if (success) {
                    reviewError.setText("");
                    messageLabel.setText("");
                    currentPage = 1;
                    setupMainContainer();
                    dialog.close();
                } else {
                    messageLabel.setText("Failed to submit review. Check database constraints.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                messageLabel.setText("An unexpected error occurred: " + ex.getMessage());
            }
        });

        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), content);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        dialog.setOnShown(e -> fadeIn.play());

        dialog.show();
    }
}