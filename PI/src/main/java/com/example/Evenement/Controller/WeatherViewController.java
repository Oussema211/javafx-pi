package com.example.Evenement.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

public class WeatherViewController {
    @FXML private Label cityLabel;
    @FXML private Label dateLabel;
    @FXML private ImageView weatherIcon;
    @FXML private Label temperatureLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label humidityLabel;
    @FXML private Label windLabel;
    @FXML private Label pressureLabel;
    @FXML private Label visibilityLabel;

    private Stage stage;

    public void setWeatherData(JSONObject weatherData, String city, LocalDateTime date) {
        // Mise à jour des labels ville et date
        cityLabel.setText(city);
        dateLabel.setText(date.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));

        // Extraction et affichage des données météo
        JSONObject main = weatherData.getJSONObject("main");
        JSONObject weather = weatherData.getJSONArray("weather").getJSONObject(0);
        JSONObject wind = weatherData.getJSONObject("wind");

        // Température
        double temp = main.getDouble("temp");
        temperatureLabel.setText(String.format("%.1f", temp));

        // Description
        String description = weather.getString("description");
        descriptionLabel.setText(description.substring(0, 1).toUpperCase() + description.substring(1));

        // Humidité
        int humidity = main.getInt("humidity");
        humidityLabel.setText(humidity + "%");

        // Vent
        double windSpeed = wind.getDouble("speed");
        windLabel.setText(String.format("%.1f km/h", windSpeed * 3.6)); // Conversion m/s en km/h

        // Pression
        int pressure = main.getInt("pressure");
        pressureLabel.setText(pressure + " hPa");

        // Visibilité
        int visibility = weatherData.getInt("visibility");
        visibilityLabel.setText(String.format("%.1f km", visibility / 1000.0));

        // Icône météo
        String iconCode = weather.getString("icon");
        String iconUrl = String.format("http://openweathermap.org/img/w/%s.png", iconCode);
        weatherIcon.setImage(new Image(iconUrl));
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void handleClose() {
        stage.close();
    }
} 