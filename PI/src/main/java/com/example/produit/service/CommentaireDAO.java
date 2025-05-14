package com.example.produit.service;

import com.example.produit.model.Commentaire;
import com.example.produit.model.Produit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommentaireDAO {

    private static final String URL = "jdbc:mysql://localhost:3306/PIDES";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static {
        createCommentaireTable();
    }

    private static void createCommentaireTable() {
        String query = """
            CREATE TABLE IF NOT EXISTS commentaire (
                id VARCHAR(36) PRIMARY KEY,
                auteur VARCHAR(255),
                contenu TEXT,
                note FLOAT,
                date_creation DATETIME,
                produit_id VARCHAR(36),
                user_id VARCHAR(36)
            )
        """;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            System.out.println("Commentaire table checked/created successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create commentaire table: " + e.getMessage());
        }
    }

    public static void saveCommentaire(Commentaire commentaire) {
        String query = "INSERT INTO commentaire (id, auteur, contenu, note, date_creation, produit_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, commentaire.getId().toString()); // Include the id
            pstmt.setString(2, commentaire.getAuteur());
            pstmt.setString(3, commentaire.getContenu());
            pstmt.setFloat(4, commentaire.getNote());
            pstmt.setTimestamp(5, commentaire.getDateCreation() != null ? Timestamp.valueOf(commentaire.getDateCreation()) : null);
            pstmt.setString(6, commentaire.getProduit() != null ? commentaire.getProduit().getId().toString() : null);
            pstmt.setString(7, commentaire.getUser() != null ? commentaire.getUser().getId().toString() : null);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("SQL Error in saveCommentaire: " + e.getMessage());
        }
    }

    public static List<Commentaire> getCommentairesByProduit(Produit produit) {
        List<Commentaire> commentaires = new ArrayList<>();
        String query = "SELECT * FROM commentaire WHERE produit_id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, produit.getId().toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Commentaire commentaire = new Commentaire();
                    commentaire.setId(UUID.fromString(rs.getString("id")));
                    commentaire.setAuteur(rs.getString("auteur"));
                    commentaire.setContenu(rs.getString("contenu"));
                    commentaire.setNote(rs.getFloat("note"));
                    commentaire.setDateCreation(rs.getTimestamp("date_creation") != null ? rs.getTimestamp("date_creation").toLocalDateTime() : null);
                    commentaire.setProduit(produit);
                    // Note: User is not set as User class is not provided
                    commentaires.add(commentaire);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to retrieve commentaires: " + e.getMessage());
        }
        return commentaires;
    }
}