package com.example.Stock.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;

public class AppConfig {
    private static JsonObject config;

    static {
        try (InputStream is = AppConfig.class.getResourceAsStream(
                "/com/example/Stock/config/config.json")) {
            config = JsonParser.parseReader(new InputStreamReader(is)).getAsJsonObject();
        } catch (Exception e) {
            config = new JsonObject();
            System.err.println("Erreur de chargement config.json: " + e.getMessage());
        }
    }

    public static String getEnv(String key) {
        return EnvLoader.get(key);
    }

    public static String getJsonConfig(String path) {
        String env = getEnv("ENV_MODE");
        String fullPath = env + "." + path;

        String[] keys = fullPath.split("\\.");
        JsonObject current = config;

        for (int i = 0; i < keys.length - 1; i++) {
            if (!current.has(keys[i])) return null;
            current = current.getAsJsonObject(keys[i]);
        }

        return current.has(keys[keys.length-1]) ?
                current.get(keys[keys.length-1]).getAsString() : null;
    }

    public static boolean isDebug() {
        return Boolean.parseBoolean(getJsonConfig("debug"));
    }
}