package com.example.auth;

import com.example.Stock.service.StockService;
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

public class MainApp extends Application {
    private SessionManager sessionManager = SessionManager.getInstance();
    private static final boolean FULL_SCREEN = false;
    private final AuthService authService = new AuthService();
    private final TagService tagService = new TagService();
    private final ReclamationService reclamationService = new ReclamationService();
    private final StockService stockService = new StockService();
    private final MessageReclamationService messageReclamationService = new MessageReclamationService();

    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("DEBUG: Starting MainApp");
        try {
            ResetLinkServer.startServer(); // Essayez de démarrer le serveur
        } catch (java.net.BindException e) {
            System.err.println("Erreur : Le port est déjà utilisé. Essayez un autre port ou fermez l'application qui l'utilise.");
            throw e; // Relance l'exception pour arrêter l'application proprement
        }

        String fxmlFile;
        User user = sessionManager.getLoggedInUser();
        if (user == null) {
            fxmlFile = "/com/example/auth/login.fxml";
        } else {
            fxmlFile = user.hasRole("ROLE_ADMIN") ? "/com/example/auth/dashboard.fxml" : "/com/example/reclamation/Reclamation.fxml";
        }
        System.out.println("DEBUG: Loading FXML: " + fxmlFile);
        Parent root = FXMLLoader.load(getClass().getResource(fxmlFile));
        if (root == null) {
            System.out.println("DEBUG: Failed to load " + fxmlFile + " - root is null");
            return;
        }
        Scene scene = new Scene(root, 400, 500);

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
        });
    }

    @Override
    public void stop() {
        ResetLinkServer.stopServer(); // Assurez-vous que le serveur s'arrête même en cas d'arrêt inattendu
    }

    public static void main(String[] args) {
        launch(args);
    }
}