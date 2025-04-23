package com.example.reclamation.controller;

import com.example.auth.controller.DashboardFrontController;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.example.auth.utils.SessionManager;

public class NavbarController implements Initializable {
    private DashboardFrontController dashboardFrontController;

    @FXML private Label logoLabel;
    @FXML private Label shopLabel;
    @FXML private Label collectionsLabel;
    @FXML private Label exploreLabel;
    @FXML private Label moreLabel;
    @FXML private Label cartLabel;
    @FXML private Label accountLabel;
    @FXML private Label profileLabel;
    @FXML private Label logoutButton;
    private Label cartIconLabel;   // Pour l'icône 🛒
    private Label cartBadgeLabel;  // Pour le nombre
    private StackPane cartStackPane; // Pour contenir les deux ensemble

    private final String defaultStyle = "-fx-font: normal 14px 'Arial'; -fx-text-fill: #000000; -fx-cursor: hand;";
    private final String hoverStyle = "-fx-font: normal 14px 'Arial'; -fx-text-fill: #000000; -fx-cursor: hand; -fx-border-color: #000000; -fx-border-width: 0 0 1 0;";

    private final SessionManager sessionManager = SessionManager.getInstance();
    public void setDashboardFrontController(DashboardFrontController controller) {
        this.dashboardFrontController = controller;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupHoverEffects();
        setupClickActions();
        setupCartIcon();

    }

    private void setupHoverEffects() {
        shopLabel.setOnMouseEntered(e -> shopLabel.setStyle(hoverStyle));
        shopLabel.setOnMouseExited(e -> shopLabel.setStyle(defaultStyle));
        collectionsLabel.setOnMouseEntered(e -> collectionsLabel.setStyle(hoverStyle));
        collectionsLabel.setOnMouseExited(e -> collectionsLabel.setStyle(defaultStyle));
        exploreLabel.setOnMouseEntered(e -> exploreLabel.setStyle(hoverStyle));
        exploreLabel.setOnMouseExited(e -> exploreLabel.setStyle(defaultStyle));
        moreLabel.setOnMouseEntered(e -> moreLabel.setStyle(hoverStyle));
        moreLabel.setOnMouseExited(e -> moreLabel.setStyle(defaultStyle));
        cartLabel.setOnMouseEntered(e -> cartLabel.setStyle(hoverStyle));
        cartLabel.setOnMouseExited(e -> cartLabel.setStyle(defaultStyle));
        accountLabel.setOnMouseEntered(e -> accountLabel.setStyle(hoverStyle));
        accountLabel.setOnMouseExited(e -> accountLabel.setStyle(defaultStyle));
        profileLabel.setOnMouseEntered(e -> profileLabel.setStyle(hoverStyle));
        profileLabel.setOnMouseExited(e -> profileLabel.setStyle(defaultStyle));
        logoutButton.setOnMouseEntered(e -> logoutButton.setStyle(hoverStyle));
        logoutButton.setOnMouseExited(e -> logoutButton.setStyle(defaultStyle));
    }

    private void setupClickActions() {
        shopLabel.setOnMouseClicked(e -> openCartPage());

        collectionsLabel.setOnMouseClicked(e -> System.out.println("Collections clicked"));
        exploreLabel.setOnMouseClicked(e -> navigateToReclamation());
        moreLabel.setOnMouseClicked(e -> System.out.println("More clicked"));
        cartLabel.setOnMouseClicked(event -> {
            if (dashboardFrontController != null) {
                dashboardFrontController.loadContent("/com/example/frontPages/pages/cart.fxml");
            }
        });



        accountLabel.setOnMouseClicked(e -> {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/profile.fxml"));
                Scene scene = new Scene(root);
                Stage stage = (Stage) accountLabel.getScene().getWindow();
                scene.getStylesheets().add(getClass().getClassLoader().getResource("com/example/auth/style.css").toExternalForm());
                stage.setScene(scene);
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        profileLabel.setOnMouseClicked(e -> {
            try {
                Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/profile.fxml"));
                Scene scene = new Scene(root);
                Stage stage = (Stage) profileLabel.getScene().getWindow();
                scene.getStylesheets().add(getClass().getClassLoader().getResource("com/example/auth/style.css").toExternalForm());
                stage.setScene(scene);
                stage.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }
    private void openCartPage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/frontPages/pages/cart.fxml"));
            Parent cartPage = loader.load();

            BorderPane root = (BorderPane) shopLabel.getScene().lookup("#borderPane");
            if (root != null) {
                root.setCenter(cartPage);
            } else {
                System.out.println("BorderPane not found!");
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    public void updateCartCount(int count) {
        cartBadgeLabel.setText(String.valueOf(count));
    }


    @FXML
    private void handleProfileClick() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/profile.fxml"));
            Scene scene = new Scene(root);
            Stage stage = (Stage) profileLabel.getScene().getWindow();
            scene.getStylesheets().add(getClass().getClassLoader().getResource("com/example/auth/style.css").toExternalForm());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void navigateToReclamation() {
        try {
            Stage stage = (Stage) exploreLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/reclamation/Reclamation.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Reclamations");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load Reclamation.fxml: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() throws IOException {
        sessionManager.clearSession();
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/auth/login.fxml"));
        stage.setScene(new Scene(root));
    }
    private void setupCartIcon() {
        cartIconLabel = new Label("🛒");
        cartIconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: black;");

        cartBadgeLabel = new Label("0");
        cartBadgeLabel.setStyle(
                "-fx-background-color: red; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-min-width: 18px; " +
                        "-fx-min-height: 18px; " +
                        "-fx-alignment: center; " +
                        "-fx-padding: 2;"
        );

        cartStackPane = new StackPane(cartIconLabel, cartBadgeLabel);
        StackPane.setMargin(cartBadgeLabel, new javafx.geometry.Insets(-10, -10, 0, 10)); // Le badge légèrement en haut à droite

        cartLabel.setText(""); // Efface l'ancien texte
        cartLabel.setGraphic(cartStackPane); // Ajoute uniquement le nouveau design
        cartStackPane.setOnMouseClicked(event -> {
            if (dashboardFrontController != null) {
                dashboardFrontController.loadContent("/com/example/frontPages/pages/cart.fxml");
            }
        });


    }

}