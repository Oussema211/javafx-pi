package com.example.Stock.Controller;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Worker;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.util.Locale;

public class MapView extends StackPane {
    private final WebView webView = new WebView();
    private final WebEngine webEngine = webView.getEngine();
    private final DoubleProperty latitude = new SimpleDoubleProperty();
    private final DoubleProperty longitude = new SimpleDoubleProperty();
    private final BooleanProperty mapReady = new SimpleBooleanProperty(false);

    public MapView() {
        configureWebView();
        setupWebEngine();
        getChildren().add(webView);
    }

    private void configureWebView() {
        webView.prefWidthProperty().bind(widthProperty());
        webView.prefHeightProperty().bind(heightProperty());
        webView.setContextMenuEnabled(false);
    }

    private void setupWebEngine() {
        webEngine.setJavaScriptEnabled(true);
        webEngine.setOnError(event -> System.err.println("WebEngine error: " + event.getMessage()));

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            System.out.println("WebView load state: " + newState);
            if (newState == Worker.State.SUCCEEDED) {
                Platform.runLater(this::initializeMap);
            } else if (newState == Worker.State.FAILED) {
                System.err.println("WebView load failed: " + webEngine.getLoadWorker().getException());
                showError("Échec du chargement de la carte");
            }
        });

        String resourcePath = "/com/example/Stock/view/map.html";
        String url = getClass().getResource(resourcePath).toExternalForm();
        if (url == null) {
            System.err.println("Map HTML resource not found: " + resourcePath);
            showError("Ressource de la carte introuvable");
            return;
        }
        System.out.println("Loading map.html from: " + url);
        webEngine.load(url);
    }

    private void initializeMap() {
        try {
            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("javaBridge", new JavaBridge());
            webEngine.executeScript("initMap();");
            mapReady.set(true);
        } catch (Exception e) {
            System.err.println("Map initialization error: " + e.getMessage());
            showError("Erreur d'initialisation de la carte");
        }
    }

    public void setLocation(double lat, double lng) {
        if (!mapReady.get()) {
            System.out.println("Map not ready, queuing location update");
            mapReady.addListener((obs, old, isReady) -> {
                if (isReady) setLocation(lat, lng);
            });
            return;
        }

        Platform.runLater(() -> {
            try {
                String js = String.format(Locale.US,
                        "setMapView(%f, %f, 15);", lat, lng);
                webEngine.executeScript(js);
            } catch (Exception e) {
                System.err.println("Set location error: " + e.getMessage());
            }
        });
    }

    public void setMapType(String type) {
        if (!mapReady.get()) {
            System.out.println("Map not ready for map type change, queuing");
            mapReady.addListener((obs, old, isReady) -> {
                if (isReady) setMapType(type);
            });
            return;
        }

        Platform.runLater(() -> {
            try {
                webEngine.executeScript(String.format("setMapType('%s');", type));
            } catch (Exception e) {
                System.err.println("Set map type error: " + e.getMessage());
            }
        });
    }

    public void setZoom(int zoomLevel) {
        if (!mapReady.get()) return;

        Platform.runLater(() -> {
            try {
                webEngine.executeScript("setZoom(" + zoomLevel + ");");
            } catch (Exception e) {
                System.err.println("Set zoom error: " + e.getMessage());
            }
        });
    }

    private void showError(String msg) {
        Platform.runLater(() -> {
            Label lbl = new Label(msg);
            lbl.setStyle("-fx-text-fill: red; -fx-font-weight: bold; -fx-background-color: white; -fx-padding: 5;");
            getChildren().add(lbl);
        });
    }
    public void forceUpdate() {
        Platform.runLater(() -> {
            webEngine.executeScript("""
            if (window.marker) {
                var pos = marker.getLatLng();
                window.javaBridge.onLocationChanged(pos.lat, pos.lng);
            }
        """);
        });
    }

    public BooleanProperty mapReadyProperty() { return mapReady; }
    public DoubleProperty latitudeProperty() { return latitude; }
    public DoubleProperty longitudeProperty() { return longitude; }
    public double getLatitude() { return latitude.get(); }
    public double getLongitude() { return longitude.get(); }

    public class JavaBridge {
        public void onMapReady() {
            Platform.runLater(() -> {
                mapReady.set(true);
                System.out.println("Map is ready");
            });
        }
        public void onLocationChanged(double lat, double lng) {
            Platform.runLater(() -> {
                System.out.println("Reçu de JS - Lat: " + lat + " Lng: " + lng);
                latitude.set(lat);
                longitude.set(lng);
                notifyLocationChanged();
                // Force la notification
            });
        }


        public void notifyLocationChanged() {
            Platform.runLater(() -> {
                latitude.set(latitude.get()); // Force le refresh
                longitude.set(longitude.get());
            });
        }
    }
}