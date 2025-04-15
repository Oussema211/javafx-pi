package com.example.reclamation.controller;

import com.example.reclamation.model.MessageReclamation;
import com.example.reclamation.model.Reclamation;
import com.example.reclamation.service.MessageReclamationService;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;

public class MessagesRecController implements Initializable {
    
    // Formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    
    // UI Components
    @FXML private Label titleLabel;
    @FXML private Label dateLabel;
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
        setupUI();
        setupAnimations();
        setupButtonHover();
        setupInputAutoResize();
    }
    
    private void setupUI() {
        // Message input
        messageInput.setWrapText(true);
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        
        // Scroll pane
        scrollPane.setFitToWidth(true);
    }
    
    private void setupAnimations() {
        // Auto-scroll to bottom
        messagesContainer.heightProperty().addListener((obs, old, newVal) -> {
            scrollPane.setVvalue(1.0);
        });
        
        // Fade-in for container
        FadeTransition fade = new FadeTransition(Duration.millis(400), messagesContainer);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void setupButtonHover() {
        sendButton.setOnMouseEntered(e -> sendButton.setStyle(
            "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font: bold 16px 'Arial'; " +
            "-fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-padding: 0;"
        ));
        sendButton.setOnMouseExited(e -> sendButton.setStyle(
            "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font: bold 16px 'Arial'; " +
            "-fx-background-radius: 50; -fx-min-width: 36; -fx-min-height: 36; -fx-padding: 0;"
        ));
    }

    private void setupInputAutoResize() {
        messageInput.textProperty().addListener((obs, old, newVal) -> {
            // Reset height
            messageInput.setPrefHeight(36);
            // Calculate required rows
            String[] lines = newVal.split("\n");
            int rows = Math.min(lines.length, 3); // Cap at 3 lines
            if (rows > 1) {
                messageInput.setPrefHeight(36 + (rows - 1) * 18);
            }
        });
    }

    public void initData(Reclamation reclamation, UUID currentUserId) {
        this.reclamation = reclamation;
        this.currentUserId = currentUserId;
        
        titleLabel.setText(reclamation.getTitle());
        dateLabel.setText(formatDate(reclamation.getDateReclamation()));
        
        loadMessages();
    }

    private void loadMessages() {
        messagesContainer.getChildren().clear();
        
        List<MessageReclamation> messages = messageService.getAllMessages()
            .stream()
            .filter(m -> m.getReclamationId() != null && m.getReclamationId().equals(reclamation.getId()))
            .sorted((m1, m2) -> m1.getDateMessage().compareTo(m2.getDateMessage()))
            .toList();

        for (MessageReclamation message : messages) {
            addMessageToView(message);
        }
    }

    private void addMessageToView(MessageReclamation message) {
        boolean isCurrentUser = message.getUserId().equals(currentUserId);
        LocalDateTime messageTime = new java.sql.Timestamp(message.getDateMessage().getTime()).toLocalDateTime();
        
        // Message content
        Text messageText = new Text(message.getContenu());
        messageText.setFont(Font.font("Arial", 13));
        messageText.setStyle("-fx-fill: " + (isCurrentUser ? "white" : "#1e3a8a") + ";");
        
        // Time label
        Text timeText = new Text(messageTime.format(TIME_FORMATTER));
        timeText.setFont(Font.font("Arial", 10));
        timeText.setStyle("-fx-fill: #94a3b8;");
        
        // Message container
        TextFlow textFlow = new TextFlow(messageText);
        textFlow.setMaxWidth(300);
        textFlow.setPadding(new Insets(8, 12, 8, 12));
        String borderRadius = isCurrentUser ? 
            "-fx-background-radius: 15 15 5 15;" : 
            "-fx-background-radius: 15 15 15 5;";
        textFlow.setStyle(
            "-fx-background-color: " + (isCurrentUser ? "#3b82f6" : "#ffffff") + ";" +
            borderRadius +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);"
        );

        // Time container
        VBox timeBox = new VBox(timeText);
        timeBox.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        timeBox.setPadding(new Insets(2, 12, 0, 12));
        
        // Message wrapper
        VBox messageBox = new VBox(textFlow, timeBox);
        messageBox.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(4));
        messageBox.setUserData(message);
        
        // Animation
        animateMessageAppearance(messageBox);
        
        messagesContainer.getChildren().add(messageBox);
    }
    
    private void animateMessageAppearance(VBox messageBox) {
        messageBox.setOpacity(0);
        FadeTransition fade = new FadeTransition(Duration.millis(300), messageBox);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), messageBox);
        slide.setFromY(5);
        slide.setToY(0);
        
        fade.play();
        slide.play();
    }
    
    private String formatDate(Date date) {
        LocalDateTime localDate = new java.sql.Timestamp(date.getTime()).toLocalDateTime();
        return localDate.format(DATE_FORMATTER);
    }

    @FXML
    private void handleSendMessage() {
        String content = messageInput.getText().trim();
        if (!content.isEmpty()) {
            addTypingIndicator();
            
            boolean success = messageService.addMessage(
                currentUserId,
                reclamation.getId(),
                content
            );
            
            if (success) {
                messageInput.clear();
                loadMessages();
            } else {
                showAlert("Error", "Failed to send message", Alert.AlertType.ERROR);
            }
        }
    }
    
    private void addTypingIndicator() {
        VBox indicator = new VBox();
        indicator.setAlignment(Pos.CENTER_LEFT);
        indicator.setPadding(new Insets(4));
        
        HBox dots = new HBox(4);
        dots.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(2.5, Color.web("#94a3b8"));
            dots.getChildren().add(dot);
            FadeTransition ft = new FadeTransition(Duration.millis(500), dot);
            ft.setFromValue(0.3);
            ft.setToValue(1);
            ft.setAutoReverse(true);
            ft.setCycleCount(-1);
            ft.setDelay(Duration.millis(i * 150));
            ft.play();
        }
        
        TextFlow flow = new TextFlow(dots);
        flow.setMaxWidth(60);
        flow.setPadding(new Insets(6, 10, 6, 10));
        flow.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);"
        );
        
        indicator.getChildren().add(flow);
        messagesContainer.getChildren().add(indicator);
        
        PauseTransition delay = new PauseTransition(Duration.millis(600));
        delay.setOnFinished(event -> messagesContainer.getChildren().remove(indicator));
        delay.play();
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