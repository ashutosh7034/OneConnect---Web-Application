package com.oneconnect.dashboard;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.oneconnect.util.ServiceCatalog;
import com.oneconnect.util.ServiceCatalog.ServiceInfo;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/services")
public class DashboardServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        JsonArray servicesArray = new JsonArray();

        for (ServiceInfo service : ServiceCatalog.getAllServices()) {
            addService(servicesArray, service.getName(), service.getUrl(), service.getDescription(), service.getEmoji());
        }

        out.print(gson.toJson(servicesArray));
        out.flush();
    }

    private void addService(JsonArray array, String name, String url, String description, String emoji) {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("url", url);
        obj.addProperty("description", description);
        obj.addProperty("emoji", emoji);
        array.add(obj);
    }
}
