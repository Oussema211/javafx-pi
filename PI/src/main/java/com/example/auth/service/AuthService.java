package com.example.auth.service;

import com.example.auth.model.User;
import com.example.auth.utils.EmailUtil;
import com.example.auth.utils.MyDatabase;
import com.example.auth.utils.ResetLinkServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.mail.MessagingException;

public class AuthService {
    private final Connection conn;

    public AuthService() {
        conn = MyDatabase.getInstance().getCnx();
        if (conn == null) {
            System.err.println("Database connection is null. Cannot initialize tables.");
            return;
        }
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS user (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "email VARCHAR(100) NOT NULL UNIQUE, " +
                    "roles TEXT NOT NULL, " +
                    "password VARCHAR(255), " +
                    "travail VARCHAR(100), " +
                    "date_iscri DATE NOT NULL, " +
                    "photo_url TEXT, " +
                    "is_verified BOOLEAN NOT NULL, " +
                    "verification_token VARCHAR(36), " +
                    "nom VARCHAR(50) NOT NULL, " +
                    "prenom VARCHAR(50) NOT NULL, " +
                    "num_tel VARCHAR(20))";
            stmt.execute(sql);

            sql = "CREATE TABLE IF NOT EXISTS reset_token (" +
                    "token VARCHAR(36) PRIMARY KEY, " +
                    "user_id VARCHAR(36) NOT NULL, " +
                    "expiry_date TIMESTAMP NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES user(id))";
            stmt.execute(sql);

            sql = "SHOW COLUMNS FROM user LIKE 'verification_token'";
            ResultSet rs = stmt.executeQuery(sql);
            if (!rs.next()) {
                sql = "ALTER TABLE user ADD COLUMN verification_token VARCHAR(36)";
                stmt.execute(sql);
                System.out.println("Added verification_token column to user table");
            }

            sql = "ALTER TABLE user MODIFY COLUMN password VARCHAR(255)";
            stmt.execute(sql);
            System.out.println("Ensured password column allows NULL");
        } catch (SQLException e) {
            System.err.println("Error initializing database tables: " + e.getMessage());
        }
    }

    public User loginWithGmail() throws IOException, GeneralSecurityException {
        System.out.println("Attempting to authenticate with Google...");
        String email = GoogleAuthHelper.authenticate();
        if (email == null || email.isEmpty()) {
            System.err.println("Google authentication failed: No email returned");
            return null;
        }
        System.out.println("Google authentication returned email: " + email);
        
        User user = authenticate(email, null);
        if (user != null) {
            System.out.println("Existing user found: " + email);
            return user;
        }

        if (conn == null) {
            System.err.println("Cannot create user: Database connection is null");
            return null;
        }

        UUID id = UUID.randomUUID();
        List<String> roles = Arrays.asList("ROLE_USER");
        String rolesJson;
        try {
            rolesJson = new ObjectMapper().writeValueAsString(roles);
        } catch (Exception e) {
            System.err.println("Error serializing roles: " + e.getMessage());
            return null;
        }
        Date dateIscri = new Date();
        
        String sql = "INSERT INTO user (id, email, roles, password, travail, date_iscri, photo_url, " +
                     "is_verified, verification_token, nom, prenom, num_tel) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.setString(2, email);
            pstmt.setString(3, rolesJson);
            pstmt.setNull(4, Types.VARCHAR);
            pstmt.setNull(5, Types.VARCHAR);
            pstmt.setDate(6, new java.sql.Date(dateIscri.getTime()));
            pstmt.setNull(7, Types.VARCHAR);
            pstmt.setBoolean(8, true);
            pstmt.setNull(9, Types.VARCHAR);
            pstmt.setString(10, "Unknown");
            pstmt.setString(11, "Unknown");
            pstmt.setNull(12, Types.VARCHAR);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("New user created: " + email + ", rows affected: " + rowsAffected);
            return new User(id, email, rolesJson, null, null, dateIscri, null, true, null, "Unknown", "Unknown", null);
        } catch (SQLException e) {
            System.err.println("Error creating Gmail user: " + e.getMessage());
            return null;
        }
    }

    public boolean signup(String email, String password, String travail, String photoUrl,
                         String nom, String prenom, String numTel, List<String> roles, String verificationCode) {
        if (conn == null) {
            System.err.println("Cannot signup: Database connection is null");
            return false;
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt(12));
        UUID id = UUID.randomUUID();
        User tempUser = new User(id, email, "[]", password, travail, new Date(), photoUrl, false, verificationCode, nom, prenom, numTel);
        tempUser.setRoles(roles);
        String rolesJson = tempUser.getRolesAsJson();
        Date dateIscri = new Date();
        boolean isVerified = false;

        String sql = "INSERT INTO user (id, email, roles, password, travail, date_iscri, photo_url, " +
                "is_verified, verification_token, nom, prenom, num_tel) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.setString(2, email);
            pstmt.setString(3, rolesJson);
            pstmt.setString(4, hashedPassword);
            pstmt.setString(5, travail);
            pstmt.setDate(6, new java.sql.Date(dateIscri.getTime()));
            pstmt.setString(7, photoUrl);
            pstmt.setBoolean(8, isVerified);
            pstmt.setString(9, verificationCode);
            pstmt.setString(10, nom);
            pstmt.setString(11, prenom);
            pstmt.setString(12, numTel);
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

    public boolean verifyUser(String verificationCode, String email) {
        if (conn == null) {
            System.err.println("Cannot verify user: Database connection is null");
            return false;
        }

        String sql = "SELECT id FROM user WHERE verification_token = ? AND email = ? AND is_verified = false";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, verificationCode);
            pstmt.setString(2, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String userId = rs.getString("id");
                sql = "UPDATE user SET is_verified = true, verification_token = NULL WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(sql)) {
                    updateStmt.setString(1, userId);
                    updateStmt.executeUpdate();
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error verifying user: " + e.getMessage());
            return false;
        }
    }

    public String generateVerificationCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }

    public User authenticate(String email, String password) {
        if (conn == null) {
            System.err.println("Cannot authenticate: Database connection is null");
            return null;
        }

        String sql = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String storedPassword = rs.getString("password");
                // Handle Gmail login (null password) or invalid stored password
                if (password == null && storedPassword == null) {
                    System.out.println("Authenticated Gmail user: " + email);
                } else if (password != null && storedPassword != null && !storedPassword.isEmpty() && 
                           (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2y$"))) {
                    try {
                        // Convert $2y$ to $2a$ for compatibility with older jBCrypt versions
                        String checkPassword = storedPassword.startsWith("$2y$") 
                            ? "$2a$" + storedPassword.substring(4) 
                            : storedPassword;
                        if (!BCrypt.checkpw(password, checkPassword)) {
                            System.err.println("Password mismatch for user: " + email);
                            return null;
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("Invalid password hash for user " + email + ": " + storedPassword + ", error: " + e.getMessage());
                        return null;
                    }
                } else {
                    System.err.println("Authentication failed for user " + email + ": storedPassword=" + storedPassword + ", inputPasswordProvided=" + (password != null));
                    return null;
                }
                Date dateIscri = rs.getDate("date_iscri");
                if (dateIscri == null) {
                    System.err.println("Invalid date_iscri for user " + email + ", using current date");
                    dateIscri = new Date();
                }
                return new User(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("roles"),
                        storedPassword,
                        rs.getString("travail"),
                        dateIscri,
                        rs.getString("photo_url"),
                        rs.getBoolean("is_verified"),
                        rs.getString("verification_token"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("num_tel")
                );
            }
            System.err.println("No user found with email: " + email);
        } catch (SQLException e) {
            System.err.println("SQL error during authentication: " + e.getMessage());
        }
        return null;
    }

    public User login(String email, String password) {
        return authenticate(email, password);
    }

    public User getUserById(UUID id) {
        if (conn == null) {
            System.err.println("Cannot fetch user: Database connection is null");
            return null;
        }

        String sql = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Date dateIscri = rs.getDate("date_iscri");
                if (dateIscri == null) {
                    dateIscri = new Date();
                }
                return new User(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("roles"),
                        rs.getString("password"),
                        rs.getString("travail"),
                        dateIscri,
                        rs.getString("photo_url"),
                        rs.getBoolean("is_verified"),
                        rs.getString("verification_token"),
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

    public List<User> getAllUsers() {
        if (conn == null) {
            System.err.println("Cannot fetch users: Database connection is null");
            return new ArrayList<>();
        }

        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Date dateIscri = rs.getDate("date_iscri");
                if (dateIscri == null) {
                    dateIscri = new Date();
                }
                users.add(new User(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("email"),
                        rs.getString("roles"),
                        rs.getString("password"),
                        rs.getString("travail"),
                        dateIscri,
                        rs.getString("photo_url"),
                        rs.getBoolean("is_verified"),
                        rs.getString("verification_token"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("num_tel")
                ));
            }
            System.out.println("Fetched " + users.size() + " users");
        } catch (SQLException e) {
            System.err.println("Error fetching users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    public boolean updateUser(User user) {
        if (conn == null) {
            System.err.println("Cannot update user: Database connection is null");
            return false;
        }

        String sql = "UPDATE user SET email = ?, roles = ?, password = ?, travail = ?, " +
                "photo_url = ?, is_verified = ?, verification_token = ?, nom = ?, prenom = ?, num_tel = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getRolesAsJson());
            pstmt.setString(3, user.getPassword());
            pstmt.setString(4, user.getTravail());
            pstmt.setString(5, user.getPhotoUrl());
            pstmt.setBoolean(6, user.isVerified());
            pstmt.setString(7, user.getVerificationToken());
            pstmt.setString(8, user.getNom());
            pstmt.setString(9, user.getPrenom());
            pstmt.setString(10, user.getNumTel());
            pstmt.setString(11, user.getId().toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUserEmail(String oldEmail, String newEmail) {
        if (conn == null) {
            System.err.println("Cannot update email: Database connection is null");
            return false;
        }

        String checkSql = "SELECT COUNT(*) FROM user WHERE email = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, newEmail);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                System.err.println("Email already exists: " + newEmail);
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error checking email existence: " + e.getMessage());
            return false;
        }

        String sql = "UPDATE user SET email = ? WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newEmail);
            pstmt.setString(2, oldEmail);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating email: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUserPassword(String email, String newPassword) {
        if (conn == null) {
            System.err.println("Cannot update password: Database connection is null");
            return false;
        }

        String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        String sql = "UPDATE user SET password = ? WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, email);
            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(UUID id) {
        if (conn == null) {
            System.err.println("Cannot delete user: Database connection is null");
            return false;
        }

        String sql = "DELETE FROM reset_token WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting reset tokens for user: " + e.getMessage());
            return false;
        }

        sql = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }

    public boolean requestPasswordReset(String email, String numTel) {
        if (conn == null) {
            System.err.println("Cannot request password reset: Database connection is null");
            return false;
        }

        String sql = "SELECT * FROM user WHERE email = ? AND num_tel = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, numTel);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                UUID userId = UUID.fromString(rs.getString("id"));
                String token = UUID.randomUUID().toString();
                Timestamp expiryDate = new Timestamp(System.currentTimeMillis() + 60 * 60 * 1000);

                String insertSql = "INSERT INTO reset_token (token, user_id, expiry_date) VALUES (?, ?, ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, token);
                    insertStmt.setString(2, userId.toString());
                    insertStmt.setTimestamp(3, expiryDate);
                    insertStmt.executeUpdate();
                }

                if (!ResetLinkServer.startServer()) {
                    System.err.println("Failed to start reset server; password reset link may not work");
                    return false;
                }

                String resetLink = "http://localhost:8082/reset-password?token=" + token;
                String emailBody = "<h2>Password Reset Request</h2>" +
                        "<p>Click the link below to reset your password:</p>" +
                        "<a href=\"" + resetLink + "\">Reset Password</a>" +
                        "<p>This link will expire in 1 hour.</p>";
                EmailUtil.sendEmail(email, "Password Reset Request", emailBody);
                return true;
            }
        } catch (SQLException | MessagingException e) {
            System.err.println("Error requesting password reset: " + e.getMessage());
        }
        return false;
    }

    public boolean resetPasswordWithToken(String token, String newPassword) {
        if (conn == null) {
            System.err.println("Cannot reset password: Database connection is null");
            return false;
        }

        String sql = "SELECT * FROM reset_token WHERE token = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Timestamp expiryDate = rs.getTimestamp("expiry_date");
                if (expiryDate.before(new Timestamp(System.currentTimeMillis()))) {
                    deleteToken(token);
                    ResetLinkServer.stopServer();
                    return false;
                }

                String userId = rs.getString("user_id");
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                String updateSql = "UPDATE user SET password = ? WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, hashedPassword);
                    updateStmt.setString(2, userId);
                    updateStmt.executeUpdate();
                }

                deleteToken(token);
                ResetLinkServer.stopServer();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error resetting password with token: " + e.getMessage());
        }
        return false;
    }

    private void deleteToken(String token) {
        if (conn == null) {
            System.err.println("Cannot delete token: Database connection is null");
            return;
        }

        String sql = "DELETE FROM reset_token WHERE token = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting token: " + e.getMessage());
        }
    }
}