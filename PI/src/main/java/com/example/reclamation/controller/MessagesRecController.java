package com.example.reclamation.controller;

import com.example.reclamation.model.MessageReclamation;
import com.example.reclamation.model.Reclamation;
import com.example.reclamation.service.MessageReclamationService;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
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
    }
    
    private void setupUI() {
        // Styling
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        
        // Message input area
        messageInput.setStyle("-fx-background-color: #ffffff; -fx-border-color: #bdc3c7; " +
                "-fx-border-radius: 15; -fx-background-radius: 15; -fx-padding: 10;");
        messageInput.setWrapText(true);
        
        // Send button
        sendButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 15;");
        sendButton.setOnMouseEntered(e -> sendButton.setStyle("-fx-background-color: #2980b9; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 15;"));
        sendButton.setOnMouseExited(e -> sendButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 15;"));
        
        // Input container
        inputContainer.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 10; -fx-background-radius: 15;");
        HBox.setHgrow(messageInput, Priority.ALWAYS);
        
        // Messages container
        messagesContainer.setStyle("-fx-background-color: #f5f6fa;");
        messagesContainer.setPadding(new Insets(10));
        messagesContainer.setSpacing(15);
        
        // Scroll pane
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: #f5f6fa; -fx-border-color: #f5f6fa;");
    }
    
    private void setupAnimations() {
        // Auto-scroll to bottom when new messages are added
        messagesContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            scrollPane.setVvalue(1.0);
        });
    }

    public void initData(Reclamation reclamation, UUID currentUserId) {
        this.reclamation = reclamation;
        this.currentUserId = currentUserId;
        
        titleLabel.setText(reclamation.getTitle());
        dateLabel.setText("Created on " + formatDate(reclamation.getDateReclamation()));
        
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
        messageText.setFont(Font.font("Segoe UI", 14));
        messageText.setStyle("-fx-fill: " + (isCurrentUser ? "white" : "#2c3e50") + ";");
        
        // Time label
        Text timeText = new Text(" " + messageTime.format(TIME_FORMATTER));
        timeText.setFont(Font.font("Segoe UI", 10));
        timeText.setStyle("-fx-fill: " + (isCurrentUser ? "#dfe6e9" : "#7f8c8d") + ";");
        
        // Message container
        TextFlow textFlow = new TextFlow(messageText, timeText);
        textFlow.setMaxWidth(300);
        textFlow.setPadding(new Insets(10));
        textFlow.setStyle(
            "-fx-background-color: " + (isCurrentUser ? "#3498db" : "#ffffff") + ";" +
            "-fx-background-radius: 15;" +
            "-fx-border-radius: 15;" +
            "-fx-border-color: " + (isCurrentUser ? "#2980b9" : "#bdc3c7") + ";" +
            "-fx-border-width: 1;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);"
        );

        // Message wrapper
        VBox messageBox = new VBox(textFlow);
        messageBox.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5));
        messageBox.setUserData(message); // Store message data for potential future use
        
        // Add animation
        animateMessageAppearance(messageBox, isCurrentUser);
        
        messagesContainer.getChildren().add(messageBox);
    }
    
    private void animateMessageAppearance(VBox messageBox, boolean isCurrentUser) {
        // Initial state
        messageBox.setOpacity(0);
        messageBox.setTranslateX(isCurrentUser ? 20 : -20);
        
        // Fade in animation
        FadeTransition fade = new FadeTransition(Duration.millis(300), messageBox);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        // Slide animation
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), messageBox);
        slide.setFromX(isCurrentUser ? 20 : -20);
        slide.setToX(0);
        
        // Combine animations
        ParallelTransition transition = new ParallelTransition(fade, slide);
        transition.play();
    }
    
    private String formatDate(Date date) {
        LocalDateTime localDate = new java.sql.Timestamp(date.getTime()).toLocalDateTime();
        return localDate.format(DATE_FORMATTER) + " at " + localDate.format(TIME_FORMATTER);
    }

    @FXML
    private void handleSendMessage() {
        String content = messageInput.getText().trim();
        if (!content.isEmpty()) {
            // Add typing indicator
            addTypingIndicator();
            
            boolean success = messageService.addMessage(
                currentUserId,
                reclamation.getId(),
                content
            );
            
            if (success) {
                messageInput.clear();
                loadMessages(); // Refresh the messages view
            } else {
                showAlert("Error", "Failed to send message", Alert.AlertType.ERROR);
            }
        }
    }
    
    private void addTypingIndicator() {
        VBox indicator = new VBox();
        indicator.setAlignment(Pos.CENTER_LEFT);
        indicator.setPadding(new Insets(5));
        
        HBox dots = new HBox(5);
        dots.setAlignment(Pos.CENTER);
        
        for (int i = 0; i < 3; i++) {
            Circle dot = new Circle(4, Color.web("#7f8c8d"));
            dots.getChildren().add(dot);
            
            // Animation for each dot
            ScaleTransition st = new ScaleTransition(Duration.millis(500), dot);
            st.setByY(0.5);
            st.setByX(0.5);
            st.setAutoReverse(true);
            st.setCycleCount(Animation.INDEFINITE);
            st.setDelay(Duration.millis(i * 150));
            st.play();
        }
        
        TextFlow flow = new TextFlow(dots);
        flow.setMaxWidth(100);
        flow.setPadding(new Insets(10));
        flow.setStyle(
            "-fx-background-color: #ffffff;" +
            "-fx-background-radius: 15;" +
            "-fx-border-radius: 15;" +
            "-fx-border-color: #bdc3c7;" +
            "-fx-border-width: 1;"
        );
        
        indicator.getChildren().add(flow);
        messagesContainer.getChildren().add(indicator);
        
        // Auto-remove after animation
        PauseTransition delay = new PauseTransition(Duration.seconds(1));
        delay.setOnFinished(event -> messagesContainer.getChildren().remove(indicator));
        delay.play();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        
        // Custom dialog pane style
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #ffffff;");
        dialogPane.setHeaderText(null);
        dialogPane.lookup(".content.label").setStyle(
            "-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-font-family: 'Segoe UI';"
        );
        
        alert.showAndWait();
    }
}