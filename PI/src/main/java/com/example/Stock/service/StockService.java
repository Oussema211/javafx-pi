package com.example.Stock.service;

import com.example.Stock.Model.Entrepot;
import com.example.Stock.Model.Stock;
import com.example.auth.utils.MyDatabase;
import com.example.auth.utils.SessionManager;
import com.example.produit.model.Categorie;
import com.example.produit.model.Produit;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class StockService {
    private final Connection cnx;

    public StockService() {
        this.cnx = MyDatabase.getInstance().getCnx();
        initializeDatabase();
    }

    // Initialisation de la base de données
    private void initializeDatabase() {
        try {
            // Supprimer les tables existantes (pour le développement)


            // Création des tables dans le bon ordre
            createEntrepotTable();
            createCategorieTable();
            createProduitTable();
            createStockTable();
            createStockEntrepotTable();

            // Création des index
            createIndexes();

            System.out.println("Base de données initialisée avec succès");
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'initialisation de la base de données: " + e.getMessage());
        }
    }



    private void createEntrepotTable() throws SQLException {
        String query = "CREATE TABLE entrepot (" +
                "id CHAR(36) PRIMARY KEY, " +
                "nom VARCHAR(100) NOT NULL, " +
                "adresse TEXT, " +
                "ville VARCHAR(100), " +
                "espace DECIMAL(10,2), " +
                "latitude DOUBLE, " +
                "longitude DOUBLE)";
        executeUpdate(query);
    }

    private void createCategorieTable() throws SQLException {
        String query = "CREATE TABLE categorie (" +
                "id CHAR(36) PRIMARY KEY, " +
                "nom VARCHAR(100) NOT NULL, " +
                "description TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        executeUpdate(query);
    }

    private void createProduitTable() throws SQLException {
        String query = "CREATE TABLE produit (" +
                "id CHAR(36) PRIMARY KEY, " +
                "nom VARCHAR(100) NOT NULL, " +
                "description TEXT, " +
                "quantite INTEGER DEFAULT 0, " +
                "prix_unitaire DECIMAL(10,2), " +
                "categorie_id CHAR(36), " +
                "user_id CHAR(36) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (categorie_id) REFERENCES categorie(id) ON DELETE SET NULL)";
        executeUpdate(query);
    }

    private void createStockTable() throws SQLException {
        String query = "CREATE TABLE stock (" +
                "id CHAR(36) PRIMARY KEY, " +
                "produit_id CHAR(36) NOT NULL, " +
                "date_entree TIMESTAMP NOT NULL, " +
                "date_sortie TIMESTAMP NULL, " +
                "seuil_alert INTEGER, " +
                "user_id CHAR(36) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (produit_id) REFERENCES produit(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (user_id) REFERENCES user(id))";
        executeUpdate(query);
    }

    private void createStockEntrepotTable() throws SQLException {
        String query = "CREATE TABLE stock_entrepot (" +
                "stock_id CHAR(36) NOT NULL, " +
                "entrepot_id CHAR(36) NOT NULL, " +
                "PRIMARY KEY (stock_id, entrepot_id), " +
                "FOREIGN KEY (stock_id) REFERENCES stock(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (entrepot_id) REFERENCES entrepot(id) ON DELETE CASCADE)";
        executeUpdate(query);
    }

    private void createIndexes() throws SQLException {
        String[] indexes = {
                "CREATE INDEX idx_stock_produit ON stock(produit_id)",
                "CREATE INDEX idx_stock_user ON stock(user_id)",
                "CREATE INDEX idx_stock_date_entree ON stock(date_entree)",
                "CREATE INDEX idx_stock_entrepot_stock ON stock_entrepot(stock_id)",
                "CREATE INDEX idx_stock_entrepot_entrepot ON stock_entrepot(entrepot_id)"
        };

        for (String index : indexes) {
            executeUpdate(index);
        }
    }

    private void executeUpdate(String query) throws SQLException {
        try (Statement stmt = cnx.createStatement()) {
            stmt.executeUpdate(query);
        }
    }

    // CRUD Operations

    public boolean addStock(Stock stock) {
        try {
            cnx.setAutoCommit(false);

            // 1. Insérer le stock
            String query = "INSERT INTO stock (id, produit_id, date_entree, seuil_alert, user_id) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement ps = cnx.prepareStatement(query)) {
                ps.setString(1, stock.getId().toString());
                ps.setString(2, stock.getProduitId().toString());
                ps.setTimestamp(3, Timestamp.valueOf(stock.getDateEntree()));
                ps.setInt(4, stock.getSeuilAlert());
                ps.setString(5, stock.getUserId().toString());

                int affectedRows = ps.executeUpdate();
                if (affectedRows == 0) {
                    cnx.rollback();
                    return false;
                }
            }

            // 2. Ajouter les relations avec les entrepôts
            if (stock.getEntrepotIds() != null && !stock.getEntrepotIds().isEmpty()) {
                String relationQuery = "INSERT INTO stock_entrepot (stock_id, entrepot_id) VALUES (?, ?)";
                try (PreparedStatement ps = cnx.prepareStatement(relationQuery)) {
                    for (UUID entrepotId : stock.getEntrepotIds()) {
                        ps.setString(1, stock.getId().toString());
                        ps.setString(2, entrepotId.toString());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            cnx.commit();
            return true;
        } catch (SQLException e) {
            try {
                cnx.rollback();
            } catch (SQLException ex) {
                System.err.println("Erreur lors du rollback: " + ex.getMessage());
            }
            System.err.println("Erreur lors de l'ajout du stock: " + e.getMessage());
            return false;
        } finally {
            try {
                cnx.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Erreur lors du rétablissement de autoCommit: " + e.getMessage());
            }
        }
    }

    public List<Stock> getAllStocks() {
        List<Stock> stocks = new ArrayList<>();
        String query = "SELECT * FROM stock WHERE user_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, SessionManager.getInstance().getLoggedInUser().getId().toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Stock stock = mapResultSetToStock(rs);
                loadEntrepotIds(stock);
                stocks.add(stock);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des stocks: " + e.getMessage());
        }
        return stocks;
    }

    public Stock getStockById(UUID id) {
        String query = "SELECT * FROM stock WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, id.toString());
            ps.setString(2, SessionManager.getInstance().getLoggedInUser().getId().toString());

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Stock stock = mapResultSetToStock(rs);
                loadEntrepotIds(stock);
                return stock;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du stock: " + e.getMessage());
        }
        return null;
    }

    public boolean updateStock(Stock stock) {
        try {
            cnx.setAutoCommit(false);

            // 1. Mettre à jour les informations de base du stock
            String updateQuery = "UPDATE stock SET produit_id = ?, date_entree = ?, " +
                    "date_sortie = ?, seuil_alert = ? " +
                    "WHERE id = ? AND user_id = ?";

            try (PreparedStatement ps = cnx.prepareStatement(updateQuery)) {
                ps.setString(1, stock.getProduitId().toString());
                ps.setTimestamp(2, Timestamp.valueOf(stock.getDateEntree()));

                if (stock.getDateSortie() != null) {
                    ps.setTimestamp(3, Timestamp.valueOf(stock.getDateSortie()));
                } else {
                    ps.setNull(3, Types.TIMESTAMP);
                }

                ps.setInt(4, stock.getSeuilAlert());
                ps.setString(5, stock.getId().toString());
                ps.setString(6, stock.getUserId().toString());

                int rowsUpdated = ps.executeUpdate();
                if (rowsUpdated == 0) {
                    cnx.rollback();
                    return false;
                }
            }

            // 2. Mettre à jour les relations avec les entrepôts
            updateStockEntrepotRelations(stock);

            cnx.commit();
            return true;
        } catch (SQLException e) {
            try {
                cnx.rollback();
            } catch (SQLException ex) {
                System.err.println("Erreur lors du rollback: " + ex.getMessage());
            }
            System.err.println("Erreur lors de la mise à jour du stock: " + e.getMessage());
            return false;
        } finally {
            try {
                cnx.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Erreur lors du rétablissement de autoCommit: " + e.getMessage());
            }
        }
    }

    private void updateStockEntrepotRelations(Stock stock) throws SQLException {
        // 1. Supprimer les anciennes relations
        String deleteQuery = "DELETE FROM stock_entrepot WHERE stock_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(deleteQuery)) {
            ps.setString(1, stock.getId().toString());
            ps.executeUpdate();
        }

        // 2. Ajouter les nouvelles relations
        if (stock.getEntrepotIds() != null && !stock.getEntrepotIds().isEmpty()) {
            String insertQuery = "INSERT INTO stock_entrepot (stock_id, entrepot_id) VALUES (?, ?)";
            try (PreparedStatement ps = cnx.prepareStatement(insertQuery)) {
                for (UUID entrepotId : stock.getEntrepotIds()) {
                    ps.setString(1, stock.getId().toString());
                    ps.setString(2, entrepotId.toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }
    public boolean deleteStock(UUID id) {
        try {
            cnx.setAutoCommit(false);

            // 1. Supprimer les relations avec les entrepôts
            String deleteRelations = "DELETE FROM stock_entrepot WHERE stock_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(deleteRelations)) {
                ps.setString(1, id.toString());
                ps.executeUpdate();
            }

            // 2. Supprimer le stock
            String query = "DELETE FROM stock WHERE id = ? AND user_id = ?";
            try (PreparedStatement ps = cnx.prepareStatement(query)) {
                ps.setString(1, id.toString());
                ps.setString(2, SessionManager.getInstance().getLoggedInUser().getId().toString());
                int affectedRows = ps.executeUpdate();

                cnx.commit();
                return affectedRows > 0;
            }
        } catch (SQLException e) {
            try {
                cnx.rollback();
            } catch (SQLException ex) {
                System.err.println("Erreur lors du rollback: " + ex.getMessage());
            }
            System.err.println("Erreur lors de la suppression du stock: " + e.getMessage());
            return false;
        } finally {
            try {
                cnx.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Erreur lors du rétablissement de autoCommit: " + e.getMessage());
            }
        }
    }

    // Méthodes utilitaires

    private Stock mapResultSetToStock(ResultSet rs) throws SQLException {
        Stock stock = new Stock();
        stock.setId(UUID.fromString(rs.getString("id")));
        stock.setProduitId(UUID.fromString(rs.getString("produit_id")));
        stock.setDateEntree(rs.getTimestamp("date_entree").toLocalDateTime());

        if (rs.getTimestamp("date_sortie") != null) {
            stock.setDateSortie(rs.getTimestamp("date_sortie").toLocalDateTime());
        }

        stock.setSeuilAlert(rs.getInt("seuil_alert"));
        stock.setUserId(UUID.fromString(rs.getString("user_id")));
        return stock;
    }

    private void loadEntrepotIds(Stock stock) throws SQLException {
        String query = "SELECT entrepot_id FROM stock_entrepot WHERE stock_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, stock.getId().toString());
            ResultSet rs = ps.executeQuery();

            Set<UUID> entrepotIds = new HashSet<>();
            while (rs.next()) {
                entrepotIds.add(UUID.fromString(rs.getString("entrepot_id")));
            }
            stock.setEntrepotIds(entrepotIds);
        }
    }

    public List<Stock> searchStocks(String searchTerm) {
        List<Stock> stocks = new ArrayList<>();
        String query = "SELECT s.* FROM stock s " +
                "JOIN produit p ON s.produit_id = p.id " +
                "WHERE s.user_id = ? AND (p.nom LIKE ? OR p.description LIKE ?)";

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            UUID userId = SessionManager.getInstance().getLoggedInUser().getId();
            ps.setString(1, userId.toString());
            ps.setString(2, "%" + searchTerm + "%");
            ps.setString(3, "%" + searchTerm + "%");

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Stock stock = mapResultSetToStock(rs);
                loadEntrepotIds(stock);
                stocks.add(stock);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la recherche des stocks: " + e.getMessage());
        }
        return stocks;
    }

    public List<Stock> getAllStocksWithDetails() {
        List<Stock> stocks = new ArrayList<>();
        String query = "SELECT s.*, p.nom AS produit_nom, p.description AS produit_description, " +
                "p.quantite AS produit_quantite, p.image_name AS produit_image, " +
                "c.nom AS categorie_nom " +
                "FROM stock s " +
                "JOIN produit p ON s.produit_id = p.id " +
                "LEFT JOIN categorie c ON p.categorie_id = c.id " +
                "WHERE s.user_id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(query)) {
            ps.setString(1, SessionManager.getInstance().getLoggedInUser().getId().toString());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Stock stock = mapResultSetToStock(rs);

                // Créer et remplir l'objet Produit
                Produit produit = new Produit();
                produit.setId(UUID.fromString(rs.getString("produit_id")));
                produit.setNom(rs.getString("produit_nom"));
                produit.setDescription(rs.getString("produit_description"));
                produit.setQuantite(rs.getInt("produit_quantite"));
                produit.setImageName(rs.getString("produit_image"));

                // Créer et remplir la catégorie si elle existe
                if (rs.getString("categorie_nom") != null) {
                    Categorie categorie = new Categorie();
                    categorie.setNom(rs.getString("categorie_nom"));
                    produit.setCategory(categorie);
                }

                stock.setProduit(produit);
                loadEntrepotIds(stock);
                stocks.add(stock);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des stocks avec détails: " + e.getMessage());
        }

        return stocks;
    }
    public List<Stock> getFilteredStocks(String searchText, String category, String entrepot, LocalDate date) {
        return getAllStocksWithDetails().stream()
                .filter(stock -> {
                    // Filtre par texte
                    if (searchText != null && !searchText.isEmpty()) {
                        Produit p = stock.getProduit();
                        if (p == null) return false;

                        boolean matches = p.getNom().toLowerCase().contains(searchText.toLowerCase());
                        if (!matches && p.getDescription() != null) {
                            matches = p.getDescription().toLowerCase().contains(searchText.toLowerCase());
                        }
                        if (!matches) return false;
                    }

                    // Filtre par catégorie
                    if (category != null && !category.equals("Toutes catégories")) {
                        Produit p = stock.getProduit();
                        if (p == null || p.getCategory() == null ||
                                !p.getCategory().getNom().equals(category)) {
                            return false;
                        }
                    }
                    EntrepotService entrepotService = new EntrepotService();
                    // Filtre par entrepôt
                    if (entrepot != null && !entrepot.equals("Tous entrepôts")) {
                        boolean found = stock.getEntrepotIds().stream()
                                .anyMatch(id -> {
                                    Entrepot e = entrepotService.getEntrepotById(id);
                                    return e != null && e.getNom().equals(entrepot);
                                });
                        if (!found) return false;
                    }

                    // Filtre par date
                    if (date != null) {
                        if (!stock.getDateEntree().toLocalDate().equals(date)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());
    }
}