package com.example.purchasingbackend.dao;

import com.example.auth.model.User;
import com.example.auth.service.AuthService;
import com.example.produit.model.Produit;
import com.example.produit.service.ProduitDAO;
import com.example.purchasingbackend.model.CommandeFinalisee;
import com.example.purchasingbackend.model.ProduitCommandeTemp;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class CommandeFinaliseeDAO {

    private static final String URL = "jdbc:mysql://localhost:3306/pidevv";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static final AuthService userService = new AuthService();

    public static void saveCommande(CommandeFinalisee commande) {
        String insertCommandeSQL = "INSERT INTO commande_finalisee (id, user_id, date_achat, prix_total) VALUES (?, ?, ?, ?)";
        String insertLigneSQL = "INSERT INTO commande_produit (commande_id, produit_id, quantite) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement cmdStmt = conn.prepareStatement(insertCommandeSQL);
             PreparedStatement ligneStmt = conn.prepareStatement(insertLigneSQL)) {

            conn.setAutoCommit(false);

            cmdStmt.setString(1, commande.getId().toString());
            cmdStmt.setString(2, commande.getUtilisateur().getId().toString());
            cmdStmt.setTimestamp(3, Timestamp.valueOf(commande.getDate()));
            cmdStmt.setDouble(4, commande.getPrixTotal());
            cmdStmt.executeUpdate();

            for (ProduitCommandeTemp ligne : commande.getProduitsAvecQuantites()) {
                ligneStmt.setString(1, commande.getId().toString());
                ligneStmt.setString(2, ligne.getProduit().getId().toString());
                ligneStmt.setInt(3, ligne.getQuantite());
                ligneStmt.addBatch();
            }

            ligneStmt.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<CommandeFinalisee> getAllCommandes() {
        List<CommandeFinalisee> commandes = new ArrayList<>();
        String sql = "SELECT * FROM commande_finalisee";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                User user = userService.getUserById(UUID.fromString(rs.getString("user_id")));
                LocalDateTime date = rs.getTimestamp("date_achat").toLocalDateTime();
                double prix = rs.getDouble("prix_total");

                List<ProduitCommandeTemp> lignes = getLignesCommande(id, conn);

                commandes.add(new CommandeFinalisee(id, user, lignes, date, prix));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return commandes;
    }

    private static List<ProduitCommandeTemp> getLignesCommande(UUID commandeId, Connection conn) throws SQLException {
        List<ProduitCommandeTemp> lignes = new ArrayList<>();
        String sql = "SELECT produit_id, quantite FROM commande_produit WHERE commande_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, commandeId.toString());

            try (ResultSet rs = stmt.executeQuery()) {
                List<Produit> allProducts = ProduitDAO.getAllProducts();

                while (rs.next()) {
                    String prodId = rs.getString("produit_id");
                    int quantite = rs.getInt("quantite");

                    allProducts.stream()
                            .filter(p -> p.getId().toString().equals(prodId))
                            .findFirst()
                            .ifPresent(p -> lignes.add(new ProduitCommandeTemp(p, quantite)));
                }
            }
        }

        return lignes;
    }

    // ✅ MODIFIER commande
    public static void updateCommande(CommandeFinalisee commande) {
        String updateSQL = "UPDATE commande_finalisee SET user_id = ?, date_achat = ?, prix_total = ? WHERE id = ?";
        String deleteLignesSQL = "DELETE FROM commande_produit WHERE commande_id = ?";
        String insertLigneSQL = "INSERT INTO commande_produit (commande_id, produit_id, quantite) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
             PreparedStatement deleteLignesStmt = conn.prepareStatement(deleteLignesSQL);
             PreparedStatement insertLigneStmt = conn.prepareStatement(insertLigneSQL)) {

            conn.setAutoCommit(false);

            // Modifier la commande
            updateStmt.setString(1, commande.getUtilisateur().getId().toString());
            updateStmt.setTimestamp(2, Timestamp.valueOf(commande.getDate()));
            updateStmt.setDouble(3, commande.getPrixTotal());
            updateStmt.setString(4, commande.getId().toString());
            updateStmt.executeUpdate();

            // Supprimer anciennes lignes
            deleteLignesStmt.setString(1, commande.getId().toString());
            deleteLignesStmt.executeUpdate();

            // Réinsérer nouvelles lignes
            for (ProduitCommandeTemp ligne : commande.getProduitsAvecQuantites()) {
                insertLigneStmt.setString(1, commande.getId().toString());
                insertLigneStmt.setString(2, ligne.getProduit().getId().toString());
                insertLigneStmt.setInt(3, ligne.getQuantite());
                insertLigneStmt.addBatch();
            }

            insertLigneStmt.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ SUPPRIMER commande
    public static void deleteCommande(UUID id) {
        String deleteLignesSQL = "DELETE FROM commande_produit WHERE commande_id = ?";
        String deleteCommandeSQL = "DELETE FROM commande_finalisee WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement deleteLignesStmt = conn.prepareStatement(deleteLignesSQL);
             PreparedStatement deleteCommandeStmt = conn.prepareStatement(deleteCommandeSQL)) {

            conn.setAutoCommit(false);

            deleteLignesStmt.setString(1, id.toString());
            deleteLignesStmt.executeUpdate();

            deleteCommandeStmt.setString(1, id.toString());
            deleteCommandeStmt.executeUpdate();

            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
