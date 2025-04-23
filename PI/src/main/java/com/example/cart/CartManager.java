package com.example.cart;

import com.example.auth.controller.DashboardFrontController;
import com.example.cart.model.CartItem;
import com.example.produit.model.Produit;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class CartManager {
    private static final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

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