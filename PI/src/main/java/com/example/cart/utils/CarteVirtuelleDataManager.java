package com.example.cart.utils;

import com.example.cart.model.CarteVirtuelle;
import com.example.cart.model.TransactionBlockchain;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CarteVirtuelleDataManager {

    private static final String CARTE_FILE = "carte_virtuelle.json";
    private static final String TRANSACTIONS_FILE = "transactions_blockchain.json";
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();


    // ✅ Sauvegarder la carte dans fichier JSON
    public static void sauvegarderCarte(CarteVirtuelle carte) {
        try (FileWriter writer = new FileWriter(CARTE_FILE)) {
            gson.toJson(carte, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ Charger la carte depuis fichier JSON
    public static CarteVirtuelle chargerCarte() {
        try (FileReader reader = new FileReader(CARTE_FILE)) {
            return gson.fromJson(reader, CarteVirtuelle.class);
        } catch (IOException e) {
            return null; // Fichier pas trouvé ou vide
        }
    }

    // ✅ Sauvegarder l'historique des transactions
    public static void sauvegarderTransactions(List<TransactionBlockchain> transactions) {
        try (FileWriter writer = new FileWriter(TRANSACTIONS_FILE)) {
            gson.toJson(transactions, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ Charger l'historique des transactions
    public static List<TransactionBlockchain> chargerTransactions() {
        try (FileReader reader = new FileReader(TRANSACTIONS_FILE)) {
            Type listType = new TypeToken<List<TransactionBlockchain>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            return new ArrayList<>(); // Fichier vide ou inexistant = liste vide
        }
    }
}
