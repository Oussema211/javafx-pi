package com.example.auth.utils;

import com.example.auth.controller.ChangePasswordController;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;

public class ResetLinkServer {
    private static HttpServer server;

    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(1100), 0);
        server.createContext("/reset-password", new ResetPasswordHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Reset link server started on port 8080");
    }

    public static void stopServer() {
        if (server != null) {
            server.stop(0);
            System.out.println("Reset link server stopped");
        }
    }

    static class ResetPasswordHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String token = null;
            if (query != null && query.startsWith("token=")) {
                token = query.substring(6);
            }

            if (token == null) {
                String response = "Invalid reset link";
                exchange.sendResponseHeaders(400, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            // Launch JavaFX UI on the JavaFX Application Thread
            String finalToken = token;
            Platform.runLater(() -> {
                try {
                    FXMLLoader loader = new FXMLLoader(ResetLinkServer.class.getResource("/com/example/auth/changePassword.fxml"));
                    Parent root = loader.load();
                    ChangePasswordController controller = loader.getController();
                    controller.setToken(finalToken);

                    Stage stage = new Stage();
                    stage.setTitle("Change Password");
                    Scene scene = new Scene(root, 400, 500);

                    // Load stylesheet
                    URL stylesheetUrl = ResetLinkServer.class.getClassLoader().getResource("com/example/auth/styles.css");
                    if (stylesheetUrl != null) {
                        scene.getStylesheets().add(stylesheetUrl.toExternalForm());
                    } else {
                        System.out.println("DEBUG: Could not find styles.css in ResetLinkServer");
                    }

                    stage.setScene(scene);
                    stage.show();
                } catch (IOException e) {
                    System.err.println("Error loading changePassword.fxml: " + e.getMessage());
                }
            });

            String response = "Opening password reset window...";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }
}