package com.example.Evenement.Model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Evenement {
    private Integer id;
    private String titre;
    private String description;
    private TypeEvenement type;
    private StatutEvenement statut;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private String photoPath;
    private List<Region> regions = new ArrayList<>();
    //private List<Inscription> inscriptions = new ArrayList<>();
    //private List<CommentaireEvent> commentaires = new ArrayList<>();

    // Constructeurs
    public Evenement() {}

    public Evenement(String titre, String description, TypeEvenement type,
                     LocalDateTime dateDebut, LocalDateTime dateFin) {
        this.titre = titre;
        this.description = description;
        this.type = type;
        this.statut = StatutEvenement.A_VENIR;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // Getters & Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TypeEvenement getType() { return type; }
    public void setType(TypeEvenement type) { this.type = type; }

    public StatutEvenement getStatut() { return statut; }
    public void setStatut(StatutEvenement statut) { this.statut = statut; }

    public LocalDateTime getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDateTime dateDebut) { this.dateDebut = dateDebut; }

    public LocalDateTime getDateFin() { return dateFin; }
    public void setDateFin(LocalDateTime dateFin) { this.dateFin = dateFin; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public List<Region> getRegions() { return regions; }
    public void addRegion(Region region) { this.regions.add(region); }
    public void removeRegion(Region region) { this.regions.remove(region); }

    //public List<Inscription> getInscriptions() { return inscriptions; }
    //public void addInscription(Inscription inscription) { this.inscriptions.add(inscription); }

    //public List<CommentaireEvent> getCommentaires() { return commentaires; }
    //public void addCommentaire(CommentaireEvent commentaire) { this.commentaires.add(commentaire); }
}