package com.example.reclamation.service;

import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.example.reclamation.model.MessageReclamation;

public class MessageReclamationService {
    private final Connection conn;

    public MessageReclamationService() {
        conn = MyDatabase.getInstance().getCnx();
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS message_reclamation (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "user_id VARCHAR(36) NOT NULL, " +
                    "reclamation_id VARCHAR(36) DEFAULT NULL, " +
                    "contenu VARCHAR(255) NOT NULL, " +
                    "date_message DATETIME NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES user(id), " +
                    "FOREIGN KEY (reclamation_id) REFERENCES reclamations(id))";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing message_reclamation table: " + e.getMessage());
            e.printStackTrace(); // For debugging
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

    // Read: Get all messages
    public List<MessageReclamation> getAllMessages() {
        List<MessageReclamation> messages = new ArrayList<>();
        String sql = "SELECT HEX(id) AS id, HEX(user_id) AS user_id, HEX(reclamation_id) AS reclamation_id, " +
                     "contenu, date_message FROM message_reclamation";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                UUID reclamationId = rs.getString("reclamation_id") != null ?
                                    UUID.fromString(rs.getString("reclamation_id").replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5")) :
                                    null;
                messages.add(new MessageReclamation(
                        UUID.fromString(rs.getString("id").replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5")),
                        UUID.fromString(rs.getString("user_id").replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5")),
                        reclamationId,
                        rs.getString("contenu"),
                        rs.getTimestamp("date_message")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all messages: " + e.getMessage());
        }
        return messages;
    }

    // Update: Update an existing message
    public boolean updateMessage(MessageReclamation message) {
        String sql = "UPDATE message_reclamation SET user_id = UNHEX(REPLACE(?, '-', '')), " +
                     "reclamation_id = UNHEX(REPLACE(?, '-', '')), contenu = ?, date_message = ? " +
                     "WHERE id = UNHEX(REPLACE(?, '-', ''))";
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
        String sql = "DELETE FROM message_reclamation WHERE id = UNHEX(REPLACE(?, '-', ''))";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting message: " + e.getMessage());
            return false;
        }
    }
}