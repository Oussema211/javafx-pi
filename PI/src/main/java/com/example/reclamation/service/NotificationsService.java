package com.example.reclamation.service;

import com.example.reclamation.model.Notifications;
import com.example.auth.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class NotificationsService {
    private final Connection conn;

    public NotificationsService() {
        conn = MyDatabase.getInstance().getCnx();
        try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS notifications (" +
                    "id VARCHAR(255) PRIMARY KEY, " +
                    "user_id VARCHAR(255) NOT NULL, " +
                    "title VARCHAR(255) NOT NULL, " +
                    "description TEXT NOT NULL, " +
                    "is_read BOOLEAN NOT NULL, " +
                    "is_for_admins BOOLEAN NOT NULL, " +
                    "created_at DATETIME NOT NULL, " +
                    "FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE)";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error initializing notifications table: " + e.getMessage());
        }
    }

    // Create: Add a new notification
    public boolean addNotification(UUID userId, String title, String description, boolean isRead, boolean isForAdmins) {
        UUID id = UUID.randomUUID();
        Date createdAt = new Date();
        String sql = "INSERT INTO notifications (id, user_id, title, description, is_read, is_for_admins, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            pstmt.setString(2, userId.toString());
            pstmt.setString(3, title);
            pstmt.setString(4, description);
            pstmt.setBoolean(5, isRead);
            pstmt.setBoolean(6, isForAdmins);
            pstmt.setTimestamp(7, new Timestamp(createdAt.getTime()));
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            if (e.getMessage().contains("FOREIGN KEY")) {
                System.err.println("Foreign key error: User ID " + userId + " does not exist.");
            } else {
                System.err.println("Error adding notification: " + e.getMessage());
            }
            return false;
        }
    }

    // Read: Get notification by ID
    public Notifications getNotificationById(UUID id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Notifications(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getBoolean("is_read"),
                        rs.getBoolean("is_for_admins"),
                        rs.getTimestamp("created_at")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching notification by ID: " + e.getMessage());
        }
        return null;
    }

    // Read: Get all notifications
    public List<Notifications> getAllNotifications() {
        List<Notifications> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                notifications.add(new Notifications(
                        UUID.fromString(rs.getString("id")),
                        UUID.fromString(rs.getString("user_id")),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getBoolean("is_read"),
                        rs.getBoolean("is_for_admins"),
                        rs.getTimestamp("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all notifications: " + e.getMessage());
        }
        return notifications;
    }

    // Update: Update an existing notification
    public boolean updateNotification(Notifications notification) {
        String sql = "UPDATE notifications SET user_id = ?, title = ?, description = ?, is_read = ?, " +
                     "is_for_admins = ?, created_at = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notification.getUserId().toString());
            pstmt.setString(2, notification.getTitle());
            pstmt.setString(3, notification.getDescription());
            pstmt.setBoolean(4, notification.isRead());
            pstmt.setBoolean(5, notification.isForAdmins());
            pstmt.setTimestamp(6, new Timestamp(notification.getCreatedAt().getTime()));
            pstmt.setString(7, notification.getId().toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating notification: " + e.getMessage());
            return false;
        }
    }

    // Delete: Delete a notification by ID
    public boolean deleteNotification(UUID id) {
        String sql = "DELETE FROM notifications WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id.toString());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting notification: " + e.getMessage());
            return false;
        }
    }
}