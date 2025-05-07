package com.example.cart;

import com.example.auth.controller.DashboardFrontController;
import com.example.cart.model.CartItem;
import com.example.produit.model.Produit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CartManager {
    private static final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    public static void addProduct(Produit produit) {
        addProduct(produit, 1);
    }

    public static void addProduct(Produit produit, int quantity) {
        if (produit == null) {
            throw new IllegalArgumentException("Produit cannot be null");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        if (quantity > produit.getQuantite()) {
            throw new IllegalArgumentException(
                    "Requested quantity (" + quantity + ") exceeds available stock (" + produit.getQuantite() + ") for " + produit.getNom()
            );
        }

        for (CartItem item : cartItems) {
            if (item.getProduit().getId().equals(produit.getId())) {
                item.setQuantite(item.getQuantite() + quantity);
                updateCartBadge();
                return;
            }
        }
        cartItems.add(new CartItem(produit, quantity));
        updateCartBadge();
    }

    public static void removeProduct(Produit produit) {
        cartItems.removeIf(item -> item.getProduit().getId().equals(produit.getId()));
        updateCartBadge();
    }

    public static ObservableList<CartItem> getCartItems() {
        return cartItems;
    }

    public static double getTotalPrice() {
        return cartItems.stream()
                .mapToDouble(CartItem::getTotalPrice)
                .sum();
    }

    public static void clearCart() {
        cartItems.clear();
        updateCartBadge();
    }

    private static void updateCartBadge() {
        if (DashboardFrontController.getNavbarController() != null) {
            int totalQuantity = cartItems.stream()
                    .mapToInt(CartItem::getQuantite)
                    .sum();
            DashboardFrontController.getNavbarController().updateCartCount(totalQuantity);
        }
    }
}