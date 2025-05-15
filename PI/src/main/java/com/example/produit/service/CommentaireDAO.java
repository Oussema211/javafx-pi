package com.example.produit.service;

import com.example.auth.model.User;
import com.example.auth.utils.SessionManager;
import com.example.produit.model.Commentaire;
import com.example.produit.model.Produit;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

public class CommentaireDAO {
    private static final Logger LOGGER = Logger.getLogger(CommentaireDAO.class.getName());
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
                user_id VARCHAR(36),
                FOREIGN KEY (produit_id) REFERENCES produit(id),
                FOREIGN KEY (user_id) REFERENCES user(id)
            )
        """;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            LOGGER.info("Commentaire table checked/created successfully at " + LocalDateTime.now());
        } catch (SQLException e) {
            LOGGER.severe("Failed to create commentaire table: " + e.getMessage());
            throw new RuntimeException("Failed to create commentaire table", e);
        }
    }

    public static void saveCommentaire(Commentaire commentaire) {
        if (commentaire == null || commentaire.getProduit() == null || commentaire.getProduit().getId() == null ||
                commentaire.getContenu() == null || commentaire.getNote() == null) {
            LOGGER.warning("Cannot save commentaire: null or incomplete data");
            return;
        }
        String query = """
            INSERT INTO commentaire (id, auteur, contenu, note, date_creation, produit_id, user_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, commentaire.getId().toString());
            pstmt.setString(2, commentaire.getAuteur() != null ? commentaire.getAuteur() : "Anonymous");
            pstmt.setString(3, commentaire.getContenu());
            pstmt.setFloat(4, commentaire.getNote());
            pstmt.setTimestamp(5, commentaire.getDateCreation() != null ? Timestamp.valueOf(commentaire.getDateCreation()) : null);
            pstmt.setString(6, commentaire.getProduit().getId().toString());
            SessionManager sessionManager = SessionManager.getInstance();
            User currentUser = sessionManager != null ? sessionManager.getLoggedInUser() : null;
            pstmt.setString(7, currentUser != null && currentUser.getId() != null ? currentUser.getId().toString() : null);
            pstmt.executeUpdate();
            LOGGER.info("Saved comment for product " + commentaire.getProduit().getId());
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in saveCommentaire: " + e.getMessage());
            throw new RuntimeException("Failed to save commentaire", e);
        }
    }

    public static void updateCommentaire(Commentaire commentaire) {
        if (commentaire == null || commentaire.getId() == null || commentaire.getContenu() == null || commentaire.getNote() == null) {
            LOGGER.warning("Cannot update commentaire: null or incomplete data");
            return;
        }
        String query = """
            UPDATE commentaire
            SET auteur = ?, contenu = ?, note = ?, date_creation = ?, produit_id = ?, user_id = ?
            WHERE id = ?
        """;
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, commentaire.getAuteur() != null ? commentaire.getAuteur() : "Anonymous");
            pstmt.setString(2, commentaire.getContenu());
            pstmt.setFloat(3, commentaire.getNote());
            pstmt.setTimestamp(4, commentaire.getDateCreation() != null ? Timestamp.valueOf(commentaire.getDateCreation()) : null);
            pstmt.setString(5, commentaire.getProduit() != null && commentaire.getProduit().getId() != null ? commentaire.getProduit().getId().toString() : null);
            SessionManager sessionManager = SessionManager.getInstance();
            User currentUser = sessionManager != null ? sessionManager.getLoggedInUser() : null;
            pstmt.setString(6, currentUser != null && currentUser.getId() != null ? currentUser.getId().toString() : null);
            pstmt.setString(7, commentaire.getId().toString());
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                LOGGER.warning("No comment found with ID: " + commentaire.getId());
                throw new RuntimeException("No comment found with ID: " + commentaire.getId());
            }
            LOGGER.info("Updated comment with ID: " + commentaire.getId());
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in updateCommentaire: " + e.getMessage());
            throw new RuntimeException("Failed to update commentaire", e);
        }
    }

    public static List<Commentaire> getCommentairesByProduit(Produit produit) {
        if (produit == null || produit.getId() == null) {
            LOGGER.warning("Cannot retrieve comments for null product or product ID");
            return new ArrayList<>();
        }
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
                    String userId = rs.getString("user_id");
                    if (userId != null) {
                        try {
                            SessionManager sessionManager = SessionManager.getInstance();
                            User currentUser = sessionManager != null ? sessionManager.getLoggedInUser() : null;
                            commentaire.setUser(currentUser);
                        } catch (Exception e) {
                            LOGGER.warning("Failed to fetch user with ID " + userId + ": " + e.getMessage());
                            commentaire.setUser(null);
                        }
                    } else {
                        commentaire.setUser(null);
                    }
                    commentaires.add(commentaire);
                }
            }
            LOGGER.info("Retrieved " + commentaires.size() + " comments for product " + produit.getId());
        } catch (SQLException e) {
            LOGGER.severe("Failed to retrieve commentaires for product " + produit.getId() + ": " + e.getMessage());
            return new ArrayList<>();
        }
        return commentaires;
    }
}