package com.example.auth;

import java.net.URL;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.reclamation.service.MessageReclamationService;
import com.example.reclamation.service.ReclamationService;
import com.example.reclamation.service.TagService;
import utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private SessionManager sessionManager = SessionManager.getInstance();
    private static final boolean FULL_SCREEN = false; // Set to true for always full-screen, false for windowed
    private final AuthService authService = new AuthService();
    private final TagService tagService = new TagService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final MessageReclamationService messageReclamationService = new MessageReclamationService();

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("DEBUG: Starting MainApp");
        String fxmlFile;
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            fxmlFile = "/com/example/auth/login.fxml";
        } else {
            fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/frontPages/pages/dashboard.fxml";
        }
        System.out.println("DEBUG: Loading FXML: " + fxmlFile);
        
        // Debug the resource path
        URL resourceUrl = getClass().getResource(fxmlFile);
        if (resourceUrl == null) {
            System.out.println("ERROR: Resource not found for FXML: " + fxmlFile);
            throw new IllegalStateException("Cannot find FXML file: " + fxmlFile);
        }
        
        Parent root = FXMLLoader.load(resourceUrl); // Use URL directly
        if (root == null) {
            System.out.println("DEBUG: Failed to load " + fxmlFile + " - root is null");
            return;
        }
        Scene scene = new Scene(root, 800, 600);
    
        // Load stylesheet
        java.net.URL stylesheetUrl = getClass().getClassLoader().getResource("com/example/auth/styles.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            System.out.println("DEBUG: Could not find styles.css in MainApp");
        }
    
        primaryStage.setTitle("Authentication System");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setFullScreen(FULL_SCREEN);
        primaryStage.show();
        System.out.println("DEBUG: MainApp started successfully");
    }
    public static void main(String[] args) {
        launch(args);
    }
}