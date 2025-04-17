package com.example.auth.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.example.auth.utils.SessionManager;

import java.io.IOException;

import com.example.reclamation.controller.ReclamationMessagesController;
import com.example.reclamation.model.Reclamation;

public class DashboardFrontController {
    private final SessionManager sessionManager = SessionManager.getInstance();
    
    @FXML
    private BorderPane borderPane;
    @FXML
    private VBox sidebar;
    @FXML
    private Hyperlink dashboardButton;
    @FXML
    private Hyperlink achat;
    @FXML
    private Hyperlink productButton;
    @FXML
    private Hyperlink categoryButton;
    @FXML
    private Hyperlink settings;
    @FXML
    private Hyperlink logoutButton;
    @FXML
    private Hyperlink reclamationButton;
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label emailLabel;

    private Stage primaryStage;
    @FXML private Hyperlink profileButton;


    
    
    private void loadContent(String fxmlPath) {
        try {
            System.out.println("Loading FXML: " + getClass().getResource(fxmlPath));
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                System.err.println("FXML not found: " + fxmlPath);
                return;
            }
            Parent content = loader.load();
            borderPane.setCenter(content);
        } catch (IOException e) {
            System.err.println("Error loading " + fxmlPath + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void loadReclamationMessages(Reclamation reclamation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/reclamation/ReclamationMessages.fxml"));
            Parent messagesRoot = loader.load();
            ReclamationMessagesController controller = loader.getController();
            controller.setPrimaryStage(primaryStage);
            controller.setSelectedReclamation(reclamation);
            borderPane.setCenter(messagesRoot);
        } catch (IOException e) {
            System.err.println("Error loading ReclamationMessages.fxml:");
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to load reclamation messages.");
            alert.showAndWait();
        }
    }

    @FXML
    public void initialize() {
        System.out.println("DashboardController initialized");
        System.out.println("borderPane: " + borderPane);
        System.out.println("dashboardButton: " + dashboardButton);
        System.out.println("logoutButton: " + logoutButton);

        dashboardButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/dashboard.fxml"));
        achat.setOnAction(event -> loadContent("/com/example/frontPages/pages/achat.fxml"));
        productButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/products.fxml"));
        categoryButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/categories.fxml"));
        reclamationButton.setOnAction(event -> loadContent("/com/example/reclamation/Reclamation.fxml"));
        profileButton.setOnAction(event -> loadContent("/com/example/auth/profile.fxml"));
        settings.setOnAction(event -> loadContent("/com/example/frontPages/pages/settings.fxml"));

        logoutButton.setOnAction(event -> {
            try {
                System.out.println("Logout button clicked");
                handleLogout();
            } catch (IOException e) {
                System.err.println("Logout failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleLogout() throws IOException {
        System.out.println("Initiating logout");
        sessionManager.clearSession();
        System.out.println("Session cleared");

        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        System.out.println("Current stage: " + stage);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/auth/login.fxml"));
        if (loader.getLocation() == null) {
            System.err.println("login.fxml not found at /com/example/auth/login.fxml");
            throw new IOException("login.fxml resource not found");
        }

        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getClassLoader().getResource("com/example/auth/styles.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
        System.out.println("Navigated to login page");
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }
    public BorderPane getBorderPane() {
        return borderPane;
    }
    

    @FXML
    private void handleProfileClick() {
        loadContent("/com/example/auth/profile.fxml");
    }
}
