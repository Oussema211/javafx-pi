package com.example.auth.controller;

import com.example.auth.model.User;
import utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class DashboardController {
    @FXML private Label welcomeLabel;
    @FXML private Label emailLabel;

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