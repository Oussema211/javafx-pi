package com.example.cart;

import com.example.cart.model.OrderSummary;
import com.example.cart.service.CommandesHistoriquesDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * Gère l'historique des commandes (en mémoire + en base de données).
 */
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

    // Récupérer la liste observable de l'historique
    public static ObservableList<OrderSummary> getOrderHistory() {
        return orderHistory;
    }

    // Recharger depuis la base (ex : après authentification)
    public static void reloadHistoryFromDatabase() {
        orderHistory.clear();
        List<OrderSummary> dbOrders = CommandesHistoriquesDAO.getAllCommandes();
        orderHistory.addAll(dbOrders);
    }

    // Supprimer tout l'historique (utilisé rarement)
    public static void clearHistory() {
        CommandesHistoriquesDAO.clearAllCommandes();
        orderHistory.clear();
    }
}
