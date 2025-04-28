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
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ConfigManager {

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
        configMap.put("discord_command_prefix", "!");
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
        return null;
    }

    public static Integer getIntValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    public static Long getLongValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return null; // or throw an exception if you expect a Float
    }

    public static Boolean getBooleanValue(String key) {
        Object value = getConfigValue(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }

    private static void saveConfig(File configFile) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try {
            if (!app.getDataFolder().exists()) {
                app.getDataFolder().mkdirs();
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
        boolean anyValid = false;
        String[] apis = {"Discord", "Google", "LinkedIn", "OpenAI", "Patreon", "Twitch"};
        for (String api : apis) {
            boolean isValid = validateApiConfig(api);
            if (isValid) {
                anyValid = true;
            }
        }
        boolean postgresValid = validatePostgresConfig();
        if (postgresValid) {anyValid = true;}
        boolean sparkValid = validateSparkConfig();
        if (sparkValid) {anyValid = true;}
        boolean webHeadersValid = validateWebHeadersConfig();
        if (webHeadersValid) {anyValid = true;}
        boolean openAIValid = validateOpenAIConfig();
        if (openAIValid) {anyValid = true;}
        if (!anyValid) {
            app.logger.severe("No valid API configurations found. Please check your configuration.");
        }
        return anyValid;
    }

    private static boolean validateApiConfig(String api) {
        HashMap<String, String> settings = (HashMap<String, String>) ((Map<String, Object>) config.get("api_keys")).get(api);
        boolean hasValidData = false;
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Object[] values = {settings, key, value};
            if (Helpers.isNotNullOrEmpty(values)) {
                app.logger.warning(api + " setting '" + key + "' is missing or invalid.");
            } else {
                hasValidData = true;
            }
        }
        return hasValidData;
    }

    private static boolean validateSparkConfig() {
        boolean isValid = true;
        String discordEndpoint = (String) config.get("spark_discord_endpoint");
        String patreonEndpoint = (String) config.get("spark_patreon_endpoint");
        String port = (String) config.get("spark_port");
        String[] values = {discordEndpoint, patreonEndpoint, port};
        if (Helpers.isNotNullOrEmpty(values)) {
            app.logger.warning("Spark settings are invalid.");
            isValid = false;
        }
        return isValid;
    }

    private static boolean validatePostgresConfig() {
        String database = (String) config.get("postgres_database");
        String user = (String) config.get("postgres_user");
        String password = (String) config.get("postgres_password");
        String host = (String) config.get("postgres_host");
        String port = (String) config.get("postgres_port");
        boolean isValid = true;
        String[] values = {database,user, password, host, port};
        if (Helpers.isNotNullOrEmpty(values)) {
            app.logger.warning("Postgres settings are invalid.");
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
        Object[] values = {openAIChatCompletion, openAIChatModel, openAIChatModeration, openAIChatStop, openAIChatStream, openAIChatTemperature, openAIChatTopP};
        if (Helpers.isNotNullOrEmpty(values)) {
            app.logger.warning("OpenAI settings are invalid.");
            isValid = false;
        }
        return isValid; // Returns true if the Postgres config is properly set
    }

    private static boolean validateWebHeadersConfig() {
        Map<String, Object> webHeaders = (Map<String, Object>) config.get("web_headers");
        boolean isValid = true;
        for (Map.Entry<String, Object> entry : webHeaders.entrySet()) {
            String api = entry.getKey();
            Map<String, String> headers = (Map<String, String>) entry.getValue();
            for (Map.Entry<String, String> header : headers.entrySet()) {
                String key = header.getKey();
                String value = header.getValue();
                Object[] values = {webHeaders, key, value};
                if (Helpers.isNotNullOrEmpty(values)) {
                    app.logger.warning("Web headers configuration is invalid.");
                    isValid = false;
                }
            }
        }
        return isValid;
    }

    public static class ConfigSection {

        private Map<String, Object> values;

        public ConfigSection(Map<String, Object> values) {
            values = values;
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
