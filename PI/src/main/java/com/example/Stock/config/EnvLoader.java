package com.example.Stock.config;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;

public class EnvLoader {
    private static final Properties props = new Properties();

    static {
        try {
            // Charge depuis le répertoire du projet
            File envFile = new File(".env");
            if (envFile.exists()) {
                props.load(new FileReader(envFile));
            }

            // Valeurs par défaut
            props.putIfAbsent("ENV_MODE", "dev");
        } catch (Exception e) {
            System.err.println("Erreur de chargement .env: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return props.getProperty(key);
    }
}