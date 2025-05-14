package com.example.Stock.modelIa;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileReader;

public class StockAlertApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Lire le rapport JSON
        JSONObject rapport;
        try (FileReader reader = new FileReader("rapport_stock.json")) {
            rapport = new JSONObject(new JSONTokener(reader));
        }

        // Créer la liste des alertes
        ListView<String> alertesList = new ListView<>();
        JSONArray alertes = rapport.getJSONArray("alertes");
        for (int i = 0; i < alertes.length(); i++) {
            JSONObject alerte = alertes.getJSONObject(i);
            String texte = String.format("Produit: %s, Entrepôt: %s, Jours avant rupture: %.2f, Quantité: %.2f",
                    alerte.getString("produit_id"), alerte.getString("entrepot_id"),
                    alerte.getDouble("jours_avant_rupture"), alerte.getDouble("quantite_actuelle"));
            alertesList.getItems().add(texte);
        }

        // Créer le graphique
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Produit Index");
        yAxis.setLabel("Quantité Actuelle");

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Quantités Actuelles et Risques de Rupture");

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Quantité Actuelle");

        for (int i = 0; i < alertes.length(); i++) {
            JSONObject alerte = alertes.getJSONObject(i);
            series.getData().add(new XYChart.Data<>(i, alerte.getDouble("quantite_actuelle")));
        }

        lineChart.getData().add(series);

        // Créer les recommandations saisonnières
        ListView<String> recommandationsList = new ListView<>();
        JSONArray recommandations = rapport.getJSONArray("recommandations_saison");
        for (int i = 0; i < recommandations.length(); i++) {
            JSONObject reco = recommandations.getJSONObject(i);
            String texte = String.format("Produit: %s, Mois: %s, Quantité recommandée: %.2f",
                    reco.getString("produit_id"), reco.getString("mois_prochain"),
                    reco.getDouble("quantite_recommandee"));
            recommandationsList.getItems().add(texte);
        }

        // Mise en page
        VBox root = new VBox(10);
        root.getChildren().addAll(
                new Label("Alertes de Rupture de Stock"), alertesList,
                new Label("Graphique des Quantités"), lineChart,
                new Label("Recommandations Saisonnières"), recommandationsList
        );

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Gestion des Stocks Agricoles");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}