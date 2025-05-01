package com.example.Evenement.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import org.json.JSONObject;

public class WeatherService {
    private static final String API_KEY = "c862417ec2509db80194ba7d18675ce9";
    private static final String BASE_URL = "http://api.openweathermap.org/data/2.5/weather";

    public JSONObject getWeatherForDate(String city, LocalDateTime date) throws Exception {
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

        // Retourner l'objet JSON complet
        return new JSONObject(response.toString());
    }
} 