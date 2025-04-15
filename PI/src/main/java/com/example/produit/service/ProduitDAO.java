package com.example.produit.service;

import com.example.produit.model.Produit;
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
        String query = "SELECT * FROM produit";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Produit product = new Produit();
                product.setId(UUID.fromString(rs.getString("id")));
                product.setNom(rs.getString("nom"));
                product.setDescription(rs.getString("description"));
                product.setPrixUnitaire(rs.getFloat("prix_unitaire"));
                product.setQuantite(rs.getInt("quantite"));
                product.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());
                product.setImageName(rs.getString("image_name"));

                UUID categoryId = (UUID) rs.getObject("categorie_id");
                product.setCategory(CategorieDAO.getCategoryById(categoryId));

                products.add(product);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }

    public static void saveProduct(Produit product) {
        String query = "INSERT INTO produit (id, categorie_id, nom, description, prix_unitaire, quantite, date_creation, image_name) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, product.getId().toString());
            pstmt.setObject(2, product.getCategory() != null ? product.getCategory().getId() : null, java.sql.Types.OTHER); // UUID for categorie_id
            pstmt.setString(3, product.getNom());
            pstmt.setString(4, product.getDescription());
            pstmt.setFloat(5, product.getPrixUnitaire());
            pstmt.setInt(6, product.getQuantite());
            pstmt.setTimestamp(7, Timestamp.valueOf(product.getDateCreation()));
            pstmt.setString(8, product.getImageName()); // Save image_name

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateProduct(Produit product) {
        String query = "UPDATE produit SET categorie_id = ?, nom = ?, description = ?, "
                + "prix_unitaire = ?, quantite = ?, image_name = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setObject(1, product.getCategory() != null ? product.getCategory().getId() : null, java.sql.Types.OTHER);
            pstmt.setString(2, product.getNom());
            pstmt.setString(3, product.getDescription());
            pstmt.setFloat(4, product.getPrixUnitaire());
            pstmt.setInt(5, product.getQuantite());
            pstmt.setString(6, product.getImageName());
            pstmt.setString(7, product.getId().toString());

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