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
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URL;

public class ResetLinkServer {
    private static HttpServer server;
    private static final Object lock = new Object();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ResetLinkServer::stopServer));
    }

    private static boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void stopServer() {
        synchronized (lock) {
            if (server != null) {
                try {
                    server.stop(1); // Graceful shutdown with 1-second delay
                    System.out.println("Reset link server stopped");
                } catch (Exception e) {
                    System.err.println("Error stopping server: " + e.getMessage());
                } finally {
                    server = null;
                }
            }
        }
    }

    public static boolean startServer() {
        synchronized (lock) {
            stopServer(); // Ensure any existing server is stopped

            int port = 8082;
            if (!isPortAvailable(port)) {
                System.err.println("Port " + port + " is already in use. Server not started.");
                return false;
            }

            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/reset-password", new ResetPasswordHandler());
                server.setExecutor(null);
                server.start();
                System.out.println("Reset link server started on port " + port);
                return true;
            } catch (BindException e) {
                System.err.println("Failed to start server on port " + port + ": " + e.getMessage());
                return false;
            } catch (IOException e) {
                System.err.println("Unexpected error starting server on port " + port + ": " + e.getMessage());
                return false;
            }
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
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

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

                    URL stylesheetUrl = ResetLinkServer.class.getClassLoader().getResource("com/example/auth/styles.css");
                    if (stylesheetUrl != null) {
                        scene.getStylesheets().add(stylesheetUrl.toExternalForm());
                    } else {
                        System.out.println("DEBUG: Could not find styles.css in ResetLinkServer");
                    }

                    stage.setScene(scene);
                    stage.show();

                    // Do not stop server on window close; let it run until token expiry or reset
                    stage.setOnCloseRequest(event -> {
                        System.out.println("Password reset window closed");
                    });
                } catch (IOException e) {
                    System.err.println("Error loading changePassword.fxml: " + e.getMessage());
                }
            });

            String response = "Opening password reset window...";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
}