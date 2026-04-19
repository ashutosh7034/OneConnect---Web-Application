package com.oneconnect.ai;

import com.oneconnect.util.ServiceCatalog.ServiceInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RagRetriever {
    public static class RetrievedService {
        private final ServiceInfo service;
        private final double score;
        private final List<String> matchedTerms;

        public RetrievedService(ServiceInfo service, double score, List<String> matchedTerms) {
            this.service = service;
            this.score = score;
            this.matchedTerms = matchedTerms;
        }

        public ServiceInfo getService() {
            return service;
        }

        public double getScore() {
            return score;
        }

        public List<String> getMatchedTerms() {
            return matchedTerms;
        }
    }

    private static class DocumentVector {
        private final ServiceInfo service;
        private final Map<String, Double> tfidf;
        private final Set<String> tokens;

        private DocumentVector(ServiceInfo service, Map<String, Double> tfidf, Set<String> tokens) {
            this.service = service;
            this.tfidf = tfidf;
            this.tokens = tokens;
        }
    }

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "i", "me", "my", "mine", "you", "your", "yours", "want", "need", "please", "a", "an", "the", "is", "am", "are", "was", "were", "be", "to", "for", "of", "on", "in", "at", "from", "and", "or", "with", "it", "that", "this", "then", "than", "can", "could", "should", "would", "do", "does", "did", "get", "give", "show"
    ));

    private static final Map<String, List<String>> SYNONYMS = buildSynonyms();

    private final List<DocumentVector> documents;
    private final Map<String, Double> idf;

    public RagRetriever(List<ServiceInfo> services) {
        this.idf = new HashMap<>();
        this.documents = buildDocumentVectors(services);
    }

    public List<RetrievedService> retrieve(String query, int topK) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String queryLower = query.toLowerCase(Locale.ROOT);
        Map<String, Double> queryVector = buildQueryVector(queryLower);
        Set<String> queryTerms = expandedQueryTerms(queryLower);

        List<RetrievedService> scored = new ArrayList<>();

        for (DocumentVector doc : documents) {
            double score = cosine(queryVector, doc.tfidf);

            String serviceName = doc.service.getName().toLowerCase(Locale.ROOT);
            if (queryLower.contains(serviceName)) {
                score += 0.25;
            }

            List<String> matched = new ArrayList<>();
            for (String term : queryTerms) {
                if (doc.tokens.contains(term)) {
                    matched.add(term);
                }
            }

            if (!matched.isEmpty()) {
                score += Math.min(0.20, matched.size() * 0.03);
            }

            if (score > 0.02) {
                scored.add(new RetrievedService(doc.service, score, matched));
            }
        }

        scored.sort(Comparator.comparingDouble(RetrievedService::getScore).reversed());

        if (scored.isEmpty()) {
            // Fallback: suggest first few services when retrieval has no confidence.
            int fallback = Math.min(topK, documents.size());
            for (int i = 0; i < fallback; i++) {
                DocumentVector doc = documents.get(i);
                scored.add(new RetrievedService(doc.service, 0.01, Collections.singletonList("popular")));
            }
        }

        return scored.subList(0, Math.min(topK, scored.size()));
    }

    private List<DocumentVector> buildDocumentVectors(List<ServiceInfo> services) {
        List<Map<String, Double>> termFreqPerDoc = new ArrayList<>();
        List<Set<String>> tokensPerDoc = new ArrayList<>();

        Map<String, Integer> docFreq = new HashMap<>();

        for (ServiceInfo service : services) {
            String docText = buildDocText(service);
            Set<String> tokens = tokenize(docText);
            tokensPerDoc.add(tokens);

            Map<String, Double> tf = termFrequency(tokens);
            termFreqPerDoc.add(tf);

            for (String token : tokens) {
                docFreq.put(token, docFreq.getOrDefault(token, 0) + 1);
            }
        }

        int n = services.size();
        for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
            double value = Math.log((n + 1.0) / (entry.getValue() + 1.0)) + 1.0;
            idf.put(entry.getKey(), value);
        }

        List<DocumentVector> vectors = new ArrayList<>();
        for (int i = 0; i < services.size(); i++) {
            Map<String, Double> tfidf = applyTfidf(termFreqPerDoc.get(i), idf);
            normalize(tfidf);
            vectors.add(new DocumentVector(services.get(i), tfidf, tokensPerDoc.get(i)));
        }

        return vectors;
    }

    private Map<String, Double> buildQueryVector(String queryLower) {
        Set<String> terms = expandedQueryTerms(queryLower);

        Map<String, Double> weighted = new HashMap<>();
        Set<String> baseTerms = tokenize(queryLower);

        for (String term : terms) {
            double weight = baseTerms.contains(term) ? 1.0 : 0.6;
            weighted.put(term, weighted.getOrDefault(term, 0.0) + weight);
        }

        Map<String, Double> tfidf = applyTfidf(weighted, idf);
        normalize(tfidf);
        return tfidf;
    }

    private Set<String> expandedQueryTerms(String text) {
        Set<String> baseTerms = tokenize(text);
        Set<String> expanded = new LinkedHashSet<>(baseTerms);

        for (String term : baseTerms) {
            List<String> aliases = SYNONYMS.get(term);
            if (aliases != null) {
                expanded.addAll(aliases);
            }
        }

        return expanded;
    }

    private String buildDocText(ServiceInfo service) {
        StringBuilder sb = new StringBuilder();
        sb.append(service.getName()).append(' ');
        sb.append(service.getDescription()).append(' ');

        for (String keyword : service.getKeywords()) {
            sb.append(keyword).append(' ');
        }

        return sb.toString();
    }

    private Set<String> tokenize(String text) {
        String cleaned = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").trim();
        if (cleaned.isEmpty()) {
            return Collections.emptySet();
        }

        String[] raw = cleaned.split("\\s+");
        Set<String> tokens = new LinkedHashSet<>();

        for (String t : raw) {
            String token = stem(t);
            if (token.length() >= 2 && !STOP_WORDS.contains(token)) {
                tokens.add(token);
            }
        }

        return tokens;
    }

    private String stem(String token) {
        if (token.endsWith("ing") && token.length() > 5) {
            return token.substring(0, token.length() - 3);
        }
        if (token.endsWith("ed") && token.length() > 4) {
            return token.substring(0, token.length() - 2);
        }
        if (token.endsWith("es") && token.length() > 4) {
            return token.substring(0, token.length() - 2);
        }
        if (token.endsWith("s") && token.length() > 3) {
            return token.substring(0, token.length() - 1);
        }
        return token;
    }

    private Map<String, Double> termFrequency(Set<String> tokens) {
        Map<String, Double> tf = new HashMap<>();
        for (String token : tokens) {
            tf.put(token, tf.getOrDefault(token, 0.0) + 1.0);
        }
        return tf;
    }

    private Map<String, Double> applyTfidf(Map<String, Double> tf, Map<String, Double> idfValues) {
        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            double idfValue = idfValues.getOrDefault(entry.getKey(), 1.0);
            tfidf.put(entry.getKey(), entry.getValue() * idfValue);
        }
        return tfidf;
    }

    private void normalize(Map<String, Double> vector) {
        double sum = 0.0;
        for (double value : vector.values()) {
            sum += value * value;
        }

        if (sum == 0.0) {
            return;
        }

        double norm = Math.sqrt(sum);
        for (Map.Entry<String, Double> entry : vector.entrySet()) {
            entry.setValue(entry.getValue() / norm);
        }
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        double dot = 0.0;

        for (Map.Entry<String, Double> entry : a.entrySet()) {
            Double v = b.get(entry.getKey());
            if (v != null) {
                dot += entry.getValue() * v;
            }
        }

        return dot;
    }

    private static Map<String, List<String>> buildSynonyms() {
        Map<String, List<String>> map = new HashMap<>();

        map.put("hungry", Arrays.asList("food", "eat", "delivery", "restaurant", "lunch", "dinner"));
        map.put("food", Arrays.asList("hungry", "eat", "delivery", "restaurant"));
        map.put("eat", Arrays.asList("food", "hungry", "restaurant"));

        map.put("shop", Arrays.asList("shopping", "buy", "product", "fashion", "electronics"));
        map.put("shopping", Arrays.asList("shop", "buy", "product", "fashion", "electronics"));
        map.put("buy", Arrays.asList("shop", "shopping", "product"));

        map.put("movie", Arrays.asList("entertainment", "watch", "series", "show"));
        map.put("music", Arrays.asList("song", "playlist", "podcast", "audio"));

        map.put("travel", Arrays.asList("ride", "cab", "taxi", "map", "route", "ticket"));
        map.put("cab", Arrays.asList("ride", "taxi", "travel", "commute"));

        map.put("pay", Arrays.asList("payment", "upi", "bill", "recharge", "money"));
        map.put("payment", Arrays.asList("pay", "upi", "bill", "money"));

        map.put("health", Arrays.asList("doctor", "medical", "medicine", "clinic"));
        map.put("doctor", Arrays.asList("health", "medical", "appointment"));

        return map;
    }
}
