package com.example.Stock.Controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

public class MapView extends StackPane {
    private final WebView webView = new WebView();
    private final WebEngine webEngine = webView.getEngine();
    private final DoubleProperty latitude = new SimpleDoubleProperty();
    private final DoubleProperty longitude = new SimpleDoubleProperty();
    private final BooleanProperty mapReady = new SimpleBooleanProperty(false);

    public MapView() {
        // Bind le WebView à la taille du conteneur
        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());
        webView.setContextMenuEnabled(false);
        webView.setStyle("-fx-background-color: transparent;-fx-opacity: 0.99;");

        setupWebEngine();
        getChildren().add(webView);
    }
    private void setupWebEngine() {
        webView.setZoom(0.8); // Légère réduction pour forcer le recalcul
        webEngine.setJavaScriptEnabled(true);

        // Ajouter un écouteur pour les erreurs
        webEngine.setOnError(event -> {
            System.err.println("WebEngine error: " + event.getMessage());
        });

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Triple vérification du chargement
                Platform.runLater(() -> {
                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("javaBridge", new JavaBridge());

                    // Premier essai
                    webEngine.executeScript("initMap()");

                    // Deuxième essai après délai
                    PauseTransition pt1 = new PauseTransition(Duration.seconds(0.5));
                    pt1.setOnFinished(e -> {
                        webEngine.executeScript("initMap()");
                        scheduleResize();
                    });
                    pt1.play();

                    // Troisième essai
                    PauseTransition pt2 = new PauseTransition(Duration.seconds(1));
                    pt2.setOnFinished(e -> scheduleResize());
                    pt2.play();
                });
            }
        });

        // Charger la page HTML
        String url = getClass().getResource("/com/example/Stock/view/map.html").toExternalForm();
        webEngine.load(url);
    }
    private void showError(String msg) {
        Platform.runLater(() -> {
            Label lbl = new Label(msg);
            lbl.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
            getChildren().add(lbl);
        });
    }

    public void refreshMapSize() {
        Platform.runLater(() -> {
            webEngine.executeScript(
                    "if (typeof map !== 'undefined') {"
                            + "  setTimeout(function() { map.invalidateSize(); }, 100);"
                            + "}"
            );
        });
    }

    public void setLocation(double lat, double lng) {
        Platform.runLater(() ->
                webEngine.executeScript(String.format("setMapView(%f, %f)", lat, lng))
        );
    }

    public void setMapType(String type) {
        Platform.runLater(() -> {
            if (webEngine.getLoadWorker().getState() == Worker.State.SUCCEEDED) {
                webEngine.executeScript(
                        "if (typeof setMapType === 'function') setMapType('" + type + "');"
                );
            }
        });
    }
    public void scheduleResize() {
        Platform.runLater(() -> {
            // Solution complète pour le redimensionnement
            webEngine.executeScript(
                    "if (typeof map !== 'undefined') {"
                            + "  setTimeout(function() {"
                            + "    map.invalidateSize({animate: false, pan: false});"
                            + "    map._resetView(map.getCenter(), map.getZoom());"
                            + "    Array.from(document.querySelectorAll('.leaflet-tile')).forEach(tile => {"
                            + "      tile.style.transform = 'none';"
                            + "      tile.style.imageRendering = 'crisp-edges';"
                            + "    });"
                            + "  }, 100);"
                            + "}"
            );
        });
    }

    // Getters pour binding et accès direct
    public BooleanProperty mapReadyProperty() { return mapReady; }
    public DoubleProperty latitudeProperty() { return latitude; }
    public DoubleProperty longitudeProperty() { return longitude; }

    public double getLatitude() { return latitude.get(); }
    public double getLongitude() { return longitude.get(); }

    public void setZoom(int i) {
    }

    public class JavaBridge {
        public void onMapReady() {
            mapReady.set(true);
            refreshMapSize();
        }
        public void onLocationChanged(double lat, double lng) {
            latitude.set(lat);
            longitude.set(lng);
        }
    }
}