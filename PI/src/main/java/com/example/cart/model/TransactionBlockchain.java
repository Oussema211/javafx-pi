package com.example.cart.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

public class TransactionBlockchain {
    private double montant;
    private String description;
    private LocalDateTime timestamp;
    private String hash;
    private String previousHash; // ✅ Nouveau champ

    // ✅ Nouveau constructeur avec previousHash
    public TransactionBlockchain(double montant, String description, String previousHash) {
        this.montant = montant;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.previousHash = previousHash;
        this.hash = genererHash();
    }

    // ✅ Générer un hash SHA-256 basé sur tous les champs + previousHash
    private String genererHash() {
        String data = montant + description + timestamp.toString() + previousHash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur génération hash blockchain", e);
        }
    }

    // ✅ Getters
    public double getMontant() {
        return montant;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getHash() {
        return hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }
}
