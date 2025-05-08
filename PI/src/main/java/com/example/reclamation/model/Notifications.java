package com.example.reclamation.model;

import java.util.Date;
import java.util.UUID;

public class Notifications {
    private UUID id;
    private UUID userId;
    private String title;
    private String description;
    private boolean isRead;
    private boolean isForAdmins;
    private Date createdAt;

    public Notifications() {
        this.id = UUID.randomUUID();
        this.createdAt = new Date();
        this.isRead = false;
        this.isForAdmins = false;
    }

    public Notifications(UUID id, UUID userId, String title, String description, 
                        boolean isRead, boolean isForAdmins, Date createdAt) {
        this.id = (id != null) ? id : UUID.randomUUID();
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.isRead = isRead;
        this.isForAdmins = isForAdmins;
        this.createdAt = (createdAt != null) ? createdAt : new Date();
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean isForAdmins() {
        return isForAdmins;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(UUID id) {
        this.id = id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    public void setForAdmins(boolean isForAdmins) {
        this.isForAdmins = isForAdmins;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}