package com.example.reclamation.model;

import java.io.Serializable;
import java.util.UUID;

public class Tag implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String name;

    // Constructor
    public Tag(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}