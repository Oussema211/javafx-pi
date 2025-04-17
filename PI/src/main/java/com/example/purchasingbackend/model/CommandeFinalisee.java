package com.example.purchasingbackend.model;

import com.example.auth.model.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CommandeFinalisee {
    private UUID id;
    private User utilisateur;
    private List<ProduitCommandeTemp> produitsAvecQuantites;
    private LocalDateTime date;
    private double prixTotal;

    public CommandeFinalisee() {}

    public CommandeFinalisee(UUID id, User utilisateur, List<ProduitCommandeTemp> produitsAvecQuantites, LocalDateTime date, double prixTotal) {
        this.id = id;
        this.utilisateur = utilisateur;
        this.produitsAvecQuantites = produitsAvecQuantites;
        this.date = date;
        this.prixTotal = prixTotal;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getUtilisateur() { return utilisateur; }
    public void setUtilisateur(User utilisateur) { this.utilisateur = utilisateur; }

    public List<ProduitCommandeTemp> getProduitsAvecQuantites() { return produitsAvecQuantites; }
    public void setProduitsAvecQuantites(List<ProduitCommandeTemp> produitsAvecQuantites) { this.produitsAvecQuantites = produitsAvecQuantites; }

    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public double getPrixTotal() { return prixTotal; }
    public void setPrixTotal(double prixTotal) { this.prixTotal = prixTotal; }

    @Override
    public String toString() {
        return "CommandeFinalisee{" +
                "id=" + id +
                ", utilisateur=" + utilisateur.getUsername() +
                ", produits=" + produitsAvecQuantites +
                ", date=" + date +
                ", prixTotal=" + prixTotal +
                '}';
    }
}
