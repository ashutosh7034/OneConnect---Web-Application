package com.oneconnect.profile;

import com.google.gson.JsonObject;
import com.oneconnect.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProfileDAO {

    public JsonObject getUserById(int userId) {
        JsonObject user = new JsonObject();
        String sql = "SELECT name, email, username FROM users WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    user.addProperty("name", rs.getString("name"));
                    user.addProperty("email", rs.getString("email"));
                    user.addProperty("username", rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return user;
    }

    public boolean updateProfile(int userId, String name, String email) {
        String sql = "UPDATE users SET name = ?, email = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, email);
            stmt.setInt(3, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        // First verify old password
        String verifySql = "SELECT id FROM users WHERE id = ? AND password = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement verifyStmt = conn.prepareStatement(verifySql)) {
            verifyStmt.setInt(1, userId);
            verifyStmt.setString(2, oldPassword);
            try (ResultSet rs = verifyStmt.executeQuery()) {
                if (!rs.next()) {
                    return false; // Old password doesn't match
                }
            }
            
            // If valid, update password
            String updateSql = "UPDATE users SET password = ? WHERE id = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, newPassword);
                updateStmt.setInt(2, userId);
                return updateStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
