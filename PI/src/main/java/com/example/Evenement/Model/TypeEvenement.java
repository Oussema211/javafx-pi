package com.example.Evenement.Model;

import java.util.HashMap;
import java.util.Map;

public enum TypeEvenement {
    FOIRE("foire", "Foire"),
    FORMATION("formation", "Formation"),
    CONFERENCE("conference", "Conférence");

    private final String value;
    private final String label;
    private static final Map<String, TypeEvenement> BY_VALUE = new HashMap<>();

    static {
        for (TypeEvenement e : values()) {
            BY_VALUE.put(e.value, e);
        }
    }

    TypeEvenement(String value, String label) {
        this.value = value;
        this.label = label;
    }

    public String getValue() { return value; }
    public String getLabel() { return label; }

    public static TypeEvenement fromValue(String value) {
        return value == null ? null : BY_VALUE.getOrDefault(value.toLowerCase(), null);
    }
}