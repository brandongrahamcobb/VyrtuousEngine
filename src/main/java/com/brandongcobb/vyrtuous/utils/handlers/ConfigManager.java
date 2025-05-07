/*  ConfigManager.java The primary purpose of this class is to
 *  handle the application's configuration. The config file is
 *  config.yml in the app's datafolder.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General private License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General private License for more details.
 *
 *  You should have received a copy of the GNU General private License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ConfigManager<T> {

    private static Vyrtuous app;
    private static Map<String, Object> config;
    private static Map<String, Object> defaultConfig;
    private static Logger logger = Logger.getLogger("Vyrtuous");
    private Function<Object, T> converter;

    static {
        config = new HashMap<>();
        defaultConfig = Helpers.populateConfig(new HashMap<>());
    }

    public ConfigManager() {
        completeConfigure();
    }

    private static CompletableFuture<Void> completeConfigure() {
        return completeSetConfig()
            .thenCompose(config -> CompletableFuture.completedFuture(config.equals(defaultConfig)))
            .thenCompose(isDefault -> {
                if (isDefault) {
                    return CompletableFuture.failedFuture(
                        new IllegalStateException("Config is default/invalid"));
                } else {
                    return ConfigManager.completeValidateConfig();
                }
            })
            .exceptionally(ex -> {
                logger.severe("Error during initialization: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
    }

    public static CompletableFuture<Void> completeLoadConfig() {
        return completeGetDataFolder()
            .thenCompose(folder -> {
                File configFile = new File(folder, "config.yml");
                CompletableFuture<Void> ensureExists = configFile.exists()
                    ? CompletableFuture.completedFuture(null)
                    : completeSetConfig().thenApply(cfg -> null);
                return ensureExists.thenCompose(unused2 -> CompletableFuture.supplyAsync(() -> {
                    Yaml yaml = new Yaml();
                    try (InputStream inputStream = new FileInputStream(configFile)) {
                        Map<String, Object> loadedConfig = yaml.load(inputStream);
                        if (loadedConfig == null) {
                            loadedConfig = new HashMap<>();
                        }
                        config = Helpers.deepMerge(defaultConfig, loadedConfig);
                    } catch (Exception e) {
                        logger.severe("Failed to load config: " + e.getMessage());
                        throw new RuntimeException("Failed to load config", e);
                    }
                    return null;
                }));
            })
            .thenRun(() -> logger.info("All configuration values are loaded and merged successfully."))
            .exceptionally(ex -> {
                logger.severe("Failed to load configuration values: " + ex.getMessage());
                return null;
            });
    }

    /*
     * Getters
     *
     */
    public static <T> CompletableFuture<T> completeGetConfigValue(String key, Class<T> type) {
        return completeGetConfigObjectValue(key).thenApply(value -> {
            if (value == null) return null;
            try {
                return Helpers.convertValue(value, type);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private static CompletableFuture<Object> completeGetConfigObjectValue(String key) {
        return CompletableFuture.supplyAsync(() -> config.get(key));
    }

    public static CompletableFuture<File> completeGetDataFolder() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI location = Vyrtuous.class.getProtectionDomain().getCodeSource().getLocation().toURI();
                File currentDir = new File(location).getParentFile();
                return currentDir;
            } catch (Exception e) {
                throw new RuntimeException("Failed to determine program folder path", e);
            }
        });
    }

    /*
     * Setters
     *
     */
    private static CompletableFuture<Void> completeSetApp(Vyrtuous plugin) {
        return CompletableFuture.runAsync(() -> app = plugin);
    }

    private static CompletableFuture<Map<String, Object>> completeSetConfig() {
        return completeGetDataFolder()
            .thenCompose(dataFolder -> CompletableFuture.supplyAsync(() -> {
                if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                    logger.severe("Failed to create data folder: " + dataFolder.getAbsolutePath());
                    return null; // early exit
                }
                File configFile = new File(dataFolder, "config.yml");
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                Yaml yaml = new Yaml(options);
                try (Writer writer = new FileWriter(configFile)) {
                    yaml.dump(config, writer);
                } catch (IOException e) {
                    logger.severe("Failed to save config: " + e.getMessage());
                }
                return config;
            }));
    }

    /*
     * Validation
     *
     */
    private static CompletableFuture<Void> completeValidateConfig() {
        List<CompletableFuture<Boolean>> validations = new ArrayList<>();
        validations.add(completeValidateApiConfig("discord"));
        validations.add(completeValidateApiConfig("openai"));
        validations.add(completeValidateApiConfig("patreon"));
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
                    logger.severe("No valid API configurations found. Please check your configuration.");
                }
                return null;
            });
    }

    private static CompletableFuture<Boolean> completeValidateApiConfig(String api) {
        return CompletableFuture.supplyAsync(() -> {
            String apiKey = api + "_api_key";
            String clientIdKey = api + "_client_id";
            String clientSecretKey = api + "_client_secret";
            String redirectUriKey = api + "_redirect_uri";
            boolean hasValidData = false;
            String apiKeyValue = (String) config.get(apiKey);
            if (Helpers.isNullOrEmpty(new Object[]{apiKeyValue})) {
                logger.warning(api + " API key is missing or invalid.");
            } else {
                hasValidData = true;
            }
            String clientId = (String) config.get(clientIdKey);
            if (Helpers.isNullOrEmpty(new Object[]{clientId})) {
                logger.warning(api + " client_id is missing or invalid.");
            } else {
                hasValidData = true;
            }
            String clientSecret = (String) config.get(clientSecretKey);
            if (Helpers.isNullOrEmpty(new Object[]{clientSecret})) {
                logger.warning(api + " client_secret is missing or invalid.");
            } else {
                hasValidData = true;
            }
            String redirectUri = (String) config.get(redirectUriKey);
            if (Helpers.isNullOrEmpty(new Object[]{redirectUri})) {
                logger.warning(api + " redirect_uri is missing or invalid.");
            } else {
                hasValidData = true;
            }
            return hasValidData;
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
            if (Helpers.isNullOrEmpty(values)) {
                logger.warning("OpenAI settings are invalid.");
                isValid = false;
            }
            return isValid; // Returns true if the Postgres config is properly set
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
            if (Helpers.isNullOrEmpty(values)) {
                logger.warning("Postgres settings are invalid.");
                isValid = false;
            }
            return isValid; // Returns true if the Postgres config is properly set
        });
    }

    private static CompletableFuture<Boolean> completeValidateSparkConfig() {
        return CompletableFuture.supplyAsync(() -> {
            boolean isValid = true;
            String discordEndpoint = (String) config.get("spark_discord_endpoint");
            String patreonEndpoint = (String) config.get("spark_patreon_endpoint");
            String port = String.valueOf(config.get("spark_port"));
            String[] values = {discordEndpoint, patreonEndpoint, port};
            System.out.println(discordEndpoint + patreonEndpoint + port);
            if (Helpers.isNullOrEmpty(values)) {
                logger.warning("Spark settings are invalid.");
                isValid = false;
            }
            return isValid;
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
                    if (Helpers.isNullOrEmpty(values)) {
                        logger.warning("Web headers configuration is invalid.");
                        isValid = false;
                    }
                }
            }
            return isValid;
        });
    }
}
