package com.example.auth.controller;

import com.example.reclamation.controller.NavbarController;
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

    @FXML private BorderPane borderPane;
    @FXML private VBox sidebar;
    @FXML private Hyperlink dashboardButton;
    @FXML private Hyperlink achat;
    @FXML private Hyperlink productButton;
    @FXML private Hyperlink categoryButton;
    @FXML private Hyperlink settings;
    @FXML private Hyperlink eventButton; // Added Hyperlink for Events
    @FXML private Hyperlink logoutButton;
    @FXML private Hyperlink reclamationButton;
    @FXML private Hyperlink topRatedButton; // Hyperlink for Top Rated Products
    @FXML private Label welcomeLabel;
    @FXML private Label emailLabel;
    @FXML private Hyperlink profileButton;

    private Stage primaryStage;
    private static NavbarController navbarController;

    public void loadContent(String fxmlPath) {
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
        try {
            FXMLLoader navbarLoader = new FXMLLoader(getClass().getResource("/com/example/frontPages/Navbar.fxml"));
            Parent navbar = navbarLoader.load();
            navbarController = navbarLoader.getController();
            navbarController.setDashboardFrontController(this);
            borderPane.setTop(navbar);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load Navbar.fxml: " + e.getMessage());
        }

        System.out.println("borderPane: " + borderPane);
        System.out.println("dashboardButton: " + dashboardButton);
        System.out.println("logoutButton: " + logoutButton);

        // Configure navigation actions
        dashboardButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/dashboard.fxml"));
        achat.setOnAction(event -> loadContent("/com/example/frontPages/pages/cart.fxml"));
        productButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/products.fxml"));
        categoryButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/favorites.fxml"));
        reclamationButton.setOnAction(event -> loadContent("/com/example/reclamation/Reclamation.fxml"));
        profileButton.setOnAction(event -> loadContent("/com/example/auth/profile.fxml"));
        eventButton.setOnAction(event -> loadContent("/com/example/Evenement/events.fxml")); // Added action for Events
        settings.setOnAction(event -> loadContent("/com/example/frontPages/pages/settings.fxml"));
        topRatedButton.setOnAction(event -> loadContent("/com/example/frontPages/pages/top_rated_products.fxml")); // Action for Top Rated Products

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

    public static NavbarController getNavbarController() {
        return navbarController;
    }

    public BorderPane getBorderPane() {
        return borderPane;
    }

    @FXML
    private void handleProfileClick() {
        loadContent("/com/example/auth/profile.fxml");
    }
}