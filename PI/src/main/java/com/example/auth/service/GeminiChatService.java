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
    private static final String SYSTEM_INSTRUCTION = "You are a friendly and knowledgeable assistant who only answers questions related to agriculture (farming, crops, soil, livestock, irrigation, sustainable practices, etc.) and issues with the JavaFX desktop application AgriPlanner, specifically focused on login and signup systems including Face ID authentication, password reset, and user authentication—kindly decline to respond to any unrelated topics.";

    public GeminiChatService() {
        try {
            System.out.println("DEBUG: Current working directory: " + System.getProperty("user.dir"));
            System.out.println("DEBUG: Checking for .env file in classpath (src/main/resources/.env)");
            // Check if .env file exists in classpath
            if (this.getClass().getResource("/.env") == null) {
                System.out.println("DEBUG: .env file not found in classpath (target/classes/.env)");
            } else {
                System.out.println("DEBUG: .env file found in classpath");
            }
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            String dotenvKey = dotenv.get("GOOGLE_API_KEY");
            System.out.println("DEBUG: GOOGLE_API_KEY from .env: " + (dotenvKey != null ? "present (value: " + maskKey(dotenvKey) + ")" : "null"));
            String envKey = System.getenv("GOOGLE_API_KEY");
            System.out.println("DEBUG: GOOGLE_API_KEY from environment: " + (envKey != null ? "present (value: " + maskKey(envKey) + ")" : "null"));
            this.apiKey = Optional.ofNullable(dotenvKey)
                    .orElseGet(() -> envKey);
            if (this.apiKey == null || this.apiKey.isEmpty()) {
                System.err.println("WARNING: GOOGLE_API_KEY not found in .env or environment variables. Chat functionality will be disabled.");
            } else if (!this.apiKey.matches("^[A-Za-z0-9_-]{30,}$")) {
                System.err.println("WARNING: GOOGLE_API_KEY appears invalid (unexpected format). Expected a long alphanumeric string.");
                this.apiKey = null;
            } else {
                System.out.println("DEBUG: GOOGLE_API_KEY loaded successfully: " + maskKey(apiKey));
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to load .env file: " + e.getMessage());
            this.apiKey = null;
        }
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.chatHistory = new ArrayList<>();
        // Initialize with system instruction
        this.chatHistory.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
        ));
    }

    // Utility to mask API key for logging (show first 6 and last 4 characters)
    private String maskKey(String key) {
        if (key == null || key.length() < 10) {
            return "invalid";
        }
        return key.substring(0, 6) + "..." + key.substring(key.length() - 4);
    }

    public String sendMessage(String userMessage) throws Exception {
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("DEBUG: sendMessage called, but apiKey is null or empty. Returning error message.");
            return "Chat service is not configured properly. Please check API key.";
        }

        System.out.println("DEBUG: Sending message to Gemini API: " + userMessage);
        // Add user message to history
        chatHistory.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))
        ));

        // Prepare request body
        Map<String, Object> requestBody = Map.of(
                "contents", chatHistory
        );

        // Build HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(API_URL + "?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(requestBody)))
                .build();

        // Send request
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("DEBUG: Gemini API response status: " + response.statusCode());
        if (response.statusCode() != 200) {
            System.err.println("ERROR: Gemini API request failed with status: " + response.statusCode() + ", body: " + response.body());
            throw new RuntimeException("Gemini API request failed with status: " + response.statusCode() + ", body: " + response.body());
        }

        // Parse response
        Map<String, Object> responseJson = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseJson.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new RuntimeException("No candidates in Gemini API response");
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, String>> parts = (List<Map<String, String>>) content.get("parts");
        String responseText = parts.get(0).get("text");

        // Add assistant response to history
        chatHistory.add(Map.of(
                "role", "model",
                "parts", List.of(Map.of("text", responseText))
        ));

        System.out.println("DEBUG: Gemini API response text: " + responseText);
        return responseText;
    }
}