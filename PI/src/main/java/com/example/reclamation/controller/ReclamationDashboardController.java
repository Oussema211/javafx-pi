package com.example.reclamation.controller;

import com.example.reclamation.model.Reclamation;
import com.example.reclamation.model.Status;
import com.example.reclamation.service.MessageReclamationService;
import com.example.reclamation.service.ReclamationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.ChannelEventListener;
import com.pusher.client.channel.PusherEvent;

import utils.SessionManager;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
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
import javafx.scene.layout.*;
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
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReclamationDashboardController {

    @FXML private ListView<VBox> reclamationsListView;
    @FXML private TextField searchField;
    @FXML private Button clearSearchButton;
    @FXML private Button retrainButton;
    @FXML private Button prevPageButton;
    @FXML private Button nextPageButton;
    @FXML private Label pageInfoLabel;

    private Pusher pusher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final SessionManager sessionManager = SessionManager.getInstance();
    private final ReclamationService reclamationService = new ReclamationService();
    private final AuthService authService = new AuthService();
    private final MessageReclamationService messageReclamationService = new MessageReclamationService();
    private static final DropShadow CARD_SHADOW = new DropShadow(10, Color.gray(0.4, 0.5));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private ObservableList<Reclamation> reclamationsList;
    private ObservableList<Reclamation> filteredReclamations;
    private Stage primaryStage;
    private int currentPage = 1;
    private final int itemsPerPage = 4;

    @FXML
    public void initialize() {
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            System.err.println("No user logged in; should have been redirected to login");
            return;
        }

        System.out.println("Logged-in user role: " + user.hasRole("ROLE_ADMIN"));

        setupListView();
        setupSearch();
        setupRetrain();
        setupPagination();
        loadReclamations();

        // Setup Pusher for notifications only if the user is an admin
        if (user.hasRole("ROLE_ADMIN")) {
            setupPusher();
        }
    }

    private void setupPusher() {
        try {
            // Load Pusher credentials
            String appKey = "5e84f2b708f7b43445d8";
            String cluster = "eu";

            if (appKey == null || appKey.isEmpty() || cluster == null || cluster.isEmpty()) {
                throw new IllegalStateException("PUSHER_APP_KEY or PUSHER_CLUSTER not set");
            }

            System.out.println("Initializing Pusher client with appKey: " + appKey + ", cluster: " + cluster);

            // Configure Pusher options
            PusherOptions options = new PusherOptions()
                    .setCluster(cluster);

            // Initialize Pusher client
            pusher = new Pusher(appKey, options);
            System.out.println("Pusher client initialized successfully");

            // Subscribe to public admins channel
            Channel channel = pusher.subscribe("admins");
            System.out.println("Subscribed to Pusher channel: admins");

            // Bind to new-reclamation event
            channel.bind("new-reclamation", new ChannelEventListener() {
                @Override
                public void onEvent(PusherEvent event) {
                    System.out.println("Received Pusher event on channel 'admins' with data: " + event.getData());
                    try {
                        String data = event.getData();
                        Map<String, Object> payload = objectMapper.readValue(data, Map.class);
                        String id = (String) payload.get("id");
                        String userId = (String) payload.get("userId");
                        String title = (String) payload.get("title");
                        String description = (String) payload.get("description");
                        String status = (String) payload.get("status");
                        String dateStr = (String) payload.get("date");
                        Instant date = Instant.parse(dateStr);

                        Reclamation reclamation = new Reclamation(
                                UUID.fromString(id),
                                UUID.fromString(userId),
                                null,
                                java.sql.Timestamp.from(date),
                                0,
                                title,
                                description,
                                Status.fromString(status)
                        );

                        Platform.runLater(() -> {
                            System.out.println("Adding reclamation to list and showing notification for ID: " + id);
                            reclamationsList.add(reclamation);
                            filterReclamations(searchField.getText().trim());
                            showNotification(reclamation);
                            System.out.println("Notification displayed for reclamation: " + title);
                        });
                    } catch (Exception e) {
                        System.err.println("Error processing Pusher event: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                @Override
                public void onSubscriptionSucceeded(String channelName) {
                    System.out.println("Successfully subscribed to Pusher channel: " + channelName);
                }
            });

            System.out.println("Bound to 'new-reclamation' event on channel 'admins'");

            // Connect to Pusher
            pusher.connect();
            System.out.println("Pusher client connected");
        } catch (Exception e) {
            System.err.println("Failed to initialize Pusher client: " + e.getMessage());
            e.printStackTrace();
        }
    }

     private void showNotification(Reclamation reclamation) {
        System.out.println("Starting showNotification for reclamation: " + reclamation.getTitle());
        try {
            Stage notificationStage = new Stage();
            notificationStage.initStyle(StageStyle.TRANSPARENT);
            notificationStage.setAlwaysOnTop(true);

            // Create notification container with gradient background
            VBox notification = new VBox(12);
            notification.setPadding(new Insets(20));
            LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#a7f3d0")),
                new Stop(1, Color.web("#6ee7b7"))
            );
            notification.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #a7f3d0, #6ee7b7);" +
                "-fx-border-radius: 12; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 12, 0, 2, 4);"
            );
            notification.setAlignment(Pos.CENTER_LEFT);
            notification.setPrefWidth(320);
            notification.setMaxWidth(320);

            // Title with icon
            HBox titleBox = new HBox(8);
            ImageView icon = new ImageView();
            try {
                Image bellImage = new Image(getClass().getResourceAsStream("/icons/bell.png"));
                icon.setImage(bellImage);
                icon.setFitWidth(20);
                icon.setFitHeight(20);
            } catch (Exception e) {
                System.err.println("Error loading bell icon: " + e.getMessage());
            }
            Label title = new Label("New Reclamation");
            title.setFont(Font.font("System", FontWeight.BOLD, 18));
            title.setTextFill(Color.web("#064e3b"));
            titleBox.getChildren().addAll(icon, title);
            titleBox.setAlignment(Pos.CENTER_LEFT);

            // Content
            Label content = new Label("Title: " + reclamation.getTitle() + "\nDescription: " + reclamation.getDescription());
            content.setFont(Font.font("System", FontWeight.NORMAL, 13));
            content.setTextFill(Color.web("#1a2e05"));
            content.setWrapText(true);
            content.setMaxWidth(280);

            // View button
            Button viewButton = createModernButton("View Messages", "#059669");
            viewButton.setPrefWidth(120);
            viewButton.setStyle(
                "-fx-background-color: #059669; -fx-text-fill: white; -fx-font-family: 'System'; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-size: 13;"
            );
            ScaleTransition buttonHover = new ScaleTransition(Duration.millis(200), viewButton);
            buttonHover.setToX(1.05);
            buttonHover.setToY(1.05);
            viewButton.setOnMouseEntered(e -> {
                viewButton.setStyle(
                    "-fx-background-color: #047857; -fx-text-fill: white; -fx-font-family: 'System'; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-size: 13;"
                );
                buttonHover.play();
            });
            viewButton.setOnMouseExited(e -> {
                viewButton.setStyle(
                    "-fx-background-color: #059669; -fx-text-fill: white; -fx-font-family: 'System'; -fx-font-weight: bold;" +
                    "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-size: 13;"
                );
                buttonHover.setToX(1.0);
                buttonHover.setToY(1.0);
                buttonHover.play();
            });
            viewButton.setOnAction(e -> {
                System.out.println("View button clicked for reclamation: " + reclamation.getTitle());
                showMessagesWindow(reclamation);
                notificationStage.close();
            });

            notification.getChildren().addAll(titleBox, content, viewButton);

            // Animations
            FadeTransition fadeIn = new FadeTransition(Duration.millis(500), notification);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(500), notification);
            slideIn.setFromY(100);
            slideIn.setToY(0);

            ParallelTransition entrance = new ParallelTransition(fadeIn, slideIn);
            entrance.setOnFinished(e -> System.out.println("Entrance animation completed for reclamation: " + reclamation.getTitle()));

            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), notification);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            TranslateTransition slideOut = new TranslateTransition(Duration.millis(500), notification);
            slideOut.setFromY(0);
            slideOut.setToY(100);

            ParallelTransition exit = new ParallelTransition(fadeOut, slideOut);
            exit.setDelay(Duration.seconds(7));
            exit.setOnFinished(e -> {
                System.out.println("Fade out completed, closing notification stage");
                notificationStage.close();
            });

            Scene scene = new Scene(notification);
            scene.setFill(Color.TRANSPARENT);
            notificationStage.setScene(scene);

            // Position in bottom-left corner
            if (primaryStage != null) {
                System.out.println("Positioning notification at bottom-left of primaryStage at X: " + primaryStage.getX() + ", Y: " + primaryStage.getY() + ", Height: " + primaryStage.getHeight());
                notificationStage.setX(primaryStage.getX() + 20); // 20px from left edge
                notificationStage.setY(primaryStage.getY() + primaryStage.getHeight() - notification.getPrefHeight() - 20); // 20px from bottom edge
            } else {
                System.err.println("primaryStage is null, using fixed bottom-left position");
                notificationStage.setX(20); // Fallback to fixed position
                notificationStage.setY(600); // Adjust based on typical screen height
            }

            System.out.println("Showing notification stage");
            notificationStage.show();
            entrance.play();
            exit.play();
            System.out.println("Notification stage shown for reclamation: " + reclamation.getTitle());
        } catch (Exception e) {
            System.err.println("Error in showNotification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupListView() {
        reclamationsListView.setStyle("-fx-background-color: transparent;");
        reclamationsListView.setCellFactory(lv -> new ListCell<VBox>() {
            @Override
            protected void updateItem(VBox item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : item);
            }
        });
        reclamationsListView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                reclamationsListView.prefWidthProperty().bind(newScene.widthProperty().subtract(40));
                reclamationsListView.prefHeightProperty().bind(newScene.heightProperty().multiply(0.6));
            }
        });
    }

    private void setupSearch() {
        filteredReclamations = FXCollections.observableArrayList();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            filterReclamations(newValue.trim());
        });
        clearSearchButton.setOnAction(e -> {
            searchField.clear();
            currentPage = 1;
            filterReclamations("");
        });
    }

    private void setupRetrain() {
        retrainButton.setOnMouseEntered(e -> retrainButton.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12 24; -fx-font-size: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
        retrainButton.setOnMouseExited(e -> retrainButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12; -fx-padding: 12 24; -fx-font-size: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
        retrainButton.setOnAction(e -> {
            try {
                String result = messageReclamationService.retrainModel();
                showAlert("Model Retrained", result, Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                showAlert("Retrain Error", "Failed to retrain model: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void setupPagination() {
        prevPageButton.setOnAction(e -> previousPage());
        nextPageButton.setOnAction(e -> nextPage());
        prevPageButton.setOnMouseEntered(e -> prevPageButton.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-font-size: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
        prevPageButton.setOnMouseExited(e -> prevPageButton.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-font-size: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
        nextPageButton.setOnMouseEntered(e -> nextPageButton.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-font-size: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
        nextPageButton.setOnMouseExited(e -> nextPageButton.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-font-size: 14; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"));
        updatePaginationControls();
    }

    private void filterReclamations(String query) {
        if (query.isEmpty()) {
            filteredReclamations.setAll(reclamationsList);
        } else {
            String lower = query.toLowerCase();
            filteredReclamations.setAll(reclamationsList.stream()
                .filter(r -> r.getTitle().toLowerCase().contains(lower) ||
                             r.getStatut().toString().toLowerCase().contains(lower))
                .collect(Collectors.toList()));
        }
        currentPage = Math.min(currentPage, getTotalPages());
        if (currentPage < 1) currentPage = 1;
        updateListView();
        updatePaginationControls();
    }

    private VBox createReclamationCard(Reclamation reclamation) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setPrefSize(500, 200);
        card.setAlignment(Pos.TOP_LEFT);
        card.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-radius: 15; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);");

        User user = authService.getUserById(reclamation.getUserId());
        ImageView profilePicture = new ImageView();
        profilePicture.setFitWidth(50);
        profilePicture.setFitHeight(50);
        profilePicture.setClip(new Circle(25, 25, 23));

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

        ScaleTransition hover = new ScaleTransition(Duration.millis(200), profilePicture);
        hover.setToX(1.2);
        hover.setToY(1.2);
        profilePicture.setOnMouseEntered(e -> {
            profilePicture.setEffect(new DropShadow(10, Color.gray(0.4, 0.5)));
            hover.play();
        });
        profilePicture.setOnMouseExited(e -> {
            profilePicture.setEffect(null);
            hover.setToX(1.0);
            hover.setToY(1.0);
            hover.play();
        });

        HBox nameBox = createIconLabelBox("/icons/user.png", (user != null ? user.getPrenom() + " " + user.getNom() : "Unknown"), "-fx-font-size: 13; -fx-text-fill: #475569;");
        HBox titleBox = createIconLabelBox("/icons/title.png", reclamation.getTitle(), "-fx-font-size: 15; -fx-text-fill: #1e3a8a; -fx-font-weight: bold;");
        titleBox.setMaxWidth(400);
        HBox descBox = createIconLabelBox("/icons/description.png", reclamation.getDescription(), "-fx-font-size: 13; -fx-text-fill: #1f2937;");
        descBox.setMaxWidth(400);
        descBox.setMaxHeight(30);
        HBox dateBox = createIconLabelBox("/icons/calendar.png", DATE_FORMATTER.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(reclamation.getDateReclamation().getTime()), ZoneId.systemDefault())), "-fx-font-size: 13; -fx-text-fill: #64748b;");
        HBox statusBox = createIconLabelBox("/icons/status.png", reclamation.getStatut().toString(), getStatusStyle(reclamation.getStatut().toString()));

        Button editBtn = createIconButton("/icons/update.png", "#3b82f6", "Edit Reclamation");
        editBtn.setOnAction(e -> showEditReclamationForm(reclamation));

        Button delBtn = createIconButton("/icons/supp.png", "#ef4444", "Delete Reclamation");
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

        VBox leftContent = new VBox(8, nameBox, titleBox, descBox, dateBox, statusBox);
        leftContent.setMaxWidth(400);
        HBox topContent = new HBox(15, profilePicture, leftContent);
        topContent.setAlignment(Pos.CENTER_LEFT);

        HBox buttonBox = new HBox(8, autoBtn, csvBtn, editBtn, delBtn, msgBtn);
        buttonBox.setAlignment(Pos.CENTER);

        card.getChildren().addAll(topContent, buttonBox);
        applyCardAnimation(card);

        return card;
    }

    private HBox createIconLabelBox(String iconPath, String text, String style) {
        HBox box = new HBox(8);
        ImageView icon = new ImageView();
        try {
            Image image = new Image(getClass().getResourceAsStream(iconPath));
            icon.setImage(image);
            icon.setFitWidth(16);
            icon.setFitHeight(16);
        } catch (Exception e) {
            System.err.println("Error loading icon: " + iconPath);
        }

        Label label = new Label(text);
        label.setStyle(style);
        label.setWrapText(true);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);

        box.getChildren().addAll(icon, label);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void loadFallbackImage(ImageView avatar, User user) {
        String initials = (user != null && user.getPrenom() != null && user.getNom() != null)
            ? (user.getPrenom().charAt(0) + "" + user.getNom().charAt(0)).toUpperCase()
            : "UU";
        StackPane textAvatar = new StackPane();
        Circle circle = new Circle(25);
        circle.setStyle("-fx-fill: #6b7280;");
        Text text = new Text(initials);
        text.setStyle("-fx-font-size: 20; -fx-fill: white; -fx-font-weight: bold;");
        textAvatar.getChildren().addAll(circle, text);
        textAvatar.setPrefSize(50, 50);
        textAvatar.setMaxSize(50, 50);
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
            st.setToX(1.15);
            st.setToY(1.15);
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
        tooltip.setStyle("-fx-font-size: 12; -fx-background-color: rgba(30, 41, 59, 0.95); -fx-text-fill: white;");
        button.setTooltip(tooltip);

        return button;
    }

    private void applyCardAnimation(VBox card) {
        ScaleTransition hover = new ScaleTransition(Duration.millis(200), card);
        hover.setFromX(1.0);
        hover.setFromY(1.0);
        hover.setToX(1.02);
        hover.setToY(1.02);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), card);
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
            case "WAITING" -> "-fx-background-color: #fef3c7; -fx-text-fill: #b45309; -fx-font-weight: bold; -fx-font-family: 'System'; -fx-font-size: 12; -fx-background-radius: 10; -fx-padding: 6 12;";
            case "CLOSED" -> "-fx-background-color: #d1d5db; -fx-text-fill: #374151; -fx-font-weight: bold; -fx-font-family: 'System'; -fx-font-size: 12; -fx-background-radius: 10; -fx-padding: 6 12;";
            case "RESOLVED" -> "-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-font-weight: bold; -fx-font-family: 'System'; -fx-font-size: 12; -fx-background-radius: 10; -fx-padding: 6 12;";
            default -> "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-font-family: 'System'; -fx-font-size: 12; -fx-background-radius: 10; -fx-padding: 6 12;";
        };
    }

    private Button createModernButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-family: 'System'; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-font-size: 14;");
        button.setEffect(new DropShadow(5, Color.gray(0.3)));
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: " + darkenColor(color) + "; -fx-text-fill: white; -fx-font-family: 'System'; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-font-size: 14;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-family: 'System'; -fx-font-weight: bold; -fx-background-radius: 10; -fx-padding: 10 20; -fx-font-size: 14;"));
        return button;
    }

    private String darkenColor(String color) {
        return switch (color) {
            case "#059669" -> "#047857";
            default -> color;
        };
    }

    private void loadReclamations() {
        var reclamations = reclamationService.getAllReclamations();
        reclamationsList = FXCollections.observableArrayList(reclamations);
        filteredReclamations.setAll(reclamationsList);
        currentPage = 1;
        updateListView();
        updatePaginationControls();
    }

    private void updateListView() {
        ObservableList<VBox> cards = FXCollections.observableArrayList();
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredReclamations.size());

        for (int i = startIndex; i < endIndex; i++) {
            cards.add(createReclamationCard(filteredReclamations.get(i)));
        }

        reclamationsListView.setItems(cards);
    }

    private int getTotalPages() {
        return (int) Math.ceil((double) filteredReclamations.size() / itemsPerPage);
    }

    private void updatePaginationControls() {
        int totalPages = getTotalPages();
        pageInfoLabel.setText(String.format("Page %d of %d", currentPage, totalPages));
        prevPageButton.setDisable(currentPage <= 1);
        nextPageButton.setDisable(currentPage >= totalPages);
    }

    @FXML
    private void previousPage() {
        if (currentPage > 1) {
            currentPage--;
            updateListView();
            updatePaginationControls();
        }
    }

    @FXML
    private void nextPage() {
        if (currentPage < getTotalPages()) {
            currentPage++;
            updateListView();
            updatePaginationControls();
        }
    }

    @FXML
    private void clearSearch() {
        searchField.clear();
        currentPage = 1;
        filterReclamations("");
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
        form.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-radius: 15; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        applyFadeIn(form);
        return form;
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("System", FontWeight.BOLD, 14));
        label.setTextFill(Color.web("#1e3a8a"));
        return label;
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'System'; -fx-font-size: 14; -fx-padding: 8;");
        return field;
    }

    private TextArea createStyledTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefHeight(120);
        area.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'System'; -fx-font-size: 14; -fx-padding: 8;");
        return area;
    }

    private ComboBox<String> createStyledComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        combo.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #bfdbfe; -fx-border-radius: 8; -fx-background-radius: 8; -fx-font-family: 'System'; -fx-font-size: 14;");
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
                "; -fx-border-color: " + (type == Alert.AlertType.ERROR ? "#ef4444" : "#10b981") + "; -fx-border-width: 2; -fx-font-family: 'System';");
        alert.showAndWait();
    }
}