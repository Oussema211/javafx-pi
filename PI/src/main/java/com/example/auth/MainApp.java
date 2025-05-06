package com.example.auth;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.reclamation.service.MessageReclamationService;
import com.example.reclamation.service.ReclamationService;
import com.example.reclamation.service.TagService;
import com.example.reclamation.service.NotificationsService;
import com.example.auth.utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainApp extends Application {
    private SessionManager sessionManager = SessionManager.getInstance();
    private static final boolean FULL_SCREEN = false; // Set to true for always full-screen, false for windowed
    
    private final AuthService authService = new AuthService();
    private final TagService tagService = new TagService();
    private final NotificationsService notificationsService = new NotificationsService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final MessageReclamationService messageReclamationService = new MessageReclamationService();


    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("DEBUG: Starting MainApp");

        // Determine FXML file based on user session
        String fxmlFile;
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            fxmlFile = "/com/example/auth/login.fxml";
        } else {
            fxmlFile = user.hasRole("ROLE_ADMIN")
                    ? "/com/example/auth/dashboard.fxml"
                    : "/com/example/frontPages/dashboard.fxml";
        }

        System.out.println("DEBUG: Loading FXML: " + fxmlFile);
        URL fxmlUrl = getClass().getResource(fxmlFile);
        if (fxmlUrl == null) {
            System.err.println("ERROR: Resource not found for FXML: " + fxmlFile);
            throw new IOException("Cannot find FXML file: " + fxmlFile);
        }

        // Load FXML
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        if (root == null) {
            System.err.println("DEBUG: Failed to load " + fxmlFile + " - root is null");
            throw new IOException("Failed to load FXML file: " + fxmlFile);
        }

        // Create scene
        Scene scene = new Scene(root, 1400, 740);

        // Load stylesheet
        URL stylesheetUrl = getClass().getResource("/com/example/auth/styles.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            System.out.println("WARNING: Could not find styles.css in MainApp");
        }

        // Configure and show stage
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