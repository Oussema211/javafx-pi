package com.example.Evenement.Dao;

import com.example.Evenement.Model.Inscription;
import com.example.Evenement.Model.Evenement;
import com.example.auth.model.User;
import com.example.auth.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InscriptionDAO {
    private final Connection connection;

    public InscriptionDAO() {
        connection = MyDatabase.getInstance().getCnx();
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        try (Statement stmt = connection.createStatement()) {
            String createInscriptionTable = "CREATE TABLE IF NOT EXISTS inscription (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "nom VARCHAR(100) NOT NULL, " +
                    "prenom VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(255) NOT NULL, " +
                    "num_tel VARCHAR(20) NOT NULL, " +
                    "travail VARCHAR(100), " +
                    "user_id VARCHAR(36), " +
                    "evenement_id INT NOT NULL, " +
                    "date_inscription DATETIME NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL, " +
                    "FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE, " +
                    "UNIQUE (email, evenement_id), " +
                    "UNIQUE (user_id, evenement_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            stmt.execute(createInscriptionTable);

            System.out.println("Table inscription created successfully!");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int create(Inscription inscription) throws SQLException {
        String sql = "INSERT INTO inscription (nom, prenom, email, num_tel, travail, user_id, evenement_id, date_inscription) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, inscription.getNom());
            stmt.setString(2, inscription.getPrenom());
            stmt.setString(3, inscription.getEmail());
            stmt.setString(4, inscription.getNumTel());
            stmt.setString(5, inscription.getTravail());

            if (inscription.getUser() != null) {
                stmt.setString(6, inscription.getUser().getId().toString());
            } else {
                stmt.setNull(6, Types.VARCHAR);
            }

            stmt.setInt(7, inscription.getEvenement().getId());
            stmt.setTimestamp(8, Timestamp.valueOf(inscription.getDateInscription()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating inscription failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating inscription failed, no ID obtained.");
                }
            }
        }
    }

    public List<Inscription> getAll() throws SQLException {
        List<Inscription> inscriptions = new ArrayList<>();
        String sql = "SELECT i.*, u.email, u.nom, u.prenom FROM inscription i " +
                "LEFT JOIN user u ON i.user_id = u.id " +
                "ORDER BY i.date_inscription DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                inscriptions.add(mapResultSetToInscription(rs));
            }
        }
        return inscriptions;
    }

    public List<Inscription> getByEvent(int eventId) throws SQLException {
        List<Inscription> inscriptions = new ArrayList<>();
        String sql = "SELECT i.*, u.email, u.nom, u.prenom FROM inscription i " +
                "LEFT JOIN user u ON i.user_id = u.id " +
                "WHERE i.evenement_id = ? ORDER BY i.date_inscription DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    inscriptions.add(mapResultSetToInscription(rs));
                }
            }
        }
        return inscriptions;
    }

    public List<Inscription> getByUser(UUID userId) throws SQLException {
        List<Inscription> inscriptions = new ArrayList<>();
        String sql = "SELECT i.*, e.titre FROM inscription i " +
                "JOIN evenement e ON i.evenement_id = e.id " +
                "WHERE i.user_id = ? ORDER BY i.date_inscription DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    inscriptions.add(mapResultSetToInscription(rs));
                }
            }
        }
        return inscriptions;
    }

    public Inscription getById(int id) throws SQLException {
        String sql = "SELECT i.*, u.email, u.nom, u.prenom, e.titre FROM inscription i " +
                "LEFT JOIN user u ON i.user_id = u.id " +
                "JOIN evenement e ON i.evenement_id = e.id " +
                "WHERE i.id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToInscription(rs);
                }
            }
        }
        return null;
    }

    public void update(Inscription inscription) throws SQLException {
        String sql = "UPDATE inscription SET nom = ?, prenom = ?, email = ?, " +
                "num_tel = ?, travail = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, inscription.getNom());
            stmt.setString(2, inscription.getPrenom());
            stmt.setString(3, inscription.getEmail());
            stmt.setString(4, inscription.getNumTel());
            stmt.setString(5, inscription.getTravail());
            stmt.setInt(6, inscription.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating inscription failed, no rows affected.");
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM inscription WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting inscription failed, no rows affected.");
            }
        }
    }

    private Inscription mapResultSetToInscription(ResultSet rs) throws SQLException {
        Inscription inscription = new Inscription();
        inscription.setId(rs.getInt("id"));
        inscription.setNom(rs.getString("nom"));
        inscription.setPrenom(rs.getString("prenom"));
        inscription.setEmail(rs.getString("email"));
        inscription.setNumTel(rs.getString("num_tel"));
        inscription.setTravail(rs.getString("travail"));
        inscription.setDateInscription(rs.getTimestamp("date_inscription").toLocalDateTime());

        String userId = rs.getString("user_id");
        if (userId != null) {
            User user = new User(
                UUID.fromString(rs.getString("user_id")),         // UUID id
                rs.getString("email"),                            // String email
                "[]",                                             // String rolesJson
                "",                                               // String password
                "",                                               // String travail
                null,                                             // Date dateInscri
                "",                                               // String photoUrl
                false,                                            // boolean isVerified
                "",                                               // String verificationToken
                rs.getString("nom"),                              // String nom
                rs.getString("prenom"),                           // String prenom
                ""                                                // String numTel
            );
            
            inscription.setUser(user);
        }

        Evenement event = new Evenement();
        event.setId(rs.getInt("evenement_id"));
        if (hasColumn(rs, "titre")) {
            event.setTitre(rs.getString("titre"));
        }
        inscription.setEvenement(event);

        return inscription;
    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            if (meta.getColumnName(i).equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isUserRegistered(UUID userId, int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inscription WHERE user_id = ? AND evenement_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            stmt.setInt(2, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public boolean isEmailRegistered(String email, int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inscription WHERE email = ? AND evenement_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    public void createSimple(String userId, int evenementId) throws SQLException {
        String sql = "INSERT INTO inscription (user_id, evenement_id, date_inscription) VALUES (?, ?, NOW())";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setInt(2, evenementId);
            stmt.executeUpdate();
        }
    }
}