package com.oneconnect.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.oneconnect.util.ServiceCatalog;
import com.oneconnect.util.ServiceCatalog.ServiceInfo;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@WebServlet("/api/ai-guide")
public class AiGuideServlet extends HttpServlet {
    private final Gson gson = new Gson();

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "i", "me", "my", "want", "need", "to", "for", "the", "a", "an", "is", "am", "are", "on", "of", "in", "and", "or", "please"
    ));

    private static class ScoredService {
        ServiceInfo service;
        int score;
        List<String> reasons;

        ScoredService(ServiceInfo service, int score, List<String> reasons) {
            this.service = service;
            this.score = score;
            this.reasons = reasons;
        }
    }

    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setCorsHeaders(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();

        try {
            String query = extractQuery(request);
            if (query == null || query.trim().isEmpty()) {
                jsonResponse.addProperty("success", false);
                jsonResponse.addProperty("reply", "Tell me what you need, for example: 'I am hungry' or 'I need a cab'.");
                out.print(gson.toJson(jsonResponse));
                return;
            }

            List<ScoredService> ranked = rankServices(query);
            JsonArray suggestions = new JsonArray();
            int maxSuggestions = Math.min(5, ranked.size());

            for (int i = 0; i < maxSuggestions; i++) {
                ScoredService scored = ranked.get(i);
                JsonObject item = new JsonObject();
                item.addProperty("name", scored.service.getName());
                item.addProperty("url", scored.service.getUrl());
                item.addProperty("description", scored.service.getDescription());
                item.addProperty("emoji", scored.service.getEmoji());
                item.addProperty("reason", scored.reasons.isEmpty() ? "Good match" : scored.reasons.get(0));
                suggestions.add(item);
            }

            jsonResponse.addProperty("success", true);
            jsonResponse.addProperty("reply", buildReply(query, ranked));
            jsonResponse.add("suggestions", suggestions);
            out.print(gson.toJson(jsonResponse));
        } catch (Exception e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("reply", "I could not process that request right now.");
            out.print(gson.toJson(jsonResponse));
        }

        out.flush();
    }

    private String extractQuery(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        if (sb.length() == 0) {
            return null;
        }

        JsonObject req = JsonParser.parseString(sb.toString()).getAsJsonObject();
        return req.has("query") ? req.get("query").getAsString() : null;
    }

    private List<ScoredService> rankServices(String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        Set<String> queryTokens = tokenize(normalizedQuery);

        List<ScoredService> scored = new ArrayList<>();

        for (ServiceInfo service : ServiceCatalog.getAllServices()) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            String nameLower = service.getName().toLowerCase(Locale.ROOT);
            String descLower = service.getDescription().toLowerCase(Locale.ROOT);

            if (normalizedQuery.contains(nameLower)) {
                score += 8;
                reasons.add("You mentioned " + service.getName());
            }

            for (String token : queryTokens) {
                if (nameLower.contains(token)) {
                    score += 4;
                    reasons.add("Matches your query term: " + token);
                }

                for (String keyword : service.getKeywords()) {
                    if (keyword.contains(token) || token.contains(keyword)) {
                        score += 3;
                        reasons.add("Relevant for " + keyword);
                        break;
                    }
                }

                if (descLower.contains(token)) {
                    score += 1;
                }
            }

            if (looksLikeFoodIntent(normalizedQuery) && hasAnyKeyword(service, Arrays.asList("food", "hungry", "delivery", "pizza", "eat"))) {
                score += 5;
                reasons.add("Great match for food-related needs");
            }

            if (score > 0) {
                scored.add(new ScoredService(service, score, dedupeReasons(reasons)));
            }
        }

        if (scored.isEmpty()) {
            for (ServiceInfo service : ServiceCatalog.getAllServices().subList(0, Math.min(5, ServiceCatalog.getAllServices().size()))) {
                scored.add(new ScoredService(service, 1, Collections.singletonList("Popular service suggestion")));
            }
        }

        scored.sort(Comparator.comparingInt((ScoredService s) -> s.score).reversed());
        return scored;
    }

    private String buildReply(String query, List<ScoredService> ranked) {
        if (ranked.isEmpty()) {
            return "I could not find a strong match, but I can still suggest popular services.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Based on your query '").append(query).append("', you can try ");

        int limit = Math.min(3, ranked.size());
        for (int i = 0; i < limit; i++) {
            sb.append(ranked.get(i).service.getName());
            if (i < limit - 2) {
                sb.append(", ");
            } else if (i == limit - 2) {
                sb.append(" and ");
            }
        }

        sb.append(".");
        return sb.toString();
    }

    private Set<String> tokenize(String text) {
        String cleaned = text.replaceAll("[^a-z0-9 ]", " ").trim();
        String[] rawTokens = cleaned.split("\\s+");
        Set<String> tokens = new HashSet<>();

        for (String token : rawTokens) {
            if (!token.isEmpty() && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private boolean looksLikeFoodIntent(String query) {
        return query.contains("hungry") || query.contains("food") || query.contains("eat") || query.contains("lunch") || query.contains("dinner");
    }

    private boolean hasAnyKeyword(ServiceInfo service, List<String> keys) {
        for (String key : keys) {
            if (service.getKeywords().contains(key)) {
                return true;
            }
        }
        return false;
    }

    private List<String> dedupeReasons(List<String> reasons) {
        List<String> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String reason : reasons) {
            if (seen.add(reason)) {
                unique.add(reason);
            }
        }
        return unique;
    }
}
