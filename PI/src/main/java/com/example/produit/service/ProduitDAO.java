package com.example.produit.service;

import com.example.produit.model.Categorie;
import com.example.produit.model.Produit;

import java.nio.ByteBuffer;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProduitDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/PIDES";
    private static final String USER = "root";
    private static final String PASSWORD = "";
    private static final Logger LOGGER = Logger.getLogger(ProduitDAO.class.getName());

    // Static block to initialize the table (matches existing schema)
    static {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS produit (" +
                    "id BINARY(16) PRIMARY KEY, " +
                    "categorie_id INT, " +
                    "nom VARCHAR(255) NOT NULL, " +
                    "description LONGTEXT, " +
                    "prix_unitaire DECIMAL(10,2) NOT NULL, " +
                    "quantite INT NOT NULL, " +
                    "date_creation DATETIME NOT NULL, " +
                    "image_name VARCHAR(255), " +
                    "user_id VARCHAR(36) NOT NULL, " +
                    "rate DECIMAL(2,1), " +
                    "FOREIGN KEY (categorie_id) REFERENCES categorie(id) ON DELETE SET NULL)";
            stmt.execute(sql);
            LOGGER.info("Produit table initialized successfully.");
        } catch (SQLException e) {
            LOGGER.severe("Error initializing produit table: " + e.getMessage());
            throw new RuntimeException("Failed to initialize produit table: " + e.getMessage());
        }
    }

    private static byte[] uuidToBinary(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private static UUID binaryToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long mostSigBits = bb.getLong();
        long leastSigBits = bb.getLong();
        return new UUID(mostSigBits, leastSigBits);
    }

    public static List<Produit> getAllProducts() {
        List<Produit> products = new ArrayList<>();
        String query = "SELECT id, nom, description, prix_unitaire, quantite, date_creation, image_name, categorie_id, user_id, rate FROM produit";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                try {
                    Produit product = mapResultSetToProduit(rs);
                    products.add(product);
                } catch (Exception e) {
                    LOGGER.severe("Error processing product row: " + e.getMessage());
                }
            }
            LOGGER.info("getAllProducts: Retrieved " + products.size() + " products");
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in getAllProducts: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve products: " + e.getMessage());
        }
        return products;
    }

    public static Produit getProduitById(String id) {
        if (id == null || id.trim().isEmpty()) {
            LOGGER.warning("Product ID is null or empty, cannot fetch product.");
            return null;
        }
        String query = "SELECT p.id, p.nom, p.description, p.prix_unitaire, p.quantite, p.date_creation, p.image_name, p.categorie_id, p.user_id, p.rate, " +
                "c.id AS cat_id, c.nom AS cat_nom, c.img_url AS cat_img_url, c.parent_id AS cat_parent_id " +
                "FROM produit p LEFT JOIN categorie c ON p.categorie_id = c.id " +
                "WHERE p.id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setBytes(1, uuidToBinary(UUID.fromString(id)));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Produit product = new Produit();
                    product.setId(binaryToUuid(rs.getBytes("id")).toString());
                    product.setNom(rs.getString("nom"));
                    product.setDescription(rs.getString("description"));
                    product.setPrixUnitaire(rs.getFloat("prix_unitaire"));
                    product.setQuantite(rs.getInt("quantite"));
                    product.setDateCreation(rs.getTimestamp("date_creation") != null ?
                            rs.getTimestamp("date_creation").toLocalDateTime() : null);
                    product.setImageName(rs.getString("image_name"));
                    String userIdStr = rs.getString("user_id");
                    product.setUserId(userIdStr != null ? UUID.fromString(userIdStr) : null);
                    product.setRate(rs.getObject("rate") != null ? rs.getFloat("rate") : null);

                    int categoryId = rs.getInt("cat_id");
                    if (!rs.wasNull()) {
                        Categorie category = new Categorie();
                        category.setId(categoryId);
                        category.setNom(rs.getString("cat_nom"));
                        category.setImgUrl(rs.getString("cat_img_url"));
                        int parentId = rs.getInt("cat_parent_id");
                        if (!rs.wasNull()) {
                            Categorie parent = CategorieDAO.getCategoryById(parentId);
                            category.setParent(parent);
                        }
                        product.setCategory(category);
                    }
                    return product;
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in getProduitById: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve product: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.severe("Invalid UUID format for product ID: " + id + " - " + e.getMessage());
            return null;
        }
        return null;
    }

    public static boolean saveProduct(Produit product) {
        if (product == null || product.getId() == null) {
            LOGGER.warning("Product or product ID is null, cannot save product.");
            return false;
        }
        if (product.getUserId() == null) {
            LOGGER.warning("User ID is null for product ID: " + product.getId() + ", cannot save product.");
            return false;
        }

        String query = "INSERT INTO produit (id, categorie_id, nom, description, prix_unitaire, quantite, date_creation, image_name, user_id, rate) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setBytes(1, uuidToBinary(UUID.fromString(product.getId())));
            pstmt.setObject(2, product.getCategory() != null ? product.getCategory().getId() : null);
            pstmt.setString(3, product.getNom() != null ? product.getNom() : "");
            pstmt.setString(4, product.getDescription() != null ? product.getDescription() : "");
            pstmt.setFloat(5, product.getPrixUnitaire());
            pstmt.setInt(6, product.getQuantite());
            pstmt.setTimestamp(7, product.getDateCreation() != null ? Timestamp.valueOf(product.getDateCreation()) : null);
            pstmt.setString(8, product.getImageName() != null ? product.getImageName() : null);
            pstmt.setString(9, product.getUserId().toString());
            pstmt.setObject(10, product.getRate(), Types.DECIMAL);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("saveProduct: Inserted product with ID " + product.getId() + ", rows affected: " + rowsAffected);
                return true;
            }
            LOGGER.warning("saveProduct: No rows affected for product ID " + product.getId());
            return false;
        } catch (SQLException e) {
            if (e.getMessage().contains("FOREIGN KEY")) {
                LOGGER.severe("Foreign key error: Category ID " +
                        (product.getCategory() != null ? product.getCategory().getId() : "null") + " does not exist.");
            } else {
                LOGGER.severe("SQL Error in saveProduct: " + e.getMessage());
            }
            return false;
        }
    }

    public static boolean updateProduct(Produit product) {
        if (product == null || product.getId() == null) {
            LOGGER.warning("Product or product ID is null, cannot update product.");
            return false;
        }
        if (product.getUserId() == null) {
            LOGGER.warning("User ID is null for product ID: " + product.getId() + ", cannot update product.");
            return false;
        }

        String query = "UPDATE produit SET categorie_id = ?, nom = ?, description = ?, prix_unitaire = ?, quantite = ?, image_name = ?, user_id = ?, rate = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setObject(1, product.getCategory() != null ? product.getCategory().getId() : null);
            pstmt.setString(2, product.getNom() != null ? product.getNom() : "");
            pstmt.setString(3, product.getDescription() != null ? product.getDescription() : "");
            pstmt.setFloat(4, product.getPrixUnitaire());
            pstmt.setInt(5, product.getQuantite());
            pstmt.setString(6, product.getImageName() != null ? product.getImageName() : null);
            pstmt.setString(7, product.getUserId().toString());
            pstmt.setBytes(8, uuidToBinary(UUID.fromString(product.getId())));

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("updateProduct: Updated product with ID " + product.getId() + ", rows affected: " + rowsAffected);
                return true;
            }
            LOGGER.warning("updateProduct: No rows affected for product ID " + product.getId());
            return false;
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in updateProduct: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteProduct(String id) {
        if (id == null) {
            LOGGER.warning("Product ID is null, cannot delete product.");
            return false;
        }
        String query = "DELETE FROM produit WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setBytes(1, uuidToBinary(UUID.fromString(id)));
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("deleteProduct: Deleted product with ID " + id + ", rows affected: " + rowsAffected);
                return true;
            }
            LOGGER.warning("deleteProduct: No rows affected for product ID " + id);
            return false;
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in deleteProduct: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            LOGGER.severe("Invalid UUID format for product ID: " + id + " - " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteProducts(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            LOGGER.info("deleteProducts: No IDs provided, skipping deletion");
            return false;
        }

        String query = "DELETE FROM produit WHERE id IN (" + ids.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            for (int i = 0; i < ids.size(); i++) {
                pstmt.setBytes(i + 1, uuidToBinary(UUID.fromString(ids.get(i))));
            }

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("deleteProducts: Deleted " + rowsAffected + " products with IDs " + ids);
                return true;
            }
            LOGGER.warning("deleteProducts: No rows affected for product IDs " + ids);
            return false;
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in deleteProducts: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            LOGGER.severe("Invalid UUID format in product IDs: " + e.getMessage());
            return false;
        }
    }

    public static Map<Date, Integer> getNewProductsOverTime(LocalDate startDate, LocalDate endDate, String category) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Invalid date range");
        }
        Map<Date, Integer> productData = new LinkedHashMap<>();
        String query = "SELECT DATE(p.date_creation) as day, COUNT(*) as count " +
                "FROM produit p " +
                "LEFT JOIN categorie c ON p.categorie_id = c.id " +
                "WHERE p.date_creation >= ? AND p.date_creation <= ? " +
                (category != null && !category.equals("Toutes") ? "AND c.nom = ?" : "") +
                " GROUP BY DATE(p.date_creation) ORDER BY day";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            if (category != null && !category.equals("Toutes")) {
                ps.setString(3, category);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    productData.put(rs.getDate("day"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            LOGGER.severe("SQL Error in getNewProductsOverTime: " + e.getMessage());
            throw new RuntimeException("Failed to retrieve product data: " + e.getMessage());
        }
        return productData;
    }

    private static Produit mapResultSetToProduit(ResultSet rs) throws SQLException {
        Produit product = new Produit();
        product.setId(binaryToUuid(rs.getBytes("id")).toString());
        product.setNom(rs.getString("nom"));
        product.setDescription(rs.getString("description"));
        product.setPrixUnitaire(rs.getFloat("prix_unitaire"));
        product.setQuantite(rs.getInt("quantite"));
        product.setDateCreation(rs.getTimestamp("date_creation") != null ?
                rs.getTimestamp("date_creation").toLocalDateTime() : null);
        product.setImageName(rs.getString("image_name"));
        String userIdStr = rs.getString("user_id");
        product.setUserId(userIdStr != null ? UUID.fromString(userIdStr) : null);
        product.setRate(rs.getObject("rate") != null ? rs.getFloat("rate") : null);

        int categoryId = rs.getInt("categorie_id");
        if (!rs.wasNull()) {
            Categorie category = CategorieDAO.getCategoryById(categoryId);
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
        return product;
    }
}