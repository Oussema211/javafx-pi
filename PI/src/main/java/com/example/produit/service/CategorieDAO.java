package com.example.produit.service;

import com.example.produit.model.Categorie;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CategorieDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/PIDES";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final Logger LOGGER = Logger.getLogger(CategorieDAO.class.getName());

    public static List<Categorie> getAllCategories() {
        List<Categorie> categories = new ArrayList<>();
        String query = "SELECT c1.*, c2.nom AS parent_name " +
                "FROM categorie c1 " +
                "LEFT JOIN categorie c2 ON c1.parent_id = c2.id";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                try {
                    Categorie category = mapResultSetToCategorie(rs);
                    categories.add(category);
                } catch (SQLException e) {
                    LOGGER.severe("Error mapping category: " + e.getMessage());
                }
            }
            LOGGER.info("getAllCategories: Retrieved " + categories.size() + " categories");
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in getAllCategories: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve categories: " + e.getMessage());
        }
        return categories;
    }

    public static Categorie getCategoryById(int id) {
        String query = "SELECT c1.*, c2.nom AS parent_name " +
                "FROM categorie c1 " +
                "LEFT JOIN categorie c2 ON c1.parent_id = c2.id " +
                "WHERE c1.id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCategorie(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in getCategoryById: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve category: " + e.getMessage());
        }
        return null;
    }

    public static void saveCategory(Categorie category) {
        String query = "INSERT INTO categorie (nom, img_url, parent_id) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, category.getNom());
            pstmt.setString(2, category.getImgUrl());
            pstmt.setObject(3, category.getParent() != null ? category.getParent().getId() : null, Types.INTEGER);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        category.setId(generatedKeys.getInt(1));
                    }
                }
            }
            LOGGER.info("saveCategory: Inserted category with ID " + category.getId() + ", rows affected: " + rowsAffected);
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in saveCategory: " + e.getMessage());
            throw new RuntimeException("Failed to save category: " + e.getMessage());
        }
    }

    public static void updateCategory(Categorie category) {
        String query = "UPDATE categorie SET nom = ?, img_url = ?, parent_id = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, category.getNom());
            pstmt.setString(2, category.getImgUrl());
            pstmt.setObject(3, category.getParent() != null ? category.getParent().getId() : null, Types.INTEGER);
            pstmt.setInt(4, category.getId());

            int rowsAffected = pstmt.executeUpdate();
            LOGGER.info("updateCategory: Updated category with ID " + category.getId() + ", rows affected: " + rowsAffected);
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in updateCategory: " + e.getMessage());
            throw new RuntimeException("Failed to update category: " + e.getMessage());
        }
    }

    public static void deleteCategory(int id) {
        String query = "DELETE FROM categorie WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            int rowsAffected = pstmt.executeUpdate();
            LOGGER.info("deleteCategory: Deleted category with ID " + id + ", rows affected: " + rowsAffected);
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in deleteCategory: " + e.getMessage());
            throw new RuntimeException("Failed to delete category: " + e.getMessage());
        }
    }

    private static Categorie mapResultSetToCategorie(ResultSet rs) throws SQLException {
        Categorie category = new Categorie();
        category.setId(rs.getInt("id"));
        category.setNom(rs.getString("nom"));
        category.setImgUrl(rs.getString("img_url"));

        int parentId = rs.getInt("parent_id");
        if (!rs.wasNull()) {
            Categorie parent = new Categorie();
            parent.setId(parentId);
            String parentName = rs.getString("parent_name");
            if (parentName != null) {
                parent.setNom(parentName);
            }
            category.setParent(parent);
        }
        return category;
    }
}