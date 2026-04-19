package com.oneconnect.profile;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(urlPatterns = {"/api/profile", "/api/profile/password"})
public class ProfileServlet extends HttpServlet {
    private final ProfileDAO profileDAO = new ProfileDAO();
    private final Gson gson = new Gson();

    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, PUT, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String userIdStr = request.getParameter("userId");
        PrintWriter out = response.getWriter();

        if (userIdStr != null) {
            try {
                int userId = Integer.parseInt(userIdStr);
                JsonObject user = profileDAO.getUserById(userId);
                if (user.has("name")) {
                    user.addProperty("success", true);
                    out.print(gson.toJson(user));
                } else {
                    JsonObject err = new JsonObject();
                    err.addProperty("success", false);
                    err.addProperty("message", "User not found");
                    out.print(gson.toJson(err));
                }
            } catch (NumberFormatException e) {
                JsonObject err = new JsonObject();
                err.addProperty("success", false);
                err.addProperty("message", "Invalid user ID");
                out.print(gson.toJson(err));
            }
        } else {
            JsonObject err = new JsonObject();
            err.addProperty("success", false);
            err.addProperty("message", "Missing user ID");
            out.print(gson.toJson(err));
        }
        out.flush();
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();
        String pathInfo = request.getServletPath();

        try {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = request.getReader();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            JsonObject jsonRequest = JsonParser.parseString(sb.toString()).getAsJsonObject();
            int userId = jsonRequest.has("userId") ? jsonRequest.get("userId").getAsInt() : -1;

            if (userId == -1) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "User ID is required");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            if ("/api/profile/password".equals(pathInfo)) {
                String oldPassword = jsonRequest.has("oldPassword") ? jsonRequest.get("oldPassword").getAsString() : null;
                String newPassword = jsonRequest.has("newPassword") ? jsonRequest.get("newPassword").getAsString() : null;

                if (oldPassword != null && newPassword != null) {
                    boolean success = profileDAO.changePassword(userId, oldPassword, newPassword);
                    jsonResponse.addProperty("success", success);
                    jsonResponse.addProperty("message", success ? "Password changed successfully" : "Invalid old password");
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Missing password fields");
                }
            } else {
                String name = jsonRequest.has("name") ? jsonRequest.get("name").getAsString() : null;
                String email = jsonRequest.has("email") ? jsonRequest.get("email").getAsString() : null;

                if (name != null && email != null) {
                    boolean success = profileDAO.updateProfile(userId, name, email);
                    jsonResponse.addProperty("success", success);
                    jsonResponse.addProperty("message", success ? "Profile updated successfully" : "Failed to update profile");
                } else {
                    jsonResponse.addProperty("success", false);
                    jsonResponse.addProperty("message", "Missing name or email");
                }
            }
        } catch (Exception e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Error processing request");
            e.printStackTrace();
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }
}
