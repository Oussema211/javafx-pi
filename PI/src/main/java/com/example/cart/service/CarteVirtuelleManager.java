package com.example.cart.service;

import com.example.cart.model.CarteVirtuelle;
import com.example.cart.model.TransactionBlockchain;
import com.example.cart.utils.CarteVirtuelleDataManager;

import java.util.ArrayList;
import java.util.List;

public class CarteVirtuelleManager {

    private static CarteVirtuelle carteVirtuelle;
    private static final List<TransactionBlockchain> historiqueTransactions = new ArrayList<>();

    static {
        chargerDepuisFichier();
    }

    public static void desactiverCarte() {
        if (carteVirtuelle != null && carteVirtuelle.isActive()) {
            carteVirtuelle.setActive(false);
            carteVirtuelle.setMotDePasse(null);
            sauvegarder();
        }
    }

    public static void creerNouvelleCarte() {
        carteVirtuelle = new CarteVirtuelle(0.0);
    }

    public static void chargerCarte(double montant) {
        if (carteVirtuelle != null && montant > 0 && carteVirtuelle.isActive()) {
            carteVirtuelle.setSolde(carteVirtuelle.getSolde() + montant);
            historiqueTransactions.add(
                    new TransactionBlockchain(montant, "Recharge de la carte", getDernierHash())
            );
            sauvegarder();
        }
    }

    public static boolean effectuerPaiement(double montant) {
        if (carteVirtuelle != null && montant > 0 && carteVirtuelle.isActive() && carteVirtuelle.getSolde() >= montant) {
            carteVirtuelle.setSolde(carteVirtuelle.getSolde() - montant);
            historiqueTransactions.add(
                    new TransactionBlockchain(montant, "Paiement avec la carte", getDernierHash())
            );
            sauvegarder();
            return true;
        }
        return false;
    }

    public static boolean activerCarte(String motDePasse) {
        if (carteVirtuelle != null && !carteVirtuelle.isActive()) {
            carteVirtuelle.setMotDePasse(motDePasse);
            carteVirtuelle.setActive(true);
            sauvegarder();
            return true;
        }
        return false;
    }

    public static boolean verifierMotDePasse(String motDePasse) {
        return carteVirtuelle != null && carteVirtuelle.getMotDePasse() != null &&
                carteVirtuelle.getMotDePasse().equals(motDePasse);
    }

    public static double getSolde() {
        return (carteVirtuelle != null) ? carteVirtuelle.getSolde() : 0.0;
    }

    public static boolean isCarteActive() {
        return carteVirtuelle != null && carteVirtuelle.isActive();
    }

    public static String getNumeroCarte() {
        return (carteVirtuelle != null) ? carteVirtuelle.getNumero() : "Carte non disponible";
    }

    public static CarteVirtuelle getCarte() {
        return carteVirtuelle;
    }

    public static List<TransactionBlockchain> getHistoriqueTransactions() {
        return historiqueTransactions;
    }

    private static String getDernierHash() {
        if (historiqueTransactions.isEmpty()) {
            return "GENESIS"; // Hash de départ
        }
        return historiqueTransactions.get(historiqueTransactions.size() - 1).getHash();
    }

    private static void sauvegarder() {
        CarteVirtuelleDataManager.sauvegarderCarte(carteVirtuelle);
        CarteVirtuelleDataManager.sauvegarderTransactions(historiqueTransactions);
    }

    private static void chargerDepuisFichier() {
        carteVirtuelle = CarteVirtuelleDataManager.chargerCarte();
        List<TransactionBlockchain> loadedTransactions = CarteVirtuelleDataManager.chargerTransactions();
        if (loadedTransactions != null) {
            historiqueTransactions.addAll(loadedTransactions);
        }
    }
}
