package com.example.Evenement.Model;

public enum TypeEvenement {
    FOIRE("Foire"),
    FORMATION("Formation"),
    CONFERENCE("Conférence");

    private final String label;

    TypeEvenement(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}