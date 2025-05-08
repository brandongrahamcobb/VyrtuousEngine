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

    private Vyrtuous app;
    private ConfigManager instance;
    private Map<String, Object> config = new HashMap<>();
    private Logger logger;
    private Function<Object, T> converter;
    private Map<String, Object> defaultConfig = Helpers.populateConfig(new HashMap<>());

    public ConfigManager(Vyrtuous application) {
        instance = this;
        this.app = application;
        this.logger = Logger.getLogger("Vyrtuous");
    }

    public CompletableFuture<Void> completeSetAndLoadConfig() {
        return completeGetDataFolder()
            .thenCompose(folder -> {
                File configFile = new File(folder, "config.yml");
                CompletableFuture<Void> ensureExists = configFile.exists()
                    ? CompletableFuture.completedFuture(null)
                    : CompletableFuture.supplyAsync(() -> {
                        if (!folder.exists() && !folder.mkdirs()) {
                            logger.severe("Failed to create data folder: " + folder.getAbsolutePath());
                            return null;
                        }
                        DumperOptions options = new DumperOptions();
                        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                        Yaml yaml = new Yaml(options);
                        try (Writer writer = new FileWriter(configFile)) {
                            yaml.dump(defaultConfig, writer);
                        } catch (IOException e) {
                            logger.severe("Failed to save config: " + e.getMessage());
                        }
                        return null;
                    });
                return ensureExists.thenCompose(unused -> CompletableFuture.supplyAsync(() -> {
                    Yaml yaml = new Yaml();
                    try (InputStream inputStream = new FileInputStream(configFile)) {
                        Map<String, Object> loadedConfig = yaml.load(inputStream);
                        if (loadedConfig == null) {
                            loadedConfig = new HashMap<>();
                        }
                        if (defaultConfig.equals(loadedConfig)) {
                            logger.severe("Configuration has not been set");
                        }
                        this.config = Helpers.deepMerge(this.defaultConfig, loadedConfig);
                    } catch (Exception e) {
                        logger.severe("Failed to load config: " + e.getMessage()); throw new RuntimeException("Failed to load config", e);
                    }
                    return null;
                }));
            })
            .thenRun(() -> logger.info("Configuration loaded and merged successfully."))
            .exceptionally(ex -> {
                logger.severe("Failed to load configuration values: " + ex.getMessage());
                return null;
            });
    }
    /*
     * Getters
     *
     */
    public <T> CompletableFuture<T> completeGetConfigValue(String key, Class<T> type) {
        return completeGetConfigObjectValue(key).thenApply(value -> {
            if (value == null) return null;
            try {
                return (T) Helpers.convertValue(value, type);
            } catch (Exception e) {
                return null;
            }
        });
    }

    private CompletableFuture<Object> completeGetConfigObjectValue(String key) {
        return CompletableFuture.supplyAsync(() -> this.config.get(key));
    }

    public CompletableFuture<File> completeGetDataFolder() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI location = this.app.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
                File currentDir = new File(location).getParentFile();
                return currentDir;
            } catch (Exception e) {
                throw new RuntimeException("Failed to determine program folder path", e);
            }
        });
    }

    public synchronized ConfigManager completeGetInstance() {
        return this.instance;
    }

    /*
     * Setters
     *
     */
    private CompletableFuture<Map<String, Object>> completeSetConfig() {
        return completeGetDataFolder()
            .thenCompose(dataFolder -> CompletableFuture.supplyAsync(() -> {
                if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                    logger.severe("Failed to create data folder: " + dataFolder.getAbsolutePath());
                    return null;
                }
                File configFile = new File(dataFolder, "config.yml");
                boolean configExistsAndValid = false;
                if (configFile.exists()) {
                    try (InputStream inputStream = new FileInputStream(configFile)) {
                        Yaml yaml = new Yaml();
                        Map<String, Object> existingConfig = yaml.load(inputStream);
                        if (existingConfig != null && !existingConfig.equals(new HashMap<>())) {
                            configExistsAndValid = true;
                        }
                    } catch (Exception e) {
                        logger.warning("Could not read existing config: " + e.getMessage());
                    }
                }
                if (!configExistsAndValid) {
                    DumperOptions options = new DumperOptions();
                    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                    Yaml yaml = new Yaml(options);
                    try (Writer writer = new FileWriter(configFile)) {
                        yaml.dump(defaultConfig, writer);
                    } catch (IOException e) {
                        logger.severe("Failed to save config: " + e.getMessage());
                    }
                } else {
                    try (InputStream inputStream = new FileInputStream(configFile)) {
                        Yaml yaml = new Yaml();
                        Map<String, Object> loadedConfig = yaml.load(inputStream);
                        if (loadedConfig != null) {
                            this.config = Helpers.deepMerge(this.defaultConfig, loadedConfig);
                        }
                    } catch (Exception e) {
                        logger.warning("Could not read existing config: " + e.getMessage());
                    }
                }
                return defaultConfig;
            }));
    }

    /*
     * Validation
     *
     */
    public CompletableFuture<Void> completeValidateConfig() {
        List<CompletableFuture<Boolean>> validations = new ArrayList<>();
        validations.add(completeValidateApiConfig("discord"));
        validations.add(completeValidateApiConfig("openai"));
//        validations.add(completeValidateApiConfig("patreon"));
        validations.add(completeValidatePostgresConfig());
  //      validations.add(completeValidateSparkConfig());
   //     validations.add(completeValidateWebHeadersConfig());
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

    private CompletableFuture<Boolean> completeValidateApiConfig(String api) {
        return CompletableFuture.supplyAsync(() -> {
            String apiKey = api + "_api_key";
            String clientIdKey = api + "_client_id";
            String clientSecretKey = api + "_client_secret";
            String redirectUriKey = api + "_redirect_uri";
            boolean hasValidData = false;
            String apiKeyValue = (String) this.config.get(apiKey);
            if (Helpers.isNullOrEmpty(new Object[]{apiKeyValue})) {
                logger.warning(api + " API key is missing or invalid.");
            } else {
                hasValidData = true;
            }
            String clientId = (String) this.config.get(clientIdKey);
            if (Helpers.isNullOrEmpty(new Object[]{clientId})) {
                logger.warning(api + " client_id is missing or invalid.");
            } else {
                hasValidData = true;
            }
            String clientSecret = (String) this.config.get(clientSecretKey);
            if (Helpers.isNullOrEmpty(new Object[]{clientSecret})) {
                logger.warning(api + " client_secret is missing or invalid.");
            } else {
                hasValidData = true;
            }
            String redirectUri = (String) this.config.get(redirectUriKey);
            if (Helpers.isNullOrEmpty(new Object[]{redirectUri})) {
                logger.warning(api + " redirect_uri is missing or invalid.");
            } else {
                hasValidData = true;
            }
            return hasValidData;
        });
    }

    private CompletableFuture<Boolean> completeValidateOpenAIConfig() {
        return CompletableFuture.supplyAsync(() -> {
            boolean openAIChatCompletion = (boolean) Boolean.parseBoolean(String.valueOf(this.config.get("openai_chat_completion")));
            String openAIChatModel = (String) String.valueOf(this.config.get("openai_chat_model"));
            boolean openAIChatModeration = (boolean) Boolean.parseBoolean(String.valueOf(this.config.get("openai_chat_moderation")));
            String openAIChatStop = (String) String.valueOf(this.config.get("openai_chat_stop"));
            boolean openAIChatStream = (boolean) Boolean.parseBoolean(String.valueOf(this.config.get("openai_chat_stream")));
            float openAIChatTemperature = (float) Float.parseFloat(String.valueOf(this.config.get("openai_chat_temperature")));
            float openAIChatTopP = (float) Float.parseFloat(String.valueOf(this.config.get("openai_chat_top_p")));
            boolean isValid = true;
            if (!openAIChatCompletion) {
                logger.warning("OpenAI responses are off.");
            }
            if (!openAIChatModeration) {
                logger.warning("OpenAI moderations are off, completions inherits and is also off..");
            }
            if (openAIChatStream) {
                logger.warning("OpenAI chat streaming is not yet supported.");
            }
            Object[] values = {openAIChatModel, openAIChatStop, openAIChatTemperature, openAIChatTopP}; if (Helpers.isNullOrEmpty(values)) {
                logger.warning("OpenAI settings are invalid.");
                isValid = false;
            }
            return isValid;
        });
    }

    private CompletableFuture<Boolean> completeValidatePostgresConfig() {
        return CompletableFuture.supplyAsync(() -> {
            String database = (String) this.config.get("postgres_database");
            String user = (String) this.config.get("postgres_user");
            String password = (String) this.config.get("postgres_password");
            String host = (String) this.config.get("postgres_host");
            String port = (String) this.config.get("postgres_port");
            boolean isValid = true;
            String[] values = {database, user, password, host, port};
            if (Helpers.isNullOrEmpty(values)) {
                logger.warning("Postgres settings are invalid.");
                isValid = false;
            }
            return isValid;
        });
    }

//    private CompletableFuture<Boolean> completeValidateSparkConfig() {
//        return CompletableFuture.supplyAsync(() -> {
//            boolean isValid = true;
//            String discordEndpoint = (String) this.config.get("spark_discord_endpoint");
//            String patreonEndpoint = (String) this.config.get("spark_patreon_endpoint");
//            String port = String.valueOf(this.config.get("spark_port"));
//            String[] values = {discordEndpoint, patreonEndpoint, port};
//            System.out.println(discordEndpoint + patreonEndpoint + port);
//            if (Helpers.isNullOrEmpty(values)) {
//                logger.warning("Spark settings are invalid.");
//                isValid = false;
//            }
//            return isValid;
//        });
//    }
//
//    private CompletableFuture<Boolean> completeValidateWebHeadersConfig() {
//        return CompletableFuture.supplyAsync(() -> {
//            Map<String, Object> webHeaders = (Map<String, Object>) this.config.get("web_headers");
//            boolean isValid = true;
//            for (Map.Entry<String, Object> entry : webHeaders.entrySet()) {
//                String api = entry.getKey();
//                Map<String, String> headers = (Map<String, String>) entry.getValue();
//                for (Map.Entry<String, String> header : headers.entrySet()) {
//                    String key = header.getKey();
//                    String value = header.getValue();
//                    Object[] values = {webHeaders, key, value};
//                    if (Helpers.isNullOrEmpty(values)) {
//                        logger.warning("Web headers configuration is invalid.");
//                        isValid = false;
//                    }
//                }
//            }
//            return isValid;
//        });
//    }
}
