package com.example.reclamation.model;

import java.util.Date;
import java.util.UUID;

public class Reclamation { // Corrected name
    private UUID id;
    private UUID userId;
    private UUID tagId;
    private Date dateReclamation;
    private int rate;
    private String title;
    private String description;
    private String statut;

    public Reclamation(UUID id, UUID userId, UUID tagId, Date dateReclamation, int rate, 
                       String title, String description, String statut) {
        this.id = id;
        this.userId = userId;
        this.tagId = tagId;
        this.dateReclamation = dateReclamation;
        this.rate = rate;
        this.title = title;
        this.description = description;
        this.statut = statut;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getTagId() { return tagId; }
    public Date getDateReclamation() { return dateReclamation; }
    public int getRate() { return rate; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatut() { return statut; }

    public void setId(UUID id) { this.id = id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setTagId(UUID tagId) { this.tagId = tagId; }
    public void setDateReclamation(Date dateReclamation) { this.dateReclamation = dateReclamation; }
    public void setRate(int rate) { this.rate = rate; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStatut(String statut) { this.statut = statut; }
}