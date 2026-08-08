package net.alan.gui.data.source;

import java.util.Map;

public interface CardDataSource {
    Map<String, String> load(Map<String, String> context);
}