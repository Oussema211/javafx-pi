package com.example.auth.controller;

import com.example.auth.model.User;
import com.example.auth.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.RotateTransition;
import javafx.util.Duration;

import java.io.IOException;

public class DashboardController {
    @FXML private BorderPane borderPane;
    @FXML private VBox mainContent;
    @FXML private Hyperlink dashboardButton;
    @FXML private Hyperlink achat;
    @FXML private Hyperlink productButton;
    @FXML private Hyperlink categoryButton;
    @FXML private Hyperlink userButton;
    @FXML private Hyperlink settings;
    @FXML private Hyperlink logoutButton;
    @FXML private Hyperlink eventManagementButton;
    @FXML private VBox eventSubMenu;
    @FXML private Label welcomeLabel;
    @FXML private Label emailLabel;
    @FXML private ImageView dropdownArrow;

    private SessionManager sessionManager = SessionManager.getInstance();
    private boolean isSubMenuVisible = false;

    @FXML
    public void initialize() {
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            System.err.println("No user logged in; should have been redirected to login");
            return;
        }

        welcomeLabel.setText("Welcome, " + user.getPrenom() + " " + user.getNom() + "!");
        emailLabel.setText("Email: " + user.getEmail());

        // Initialiser le sous-menu comme caché
        eventSubMenu.setVisible(false);
        eventSubMenu.setManaged(false);

        // Configurer les gestionnaires d'événements
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Gestion du menu événement
        eventManagementButton.setOnAction(event -> toggleEventSubMenu());

        // Navigation principale
        dashboardButton.setOnAction(event -> loadContent("/com/example/pages/dashboard.fxml"));
        achat.setOnAction(event -> loadContent("/com/example/pages/purchasing.fxml"));
        productButton.setOnAction(event -> loadContent("/com/example/pages/products.fxml"));
        categoryButton.setOnAction(event -> loadContent("/com/example/pages/categories.fxml"));
        userButton.setOnAction(event -> loadContent("/com/example/pages/users.fxml"));
        settings.setOnAction(event -> loadContent("/com/example/pages/settings.fxml"));
        logoutButton.setOnAction(event -> {
            try {
                handleLogout();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void toggleEventSubMenu() {
        isSubMenuVisible = !isSubMenuVisible;
        eventSubMenu.setVisible(isSubMenuVisible);
        eventSubMenu.setManaged(isSubMenuVisible);
        
        // Animer la flèche déroulante
        animateDropdownArrow(isSubMenuVisible);
    }

    private void animateDropdownArrow(boolean rotate) {
        RotateTransition rotateTransition = new RotateTransition(Duration.millis(300), dropdownArrow);
        rotateTransition.setFromAngle(rotate ? 0 : 180);
        rotateTransition.setToAngle(rotate ? 180 : 0);
        rotateTransition.play();
    }

    @FXML
    private void handleAddRegion() {
        loadContent("/com/example/Evenement/AjouterRegion.fxml");
        hideSubMenu();
    }

    @FXML
    private void handleListRegions() {
        loadContent("/com/example/Evenement/RegionList.fxml");
        hideSubMenu();
    }

    @FXML
    private void handleAddEvent() {
        loadContent("/com/example/Evenement/EvenementForm.fxml");
        hideSubMenu();
    }

    @FXML
    private void handleListEvents() {
        loadContent("/com/example/Evenement/EvenementList.fxml");
        hideSubMenu();
    }

    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            borderPane.setCenter(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void hideSubMenu() {
        eventSubMenu.setVisible(false);
        eventSubMenu.setManaged(false);
        isSubMenuVisible = false;
        animateDropdownArrow(false);
    }

    @FXML
    private void handleLogout() throws IOException {
        sessionManager.clearSession();
        Stage stage = (Stage) welcomeLabel.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
        stage.setScene(new Scene(root));
    }
}