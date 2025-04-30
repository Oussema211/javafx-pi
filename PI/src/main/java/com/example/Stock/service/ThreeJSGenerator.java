package com.example.Stock.service;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ThreeJSGenerator {
    public static WebView createWarehouseView(double length, double width, double height,
                                              int aisles, String storageType, String description,
                                              ImageCaptureCallback imageCallback,
                                              LoadingCompleteCallback loadingCallback,
                                              ProgressUpdateCallback progressCallback) {
        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();

        try {
            URL htmlUrl = ThreeJSGenerator.class.getResource("/com/example/Stock/View/modelViewer.html");
            if (htmlUrl == null) {
                System.err.println("Erreur: Fichier modelViewer.html non trouvé dans src/main/resources/com/example/Stock/View/");
                throw new RuntimeException("Fichier HTML non trouvé");
            }

            String url = htmlUrl.toExternalForm() +
                    "?length=" + length +
                    "&width=" + width +
                    "&height=" + height +
                    "&aisles=" + aisles +
                    "&storageType=" + URLEncoder.encode(storageType != null ? storageType : "", StandardCharsets.UTF_8) +
                    "&description=" + URLEncoder.encode(description != null ? description : "", StandardCharsets.UTF_8);

            engine.setJavaScriptEnabled(true);

            engine.setOnError(event -> System.err.println("Erreur WebView: " + event.getMessage()));
            engine.setOnAlert(event -> System.err.println("Alerte JavaScript: " + event.getData()));

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                System.out.println("État WebView: " + newState);
                if (newState == Worker.State.SUCCEEDED) {
                    try {
                        JSObject window = (JSObject) engine.executeScript("window");
                        window.setMember("javaBridge", new JavaBridge(imageCallback, loadingCallback, progressCallback));
                        System.out.println("JavaBridge initialisé avec succès");
                    } catch (Exception e) {
                        System.err.println("Erreur initialisation JavaBridge: " + e.getMessage());
                        if (loadingCallback != null) {
                            loadingCallback.onLoadingComplete(false);
                        }
                    }
                } else if (newState == Worker.State.FAILED) {
                    System.err.println("Échec du chargement WebView");
                    if (loadingCallback != null) {
                        loadingCallback.onLoadingComplete(false);
                    }
                }
            });

            System.out.println("Chargement de l'URL: " + url);
            engine.load(url);
        } catch (Exception e) {
            System.err.println("Erreur création vue 3D: " + e.getMessage());
            throw new RuntimeException("Erreur création vue 3D: " + e.getMessage(), e);
        }

        return webView;
    }

    public interface ImageCaptureCallback {
        void onImageCaptured(String imageData);
    }

    public interface LoadingCompleteCallback {
        void onLoadingComplete(boolean success);
    }

    public interface ProgressUpdateCallback {
        void onProgressUpdate(double progress);
    }

    public static class JavaBridge {
        private final ImageCaptureCallback imageCallback;
        private final LoadingCompleteCallback loadingCallback;
        private final ProgressUpdateCallback progressCallback;

        public JavaBridge(ImageCaptureCallback imageCallback,
                          LoadingCompleteCallback loadingCallback,
                          ProgressUpdateCallback progressCallback) {
            this.imageCallback = imageCallback;
            this.loadingCallback = loadingCallback;
            this.progressCallback = progressCallback;
        }

        public void onImageCaptured(String imageData) {
            System.out.println("Image capturée reçue dans JavaBridge: " + (imageData != null ? "Valide" : "Nulle"));
            if (imageCallback != null) {
                Platform.runLater(() -> imageCallback.onImageCaptured(imageData));
            }
        }

        public void onLoadingComplete(boolean success) {
            System.out.println("Chargement terminé: " + (success ? "Succès" : "Échec"));
            if (loadingCallback != null) {
                Platform.runLater(() -> loadingCallback.onLoadingComplete(success));
            }
        }

        public void onProgressUpdate(double progress) {
            System.out.println("Mise à jour progression: " + progress + "%");
            if (progressCallback != null) {
                Platform.runLater(() -> progressCallback.onProgressUpdate(progress));
            }
        }
    }
}