package com.example.produit.service;

import com.example.produit.model.Produit;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProduitDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/pidev";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static List<Produit> getAllProducts() {
        List<Produit> products = new ArrayList<>();
        String query = "SELECT id, nom, description, prix_unitaire, quantite, " +
                "created_at, categorie_id, user_id FROM produit";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                try {
                    Produit product = new Produit();
                    product.setId(UUID.fromString(rs.getString("id")));
                    product.setNom(rs.getString("nom"));

                    // Gestion des champs optionnels
                    String description = rs.getString("description");
                    if (description != null) {
                        product.setDescription(description);
                    }

                    BigDecimal prix = rs.getBigDecimal("prix_unitaire");
                    if (prix != null) {
                        product.setPrixUnitaire(prix.floatValue());
                    }

                    product.setQuantite(rs.getInt("quantite"));
                    product.setDateCreation(rs.getTimestamp("created_at").toLocalDateTime());

                    // Gestion de la catégorie (UUID)
                    String categorieId = rs.getString("categorie_id");
                    if (categorieId != null) {
                        product.setCategory(CategorieDAO.getCategoryById(UUID.fromString(categorieId)));
                    }

                    products.add(product);
                } catch (SQLException e) {
                    System.err.println("Erreur lors de la lecture d'un produit: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur de connexion ou requête SQL: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Produits récupérés: " + products.size());
        return products;
    }

    public static void saveProduct(Produit product) {
        String query = "INSERT INTO produit (id, user_id, nom, description, prix_unitaire, " +
                "quantite, created_at, categorie_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, product.getId().toString());
            pstmt.setString(2, product.getUserId().toString());
            pstmt.setString(3, product.getNom());

            if (product.getDescription() != null) {
                pstmt.setString(4, product.getDescription());
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }

            if (product.getPrixUnitaire() > 0) {
                pstmt.setBigDecimal(5, BigDecimal.valueOf(product.getPrixUnitaire()));
            } else {
                pstmt.setNull(5, Types.DECIMAL);
            }

            pstmt.setInt(6, product.getQuantite());
            pstmt.setTimestamp(7, Timestamp.valueOf(product.getDateCreation()));

            if (product.getCategory() != null) {
                pstmt.setString(8, product.getCategory().getId().toString());
            } else {
                pstmt.setNull(8, Types.CHAR);
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateProduct(Produit product) {
        String query = "UPDATE produit SET nom = ?, description = ?, prix_unitaire = ?, " +
                "quantite = ?, categorie_id = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, product.getNom());

            if (product.getDescription() != null) {
                pstmt.setString(2, product.getDescription());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }

            if (product.getPrixUnitaire() > 0) {
                pstmt.setBigDecimal(3, BigDecimal.valueOf(product.getPrixUnitaire()));
            } else {
                pstmt.setNull(3, Types.DECIMAL);
            }

            pstmt.setInt(4, product.getQuantite());

            if (product.getCategory() != null) {
                pstmt.setString(5, product.getCategory().getId().toString());
            } else {
                pstmt.setNull(5, Types.CHAR);
            }

            pstmt.setString(6, product.getId().toString());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteProduct(UUID id) {
        String query = "DELETE FROM produit WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, id.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}