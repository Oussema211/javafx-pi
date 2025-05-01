package com.example.Evenement.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.JSONObject;

public class WeatherService {
    private static final String API_KEY = "c862417ec2509db80194ba7d18675ce9";
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather";

    public String getWeatherForDate(String city, LocalDateTime date) {
        try {
            // Construire l'URL avec les paramètres
            String urlString = String.format("%s?q=%s&appid=%s&units=metric&lang=fr",
                    BASE_URL, city, API_KEY);
            URL url = new URL(urlString);

            // Établir la connexion
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Lire la réponse
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parser la réponse JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject main = jsonResponse.getJSONObject("main");
            JSONObject weather = jsonResponse.getJSONArray("weather").getJSONObject(0);

            // Extraire les informations pertinentes
            double temperature = main.getDouble("temp");
            String description = weather.getString("description");
            double humidity = main.getDouble("humidity");

            // Formater la date
            String formattedDate = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            // Construire le message de retour
            return String.format("Météo pour %s le %s:\n" +
                    "Température: %.1f°C\n" +
                    "Conditions: %s\n" +
                    "Humidité: %.0f%%",
                    city, formattedDate, temperature, description, humidity);

        } catch (Exception e) {
            return "Erreur lors de la récupération de la météo: " + e.getMessage();
        }
    }
} 