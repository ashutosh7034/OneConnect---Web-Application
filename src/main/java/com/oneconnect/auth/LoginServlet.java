package com.oneconnect.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oneconnect.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set CORS headers
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            if(sb.length() == 0) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Empty request body");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            JsonObject jsonRequest = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String email = jsonRequest.has("email") ? jsonRequest.get("email").getAsString() : null;
            String password = jsonRequest.has("password") ? jsonRequest.get("password").getAsString() : null;

            if (email == null || password == null || email.isEmpty() || password.isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Email and password are required");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                if (conn == null) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Database connection error");
                    out.print(gson.toJson(jsonResponse));
                    return;
                }

                String sql = "SELECT id, username FROM users WHERE email = ? AND password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, email);
                    stmt.setString(2, password); // Note: In production, passwords should be hashed
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            jsonResponse.addProperty("success", true);
                            jsonResponse.addProperty("userId", rs.getInt("id"));
                            jsonResponse.addProperty("username", rs.getString("username"));
                        } else {
                            jsonResponse.addProperty("success", false);
                            jsonResponse.addProperty("message", "Invalid email or password");
                        }
                    }
                }
            } catch (SQLException e) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Database error: " + e.getMessage());
            }

        } catch (Exception e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Server error: " + e.getMessage());
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }
    
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
