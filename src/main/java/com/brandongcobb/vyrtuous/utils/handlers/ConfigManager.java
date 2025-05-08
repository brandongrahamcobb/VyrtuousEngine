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
import java.util.concurrent.ConcurrentMap;
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
    private Map<Long, String> userModelSettings = new HashMap<>();

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

    public CompletableFuture<Map<Long, String>> completeGetUserModelSettings() {
        return CompletableFuture.completedFuture(this.userModelSettings);
    }

    /*
     * Setters
     *
     */
    public void completeSetUserModelSettings(Map<Long, String> userModelSettings) {
        this.userModelSettings = userModelSettings;
    }

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
        validations.add(completeValidateDiscordConfig());
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


    private CompletableFuture<Boolean> completeValidateDiscordConfig() {
        return CompletableFuture.supplyAsync(() -> {
            String discordApiKey = (String) String.valueOf(this.config.get("discord_api_key"));
            boolean isValid = true;
            if (discordApiKey == null) {
                logger.warning("Set DISCORD_API_KEY=<your-key>.");
                isValid = false;
            }
            return isValid;
        });
    }

    private CompletableFuture<Boolean> completeValidateOpenAIConfig() {
        return CompletableFuture.supplyAsync(() -> {
            String openAIApiKey = (String) String.valueOf(this.config.get("openai_api_key")); // or config.get directly for static
            boolean openAIChatCompletion = Boolean.parseBoolean(String.valueOf(this.config.get("openai_chat_completion")));
            String openAIChatModel = (String) String.valueOf(this.config.get("openai_chat_model"));
            boolean openAIChatModeration = Boolean.parseBoolean(String.valueOf(this.config.get("openai_chat_moderation")));
            String openAIChatStop = (String) String.valueOf(this.config.get("openai_chat_stop"));
            boolean openAIChatStream = Boolean.parseBoolean(String.valueOf(this.config.get("openai_chat_stream")));
            float openAIChatTemperature = Float.parseFloat(String.valueOf(this.config.get("openai_chat_temperature")));
            float openAIChatTopP = Float.parseFloat(String.valueOf(this.config.get("openai_chat_top_p")));
            boolean isValid = true;
            if (openAIApiKey == null || openAIApiKey.trim().isEmpty()) {
                logger.warning("Set OPENAI_API_KEY=<your-key>.");
                isValid = false;
            }
            if (!openAIChatCompletion) {
                logger.warning("OpenAI responses are off.");
            }
            if (!openAIChatModeration) {
                logger.warning("OpenAI moderations are off, completions inherits and is also off..");
            }
            if (openAIChatStream) {
                logger.warning("OpenAI chat streaming is not yet supported.");
            }
            // Optional: check other values
            return isValid;
        });
    }
}
