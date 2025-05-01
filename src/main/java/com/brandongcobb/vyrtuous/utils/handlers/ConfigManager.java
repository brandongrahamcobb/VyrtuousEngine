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
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ConfigManager {

    private static Vyrtuous app;
    private static Map<String, Object> config;
    public static Map<String, Object> defaultConfig;
    private ConfigSection configSection;
    private Map<String, Object> inputConfigMap;
    private static Logger logger;

    static {
        config = new HashMap<>();
        defaultConfig = populateConfig(new HashMap<>());
    // Instead of an asynchronous thenAccept, do a blocking call:
    }

    public static CompletableFuture<Void> completeSetApp(Vyrtuous plugin) {
        return CompletableFuture.runAsync(() -> app = plugin);
    }

    public static CompletableFuture<Vyrtuous> completeGetApp() {
        return CompletableFuture.supplyAsync(() -> app);
    }

    public static CompletableFuture<Map<String, Object>> completeGetConfig() {
        return CompletableFuture.supplyAsync(() -> config);
    }

    public CompletableFuture<Boolean> completeExists() {
        return app.completeGetDataFolder().thenApply(folder -> {
            File configFile = new File(folder, "config.yml");
            return configFile.exists();
        });
    }

    public static CompletableFuture<Void> completeCreateDefaultConfig() {
        return app.completeGetDataFolder().thenCompose(folder -> {
            File configFile = new File(folder, "config.yml");
            return completeSaveConfig(configFile);
        });
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
            // Add other keys...
            put("Google", new HashMap<String, String>() {{
                put("api_key", "");
                put("client_id", "");
                put("client_secret", "");
                put("redirect_uri", "");
            }});
            // etc.
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
//        public static CompletableFuture<Map<String, Object>> completePopulateConfig(Map<String, Object> configMap) {
//        return CompletableFuture.supplyAsync(() -> {
//            configMap.put("discord_command_prefix", "!");
//            configMap.put("api_keys", new HashMap<String, Object>() {{
//                    put("Discord", new HashMap<String, String>() {{
//                    put("api_key", "");
//                    put("client_id", "");
//                    put("client_secret", "");
//                    put("redirect_uri", "");
//            }});
//                put("Google", new HashMap<String, String>() {{
//                    put("api_key", "");
//                    put("client_id", "");
//                    put("client_secret", "");
//                    put("redirect_uri", "");
//            }});
//                put("LinkedIn", new HashMap<String, String>() {{
//                    put("api_key", "");
//                    put("client_id", "");
//                    put("client_secret", "");
//                    put("redirect_uri", "");
//            }});
//                put("OpenAI", new HashMap<String, String>() {{
//                    put("api_key", "");
//                    put("client_id", "");
//                    put("client_secret", "");
//                    put("redirect_uri", "");
//            }});
//                put("Patreon", new HashMap<String, String>() {{
//                    put("api_key", "");
//                    put("client_id", "");
//                    put("client_secret", "");
//                    put("redirect_uri", "");
//            }});
//                put("Twitch", new HashMap<String, String>() {{
//                    put("api_key", "");
//                    put("client_id", "");
//                    put("client_secret", "");
//                    put("redirect_uri", "");
//            }});
//            }});
//            configMap.put("discord_owner_id", "YOUR DISCORD ID");
//            configMap.put("discord_role_pass", "ID FOR MODERATION BYPASS");
//            configMap.put("discord_testing_guild_id", "MAIN GUILD ID");
//            configMap.put("openai_chat_completion", false);
//            configMap.put("openai_chat_moderation", true);
//            configMap.put("openai_chat_stream", true);
//            configMap.put("openai_chat_temperature", 0.7);
//            configMap.put("openai_chat_top_p", 1.0);
//            configMap.put("postgres_database", "");
//            configMap.put("postgres_user", "postgres");
//            configMap.put("postgres_host", "localhost");
//            configMap.put("postgres_password", "");
//            configMap.put("postgres_port", "");
//            configMap.put("spark_discord_endpoint", "/oauth/discord_callback");
//            configMap.put("spark_patreon_endpoint", "/oauth/patreon_callback");
//            configMap.put("spark_port", Helpers.parseCommaNumber("8,000"));
//            configMap.put("web_headers", new HashMap<String, Object>() {{
//                put("API.Bible", new HashMap<String, String>() {{
//                    put("User-Agent", "Vyrtuous https://github.com/brandongrahamcobb/Vyrtuous.git");
//                    put("api-key", "");
//                }});
//            }});
//            return configMap;
//        });
//    }

    public static CompletableFuture<Boolean> completeIsConfigSameAsDefault() {
        return CompletableFuture.supplyAsync(() -> {
            return config.equals(defaultConfig);
        });
    }

    public static CompletableFuture<Void> completeLoadConfig() {
        // Ensure defaultConfig is fully populated before loading the file.
        return app.completeGetDataFolder()
          .thenCompose(folder -> {
              File configFile = new File(folder, "config.yml");
              CompletableFuture<Void> ensureExists = configFile.exists()
                  ? CompletableFuture.completedFuture(null)
                  : completeCreateDefaultConfig();
              return ensureExists.thenCompose(unused2 -> CompletableFuture.supplyAsync(() -> {
                  Yaml yaml = new Yaml();
                  try (InputStream inputStream = new FileInputStream(configFile)) {
                      Map<String, Object> loadedConfig = yaml.load(inputStream);
                      if (loadedConfig == null) {
                          loadedConfig = new HashMap<>();
                      }
                      // Merge loadedConfig with defaults (deep merge)
                      config = deepMerge(defaultConfig, loadedConfig);
                  } catch (Exception e) {
                      app.logger.severe("Failed to load config: " + e.getMessage());
                      throw new RuntimeException("Failed to load config", e);
                  }
                  return null;
              }));
          }).thenRun(() -> {
              app.logger.info("All configuration values are loaded and merged successfully.");
          }).exceptionally(ex -> {
              app.logger.severe("Failed to load configuration values: " + ex.getMessage());
              return null;
          });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> deepMerge(Map<String, Object> defaults, Map<String, Object> loaded) {
        Map<String, Object> merged = new HashMap<>(defaults);
        for (Map.Entry<String, Object> entry : loaded.entrySet()) {
            String key = entry.getKey();
            Object loadedVal = entry.getValue();
            if (merged.containsKey(key)) {
                Object defaultVal = merged.get(key);
                if (defaultVal instanceof Map && loadedVal instanceof Map) {
                    merged.put(key, deepMerge((Map<String, Object>) defaultVal, (Map<String, Object>) loadedVal));
                } else {
                    // Use the loaded value even if it's empty – user input wins.
                    merged.put(key, loadedVal);
                }
            } else {
                merged.put(key, loadedVal);
            }
        }
        return merged;
    }
    
    public static CompletableFuture<Object> completeGetConfigObjectValue(String key) {
        return CompletableFuture.supplyAsync(() -> {
            return config.get(key);
        });
    }

    public static CompletableFuture<String> completeGetConfigStringValue(String key) {
        return completeGetConfigObjectValue(key)
            .thenApply(value -> {
                if (value instanceof String) {
                    return (String) value;
                }
                return null;
            });
    }

    public static CompletableFuture<Integer> completeGetConfigIntegerValue(String key) {
        return completeGetConfigObjectValue(key)
            .thenApply(value -> {
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                } else if (value instanceof String) {
                    return Integer.parseInt((String) value);
                }
                return null;
            });
    }


    public static CompletableFuture<Long> completeGetConfigLongValue(String key) {
        return completeGetConfigObjectValue(key)
            .thenApply(value -> {
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                } else if (value instanceof String) {
                    return Long.parseLong((String) value);
                }
                return null; // or throw an exception if you expect a Float
            });
    }

    public static CompletableFuture<Float> completeGetConfigFloatValue(String key) {
        return completeGetConfigObjectValue(key)
            .thenApply(value -> {
                if (value instanceof Number) {
                    return ((Number) value).floatValue();
                } else if (value instanceof String) {
                    return Float.parseFloat((String) value);
                }
                return null; // or throw an exception if you expect a Float
            });
    }

    public static CompletableFuture<Boolean> completeGetConfigBooleanValue(String key) {
        return completeGetConfigObjectValue(key)
            .thenApply(value -> {
                if (value instanceof Boolean) {
                    return (Boolean) value;
                } else if (value instanceof String) {
                    return Boolean.parseBoolean((String) value);
                }
                return null;
            });
    }

    public static CompletableFuture<Void> completeSaveConfig(File configFile) {
        return app.completeGetDataFolder()
            .thenCompose(dataFolder -> CompletableFuture.supplyAsync(() -> {
                if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                    app.logger.severe("Failed to create data folder: " + dataFolder.getAbsolutePath());
                    return null; // early exit
                }
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml yaml = new Yaml(options);
                try (Writer writer = new FileWriter(configFile)) {
                    yaml.dump(config, writer);
                } catch (IOException e) {
                    app.logger.severe("Failed to save config: " + e.getMessage());
                }
                return null;
            }));
    }

    public static CompletableFuture<ConfigSection> completeGetNestedConfigValue(String outerKey, String innerKey) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> outerMap = (Map<String, Object>) config.get(outerKey);
            if (outerMap != null) {
                Object innerValue = outerMap.get(innerKey);
                if (innerValue instanceof Map) {
                    return new ConfigSection((Map<String, Object>) innerValue);
                }
            }
            return null; // Return null or handle accordingly if the outer key doesn't exist
        });
    }

    public static CompletableFuture<Void> completeValidateConfig() {
        List<CompletableFuture<Boolean>> validations = new ArrayList<>();
        String[] apis = {"Discord", "Google", "LinkedIn", "OpenAI", "Patreon", "Twitch"};
        for (String api : apis) {
            validations.add(completeValidateApiConfig(api));
        }
        validations.add(completeValidatePostgresConfig());
        validations.add(completeValidateSparkConfig());
        validations.add(completeValidateWebHeadersConfig());
        validations.add(completeValidateOpenAIConfig());
        return CompletableFuture.allOf(validations.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                boolean anyValid = validations.stream()
                    .map(CompletableFuture::join)
                    .anyMatch(valid -> Boolean.TRUE.equals(valid));
                if (!anyValid) {
                    app.logger.severe("No valid API configurations found. Please check your configuration.");
                }
                return null;
            });
    }

    private static CompletableFuture<Boolean> completeValidateApiConfig(String api) {
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    private static CompletableFuture<Boolean> completeValidateSparkConfig() {
        return CompletableFuture.supplyAsync(() -> {
            boolean isValid = true;
            String discordEndpoint = (String) config.get("spark_discord_endpoint");
            String patreonEndpoint = (String) config.get("spark_patreon_endpoint");
            String port = String.valueOf(config.get("spark_port"));
            String[] values = {discordEndpoint, patreonEndpoint, port};
            if (Helpers.isNotNullOrEmpty(values)) {
                app.logger.warning("Spark settings are invalid.");
                isValid = false;
            }
            return isValid;
        });
    }

    private static CompletableFuture<Boolean> completeValidatePostgresConfig() {
        return CompletableFuture.supplyAsync(() -> {
            String database = (String) config.get("postgres_database");
            String user = (String) config.get("postgres_user");
            String password = (String) config.get("postgres_password");
            String host = (String) config.get("postgres_host");
            String port = (String) config.get("postgres_port");
            boolean isValid = true;
            String[] values = {database, user, password, host, port};
            if (Helpers.isNotNullOrEmpty(values)) {
                app.logger.warning("Postgres settings are invalid.");
                isValid = false;
            }
            return isValid; // Returns true if the Postgres config is properly set
        });
    }

    private static CompletableFuture<Boolean> completeValidateOpenAIConfig() {
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    private static CompletableFuture<Boolean> completeValidateWebHeadersConfig() {
        return CompletableFuture.supplyAsync(() -> {
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
        });
    }

    public static class ConfigSection {

        private Map<String, Object> values;

        public ConfigSection(Map<String, Object> values) {
            this.values = values;
        }

        public CompletableFuture<String> completeGetConfigStringValue(String key) {
            return CompletableFuture.supplyAsync(() -> {
                Object value = values.get(key);
                if (value instanceof String) {
                    return (String) value;
                }
                return null; // or throw an exception if you expect a String
            });
        }
    }
}
