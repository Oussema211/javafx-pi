package com.example.reclamation.controller;

import com.example.reclamation.model.MessageReclamation;
import com.example.reclamation.model.Reclamation;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.reclamation.service.MessageReclamationService;
import com.example.reclamation.service.ReclamationService;
import utils.SessionManager;

import javafx.animation.PauseTransition;
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
        root.setStyle("-fx-background-color: #D6DDD8;");
        if (selectedReclamation != null) {
            initializeUI();
        }
    }

    private void initializeUI() {
        mainContainer.getChildren().clear();
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #ddd; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2);");

        // Back Button
        Button backButton = new Button("Back to Reclamations");
        backButton.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 5; -fx-padding: 10;");
        backButton.setOnAction(e -> handleBackToReclamations());
        mainContainer.getChildren().add(backButton);

        // Header
        Label header = new Label(selectedReclamation.getTitle());
        header.setStyle("-fx-font-size: 1.5em; -fx-font-weight: bold; -fx-text-fill: #7AAE49; -fx-alignment: center;");
        mainContainer.getChildren().add(header);

        // Description
        Label description = new Label(selectedReclamation.getDescription());
        description.setStyle("-fx-font-size: 1.1em; -fx-text-fill: #555; -fx-alignment: center; -fx-padding: 0 0 15 0;");
        mainContainer.getChildren().add(description);

        // Toggle Messages Button
        Button toggleMessages = new Button("Show Messages");
        toggleMessages.setStyle("-fx-background-color: #7AAE49; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 5; -fx-padding: 12;");
        toggleMessages.setMaxWidth(Double.MAX_VALUE);
        VBox messagesContainer = new VBox(10);
        messagesContainer.setVisible(false);
        messagesContainer.setPadding(new Insets(20, 0, 0, 0));

        toggleMessages.setOnAction(e -> {
            if (messagesContainer.isVisible()) {
                messagesContainer.setVisible(false);
                toggleMessages.setText("Show Messages");
            } else {
                messagesContainer.setVisible(true);
                toggleMessages.setText("Hide Messages");
            }
        });

        // Messages Section
        List<MessageReclamation> messages = messageService.getAllMessages().stream()
                .filter(m -> m.getReclamationId().equals(selectedReclamation.getId()))
                .toList();

        if (messages.isEmpty()) {
            Label noMessages = new Label("Aucun message pour cette réclamation.");
            noMessages.setStyle("-fx-font-size: 14px; -fx-text-fill: #333; -fx-alignment: center;");
            messagesContainer.getChildren().add(noMessages);
        } else {
            for (MessageReclamation message : messages) {
                HBox messageBox = createMessageBox(message);
                messagesContainer.getChildren().add(messageBox);
            }
        }

        mainContainer.getChildren().addAll(toggleMessages, messagesContainer);

        // Reply Container
        VBox replyContainer = createReplyContainer();
        mainContainer.getChildren().add(replyContainer);
    }

    private HBox createMessageBox(MessageReclamation message) {
        HBox messageBox = new HBox(10);
        messageBox.setPadding(new Insets(10));
        messageBox.setStyle("-fx-border-color: #ddd; -fx-border-width: 0 0 1 0;");

        User user = authService.getUserById(message.getUserId());
        String photoUrl = user != null && user.getPhotoUrl() != null ? "file:" + user.getPhotoUrl() : "file:images/admin.jpg";
        ImageView avatar = new ImageView(new Image(photoUrl, true));
        avatar.setFitWidth(50);
        avatar.setFitHeight(50);
        avatar.setClip(new Circle(25, 25, 23));
        avatar.setStyle("-fx-border-color: #7AAE49; -fx-border-width: 2;");

        VBox content = new VBox(5);
        HBox header = new HBox(10);
        Label userName = new Label(user != null ? user.getNom() + " " + user.getPrenom() : "Unknown User");
        userName.setStyle("-fx-font-weight: bold; -fx-text-fill: #333;");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm");
        Label date = new Label(dateFormat.format(message.getDateMessage()));
        date.setStyle("-fx-font-size: 0.8em; -fx-text-fill: gray;");
        header.getChildren().addAll(userName, date);
        HBox.setHgrow(date, Priority.ALWAYS);
        date.setAlignment(Pos.CENTER_RIGHT);

        Label contentText = new Label(message.getContenu());
        contentText.setStyle("-fx-font-size: 14px; -fx-text-fill: #333; -fx-wrap-text: true;");
        content.getChildren().addAll(header, contentText);

        User currentUser = sessionManager.getLoggedInUser();
        if (currentUser != null && message.getUserId().equals(currentUser.getId())) {
            Button deleteBtn = new Button("🗑️");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-text-fill: #ff5555;");
            deleteBtn.setOnMouseEntered(e -> deleteBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-text-fill: #cc0000;"));
            deleteBtn.setOnMouseExited(e -> deleteBtn.setStyle("-fx-background-color: transparent; -fx-font-size: 18px; -fx-text-fill: #ff5555;"));
            deleteBtn.setOnAction(e -> handleDeleteMessage(message.getId()));
            messageBox.getChildren().add(deleteBtn);
        }

        messageBox.getChildren().addAll(avatar, content);
        HBox.setHgrow(content, Priority.ALWAYS);
        return messageBox;
    }

    private VBox createReplyContainer() {
        VBox replyContainer = new VBox(10);
        replyContainer.setPadding(new Insets(20));
        replyContainer.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dcdcdc; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 6, 0, 0, 2);");

        Label replyHeader = new Label("Reply");
        replyHeader.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #6C983B; -fx-text-transform: uppercase;");

        HBox replyBox = new HBox(10);
        replyBox.setStyle("-fx-background-color: #f8f8f8; -fx-border-color: #dcdcdc; -fx-border-radius: 6; -fx-padding: 10;");

        User currentUser = sessionManager.getLoggedInUser();
        String photoUrl = currentUser != null && currentUser.getPhotoUrl() != null ? "file:" + currentUser.getPhotoUrl() : "file:images/admin.jpg";
        ImageView avatar = new ImageView(new Image(photoUrl, true));
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setClip(new Circle(20, 20, 18));
        avatar.setStyle("-fx-border-color: #7AAE49; -fx-border-width: 2;");

        TextArea replyText = new TextArea();
        replyText.setPromptText("Type your reply...");
        replyText.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-text-fill: #333;");
        replyText.setPrefHeight(50);
        replyText.setWrapText(true);

        replyBox.getChildren().addAll(avatar, replyText);
        HBox.setHgrow(replyText, Priority.ALWAYS);

        Button submitBtn = new Button("Submit");
        submitBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 6; -fx-padding: 8 14;");
        submitBtn.setOnMouseEntered(e -> submitBtn.setStyle("-fx-background-color: #7AAE49; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 6; -fx-padding: 8 14;"));
        submitBtn.setOnMouseExited(e -> submitBtn.setStyle("-fx-background-color: #6C983B; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 6; -fx-padding: 8 14;"));
        submitBtn.setOnAction(e -> handleReplySubmit(replyText.getText(), replyText));

        replyContainer.getChildren().addAll(replyHeader, replyBox, submitBtn);
        return replyContainer;
    }

    private void handleDeleteMessage(UUID messageId) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this message?");
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
            alert.showAndWait();
            return;
        }

        if (content.trim().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Reply content cannot be empty.");
            alert.showAndWait();
            return;
        }

        // Verify if the user exists in the database
        User dbUser = authService.getUserById(currentUser.getId());
        if (dbUser == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Your user account is not found in the database. Please log in again.");
            alert.showAndWait();
            sessionManager.clearSession(); // Clear invalid session
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
            delay.setOnFinished(e -> {});
            delay.play();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to add reply. User or reclamation may not exist in the database.");
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
            alert.showAndWait();
        }
    }
}