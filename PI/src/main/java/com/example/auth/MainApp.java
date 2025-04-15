package com.example.auth;

import java.net.URL;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.auth.utils.ResetLinkServer;
import com.example.auth.utils.SessionManager;
import com.example.reclamation.service.MessageReclamationService;
import com.example.reclamation.service.ReclamationService;
import com.example.reclamation.service.TagService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {
    private final SessionManager sessionManager = SessionManager.getInstance();
    private static final boolean FULL_SCREEN = false; // Set to true for always full-screen, false for windowed
    private final AuthService authService = new AuthService();
    private final TagService tagService = new TagService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final MessageReclamationService messageReclamationService = new MessageReclamationService();

    @Override
    public void start(Stage primaryStage) throws IOException {
        System.out.println("DEBUG: Starting MainApp");
        String fxmlFile;
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            fxmlFile = "/com/example/auth/login.fxml";
        } else {
            fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/reclamation/Reclamation.fxml";
        }
        System.out.println("DEBUG: Loading FXML: " + fxmlFile);

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            System.err.println("ERROR: Failed to load " + fxmlFile + ": " + e.getMessage());
            throw e;
        }

        if (root == null) {
            System.err.println("ERROR: Root is null after loading FXML: " + fxmlFile);
            throw new IOException("Failed to load FXML: " + fxmlFile);
        }

        Scene scene = new Scene(root, 400, 500);

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

        primaryStage.setOnCloseRequest(event -> {
            ResetLinkServer.stopServer();
            System.out.println("DEBUG: Stopping MainApp");
        });
    }
    public static void main(String[] args) {
        launch(args);
    }
}