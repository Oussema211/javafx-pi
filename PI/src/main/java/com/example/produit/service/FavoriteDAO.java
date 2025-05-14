package com.example.produit.service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FavoriteDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/PIDES";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static boolean tableChecked = false; // Flag to avoid repeated checks

    private static void createTableIfNotExists() {
        // Skip if already checked
        if (tableChecked) {
            return;
        }

        String createTableSQL = """
            CREATE TABLE user_favorites (
                user_id VARCHAR(36) NOT NULL,
                produit_id VARCHAR(36) NOT NULL,
                PRIMARY KEY (user_id, produit_id)
            )
        """;

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD)) {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(null, null, "user_favorites", new String[]{"TABLE"})) {
                if (!rs.next()) {
                    // Table does not exist, create it
                    try (Statement stmt = conn.createStatement()) {
                        stmt.executeUpdate(createTableSQL);
                        System.out.println("Table user_favorites created successfully.");
                    }
                } else {
                    System.out.println("Table user_favorites already exists.");
                }
            }
            tableChecked = true; // Mark as checked
        } catch (SQLException e) {
            System.err.println("Error creating table user_favorites: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void addFavorite(UUID userId, UUID produitId) {
        createTableIfNotExists(); // Ensure table exists before operation
        String sql = "INSERT IGNORE INTO user_favorites (user_id, produit_id) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            stmt.setString(2, produitId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Log other SQL exceptions if needed
            e.printStackTrace();
        }
    }

    public static void removeFavorite(UUID userId, UUID produitId) {
        createTableIfNotExists(); // Ensure table exists before operation
        String sql = "DELETE FROM user_favorites WHERE user_id = ? AND produit_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            stmt.setString(2, produitId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isFavorite(UUID userId, UUID produitId) {
        createTableIfNotExists(); // Ensure table exists before operation
        String sql = "SELECT COUNT(*) FROM user_favorites WHERE user_id = ? AND produit_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            stmt.setString(2, produitId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<UUID> getFavoritesByUser(UUID userId) {
        createTableIfNotExists(); // Ensure table exists before operation
        List<UUID> favorites = new ArrayList<>();
        String sql = "SELECT produit_id FROM user_favorites WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    favorites.add(UUID.fromString(rs.getString("produit_id")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return favorites;
    }
}