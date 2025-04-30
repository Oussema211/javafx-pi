package com.example.cart.model;

public class ProduitCommande {
    private String nomProduit;
    private int quantite;
    private double prixUnitaire;

    public ProduitCommande(String nomProduit, int quantite, double prixUnitaire) {
        this.nomProduit = nomProduit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    public String getNomProduit() {
        return nomProduit;
    }

    public int getQuantite() {
        return quantite;
    }

    public double getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setNomProduit(String nomProduit) {
        this.nomProduit = nomProduit;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public void setPrixUnitaire(double prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }
}
