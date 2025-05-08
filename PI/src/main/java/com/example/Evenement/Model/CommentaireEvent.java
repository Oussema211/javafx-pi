package com.example.Evenement.Model;

import com.example.auth.model.User;
import java.time.LocalDateTime;

public class CommentaireEvent {
    private Integer id;
    private String contenu;
    private LocalDateTime dateCreation;
    private User user;
    private Evenement evenement;

    public CommentaireEvent() {
        this.dateCreation = LocalDateTime.now();
    }

    public CommentaireEvent(String contenu, User user, Evenement evenement) {
        this();
        this.contenu = contenu;
        this.user = user;
        this.evenement = evenement;
    }

    // Getters and Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Evenement getEvenement() {
        return evenement;
    }

    public void setEvenement(Evenement evenement) {
        this.evenement = evenement;
    }

    @Override
    public String toString() {
        return "CommentaireEvent{" +
                "id=" + id +
                ", contenu='" + contenu + '\'' +
                ", dateCreation=" + dateCreation +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", evenement=" + (evenement != null ? evenement.toString() : "null") +
                '}';
    }
}