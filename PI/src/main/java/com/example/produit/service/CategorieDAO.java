package com.example.produit.service;

import com.example.produit.model.Categorie;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CategorieDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/pidev";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static List<Categorie> getAllCategories() {
        List<Categorie> categories = new ArrayList<>();
        String query = "SELECT id, nom, description, date_creation FROM categorie";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                categories.add(mapResultSetToCategorie(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch categories: " + e.getMessage());
        }
        return categories;
    }

    public static Categorie getCategoryById(UUID id) {
        String query = "SELECT id, nom, description, date_creation FROM categorie WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToCategorie(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveCategory(Categorie categorie) {
        String query = "INSERT INTO categorie (id, nom, description, date_creation) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            if (categorie.getId() == null) {
                categorie.setId(UUID.randomUUID());
            }
            pstmt.setString(1, categorie.getId().toString());
            pstmt.setString(2, categorie.getNom());
            pstmt.setString(3, categorie.getDescription());
            pstmt.setTimestamp(4, categorie.getDateCreation() != null ?
                    Timestamp.valueOf(categorie.getDateCreation()) : null);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save category: " + e.getMessage());
        }
    }

    public static void updateCategory(Categorie categorie) {
        String query = "UPDATE categorie SET nom = ?, description = ?, date_creation = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, categorie.getNom());
            pstmt.setString(2, categorie.getDescription());
            pstmt.setTimestamp(3, categorie.getDateCreation() != null ?
                    Timestamp.valueOf(categorie.getDateCreation()) : null);
            pstmt.setString(4, categorie.getId().toString());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update category: " + e.getMessage());
        }
    }

    public static void deleteCategory(UUID id) {
        String query = "DELETE FROM categorie WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, id.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to delete category: " + e.getMessage());
        }
    }

    private static Categorie mapResultSetToCategorie(ResultSet rs) throws SQLException {
        Categorie categorie = new Categorie();
        String idStr = rs.getString("id");
        categorie.setId(idStr != null ? UUID.fromString(idStr) : null);
        categorie.setNom(rs.getString("nom"));
        categorie.setDescription(rs.getString("description"));
        categorie.setDateCreation(rs.getTimestamp("date_creation") != null ?
                rs.getTimestamp("date_creation").toLocalDateTime() : null);
        return categorie;
    }
}