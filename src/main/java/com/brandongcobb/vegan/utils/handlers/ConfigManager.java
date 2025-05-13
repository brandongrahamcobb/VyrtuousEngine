/*  ConfigManager.java This hosts configuration data.
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
package com.brandongcobb.vegan.utils.handlers;

import com.brandongcobb.vegan.Application;
import com.brandongcobb.vegan.utils.inc.*;

import java.io.*;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ConfigManager<T> {

    private static ConfigManager instance;
    private Map<String, Object> config = new HashMap<>();
    private Logger logger = Logger.getLogger("Application");
    private Map<String, Object> defaultConfig = getDefaultConfig();

    public ConfigManager() {
        instance = this;
    }

    /*
     * Setters
     *
     */
    public void completeSetAndLoadConfig() {
        File folder = Application.getInstance().getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            logger.severe("Failed to create data folder: " + folder.getAbsolutePath());
            return;
        }
        File configFile = new File(folder, "config.yml");
        if (!configFile.exists()) {
            try (Writer writer = new FileWriter(configFile)) {
                DumperOptions options = new DumperOptions();
                options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
                new Yaml(options).dump(defaultConfig, writer);
            } catch (IOException e) {
                logger.severe("Failed to save default config: " + e.getMessage());
            }
        }
        Map<String, Object> combinedConfig = buildLayeredConfig();
        this.config = Helpers.deepMerge(new HashMap<>(), combinedConfig);
        try (Writer writer = new FileWriter(configFile)) {
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            new Yaml(options).dump(this.config, writer);
        } catch (Exception e) {
            logger.severe("Failed to save merged config: " + e.getMessage());
        }
        logger.info("Configuration loaded, merged, and saved successfully.");
    }

    public static Map<String, Object> buildLayeredConfig() {
        List<String> keys = Arrays.asList(
            "DISCORD_API_KEY", "DISCORD_CLIENT_ID", "DISCORD_CLIENT_SECRET",
            "DISCORD_COMMAND_PREFIX", "DISCORD_OWNER_ID", "DISCORD_REDIRECT_URI",
            "DISCORD_ROLE_PASS", "DISCORD_TESTING_GUILD_ID",
            "PATREON_API_KEY", "PATREON_CLIENT_ID", "PATREON_CLIENT_SECRET",
            "PATREON_REDIRECT_URI",
            "POSTGRES_DATABASE", "POSTGRES_USER", "POSTGRES_HOST",
            "POSTGRES_PASSWORD", "POSTGRES_PORT",
            "SPARK_DISCORD_ENDPOINT", "SPARK_PATREON_ENDPOINT", "SPARK_PORT"
        );
        Path globalPath = Paths.get(System.getProperty("user.home"), ".config", "lucy", "config.yml");
        Map<String, Object> globalConfig = loadYamlConfig(globalPath);
        File pluginPath = Application.getInstance().getDataFolder();
        File configFile = new File(pluginPath, "config.yml");
        Path configPath = configFile.toPath();
        Map<String, Object> pluginConfig = loadYamlConfig(configPath);
        Map<String, Object> envConfig = new HashMap<>();
        Map<String, String> envVars = System.getenv();
        for (String key : keys) {
            String envKey = key.toUpperCase();
            if (envVars.containsKey(envKey)) {
                envConfig.put(key, envVars.get(envKey));
            }
        }
        Map<String, Object> merged = new HashMap<>();
        merged = Helpers.deepMerge(merged, globalConfig);
        merged = Helpers.deepMerge(merged, pluginConfig);
        merged = Helpers.deepMerge(merged, envConfig);
        return merged;
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

    public static Map<String, Object> getDefaultConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("DISCORD_API_KEY", "");
        config.put("DISCORD_CLIENT_ID", "");
        config.put("DISCORD_CLIENT_SECRET", "");
        config.put("DISCORD_COMMAND_PREFIX", "!");
        config.put("DISCORD_OWNER_ID", "");
        config.put("DISCORD_REDIRECT_URI", "");
        config.put("DISCORD_ROLE_PASS", "");
        config.put("DISCORD_TESTING_GUILD_ID", "");
        config.put("PATREON_API_KEY", "");
        config.put("PATREON_CLIENT_ID", "");
        config.put("PATREON_CLIENT_SECRET", "");
        config.put("PATREON_REDIRECT_URI", "");
        config.put("POSTGRES_DATABASE", "");
        config.put("POSTGRES_USER", "postgres");
        config.put("POSTGRES_HOST", "localhost");
        config.put("POSTGRES_PASSWORD", "");
        config.put("POSTGRES_PORT", "");
        config.put("SPARK_DISCORD_ENDPOINT", "/oauth/discord_callback");
        config.put("SPARK_PATREON_ENDPOINT", "/oauth/patreon_callback");
        config.put("SPARK_PORT", Helpers.parseCommaNumber("8,000"));
        return config;
    }

    public static Map<String, Object> loadYamlConfig(Path path) {
        Map<String, Object> map = new HashMap<>();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                Yaml yaml = new Yaml();
                Object data = yaml.load(in);
                if (data instanceof Map) {
                    map = (Map<String, Object>) data;
                }
            } catch (Exception e) {
                // optionally log
            }
        }
        return map;
    }


    public static synchronized ConfigManager getInstance() {
        return instance;
    }

    /*
     * Validation
     *
     */
    private CompletableFuture<Boolean> completeValidatePostgresConfig() {
        return CompletableFuture.supplyAsync(() -> {
            String database = (String) this.config.get("POSTGRES_DATABASE");
            String user = (String) this.config.get("POSTGRES_USER");
            String password = (String) this.config.get("POSTGRES_PASSWORD");
            String host = (String) this.config.get("POSTGRES_HOST");
            String port = (String) this.config.get("POSTGRES_PORT");
            boolean isValid = true;
            String[] values = {database, user, password, host, port};
            if (Helpers.isNullOrEmpty(values)) {
                logger.warning("Postgres settings are invalid.");
                isValid = false;
            }
            return isValid;
        });
    }
}
