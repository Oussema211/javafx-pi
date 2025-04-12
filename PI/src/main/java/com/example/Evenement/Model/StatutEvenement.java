package com.example.Evenement.Model;

public enum StatutEvenement {
    A_VENIR("À venir"),
    ANNULE("Annulé"),
    TERMINE("Terminé");

    private final String label;

    StatutEvenement(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}