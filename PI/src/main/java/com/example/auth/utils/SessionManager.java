package com.example.auth.utils;
import com.example.Stock.service.StockService;
import com.example.auth.model.User;
import com.example.auth.service.AuthService;

import java.io.*;
import java.util.UUID;

public class SessionManager {
    private static SessionManager instance;
    private User loggedInUser;
    private AuthService authService = new AuthService();
    private StockService stockService = new StockService();
    private static final String SESSION_FILE = "session.dat";

    public SessionManager() {
        // Load session on startup
        loadSession();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setLoggedInUser(User user) {
        this.loggedInUser = user;
        saveSession();
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public boolean isLoggedIn() {
        return loggedInUser != null;
    }

    public void clearSession() {
        loggedInUser = null;
        saveSession();
    }

    private void saveSession() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SESSION_FILE))) {
            if (loggedInUser != null) {
                oos.writeObject(loggedInUser.getId().toString());
            } else {
                oos.writeObject(null);
            }
        } catch (IOException e) {
            System.err.println("Error saving session: " + e.getMessage());
        }
    }

    private void loadSession() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SESSION_FILE))) {
            String userId = (String) ois.readObject();
            if (userId != null) {
                // Fetch user from database using ID
                loggedInUser = authService.getUserById(UUID.fromString(userId));
            }
        } catch (FileNotFoundException e) {
            // No session file yet, ignore
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading session: " + e.getMessage());
        }
    }


}