package com.example.produit.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.util.UUID;

public class Produit {

    private final ObjectProperty<UUID> id = new SimpleObjectProperty<>();
    private final ObjectProperty<Categorie> category = new SimpleObjectProperty<>();
    private final ObjectProperty<UUID> userId = new SimpleObjectProperty<>();
    private final StringProperty nom = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final FloatProperty prixUnitaire = new SimpleFloatProperty();
    private final ObjectProperty<LocalDateTime> dateCreation = new SimpleObjectProperty<>(LocalDateTime.now());
    private final IntegerProperty quantite = new SimpleIntegerProperty();
    private final FloatProperty rate = new SimpleFloatProperty();
    private final StringProperty imageName = new SimpleStringProperty();
    private final BooleanProperty selected = new SimpleBooleanProperty(false);

    public Produit() {}

    public Produit(UUID id, Categorie category, UUID userId, String nom, String description,
                   float prixUnitaire, LocalDateTime dateCreation, int quantite, Float rate, String imageName) {
        setId(id);
        setCategory(category);
        setUserId(userId);
        setNom(nom);
        setDescription(description);
        setPrixUnitaire(prixUnitaire);
        setDateCreation(dateCreation);
        setQuantite(quantite);
        setRate(rate);
        setImageName(imageName);
    }

    // Getters and Setters
    public UUID getId() { return id.get(); }
    public void setId(UUID id) { this.id.set(id); }
    public ObjectProperty<UUID> idProperty() { return id; }

    public Categorie getCategory() { return category.get(); }
    public void setCategory(Categorie category) { this.category.set(category); }
    public ObjectProperty<Categorie> categoryProperty() { return category; }

    public UUID getUserId() { return userId.get(); }
    public void setUserId(UUID userId) { this.userId.set(userId); }
    public ObjectProperty<UUID> userIdProperty() { return userId; }

    public String getNom() { return nom.get(); }
    public void setNom(String nom) { this.nom.set(nom); }
    public StringProperty nomProperty() { return nom; }

    public String getDescription() { return description.get(); }
    public void setDescription(String description) { this.description.set(description); }
    public StringProperty descriptionProperty() { return description; }

    public float getPrixUnitaire() { return prixUnitaire.get(); }
    public void setPrixUnitaire(float prixUnitaire) { this.prixUnitaire.set(prixUnitaire); }
    public FloatProperty prixUnitaireProperty() { return prixUnitaire; }

    public LocalDateTime getDateCreation() { return dateCreation.get(); }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation.set(dateCreation); }
    public ObjectProperty<LocalDateTime> dateCreationProperty() { return dateCreation; }

    public int getQuantite() { return quantite.get(); }
    public void setQuantite(int quantite) { this.quantite.set(quantite); }
    public IntegerProperty quantiteProperty() { return quantite; }

    public Float getRate() { return rate.get(); }
    public void setRate(Float rate) { this.rate.set(rate); }
    public FloatProperty rateProperty() { return rate; }

    public String getImageName() { return imageName.get(); }
    public void setImageName(String imageName) { this.imageName.set(imageName); }
    public StringProperty imageNameProperty() { return imageName; }

    public boolean isSelected() { return selected.get(); }
    public void setSelected(boolean selected) { this.selected.set(selected); }
    public BooleanProperty selectedProperty() { return selected; }

    @Override
    public String toString() {
        return getNom();
    }
}
