package com.example.cart.service;

import com.example.produit.model.Produit;
import com.example.produit.service.ProduitDAO;
import com.example.utils.DatabaseConnection;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {

    /* ------------ TOP personnelle ---------------- */
    public static List<String> getTopProductIds(String userId, int limit) {
        List<String> ids = new ArrayList<>();

        String sql = """
            SELECT cp.produit_id, SUM(cp.quantite) AS total
            FROM commandes_historiques ch
            JOIN commande_produit cp ON ch.id = cp.commande_id
            WHERE ch.utilisateur = ?
            GROUP BY cp.produit_id
            ORDER BY total DESC
            LIMIT ?
        """;

        try (Connection conn = new DatabaseConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.setInt(2, limit);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getString("produit_id"));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    /* ----------- Recos par co-occurrence (personnalisées) ---------- */
    public static List<Produit> getSmartRecommendations(String userId, int limit) {

        List<String> topUser = getTopProductIds(userId, 5);
        if (topUser.isEmpty()) return List.of();

        String in = topUser.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = """
           SELECT pc.prod_b, SUM(pc.score) AS totalScore
           FROM   produit_cooccur pc
           WHERE  pc.prod_a IN (""" + in + ") " +
                "AND   pc.prod_b NOT IN (" + in + ") " +
                "GROUP BY pc.prod_b ORDER BY totalScore DESC LIMIT ?";

        try (Connection conn = new DatabaseConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int i = 1;
            for (String id : topUser) ps.setString(i++, id);   // IN
            for (String id : topUser) ps.setString(i++, id);   // NOT IN
            ps.setInt(i, limit);

            ResultSet rs = ps.executeQuery();
            List<Produit> recos = new ArrayList<>();
            while (rs.next()) {
                recos.add(ProduitDAO.getProduitById(rs.getString("prod_b")));
            }
            return recos;

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return List.of();
    }

    /* ----------- Recos générales (globale, sans user) ---------- */
    public static List<Produit> getMostFrequentRecommendations(int limit) {
        List<Produit> produits = new ArrayList<>();

        String sql = """
            SELECT prod_b, SUM(score) AS totalScore
            FROM produit_cooccur
            GROUP BY prod_b
            ORDER BY totalScore DESC
            LIMIT ?
        """;

        try (Connection conn = new DatabaseConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Produit produit = ProduitDAO.getProduitById(rs.getString("prod_b"));
                if (produit != null) {
                    produits.add(produit);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return produits;
    }
}
