package com.example.cart.service;

import com.example.cart.model.ProduitCommande;
import com.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitCommandeDAO {

    public static List<ProduitCommande> getProduitsParCommande(String commandeId) {
        List<ProduitCommande> produits = new ArrayList<>();

        try {
            DatabaseConnection db = new DatabaseConnection();
            Connection conn = db.getConnection();

            String sql = "SELECT nom_produit, quantite, prix_unitaire FROM produits_commandes WHERE commande_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, commandeId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String nom = rs.getString("nom_produit");
                int quantite = rs.getInt("quantite");
                double prix = rs.getDouble("prix_unitaire");

                produits.add(new ProduitCommande(nom, quantite, prix));
            }

            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return produits;
    }
}
