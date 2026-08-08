package net.alan.gui.data.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CardDataSourceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CardDataSourceRegistry.class);
    private static final ConcurrentHashMap<String, CardDataSource> SOURCES = new ConcurrentHashMap<>();

    public static void register(String id, CardDataSource source) {
        SOURCES.put(id, source);
        LOGGER.debug("Registered card data source: {}", id);
    }

    public static CardDataSource get(String id) {
        return SOURCES.get(id);
    }

    public static Map<String, String> load(String id, Map<String, String> context) {
        CardDataSource source = SOURCES.get(id);
        if (source == null) {
            LOGGER.warn("Card data source not found: {}", id);
            return Map.of();
        }
        try {
            Map<String, String> result = source.load(context);
            return result != null ? result : Map.of();
        } catch (Exception e) {
            LOGGER.error("Failed to load card data source '{}': {}", id, e.getMessage(), e);
            return Map.of();
        }
    }
}