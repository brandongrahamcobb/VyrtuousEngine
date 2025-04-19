package com.brandongcobb.patreonplugin;

import java.util.Map;

public class ConfigSection {
    private Map<String, Object> values;

    public ConfigSection(Map<String, Object> values) {
        this.values = values;
    }

    public String getStringValue(String key) {
        Object value = values.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null; // or throw an exception if you expect a String
    }
}
