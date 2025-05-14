package com.example.produit.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Produit {
    private String id;
    private String nom;
    private String description;
    private float prixUnitaire;
    private int quantite;
    private LocalDateTime dateCreation;
    private String imageName;
    private Categorie category;
    private UUID userId;
    private Float rate;
    private List<Commentaire> commentaires;
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public Produit() {
        this.commentaires = new ArrayList<>();
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public float getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(float prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getImageName() { return imageName; }
    public void setImageName(String imageName) { this.imageName = imageName; }

    public Categorie getCategory() { return category; }
    public void setCategory(Categorie category) { this.category = category; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public Float getRate() { return rate; }
    public void setRate(Float rate) { this.rate = rate; }

    public List<Commentaire> getCommentaires() { return commentaires; }
    public void setCommentaires(List<Commentaire> commentaires) { this.commentaires = commentaires != null ? commentaires : new ArrayList<>(); }

    public void addCommentaire(Commentaire commentaire) {
        if (commentaire != null) {
            this.commentaires.add(commentaire);
        }
    }

    public BooleanProperty selectedProperty() { return selected; }
    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
}