package com.example.reclamation.service;

import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.reclamation.model.Tag;

public class TagService {
    private final Connection conn;

    public TagService() {
        conn = MyDatabase.getInstance().getCnx();
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS tag (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL UNIQUE)";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing tag table: " + e.getMessage());
        }
    }

    // Create: Add a new tag
    public boolean addTag(String name) {
        UUID id = UUID.randomUUID();
        String sql = "INSERT INTO tag (id, name) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.setString(2, name);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.err.println("Tag with name '" + name + "' already exists.");
                return false;
            }
            System.err.println("Error adding tag: " + e.getMessage());
            return false;
        }
    }

    // Read: Get tag by ID
    public Tag getTagById(UUID id) {
        String sql = "SELECT * FROM tag WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Tag(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("name")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching tag by ID: " + e.getMessage());
        }
        return null;
    }

    // Read: Get tag by name
    public Tag getTagByName(String name) {
        String sql = "SELECT * FROM tag WHERE name = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Tag(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("name")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching tag by name: " + e.getMessage());
        }
        return null;
    }

    // Read: Get all tags
    public List<Tag> getAllTags() {
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT * FROM tag";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tags.add(new Tag(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all tags: " + e.getMessage());
        }
        return tags;
    }

    // Update: Update an existing tag
    public boolean updateTag(Tag tag) {
        String sql = "UPDATE tag SET name = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tag.getName());
            pstmt.setString(2, tag.getId().toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                System.err.println("Tag name '" + tag.getName() + "' already exists.");
                return false;
            }
            System.err.println("Error updating tag: " + e.getMessage());
            return false;
        }
    }

    // Delete: Delete a tag by ID
    public boolean deleteTag(UUID id) {
        String sql = "DELETE FROM tag WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting tag: " + e.getMessage());
            return false;
        }
    }
}