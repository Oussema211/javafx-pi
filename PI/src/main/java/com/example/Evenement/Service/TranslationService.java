
package com.example.Evenement.Service;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

public class TranslationService {
    private static final TranslationService instance = new TranslationService();
    private String currentLanguage = "fr"; // Français par défaut
    private static final Map<String, String> LANGUAGE_CODES = Map.of(
        "fr", "fr",
        "en", "en",
        "ar", "ar"
    );
    
    private static final Map<String, String> LANGUAGE_NAMES = Map.of(
        "fr", "Français",
        "en", "Anglais",
        "ar", "Arabe"
    );
    private static final String API_URL = "https://api.mymemory.translated.net/get";

    private TranslationService() {
        // Initialisation du service
    }

    public static TranslationService getInstance() {
        return instance;
    }

    public void setLanguage(String language) {
        if (language == null || language.isEmpty()) {
            throw new IllegalArgumentException("La langue ne peut pas être null ou vide");
        }
        
        String normalizedLanguage = language.toLowerCase();
        if (!LANGUAGE_CODES.containsKey(normalizedLanguage)) {
            throw new IllegalArgumentException("Langue non supportée: " + language);
        }
        
        currentLanguage = normalizedLanguage;
        System.out.println("Langue sélectionnée: " + LANGUAGE_NAMES.get(currentLanguage));
    }

    public String translateText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        HttpURLConnection connection = null;
        
        try {
            // Vérifier la longueur du texte
            if (text.length() > 5000) {
                throw new IllegalArgumentException("Le texte est trop long pour la traduction");
            }

            // Préparer la requête
            String query = URLEncoder.encode(text, "UTF-8");
            String sourceCode = "fr";
            String targetCode = LANGUAGE_CODES.get(currentLanguage);
            String langPair = sourceCode + "|" + targetCode;
            System.out.println("Langue source: " + sourceCode + ", Langue cible: " + targetCode);
            URL url = new URL(API_URL + "?q=" + query + "&langpair=" + langPair);
            
            // Envoyer la requête
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(10000); // 10 secondes
            connection.setReadTimeout(10000); // 10 secondes

            // Gérer la réponse
            int responseCode = connection.getResponseCode();
            System.out.println("Code de réponse: " + responseCode);
            
            if (responseCode == 200) {
                try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.out.println("Réponse: " + response.toString());
                    JSONObject json = new JSONObject(response.toString());
                    JSONObject responseDetails = json.getJSONObject("responseData");
                    String translatedText = responseDetails.getString("translatedText");
                    if (translatedText.equals("PLEASE SELECT TWO DISTINCT LANGUAGES")) {
                        System.err.println("Erreur: Les langues source et cible doivent être différentes");
                        return text;
                    }
                    return translatedText;
                }
            } else {
                System.err.println("Erreur de traduction (code " + responseCode + "): Utilisation du texte original");
                return text;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la traduction : " + e.getMessage() + ". Utilisation du texte original");
            e.printStackTrace(); // Afficher la trace complète de l'erreur
            return text;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public boolean isServiceAvailable() {
        try {
            // Tester la traduction d'un texte simple
            String testText = "test";
            String translatedText = translateText(testText);
            System.out.println("Test de traduction: " + testText + " -> " + translatedText);
            return !translatedText.equals(testText); // Si le texte a été traduit, le service fonctionne
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de la disponibilité du service : " + e.getMessage());
            e.printStackTrace(); // Afficher la trace complète de l'erreur
            return false;
        }
    }
}
