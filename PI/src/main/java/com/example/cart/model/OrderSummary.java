package com.example.cart.model;

import java.util.ArrayList;
import java.util.List;

public class OrderSummary {
    private String id;
    private String userId;
    private String dateAchat;
    private double prixTotal;
    private List<ProduitCommande> produitsCommandes = new ArrayList<>(); // ✅ Initialisation

    public OrderSummary(String id, String userId, String dateAchat, double prixTotal) {
        this.id = id;
        this.userId = userId;
        this.dateAchat = dateAchat;
        this.prixTotal = prixTotal;
    }

    public List<ProduitCommande> getProduitsCommandes() {
        return produitsCommandes; // Plus besoin de null check
    }

    public void setProduitsCommandes(List<ProduitCommande> produitsCommandes) {
        this.produitsCommandes = produitsCommandes;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getDateAchat() {
        return dateAchat;
    }

    public double getPrixTotal() {
        return prixTotal;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setDateAchat(String dateAchat) {
        this.dateAchat = dateAchat;
    }

    public void setPrixTotal(double prixTotal) {
        this.prixTotal = prixTotal;
    }
}
