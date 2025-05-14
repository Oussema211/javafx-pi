package com.example.produit.model;

import com.example.auth.model.User;
import javafx.beans.property.*;
import javafx.collections.FXCollections;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Commentaire {

    private final ObjectProperty<UUID> id = new SimpleObjectProperty<>();
    private final StringProperty auteur = new SimpleStringProperty();
    private final StringProperty contenu = new SimpleStringProperty();
    private final FloatProperty note = new SimpleFloatProperty();
    private final ObjectProperty<LocalDateTime> dateCreation = new SimpleObjectProperty<>();
    private final ObjectProperty<Produit> produit = new SimpleObjectProperty<>();
    private final ObjectProperty<User> user = new SimpleObjectProperty<>();
    private final ObjectProperty<Commentaire> parentComment = new SimpleObjectProperty<>();
    private final ListProperty<Commentaire> replies = new SimpleListProperty<>(FXCollections.observableArrayList());
    private static final int MAX_CONTENT_LENGTH = 500;

    public Commentaire() {
        // Initialize ID and dateCreation
        this.id.set(UUID.randomUUID());
        // Set initial dateCreation to the provided time (May 14, 2025, 11:39 PM CET)
        ZonedDateTime cetTime = ZonedDateTime.of(2025, 5, 14, 23, 39, 0, 0, ZoneId.of("Europe/Paris"));
        this.dateCreation.set(cetTime.toLocalDateTime());
        // Bind auteur to user's name changes
        user.addListener((obs, oldUser, newUser) -> {
            if (newUser != null) {
                this.auteur.set(newUser.getNom());
            }
        });
    }

    // Getters and Setters with Validation
    public UUID getId() { return id.get(); }
    public void setId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        this.id.set(id);
    }
    public ObjectProperty<UUID> idProperty() { return id; }

    public String getAuteur() { return auteur.get(); }
    public void setAuteur(String auteur) {
        if (auteur == null || auteur.trim().isEmpty()) {
            throw new IllegalArgumentException("Author cannot be null or empty");
        }
        this.auteur.set(auteur.trim());
    }
    public StringProperty auteurProperty() { return auteur; }

    public String getContenu() { return contenu.get(); }
    public void setContenu(String contenu) {
        if (contenu == null || contenu.trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
        if (contenu.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("Content cannot exceed " + MAX_CONTENT_LENGTH + " characters");
        }
        this.contenu.set(contenu.trim());
    }
    public StringProperty contenuProperty() { return contenu; }

    public Float getNote() { return note.get(); }
    public void setNote(Float note) {
        if (note == null || note < 1.0f || note > 5.0f) {
            throw new IllegalArgumentException("Rating must be between 1.0 and 5.0");
        }
        this.note.set(note);
    }
    public FloatProperty noteProperty() { return note; }

    public LocalDateTime getDateCreation() { return dateCreation.get(); }
    public void setDateCreation(LocalDateTime dateCreation) {
        if (dateCreation == null) {
            throw new IllegalArgumentException("Date creation cannot be null");
        }
        this.dateCreation.set(dateCreation);
    }
    public ObjectProperty<LocalDateTime> dateCreationProperty() { return dateCreation; }

    public Produit getProduit() { return produit.get(); }
    public void setProduit(Produit produit) {
        if (produit == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        this.produit.set(produit);
    }
    public ObjectProperty<Produit> produitProperty() { return produit; }

    public User getUser() { return user.get(); }
    public void setUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        this.user.set(user);
    }
    public ObjectProperty<User> userProperty() { return user; }

    public Commentaire getParentComment() { return parentComment.get(); }
    public void setParentComment(Commentaire parent) {
        this.parentComment.set(parent);
    }
    public ObjectProperty<Commentaire> parentCommentProperty() { return parentComment; }

    public List<Commentaire> getReplies() { return replies.get(); }
    public ListProperty<Commentaire> repliesProperty() { return replies; }
    public void addReply(Commentaire reply) {
        if (reply == null) {
            throw new IllegalArgumentException("Reply cannot be null");
        }
        reply.setParentComment(this);
        this.replies.add(reply);
    }

    // Method to update comment details
    public void updateComment(String newContenu, Float newNote) {
        if (newContenu != null && !newContenu.trim().isEmpty()) {
            setContenu(newContenu);
        }
        if (newNote != null && newNote >= 1.0f && newNote <= 5.0f) {
            setNote(newNote);
        }
        // Update timestamp to current time in CET
        ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Europe/Paris"));
        this.dateCreation.set(currentTime.toLocalDateTime());
    }

    // Utility to check if comment is editable by a user
    public boolean isEditableBy(User user) {
        return this.user.get() != null && user != null && this.user.get().getId().equals(user.getId());
    }
}