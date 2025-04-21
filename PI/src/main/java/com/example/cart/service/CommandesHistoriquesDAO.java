package com.example.cart.service;

import com.example.cart.model.OrderSummary;
import com.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandesHistoriquesDAO {

    // ✅ Charger toutes les commandes enregistrées
    public static List<OrderSummary> getAllCommandes() {
        List<OrderSummary> commandes = new ArrayList<>();

        try {
            DatabaseConnection databaseConnection = new DatabaseConnection();
            Connection conn = databaseConnection.getConnection();

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM commandes_historiques");

            while (rs.next()) {
                String id = rs.getString("id");
                String userId = rs.getString("utilisateur");
                String dateAchat = rs.getString("date_achat");
                double prixTotal = rs.getDouble("prix_total");

                OrderSummary order = new OrderSummary(id, userId, dateAchat, prixTotal);
                commandes.add(order);
            }

            conn.close(); // Très important de fermer après usage
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return commandes;
    }

    // ✅ Sauvegarder une commande après paiement
    public static void saveCommande(OrderSummary order) {
        String sql = "INSERT INTO commandes_historiques (id, utilisateur, date_achat, prix_total) VALUES (?, ?, ?, ?)";

        try {
            DatabaseConnection databaseConnection = new DatabaseConnection();
            Connection conn = databaseConnection.getConnection();

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, order.getId());
            pstmt.setString(2, order.getUserId());
            pstmt.setString(3, order.getDateAchat());
            pstmt.setDouble(4, order.getPrixTotal());

            pstmt.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ✅ Supprimer toutes les commandes de la BDD
    public static void clearAllCommandes() {
        String sql = "DELETE FROM commandes_historiques";

        try {
            DatabaseConnection databaseConnection = new DatabaseConnection();
            Connection conn = databaseConnection.getConnection();

            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
