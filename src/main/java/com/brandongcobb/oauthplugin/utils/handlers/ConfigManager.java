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

package com.brandongcobb.oauthplugin.utils.handlers;

import com.brandongcobb.oauthplugin.OAuthPlugin;
import com.brandongcobb.oauthplugin.utils.inc.Helpers;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.net.URI;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager<T> {

    /**
     * Build the default config map from the template.
     */
    private static Map<String, Object> getDefaultConfig() {
        return Helpers.populateConfig(new java.util.HashMap<>());
    }

    private OAuthPlugin app;
    private static ConfigManager instance;
    private Map<String, Object> config = new HashMap<>();
    private Logger logger = Logger.getLogger("OAuthPlugin");
    private Function<Object, T> converter;
    private Map<String, Object> defaultConfig = getDefaultConfig();

    public ConfigManager(OAuthPlugin application) {
        instance = this;
        this.app = application;
    }

    /**
     * Load (or copy) a fully-commented config.yml template on first run,
     * then merge it into an in-memory map (preserving comments on disk),
     * validate, and check feature toggles.
     *
     * @return true if Discord or Patreon is enabled; false otherwise.
     */
    public boolean setLoadAndVerifyStartup() {
        // Ensure data folder
        File folder = app.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            logger.severe("Failed to create data folder: " + folder.getAbsolutePath());
            return false;
        }
        File configFile = new File(folder, "config.yml");
        // Copy commented template if missing
        if (!configFile.exists()) {
            app.saveResource("config.yml", false);
        }
        // Merge runtime settings
        Map<String, Object> mergedConfig = buildLayeredConfig();
        this.config = Helpers.deepMerge(new HashMap<>(), mergedConfig);
        // Make config readable/writable
        try {
            configFile.setReadable(true, false);
            configFile.setWritable(true, true);
        } catch (Exception e) {
            logger.warning("Could not set config file permissions: " + e.getMessage());
        }
        // Validate
        validateConfig();
        logger.info("Configuration loaded and validated successfully.");
        // Feature toggles
        boolean discordEnabled = false, patreonEnabled = false;
        Object od = this.config.get("ENABLE_DISCORD");
        if (od instanceof Boolean) discordEnabled = (Boolean) od;
        else if (od instanceof String) discordEnabled = Boolean.parseBoolean((String) od);
        Object op = this.config.get("ENABLE_PATREON");
        if (op instanceof Boolean) patreonEnabled = (Boolean) op;
        else if (op instanceof String) patreonEnabled = Boolean.parseBoolean((String) op);
        return discordEnabled || patreonEnabled;
    }

    private static Map<String, Object> buildLayeredConfig() {
        List<String> keys = Arrays.asList(
            "DISCORD_CLIENT_ID", "DISCORD_CLIENT_SECRET", "DISCORD_REDIRECT_URI",
            "ENABLE_DISCORD", "ENABLE_PATREON",
            "PATREON_CLIENT_ID", "PATREON_CLIENT_SECRET", "PATREON_REDIRECT_URI",
            "POSTGRES_DATABASE", "POSTGRES_USER", "POSTGRES_HOST",
            "POSTGRES_PASSWORD", "POSTGRES_PORT",
            "SPARK_DISCORD_ENDPOINT", "SPARK_PATREON_ENDPOINT", "SPARK_PORT",
            // Message keys are also merged but rarely overridden via env
            "MSG_ONLY_PLAYERS", "MSG_PROVIDE_CODE", "MSG_NO_PENDING_AUTH",
            "MSG_INVALID_CODE", "MSG_PROCESSING_AUTH",
            "MSG_DISCORD_LINK_SUCCESS", "MSG_DISCORD_LINK_FAIL",
            "MSG_PATREON_LINK_SUCCESS", "MSG_PATREON_LINK_FAIL",
            "MSG_AUTHORIZE_DISCORD", "MSG_AUTHORIZE_PATREON",
            "MSG_DISCORD_TIMEOUT", "MSG_PATREON_TIMEOUT",
            "MSG_DISCORD_CALLBACK", "MSG_PATREON_CALLBACK"
        );
        Path globalPath = Paths.get(System.getProperty("user.home"), ".config", "oauthplugin", "config.yml");
        Map<String, Object> globalConfig = loadYamlConfig(globalPath);
        // Use the singleton instance to access the plugin data folder
        File pluginPath = instance.app.getDataFolder();
        Path pluginConfigPath = new File(pluginPath, "config.yml").toPath();
        Map<String, Object> pluginConfig = loadYamlConfig(pluginConfigPath);
        Map<String, Object> envConfig = new HashMap<>();
        System.getenv().forEach((k, v) -> {
            if (keys.contains(k) || keys.contains(k.toUpperCase())) {
                envConfig.put(k, v);
            }
        });
        Map<String, Object> merged = new HashMap<>();
        merged = Helpers.deepMerge(merged, globalConfig);
        merged = Helpers.deepMerge(merged, pluginConfig);
        merged = Helpers.deepMerge(merged, envConfig);
        return merged;
    }

    public <U> CompletableFuture<U> completeGetConfigValue(String key, Class<U> type) {
        return CompletableFuture.supplyAsync(() -> this.config.get(key))
            .thenApply(value -> {
                if (value == null) return null;
                try {
                    return (U) Helpers.convertValue(value, type);
                } catch (Exception e) {
                    return null;
                }
            });
    }

    private static Map<String, Object> loadYamlConfig(Path path) {
        try {
            if (Files.exists(path)) {
                Yaml yaml = new Yaml();
                try (InputStream in = Files.newInputStream(path)) {
                    Object obj = yaml.load(in);
                    if (obj instanceof Map) return (Map<String, Object>) obj;
                }
            }
        } catch (Exception ignored) { }
        return new HashMap<>();
    }


    private CompletableFuture<File> completeGetDataFolder() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URI location = this.app.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
                File currentDir = new File(location).getParentFile();
                return currentDir;
            } catch (Exception e) {
                throw new RuntimeException("Failed to determine or create program folder path", e);
            }
        });
    }

    public static synchronized ConfigManager getInstance() {
        return instance;
    }

    /*
     * Validation
     *
     */
    private void validateConfig() {
        List<Boolean> validations = new ArrayList<>();
        // Validate each configuration section
        validations.add(validateApiConfig("DISCORD"));
        validations.add(validateApiConfig("PATREON"));
        validations.add(validatePostgresConfig());
        validations.add(validateSparkConfig());
        boolean anyValid = validations.stream().anyMatch(valid -> Boolean.TRUE.equals(valid));
        if (!anyValid) {
            logger.severe("No valid API configurations found. Please check your configuration.");
        }
    }

    private boolean validateApiConfig(String api) {
        String clientIdKey = api + "_CLIENT_ID";
        String clientSecretKey = api + "_CLIENT_SECRET";
        String redirectUriKey = api + "_REDIRECT_URI";
        boolean hasValidData = false;
        String clientId = (String) this.config.get(clientIdKey);
        if (Helpers.isNullOrEmpty(new Object[]{clientId})) {
            logger.warning(api + "_CLIENT_ID is missing or invalid.");
        } else {
            hasValidData = true;
        }
        String clientSecret = (String) this.config.get(clientSecretKey);
        if (Helpers.isNullOrEmpty(new Object[]{clientSecret})) {
            logger.warning(api + "_CLIENT_SECRET is missing or invalid.");
        } else {
            hasValidData = true;
        }
        String redirectUri = (String) this.config.get(redirectUriKey);
        if (Helpers.isNullOrEmpty(new Object[]{redirectUri})) {
            logger.warning(api + "_REDIRECT_URI is missing or invalid.");
        } else {
            hasValidData = true;
        }
        return hasValidData;
    }

    private boolean validatePostgresConfig() {
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
    }

    private boolean validateSparkConfig() {
        boolean isValid = true;
        String discordEndpoint = (String) this.config.get("SPARK_DISCORD_ENDPOINT");
        String patreonEndpoint = (String) this.config.get("SPARK_PATREON_ENDPOINT");
        String port = String.valueOf(this.config.get("SPARK_PORT"));
        String[] values = {discordEndpoint, patreonEndpoint, port};
        System.out.println(discordEndpoint + patreonEndpoint + port);
        if (Helpers.isNullOrEmpty(values)) {
            logger.warning("Spark settings are invalid.");
            isValid = false;
        }
        return isValid;
    }

}
