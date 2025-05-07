package com.example.produit.service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class FavoriteDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/pidevv";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static void addFavorite(UUID userId, UUID produitId) {
        String sql = "INSERT INTO user_favorites (user_id, produit_id) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            stmt.setString(2, produitId.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            // Ignore duplicate entry errors (already favorited)
            if (!e.getSQLState().equals("23000")) {
                e.printStackTrace();
            }
        }
    }

    public static void removeFavorite(UUID userId, UUID produitId) {
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
        String sql = "SELECT COUNT(*) FROM user_favorites WHERE user_id = ? AND produit_id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            stmt.setString(2, produitId.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}