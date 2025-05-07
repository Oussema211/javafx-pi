package com.example.produit.service;

import com.example.produit.model.Produit;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProduitDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/pidevv";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static List<Produit> getAllProducts() {
        List<Produit> products = new ArrayList<>();
        String query = "SELECT id, nom, description, prix_unitaire, quantite, date_creation, image_name, categorie_id FROM produit";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                try {
                    Produit product = new Produit();
                    product.setId(UUID.fromString(rs.getString("id")));
                    product.setNom(rs.getString("nom"));
                    product.setDescription(rs.getString("description"));
                    product.setPrixUnitaire(rs.getFloat("prix_unitaire"));
                    product.setQuantite(rs.getInt("quantite"));
                    product.setDateCreation(rs.getTimestamp("date_creation") != null ? rs.getTimestamp("date_creation").toLocalDateTime() : null);
                    product.setImageName(rs.getString("image_name"));

                    String categoryIdStr = rs.getString("categorie_id");
                    if (categoryIdStr != null && !categoryIdStr.trim().isEmpty()) {
                        try {
                            UUID categoryId = UUID.fromString(categoryIdStr);
                            product.setCategory(CategorieDAO.getCategoryById(categoryId));
                        } catch (IllegalArgumentException e) {
                            System.err.println("Invalid UUID format for categorie_id: " + categoryIdStr);
                            product.setCategory(null);
                        }
                    } else {
                        product.setCategory(null);
                    }

                    products.add(product);
                } catch (Exception e) {
                    System.err.println("Error processing product row: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            System.out.println("getAllProducts: Retrieved " + products.size() + " products");
        } catch (SQLException e) {
            System.err.println("SQL Error in getAllProducts: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve products: " + e.getMessage());
        }
        return products;
    }

    public static Produit getProduitById(String id) {
        String query = "SELECT id, nom, description, prix_unitaire, quantite, " +
                "date_creation, image_name, categorie_id " +
                "FROM produit WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Produit p = new Produit();
                    p.setId(java.util.UUID.fromString(rs.getString("id")));
                    p.setNom(rs.getString("nom"));
                    p.setDescription(rs.getString("description"));
                    p.setPrixUnitaire(rs.getFloat("prix_unitaire"));
                    p.setQuantite(rs.getInt("quantite"));
                    p.setDateCreation(rs.getTimestamp("date_creation") != null
                            ? rs.getTimestamp("date_creation").toLocalDateTime()
                            : null);
                    p.setImageName(rs.getString("image_name"));

                    String catId = rs.getString("categorie_id");
                    if (catId != null && !catId.isBlank()) {
                        p.setCategory(CategorieDAO.getCategoryById(java.util.UUID.fromString(catId)));
                    }
                    return p;
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getProduitById: " + e.getMessage());
        }
        return null;
    }

    public static void saveProduct(Produit product) {
        String query = "INSERT INTO produit (id, categorie_id, nom, description, prix_unitaire, quantite, date_creation, image_name) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, product.getId().toString());
            pstmt.setString(2, product.getCategory() != null ? product.getCategory().getId().toString() : null);
            pstmt.setString(3, product.getNom() != null ? product.getNom() : "");
            pstmt.setString(4, product.getDescription() != null ? product.getDescription() : "");
            pstmt.setFloat(5, product.getPrixUnitaire());
            pstmt.setInt(6, product.getQuantite());
            pstmt.setTimestamp(7, product.getDateCreation() != null ? Timestamp.valueOf(product.getDateCreation()) : null);
            pstmt.setString(8, product.getImageName() != null ? product.getImageName() : null);

            int rowsAffected = pstmt.executeUpdate();
            System.out.println("saveProduct: Inserted product with ID " + product.getId() + ", rows affected: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("SQL Error in saveProduct: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to save product: " + e.getMessage());
        }
    }

    public static void updateProduct(Produit product) {
        String query = "UPDATE produit SET categorie_id = ?, nom = ?, description = ?, " +
                "prix_unitaire = ?, quantite = ?, image_name = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, product.getCategory() != null ? product.getCategory().getId().toString() : null);
            pstmt.setString(2, product.getNom() != null ? product.getNom() : "");
            pstmt.setString(3, product.getDescription() != null ? product.getDescription() : "");
            pstmt.setFloat(4, product.getPrixUnitaire());
            pstmt.setInt(5, product.getQuantite());
            pstmt.setString(6, product.getImageName() != null ? product.getImageName() : null);
            pstmt.setString(7, product.getId().toString());

            int rowsAffected = pstmt.executeUpdate();
            System.out.println("updateProduct: Updated product with ID " + product.getId() + ", rows affected: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("SQL Error in updateProduct: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update product: " + e.getMessage());
        }
    }

    public static void deleteProduct(UUID id) {
        String query = "DELETE FROM produit WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, id.toString());
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("deleteProduct: Deleted product with ID " + id + ", rows affected: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("SQL Error in deleteProduct: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete product: " + e.getMessage());
        }
    }

    public static void deleteProducts(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            System.out.println("deleteProducts: No IDs provided, skipping deletion");
            return;
        }

        String query = "DELETE FROM produit WHERE id IN (" + ids.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Set parameters for each ID
            for (int i = 0; i < ids.size(); i++) {
                pstmt.setString(i + 1, ids.get(i).toString());
            }

            int rowsAffected = pstmt.executeUpdate();
            System.out.println("deleteProducts: Deleted " + rowsAffected + " products with IDs " + ids);
        } catch (SQLException e) {
            System.err.println("SQL Error in deleteProducts: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete products: " + e.getMessage());
        }
    }
}