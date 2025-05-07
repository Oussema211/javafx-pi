package com.example.cart.model;

import java.util.UUID;

public class CarteVirtuelle {
    private String numero;
    private double solde;
    private boolean active;
    private String motDePasse;

    public CarteVirtuelle(double soldeInitial) {
        this.numero = UUID.randomUUID().toString();
        this.solde = soldeInitial;
        this.active = false; // ❌ Par défaut désactivée
        this.motDePasse = null; // 🔒 Aucun mot de passe par défaut
    }

    public String getNumero() {
        return numero;
    }

    public double getSolde() {
        return solde;
    }

    public void setSolde(double solde) {
        this.solde = solde;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }
}
