package com.example.reclamation.controller;

import com.example.reclamation.model.MessageReclamation;
import com.example.reclamation.model.Reclamation;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.utils.SessionManager;
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
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
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
        mainContainer.setMaxWidth(900);

        scrollPane = new ScrollPane(mainContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; " +
                "-fx-border-color: transparent;");
        root.setCenter(scrollPane);

        VBox headerCard = new VBox(10);
        headerCard.setPadding(new Insets(20));
        headerCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 5); " +
                "-fx-border-color: linear-gradient(to right, #6C983B, #7AAE49); -fx-border-width: 2; -fx-border-radius: 20;");
        headerCard.setOnMouseEntered(e -> headerCard.setStyle(headerCard.getStyle() + "-fx-translate-y: -5; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 18, 0, 0, 7);"));
        headerCard.setOnMouseExited(e -> headerCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.9); -fx-background-radius: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 12, 0, 0, 5); -fx-border-color: linear-gradient(to right, #6C983B, #7AAE49); -fx-border-width: 2; -fx-border-radius: 20;"));

        Button backButton = new Button();
        ImageView backIcon = new ImageView(loadBackIcon());
        backIcon.setFitWidth(24);
        backIcon.setFitHeight(24);
        backButton.setGraphic(backIcon);
        backButton.setStyle("-fx-background-color: transparent; -fx-padding: 8; -fx-cursor: hand;");
        backButton.setOnMouseEntered(e -> {
            backButton.setStyle("-fx-background-color: rgba(108, 152, 59, 0.2); -fx-background-radius: 50%; -fx-padding: 8; -fx-cursor: hand;");
            ScaleTransition st = new ScaleTransition(Duration.millis(200), backButton);
            st.setToX(1.1);
            st.setToY(1.1);
            st.play();
        });
        backButton.setOnMouseExited(e -> {
            backButton.setStyle("-fx-background-color: transparent; -fx-padding: 8; -fx-cursor: hand;");
            ScaleTransition st = new ScaleTransition(Duration.millis(200), backButton);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
        backButton.setOnAction(e -> handleBackToReclamations());
        Tooltip backTooltip = new Tooltip("Back to Discussions");
        backTooltip.setStyle("-fx-font-size: 12px; -fx-background-color: rgba(44, 62, 80, 0.95); -fx-text-fill: white;");
        backButton.setTooltip(backTooltip);

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

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getChildren().addAll(backButton, header);
        headerCard.getChildren().addAll(headerBox, toggleDesc, description);

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

        VBox replyContainer = createReplyContainer();
        mainContainer.getChildren().addAll(headerCard, messagesContainer, replyContainer);

        scrollPane.vvalueProperty().addListener((obs, old, newVal) -> {
            if (messages.size() > 0) {
                scrollPane.setVvalue(1.0);
            }
        });

        FadeTransition fadeIn = new FadeTransition(Duration.millis(600), mainContainer);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    private Image loadBackIcon() {
        try {
            Image image = new Image(getClass().getResourceAsStream("/icons/back.png"));
            if (!image.isError()) {
                return image;
            } else {
                System.err.println("Error loading back icon: Image is corrupted or invalid.");
            }
        } catch (Exception e) {
            System.err.println("Error loading back icon: " + e.getMessage());
        }
        // Fallback to a blank image
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/wcAAgAB/5r7xQAAAABJRU5ErkJggg==");
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
    
        StackPane avatarContainer = new StackPane();
        ImageView avatar = new ImageView();
        avatar.setFitWidth(44);
        avatar.setFitHeight(44);
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
    
        Tooltip avatarTooltip = new Tooltip(user != null ? user.getNom() + " " + user.getPrenom() : "Unknown User");
        avatarTooltip.setStyle("-fx-font-size: 13px; -fx-background-color: rgba(44, 62, 80, 0.95); -fx-text-fill: white; -fx-background-radius: 8;");
        Tooltip.install(avatarContainer, avatarTooltip);
    
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
    
        if (isCurrentUser) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.getChildren().addAll(content, avatarContainer);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageBox.getChildren().addAll(avatarContainer, content);
        }
    
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
    
            Button editBtn = new Button();
            Image editImage = loadEditIcon();
            ImageView editIcon = new ImageView(editImage);
            editIcon.setFitWidth(18);
            editIcon.setFitHeight(18);
            editBtn.setGraphic(editIcon);
            editBtn.setStyle("-fx-background-color: transparent; -fx-padding: 6; -fx-opacity: 0.7;");
            editBtn.setOnMouseEntered(e -> editBtn.setStyle("-fx-background-color: rgba(52, 152, 219, 0.2); -fx-background-radius: 50%; -fx-padding: 6; -fx-opacity: 1.0;"));
            editBtn.setOnMouseExited(e -> editBtn.setStyle("-fx-background-color: transparent; -fx-padding: 6; -fx-opacity: 0.7;"));
            editBtn.setOnAction(e -> handleEdit(message));
            Tooltip editTooltip = new Tooltip("Edit Message");
            editTooltip.setStyle("-fx-font-size: 12px; -fx-background-color: rgba(44, 62, 80, 0.95); -fx-text-fill: white;");
            editBtn.setTooltip(editTooltip);
    
            // Use HBox to align delete and edit buttons horizontally
            HBox actionButtons = new HBox(10);
            actionButtons.setAlignment(Pos.CENTER_RIGHT);
            actionButtons.getChildren().addAll(deleteBtn, editBtn);
            content.getChildren().add(actionButtons);
        }
    
        return messageBox;
    }
    private void handleEdit(MessageReclamation message) {
    Dialog<MessageReclamation> dialog = new Dialog<>();
    dialog.setTitle("Edit Message");

    DialogPane dialogPane = dialog.getDialogPane();
    dialogPane.getStylesheets().add(getClass().getResource("/css/modern-dialog.css").toExternalForm());

    ButtonType saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

    VBox content = new VBox(15);
    content.setStyle("-fx-padding: 20;");

    // Content field with error label
    VBox contentGroup = new VBox(8);
    Label contentLabel = new Label("Message Content:");
    contentLabel.getStyleClass().add("form-label");
    TextArea contentField = new TextArea();
    contentField.setText(message.getContenu());
    contentField.setPromptText("Enter new message content (min 5 characters)");
    contentField.getStyleClass().add("form-field");
    Label contentError = new Label();
    contentError.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");
    contentGroup.getChildren().addAll(contentLabel, contentField, contentError);

    // Message label for database errors
    Label messageLabel = new Label();
    messageLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 12px;");

    content.getChildren().addAll(contentGroup, messageLabel);
    dialog.getDialogPane().setContent(content);

    Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
    saveButton.getStyleClass().add("primary-button");

    // Button press animation
    saveButton.setOnMousePressed(e -> {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), saveButton);
        st.setFromX(1.0);
        st.setFromY(1.0);
        st.setToX(1.05);
        st.setToY(1.05);
        st.setCycleCount(2);
        st.setAutoReverse(true);
        st.play();
    });

    // Add an event filter to validate before allowing the dialog to proceed
    saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
        String newContent = contentField.getText().trim();

        // Reset error messages
        contentError.setText("");
        messageLabel.setText("");

        // Validate content length (at least 5 characters)
        if (newContent.length() < 5) {
            contentError.setText("Message content must be at least 5 characters long.");
            event.consume();
            return;
        }

        // Update the message
        MessageReclamation updatedMessage = new MessageReclamation(
            message.getId(),
            message.getUserId(),
            message.getReclamationId(),
            newContent,
            new java.util.Date()
        );
        if (!messageService.updateMessage(updatedMessage)) {
            messageLabel.setText("Failed to update message. Please try again.");
            event.consume();
        }
    });

    // Set result converter to handle the save action
    dialog.setResultConverter(buttonType -> {
        if (buttonType == saveButtonType) {
            String newContent = contentField.getText().trim();
            if (newContent.length() >= 5) {
                MessageReclamation updatedMessage = new MessageReclamation(
                    message.getId(),
                    message.getUserId(),
                    message.getReclamationId(),
                    newContent,
                    new java.util.Date()
                );
                if (messageService.updateMessage(updatedMessage)) {
                    return updatedMessage;
                }
            }
        }
        return null;
    });

    // Show dialog and refresh UI on successful edit
    dialog.showAndWait().ifPresent(updatedMessage -> {
        if (updatedMessage != null) {
            initializeUI(); // Refresh the UI with the updated message
        }
    });
}
    private Image loadEditIcon() {
        try {
            Image image = new Image(getClass().getResourceAsStream("/icons/edit.png"));
            if (!image.isError()) {
                return image;
            } else {
                System.err.println("Error loading edit icon: Image is corrupted or invalid.");
            }
        } catch (Exception e) {
            System.err.println("Error loading edit icon: " + e.getMessage());
        }
        // Fallback to a blank image
        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/wcAAgAB/5r7xQAAAABJRU5ErkJggg==");
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

    private void handleReplySubmit(String content, TextArea replyText) {
        User currentUser = sessionManager.getLoggedInUser();
        if (currentUser == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "You must be logged in to reply.");
            alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }
    
        String trimmedContent = content.trim();
        // Check if the content is blank
        if (trimmedContent.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Reply content cannot be empty.");
            alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
            alert.setHeaderText(null);
            alert.showAndWait();
            return;
        }
    
        // Check if the content has at least two words
        String[] words = trimmedContent.split("\\s+");
        if (words.length < 2) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Reply must contain at least two words.");
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
            // Get the current stage from the root node
            Stage currentStage = (Stage) root.getScene().getWindow();
            if (currentStage == null) {
                System.err.println("Error: Current stage is null, cannot navigate back.");
                Alert alert = new Alert(Alert.AlertType.ERROR, "Unable to navigate back due to an internal error.");
                alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
                alert.setHeaderText(null);
                alert.showAndWait();
                return;
            }
    
            URL dashboardUrl = getClass().getResource("/com/example/frontPages/dashboard.fxml");
            if (dashboardUrl == null) {
                System.err.println("Error: /com/example/auth/DashboardFront.fxml not found in resources");
                Alert alert = new Alert(Alert.AlertType.ERROR, "Dashboard FXML file not found. Please check resources.");
                alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
                alert.setHeaderText(null);
                alert.showAndWait();
                return;
            }
            System.out.println("Loading dashboard from: " + dashboardUrl);
    
            FXMLLoader dashboardLoader = new FXMLLoader(dashboardUrl);
            Parent dashboardRoot = dashboardLoader.load();
            com.example.auth.controller.DashboardFrontController dashboardController = dashboardLoader.getController();
            dashboardController.setPrimaryStage(currentStage);
    
            URL reclamationUrl = getClass().getResource("/com/example/reclamation/Reclamation.fxml");
            if (reclamationUrl == null) {
                System.err.println("Error: /com/example/reclamation/Reclamation.fxml not found in resources");
                Alert alert = new Alert(Alert.AlertType.ERROR, "Reclamation FXML file not found. Please check resources.");
                alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
                alert.setHeaderText(null);
                alert.showAndWait();
                return;
            }
            System.out.println("Loading reclamation from: " + reclamationUrl);
    
            FXMLLoader reclamationLoader = new FXMLLoader(reclamationUrl);
            Parent reclamationRoot = reclamationLoader.load();
            ReclamationController reclamationController = reclamationLoader.getController();
            reclamationController.setPrimaryStage(currentStage);
    
            dashboardController.getBorderPane().setCenter(reclamationRoot);
    
            // Create the new dashboard scene with current stage dimensions
            Scene dashboardScene = new Scene(dashboardRoot, currentStage.getWidth(), currentStage.getHeight());
            currentStage.setScene(dashboardScene);
            currentStage.setTitle("Dashboard - Reclamation Discussions");
            currentStage.show(); // Ensure the stage is visible (though it should already be)
    
            // Optionally add a fade transition to smooth the switch
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), root);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                // No need to close the stage, just replace the scene
            });
            fadeOut.play();
    
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load reclamation discussions: " + e.getMessage());
            alert.getDialogPane().setStyle("-fx-background-color: #ffffff; -fx-border-color: #6C983B; -fx-border-radius: 10;");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }
        private void loadFallbackImage(ImageView avatar, User user) {
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

        avatar.setImage(null);
        avatar.setStyle("-fx-border-color: " + (user != null && sessionManager.getLoggedInUser() != null && user.getId().equals(sessionManager.getLoggedInUser().getId()) ? "#6C983B" : "#7AAE49") + "; -fx-border-width: 2;");
    }

    public BorderPane getBorderPane() {
        return root;
    }
}