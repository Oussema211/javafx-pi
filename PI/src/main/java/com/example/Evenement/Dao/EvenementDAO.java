package com.example.Evenement.Dao;

import com.example.Evenement.Model.Evenement;
import com.example.Evenement.Model.Region;
import com.example.Evenement.Model.TypeEvenement;
import com.example.Evenement.Model.StatutEvenement;
import com.example.auth.utils.MyDatabase;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EvenementDAO {
    private final Connection connection;

    public EvenementDAO() {
        this.connection = MyDatabase.getInstance().getCnx();
        if (this.connection == null) {
            throw new RuntimeException("La connexion à la base de données n'est pas disponible");
        }
    }

    public int create(Evenement event) throws SQLException {
        String sql = "INSERT INTO evenement (titre, description, type, statut, date_debut, date_fin, photo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, event.getTitre());
            stmt.setString(2, event.getDescription());
            stmt.setString(3, event.getType().name());
            stmt.setString(4, event.getStatut().name());
            stmt.setTimestamp(5, Timestamp.valueOf(event.getDateDebut()));
            stmt.setTimestamp(6, Timestamp.valueOf(event.getDateFin()));
            stmt.setString(7, event.getPhotoPath());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("La création de l'événement a échoué, aucune ligne affectée.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("La création de l'événement a échoué, aucun ID obtenu.");
                }
            }
        }
    }

    public void linkRegionsToEvent(int eventId, List<Region> regions) throws SQLException {
        if (regions == null || regions.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO evenement_region (evenement_id, region_id) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            for (Region region : regions) {
                stmt.setInt(1, eventId);
                stmt.setInt(2, region.getId());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    public List<Evenement> getAll() throws SQLException {
        List<Evenement> events = new ArrayList<>();
        String sql = "SELECT * FROM evenement ORDER BY date_debut DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                events.add(mapResultSetToEvenement(rs));
            }
        }
        return events;
    }

    public Evenement getById(int id) throws SQLException {
        String sql = "SELECT * FROM evenement WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToEvenement(rs);
                }
            }
        }
        return null;
    }

    public void update(Evenement event) throws SQLException {
        String sql = "UPDATE evenement SET titre = ?, description = ?, type = ?, " +
                "statut = ?, date_debut = ?, date_fin = ?, photo = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, event.getTitre());
            stmt.setString(2, event.getDescription());
            stmt.setString(3, event.getType().name());
            stmt.setString(4, event.getStatut().name());
            stmt.setTimestamp(5, Timestamp.valueOf(event.getDateDebut()));
            stmt.setTimestamp(6, Timestamp.valueOf(event.getDateFin()));
            stmt.setString(7, event.getPhotoPath());
            stmt.setInt(8, event.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("La mise à jour de l'événement a échoué, aucune ligne affectée.");
            }

            updateEventRegions(event);
        }
    }

    public void delete(int id) throws SQLException {
        // Désactiver temporairement les contraintes de clé étrangère
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS=0");
        }

        try {
            // Supprimer d'abord les associations avec les régions
            String deleteRegionsSql = "DELETE FROM evenement_region WHERE evenement_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteRegionsSql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }

            // Puis supprimer l'événement
            String deleteEventSql = "DELETE FROM evenement WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(deleteEventSql)) {
                stmt.setInt(1, id);
                int affectedRows = stmt.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("La suppression de l'événement a échoué, aucune ligne affectée.");
                }
            }
        } finally {
            // Réactiver les contraintes de clé étrangère
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS=1");
            }
        }
    }

    public List<Region> getRegionsForEvent(int eventId) throws SQLException {
        List<Region> regions = new ArrayList<>();
        String sql = "SELECT r.* FROM region r " +
                "JOIN evenement_region er ON r.id = er.region_id " +
                "WHERE er.evenement_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Region region = new Region();
                    region.setId(rs.getInt("id"));
                    region.setNom(rs.getString("nom"));
                    region.setVille(rs.getString("ville"));
                    region.setDescription(rs.getString("description"));
                    regions.add(region);
                }
            }
        }
        return regions;
    }

    private Evenement mapResultSetToEvenement(ResultSet rs) throws SQLException {
        Evenement event = new Evenement();
        event.setId(rs.getInt("id"));
        event.setTitre(rs.getString("titre"));
        event.setDescription(rs.getString("description"));
        event.setType(TypeEvenement.valueOf(rs.getString("type")));
        event.setStatut(StatutEvenement.valueOf(rs.getString("statut")));
        event.setDateDebut(rs.getTimestamp("date_debut").toLocalDateTime());
        event.setDateFin(rs.getTimestamp("date_fin").toLocalDateTime());
        event.setPhotoPath(rs.getString("photo"));

        // Charger les régions associées
        event.getRegions().addAll(getRegionsForEvent(event.getId()));

        return event;
    }

    private void updateEventRegions(Evenement event) throws SQLException {
        // Supprimer les anciennes associations
        String deleteSql = "DELETE FROM evenement_region WHERE evenement_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
            stmt.setInt(1, event.getId());
            stmt.executeUpdate();
        }

        // Ajouter les nouvelles associations si elles existent
        if (event.getRegions() != null && !event.getRegions().isEmpty()) {
            linkRegionsToEvent(event.getId(), event.getRegions());
        }
    }
}