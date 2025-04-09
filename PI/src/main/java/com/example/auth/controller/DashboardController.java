package com.example.auth.controller;

import com.example.auth.model.User;
import utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {

    @FXML
    private BorderPane borderPane;
    @FXML
    private VBox mainContent;
    @FXML
    private Hyperlink dashboardButton;
    @FXML
    private Hyperlink achat;
    @FXML
    private Hyperlink productButton;
    @FXML
    private Hyperlink categoryButton;
    @FXML
    private Hyperlink userButton;
    @FXML
    private Hyperlink settings;
    @FXML
    private Hyperlink logoutButton;

    @FXML private Label welcomeLabel;
    @FXML private Label emailLabel;


    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            VBox content = loader.load();
            borderPane.setCenter(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SessionManager sessionManager = SessionManager.getInstance();

    @FXML
    public void initialize() {
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            System.err.println("No user logged in; should have been redirected to login");
            return;
        }

        welcomeLabel.setText("Welcome, " + user.getPrenom() + " " + user.getNom() + "!");
        emailLabel.setText("Email: " + user.getEmail());

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
                throw new RuntimeException(e);
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

    @FXML
    private void handleReclamationLink() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/reclamation/Reclamation.fxml"));
            Parent reclamationRoot = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Reclamation Discussions");
            stage.setScene(new Scene(reclamationRoot, 1400, 800));

            com.example.reclamation.controller.ReclamationController controller = loader.getController();
            controller.setPrimaryStage(stage);

            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}