package com.example.reclamation.model;

public enum Status {
    WAITING("en_cours"),
    RESOLVED("resolue"),
    CLOSED("fermee"),
    REVIEW("review");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Convert string from DB to enum
    public static Status fromString(String value) {
        for (Status status : Status.values()) {
            if (status.getDisplayName().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No Status enum constant for value: " + value);
    }
}