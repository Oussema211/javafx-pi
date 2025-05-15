package com.example.Evenement.Model;

import java.util.HashMap;
import java.util.Map;

public enum StatutEvenement {
    A_VENIR("a_venir", "À venir"),
    ANNULE("annule", "Annulé"),
    TERMINE("termine", "Terminé");

    private final String value;
    private final String label;
    private static final Map<String, StatutEvenement> BY_VALUE = new HashMap<>();

    static {
        for (StatutEvenement e : values()) {
            BY_VALUE.put(e.value, e);
        }
    }

    StatutEvenement(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() { return value; }
    public String getLabel() { return label; }

    public static StatutEvenement fromValue(String value) {
        return value == null ? null : BY_VALUE.getOrDefault(value.toLowerCase(), null);
    }
}
