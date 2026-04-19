package com.oneconnect.search;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@WebServlet("/api/search")
public class SearchServlet extends HttpServlet {
    private final Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String query = request.getParameter("q");
        PrintWriter out = response.getWriter();
        JsonArray matchingServices = new JsonArray();

        try {
            if (query != null && !query.trim().isEmpty()) {
                String lowerQuery = query.toLowerCase(Locale.ROOT).trim();
                List<String> queryTokens = tokenize(lowerQuery);
                List<ServiceInfo> allServices = ServiceCatalog.getAllServices();

                for (ServiceInfo service : allServices) {
                    if (matchesQuery(service, lowerQuery, queryTokens)) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("name", service.getName());
                        obj.addProperty("url", service.getUrl());
                        obj.addProperty("description", service.getDescription());
                        obj.addProperty("emoji", service.getEmoji());
                        matchingServices.add(obj);
                    }
                }
            }
        } catch (Throwable t) {
            response.setStatus(HttpServletResponse.SC_OK);
        }

        out.print(gson.toJson(matchingServices));
        out.flush();
    }

    private boolean matchesQuery(ServiceInfo service, String lowerQuery, List<String> queryTokens) {
        String normalizedFullQuery = normalizeToken(lowerQuery);
        Set<String> serviceTokens = buildServiceTokens(service);

        if (service.getName().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
            return true;
        }

        if (service.getDescription().toLowerCase(Locale.ROOT).contains(lowerQuery)) {
            return true;
        }

        String joinedKeywords = String.join(" ", service.getKeywords()).toLowerCase(Locale.ROOT);
        if (joinedKeywords.contains(lowerQuery)) {
            return true;
        }

        // For multi-word queries (e.g., "google pay"), match if any token has strong overlap.
        for (String token : queryTokens) {
            String normalizedToken = normalizeToken(token);
            if (normalizedToken.length() < 2) {
                continue;
            }

            if (service.getName().toLowerCase(Locale.ROOT).contains(normalizedToken)
                    || service.getDescription().toLowerCase(Locale.ROOT).contains(normalizedToken)
                    || joinedKeywords.contains(normalizedToken)
                    || fuzzyTokenMatch(normalizedToken, serviceTokens)) {
                return true;
            }
        }

        // Handle short typos against complete query, e.g. "youtubr" -> "youtube".
        if (normalizedFullQuery.length() >= 4 && fuzzyTokenMatch(normalizedFullQuery, serviceTokens)) {
            return true;
        }

        for (String keyword : service.getKeywords()) {
            if (keyword.toLowerCase(Locale.ROOT).contains(lowerQuery) || lowerQuery.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }

        return false;
    }

    private List<String> tokenize(String lowerQuery) {
        return Arrays.stream(lowerQuery.split("\\s+"))
            .map(String::trim)
            .filter(token -> !token.isEmpty())
            .collect(Collectors.toList());
    }

    private Set<String> buildServiceTokens(ServiceInfo service) {
        Set<String> tokens = new HashSet<>();

        for (String token : tokenize(service.getName().toLowerCase(Locale.ROOT))) {
            String normalized = normalizeToken(token);
            if (!normalized.isEmpty()) {
                tokens.add(normalized);
            }
        }

        for (String token : tokenize(service.getDescription().toLowerCase(Locale.ROOT))) {
            String normalized = normalizeToken(token);
            if (!normalized.isEmpty()) {
                tokens.add(normalized);
            }
        }

        for (String keyword : service.getKeywords()) {
            for (String token : tokenize(keyword.toLowerCase(Locale.ROOT))) {
                String normalized = normalizeToken(token);
                if (!normalized.isEmpty()) {
                    tokens.add(normalized);
                }
            }
        }

        return tokens;
    }

    private String normalizeToken(String token) {
        return token.replaceAll("[^a-z0-9]", "");
    }

    private boolean fuzzyTokenMatch(String queryToken, Set<String> serviceTokens) {
        for (String serviceToken : serviceTokens) {
            if (serviceToken.startsWith(queryToken) || queryToken.startsWith(serviceToken)) {
                return true;
            }

            int distance = levenshtein(queryToken, serviceToken);
            if (distance <= maxAllowedDistance(queryToken.length(), serviceToken.length())) {
                return true;
            }
        }
        return false;
    }

    private int maxAllowedDistance(int a, int b) {
        int len = Math.max(a, b);
        if (len <= 4) {
            return 1;
        }
        if (len <= 8) {
            return 2;
        }
        return 3;
    }

    private int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }
}
