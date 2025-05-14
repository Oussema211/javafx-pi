package com.example.reclamation.service;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.reclamation.model.MessageReclamation;
import com.example.reclamation.model.Reclamation;

import com.example.auth.utils.MyDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MessageReclamationService {
    private final Connection conn;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final AuthService authService;
    private final ReclamationService reclamationService;
    public MessageReclamationService() {
        this.conn = MyDatabase.getInstance().getCnx();
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newHttpClient();
        this.authService = new AuthService();
        this.reclamationService = new ReclamationService();
        initializeTable();
    }

    private void initializeTable() {
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS message_reclamation (" +
                         "id VARCHAR(255) PRIMARY KEY, " +
                         "user_id VARCHAR(255) NOT NULL, " +
                         "reclamation_id VARCHAR(255) DEFAULT NULL, " +
                         "contenu VARCHAR(255) NOT NULL, " +
                         "date_message DATETIME NOT NULL, " +
                         "FOREIGN KEY (user_id) REFERENCES user(id), " +
                         "FOREIGN KEY (reclamation_id) REFERENCES reclamations(id))";
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Error initializing message_reclamation table", e);
        }
    }

    public String generateAutoReply(UUID userId, UUID reclamationId) throws Exception {
        String selectSql = "SELECT title, description FROM reclamations WHERE id = ?";
        String reclamationText;
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, reclamationId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Reclamation not found: " + reclamationId);
                }
                String title = rs.getString("title");
                String desc  = rs.getString("description");
                reclamationText = (title + " " + desc).trim();
                if (reclamationText.isEmpty()) {
                    throw new IllegalStateException("Reclamation text is empty");
                }
            }
        }

        // 2) Call the Flask API
        String requestJson = objectMapper.writeValueAsString(
            Map.of("reclamation", reclamationText)
        );
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://192.168.1.193:5000/predict"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build();

        HttpResponse<String> response = httpClient.send(request,
            HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Flask API error: HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String autoReply = root.path("response").asText(null);
        if (autoReply == null || autoReply.isBlank()) {
            throw new RuntimeException("No 'response' field in API reply");
        }

        // 3) Persist the new message
        boolean saved = addMessage(userId, reclamationId, autoReply);
        if (!saved) {
            throw new SQLException("Failed to save auto‐reply message");
        }

        // 4) Update reclamation status to RESOLUE
        String updateSql = "UPDATE reclamations SET statut = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, "resolved");  // or use an enum constant if you have one
            ps.setString(2, reclamationId.toString());
            if (ps.executeUpdate() != 1) {
                throw new SQLException("Failed to update reclamation status");
            }
        }

        return autoReply;
    }
    public String retrainModel() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://192.168.1.193:5000/retrain"))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());

        if (response.statusCode() == 200) {
            return root.path("message").asText("Model retrained successfully");
        } else {
            String error = root.path("error").asText("Retraining failed");
            String details = root.path("details").asText("");
            throw new RuntimeException(error + (details.isEmpty() ? "" : ": " + details));
        }
    }
     public String addReclamationToCsv(UUID reclamationId) throws Exception {
        // 1) Load the reclamation
        Reclamation rec = reclamationService.getReclamationById(reclamationId);
        if (rec == null) {
            throw new IllegalArgumentException("Reclamation not found: " + reclamationId);
        }

        // 2) Find the first admin reply
        List<MessageReclamation> messages = getMessagesForReclamation(reclamationId);
        MessageReclamation adminMsg = null;
        for (MessageReclamation msg : messages) {
            User u = authService.getUserById(msg.getUserId());
            if (u != null && u.hasRole("ROLE_ADMIN")) {
                adminMsg = msg;
                break;
            }
        }
        if (adminMsg == null) {
            throw new IllegalStateException("No admin response found for reclamation: " + reclamationId);
        }

        // 3) Build JSON payload
        String reclamationText = (rec.getTitle() + " " + rec.getDescription()).trim();
        String responseText = adminMsg.getContenu();
        Map<String, String> payload = Map.of(
            "reclamation", reclamationText,
            "response", responseText
        );
        String requestJson = objectMapper.writeValueAsString(payload);

        // 4) Call Flask API
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("http://192.168.1.193:5000/add_reclamations"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestJson))
            .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(resp.body());

        // 5) Handle response
        if (resp.statusCode() == 200) {
            if (root.has("added") && root.get("added").asBoolean()) {
                return "Reclamation added to CSV successfully";
            } else if (root.has("skipped")) {
                return "Reclamation skipped: " + root.get("skipped").toString();
            } else {
                throw new RuntimeException("Unexpected API response: " + resp.body());
            }
        } else {
            String error = root.path("error").asText("Retraining failed");
            String details = root.path("details").asText("");
            throw new RuntimeException(error + (details.isEmpty() ? "" : ": " + details));
        }
    }

    public boolean addMessage(UUID userId, UUID reclamationId, String contenu) {
        UUID id = UUID.randomUUID();
        Date dateMessage = new Date();
        String sql = "INSERT INTO message_reclamation (id, user_id, reclamation_id, contenu, date_message) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.setString(2, userId.toString());
            pstmt.setString(3, reclamationId != null ? reclamationId.toString() : null);
            pstmt.setString(4, contenu);
            pstmt.setTimestamp(5, new Timestamp(dateMessage.getTime()));
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding message: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Read: Get message by ID
    public MessageReclamation getMessageById(UUID id) {
        String sql = "SELECT HEX(id) AS id, HEX(user_id) AS user_id, HEX(reclamation_id) AS reclamation_id, " +
                     "contenu, date_message FROM message_reclamation WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                UUID reclamationId = rs.getString("reclamation_id") != null ?
                                    UUID.fromString(rs.getString("reclamation_id").replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5")) :
                                    null;
                return new MessageReclamation(
                        UUID.fromString(rs.getString("id").replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5")),
                        UUID.fromString(rs.getString("user_id").replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5")),
                        reclamationId,
                        rs.getString("contenu"),
                        rs.getTimestamp("date_message")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching message by ID: " + e.getMessage());
        }
        return null;
    }

    public List<MessageReclamation> getAllMessages() {
        List<MessageReclamation> messages = new ArrayList<>();
        String sql = "SELECT id, user_id, reclamation_id, contenu, date_message FROM message_reclamation";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String idStr = rs.getString("id");
                String userIdStr = rs.getString("user_id");
                String reclamationIdStr = rs.getString("reclamation_id");
    
                // Debug raw values to ensure they're in the correct format
                System.out.println("Raw id: " + idStr);
                System.out.println("Raw user_id: " + userIdStr);
                System.out.println("Raw reclamation_id: " + reclamationIdStr);
    
                // Parse UUIDs directly from the VARCHAR strings
                UUID id = UUID.fromString(idStr);
                UUID userId = UUID.fromString(userIdStr);
                UUID reclamationId = reclamationIdStr != null ? UUID.fromString(reclamationIdStr) : null;
    
                messages.add(new MessageReclamation(
                        id,
                        userId,
                        reclamationId,
                        rs.getString("contenu"),
                        rs.getTimestamp("date_message")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all messages: " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Error parsing UUID: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }

    // Update: Update an existing message
    public boolean updateMessage(MessageReclamation message) {
        String sql = "UPDATE message_reclamation SET user_id = ?, " +
                     "reclamation_id = ?, contenu = ?, date_message = ? " +
                     "WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, message.getUserId().toString());
            pstmt.setString(2, message.getReclamationId() != null ? message.getReclamationId().toString() : null);
            pstmt.setString(3, message.getContenu());
            pstmt.setTimestamp(4, new Timestamp(message.getDateMessage().getTime()));
            pstmt.setString(5, message.getId().toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating message: " + e.getMessage());
            return false;
        }
    }

    // Delete: Delete a message by ID
    public boolean deleteMessage(UUID id) {
        String sql = "DELETE FROM message_reclamation WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting message: " + e.getMessage());
            return false;
        }
    }
    public List<MessageReclamation> getMessagesForReclamation(UUID reclamationId) {
        List<MessageReclamation> messages = new ArrayList<>();
        String sql = "SELECT id, user_id, reclamation_id, contenu, date_message " +
                     "FROM message_reclamation WHERE reclamation_id = ? ORDER BY date_message";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, reclamationId.toString());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                messages.add(new MessageReclamation(
                    UUID.fromString(rs.getString("id")),
                    UUID.fromString(rs.getString("user_id")),
                    UUID.fromString(rs.getString("reclamation_id")),
                    rs.getString("contenu"),
                    rs.getTimestamp("date_message")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching messages for reclamation: " + e.getMessage());
            e.printStackTrace();
        }
        return messages;
    }
}