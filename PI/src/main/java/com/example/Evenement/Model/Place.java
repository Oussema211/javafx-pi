package com.example.Evenement.Model;

public class Place {
    private int id;
    private int evenementId;
    private int numeroLigne;
    private int numeroColonne;
    private String statut; // "libre", "reservee", "occupee"
    private String userId;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getEvenementId() { return evenementId; }
    public void setEvenementId(int evenementId) { this.evenementId = evenementId; }
    public int getNumeroLigne() { return numeroLigne; }
    public void setNumeroLigne(int numeroLigne) { this.numeroLigne = numeroLigne; }
    public int getNumeroColonne() { return numeroColonne; }
    public void setNumeroColonne(int numeroColonne) { this.numeroColonne = numeroColonne; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
} 