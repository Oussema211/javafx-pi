package com.example.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GeminiChatService {
    private String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final List<Map<String, Object>> chatHistory;
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent";

    public GeminiChatService(String systemInstruction) {
        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String dotenvKey = dotenv.get("GOOGLE_API_KEY");
            String envKey = System.getenv("GOOGLE_API_KEY");
            this.apiKey = Optional.ofNullable(dotenvKey).orElseGet(() -> envKey);

            if (this.apiKey == null || this.apiKey.isEmpty()) {
                System.err.println("WARNING: GOOGLE_API_KEY not found in .env or environment variables.");
            } else if (!this.apiKey.matches("^[A-Za-z0-9_-]{30,}$")) {
                System.err.println("WARNING: GOOGLE_API_KEY appears invalid.");
                this.apiKey = null;
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to load .env file: " + e.getMessage());
            this.apiKey = null;
        }
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.chatHistory = new ArrayList<>();
        this.chatHistory.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", systemInstruction))
        ));
    }

    public String sendMessage(String userMessage) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            return "Chat service is not configured properly. Please check API key.";
        }

        chatHistory.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        Map<String, Object> requestBody = Map.of(
                "contents", chatHistory
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(API_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Gemini API request failed with status: " + response.statusCode());
        }

        Map<String, Object> responseJson = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseJson.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No candidates in Gemini API response");
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
        String responseText = parts.get(0).get("text");

        chatHistory.add(Map.of(
                "role", "model",
                "parts", List.of(Map.of("text", responseText))
        ));

        return responseText;
    }
}