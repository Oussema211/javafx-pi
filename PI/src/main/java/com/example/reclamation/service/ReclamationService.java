package com.example.reclamation.service;

import utils.MyDatabase;

import com.example.reclamation.model.Reclamation;
import com.example.reclamation.model.Status;
import com.example.reclamation.model.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class ReclamationService {
    private final Connection conn;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final TagService tagService;

    public ReclamationService() {
        conn = MyDatabase.getInstance().getCnx();
        httpClient = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();
        tagService = new TagService();
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS reclamations (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "tag_id VARCHAR(255), " +
                    "date_reclamation DATETIME NOT NULL, " +
                    "rate INT NOT NULL, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "description VARCHAR(255) NOT NULL, " +
                    "statut VARCHAR(255) NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE SET NULL)";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing reclamations table: " + e.getMessage());
        }
    }

    public String assignTagToReclamation(UUID id) throws Exception {
        Reclamation reclamation = getReclamationById(id);
        if (reclamation == null) {
            return null;
        }
        List<Tag> tags = tagService.getAllTags();
        if (tags.isEmpty()) {
            System.err.println("No tags found in the database.");
            return null;
        }

        String formattedTags = tags.stream()
                .map(Tag::getName)
                .collect(Collectors.joining(", "));

        String description = reclamation.getDescription();
        String prompt = "answer with only one of those tags: " + formattedTags + " to this reclamation " + description;
        String apiKey = "AIzaSyBaRoGkT-edsd9WToHHsSjEaCfaNzLcYM4";
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Gemini API key is not set in the environment.");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent?key=" + apiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        objectMapper.writeValueAsString(Map.of(
                                "contents", List.of(Map.of(
                                        "parts", List.of(Map.of("text", prompt))
                                ))
                        ))
                ))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.err.println("Gemini API request failed with status: " + response.statusCode());
            return null;
        }

        ObjectNode root = objectMapper.readValue(response.body(), ObjectNode.class);
        String responseText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText("").trim();
        System.out.println("Gemini response: " + responseText);
        Tag tag = tagService.getTagByName(responseText);
        if (tag == null) {
            System.err.println("Tag not found: " + responseText);
            return null;
        }

        reclamation.setTagId(tag.getId());
        boolean updated = updateReclamation(reclamation);
        if (!updated) {
            System.err.println("Failed to update reclamation with tag: " + responseText);
            return null;
        }

        return responseText;
    }

    public boolean addReclamation(UUID userId, UUID tagId, int rate, String title, String description, Status statut) {
        UUID id = UUID.randomUUID();
        Date dateReclamation = new Date();
        String sql = "INSERT INTO reclamations (id, user_id, tag_id, date_reclamation, rate, title, description, statut) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.setString(2, userId.toString());
            pstmt.setString(3, null); // Set tag_id to null initially; assignTagToReclamation will set it
            pstmt.setTimestamp(4, new Timestamp(dateReclamation.getTime()));
            pstmt.setInt(5, rate);
            pstmt.setString(6, title);
            pstmt.setString(7, description);
            pstmt.setString(8, statut.getDisplayName());
            int rowsAffected = pstmt.executeUpdate();
    
            if (rowsAffected > 0) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("id", id.toString());
                payload.put("userId", userId.toString());
                payload.put("title", title);
                payload.put("description", description);
                payload.put("status", statut.getDisplayName());
                payload.put("date", Instant.ofEpochMilli(dateReclamation.getTime()).toString());
    
                System.out.println("Attempting to trigger Pusher event on channel 'admins' with event 'new-reclamation'");
                try {
                    PusherClient.get().trigger("admins", "new-reclamation", payload);
                    System.out.println("Successfully triggered Pusher event for reclamation ID: " + id);
                } catch (Exception e) {
                    System.err.println("Failed to trigger Pusher event: " + e.getMessage());
                    e.printStackTrace();
                }
    
                // Call assignTagToReclamation to automatically assign a tag
                try {
                    String assignedTag = assignTagToReclamation(id);
                    if (assignedTag == null && tagId != null) {
                        // Fallback to provided tagId if assignTagToReclamation fails
                        Reclamation reclamation = getReclamationById(id);
                        if (reclamation != null) {
                            reclamation.setTagId(tagId);
                            updateReclamation(reclamation);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to assign tag to reclamation: " + e.getMessage());
                    // Fallback to provided tagId if available
                    if (tagId != null) {
                        Reclamation reclamation = getReclamationById(id);
                        if (reclamation != null) {
                            reclamation.setTagId(tagId);
                            updateReclamation(reclamation);
                        }
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            if (e.getMessage().contains("FOREIGN KEY")) {
                System.err.println("Foreign key error: User ID " + userId + " does not exist.");
            } else {
                System.err.println("Error adding reclamation: " + e.getMessage());
            }
            return false;
        }
    }

    public Reclamation getReclamationById(UUID id) {
        String sql = "SELECT * FROM reclamations WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Reclamation(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("tag_id") != null ? UUID.fromString(rs.getString("tag_id")) : null,
                        rs.getTimestamp("date_reclamation"),
                        rs.getInt("rate"),
                        rs.getString("title"),
                        rs.getString("description"),
                        Status.fromString(rs.getString("statut"))
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching reclamation by ID: " + e.getMessage());
        }
        return null;
    }

    public List<Reclamation> getAllReclamations() {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamations";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                reclamations.add(new Reclamation(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("tag_id") != null ? UUID.fromString(rs.getString("tag_id")) : null,
                        rs.getTimestamp("date_reclamation"),
                        rs.getInt("rate"),
                        rs.getString("title"),
                        rs.getString("description"),
                        Status.fromString(rs.getString("statut"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all reclamations: " + e.getMessage());
        }
        return reclamations;
    }

    public List<Reclamation> getReclamationsByTag(UUID tagId) {
        List<Reclamation> reclamations = new ArrayList<>();
        String sql = "SELECT * FROM reclamations WHERE tag_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tagId.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                reclamations.add(new Reclamation(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("tag_id") != null ? UUID.fromString(rs.getString("tag_id")) : null,
                        rs.getTimestamp("date_reclamation"),
                        rs.getInt("rate"),
                        rs.getString("title"),
                        rs.getString("description"),
                        Status.fromString(rs.getString("statut"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching reclamations by tag ID: " + e.getMessage());
        }
        return reclamations;
    }

    public boolean updateReclamation(Reclamation reclamation) {
        String sql = "UPDATE reclamations SET user_id = ?, tag_id = ?, date_reclamation = ?, rate = ?, " +
                     "title = ?, description = ?, statut = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reclamation.getUserId().toString());
            pstmt.setString(2, reclamation.getTagId() != null ? reclamation.getTagId().toString() : null);
            pstmt.setTimestamp(3, new Timestamp(reclamation.getDateReclamation().getTime()));
            pstmt.setInt(4, reclamation.getRate());
            pstmt.setString(5, reclamation.getTitle());
            pstmt.setString(6, reclamation.getDescription());
            pstmt.setString(7, reclamation.getStatut().getDisplayName());
            pstmt.setString(8, reclamation.getId().toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating reclamation: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteReclamation(UUID id) {
        String sql = "DELETE FROM reclamations WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting reclamation: " + e.getMessage());
            return false;
        }
    }
}