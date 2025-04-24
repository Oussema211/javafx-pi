package com.example.auth.utils;

import com.example.auth.service.AuthService;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.example.auth.controller.CodeVerificationController;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class VerificationLinkServer {
    private static HttpServer server;

    public static boolean startServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/verify", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String code = null;
                String finalEmail;
                if (query != null) {
                    String tempEmail = null;
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) {
                            code = param.split("code=")[1];
                        } else if (param.startsWith("email=")) {
                            tempEmail = param.split("email=")[1];
                        }
                    }
                    finalEmail = tempEmail != null ? tempEmail : "";
                } else {
                    finalEmail = "";
                }

                AuthService authService = new AuthService();
                boolean verified = code != null && !finalEmail.isEmpty() && authService.verifyUser(code, finalEmail);

                String response = verified ? "Verification successful! Redirecting..." : "Invalid or expired code.";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

                // Load code verification UI in JavaFX
                Platform.runLater(() -> {
                    try {
                        Stage stage = new Stage();
                        FXMLLoader loader = new FXMLLoader(VerificationLinkServer.class.getResource("/com/example/auth/code-verification.fxml"));
                        Parent root = loader.load();
                        CodeVerificationController controller = loader.getController();
                        controller.setEmail(finalEmail);
                        Scene scene = new Scene(root, 400, 300);
                        java.net.URL cssResource = VerificationLinkServer.class.getClassLoader()
                                .getResource("com/example/auth/modern-theme.css");
                        if (cssResource != null) {
                            scene.getStylesheets().add(cssResource.toExternalForm());
                        } else {
                            System.err.println("Warning: modern-theme.css not found");
                        }
                        stage.setScene(scene);
                        stage.setTitle("Code Verification");
                        stage.show();
                    } catch (IOException e) {
                        System.err.println("Error loading verification UI: " + e.getMessage());
                    }
                });

                // Stop server after handling request
                stopServer();
            });
            server.setExecutor(null);
            server.start();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to start verification server: " + e.getMessage());
            return false;
        }
    }

    public static void stopServer() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }
}