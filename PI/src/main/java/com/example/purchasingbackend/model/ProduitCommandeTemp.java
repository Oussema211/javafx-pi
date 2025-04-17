package com.example.purchasingbackend.model;

import com.example.produit.model.Produit;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class ProduitCommandeTemp {
    private Produit produit;
    private IntegerProperty quantite;

    public ProduitCommandeTemp(Produit produit) {
        this.produit = produit;
        this.quantite = new SimpleIntegerProperty(1); // quantité par défaut
    }

    public ProduitCommandeTemp(Produit produit, int quantite) {
        this.produit = produit;
        this.quantite = new SimpleIntegerProperty(quantite);
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public int getQuantite() {
        return quantite.get();
    }

    public void setQuantite(int quantite) {
        this.quantite.set(quantite);
    }

    public IntegerProperty quantiteProperty() {
        return quantite;
    }

    @Override
    public String toString() {
        return produit.getNom() + " x" + quantite.get();
    }
}
