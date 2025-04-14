package com.example.Stock.Model;

import com.example.produit.model.Produit;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Stock {
    private UUID id;
    private UUID produitId;
    private Produit produit;
    private LocalDateTime dateEntree;
    private LocalDateTime dateSortie;   
    private Integer seuilAlert;
    private UUID userId;
    private Set<UUID> entrepotIds = new HashSet<>();

    // Constructeurs
    public Stock() {}

    public Stock(UUID id, UUID produitId, LocalDateTime dateEntree, Integer seuilAlert, UUID userId) {
        this.id = id;
        this.produitId = produitId;
        this.dateEntree = dateEntree;
        this.seuilAlert = seuilAlert;
        this.userId = userId;
    }

    // Getters et Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProduitId() {
        return produitId;
    }

    public void setProduitId(UUID produitId) {
        this.produitId = produitId;
    }
    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
        if (produit != null) {
            this.produitId = produit.getId();
        }
    }

    public LocalDateTime getDateEntree() {
        return dateEntree;
    }

    public void setDateEntree(LocalDateTime dateEntree) {
        this.dateEntree = dateEntree;
    }

    public LocalDateTime getDateSortie() {
        return dateSortie;
    }

    public void setDateSortie(LocalDateTime dateSortie) {
        this.dateSortie = dateSortie;
    }

    public Integer getSeuilAlert() {
        return seuilAlert;
    }

    public void setSeuilAlert(Integer seuilAlert) {
        this.seuilAlert = seuilAlert;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Set<UUID> getEntrepotIds() {
        return entrepotIds;
    }

    public void setEntrepotIds(Set<UUID> entrepotIds) {
        this.entrepotIds = entrepotIds;
    }

    // Méthodes utilitaires
    public void addEntrepotId(UUID entrepotId) {
        this.entrepotIds.add(entrepotId);
    }

    public void removeEntrepotId(UUID entrepotId) {
        this.entrepotIds.remove(entrepotId);
    }
}