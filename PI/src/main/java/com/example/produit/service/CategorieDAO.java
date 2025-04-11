package com.example.produit.service;

import com.example.produit.model.Categorie;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategorieDAO {
    private static final String URL = "jdbc:mysql://localhost:3306/pidevv";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static List<Categorie> getAllCategories() {
        List<Categorie> categories = new ArrayList<>();
        String query = "SELECT * FROM categorie";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Categorie categorie = mapResultSetToCategorie(rs);
                categories.add(categorie);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public static Categorie getCategoryById(int id) {
        String query = "SELECT * FROM categorie WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToCategorie(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveCategory(Categorie categorie) {
        String query = "INSERT INTO categorie (parent_id, nom, slug, lft, rgt, lvl, img_url, description) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {

            setCommonParameters(pstmt, categorie);

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    categorie.setId(generatedKeys.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void updateCategory(Categorie categorie) {
        String query = "UPDATE categorie SET parent_id = ?, nom = ?, slug = ?, "
                + "lft = ?, rgt = ?, lvl = ?, img_url = ?, description = ? "
                + "WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            setCommonParameters(pstmt, categorie);
            pstmt.setInt(9, categorie.getId());

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deleteCategory(int id) {
        String query = "DELETE FROM categorie WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Categorie mapResultSetToCategorie(ResultSet rs) throws SQLException {
        Categorie categorie = new Categorie();
        categorie.setId(rs.getInt("id"));
        //categorie.setParentId(rs.getObject("parent_id", Integer.class));
        categorie.setNom(rs.getString("nom"));
        //categorie.setSlug(rs.getString("slug"));
        //categorie.setLft(rs.getObject("lft", Integer.class));
        //categorie.setRgt(rs.getObject("rgt", Integer.class));
        // categorie.setLvl(rs.getObject("lvl", Integer.class));
        //categorie.setImgUrl(rs.getString("img_url"));
        categorie.setDescription(rs.getString("description"));
        return categorie;
    }

    private static void setCommonParameters(PreparedStatement pstmt, Categorie categorie) throws SQLException {
        pstmt.setObject(1, categorie.getParentId(), Types.INTEGER);
        pstmt.setString(2, categorie.getNom());
        pstmt.setString(3, categorie.getSlug());
        pstmt.setObject(4, categorie.getLft(), Types.INTEGER);
        pstmt.setObject(5, categorie.getRgt(), Types.INTEGER);
        pstmt.setObject(6, categorie.getLvl(), Types.INTEGER);
        pstmt.setString(7, categorie.getImgUrl());
        pstmt.setString(8, categorie.getDescription());
    }

    // Additional methods for tree operations
    public static List<Categorie> getChildCategories(int parentId) {
        List<Categorie> categories = new ArrayList<>();
        String query = "SELECT * FROM categorie WHERE parent_id = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, parentId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                categories.add(mapResultSetToCategorie(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public static List<Categorie> getRootCategories() {
        return getChildCategories(null);
    }

    private static List<Categorie> getChildCategories(Integer parentId) {
        List<Categorie> categories = new ArrayList<>();
        String query = "SELECT * FROM categorie WHERE parent_id " +
                (parentId == null ? "IS NULL" : "= ?");

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            if (parentId != null) {
                pstmt.setInt(1, parentId);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                categories.add(mapResultSetToCategorie(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }
}
