package com.oneconnect.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ServiceCatalog {
    private static final String SERVICES_RESOURCE = "services.json";
    private static final Gson GSON = new Gson();

    public static final class ServiceInfo {
        private final String name;
        private final String url;
        private final String description;
        private final String emoji;
        private final List<String> keywords;

        public ServiceInfo(String name, String url, String description, String emoji, List<String> keywords) {
            this.name = name;
            this.url = url;
            this.description = description;
            this.emoji = emoji;
            this.keywords = Collections.unmodifiableList(new ArrayList<>(keywords));
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public String getDescription() {
            return description;
        }

        public String getEmoji() {
            return emoji;
        }

        public List<String> getKeywords() {
            return keywords;
        }
    }

    private static final List<ServiceInfo> SERVICES = loadServices();

    private ServiceCatalog() {
    }

    public static List<ServiceInfo> getAllServices() {
        return SERVICES;
    }

    private static List<ServiceInfo> loadServices() {
        try (InputStream input = ServiceCatalog.class.getClassLoader().getResourceAsStream(SERVICES_RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + SERVICES_RESOURCE);
            }

            try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<ServiceInfo>>() {}.getType();
                List<ServiceInfo> parsed = GSON.fromJson(reader, listType);

                if (parsed == null) {
                    return Collections.emptyList();
                }

                return Collections.unmodifiableList(parsed);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load services catalog", e);
        }
    }
}
