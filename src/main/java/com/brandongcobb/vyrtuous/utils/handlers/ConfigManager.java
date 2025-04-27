/*  ConfigManager.java The primary purpose of this handler is to 
 *  manage the configuration.
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
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;

import java.io.*;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public  class ConfigManager {

    private static Vyrtuous app;
    private static Map<String, Object> config;
    private static Map<String, Object> defaultConfig;
    private ConfigSection configSection;
    private Map<String, Object> inputConfigMap;
    private static Logger logger;

    static {
        config = new HashMap<>();
    }

    public static void setApp(Vyrtuous plugin) {
        app = plugin;
    }

    public static Vyrtuous getApp() {
        return app;
    }

    public static Map<String, Object> getConfig() {
        return config;
    }

    public boolean exists() {
        File configFile = new File(app.getDataFolder(), "config.yml");
        return configFile.exists();
    }

    public void createDefaultConfig() {
        File configFile = new File(app.getDataFolder(), "config.yml");
        populateConfig(config);
        saveConfig(configFile); // Save the default config
    }

    private static Map<String, Object> populateConfig(Map<String, Object> configMap) {
        configMap.put("api_keys", new HashMap<String, Object>() {{
                put("Discord", new HashMap<String, String>() {{
                put("api_key", "");
                put("client_id", "");
                put("client_secret", "");
                put("redirect_uri", "");
            }});
            put("Google", new HashMap<String, String>() {{
                put("api_key", "");
                put("client_id", "");
                put("client_secret", "");
                put("redirect_uri", "");
            }});
            put("LinkedIn", new HashMap<String, String>() {{
                put("api_key", "");
                put("client_id", "");
                put("client_secret", "");
                put("redirect_uri", "");
            }});
            put("OpenAI", new HashMap<String, String>() {{
                put("api_key", "");
                put("client_id", "");
                put("client_secret", "");
                put("redirect_uri", "");
            }});
            put("Patreon", new HashMap<String, String>() {{
                put("api_key", "");
                put("client_id", "");
                put("client_secret", "");
                put("redirect_uri", "");
            }});
            put("Twitch", new HashMap<String, String>() {{
                put("api_key", "");
                put("client_id", "");
                put("client_secret", "");
                put("redirect_uri", "");
            }});
        }});
        configMap.put("discord_owner_id", "YOUR DISCORD ID");
        configMap.put("discord_role_pass", "ID FOR MODERATION BYPASS");
        configMap.put("discord_testing_guild_id", "MAIN GUILD ID");
        configMap.put("openai_chat_completion", false);
        configMap.put("openai_chat_moderation", true);
        configMap.put("openai_chat_stream", true);
        configMap.put("openai_chat_temperature", 0.7);
        configMap.put("openai_chat_top_p", 1.0);
        configMap.put("postgres_database", "");
        configMap.put("postgres_user", "postgres");
        configMap.put("postgres_host", "localhost");
        configMap.put("postgres_password", "");
        configMap.put("postgres_port", "");
        configMap.put("spark_discord_endpoint", "/oauth/discord_callback");
        configMap.put("spark_patreon_endpoint", "/oauth/patreon_callback");
        configMap.put("spark_port", Helpers.parseCommaNumber("8,000"));
        configMap.put("web_headers", new HashMap<String, Object>() {{
            put("API.Bible", new HashMap<String, String>() {{
                put("User-Agent", "Vyrtuous https://github.com/brandongrahamcobb/Vyrtuous.git");
                put("api-key", "");
            }});
        }});
        return configMap;
    }

    public static boolean isConfigSameAsDefault() {
        return config.equals(defaultConfig);
    }

    public void loadConfig() throws IOException {
        File configFile = new File(app.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            try (InputStream inputStream = new FileInputStream(configFile)) {
                Yaml yaml = new Yaml();
                config = yaml.load(inputStream);
            } catch (IOException e) {
                app.logger.severe("Failed to load config: " + e.getMessage());
            } catch (Exception e) {
                app.logger.severe("The config file is corrupted. Please delete it or fix it. Error: " + e.getMessage());
            }
        } else {
            createDefaultConfig();
            try (InputStream inputStream = new FileInputStream(configFile)) {
                Yaml yaml = new Yaml();
                config = yaml.load(inputStream);
            } catch (IOException e) {
                app.logger.severe("Failed to load config: " + e.getMessage());
            } catch (Exception e) {
                app.logger.severe("The config file is corrupted. Please delete it or fix it. Error: " + e.getMessage());
            }
        }
    }

    public static Object getConfigValue(String key) {
        return config.get(key);
    }

    public static String getStringValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null; // or throw an exception if you expect a String
    }

    public static Integer getIntValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null; // or throw an exception if you expect an Integer
    }
//    public static Float getFloatValue(String key) {
//        Object value = getConfigValue(key);
//        if (value instanceof Number) {
//            return ((Number) value).floatValue();
//        } else if (value instanceof String) {
//            return Float.parseFloat((String) value);
//        }
//        return null; // or throw an exception if you expect a Float
//    }

    public static Long getLongValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return null; // or throw an exception if you expect a Float
    }

        // Check if 'api_keys' section exists
    public static Boolean getBooleanValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            // Handle string representations of boolean, e.g., "true", "false"
            return Boolean.parseBoolean((String) value);
        }
        return null; // or throw an exception if you expect a Boolean
    }

    private static void saveConfig(File configFile) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try {
            if (!app.getDataFolder().exists()) {
                app.getDataFolder().mkdirs(); // Create directories if they don't exist
            }
            try (Writer writer = new FileWriter(configFile)) {
                yaml.dump(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ConfigSection getNestedConfigValue(String outerKey, String innerKey) {
        Map<String, Object> outerMap = (Map<String, Object>) config.get(outerKey);
        if (outerMap != null) {
            Object innerValue = outerMap.get(innerKey);
            if (innerValue instanceof Map) {
                return new ConfigSection((Map<String, Object>) innerValue);
            }
        }
        return null; // Return null or handle accordingly if the outer key doesn't exist
    }

    public static boolean validateConfig() {
        boolean anyValid = false; // Flag to check if at least one block is compliant

        // Validate each API configuration
        String[] apis = {"Discord", "Google", "LinkedIn", "OpenAI", "Patreon", "Twitch"};
        for (String api : apis) {
            boolean isValid = validateApiConfig(api);
            if (isValid) {
                anyValid = true; // At least one API configuration is valid
            }
        }

        // Validate PostgreSQL Config
        boolean postgresValid = validatePostgresConfig();
        if (postgresValid) {
            anyValid = true; // PostgreSQL configuration is valid
        }

        boolean openAIValid = validateOpenAIConfig();
        if (openAIValid) {
            anyValid = true; // PostgreSQL configuration is valid
        }

        if (!anyValid) {
            app.logger.severe("No valid API configurations found. Please check your configuration.");
            // Optionally, throw an exception or halt further execution
        }
        return anyValid;
    }

    private static boolean validateApiConfig(String api) {
        HashMap<String, String> settings = (HashMap<String, String>) ((Map<String, Object>) config.get("api_keys")).get(api);

        if (settings == null) {
            app.logger.severe(api + " configuration is missing.");
            return false; // Configuration block not present
        }

        boolean hasValidData = false; // Track if any setting is valid

        for (Map.Entry<String, String> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null || value.trim().isEmpty()) {
                app.logger.warning(api + " setting '" + key + "' is missing or invalid.");
            } else {
                hasValidData = true; // Found at least one valid setting
            }
        }

        return hasValidData; // Returns true if at least one setting is valid
    }

    private static boolean validatePostgresConfig() {
        String database = (String) config.get("postgres_database");
        String user = (String) config.get("postgres_user");
        String password = (String) config.get("postgres_password");
        String host = (String) config.get("postgres_host");
        String port = (String) config.get("postgres_port");

        boolean isValid = true;

        if (database == null || database.trim().isEmpty()) {
            app.logger.warning("Postgres database setting is missing.");
            isValid = false;
        }
        if (user == null || user.trim().isEmpty()) {
            app.logger.warning("Postgres user setting is missing.");
            isValid = false;
        }
        if (password == null || password.trim().isEmpty()) {
            app.logger.warning("Postgres password setting is missing.");
            isValid = false;
        }
        if (host == null || host.trim().isEmpty()) {
            app.logger.warning("Postgres host setting is missing.");
            isValid = false;
        }
        if (port == null || port.trim().isEmpty()) {
            app.logger.warning("Postgres port setting is missing.");
            isValid = false;
        }

        return isValid; // Returns true if the Postgres config is properly set
    }

    private static boolean validateOpenAIConfig() {
        String openAIChatCompletion = (String) String.valueOf(config.get("openai_chat_completion"));
        String openAIChatModel = (String) String.valueOf(config.get("openai_chat_model"));
        String openAIChatModeration = (String) String.valueOf(config.get("openai_chat_moderation"));
        String openAIChatStop = (String) String.valueOf(config.get("openai_chat_stop"));
        boolean openAIChatStream = (boolean) Boolean.parseBoolean(String.valueOf(config.get("openai_chat_stream")));
        float openAIChatTemperature = (float) Float.parseFloat(String.valueOf(config.get("openai_chat_temperature")));
        float openAIChatTopP = (float) Float.parseFloat(String.valueOf(config.get("openai_chat_top_p")));
        boolean isValid = true;
        if (openAIChatModel == null || openAIChatModel.trim().isEmpty()) {
            app.logger.warning("OpenAI model setting is missing.");
            isValid = false;
        }
        if (openAIChatStop == null || openAIChatStop.trim().isEmpty()) {
            app.logger.warning("OpenAI user setting is missing.");
            isValid = false;
        }
        if (openAIChatStream == (boolean) false) {
            app.logger.warning("OpenAI chat streaming is disabled.");
            isValid = false;
        }
        if (openAIChatTopP < 0.0f || openAIChatTopP > 2.0f) {
            app.logger.warning("OpenAI chat top P is broken.");
            isValid = false;
        }
        if (openAIChatTemperature < 0.0f && openAIChatTemperature > 2.0f) {
            app.logger.warning("OpenAI chat temperature is broken.");
            isValid = false;
        }
        return isValid; // Returns true if the Postgres config is properly set
    }

    public static class ConfigSection {

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
}
