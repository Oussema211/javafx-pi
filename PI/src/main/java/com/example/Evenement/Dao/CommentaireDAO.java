package com.example.Evenement.Dao;

import com.example.Evenement.Model.CommentaireEvent;
import com.example.Evenement.Model.Evenement;
import com.example.auth.model.User;
import com.example.auth.utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommentaireDAO {
    private final Connection connection;

    public CommentaireDAO() {
        connection = MyDatabase.getInstance().getCnx();
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        try (Statement stmt = connection.createStatement()) {
            String createCommentTable = "CREATE TABLE IF NOT EXISTS commentaire (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT, " +
                    "contenu TEXT NOT NULL, " +
                    "date_creation DATETIME NOT NULL, " +
                    "user_id VARCHAR(36) NOT NULL, " +
                    "evenement_id INT NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (evenement_id) REFERENCES evenement(id) ON DELETE CASCADE" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            stmt.execute(createCommentTable);

            System.out.println("Table commentaire created successfully!");
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int create(CommentaireEvent comment) throws SQLException {
        String sql = "INSERT INTO commentaire (contenu, date_creation, user_id, evenement_id) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, comment.getContenu());
            stmt.setTimestamp(2, Timestamp.valueOf(comment.getDateCreation()));
            stmt.setString(3, comment.getUser().getId().toString());
            stmt.setInt(4, comment.getEvenement().getId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating comment failed, no rows affected.");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating comment failed, no ID obtained.");
                }
            }
        }
    }

    public List<CommentaireEvent> getAll() throws SQLException {
        List<CommentaireEvent> comments = new ArrayList<>();
        String sql = "SELECT * FROM commentaire ORDER BY date_creation DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                comments.add(mapResultSetToComment(rs));
            }
        }
        return comments;
    }

    public List<CommentaireEvent> getByEvent(int eventId) throws SQLException {
        List<CommentaireEvent> comments = new ArrayList<>();
        String sql = "SELECT c.*, u.email, u.nom, u.prenom FROM commentaire c " +
                "JOIN user u ON c.user_id = u.id " +
                "WHERE c.evenement_id = ? ORDER BY c.date_creation DESC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapResultSetToComment(rs));
                }
            }
        }
        return comments;
    }

    public CommentaireEvent getById(int id) throws SQLException {
        String sql = "SELECT c.*, u.email, u.nom, u.prenom FROM commentaire c " +
                "JOIN user u ON c.user_id = u.id " +
                "WHERE c.id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToComment(rs);
                }
            }
        }
        return null;
    }

    public void update(CommentaireEvent comment) throws SQLException {
        String sql = "UPDATE commentaire SET contenu = ?, date_creation = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, comment.getContenu());
            stmt.setTimestamp(2, Timestamp.valueOf(comment.getDateCreation()));
            stmt.setInt(3, comment.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Updating comment failed, no rows affected.");
            }
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM commentaire WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Deleting comment failed, no rows affected.");
            }
        }
    }

    private CommentaireEvent mapResultSetToComment(ResultSet rs) throws SQLException {
        CommentaireEvent comment = new CommentaireEvent();
        comment.setId(rs.getInt("id"));
        comment.setContenu(rs.getString("contenu"));
        comment.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());

        User user = new User(
                UUID.fromString(rs.getString("user_id")),
                rs.getString("email"),
                "[]", "", "", null, "", false,
                rs.getString("nom"),
                rs.getString("prenom"),
                ""
        );
        comment.setUser(user);

        Evenement event = new Evenement();
        event.setId(rs.getInt("evenement_id"));
        comment.setEvenement(event);

        return comment;
    }

    public boolean userOwnsComment(UUID userId, int commentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM commentaire WHERE id = ? AND user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            stmt.setString(2, userId.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
}