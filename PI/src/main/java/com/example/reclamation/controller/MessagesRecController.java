package com.example.reclamation.controller;

import com.example.reclamation.model.MessageReclamation;
import com.example.reclamation.model.Reclamation;
import com.example.reclamation.service.MessageReclamationService;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controller for displaying and sending messages related to a Reclamation.
 * Enhanced for a modern, user-friendly interface with animations and styling.
 */
public class MessagesRecController implements Initializable {
    // Formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // UI Components
    @FXML private Label titleLabel;
    @FXML private Label dateLabel;
    @FXML private Label descriptionLabel;
    @FXML private TextArea messageInput;
    @FXML private VBox messagesContainer;
    @FXML private ScrollPane scrollPane;
    @FXML private Button sendButton;
    @FXML private HBox inputContainer;

    // Data
    private Reclamation reclamation;
    private UUID currentUserId;
    private final MessageReclamationService messageService = new MessageReclamationService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        applyStyles();
        setupUIBehavior();
    }

    /** Apply initial inline styles for modern look. */
    private void applyStyles() {
        // Container background
        messagesContainer.setStyle("-fx-background-color: #F9FAFB; -fx-padding: 10;");
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // Title and description
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1E3A8A;");
        descriptionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #4B5563;");
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");

        // Input container styling
        inputContainer.setStyle("-fx-background-color: #FFFFFF; -fx-padding: 8; -fx-spacing: 8;"
            + "-fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-background-radius: 8; -fx-border-radius: 8;");

        // Message input
        messageInput.setPromptText("Type your message...");
        messageInput.setStyle("-fx-font-size: 13px; -fx-background-color: #F3F4F6; -fx-background-radius: 6;");

        // Send button default style
        sendButton.setStyle("-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 16px;"
            + "-fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40;");
        sendButton.setText("➤"); // Unicode arrow
    }

    /** Set up behaviors: animations, events, resizing. */
    private void setupUIBehavior() {
        // Auto-scroll on new message
        messagesContainer.heightProperty().addListener((obs, oldH, newH) -> scrollPane.setVvalue(1.0));

        // Fade-in container
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), messagesContainer);
        fadeIn.setFromValue(0); fadeIn.setToValue(1); fadeIn.play();

        // Button hover effect
        sendButton.setOnMouseEntered(e -> sendButton.setStyle(
            "-fx-background-color: #2563EB; -fx-text-fill: white; -fx-font-size: 16px;"
            + "-fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40;"
        ));
        sendButton.setOnMouseExited(e -> sendButton.setStyle(
            "-fx-background-color: #3B82F6; -fx-text-fill: white; -fx-font-size: 16px;"
            + "-fx-background-radius: 50; -fx-min-width: 40; -fx-min-height: 40;"
        ));

        // Auto-resize input (1-5 lines)
        messageInput.textProperty().addListener((obs, old, text) -> {
            messageInput.setPrefHeight(30);
            int lines = Math.min(text.split("\r?\n").length, 5);
            if (lines > 1) messageInput.setPrefHeight(30 + (lines - 1) * 18);
        });
    }

    /** Initialize controller with data. */
    public void initData(Reclamation reclamation, UUID currentUserId) {
        this.reclamation = reclamation;
        this.currentUserId = currentUserId;

        titleLabel.setText(reclamation.getTitle());
        descriptionLabel.setText(reclamation.getDescription());
        dateLabel.setText(formatDate(reclamation.getDateReclamation()));

        loadMessages();
    }

    /** Loads existing messages for this reclamation. */
    private void loadMessages() {
        messagesContainer.getChildren().clear();
        List<MessageReclamation> messages = messageService.getAllMessages().stream()
            .filter(m -> Objects.equals(m.getReclamationId(), reclamation.getId()))
            .sorted(Comparator.comparing(MessageReclamation::getDateMessage))
            .toList();
        messages.forEach(this::addMessageToView);
    }

    /** Adds a single message bubble to the view with styling and animation. */
    private void addMessageToView(MessageReclamation message) {
        boolean mine = message.getUserId().equals(currentUserId);
        LocalDateTime time = new java.sql.Timestamp(message.getDateMessage().getTime()).toLocalDateTime();

        Text msgText = new Text(message.getContenu());
        msgText.setFont(Font.font("Arial", 13));
        msgText.setFill(mine ? Color.WHITE : Color.web("#1E3A8A"));

        Text timestamp = new Text(time.format(TIME_FORMATTER));
        timestamp.setFont(Font.font(10));
        timestamp.setFill(Color.web("#94A3B8"));

        TextFlow bubble = new TextFlow(msgText);
        bubble.setMaxWidth(320);
        bubble.setPadding(new Insets(8,12,8,12));
        bubble.setStyle(
            (mine ? "-fx-background-color: #3B82F6; -fx-text-fill: white;" : "-fx-background-color: #FFFFFF;") +
            (mine ? "-fx-background-radius: 15 15 5 15;" : "-fx-background-radius: 15 15 15 5;") +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4,0,0,1);"
        );

        VBox wrapper = new VBox(bubble, timestamp);
        wrapper.setAlignment(mine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(4));

        // Slide & fade
        wrapper.setOpacity(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), wrapper);
        slide.setFromY(5); slide.setToY(0);
        FadeTransition fade = new FadeTransition(Duration.millis(300), wrapper);
        fade.setFromValue(0); fade.setToValue(1);
        new ParallelTransition(slide, fade).play();

        messagesContainer.getChildren().add(wrapper);
    }

    /** Formats a Date object to a readable string. */
    private String formatDate(Date date) {
        return new java.sql.Timestamp(date.getTime()).toLocalDateTime().format(DATE_FORMATTER);
    }

    /** Handles sending of a new message. */
    @FXML private void handleSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty()) return;

        messageInput.clear();
        if (messageService.addMessage(currentUserId, reclamation.getId(), content)) {
            loadMessages();
        } else {
            showAlert("Error", "Unable to send message.", Alert.AlertType.ERROR);
        }
    }

    /** Displays an alert dialog. */
    private void showAlert(String title, String text, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.getDialogPane().setStyle(
            "-fx-background-color: " + (type==Alert.AlertType.ERROR ? "#fee2e2" : "#d1fae5") + ";"
            + "-fx-border-color: " + (type==Alert.AlertType.ERROR ? "#ef4444" : "#10b981") + ";"
            + "-fx-border-width: 2; -fx-font-family: 'Arial';"
        );
        alert.showAndWait();
    }
}
