package com.example.auth.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import java.io.IOException;

public class FrontDashboardController {
    @FXML private Button profileButton;

    @FXML
    private void goToProfile() throws IOException {
        Stage stage = (Stage) profileButton.getScene().getWindow();
        boolean isFullScreen = stage.isFullScreen();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/profile.fxml"));
        Scene scene = new Scene(root, 400, 500);
        scene.getStylesheets().add(getClass().getClassLoader().getResource("com/example/auth/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setFullScreen(isFullScreen);
        stage.show();
    }
}