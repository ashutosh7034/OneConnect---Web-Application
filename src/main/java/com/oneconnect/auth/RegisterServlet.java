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
import java.sql.SQLException;

@WebServlet("/api/register")
public class RegisterServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
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
            String name = jsonRequest.has("name") ? jsonRequest.get("name").getAsString() : null;
            String email = jsonRequest.has("email") ? jsonRequest.get("email").getAsString() : null;
            String username = jsonRequest.has("username") ? jsonRequest.get("username").getAsString() : null;
            String password = jsonRequest.has("password") ? jsonRequest.get("password").getAsString() : null;

            if (name == null || email == null || username == null || password == null ||
                name.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "All fields are required");
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

                String sql = "INSERT INTO users (name, email, username, password) VALUES (?, ?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, name);
                    stmt.setString(2, email);
                    stmt.setString(3, username);
                    stmt.setString(4, password); // Note: In production, passwords should be hashed
                    
                    int rowsAffected = stmt.executeUpdate();
                    if (rowsAffected > 0) {
                        jsonResponse.addProperty("success", true);
                        jsonResponse.addProperty("message", "Registration successful");
                    } else {
                        jsonResponse.addProperty("success", false);
                        jsonResponse.addProperty("message", "Registration failed");
                    }
                }
            } catch (SQLException e) {
                // Check for duplicate entry error (e.g. email or username)
                if (e.getErrorCode() == 1062) {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Email or username already exists");
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Database error: " + e.getMessage());
                }
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
