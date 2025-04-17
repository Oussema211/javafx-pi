package com.example.Stock.service;

import com.example.Stock.Model.Entrepot;
import com.example.auth.utils.MyDatabase;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class EntrepotService {
    private final Connection connection;

    public EntrepotService() {
        this.connection = MyDatabase.getInstance().getCnx();
    }

    // CRUD Operations

    public boolean addEntrepot(Entrepot entrepot) {
        String query = "INSERT INTO entrepot (id, nom, adresse, ville, espace, latitude, longitude) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, entrepot.getId().toString());
            ps.setString(2, entrepot.getNom());
            ps.setString(3, entrepot.getAdresse());
            ps.setString(4, entrepot.getVille());
            ps.setDouble(5, entrepot.getEspace());

            if (entrepot.getLatitude() != null) {
                ps.setDouble(6, entrepot.getLatitude());
            } else {
                ps.setNull(6, Types.DOUBLE);
            }

            if (entrepot.getLongitude() != null) {
                ps.setDouble(7, entrepot.getLongitude());
            } else {
                ps.setNull(7, Types.DOUBLE);
            }

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de l'entrepôt: " + e.getMessage());
            return false;
        }
    }

    public List<Entrepot> getAllEntrepots() {
        List<Entrepot> entrepots = new ArrayList<>();
        String query = "SELECT * FROM entrepot";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Entrepot entrepot = mapResultSetToEntrepot(rs);
                loadStockIds(entrepot); // Charger les IDs des stocks associés
                entrepots.add(entrepot);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des entrepôts: " + e.getMessage());
        }

        return entrepots;
    }

    public Entrepot getEntrepotById(UUID id) {
        String query = "SELECT * FROM entrepot WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, id.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Entrepot entrepot = mapResultSetToEntrepot(rs);
                    loadStockIds(entrepot);
                    return entrepot;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'entrepôt ID " + id + ": " + e.getMessage());
        }

        return null;
    }

    public boolean updateEntrepot(Entrepot entrepot) {
        String query = "UPDATE entrepot SET nom = ?, adresse = ?, ville = ?, espace = ?, " +
                "latitude = ?, longitude = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, entrepot.getNom());
            ps.setString(2, entrepot.getAdresse());
            ps.setString(3, entrepot.getVille());
            ps.setDouble(4, entrepot.getEspace());

            if (entrepot.getLatitude() != null) {
                ps.setDouble(5, entrepot.getLatitude());
            } else {
                ps.setNull(5, Types.DOUBLE);
            }

            if (entrepot.getLongitude() != null) {
                ps.setDouble(6, entrepot.getLongitude());
            } else {
                ps.setNull(6, Types.DOUBLE);
            }

            ps.setString(7, entrepot.getId().toString());

            int affectedRows = ps.executeUpdate();

            // Mettre à jour les relations avec les stocks si nécessaire
            if (affectedRows > 0 && entrepot.getStockIds() != null) {
                updateStockRelations(entrepot);
            }

            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de l'entrepôt: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteEntrepot(UUID id) {
        // D'abord supprimer les relations avec les stocks
        String deleteRelationsQuery = "DELETE FROM stock_entrepot WHERE entrepot_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(deleteRelationsQuery)) {
            ps.setString(1, id.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression des relations stock-entrepôt: " + e.getMessage());
            return false;
        }

        // Puis supprimer l'entrepôt
        String deleteQuery = "DELETE FROM entrepot WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(deleteQuery)) {
            ps.setString(1, id.toString());
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de l'entrepôt: " + e.getMessage());
            return false;
        }
    }

    // Méthodes utilitaires

    private Entrepot mapResultSetToEntrepot(ResultSet rs) throws SQLException {
        Entrepot entrepot = new Entrepot();
        entrepot.setId(UUID.fromString(rs.getString("id")));
        entrepot.setNom(rs.getString("nom"));
        entrepot.setAdresse(rs.getString("adresse"));
        entrepot.setVille(rs.getString("ville"));
        entrepot.setEspace(rs.getDouble("espace"));

        // Gestion des champs optionnels
        try {
            entrepot.setLatitude(rs.getDouble("latitude"));
        } catch (SQLException e) {
            // Champ latitude non présent ou null
        }

        try {
            entrepot.setLongitude(rs.getDouble("longitude"));
        } catch (SQLException e) {
            // Champ longitude non présent ou null
        }

        return entrepot;
    }

    private void loadStockIds(Entrepot entrepot) throws SQLException {
        String query = "SELECT stock_id FROM stock_entrepot WHERE entrepot_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, entrepot.getId().toString());

            try (ResultSet rs = ps.executeQuery()) {
                Set<UUID> stockIds = new HashSet<>();
                while (rs.next()) {
                    stockIds.add(UUID.fromString(rs.getString("stock_id")));
                }
                entrepot.setStockIds(stockIds);
            }
        }
    }

    private void updateStockRelations(Entrepot entrepot) throws SQLException {
        // D'abord supprimer toutes les relations existantes
        String deleteQuery = "DELETE FROM stock_entrepot WHERE entrepot_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteQuery)) {
            ps.setString(1, entrepot.getId().toString());
            ps.executeUpdate();
        }

        // Puis ajouter les nouvelles relations
        if (!entrepot.getStockIds().isEmpty()) {
            String insertQuery = "INSERT INTO stock_entrepot (entrepot_id, stock_id) VALUES (?, ?)";

            try (PreparedStatement ps = connection.prepareStatement(insertQuery)) {
                for (UUID stockId : entrepot.getStockIds()) {
                    ps.setString(1, entrepot.getId().toString());
                    ps.setString(2, stockId.toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    // Méthodes de recherche/filtrage

    public List<Entrepot> searchEntrepots(String searchTerm) {
        String query = "SELECT * FROM entrepot WHERE LOWER(nom) LIKE ? OR LOWER(adresse) LIKE ? OR LOWER(ville) LIKE ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            String likeTerm = "%" + searchTerm.toLowerCase() + "%";
            ps.setString(1, likeTerm);
            ps.setString(2, likeTerm);
            ps.setString(3, likeTerm);

            try (ResultSet rs = ps.executeQuery()) {
                List<Entrepot> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToEntrepot(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche d'entrepôts: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Entrepot> getEntrepotsByVille(String ville) {
        String query = "SELECT * FROM entrepot WHERE ville = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, ville);

            try (ResultSet rs = ps.executeQuery()) {
                List<Entrepot> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToEntrepot(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par ville: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<Entrepot> getEntrepotsByEspaceRange(double min, double max) {
        String query = "SELECT * FROM entrepot WHERE espace BETWEEN ? AND ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setDouble(1, min);
            ps.setDouble(2, max);

            try (ResultSet rs = ps.executeQuery()) {
                List<Entrepot> results = new ArrayList<>();
                while (rs.next()) {
                    results.add(mapResultSetToEntrepot(rs));
                }
                return results;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche par espace: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    private Integer activeWarehousesCache = null;
    private long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 300000; // Cache valide 5 minutes

    public int getActiveWarehousesCount() {
        // Utiliser le cache si valide
        if (activeWarehousesCache != null &&
                System.currentTimeMillis() - cacheTimestamp < CACHE_DURATION_MS) {
            return activeWarehousesCache;
        }

        String query = "SELECT COUNT(DISTINCT e.id) FROM entrepot e " +
                "JOIN stock_entrepot se ON e.id = se.entrepot_id";

        try (PreparedStatement ps = connection.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                activeWarehousesCache = rs.getInt(1);
                cacheTimestamp = System.currentTimeMillis();
                return activeWarehousesCache;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du comptage des entrepôts actifs: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }
}