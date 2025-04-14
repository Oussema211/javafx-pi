package com.example.produit.service;

import com.example.produit.model.Categorie;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CategorieDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/pidevv";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static List<Categorie> getAllCategories() {
        List<Categorie> categories = new ArrayList<>();
        String query = "SELECT * FROM categorie";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Categorie categorie = mapResultSetToCategorie(rs);
                categories.add(categorie);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public static Categorie getCategoryById(UUID id) {
        String query = "SELECT * FROM categorie WHERE id = ?";

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
        String query = "INSERT INTO categorie (id, parent_id, nom, description) VALUES (?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, categorie.getId().toString());
            pstmt.setString(2, categorie.getParentId() != null ? categorie.getParentId().toString() : null);
            pstmt.setString(3, categorie.getNom());
            pstmt.setString(4, categorie.getDescription());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateCategory(Categorie categorie) {
        String query = "UPDATE categorie SET parent_id = ?, nom = ?, description = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, categorie.getParentId() != null ? categorie.getParentId().toString() : null);
            pstmt.setString(2, categorie.getNom());
            pstmt.setString(3, categorie.getDescription());
            pstmt.setString(4, categorie.getId().toString());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
        }
    }

    private static Categorie mapResultSetToCategorie(ResultSet rs) throws SQLException {
        Categorie categorie = new Categorie();
        categorie.setId(UUID.fromString(rs.getString("id")));

        String parentId = rs.getString("parent_id");
        if (parentId != null) {
            categorie.setParentId(UUID.fromString(parentId));
        }

        categorie.setNom(rs.getString("nom"));
        categorie.setDescription(rs.getString("description"));
        return categorie;
    }

    public static List<Categorie> getChildCategories(UUID parentId) {
        List<Categorie> categories = new ArrayList<>();
        String query = "SELECT * FROM categorie WHERE parent_id " + (parentId == null ? "IS NULL" : "= ?");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            if (parentId != null) {
                pstmt.setString(1, parentId.toString());
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                categories.add(mapResultSetToCategorie(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public static List<Categorie> getRootCategories() {
        return getChildCategories(null);
    }
}
