package com.example.frontoffice;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class DashboardController {

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
            VBox content = loader.load();
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

        dashboardButton.setOnAction(event -> loadContent("/org/example/demo/pages/dashboard.fxml"));
        achat.setOnAction(event -> loadContent("/org/example/demo/pages/achat.fxml"));
        productButton.setOnAction(event -> loadContent("/org/example/demo/pages/products.fxml"));
        categoryButton.setOnAction(event -> loadContent("/org/example/demo/pages/categories.fxml"));
        settings.setOnAction(event -> loadContent("/org/example/demo/pages/settings.fxml"));
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
        System.out.println("Logout clicked");
    }
}