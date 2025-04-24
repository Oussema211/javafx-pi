package com.example.cart.model;

import com.example.produit.model.Produit;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class CartItem implements Comparable<CartItem> {
    private final Produit produit;
    private final IntegerProperty quantite;

    public CartItem(Produit produit, int quantite) {
        this.produit = produit;
        this.quantite = new SimpleIntegerProperty(quantite);
    }

    public Produit getProduit() {
        return produit;
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

    public double getTotalPrice() {
        return produit.getPrixUnitaire() * getQuantite();
    }

    @Override
    public int compareTo(CartItem other) {
        return this.produit.getNom().compareToIgnoreCase(other.produit.getNom());
    }
    public javafx.beans.property.DoubleProperty totalPriceProperty() {
        return new javafx.beans.property.SimpleDoubleProperty(getTotalPrice());
    }

}