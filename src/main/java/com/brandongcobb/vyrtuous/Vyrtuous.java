/*  Vyrtuous.java The primary purpose of this class is to integrate
 *  Discord, LinkedIn, OpenAI, Patreon, Twitch and many more into one
 *  hub.
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
package com.brandongcobb.vyrtuous;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.DiscordUser;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.utils.handlers.MinecraftUser;
import com.brandongcobb.vyrtuous.utils.handlers.ModerationManager;
import com.brandongcobb.vyrtuous.utils.handlers.OAuthServer;
import com.brandongcobb.vyrtuous.utils.handlers.OAuthUserSession;
import com.brandongcobb.vyrtuous.utils.handlers.PatreonUser;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import com.brandongcobb.vyrtuous.utils.handlers.User;
import com.brandongcobb.vyrtuous.utils.handlers.UserManager;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;
import com.brandongcobb.vyrtuous.utils.listeners.PlayerJoinListener;
import com.brandongcobb.vyrtuous.utils.sec.DiscordOAuth;
import com.brandongcobb.vyrtuous.utils.sec.PatreonOAuth;
import com.patreon.PatreonAPI;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;
import java.io.File;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;

public class Vyrtuous extends JavaPlugin {

    public static ConfigManager configManager;
    private BukkitRunnable callbackRunnable;
    public static Map<MinecraftUser, OAuthUserSession> sessions = new HashMap<>();
    public static String accessToken;
    public static AIManager aiManager;
    public static Vyrtuous app;
    public static String authUrl;
    public static Timer callbackTimer;
    public static Map<String, List<Map<String, String>>> conversations;
    public static LocalDateTime createDate;
    public static boolean createdDefaultConfig;
    public static Player currentPlayer;
    public static Connection connection;
    private CompletableFuture<Void> databaseTask;
    public static HikariDataSource dbPool;
    public static String discordApiKey;
    public static Long discordOwnerId;
    public static DiscordBot discordBot;
    public static DiscordOAuth discordOAuth;
    public static long discordId;
    private CompletableFuture<Void> discordTask;
    public static DiscordUser discordUser;
    public static int exp;
    public static String factionName;
    public static Helpers helpers;
    public static int level;
    private boolean listeningForCallback;
    public static Lock lock;
    public static Logger logger;
    private CompletableFuture<Void> loggingTask;
    private CompletableFuture<Void> managerTask;
    public static MessageManager messageManager;
    public static String minecraftId;
    private CompletableFuture<Void> minecraftTask;
    public static MinecraftUser minecraftUser;
    public static ModerationManager moderationManager;
    public static OAuthServer oAuthServer;
    public static OAuthUserSession oAuthUserSession;
    public static boolean openAIDefaultChatCompletion;
    public static boolean openAIDefaultChatCompletionAddToHistory;
    public static String openAIDefaultChatCompletionModel;
    public static long openAIDefaultChatCompletionMaxTokens;
    public static long openAIDefaultChatCompletionNumber;
    public static Map<String, Object> openAIDefaultChatCompletionResponseFormat;
    public static String openAIDefaultChatCompletionStop;
    public static boolean openAIDefaultChatCompletionStore;
    public static boolean openAIDefaultChatCompletionStream;
    public static String openAIDefaultChatCompletionSysInput;
    public static float openAIDefaultChatCompletionTemperature;
    public static float openAIDefaultChatCompletionTopP;
    public static boolean openAIDefaultChatCompletionUseHistory;
    public static boolean openAIDefaultChatModeration;
    public static boolean openAIDefaultChatModerationAddToHistory;
    public static long openAIDefaultChatModerationNumber;
    public static long openAIDefaultChatModerationMaxTokens;
    public static String openAIDefaultChatModerationModel;
    public static Map<String, Object> openAIDefaultChatModerationResponseFormat;
    public static String openAIDefaultChatModerationStop;
    public static boolean openAIDefaultChatModerationStore;
    public static boolean openAIDefaultChatModerationStream;
    public static String openAIDefaultChatModerationSysInput;
    public static float openAIDefaultChatModerationTemperature;
    public static float openAIDefaultChatModerationTopP;
    public static boolean openAIDefaultChatModerationUseHistory;
    public static String openAIGenericApiKey;
    public static String patreonAbout;
    public static int patreonAmountCents;
    public static PatreonAPI patreonApi;
    public static String patreonApiKey;
    public static String patreonClientId;
    public static String patreonClientSecret;
    public static String patreonEmail;
    public static long patreonId;
    public static String patreonName;
    public static PatreonOAuth patreonOAuth;
    public static String patreonRedirectUrl;
    public static String patreonStatus;
    private CompletableFuture<Void> patreonTask;
    public static String patreonTier;
    public static String patreonVanity;
    public static PatreonUser patreonUser;
    public static Predicator predicator;
    public static File tempDirectory;
    public static Timestamp timestamp;
    private Map<String, OAuthUserSession> waitingForResponse;
    public static long userId;
    public static UserManager userManager;
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RESET = "\u001B[0m";

    public Vyrtuous () {
        app = this;
        this.logger = Logger.getLogger("Vyrtuous");
        this.configManager = new ConfigManager(this);
        if (configManager.exists() && configManager.isConfigSameAsDefault()) {
            if (configManager.isConfigSameAsDefault()) {
                throw new IllegalStateException("Could not load Vyrtuous, the config is invalid.");
            }
        } else if (!configManager.exists()){
            configManager.createDefaultConfig();
        }
        configManager.validateConfig();
        this.conversations = new HashMap<>();
        this.tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        this.messageManager = new MessageManager(this);
        this.moderationManager = new ModerationManager(this);
        this.predicator = new Predicator(this);
        this.callbackTimer = new Timer();
        this.createDate = LocalDateTime.now();
        this.createdDefaultConfig = false;
        this.currentPlayer = null;
        this.discordApiKey = configManager.getNestedConfigValue("api_keys", "Discord").getStringValue("api_key");
        this.discordOwnerId = configManager.getLongValue("discord_owner_id");
        this.dbPool = null;
        this.helpers = new Helpers();
        this.lock = null;
        this.loggingTask = new CompletableFuture<Void>(); // Initialize to null
        this.openAIDefaultChatCompletion = false;
        this.openAIDefaultChatCompletionAddToHistory = false;
        this.openAIDefaultChatCompletionMaxTokens = helpers.parseCommaNumber("32,768");
        this.openAIDefaultChatCompletionModel = "gpt-4.1-nano";
        this.openAIDefaultChatCompletionNumber = 1;
        this.openAIDefaultChatCompletionResponseFormat = helpers.OPENAI_CHAT_COMPLETION_RESPONSE_FORMAT;
        this.openAIDefaultChatCompletionStop = "";
        this.openAIDefaultChatCompletionStore = false;
        this.openAIDefaultChatCompletionStream = false;
        this.openAIDefaultChatCompletionSysInput = helpers.OPENAI_CHAT_COMPLETION_SYS_INPUT;;
        this.openAIDefaultChatCompletionTemperature = 0.7f;
        this.openAIDefaultChatCompletionTopP = 1.0f;
        this.openAIDefaultChatCompletionUseHistory = false;
        this.openAIDefaultChatModeration = true;
        this.openAIDefaultChatModerationAddToHistory = false;
        this.openAIDefaultChatModerationMaxTokens = helpers.parseCommaNumber("32,768");
        this.openAIDefaultChatModerationModel = "gpt-4.1-nano";
        this.openAIDefaultChatModerationNumber = 1;
        this.openAIDefaultChatModerationResponseFormat = helpers.OPENAI_CHAT_MODERATION_RESPONSE_FORMAT;
        this.openAIDefaultChatModerationStop = "";
        this.openAIDefaultChatModerationStore = false;
        this.openAIDefaultChatModerationStream = false;
        this.openAIDefaultChatModerationSysInput = "All incoming data is subject to moderation. Protect your backend by flagging a message if it is unsuitable for a public community.";
        this.openAIDefaultChatModerationTemperature = 0.7f;
        this.openAIDefaultChatModerationTopP = 1.0f;
        this.openAIDefaultChatModerationUseHistory = false;
        this.openAIGenericApiKey = configManager.getNestedConfigValue("api_keys", "OpenAI").getStringValue("api_key");
        this.timestamp = new Timestamp(System.currentTimeMillis());
        try {
            this.aiManager = new AIManager(this);
        } catch (IOException ioe) {}
        this.discordBot = new DiscordBot(this);
        this.discordOAuth = new DiscordOAuth(this);
        this.discordUser = new DiscordUser(this);
        this.patreonOAuth = new PatreonOAuth(this);
        this.oAuthServer = new OAuthServer(this);
        this.userManager = new UserManager(this);
    }

    private void cancelOAuthSession() {
        boolean listeningForCallback = false; // End the current OAuth flow
        if (callbackTimer != null) {
            callbackTimer.cancel(); // Cancel the timer
            callbackTimer = null;
        }
    }

    public void closeDatabase() {
        if (dbPool != null && !dbPool.isClosed()) {
            dbPool.close();
        }
    }

    private void connectDatabase(Runnable afterConnect) {
        logger.log(Level.INFO, "Initializing PostgreSQL connection pool...");
        new BukkitRunnable() {
            @Override
            public void run() {
                String host = getConfig().getString("postgres_host", "jdbc:postgresql://" + String.valueOf(configManager.getConfigValue("postgres_host")));
                String db = getConfig().getString("postgres_database", String.valueOf(configManager.getConfigValue("postgres_database")));
                String user = getConfig().getString("postgres_user", String.valueOf(configManager.getConfigValue("postgres_user")));
                String password = getConfig().getString("postgres_password", String.valueOf(configManager.getConfigValue("postgres_password")));
                String port = getConfig().getString("postgres_port", String.valueOf(configManager.getConfigValue("postgres_port")));
                String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
                logger.info("Connecting to: " + jdbcUrl);
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setJdbcUrl(jdbcUrl);
                hikariConfig.setUsername(user);
                hikariConfig.setPassword(password);
                hikariConfig.setDriverClassName("org.postgresql.Driver");
                hikariConfig.setLeakDetectionThreshold(2000);
                try {
                    dbPool = new HikariDataSource(hikariConfig);
                    logger.log(Level.INFO, "PostgreSQL connection pool initialized.");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            afterConnect.run();
                        }
                    }.runTask(Vyrtuous.this);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to initialize PostgreSQL connection pool!", e);
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
                    if (dbPool == null) {
                        logger.warning("DataSource not initialized");
                        Bukkit.getScheduler().runTask(Vyrtuous.this, () -> callback.accept(null));
                        return;
                    }
                    conn[0] = dbPool.getConnection(); // Get the connection
                    logger.log(Level.INFO, "PostgreSQL connection opened.");
                    Bukkit.getScheduler().runTask(Vyrtuous.this, () -> callback.accept(conn[0]));
                } catch (SQLException e) {
                    e.printStackTrace(); // Handle potential SQLException
                    Bukkit.getScheduler().runTask(Vyrtuous.this, () -> callback.accept(null));
                }
            }
        }.runTaskAsynchronously(this); // Run asynchronously
    }

    public void onDisable() {
        closeDatabase();
        logger.log(Level.INFO, "PostgreSQL Example plugin disabled.");
        cancelOAuthSession();
        oAuthServer.stop();
    }

    public void onEnable() {
        try {
            CompletableFuture<Void> loggingTask = CompletableFuture.runAsync(() -> {
                setupLogging();
            });
            this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
            CompletableFuture<Void> databaseTask = CompletableFuture.runAsync(() -> {
                connectDatabase(() -> {});
            });
            CompletableFuture<Void> discordTask = CompletableFuture.runAsync(() -> {
                discordBot.start();
            });
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(databaseTask, discordTask, loggingTask);
            allTasks.join();
        } catch (Exception e) {
            logger.severe("Error initializing the application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false; // Safety check
        Player currentPlayer = (Player)sender;
        MinecraftUser minecraftUser = new MinecraftUser(this, currentPlayer);
        if (cmd.getName().equalsIgnoreCase("patreon") || cmd.getName().equalsIgnoreCase("discord")) {
            try {
                if (callbackRunnable != null) {
                    callbackRunnable.cancel();
                }
                OAuthUserSession session = new OAuthUserSession(this, minecraftUser, cmd.getName());
                sessions.put(minecraftUser, session);
                String authUrl;
                String state = URLEncoder.encode(currentPlayer.getUniqueId().toString(), "UTF-8");;
                if (cmd.getName().equalsIgnoreCase("patreon")) {
                    authUrl = patreonOAuth.getAuthorizationUrl() + "&state=" + state;
                } else {
                    authUrl = discordOAuth.getAuthorizationUrl() + "&state=" + state;
                }
                currentPlayer.sendMessage("Please visit the following URL to authorize: " + authUrl);
                callbackRunnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        sessions.remove(minecraftUser);
                        currentPlayer.sendMessage("Waiting for callback has timed out.");
                    }
                };
                callbackRunnable.runTaskLater(this, 20 * 60 * 10); // 10 minutes = 12000 ticks
                return true;
            } catch (Exception e) {
                logger.warning("Error starting OAuth flow: " + e.getMessage());
            }
        }
        if (cmd.getName().equalsIgnoreCase("code")) {
            if (args.length < 1) {
                sender.sendMessage("Please provide an access code after /code.");
                return false;
            }
            OAuthUserSession session = sessions.get(minecraftUser);
            if (session != null && session.getAccessToken() != null) {
                String providedCode = args[0];
                if (providedCode.equals(session.getAccessToken())) {
                    waitingForResponse.remove(currentPlayer.getUniqueId().toString());
                    // Cancel timeout
                    if (callbackRunnable != null) {
                        callbackRunnable.cancel();
                        callbackRunnable = null;
                    }
                    currentPlayer.sendMessage("Authentication successful. Happy mapling!");
                } else {
                    currentPlayer.sendMessage("Invalid code, please try again.");
                }
            } else {
                sender.sendMessage("No pending authentication or token not yet received.");
            }
            return true;
        }
        return false;
    }

    private Logger setupLogging() {
        logger = Logger.getLogger("Vyrtuous");
        return logger;
    }
}

