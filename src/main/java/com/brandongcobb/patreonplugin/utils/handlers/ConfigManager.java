/*  ConfigManager.java The purpose of this program is to serve as the configuration manager for PatreonPlugin.
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
package com.brandongcobb.patreonplugin.utils.handlers;

import com.brandongcobb.patreonplugin.PatreonPlugin;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ConfigManager {

    private static PatreonPlugin plugin;
    public static Map<String, Object> config = new HashMap<>();
    private static int i;
    private static Logger logger;

    public ConfigManager(PatreonPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public static Map<String, Object> getConfig() {
        return config;
    }

    private static void createDefaultConfig(File configFile) {
        config.clear();  // Clear any existing config entries
        config.put("Patreon", new HashMap<String, String>() {{
            put("api_key", "YOUR_API_KEY_HERE"); // Placeholder for API key
            put("client_id", "YOUR_CLIENT_ID_HERE");
            put("client_secret", "YOUR_CLIENT_SECRET_HERE");
            put("redirect_uri", "http://localhost:8000");
        }});
        config.put("Postgres", new HashMap<String, String>() {{
            put("host", "localhost"); // Default PostgreSQL host
            put("database", "your_database"); // Default database name
            put("user", "your_username"); // Default user name
            put("password", "your_password"); // Default password
            put("port", "5432"); // Default Postgres port
        }});
        config.put("Spark", new HashMap<String, String>() {{
            put("port", "8000"); // Default port for Spark
            put("redirect_uri_endpoint", "/oauth/patreon_callback"); // Default port for Spark
        }});
        saveConfig(configFile); // Save the default config
    }

    private static void saveConfig(File configFile) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs(); // Create directories if they don't exist
            }
            try (Writer writer = new FileWriter(configFile)) {
                yaml.dump(config, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ConfigSection getConfigValue(String outerKey, String innerKey) {
        Map<String, Object> outerMap = (Map<String, Object>) config.get(outerKey);
        if (outerMap != null) {
            Object innerValue = outerMap.get(innerKey);
            if (innerValue instanceof Map) {
                return new ConfigSection((Map<String, Object>) innerValue);
            }
        }
        return null; // Return null or handle accordingly if the outer key doesn't exist
    }

    private static void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml"); // Use getDataFolder for correct path
        if (configFile.exists()) {
            try (InputStream inputStream = new FileInputStream(configFile)) {
                Yaml yaml = new Yaml();
                config = yaml.load(inputStream);
                validateConfig(config);
            } catch (IOException e) {
                logger.severe("Failed to load config: " + e.getMessage());
            } catch (Exception e) {
                logger.severe("The config file is corrupted. Please delete it or fix it. Error: " + e.getMessage());
            }
        } else {
            createDefaultConfig(configFile); // Create default config if it doesn't exist
        }
    }

    private static void validateConfig(Map<String, Object> config) {
        if (!config.containsKey("Patreon") || !config.containsKey("Postgres")) {
            logger.warning("The config is missing required sections. A default config will be created.");
        }
    }
}
