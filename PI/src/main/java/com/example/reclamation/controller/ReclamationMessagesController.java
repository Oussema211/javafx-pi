
package com.example.reclamation.controller;

import com.example.reclamation.model.MessageReclamation;
import com.example.reclamation.model.Reclamation;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import utils.SessionManager;
import com.example.reclamation.service.MessageReclamationService;
import com.example.reclamation.service.ReclamationService;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
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
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ReclamationMessagesController {

    private final AuthService authService = new AuthService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final MessageReclamationService messageService = new MessageReclamationService();
    private final SessionManager sessionManager = SessionManager.getInstance();

    @FXML private BorderPane root;
    @FXML private VBox mainContainer;
    private Stage primaryStage;
    private Reclamation selectedReclamation;
    private ScrollPane scrollPane;

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public void setSelectedReclamation(Reclamation reclamation) {
        this.selectedReclamation = reclamation;
        initializeUI();
    }

    @FXML
    public void initialize() {
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #eceff1, #d7dee3);");
        if (selectedReclamation != null) {
            initializeUI();
        }
    }

    private void initializeUI() {
        mainContainer.getChildren().clear();
        mainContainer.setPadding(new Insets(15));
        mainContainer.setSpacing(15);
        mainContainer.setStyle("-fx-background-color: transparent;");

        // Create ScrollPane programmatically
        scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; " +
                "-fx-border-color: transparent;");
        root.setCenter(scrollPane);

        // Header Card
        VBox headerCard = new VBox(10);
        headerCard.setPadding(new Insets(20));
        headerCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 5); " +
                "-fx-border-color: linear-gradient(to right, #6C983B, #7AAE49); -fx-border-width: 2; -fx-border-radius: 20;");
        headerCard.setOnMouseEntered(e -> headerCard.setStyle(headerCard.getStyle() + "-fx-translate-y: -5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 18, 0, 0, 7);"));
        headerCard.setOnMouseExited(e -> headerCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 5); -fx-border-color: linear-gradient(to right, #6C983B, #7AAE49); -fx-border-width: 2; -fx-border-radius: 20;"));

        Label header = new Label(selectedReclamation.getTitle());
        header.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Label description = new Label(selectedReclamation.getDescription());
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #4a6078; -fx-wrap-text: true; -fx-max-height: 0; -fx-opacity: 0;");
        description.setManaged(false);

        Button toggleDesc = new Button("Show Description");
        toggleDesc.setStyle("-fx-background-color: transparent; -fx-text-fill: #6C983B; -fx-font-size: 12px;");
        toggleDesc.setOnAction(e -> {
            boolean isVisible = description.isManaged();
            description.setManaged(!isVisible);
            description.setVisible(!isVisible);
            toggleDesc.setText(isVisible ? "Show Description" : "Hide Description");
            FadeTransition fade = new FadeTransition(Duration.millis(300), description);
            fade.setToValue(isVisible ? 0 : 1);
            fade.setOnFinished(evt -> description.setMaxHeight(isVisible ? 0 : Region.USE_COMPUTED_SIZE));
            fade.play();
        });

        headerCard.getChildren().addAll(header, toggleDesc, description);

        // Messages Section
        VBox messagesContainer = new VBox(10);
        messagesContainer.setPadding(new Insets(15));
        messagesContainer.setStyle("-fx-background-color: rgba(245, 247, 250, 0.95); -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 3);");

        List<MessageReclamation> messages = messageService.getAllMessages().stream()
                .filter(m -> m.getReclamationId().equals(selectedReclamation.getId()))
                .sorted(Comparator.comparing(MessageReclamation::getDateMessage).reversed())
                .toList();

        if (messages.isEmpty()) {
            Label noMessages = new Label("No messages for this reclamation yet.");
            noMessages.setStyle("-fx-font-size: 16px; -fx-text-fill: #78909c; -fx-alignment: center; -fx-padding: 20;");
            messagesContainer.getChildren().add(noMessages);
        } else {
            for (int i = 0; i < messages.size(); i++) {
                HBox messageBox = createMessageBox(messages.get(i));
                messagesContainer.getChildren().add(messageBox);
                TranslateTransition slide = new TranslateTransition(Duration.millis(300), messageBox);
                slide.setFromX(100);
                slide.setToX(0);
                slide.setDelay(Duration.millis(i * 50));
                slide.play();
            }
        }

        // Reply Container (Sticky)
        VBox replyContainer = createReplyContainer();

        // Add components to main container
        mainContainer.getChildren().addAll(headerCard, messagesContainer, replyContainer);

        // Auto-scroll to bottom
        scrollPane.vvalueProperty().addListener((obs, old, newVal) -> {
            if (messages.size() > 0) {
                scrollPane.setVvalue(1.0);
            }
        });

        // Fade-in animation for main container
        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), mainContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private HBox createMessageBox(MessageReclamation message) {
        HBox messageBox = new HBox(10);
        messageBox.setPadding(new Insets(12));
        messageBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85); -fx-background-radius: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 3);");
        messageBox.setOnMouseEntered(e -> messageBox.setStyle(messageBox.getStyle() + "-fx-translate-y: -4; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);"));
        messageBox.setOnMouseExited(e -> messageBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85); -fx-background-radius: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 3);"));

        User user = authService.getUserById(message.getUserId());
        User currentUser = sessionManager.getLoggedInUser();
        boolean isCurrentUser = currentUser != null && message.getUserId().equals(currentUser.getId());

        // Avatar
        StackPane avatarContainer = new StackPane();
        ImageView avatar = new ImageView();
        avatar.setFitWidth(44);
        avatar.setFitHeight(44);
        // Set clip only once
        avatar.setClip(new Circle(22, 22, 20));
        avatar.setStyle("-fx-border-color: " + (isCurrentUser ? "#6C983B" : "#7AAE49") + "; -fx-border-width: 2;");

        String profilePhotoPath = user != null ? user.getPhotoUrl() : null;
        if (profilePhotoPath != null && !profilePhotoPath.isEmpty()) {
            try {
                Image image = new Image(getClass().getResourceAsStream(profilePhotoPath));
                if (!image.isError()) {
                    avatar.setImage(image);
                } else {
                    loadFallbackImage(avatar, user);
                }
            } catch (Exception e) {
                System.err.println("Error loading profile image: " + e.getMessage());
                loadFallbackImage(avatar, user);
            }
        } else {
            loadFallbackImage(avatar, user);
        }

        // Avatar hover animation
        ScaleTransition avatarHover = new ScaleTransition(Duration.millis(200), avatar);
        avatarContainer.setOnMouseEntered(e -> {
            avatarHover.setToX(1.1);
            avatarHover.setToY(1.1);
            avatarHover.play();
        });
        avatarContainer.setOnMouseExited(e -> {
            avatarHover.setToX(1.0);
            avatarHover.setToY(1.0);
            avatarHover.play();
        });

        avatarContainer.getChildren().add(avatar);

        // Tooltip for avatar
        Tooltip avatarTooltip = new Tooltip(user != null ? user.getNom() + " " + user.getPrenom() : "Unknown User");
        avatarTooltip.setStyle("-fx-font-size: 13px; -fx-background-color: rgba(44, 62, 80, 0.95); -fx-text-fill: white; -fx-background-radius: 8;");
        Tooltip.install(avatarContainer, avatarTooltip);

        // Message Content
        VBox content = new VBox(6);
        content.setMaxWidth(650);
        HBox header = new HBox(12);
        Label userName = new Label(user != null ? user.getNom() + " " + user.getPrenom() : "Unknown User");
        userName.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
        Label date = new Label(dateFormat.format(message.getDateMessage()));
        date.setStyle("-fx-font-size: 12px; -fx-text-fill: #90a4ae;");
        header.getChildren().addAll(userName, date);
        HBox.setHgrow(date, Priority.ALWAYS);
        date.setAlignment(Pos.CENTER_RIGHT);

        Label contentText = new Label(message.getContenu());
        contentText.setStyle("-fx-font-size: 14px; -fx-text-fill: #37474f; -fx-wrap-text: true; " +
                "-fx-padding: 10; -fx-background-color: " + (isCurrentUser ? "rgba(230, 245, 230, 0.9)" : "rgba(240, 244, 248, 0.9)") + "; " +
                "-fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 2);");
        content.getChildren().addAll(header, contentText);

        // Align message based on user
        if (isCurrentUser) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.getChildren().addAll(content, avatarContainer);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageBox.getChildren().addAll(avatarContainer, content);
        }

        // Delete Button for Current User's Messages
        if (isCurrentUser) {
            Button deleteBtn = new Button();
            Image deleteImage = loadDeleteIcon();
            ImageView deleteIcon = new ImageView(deleteImage);
            deleteIcon.setFitWidth(18);
            deleteIcon.setFitHeight(18);
            deleteBtn.setGraphic(deleteIcon);
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-padding: 6; -fx-opacity: 0.7;");
            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: rgba(255, 85, 85, 0.2); -fx-background-radius: 50%; -fx-padding: 6; -fx-opacity: 1.0;"));
            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: transparent; -fx-padding: 6; -fx-opacity: 0.7;"));
            deleteBtn.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this message permanently?");
                confirm.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
                confirm.setHeaderText(null);
                confirm.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK && messageService.deleteMessage(message.getId())) {
                        initializeUI();
                    }
                });
            });
            Tooltip deleteTooltip = new Tooltip("Delete Message");
            deleteTooltip.setStyle("-fx-font-size: 12px; -fx-background-color: rgba(44, 62, 80, 0.95); -fx-text-fill: white;");
            deleteBtn.setTooltip(deleteTooltip);
            content.getChildren().add(deleteBtn);
        }

        return messageBox;
    }

    private Image loadDeleteIcon() {
        try {
            Image image = new Image(getClass().getResourceAsStream("/icons/delete.png"));
            if (!image.isError()) {
                return image;
            } else {
                System.err.println("Error loading delete icon: Image is corrupted or invalid.");
            }
        } catch (Exception e) {
            System.err.println("Error loading delete icon: " + e.getMessage());
        }
        // Fallback to a blank image
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/wcAAgAB/5r7xQAAAABJRU5ErkJggg==");
    }

    private VBox createReplyContainer() {
        VBox replyContainer = new VBox(12);
        replyContainer.setPadding(new Insets(15));
        replyContainer.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 12, 0, 0, 5); " +
                "-fx-border-color: rgba(0,0,0,0.1); -fx-border-radius: 15;");
        replyContainer.setMaxWidth(900);

        HBox replyBox = new HBox(10);
        replyBox.setPadding(new Insets(12));
        replyBox.setStyle("-fx-background-color: rgba(245, 247, 250, 0.95); -fx-background-radius: 12; " +
                "-fx-border-color: rgba(0,0,0,0.05); -fx-border-radius: 12;");

        User currentUser = sessionManager.getLoggedInUser();
        StackPane avatarContainer = new StackPane();
        ImageView avatar = new ImageView();
        avatar.setFitWidth(44);
        avatar.setFitHeight(44);
        // Set clip only once
        avatar.setClip(new Circle(22, 22, 20));
        avatar.setStyle("-fx-border-color: #6C983B; -fx-border-width: 2;");

        String profilePhotoPath = currentUser != null ? currentUser.getPhotoUrl() : null;
        if (profilePhotoPath != null && !profilePhotoPath.isEmpty()) {
            try {
                Image image = new Image(getClass().getResourceAsStream(profilePhotoPath));
                if (!image.isError()) {
                    avatar.setImage(image);
                } else {
                    loadFallbackImage(avatar, currentUser);
                }
            } catch (Exception e) {
                System.err.println("Error loading profile image: " + e.getMessage());
                loadFallbackImage(avatar, currentUser);
            }
        } else {
            loadFallbackImage(avatar, currentUser);
        }

        // Avatar hover animation
        ScaleTransition avatarHover = new ScaleTransition(Duration.millis(200), avatar);
        avatarContainer.setOnMouseEntered(e -> {
            avatarHover.setToX(1.1);
            avatarHover.setToY(1.1);
            avatarHover.play();
        });
        avatarContainer.setOnMouseExited(e -> {
            avatarHover.setToX(1.0);
            avatarHover.setToY(1.0);
            avatarHover.play();
        });

        avatarContainer.getChildren().add(avatar);

        TextArea replyText = new TextArea();
        replyText.setPromptText("Type your reply...");
        replyText.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-text-fill: #37474f; " +
                "-fx-border-color: rgba(0,0,0,0.1); -fx-border-radius: 8; -fx-padding: 10;");
        replyText.setPrefHeight(70);
        replyText.setWrapText(true);

        replyBox.getChildren().addAll(avatarContainer, replyText);
        HBox.setHgrow(replyText, Priority.ALWAYS);

        Button submitBtn = new Button("Send");
        submitBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 30; -fx-padding: 12 24; -fx-font-size: 14px;");
        submitBtn.setOnMouseEntered(e -> {
            submitBtn.setStyle("-fx-background-color: #7AAE49; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 30; -fx-padding: 12 24; -fx-font-size: 14px; -fx-translate-y: -3; " +
                    "-fx-effect: dropshadow(gaussian, rgba(108,152,59,0.4), 8, 0, 0, 3);");
            ScaleTransition st = new ScaleTransition(Duration.millis(200), submitBtn);
            st.setToX(1.08);
            st.setToY(1.08);
            st.play();
        });
        submitBtn.setOnMouseExited(e -> {
            submitBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 30; -fx-padding: 12 24; -fx-font-size: 14px;");
            ScaleTransition st = new ScaleTransition(Duration.millis(200), submitBtn);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        submitBtn.setOnAction(e -> {
            handleReplySubmit(replyText.getText(), replyText);
            ScaleTransition ripple = new ScaleTransition(Duration.millis(300), submitBtn);
            ripple.setToX(1.1);
            ripple.setToY(1.1);
            ripple.setCycleCount(2);
            ripple.setAutoReverse(true);
            ripple.play();
        });

        replyContainer.getChildren().addAll(replyBox, submitBtn);
        VBox.setMargin(submitBtn, new Insets(0, 0, 0, 600));

        return replyContainer;
    }

    private void handleDeleteMessage(UUID messageId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete this message permanently?");
        confirm.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && messageService.deleteMessage(messageId)) {
                initializeUI();
            }
        });
    }

    private void handleReplySubmit(String content, TextArea replyText) {
        User currentUser = sessionManager.getLoggedInUser();
        if (currentUser == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "You must be logged in to reply.");
            alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        if (content.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Reply content cannot be empty.");
            alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }

        User dbUser = authService.getUserById(currentUser.getId());
        if (dbUser == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Your user account is not found in the database. Please log in again.");
            alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
            alert.setHeaderText(null);
            alert.showAndWait();
            sessionManager.clearSession();
            return;
        }

        System.out.println("Attempting to add message with user_id: " + currentUser.getId() +
                          ", reclamation_id: " + selectedReclamation.getId() +
                          ", content: " + content);

        boolean success = messageService.addMessage(currentUser.getId(), selectedReclamation.getId(), content);
        if (success) {
            replyText.clear();
            initializeUI();
            PauseTransition delay = new PauseTransition(Duration.seconds(1));
            delay.setOnFinished(e -> scrollPane.setVvalue(1.0));
            delay.play();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to add reply. User or reclamation may not exist in the database.");
            alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    private void handleBackToReclamations() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/reclamation/Reclamation.fxml"));
            Parent reclamationRoot = loader.load();

            ReclamationController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);

            Scene reclamationScene = new Scene(reclamationRoot, primaryStage.getWidth(), primaryStage.getHeight());
            primaryStage.setScene(reclamationScene);
            primaryStage.setTitle("Reclamation Discussions");
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load reclamations page.");
            alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    private void loadFallbackImage(ImageView avatar, User user) {
        // Try loading fallback image
        try {
            Image fallbackImage = new Image(getClass().getResourceAsStream("/images/admin.jpg"));
            if (!fallbackImage.isError()) {
                avatar.setImage(fallbackImage);
                return;
            } else {
                System.err.println("Fallback image is corrupted or invalid: /images/admin.jpg");
            }
        } catch (Exception e) {
            System.err.println("Error loading fallback image: " + e.getMessage());
        }

        // Generate text-based avatar with initials
        String initials = user != null && user.getNom() != null && user.getPrenom() != null
                ? (user.getPrenom().charAt(0) + "" + user.getNom().charAt(0)).toUpperCase()
                : "UU";
        StackPane textAvatar = new StackPane();
        Circle circle = new Circle(22);
        circle.setStyle("-fx-fill: #78909c;");
        Text text = new Text(initials);
        text.setStyle("-fx-font-size: 18px; -fx-fill: white; -fx-font-weight: bold;");
        textAvatar.getChildren().addAll(circle, text);
        textAvatar.setPrefSize(44, 44);
        textAvatar.setMaxSize(44, 44);

        // Clear image and set graphic
        avatar.setImage(null);
        avatar.setStyle("-fx-border-color: " + (user != null && sessionManager.getLoggedInUser() != null && user.getId().equals(sessionManager.getLoggedInUser().getId()) ? "#6C983B" : "#7AAE49") + "; -fx-border-width: 2;");
    }
}
