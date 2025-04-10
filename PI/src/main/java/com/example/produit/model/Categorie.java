package com.example.produit.model;

public class Categorie {

    private int id;
    private Integer parentId;
    private String nom;
    private String slug;
    private Integer lft;
    private Integer rgt;
    private Integer lvl;
    private String imgUrl;
    private String description;

    public Categorie() {
    }

    public Categorie(int id, Integer parentId, String nom, String slug, Integer lft, Integer rgt, Integer lvl, String imgUrl, String description) {
        this.id = id;
        this.parentId = parentId;
        this.nom = nom;
        this.slug = slug;
        this.lft = lft;
        this.rgt = rgt;
        this.lvl = lvl;
        this.imgUrl = imgUrl;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public Integer getLft() {
        return lft;
    }

    public void setLft(Integer lft) {
        this.lft = lft;
    }

    public Integer getRgt() {
        return rgt;
    }

    public void setRgt(Integer rgt) {
        this.rgt = rgt;
    }

    public Integer getLvl() {
        return lvl;
    }

    public void setLvl(Integer lvl) {
        this.lvl = lvl;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Categorie{" +
                "id=" + id +
                ", parentId=" + parentId +
                ", nom='" + nom + '\'' +
                ", slug='" + slug + '\'' +
                ", lft=" + lft +
                ", rgt=" + rgt +
                ", lvl=" + lvl +
                ", imgUrl='" + imgUrl + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}