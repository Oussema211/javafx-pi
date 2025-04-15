package com.example.reclamation.service;

import com.example.auth.utils.MyDatabase;
import com.example.reclamation.model.Reclamation;
import com.example.reclamation.model.Status;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReclamationService {
    private final Connection conn;

    public ReclamationService() {
        conn = MyDatabase.getInstance().getCnx();
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS reclamations (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "user_id VARCHAR(36) NOT NULL, " +
                    "tag_id VARCHAR(36), " +
                    "date_reclamation DATETIME NOT NULL, " +
                    "rate INT NOT NULL, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "description VARCHAR(255) NOT NULL, " +
                    "statut VARCHAR(255) NOT NULL, " + // Still stored as VARCHAR in DB
                    "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (tag_id) REFERENCES tag(id) ON DELETE SET NULL)";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing reclamations table: " + e.getMessage());
        }
    }

    public boolean addReclamation(UUID userId, UUID tagId, int rate, String title, String description, Status statut) {
        UUID id = UUID.randomUUID();
        Date dateReclamation = new Date();
        String sql = "INSERT INTO reclamations (id, user_id, tag_id, date_reclamation, rate, title, description, statut) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.setString(2, userId.toString());
            pstmt.setString(3, tagId != null ? tagId.toString() : null);
            pstmt.setTimestamp(4, new Timestamp(dateReclamation.getTime()));
            pstmt.setInt(5, rate);
            pstmt.setString(6, title);
            pstmt.setString(7, description);
            pstmt.setString(8, statut.getDisplayName());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("FOREIGN KEY")) {
                System.err.println("Foreign key error: User ID " + userId + " does not exist.");
            } else {
                System.err.println("Error adding reclamation: " + e.getMessage());
            }
            return false;
        }
    }
    // Read: Get reclamation by ID
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
                        Status.fromString(rs.getString("statut")) // Convert DB string to enum
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching reclamation by ID: " + e.getMessage());
        }
        return null;
    }

    // Read: Get all reclamations
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
                        Status.fromString(rs.getString("statut")) // Convert DB string to enum
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all reclamations: " + e.getMessage());
        }
        return reclamations;
    }

    // Update: Update an existing reclamation
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
            pstmt.setString(7, reclamation.getStatut().getDisplayName()); // Store enum's display name
            pstmt.setString(8, reclamation.getId().toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating reclamation: " + e.getMessage());
            return false;
        }
    }

    // Delete: Delete a reclamation by ID
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