package com.example.produit.model;

import com.example.auth.model.User;
import javafx.beans.property.*;
import java.time.LocalDateTime;
import java.util.UUID;

public class Commentaire {

    private final ObjectProperty<UUID> id = new SimpleObjectProperty<>();
    private final StringProperty auteur = new SimpleStringProperty();
    private final StringProperty contenu = new SimpleStringProperty();
    private final FloatProperty note = new SimpleFloatProperty();
    private final ObjectProperty<LocalDateTime> dateCreation = new SimpleObjectProperty<>(LocalDateTime.now());
    private final ObjectProperty<Produit> produit = new SimpleObjectProperty<>();
    private final ObjectProperty<User> user = new SimpleObjectProperty<>();

    public Commentaire() {
        this.id.set(UUID.randomUUID()); // Generate UUID on creation
    }

    // Getters and Setters
    public UUID getId() { return id.get(); }
    public void setId(UUID id) { this.id.set(id); }
    public ObjectProperty<UUID> idProperty() { return id; }

    public String getAuteur() { return auteur.get(); }
    public void setAuteur(String auteur) { this.auteur.set(auteur); }
    public StringProperty auteurProperty() { return auteur; }

    public String getContenu() { return contenu.get(); }
    public void setContenu(String contenu) { this.contenu.set(contenu); }
    public StringProperty contenuProperty() { return contenu; }

    public Float getNote() { return note.get(); }
    public void setNote(Float note) { this.note.set(note); }
    public FloatProperty noteProperty() { return note; }

    public LocalDateTime getDateCreation() { return dateCreation.get(); }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation.set(dateCreation); }
    public ObjectProperty<LocalDateTime> dateCreationProperty() { return dateCreation; }

    public Produit getProduit() { return produit.get(); }
    public void setProduit(Produit produit) { this.produit.set(produit); }
    public ObjectProperty<Produit> produitProperty() { return produit; }

    public User getUser() { return user.get(); }
    public void setUser(User user) { this.user.set(user); }
    public ObjectProperty<User> userProperty() { return user; }
}