package com.example.Evenement.Model;

import javafx.beans.property.*;

public class Region {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty ville = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();

    public Region() {}

    public Region(String nom, String ville, String description) {
        setNom(nom);
        setVille(ville);
        setDescription(description);
    }

    public Region(int id, String nom, String ville, String description) {
        setId(id);
        setNom(nom);
        setVille(ville);
        setDescription(description);
    }

    public IntegerProperty idProperty() { return id; }
    public StringProperty nomProperty() { return nom; }
    public StringProperty villeProperty() { return ville; }
    public StringProperty descriptionProperty() { return description; }

    public int getId() { return id.get(); }
    public String getNom() { return nom.get(); }
    public String getVille() { return ville.get(); }
    public String getDescription() { return description.get(); }

    public void setId(int id) { this.id.set(id); }
    public void setNom(String nom) { this.nom.set(nom); }
    public void setVille(String ville) { this.ville.set(ville); }
    public void setDescription(String description) { this.description.set(description); }

    @Override
    public String toString() {
        return getNom() + " (" + getVille() + ")";
    }
}
