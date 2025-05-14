package com.example.produit.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Categorie {
    private final ObjectProperty<Integer> id = new SimpleObjectProperty<>();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty imgUrl = new SimpleStringProperty();
    private final ObjectProperty<Categorie> parent = new SimpleObjectProperty<>();

    public Categorie() {
    }

    public Categorie(Integer id, String nom, String imgUrl, Categorie parent) {
        setId(id);
        setNom(nom);
        setImgUrl(imgUrl);
        setParent(parent);
    }

    public Integer getId() { return id.get(); }
    public void setId(Integer id) { this.id.set(id); }
    public ObjectProperty<Integer> idProperty() { return id; }

    public String getNom() { return nom.get(); }
    public void setNom(String nom) { this.nom.set(nom); }
    public StringProperty nomProperty() { return nom; }

    public String getImgUrl() { return imgUrl.get(); }
    public void setImgUrl(String imgUrl) { this.imgUrl.set(imgUrl); }
    public StringProperty imgUrlProperty() { return imgUrl; }

    public Categorie getParent() { return parent.get(); }
    public void setParent(Categorie parent) { this.parent.set(parent); }
    public ObjectProperty<Categorie> parentProperty() { return parent; }

    @Override
    public String toString() {
        return getNom();
    }
}