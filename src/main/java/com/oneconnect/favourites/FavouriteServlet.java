package com.oneconnect.favourites;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

@WebServlet("/api/favourites")
public class FavouriteServlet extends HttpServlet {
    private final FavouriteDAO favouriteDAO = new FavouriteDAO();
    private final Gson gson = new Gson();

    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
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
                JsonArray favourites = favouriteDAO.getFavouritesByUser(userId);
                out.print(gson.toJson(favourites));
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeaders(response);
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

            JsonObject jsonRequest = JsonParser.parseString(sb.toString()).getAsJsonObject();
            int userId = jsonRequest.has("userId") ? jsonRequest.get("userId").getAsInt() : -1;
            String serviceName = jsonRequest.has("serviceName") ? jsonRequest.get("serviceName").getAsString() : null;
            String serviceUrl = jsonRequest.has("serviceUrl") ? jsonRequest.get("serviceUrl").getAsString() : null;

            if (userId != -1 && serviceName != null && serviceUrl != null) {
                boolean success = favouriteDAO.addFavourite(userId, serviceName, serviceUrl);
                jsonResponse.addProperty("success", success);
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Missing parameters");
            }
        } catch (Exception e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Error adding favourite");
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeaders(response);
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

            JsonObject jsonRequest = JsonParser.parseString(sb.toString()).getAsJsonObject();
            int userId = jsonRequest.has("userId") ? jsonRequest.get("userId").getAsInt() : -1;
            String serviceName = jsonRequest.has("serviceName") ? jsonRequest.get("serviceName").getAsString() : null;

            if (userId != -1 && serviceName != null) {
                boolean success = favouriteDAO.removeFavourite(userId, serviceName);
                jsonResponse.addProperty("success", success);
            } else {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("message", "Missing parameters");
            }
        } catch (Exception e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Error removing favourite");
        }

        out.print(gson.toJson(jsonResponse));
        out.flush();
    }
}
