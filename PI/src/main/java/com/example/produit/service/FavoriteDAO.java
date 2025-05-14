package com.example.produit.service;

import java.sql.*;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class FavoriteDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/pides?useSSL=false&serverTimezone=Europe/Paris";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static volatile boolean tableChecked = false;
    private static final ReentrantLock lock = new ReentrantLock();
    private static final int MAX_CONNECTION_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    static {
        createTableIfNotExists();
    }

    private static Connection getConnection() throws SQLException {
        for (int attempt = 1; attempt <= MAX_CONNECTION_RETRIES; attempt++) {
            try {
                return DriverManager.getConnection(URL, USER, PASSWORD);
            } catch (SQLException e) {
                logError("Connection attempt " + attempt + " failed", e);
                if (attempt == MAX_CONNECTION_RETRIES) {
                    throw new SQLException("Failed to connect to database after " + MAX_CONNECTION_RETRIES + " attempts", e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SQLException("Interrupted during connection retry", ie);
                }
            }
        }
        throw new SQLException("Unexpected error in connection logic");
    }

    private static void createTableIfNotExists() {
        if (tableChecked) return;

        lock.lock();
        try {
            if (tableChecked) return;

            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS user_favorites (
                    user_id VARCHAR(36) NOT NULL,
                    produit_id VARCHAR(255) NOT NULL,
                    PRIMARY KEY (user_id, produit_id),
                    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE,
                    FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
            """;

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet rs = metaData.getTables(null, null, "user_favorites", new String[]{"TABLE"})) {
                    if (!rs.next()) {
                        stmt.executeUpdate(createTableSQL);
                        logInfo("Table user_favorites created successfully");
                    } else {
                        logInfo("Table user_favorites already exists");
                    }
                }
                tableChecked = true;
            } catch (SQLException e) {
                logError("Error creating table user_favorites", e);
                throw new RuntimeException("Failed to initialize user_favorites table: " + e.getMessage(), e);
            }
        } finally {
            lock.unlock();
        }
    }

    public static void addFavorite(UUID userId, String produitId) {
        validateInputs(userId, produitId);
        String sql = "INSERT IGNORE INTO user_favorites (user_id, produit_id) VALUES (?, ?)";
        executeWithRetry(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                try {
                    stmt.setString(1, userId.toString());
                    stmt.setString(2, produitId.trim());
                    int rowsAffected = stmt.executeUpdate();
                    conn.commit();
                    if (rowsAffected > 0) {
                        logInfo("Favorite added for user " + userId + " and product " + produitId);
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }, 3);
    }

    public static void removeFavorite(UUID userId, String produitId) {
        validateInputs(userId, produitId);
        String sql = "DELETE FROM user_favorites WHERE user_id = ? AND produit_id = ?";
        executeWithRetry(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                try {
                    stmt.setString(1, userId.toString());
                    stmt.setString(2, produitId.trim());
                    int rowsAffected = stmt.executeUpdate();
                    conn.commit();
                    if (rowsAffected > 0) {
                        logInfo("Favorite removed for user " + userId + " and product " + produitId);
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }, 3);
    }

    public static boolean isFavorite(UUID userId, String produitId) {
        validateInputs(userId, produitId);
        createTableIfNotExists();
        String sql = "SELECT COUNT(*) FROM user_favorites WHERE user_id = ? AND produit_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId.toString());
            stmt.setString(2, produitId.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            return false;
        } catch (SQLException e) {
            logError("Error checking favorite for user " + userId + " and product " + produitId, e);
            throw new RuntimeException("Failed to check favorite: " + e.getMessage(), e);
        }
    }

    public static List<String> getFavoritesByUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        List<String> favorites = new ArrayList<>();
        String sql = "SELECT produit_id FROM user_favorites WHERE user_id = ?";
        executeWithRetry(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, userId.toString());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        favorites.add(rs.getString("produit_id"));
                    }
                }
            }
        }, 3);
        logInfo("Retrieved " + favorites.size() + " favorites for user " + userId);
        return favorites;
    }

    public static void clearFavorites(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        String sql = "DELETE FROM user_favorites WHERE user_id = ?";
        executeWithRetry(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                conn.setAutoCommit(false);
                try {
                    stmt.setString(1, userId.toString());
                    int rowsAffected = stmt.executeUpdate();
                    conn.commit();
                    logInfo("Cleared " + rowsAffected + " favorites for user " + userId);
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            }
        }, 3);
    }

    private static void validateInputs(UUID userId, String produitId) {
        if (userId == null || produitId == null || produitId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID and Product ID cannot be null or empty");
        }
    }

    private static void executeWithRetry(RunnableWithException operation, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                operation.run();
                return;
            } catch (SQLException e) {
                logError("Attempt " + attempt + " failed", e);
                if (attempt == maxAttempts) {
                    throw new RuntimeException("Max retry attempts reached: " + e.getMessage(), e);
                }
                try {
                    Thread.sleep(RETRY_DELAY_MS * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry: " + ie.getMessage(), ie);
                }
            }
        }
    }

    private static void logInfo(String message) {
        System.out.println(message + " at " + ZonedDateTime.now(ZoneId.of("Europe/Paris")));
    }

    private static void logError(String message, Throwable e) {
        System.err.println(message + " at " + ZonedDateTime.now(ZoneId.of("Europe/Paris")) + (e != null ? ": " + e.getMessage() : ""));
        if (e != null) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface RunnableWithException {
        void run() throws SQLException;
    }
}