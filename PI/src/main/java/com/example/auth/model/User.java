package com.example.auth.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String email;
    private List<String> roles;
    private String password;
    private String travail;
    private Date dateInscri;
    private String photoUrl;
    private boolean isVerified;
    private String verificationToken; // New field
    private String nom;
    private String prenom;
    private String numTel;

    // Updated constructor
    public User(UUID id, String email, String rolesJson, String password, String travail, Date dateInscri,
                String photoUrl, boolean isVerified, String verificationToken, String nom, String prenom, String numTel) {
        this.id = id;
        this.email = email;
        this.roles = parseRoles(rolesJson);
        this.password = password;
        this.travail = travail;
        this.dateInscri = dateInscri;
        this.photoUrl = photoUrl;
        this.isVerified = isVerified;
        this.verificationToken = verificationToken;
        this.nom = nom;
        this.prenom = prenom;
        this.numTel = numTel;
    }

    // Parse JSON roles string into List<String>
    private List<String> parseRoles(String rolesJson) {
        if (rolesJson == null || rolesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(rolesJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            System.err.println("Error parsing roles: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Convert roles back to JSON string for storage
    public String getRolesAsJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(roles != null ? roles : new ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error converting roles to JSON: " + e.getMessage());
            return "[]";
        }
    }

    // Check if user has a specific role
    public boolean hasRole(String role) {
        return roles != null && role != null && roles.contains(role);
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles != null ? roles : new ArrayList<>();
    }

    public void setRoles(List<String> roles) {
        this.roles = roles != null ? roles : new ArrayList<>();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTravail() {
        return travail;
    }

    public void setTravail(String travail) {
        this.travail = travail;
    }

    public Date getDateInscri() {
        return dateInscri;
    }

    public void setDateInscri(Date dateInscri) {
        this.dateInscri = dateInscri;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNumTel() {
        return numTel;
    }

    public void setNumTel(String numTel) {
        this.numTel = numTel;
    }
}