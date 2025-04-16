package com.example.auth.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.example.auth.utils.SessionManager;

import java.io.IOException;

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
    
    private void loadContent(String fxmlPath) {
        try {
            System.out.println("Loading FXML: " + getClass().getResource(fxmlPath));
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            if (loader.getLocation() == null) {
                System.err.println("FXML not found: " + fxmlPath);
                return;
            }
            Parent content = loader.load();  // Use Parent to support any node type
            borderPane.setCenter(content);
        } catch (IOException e) {
            System.err.println("Error loading " + fxmlPath + ":");
            e.printStackTrace();
        }
    }
    
    @FXML
    public void initialize() {
        System.out.println("DashboardController initialized");
        System.out.println("borderPane: " + borderPane);
        System.out.println("dashboardButton: " + dashboardButton);
    
        dashboardButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/dashboard.fxml"));
        achat.setOnAction(event -> loadContent("/com/example/frontPages/pages/achat.fxml"));
        productButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/products.fxml"));
        categoryButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/categories.fxml"));
        reclamationButton.setOnAction(event -> loadContent("/com/example/reclamation/Reclamation.fxml"));
        settings.setOnAction(event -> loadContent("/com/example/frontPages/pages/settings.fxml"));
    
        logoutButton.setOnAction(event -> {
            try {
                handleLogout();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    @FXML
    private void handleLogout() throws IOException {
        sessionManager.clearSession();
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
        stage.setScene(new Scene(root));
    }
    
}
