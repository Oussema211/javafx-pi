package com.example.cart.model;

public class OrderSummary {
    private String id;         // UUID de la commande
    private String userId;     // UUID de l'utilisateur
    private String dateAchat;  // Date d'achat au format String
    private double prixTotal;  // Prix total de la commande

    public OrderSummary(String id, String userId, String dateAchat, double prixTotal) {
        this.id = id;
        this.userId = userId;
        this.dateAchat = dateAchat;
        this.prixTotal = prixTotal;
    }

    // Getters
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

    // Setters
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
