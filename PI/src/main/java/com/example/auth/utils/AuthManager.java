package com.example.auth.utils;

import com.example.auth.model.User;
import java.util.UUID;

/**
 * Classe statique pour stocker l'utilisateur connecté dans toute l'application.
 */
public class AuthManager {

    private static User currentUser;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static UUID getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public static void clear() {
        currentUser = null;
    }
}
