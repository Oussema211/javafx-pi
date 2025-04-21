package com.example.cart;

import com.example.cart.model.OrderSummary;
import com.example.cart.service.CommandesHistoriquesDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

public class OrderHistoryManager {

    private static final ObservableList<OrderSummary> orderHistory = FXCollections.observableArrayList();

    static {
        // Charger tout l'historique depuis la BDD au démarrage
        List<OrderSummary> dbOrders = CommandesHistoriquesDAO.getAllCommandes();
        orderHistory.addAll(dbOrders);
    }

    // Ajouter une commande à l'historique ET dans la base
    public static void addOrder(OrderSummary order) {
        CommandesHistoriquesDAO.saveCommande(order); // Sauvegarder dans MySQL
        orderHistory.add(order);                     // Ajouter en mémoire observable
    }

    // Récupérer la liste observable
    public static ObservableList<OrderSummary> getOrderHistory() {
        return orderHistory;
    }

    // Recharger l'historique depuis la base (utilisé après login par exemple)
    public static void reloadHistoryFromDatabase() {
        orderHistory.clear();
        List<OrderSummary> dbOrders = CommandesHistoriquesDAO.getAllCommandes();
        orderHistory.addAll(dbOrders);
    }

    // Réinitialiser tout l'historique (mémoire + base) - pas forcément utilisé
    public static void clearHistory() {
        CommandesHistoriquesDAO.clearAllCommandes();
        orderHistory.clear();
    }
}
