package com.example.auth;

import com.example.Evenement.Dao.EvenementDAO;
import com.example.Evenement.Dao.RegionDAO;
import com.example.auth.model.User;
import com.example.auth.utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    private SessionManager sessionManager = SessionManager.getInstance();
    private static final boolean FULL_SCREEN = false; // Set to true for always full-screen, false for windowed
    private final RegionDAO reclamationService = new RegionDAO();
    private final EvenementDAO tagService = new EvenementDAO();

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("DEBUG: Starting MainApp");
        String fxmlFile;
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            fxmlFile = "/com/example/auth/login.fxml";
        } else {
            fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/adminDashboard.fxml" : "/com/example/auth/dashboard.fxml";
        }
        System.out.println("DEBUG: Loading FXML: " + fxmlFile);
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        if (root == null) {
            System.out.println("DEBUG: Failed to load " + fxmlFile + " - root is null");
            return;
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
        primaryStage.setResizable(true); // Allow resizing and enable title bar controls
        primaryStage.setFullScreen(FULL_SCREEN); // Set initial full-screen state
        primaryStage.show();
        System.out.println("DEBUG: MainApp started successfully");
    }

    public static void main(String[] args) {
        launch(args);
    }
}