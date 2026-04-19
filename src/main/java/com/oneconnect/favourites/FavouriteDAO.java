package com.oneconnect.favourites;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oneconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FavouriteDAO {

    public boolean addFavourite(int userId, String serviceName, String serviceUrl) {
        String sql = "INSERT INTO favourites (user_id, service_name, service_url) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, serviceName);
            stmt.setString(3, serviceUrl);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean removeFavourite(int userId, String serviceName) {
        String sql = "DELETE FROM favourites WHERE user_id = ? AND service_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setString(2, serviceName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public JsonArray getFavouritesByUser(int userId) {
        JsonArray array = new JsonArray();
        String sql = "SELECT service_name, service_url FROM favourites WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("name", rs.getString("service_name"));
                    obj.addProperty("url", rs.getString("service_url"));
                    array.add(obj);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return array;
    }
}
