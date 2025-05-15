package com.example.Stock.Model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Entrepot implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String nom;
    private String adresse;
    private String ville;
    private Double espace;
    private Set<UUID> stockIds = new HashSet<>(); // Remplace Set<Stock>
    private Double latitude;
    private Double longitude;

    // Constructeur par défaut
    public Entrepot() {
    }

    // Constructeur avec paramètres
    public Entrepot(UUID id, String nom, String adresse, String ville, Double espace) {
        this.id = id;
        this.nom = nom;
        this.adresse = adresse;
        this.ville = ville;
        this.espace = espace;
    }

    // Getters & Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public Double getEspace() {
        return espace;
    }

    public void setEspace(Double espace) {
        this.espace = espace;
    }

    public Set<UUID> getStockIds() {
        return stockIds;
    }

    public void setStockIds(Set<UUID> stockIds) {
        this.stockIds = stockIds;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    // Méthodes helpers simplifiées
    public void addStockId(UUID stockId) {
        this.stockIds.add(stockId);
    }

    public void removeStockId(UUID stockId) {
        this.stockIds.remove(stockId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entrepot)) return false;
        Entrepot entrepot = (Entrepot) o;
        return id != null && id.equals(entrepot.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}