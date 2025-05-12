/*  Helpers.java The purpose of this program is to support the Vytuous class
 *  for any values which would make the legibility of the code worsen if it
 *  was inluded explicitly.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.utils.inc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;

public class Helpers {

    private static String finalSchema;

    @SuppressWarnings("unchecked")
    public static <T> T convertValue(Object value, Class<T> type) {
        if (type.isInstance(value)) {
            return (T) value;
        }
        if (type == Boolean.class) {
            if (value instanceof String) return (T) Boolean.valueOf((String) value);
        } else if (type == Integer.class) {
            if (value instanceof Number) return (T) Integer.valueOf(((Number) value).intValue());
            if (value instanceof String) return (T) Integer.valueOf(Integer.parseInt((String) value));
        } else if (type == Long.class) {
            if (value instanceof Number) return (T) Long.valueOf(((Number) value).longValue());
            if (value instanceof String) return (T) Long.valueOf(Long.parseLong((String) value));
        } else if (type == Float.class) {
            if (value instanceof Number) return (T) Float.valueOf(((Number) value).floatValue());
            if (value instanceof String) return (T) Float.valueOf(Float.parseFloat((String) value));
        } else if (type == Double.class) {
            if (value instanceof Number) return (T) Double.valueOf(((Number) value).doubleValue());
            if (value instanceof String) return (T) Double.valueOf(Double.parseDouble((String) value));
        } else if (type == String.class) {
            return (T) value.toString();
        }

        throw new IllegalArgumentException("Unsupported type conversion for: " + type.getName());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepMerge(Map<String, Object> defaults, Map<String, Object> loaded) {
        Map<String, Object> merged = new HashMap<>(defaults);
        for (Map.Entry<String, Object> entry : loaded.entrySet()) {
            String key = entry.getKey();
            Object loadedVal = entry.getValue();
            if (merged.containsKey(key)) {
                Object defaultVal = merged.get(key);
                if (defaultVal instanceof Map && loadedVal instanceof Map) {
                    merged.put(key, deepMerge((Map<String, Object>) defaultVal, (Map<String, Object>) loadedVal));
                } else {
                    merged.put(key, loadedVal);
                }
            } else {
                merged.put(key, loadedVal);
            }
        }
        return merged;
    }

    public static boolean isNullOrEmpty(Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            if (objects[i] instanceof String) {
                if (objects[i] == null || ((String) objects[i]).trim().isEmpty()) {
                    return true;
                }
            } else if (objects[i] == null) {
                return true;
            }
        }
        return false;
    }

    public static Long parseCommaNumber(String number) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            if (c != ',') {
                sb.append(c);
            }
        }
        String cleanedNumber = sb.toString();
        try {
            int intVal = Integer.parseInt(cleanedNumber);
            return (long) intVal; // safely fits in int
        } catch (NumberFormatException e) {
            return Long.parseLong(cleanedNumber);
        }
    }

    public static Map<String, Object> populateConfig(Map<String, Object> configMap) {
        configMap.put("DISCORD_API_KEY", "");
        configMap.put("DISCORD_CLIENT_ID", "");
        configMap.put("DISCORD_CLIENT_SECRET", "");
        configMap.put("DISCORD_COMMAND_PREFIX", "!");
        configMap.put("DISCORD_OWNER_ID", "");
        configMap.put("DISCORD_REDIRECT_URI", "");
        configMap.put("DISCORD_ROLE_PASS", "");
        configMap.put("DISCORD_TESTING_GUILD_ID", "");
        configMap.put("openai_api_key", "");
        configMap.put("openai_chat_completion", false);
        configMap.put("openai_chat_model", "");
        configMap.put("openai_chat_moderation", true);
        configMap.put("openai_chat_stream", true);
        configMap.put("openai_chat_temperature", 0.7);
        configMap.put("openai_chat_top_p", 1.0);
        configMap.put("openai_client_id", "");
        configMap.put("openai_client_secret", "");
        configMap.put("openai_redirect_uri", "");
        configMap.put("PATREON_API_KEY", "");
        configMap.put("PATREON_CLIENT_ID", "");
        configMap.put("PATREON_CLIENT_SECRET", "");
        configMap.put("PATREON_REDIRECT_URI", "");
        configMap.put("POSTGRES_DATABASE", "");
        configMap.put("POSTGRES_USER", "postgres");
        configMap.put("POSTGRES_HOST", "localhost");
        configMap.put("POSTGRES_PASSWORD", "");
        configMap.put("POSTGRES_PORT", "");
        configMap.put("SPARK_DISCORD_ENDPOINT", "/oauth/discord_callback");
        configMap.put("SPARK_PATREON_ENDPOINT", "/oauth/patreon_callback");
        configMap.put("SPARK_PORT", Helpers.parseCommaNumber("8,000"));
        return configMap;
    }

    public static final long[] DISCORD_CHARACTER_LIMITS = new long[]{parseCommaNumber("2,000"), parseCommaNumber("4,000")};
    public static final long DISCORD_CHARACTER_LIMIT = parseCommaNumber("2,000");
    public static final String[] DISCORD_COGS = new String[]{"vyrtuous.cogs.EventListeners"};
    public static final String DISCORD_COMMAND_PREFIX = "!";

    public static final String LOGGING_LEVEL = "INFO";
}
