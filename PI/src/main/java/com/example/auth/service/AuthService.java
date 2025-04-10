package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.utils.EmailUtil;
import com.example.auth.utils.MyDatabase;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;

public class AuthService {
    private final Connection conn;

    public AuthService() {
        conn = MyDatabase.getInstance().getCnx();
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS user (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "email VARCHAR(100) NOT NULL UNIQUE, " +
                    "roles TEXT NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "travail VARCHAR(100), " +
                    "date_inscri DATE NOT NULL, " +
                    "photo_url TEXT, " +
                    "is_verified BOOLEAN NOT NULL, " +
                    "nom VARCHAR(50) NOT NULL, " +
                    "prenom VARCHAR(50) NOT NULL, " +
                    "num_tel VARCHAR(20))";
            stmt.execute(sql);

            // Create reset_token table for password reset
            sql = "CREATE TABLE IF NOT EXISTS reset_token (" +
                    "token VARCHAR(36) PRIMARY KEY, " +
                    "user_id VARCHAR(36) NOT NULL, " +
                    "expiry_date TIMESTAMP NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES user(id))";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public boolean signup(String email, String password, String travail, String photoUrl,
                          String nom, String prenom, String numTel, List<String> roles) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        UUID id = UUID.randomUUID();
        // Convert roles list to JSON string
        User tempUser = new User(id, email, "[]", password, travail, new Date(), photoUrl, false, nom, prenom, numTel);
        tempUser.setRoles(roles);
        String rolesJson = tempUser.getRolesAsJson();
        Date dateInscri = new Date();
        boolean isVerified = false;

        String sql = "INSERT INTO user (id, email, roles, password, travail, date_inscri, photo_url, " +
                "is_verified, nom, prenom, num_tel) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.setString(2, email);
            pstmt.setString(3, rolesJson);
            pstmt.setString(4, hashedPassword);
            pstmt.setString(5, travail);
            pstmt.setDate(6, new java.sql.Date(dateInscri.getTime()));
            pstmt.setString(7, photoUrl);
            pstmt.setBoolean(8, isVerified);
            pstmt.setString(9, nom);
            pstmt.setString(10, prenom);
            pstmt.setString(11, numTel);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                return false;
            }
            System.err.println("Error during signup: " + e.getMessage());
            return false;
        }
    }

    public User login(String email, String password) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                if (BCrypt.checkpw(password, hashedPassword)) {
                    Date dateInscri = null;
                    try {
                        dateInscri = rs.getDate("date_inscri");
                    } catch (SQLException e) {
                        System.err.println("Invalid date_inscri for user " + email + ", using current date: " + e.getMessage());
                        dateInscri = new Date();
                    }
                    return new User(
                            UUID.fromString(rs.getString("id")),
                            rs.getString("email"),
                            rs.getString("roles"),
                            hashedPassword,
                            rs.getString("travail"),
                            dateInscri,
                            rs.getString("photo_url"),
                            rs.getBoolean("is_verified"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("num_tel")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error during login: " + e.getMessage());
        }
        return null;
    }

    public User getUserById(UUID id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Date dateInscri = null;
                try {
                    dateInscri = rs.getDate("date_inscri");
                } catch (SQLException e) {
                    System.err.println("Invalid date_inscri for user with ID " + id + ", using current date: " + e.getMessage());
                    dateInscri = new Date();
                }
                return new User(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("roles"),
                        rs.getString("password"),
                        rs.getString("travail"),
                        dateInscri,
                        rs.getString("photo_url"),
                        rs.getBoolean("is_verified"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("num_tel")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user by ID: " + e.getMessage());
        }
        return null;
    }

    // Read: Get all users
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Date dateInscri = null;
                try {
                    dateInscri = rs.getDate("date_inscri");
                } catch (SQLException e) {
                    System.err.println("Invalid date_inscri for user with ID " + rs.getString("id") + ", using current date: " + e.getMessage());
                    dateInscri = new Date();
                }
                users.add(new User(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("roles"),
                        rs.getString("password"),
                        rs.getString("travail"),
                        dateInscri,
                        rs.getString("photo_url"),
                        rs.getBoolean("is_verified"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("num_tel")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all users: " + e.getMessage());
        }
        return users;
    }

    // Update: Update an existing user
    public boolean updateUser(User user) {
        String sql = "UPDATE user SET email = ?, roles = ?, password = ?, travail = ?, " +
                "photo_url = ?, is_verified = ?, nom = ?, prenom = ?, num_tel = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getRolesAsJson());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getTravail());
            pstmt.setString(5, user.getPhotoUrl());
            pstmt.setBoolean(6, user.isVerified());
            pstmt.setString(7, user.getNom());
            pstmt.setString(8, user.getPrenom());
            pstmt.setString(9, user.getNumTel());
            pstmt.setString(10, user.getId().toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            return false;
        }
    }

    // Delete: Delete a user by ID
    public boolean deleteUser(UUID id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }

    // Corrected updatePasswordByEmail method
    public boolean updatePasswordByEmail(String email, String newPassword) {
        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        String sql = "UPDATE user SET password = ? WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, email);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    // Generate and send reset token
    public boolean requestPasswordReset(String email, String numTel) {
        // Find user by email and numTel
        String sql = "SELECT * FROM user WHERE email = ? AND num_tel = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, numTel);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                UUID userId = UUID.fromString(rs.getString("id"));
                // Generate a unique token
                String token = UUID.randomUUID().toString();
                // Set expiration to 1 hour from now
                Timestamp expiryDate = new Timestamp(System.currentTimeMillis() + 60 * 60 * 1000);
    
                // Store the token in the database
                String insertSql = "INSERT INTO reset_token (token, user_id, expiry_date) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, token);
                    insertStmt.setString(2, userId.toString());
                    insertStmt.setTimestamp(3, expiryDate);
                    insertStmt.executeUpdate();
                }
    
                // Send email with reset link
                String resetLink = "http://localhost:8080/reset-password?token=" + token;  // Corrected port
                String emailBody = "<h2>Password Reset Request</h2>" +
                        "<p>Click the link below to reset your password:</p>" +
                        "<a href=\"" + resetLink + "\">Reset Password</a>" +
                        "<p>This link will expire in 1 hour.</p>";
                EmailUtil.sendEmail(email, "Password Reset Request", emailBody);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error requesting password reset: " + e.getMessage());
        } catch (MessagingException e) {
            System.err.println("Error sending reset email: " + e.getMessage());
        }
        return false;
    }
    
    // Verify token and reset password
    public boolean resetPasswordWithToken(String token, String newPassword) {
        // Find the token in the database
        String sql = "SELECT * FROM reset_token WHERE token = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Timestamp expiryDate = rs.getTimestamp("expiry_date");
                if (expiryDate.before(new Timestamp(System.currentTimeMillis()))) {
                    // Token has expired
                    deleteToken(token);
                    return false;
                }

                String userId = rs.getString("user_id");
                // Update the user's password
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                String updateSql = "UPDATE user SET password = ? WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setString(2, userId);
                    updateStmt.executeUpdate();
                }

                // Delete the token after use
                deleteToken(token);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error resetting password with token: " + e.getMessage());
        }
        return false;
    }

    // Helper method to delete a token
    private void deleteToken(String token) {
        String sql = "DELETE FROM reset_token WHERE token = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting token: " + e.getMessage());
        }
    }
}