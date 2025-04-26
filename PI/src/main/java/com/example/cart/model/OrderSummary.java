package com.example.cart.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un résumé de commande incluant les produits achetés, la date,
 * l'utilisateur et le total payé.
 */
public class OrderSummary {

    private String id;                       // UUID unique de la commande
    private String userId;                   // UUID de l'utilisateur ayant passé la commande
    private String dateAchat;                // Date d'achat (format ISO ou string simple)
    private double prixTotal;                // Total payé pour la commande
    private List<ProduitCommande> produitsCommandes = new ArrayList<>(); // Liste des produits commandés

    // Constructeur
    public OrderSummary(String id, String userId, String dateAchat, double prixTotal) {
        this.id = id;
        this.userId = userId;
        this.dateAchat = dateAchat;
        this.prixTotal = prixTotal;
    }

    // Getter & Setter pour la liste des produits commandés
    public List<ProduitCommande> getProduitsCommandes() {
        return produitsCommandes;
    }

    public void setProduitsCommandes(List<ProduitCommande> produitsCommandes) {
        this.produitsCommandes = produitsCommandes;
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
