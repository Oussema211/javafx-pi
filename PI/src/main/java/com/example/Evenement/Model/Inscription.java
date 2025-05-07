package com.example.Evenement.Model;

import com.example.auth.model.User;
import java.time.LocalDateTime;

public class Inscription {
    private Integer id;
    private String nom;
    private String prenom;
    private String email;
    private String numTel;
    private String travail;
    private User user;
    private Evenement evenement;
    private LocalDateTime dateInscription;

    public Inscription() {
        this.dateInscription = LocalDateTime.now();
    }

    public Inscription(String nom, String prenom, String email, String numTel,
                       String travail, User user, Evenement evenement) {
        this();
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.numTel = numTel;
        this.travail = travail;
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

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNumTel() {
        return numTel;
    }

    public void setNumTel(String numTel) {
        this.numTel = numTel;
    }

    public String getTravail() {
        return travail;
    }

    public void setTravail(String travail) {
        this.travail = travail;
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

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    @Override
    public String toString() {
        return "Inscription{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", numTel='" + numTel + '\'' +
                ", travail='" + travail + '\'' +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", evenement=" + (evenement != null ? evenement.toString() : "null") +
                ", dateInscription=" + dateInscription +
                '}';
    }
}