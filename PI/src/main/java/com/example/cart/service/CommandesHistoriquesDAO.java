package com.example.cart.service;

import com.example.cart.model.OrderSummary;
import com.example.auth.utils.MyDatabase;
import com.example.cart.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CommandesHistoriquesDAO {

    // Method to create the table if it doesn't exist
    private static void createTableIfNotExists(Connection conn) throws SQLException {
        // Check if the table exists using database metadata
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet tables = metaData.getTables(null, null, "commandes_historiques", new String[]{"TABLE"});

        if (!tables.next()) {
            // Table doesn't exist, create it
            String createTableSQL = """
                CREATE TABLE commandes_historiques (
                    id VARCHAR(255) PRIMARY KEY,
                    utilisateur VARCHAR(255) NOT NULL,
                    date_achat VARCHAR(255) NOT NULL,
                    prix_total DOUBLE NOT NULL
                )
            """;

            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(createTableSQL);
                System.out.println("Table 'commandes_historiques' created successfully.");
            }
        }
    }

    // ✅ Charger toutes les commandes enregistrées
    public static List<OrderSummary> getAllCommandes() {
        List<OrderSummary> commandes = new ArrayList<>();

        try {
            DatabaseConnection databaseConnection = new DatabaseConnection();
            Connection conn = databaseConnection.getConnection();

            // Ensure the table exists before querying
            createTableIfNotExists(conn);

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

            // Ensure the table exists before inserting
            createTableIfNotExists(conn);

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

            // Ensure the table exists before deleting
            createTableIfNotExists(conn);

            Statement stmt = conn.createStatement();
            stmt.executeUpdate(sql);

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}