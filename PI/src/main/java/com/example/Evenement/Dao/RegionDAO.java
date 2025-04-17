package com.example.Evenement.Dao;

import com.example.Evenement.Model.Region;
import com.example.auth.utils.MyDatabase;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegionDAO {
    private Connection connection;

    public RegionDAO() {
        connection = MyDatabase.getInstance().getCnx();
        try (Statement stmt = connection.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS region (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "nom VARCHAR(100) NOT NULL, " +
                    "ville VARCHAR(100) NOT NULL, " +
                    "description TEXT)";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de la table region : " + e.getMessage());
            e.printStackTrace();
        }
    }


    public boolean addRegion(Region region) {
        String query = "INSERT INTO region(nom, ville, description) VALUES(?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, region.getNom());
            pst.setString(2, region.getVille());
            pst.setString(3, region.getDescription());
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout de la région: " + e.getMessage());
            return false;
        }
    }

    public List<Region> getAllRegions() {
        List<Region> regions = new ArrayList<>();
        String query = "SELECT * FROM region";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Region region = new Region();
                region.setId(rs.getInt("id"));
                region.setNom(rs.getString("nom"));
                region.setVille(rs.getString("ville"));
                region.setDescription(rs.getString("description"));
                regions.add(region);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des régions: " + e.getMessage());
        }

        return regions;
    }

    public Region getRegionById(int id) {
        String query = "SELECT * FROM region WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Region region = new Region();
                    region.setId(rs.getInt("id"));
                    region.setNom(rs.getString("nom"));
                    region.setVille(rs.getString("ville"));
                    region.setDescription(rs.getString("description"));
                    return region;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la région: " + e.getMessage());
        }
        return null;
    }

    public boolean updateRegion(Region region) {
        String query = "UPDATE region SET nom = ?, ville = ?, description = ? WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, region.getNom());
            pst.setString(2, region.getVille());
            pst.setString(3, region.getDescription());
            pst.setInt(4, region.getId());
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de la région: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteRegion(int id) {
        String query = "DELETE FROM region WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la région: " + e.getMessage());
            return false;
        }
    }

    public boolean titreExists(String titre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM region WHERE LOWER(nom) = LOWER(?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, titre);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public boolean titreExistsForOtherEvent(String titre, int currentRegionId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM region WHERE LOWER(nom) = LOWER(?) AND id != ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, titre);
            stmt.setInt(2, currentRegionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}
