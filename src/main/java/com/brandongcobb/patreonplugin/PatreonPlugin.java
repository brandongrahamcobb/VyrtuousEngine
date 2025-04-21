/*  PatreonPlugin.java The purpose of this program is to server as a drop-in plugin for minecraft servers, linking together Patreon and Minecraft accounts for server users.
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
package com.brandongcobb.patreonplugin;

import com.brandongcobb.patreonplugin.utils.listeners.PlayerJoinListener;
import com.brandongcobb.patreonplugin.utils.handlers.ConfigManager;
import com.brandongcobb.patreonplugin.utils.handlers.OAuthServer;
import com.brandongcobb.patreonplugin.utils.handlers.PatreonUser;
import com.brandongcobb.patreonplugin.utils.handlers.UserManager;
import com.brandongcobb.patreonplugin.utils.sec.PatreonOAuth;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement; // For SQL prepared statements
import java.sql.ResultSet; // For SQL result handling
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.List;
import java.util.logging.Level;
import java.util.UUID; // For handling player UUIDs
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player; // For Player entity
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable; // For creating scheduled tasks
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.google.gson.JsonObject;
import com.patreon.resources.User;
import com.patreon.PatreonAPI;

public final class PatreonPlugin extends JavaPlugin {

    public static String accessToken;
    private Timer callbackTimer;
    private FileConfiguration config;
    public static ConfigManager configManager;
    public static Connection[] conn;
    public static Connection connection;
    public LocalDateTime createDate = LocalDateTime.now();
    private HikariDataSource dataSource;
    private File dataF;
    private FileConfiguration data;
    public long discordId;
    public int exp;
    public String factionName;
    public int level;
    private boolean listeningForCallback = false;
    public String minecraftId;
    public OAuthServer oAuthServer;
    public String patreonAbout;
    public int patreonAmountCents;
    public PatreonAPI patreonApi;
    public String patreonEmail;
    public long patreonId;
    public String patreonName;
    public PatreonOAuth patreonOAuth;
    public String patreonStatus;
    public String patreonTier;
    public String patreonVanity;
    public PatreonUser patreonUser;
    public PatreonPlugin plugin;
    public Timestamp timestamp = Timestamp.valueOf(createDate);
    public UserManager userManager;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("patreon")) {
            if (this.listeningForCallback) {
                sender.sendMessage("Already listening for callback.");
                return true;
            }
            oAuthServer.start();
            this.listeningForCallback = true;
            String authUrl = PatreonOAuth.getAuthorizationUrl();
            sender.sendMessage("Please visit the following URL to authorize: " + authUrl);
            callbackTimer = new Timer();
            callbackTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    listeningForCallback = false;
                }
            }, 600000); // 10 minutes
            return true;
        }
        return false;
    }

    public void onEnable() {
        plugin = this;
        this.configManager = new ConfigManager(plugin);
        this.oAuthServer = new OAuthServer(plugin);
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        connectDatabase(() -> {
            this.patreonOAuth = new PatreonOAuth(plugin,
                                             configManager.getConfigValue("Configuration", "Patreon").getStringValue("client_id"),
                                                 configManager.getConfigValue("Configuration", "Patreon").getStringValue("client_secret"),
                                                 configManager.getConfigValue("Configuration", "Patreon").getStringValue("redirect_uri"));
            this.userManager = new UserManager(plugin);
            this.patreonUser = new PatreonUser(configManager.getConfigValue("Configuration", "Patreon").getStringValue("api_key"),
                                         configManager,
                                          discordId,
                                          exp,
                                          factionName,
                                          level,
                                          minecraftId,
                                          patreonAbout,
                                          patreonAmountCents,
                                          patreonApi,
                                          patreonEmail,
                                          patreonId,
                                          patreonName,
                                          patreonStatus,
                                          patreonTier,
                                          patreonVanity,
                                          plugin,
                                          userManager
            );
        });
    }

    private void executeMinecraftCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    public void handleOAuthCallback(String code) {
        try {
            if (!listeningForCallback) {
                this.getLogger().warning("No OAuth flow is currently active.");
                return;
            }
            accessToken = PatreonOAuth.exchangeCodeForToken(code);
            long userId = Long.parseLong(String.valueOf(PatreonUser.getCurrentUserId(accessToken)));
            int userAmountCents = Integer.parseInt(String.valueOf(PatreonUser.getCurrentPatreonAmountCents(accessToken)));
            PatreonUser.userExists(String.valueOf(userId), exists -> {
                if (!exists) {
                    PatreonUser.createUser(timestamp, 0L, 0, "", 1, "", "", userAmountCents, "", userId, "", "", "", "", () -> {
                    });
                }
                listeningForCallback = false; // Reset the callback flag
                if (callbackTimer != null) {
                    callbackTimer.cancel();
                    callbackTimer = null;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectDatabase(Runnable afterConnect) {
        this.getLogger().log(Level.INFO, "Initializing PostgreSQL connection pool...");
        new BukkitRunnable() {
            @Override
            public void run() {
                String host = getConfig().getString("postgres_host", "jdbc:postgresql://" + configManager.getConfigValue("Configuration", "Postgres").getStringValue("host"));
                String db = getConfig().getString("postgres_db", configManager.getConfigValue("Configuration", "Postgres").getStringValue("database"));
                String user = getConfig().getString("postgres_user", configManager.getConfigValue("Configuration", "Postgres").getStringValue("user"));
                String password = getConfig().getString("postgres_password", configManager.getConfigValue("Configuration", "Postgres").getStringValue("password"));
                String port = getConfig().getString("postgres_port", configManager.getConfigValue("Configuration", "Postgres").getStringValue("port"));
                String jdbcUrl = String.format("%s:%s/%s", host, port, db);
                getLogger().info("Connecting to: " + jdbcUrl);
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setJdbcUrl(jdbcUrl);
                hikariConfig.setUsername(user);
                hikariConfig.setPassword(password);
                hikariConfig.setDriverClassName("org.postgresql.Driver");
                hikariConfig.setLeakDetectionThreshold(2000);
                try {
                    dataSource = new HikariDataSource(hikariConfig);
                    getLogger().log(Level.INFO, "PostgreSQL connection pool initialized.");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            afterConnect.run(); // Execute after connection is confirmed, back on main thread
                        }
                    }.runTask(PatreonPlugin.this);
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Failed to initialize PostgreSQL connection pool!", e);
                }
            }
        }.runTaskAsynchronously(this);
    }

    public void getConnection(Consumer<Connection> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Connection[] conn = {null}; // Use an array to hold the connection
                try {
                    if (dataSource == null) {
                        getLogger().warning("DataSource not initialized");
                        Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                        return;
                    }
                    conn[0] = dataSource.getConnection(); // Get the connection
                    getLogger().log(Level.INFO, "PostgreSQL connection opened.");
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(conn[0]));
                } catch (SQLException e) {
                    e.printStackTrace(); // Handle potential SQLException
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                }
            }
        }.runTaskAsynchronously(plugin); // Run asynchronously
    }

    public void closeDatabase() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }

    public void onDisable() {
        this.closeDatabase();
        this.getLogger().log(Level.INFO, "PostgreSQL Example plugin disabled.");
        if (callbackTimer != null) {
            callbackTimer.cancel();
        }
        oAuthServer.stop();
    }
}
