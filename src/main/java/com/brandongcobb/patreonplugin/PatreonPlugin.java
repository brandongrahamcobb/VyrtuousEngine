package com.brandongcobb.patreonplugin;

import com.brandongcobb.patreonplugin.Config;
import com.brandongcobb.patreonplugin.utils.listeners.PlayerJoinListener;
import com.brandongcobb.patreonplugin.OAuthServer;
import com.brandongcobb.patreonplugin.utils.sec.PatreonOAuth;
import com.brandongcobb.patreonplugin.utils.handlers.UserManager;
import com.brandongcobb.patreonplugin.utils.handlers.PatreonUser;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.patreon.resources.User;
import com.google.gson.JsonObject;
import com.patreon.PatreonAPI;
import org.bukkit.Bukkit; // For Bukkit API
import org.bukkit.entity.Player; // For Player entity
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Timer;
import java.util.TimerTask;
import org.bukkit.plugin.java.JavaPlugin; // For main plugin class
import org.bukkit.configuration.file.FileConfiguration; // For configuration handling
import java.sql.Connection; // For database connections
import java.sql.PreparedStatement; // For SQL prepared statements
import java.sql.ResultSet; // For SQL result handling
import java.sql.SQLException; // For SQL exceptions
import java.util.UUID; // For handling player UUIDs
import org.bukkit.scheduler.BukkitRunnable; // For creating scheduled tasks
import java.util.logging.Level; // For logging

public final class PatreonPlugin extends JavaPlugin {

    public static String accessToken;
    public static Config configMaster;
    public Connection connection;
    public String createDate;
    public long discordId;
    public int exp;
    public String factionName;
    public int level;
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
    public UserManager userManager;

    private HikariDataSource dataSource;
    private File dataF;
    private FileConfiguration data;
    private FileConfiguration config;


    private boolean listeningForCallback = false;
    private Timer callbackTimer;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("patreon")) {
            if (this.listeningForCallback) {
                sender.sendMessage("Already listening for callback.");
                return true;
            }
            this.listeningForCallback = true;
            String authUrl = PatreonOAuth.getAuthorizationUrl();
            sender.sendMessage("Please visit the following URL to authorize: " + authUrl);
            callbackTimer = new Timer();
            callbackTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    listeningForCallback = false;
                    sender.sendMessage("Listening for OAuth callback has timed out.");
                }
            }, 600000); // 10 minutes
            return true;
        }
        return false;
    }

    public void onEnable() {
        plugin = this;
        this.configMaster = configMaster;
        this.createConfig();
        this.createData();
        this.oAuthServer = new OAuthServer(plugin);
        this.oAuthServer.start();
        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        connectDatabase(() -> {
            this.patreonOAuth = new PatreonOAuth(plugin,
                                                 configMaster.getNestedConfigValue("api_keys", "Patreon").getStringValue("client_id"),
                                                 configMaster.getNestedConfigValue("api_keys", "Patreon").getStringValue("client_secret"),
                                                 configMaster.getNestedConfigValue("api_keys", "Patreon").getStringValue("redirect_uri"));
            this.userManager = new UserManager(plugin);
            this.patreonUser = new PatreonUser(configMaster.getNestedConfigValue("api_keys", "Patreon").getStringValue("api_key"),
                                          configMaster,
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
                getLogger().warning("No OAuth flow is wcurrently active.");
                return;
            }
            // Process the received code
            accessToken = PatreonOAuth.exchangeCodeForToken(code);
            System.out.println(accessToken);
            long userId = Long.parseLong(String.valueOf(PatreonUser.getCurrentUserId(accessToken)));
            int userAmountCents = Integer.parseInt(String.valueOf(PatreonUser.getCurrentPatreonAmountCents(accessToken)));
  //              String userEmail = patreonUser.getPatreonEmail();
//                long userId = patreonUser.getPatreonId();
    //            String userName = patreonUser.getPatreonName();
      //          String userStatus = patreonUser.getPatreonStatus();
        //        String userTier = patreonUser.getPatreonTier();
          //      String userVanity = patreonUser.getPatreonVanity();
            if (PatreonUser.userExists(String.valueOf(userId))) {
                // Create a new user since they do not exist
                PatreonUser.createUser(createDate, 0L, 0, "", 1, "", "", userAmountCents, "", userId, "", "", "", "", () -> {
                     new BukkitRunnable() {
                        @Override
                        public void run() {
                            Bukkit.getPlayer(UUID.fromString(minecraftId)).sendMessage("You have been created as a Patreon user.");
                        }
                    }.runTaskAsynchronously(plugin);
                });
            }
            startPledgeCheck();
            userManager.consolidateUsers();
            listeningForCallback = false;
            if (callbackTimer != null) {
                callbackTimer.cancel();
                callbackTimer = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startPledgeCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    String sql = "SELECT patreon_amount_cents FROM users WHERE minecraft_id = ?";
                    try (PreparedStatement stmt = plugin.getConnection().prepareStatement(sql)) {
                        stmt.setString(1, minecraftId);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            int pledgeAmount = rs.getInt("patreon_amount_cents");
                            if (pledgeAmount > patreonAmountCents) {
                                Player player = Bukkit.getPlayer(UUID.fromString(minecraftId)); // Retrieve the player by their UUID
                                if (pledgeAmount >= 1500) { // $15.00 for Diamond
                                   executeMinecraftCommand("mangadd diamond " + player.getName());
                                } else  if (pledgeAmount >= 1000) { // $10.00 for Gold
                                   executeMinecraftCommand("mangadd gold " + player.getName());
                                } else if (pledgeAmount >= 500) { // $5.00 for Iron
                                  executeMinecraftCommand("mangadd iron " + player.getName());
                                } else {
                                }
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }.runTaskTimer(this, 0L, 1200L); // Run every 60 seconds (1200 ticks)
    }

    private void connectDatabase(Runnable afterConnect) {
        this.getLogger().log(Level.INFO, "Initializing PostgreSQL connection pool...");
    
        new BukkitRunnable() {
            @Override
            public void run() {
                String host = getConfig().getString("postgres_host", "jdbc:postgresql://localhost");
                String db = getConfig().getString("postgres_db", "lucy");
                String user = getConfig().getString("postgres_user", "postgres");
                String password = getConfig().getString("postgres_password", "");
                String port = getConfig().getString("postgres_port", "5432");
    
                String jdbcUrl = String.format("%s:%s/%s", host, port, db);
                getLogger().info("Connecting to: " + jdbcUrl);
    
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setJdbcUrl(jdbcUrl);
                hikariConfig.setUsername(user);
                hikariConfig.setPassword(password);
                hikariConfig.setDriverClassName("org.postgresql.Driver");
    
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

    public Connection getConnection() throws SQLException {
        if (this.dataSource == null) {
            throw new SQLException("DataSource not initialized");
        } else {
            return this.dataSource.getConnection();
        }
    }

    public void closeDatabase() {
        if (this.dataSource != null && !this.dataSource.isClosed()) {
            this.dataSource.close();
        }
    }

    private void createData() {
        this.dataF = new File(this.getDataFolder(), "data.yml");
        if (!this.dataF.exists()) {
            this.dataF.getParentFile().mkdirs();
            this.saveResource("data.yml", false);
        }
        this.data = new YamlConfiguration();
        try {
            this.data.load(this.dataF);
        } catch (InvalidConfigurationException | IOException var2) {
            var2.printStackTrace();
        }
    }

    public void saveData() {
        try {
           this.data.save(this.dataF);
        } catch (IOException var2) {
          var2.printStackTrace();
        }
    }

     private void createConfig() {
        File configf = new File(this.getDataFolder(), "config.yml");
        if (!configf.exists()) {
           configf.getParentFile().mkdirs();
           this.saveResource("config.yml", false);
        }
        this.config = new YamlConfiguration();
        try {
           this.config.load(configf);
        } catch (InvalidConfigurationException | IOException var3) {
           var3.printStackTrace();
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
