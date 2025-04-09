package com.example.reclamation.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class MessageReclamation implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private UUID userId;
    private UUID reclamationId;
    private String contenu;
    private Date dateMessage;

    // Constructor
    public MessageReclamation(UUID id, UUID userId, UUID reclamationId, String contenu, Date dateMessage) {
        this.id = id;
        this.userId = userId;
        this.reclamationId = reclamationId;
        this.contenu = contenu;
        this.dateMessage = dateMessage;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getReclamationId() {
        return reclamationId;
    }

    public void setReclamationId(UUID reclamationId) {
        this.reclamationId = reclamationId;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Date getDateMessage() {
        return dateMessage;
    }

    public void setDateMessage(Date dateMessage) {
        this.dateMessage = dateMessage;
    }
}