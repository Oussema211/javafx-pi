package com.example.cart;

import com.example.auth.controller.DashboardFrontController;
import com.example.cart.model.CartItem;
import com.example.produit.model.Produit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Gère le panier de l'utilisateur (ajout, suppression, total, synchronisation).
 */
public class CartManager {
    private static final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    // Ajouter un produit au panier (ou augmenter la quantité si déjà présent)
    public static void addProduct(Produit produit) {
        for (CartItem item : cartItems) {
            if (item.getProduit().getId().equals(produit.getId())) {
                item.setQuantite(item.getQuantite() + 1);
                updateCartBadge();
                return;
            }
        }
        cartItems.add(new CartItem(produit, 1));
        updateCartBadge();
    }

    // Supprimer un produit du panier
    public static void removeProduct(Produit produit) {
        cartItems.removeIf(item -> item.getProduit().getId().equals(produit.getId()));
        updateCartBadge();
    }

    // Récupérer tous les éléments du panier
    public static ObservableList<CartItem> getCartItems() {
        return cartItems;
    }

    // Calculer le prix total du panier
    public static double getTotalPrice() {
        return cartItems.stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();
    }

    // Vider complètement le panier
    public static void clearCart() {
        cartItems.clear();
        updateCartBadge();
    }

    // Met à jour le badge du panier dans la navbar (si disponible)
    private static void updateCartBadge() {
        if (DashboardFrontController.getNavbarController() != null) {
            int totalQuantity = cartItems.stream()
                    .mapToInt(CartItem::getQuantite)
                    .sum();
            DashboardFrontController.getNavbarController().updateCartCount(totalQuantity);
        }
    }
}
