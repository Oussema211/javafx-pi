package com.example.auth;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.reclamation.controller.ReclamationController; // Import the controller
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
            fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/reclamation/Reclamation.fxml";
        }
        System.out.println("DEBUG: Loading FXML: " + fxmlFile);

        // Use FXMLLoader to load the FXML and get the controller if needed
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
        Parent root = loader.load();
        if (root == null) {
            System.out.println("DEBUG: Failed to load " + fxmlFile + " - root is null");
            return;
        }

        // Check if the loaded FXML is for Reclamation.fxml and set the primaryStage
        if (fxmlFile.equals("/com/example/reclamation/Reclamation.fxml")) {
            ReclamationController controller = loader.getController();
            if (controller != null) {
                controller.setPrimaryStage(primaryStage);
                System.out.println("DEBUG: Set primaryStage in ReclamationController");
            } else {
                System.out.println("DEBUG: ReclamationController is null");
            }
        }

        // Set up the scene with a default size
        Scene scene = new Scene(root, 800, 600); // Adjusted default size for better usability

        // Load stylesheet
        java.net.URL stylesheetUrl = getClass().getClassLoader().getResource("com/example/auth/styles.css");
        if (stylesheetUrl != null) {
            scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        } else {
            System.out.println("DEBUG: Could not find styles.css in MainApp");
        }

        primaryStage.setTitle("Authentication System");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true); // Allow resizing and enable title bar controls
        primaryStage.setFullScreen(FULL_SCREEN); // Set initial full-screen state
        primaryStage.show();
        System.out.println("DEBUG: MainApp started successfully");
    }

    public static void main(String[] args) {
        launch(args);
    }
}