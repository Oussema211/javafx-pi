package com.example.Evenement.Dao;

import com.example.Evenement.Model.Place;
import com.example.auth.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlaceDAO {
    private final Connection connection;
    public PlaceDAO() {
        connection = MyDatabase.getInstance().getCnx();
    }
    public List<Place> getPlacesByEvenement(int evenementId) throws SQLException {
        List<Place> places = new ArrayList<>();
        String sql = "SELECT * FROM place WHERE evenement_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, evenementId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Place p = new Place();
                p.setId(rs.getInt("id"));
                p.setEvenementId(rs.getInt("evenement_id"));
                p.setNumeroLigne(rs.getInt("numero_ligne"));
                p.setNumeroColonne(rs.getInt("numero_colonne"));
                p.setStatut(rs.getString("statut"));
                p.setUserId(rs.getString("user_id"));
                places.add(p);
            }
        }
        return places;
    }
    public void reserverPlace(int placeId, String userId) throws SQLException {
        String sql = "UPDATE place SET statut = 'occupee', user_id = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userId);
            stmt.setInt(2, placeId);
            stmt.executeUpdate();
        }
    }
} 