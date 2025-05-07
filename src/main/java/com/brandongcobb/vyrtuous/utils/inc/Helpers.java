/*  Helpers.java The purpose of this program is to support the Vytuous class
 *  for any values which would make the legibility of the code worsen if it
 *  was inluded explicitly.
 *  Copyright (C) 2024  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
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
        configMap.put("discord_api_key", "");
        configMap.put("discord_client_id", "");
        configMap.put("discord_client_secret", "");
        configMap.put("discord_command_prefix", "!");
        configMap.put("discord_owner_id", "YOUR DISCORD ID");
        configMap.put("discord_redirect_uri", "");
        configMap.put("discord_role_pass", "ID FOR MODERATION BYPASS");
        configMap.put("discord_testing_guild_id", "MAIN GUILD ID");
        configMap.put("openai_api_key", "");
        configMap.put("openai_chat_completion", false);
        configMap.put("openai_chat_moderation", true);
        configMap.put("openai_chat_stream", true);
        configMap.put("openai_chat_temperature", 0.7);
        configMap.put("openai_chat_top_p", 1.0);
        configMap.put("openai_client_id", "");
        configMap.put("openai_client_secret", "");
        configMap.put("openai_redirect_uri", "");
        configMap.put("patreon_api_key", "");
        configMap.put("patreon_client_id", "");
        configMap.put("patreon_client_secret", "");
        configMap.put("patreon_redirect_uri", "");
        configMap.put("postgres_database", "");
        configMap.put("postgres_user", "postgres");
        configMap.put("postgres_host", "localhost");
        configMap.put("postgres_password", "");
        configMap.put("postgres_port", "");
        configMap.put("spark_discord_endpoint", "/oauth/discord_callback");
        configMap.put("spark_patreon_endpoint", "/oauth/patreon_callback");
        configMap.put("spark_port", Helpers.parseCommaNumber("8,000"));
        return configMap;
    }


    // Base directories
    public static final String DIR_BASE = Paths.get("/home/spawd/Vystopia/src/main/java/com/brandongcobb/").toAbsolutePath().toString(); // Placeholder
    public static final String DIR_HOME = System.getProperty("user.home");
    public static final String DIR_TEMP = Paths.get(DIR_BASE, "vyrtuous", "temp").toString();

    // Paths
    public static final String PATH_VYRTUOUS = Paths.get(DIR_BASE, "vyrtuous", "Vyrtuous.java").toString();
    public static final String PATH_DISCORD_BOT = Paths.get(DIR_BASE, "vyrtuous", "bots", "DiscordBot.java").toString();
    public static final String PATH_COG = Paths.get(DIR_BASE, "vyrtuous", "cogs", "Cog.java").toString();
    public static final String PATH_EVENT_LISTENERS = Paths.get(DIR_BASE, "vyrtuous", "cogs", "EventListeners.java").toString();
    public static final String PATH_AI_MANAGER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "AIManager.java").toString();
    public static final String PATH_CONFIG_MANAGER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "ConfigManager.java").toString();
    public static final String PATH_DISCORD_USER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "DiscordUser.java").toString();
    public static final String PATH_MESSAGE_MANAGER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "MessageManager.java").toString();
    public static final String PATH_MINECRAFT_USER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "MinecraftUser.java").toString();
    public static final String PATH_MODEL_INFO = Paths.get(DIR_BASE, "vyrtuous", "records", "ModelInfo.java").toString();
    public static final String PATH_MODERATION_MANAGER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "ModerationManager.java").toString();
    public static final String PATH_OAUTH_SERVER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "OAuthServer.java").toString();
    public static final String PATH_OAUTH_USER_SESSION = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "OAuthUserSession.java").toString();
    public static final String PATH_PATREON_USER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "PatreonUser.java").toString();
    public static final String PATH_PREDICATOR = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "Predicator.java").toString();
    public static final String PATH_USER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "User.java").toString();
    public static final String PATH_USER_MANAGER = Paths.get(DIR_BASE, "vyrtuous", "utils", "handlers", "UserManager.java").toString();
    public static final String PATH_PLAYER_JOIN_LISTENER = Paths.get(DIR_BASE, "vyrtuous", "utils", "listeners", "PlayerJoinListener.java").toString();
    public static final String PATH_HELPERS = Paths.get(DIR_BASE, "vyrtuous", "utils", "inc", "Helpers.java").toString();
    public static final String PATH_MODEL_REGISTRY = Paths.get(DIR_BASE, "vyrtuous", "utils", "inc", "ModelRegistry.java").toString();
    public static final String PATH_DISCORD_OAUTH = Paths.get(DIR_BASE, "vyrtuous", "utils", "sec", "DiscordOAuth.py").toString();
    public static final String PATH_PATREON_OAUTH = Paths.get(DIR_BASE, "vyrtuous", "utils", "sec", "PatreonOAuth.py").toString();

    public static final long[] DISCORD_CHARACTER_LIMITS = new long[]{parseCommaNumber("2,000"), parseCommaNumber("4,000")};
    public static final long DISCORD_CHARACTER_LIMIT = parseCommaNumber("2,000");
    public static final String[] DISCORD_COGS = new String[]{"vyrtuous.cogs.EventListeners"};
    public static final String DISCORD_COMMAND_PREFIX = "!";
    public static final String DISCORD_MODERATION_WARNING = "You have been warned.";
    public static final long DISCORD_OWNER_ID = parseCommaNumber("154,749,533,429,956,608");
    public static final boolean DISCORD_RELEASE_MODE = false;
    public static final long DISCORD_ROLE_PASS = parseCommaNumber("1,308,689,505,158,565,918");
    public static final long DISCORD_TESTING_GUILD_ID = parseCommaNumber("1,300,517,536,001,036,348");

    public static final String LOGGING_LEVEL = "INFO";

    public static final boolean OPENAI_CHAT_ADD_COMPLETION_TO_HISTORY = true;
    public static final Map<String, Object> OPENAI_CHAT_COLORIZE_RESPONSE_FORMAT = createColorizeSchema();
    public static final boolean OPENAI_CHAT_COMPLETION = true;
    public static final Map<String, Object> OPENAI_RESPONSES_TEXT_PERPLEXITY = createPerplexitySchema();
    public static final Map<String, Object> OPENAI_CHAT_COMPLETION_RESPONSE_FORMAT = new HashMap<>();
    public static final Map<String, String> OPENAI_CHAT_HEADERS = Map.of(
        "Content-Type", "application/json",
        "OpenAI-Organization", "org-3LYwtg7DSFJ7RLn9bfk4hATf",
        "User-Agent", "brandongrahamcobb@icloud.com",
        "OpenAI-Project", "proj_u5htBCWX0LSHxkw45po1Vfz9"
    );
    public static final long OPENAI_CHAT_N = 1;
    public static final Map<String, List<String>> OPENAI_CHAT_MODELS = Map.of(
        "current", List.of("chatgpt-4o-mini-latest", "gpt-4.1", "gpt-4.1-nano", "o1-mini", "o1-preview", "o3-mini", "o4-mini"),
        "deprecated", List.of("chatgpt-4o-latest", "gpt-3.5-turbo", "gpt-4", "gpt-4-32k", "gpt-4o", "gpt-4o-mini", "gpt-4-turbo", "chatgpt-4o-latest")
    );
    public static final boolean OPENAI_CHAT_MODERATION = true;
    public static final String OPENAI_CHAT_MODERATION_MODEL = "gpt-4.1-nano";
    public static final Map<String, Object> OPENAI_CHAT_MODERATION_RESPONSE_FORMAT = createModerationSchema();
    public static final String OPENAI_CHAT_MODERATION_STOP = "";
    public static final boolean OPENAI_CHAT_MODERATION_STORE = false;
    public static final boolean OPENAI_CHAT_MODERATION_STREAM = false;
    public static final String OPENAI_CHAT_MODERATION_SYS_INPUT = "You are a JSON moderation assistant";
    public static final float OPENAI_CHAT_MODERATION_TEMPERATURE = 1.0f;
    public static final float OPENAI_CHAT_MODERATION_TOP_P = 1.0f;
    public static final String OPENAI_CHAT_MODEL = "gpt-4.1-nano";
    public static final boolean OPENAI_CHAT_MODERATION_USE_HISTORY = false;
    public static final boolean OPENAI_CHAT_MODERATION_ADD_COMPLETION_TO_HISTORY = false;
    public static final Map<String, Object> OPENAI_CHAT_RESPONSE_FORMAT = new HashMap<>();
    public static final String OPENAI_CHAT_STOP = "";
    public static final boolean OPENAI_CHAT_STORE = false;
    public static final boolean OPENAI_CHAT_STREAM = false;
    private static final String SUPER_STRING = PATH_AI_MANAGER + " | " + PATH_CONFIG_MANAGER + " | " + PATH_DISCORD_USER + " | " +
                     PATH_MESSAGE_MANAGER + " | " + PATH_MINECRAFT_USER + " | " + PATH_MODERATION_MANAGER + " | " +
                     PATH_OAUTH_SERVER + " | " + PATH_OAUTH_USER_SESSION + " | " + PATH_PATREON_USER + " | " +
                     PATH_PREDICATOR + " | " + PATH_USER + " | " + PATH_USER_MANAGER + " | " + PATH_VYRTUOUS;
    public static final String OPENAI_CHAT_COMPLETION_SYS_INPUT = "You are Vyrtuous." + SUPER_STRING;
    public static final float OPENAI_CHAT_TOP_P = 1.0f;
    public static final float OPENAI_CHAT_TEMPERATURE = 0.7f;
    public static final boolean OPENAI_CHAT_USE_HISTORY = true;
    public static final String OPENAI_CHAT_USER = "Brandon Graham Cobb";
    public static final Map<String, String> OPENAI_ENDPOINT_URLS = Map.of(
        "audio", "https://api.openai.com/v1/audio/speech",
        "batch", "https://api.openai.com/v1/audio/batches",
        "chat", "https://api.openai.com/v1/chat/completions",
        "embeddings", "https://api.openai.com/v1/embeddings",
        "files", "https://api.openai.com/v1/files",
        "fine-tuning", "https://api.openai.com/v1/fine_tuning/jobs",
        "images", "https://api.openai.com/v1/images/generations",
        "models", "https://api.openai.com/v1/models",
        "moderations", "https://api.openai.com/v1/moderations",
        "uploads", "https://api.openai.com/v1/uploads"
    );
    public static final String OPENAI_MODERATION_MODEL = "omni-moderation-latest";
    public static final boolean OPENAI_MODERATION_IMAGE = true;

    public static final Map<String, String> SCRIPTURE_HEADERS = Map.of(
        "User-Agent", "brandongrahamcobb@icloud.com",
        "api-key", "2eb327f99245cd3d68da55370656d6e2"
    );

    public static final String USER_AGENT = "https://github.com/brandongrahamcobb/Vyrtuous.git";
    public static final String VERSION = "1.0.0";

    @SuppressWarnings("unchecked")
    private static Map<String, Object> createColorizeSchema() {
        // Define the color properties
        Map<String, Object> properties = new HashMap<>();
        properties.put("r", Map.of("type", "integer", "minimum", 0, "maximum", 255));
        properties.put("g", Map.of("type", "integer", "minimum", 0, "maximum", 255));
        properties.put("b", Map.of("type", "integer", "minimum", 0, "maximum", 255));
    
        // Build the schema for the colorize response
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("r", "g", "b"));
        schema.put("additionalProperties", false);
    
        // Define the format containing the schema
        Map<String, Object> format = new HashMap<>();
        format.put("type", "json_schema");
        format.put("strict", true);
        format.put("schema", schema);
        format.put("name", "colorize");
    
        // Wrap format in the new responses API structure
        return format;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> createPerplexitySchema() {
        // Define the color properties
        Map<String, Object> properties = new HashMap<>();
        properties.put("perplexity", Map.of("type", "integer"));
    
        // Build the schema for the colorize response
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("perplexity"));
        schema.put("additionalProperties", false);
    
        // Define the format containing the schema
        Map<String, Object> format = new HashMap<>();
        format.put("type", "json_schema");
        format.put("strict", true);
        format.put("schema", schema);
        format.put("name", "perplexity");
    
        // Wrap format in the new responses API structure
        return format;
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, Object> createModerationSchema() {
        // Main properties for id and model
        Map<String, Object> properties = new HashMap<>();
        properties.put("id", Map.of("type", "string"));
        properties.put("model", Map.of("type", "string"));
    
        // Create properties for each moderation category
        Map<String, Object> categoriesProps = new HashMap<>();
        String[] categoryKeys = {
            "sexual", "hate", "harassment", "self-harm", "sexual/minors",
            "hate/threatening", "violence/graphic", "self-harm/intent",
            "self-harm/instructions", "harassment/threatening", "violence"
        };
        for (String key : categoryKeys) {
            categoriesProps.put(key, Map.of("type", "boolean"));
        }
    
        // Build the schema part for "categories"
        Map<String, Object> categories = new HashMap<>();
        categories.put("type", "object");
        categories.put("properties", categoriesProps);
        categories.put("required", Arrays.asList(categoryKeys));
        categories.put("additionalProperties", false); // Disallow extra props here
        Map<String, Object> scoresProps = new HashMap<>();
        for (String key : categoryKeys) {
            scoresProps.put(key, Map.of("type", "number"));
        }
        Map<String, Object> categoryScores = new HashMap<>();
        categoryScores.put("type", "object");
        categoryScores.put("properties", scoresProps);
        categoryScores.put("required", Arrays.asList(categoryKeys));
        categoryScores.put("additionalProperties", false); // Disallow extra props here
    
        // Assemble the "result" object schema
        Map<String, Object> resultProps = new HashMap<>();
        resultProps.put("flagged", Map.of("type", "boolean"));
        resultProps.put("categories", categories);
        resultProps.put("category_scores", categoryScores);
    
        // Use a mutable map so we can add additionalProperties
        Map<String, Object> resultObject = new HashMap<>();
        resultObject.put("type", "object");
        resultObject.put("properties", resultProps);
        resultObject.put("required", List.of("flagged", "categories", "category_scores"));
        resultObject.put("additionalProperties", false);  // <-- This line is essential!
    
        // Define the results array schema
        Map<String, Object> results = new HashMap<>();
        results.put("type", "array");
        results.put("items", resultObject);
        properties.put("results", results);
    
        // Build the main schema for the moderation response
        Map<String, Object> mainSchema = new HashMap<>();
        mainSchema.put("type", "object");
        mainSchema.put("properties", properties);
        mainSchema.put("required", Arrays.asList("id", "model", "results"));
        mainSchema.put("additionalProperties", false);
    
        // Wrap it up in a "format" object
        Map<String, Object> format = new HashMap<>();
        format.put("type", "json_schema");
        format.put("strict", true);
        format.put("name", "moderations");
        format.put("schema", mainSchema);
    
        return format;
    }

}
