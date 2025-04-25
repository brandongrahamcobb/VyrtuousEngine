package com.brandongcobb.vyrtuous;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
//import com.brandongcobb.vyrtuous.Bots.LinkedInBot;
//import com.brandongcobb.vyrtuous.Bots.TwitchBot;
import com.brandongcobb.vyrtuous.cogs.EventListeners;
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
//import com.brandongcobb.vyrtuous.Bots.TwitchBot;
import com.brandongcobb.vyrtuous.utils.sec.DiscordOAuth;
//import com.brandongcobb.vyrtuous.Security.LinkedInOAuth;
import com.brandongcobb.vyrtuous.utils.sec.PatreonOAuth;
//import com.brandongcobb.vyrtuous.Security.TwitchOAuth;
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
//    private Map<String, OAuthUserSession> waitingForResponse = new HashMap<>();
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
    public static EventListeners eventListeners;
    public static int exp;
    public static String factionName;
    public static Helpers helpers;
    public static int level;
    private boolean listeningForCallback = false;
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
    private CompletableFuture<Void> oAuthTask;
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
    public static Timestamp timestamp;
    private Map<String, OAuthUserSession> waitingForResponse;
    public static long userId;
    public static UserManager userManager;
//    public static String accessToken = ""; // Initialized with an empty string
////    public static AIManager aiManager;
//    public static Vyrtuous app; // Static reference for the application instance
//    public static String authUrl = ""; // Initialized with an empty string
//  //  public static Timer callbackTimer = new Timer(); // Instantiated Timer
////    public static ConfigManager configManager = new ConfigManager(app); // Initialize ConfigManager (app will be null here)
//    public static LocalDateTime createDate = LocalDateTime.now(); // Current date
//    public static boolean createdDefaultConfig = false; // Initialized as false
//    public static Player currentPlayer = null; // Initialized to null
//    public static Connection connection = null; // Initialized to null
//    public static CompletableFuture<Void> databaseTask = null; // Initialized to null
//    public static HikariDataSource dbPool = null; // Initialized to null
//  //  public static DiscordBot discordBot = new DiscordBot(app); // Initialize DiscordBot (app will be null here)
// //   public static DiscordOAuth discordOAuth = new DiscordOAuth(app); // Initialize DiscordOAuth (app will be null here)
//    public static long discordId = 0; // Initialized as 0
//    public static CompletableFuture<Void> discordTask = null; // Initialized to null
////    public static DiscordUser discordUser = new DiscordUser(app); // Initialize DiscordUser (app will be null here)
//    public static int exp = 0; // Initialized to 0
//    public static String factionName = ""; // Initialized with an empty string
//    public static int level = 0; // Initialized to 0
//    public static boolean listeningForCallback = false; // Initialized to false
//    public static Lock lock; // Initialized to null
//    public static Logger logger = Logger.getLogger("Vyrtuous"); // Initialize logger
//    public static CompletableFuture<Void> loggingTask = null; // Initialized to null
//    public static CompletableFuture<Void> managerTask = null; // Initialized to null
// //   public static MessageManager messageManager = new MessageManager(app); // Initialize MessageManager (app will be null here)
//    public static String minecraftId = ""; // Initialized with an empty string
//    public static CompletableFuture<Void> minecraftTask = null; // Initialized to null
// //   public static MinecraftUser minecraftUser = new MinecraftUser(app); // Initialize MinecraftUser (app will be null here)
// //   public static OAuthServer oAuthServer = new OAuthServer(app); // Initialize OAuthServer (app will be null here)
//    public static CompletableFuture<Void> oAuthTask = null; // Initialized to null
//    public static String patreonAbout = ""; // Initialized with an empty string
//    public static int patreonAmountCents = 0; // Initialized to 0
//    public static PatreonAPI patreonApi = null; // Initialized to null until set
//    public static String patreonApiKey = ""; // Initialized with an empty string
//    public static String patreonClientId = ""; // Initialized with an empty string
//    public static String patreonClientSecret = ""; // Initialized with an empty string
//    public static String patreonEmail = ""; // Initialized with an empty string
//    public static long patreonId = 0L; // Initialized to 0
//    public static String patreonName = ""; // Initialized with an empty string
// //   public static PatreonOAuth patreonOAuth = new PatreonOAuth(app); // Initialize PatreonOAuth (app will be null here)
//    public static String patreonRedirectUrl = ""; // Initialized with an empty string
//    public static String patreonStatus = ""; // Initialized with an empty string
//    public static CompletableFuture<Void> patreonTask = null; // Initialized to null
//    public static String patreonTier = ""; // Initialized with an empty string
//    public static String patreonVanity = ""; // Initialized with an empty string
////    public static PatreonUser patreonUser = new PatreonUser(app); // Initialize PatreonUser (app will be null here)
////    public static Predicator predicator = new Predicator(app); // Initialize Predicator (app will be null here)
////    public static Timestamp timestamp = new Timestamp(System.currentTimeMillis()); // Initialize with current time
//    public static List<OAuthUserSession> waitingForResponse = new ArrayList<>(); // Directly initialized
//    public static OAuthUserSession session = null; // Initialized to null
//    public static long userId = 0L; // Initialized to 0
////    public static UserManager userManager = new UserManager(app); // Initialize UserManager (app will be null here)
//

    public Vyrtuous () {
        app = this;
        this.logger = Logger.getLogger("Vyrtuous"); // Initialize logger
        this.configManager = new ConfigManager(this); // Instantiate ConfigManager
        if (configManager.exists() && configManager.isConfigSameAsDefault()) {
            if (configManager.isConfigSameAsDefault()) {
                throw new IllegalStateException("Could not load Vyrtuous, the config is invalid.");
            }
        } else if (!configManager.exists()){
            configManager.createDefaultConfig();
        }
        configManager.validateConfig();
        this.conversations = new HashMap<>();
        this.messageManager = new MessageManager(this); // Instantiate MessageManager
        this.moderationManager = new ModerationManager(this); // Initialize to null
        this.predicator = new Predicator(this); // Assuming Predicator takes `Vyrtuous` instance
        this.accessToken = ""; // Initialize with empty string
        this.authUrl = ""; // Initialize with empty string
        this.callbackTimer = new Timer(); // Instantiate the Timer
        this.connection = null; // Initialize to null
        this.createDate = LocalDateTime.now(); // Initialize with the current date
        this.createdDefaultConfig = false; // Initialize with false
        this.currentPlayer = null; // Initialize to null
        this.discordApiKey = configManager.getNestedConfigValue("api_keys", "Discord").getStringValue("api_key"); // Initialize to null
        this.discordOwnerId = configManager.getLongValue("discord_owner_id");
        this.dbPool = null; // Initialize to null until set
        this.exp = 0; // Initialize with zero
        this.factionName = ""; // Initialize with empty string
        this.helpers = helpers;
        this.level = 0; // Initialize with zero
        this.listeningForCallback = false; // Initialize with false
        this.lock = null;
        this.loggingTask = new CompletableFuture<Void>(); // Initialize to null
        this.managerTask = new CompletableFuture<Void>(); // Initialize to null
        this.minecraftId = ""; // Initialize with empty string
        this.minecraftTask = new CompletableFuture<Void>(); // Initialize to null
        this.oAuthTask = new CompletableFuture<Void>();
        //this.oAuthUserSession = new ArrayList<wOAuthUserSession>();
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
        this.patreonAbout = ""; // Initialize with empty string
        this.patreonAmountCents = 0; // Initialize with zero
//        this.patreonApi = new PatreonAPI(); // Initialize to null until set
        this.patreonApiKey = ""; // Initialize with empty string
        this.patreonClientId = ""; // Initialize with empty string
        this.patreonClientSecret = ""; // Initialize with empty string
        this.patreonEmail = ""; // Initialize with empty string
        long patreonId = 0; // Initialize with zero
        this.patreonName = ""; // Initialize with empty string
        this.patreonRedirectUrl = ""; // Initialize with empty string
        this.patreonStatus = ""; // Initialize with empty string
        this.patreonTask = new CompletableFuture<Void>(); // Initialize to null
        this.patreonTier = ""; // Initialize with empty string
        this.patreonVanity = ""; // Initialize with empty string
        this.timestamp = new Timestamp(System.currentTimeMillis()); // Initialize with current time
        this.waitingForResponse = new HashMap<String, OAuthUserSession>(); // Initialize to null
        this.userId = 0L; // Initialize with zero
        try {
            this.aiManager = new AIManager(this);
        } catch (IOException ioe) {}
        this.discordBot = new DiscordBot(this);
        this.discordOAuth = new DiscordOAuth(this); // Assuming DiscordOAuth takes `Vyrtuous` instance
        this.discordUser = new DiscordUser(this); // Assuming DiscordUser takes `Vyrtuous` instance
        this.patreonOAuth = new PatreonOAuth(this); // Instantiate PatreonOAuth
        this.patreonUser = new PatreonUser(this); // Instantiate PatreonUser
        this.oAuthServer = new OAuthServer(this); // Instantiate OAuthServer
        this.userManager = new UserManager(this); // Instantiate UserManager
    }

    private void cancelOAuthSession() {
        listeningForCallback = false; // End the current OAuth flow
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
                String db = getConfig().getString("postgres_db", String.valueOf(configManager.getConfigValue("postgres_database")));
                String user = getConfig().getString("postgres_user", String.valueOf(configManager.getConfigValue("postgres_user")));
                String password = getConfig().getString("postgres_password", String.valueOf(configManager.getConfigValue("postgres_password")));
                String port = getConfig().getString("postgres_port", String.valueOf(configManager.getConfigValue("postgres_port")));
//                String host = getConfig().getString("postgres_host", "jdbc:postgresql://localhost");
//                String db = getConfig().getString("postgres_db", "lucy");
//                String user = getConfig().getString("postgres_user", "postgres");
//                String password = getConfig().getString("postgres_password", "");
//                String port = getConfig().getString("postgres_port", "5432");
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
                            afterConnect.run(); // Execute after connection is confirmed, back on main thread
                        }
                    }.runTask(Vyrtuous.this);
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
                    if (dbPool == null) {
                        getLogger().warning("DataSource not initialized");
                        Bukkit.getScheduler().runTask(Vyrtuous.this, () -> callback.accept(null));
                        return;
                    }
                    conn[0] = dbPool.getConnection(); // Get the connection
                    getLogger().log(Level.INFO, "PostgreSQL connection opened.");
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
        getLogger().log(Level.INFO, "PostgreSQL Example plugin disabled.");
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
                // Cancel existing callback timer
                if (callbackRunnable != null) {
                    callbackRunnable.cancel();
                }
                // Setup session
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

                // Schedule timeout with Bukkit
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
                // Handle exceptions
                getLogger().warning("Error starting OAuth flow: " + e.getMessage());
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
//
//    @Override
//    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//        currentPlayer = (Player) sender;
//        minecraftUser = minecraftUser.getCurrentUser();
//        if (cmd.getName().equalsIgnoreCase("patreon") || cmd.getName().equalsIgnoreCase("discord")) {
//            try {
//                if (callbackTimer != null) {
//                    callbackTimer.cancel(); // Cancel any existing timer
//                    callbackTimer = null; // Allow for garbage collection
//                    listeningForCallback = false; // We aren't waiting now
//                    sender.sendMessage("Previous callback waiting has been interrupted. Starting over.");
//                }
//                listeningForCallback = true;
//                OAuthSession session = new OAuthUserSession(this, minecraftUser, cmd.getName());
//                waitingForResponse.put(String.valueOf(currentPlayer.getUniqueId()), session);
//                String authUrl = "";
//                if (cmd.getName().equalsIgnoreCase("patreon")) {
//                    authUrl = patreonOAuth.getAuthorizationUrl();
//                } else if (cmd.getName().equalsIgnoreCase("discord")) {
//                    authUrl = discordOAuth.getAuthorizationUrl();
//                }
//                sender.sendMessage("Please visit the following URL to authorize: " + authUrl);
//                callbackRunnable = new BukkitRunnable() {
//                    @Override
//                    public void run() {
//                        // Remove session if timed out and notify the player
//                        listeningForCallback = false;
//                        waitingForResponse.remove(currentPlayer.getUniqueId().toString());
//                        currentPlayer.sendMessage("Waiting for callback has timed out.");
//                    }
//                };
//                 callbackRunnable.runTaskLater(this, 12000L);
//                return true;
//            } catch (Exception e) {}
//        if (cmd.getName().equalsIgnoreCase("code")) {
//            if (args.length < 1) {
//                sender.sendMessage("Please provide an access code after /code.");
//                return false;
//            }
//            String providedCode = args[0];
//            OAuthUserSession session = waitingForResponse.get(currentPlayer.getUniqueId().toString());
//            if (session != null) {
//                String expectedToken = session.getAccessToken();
//                if (providedCode.equals(expectedToken)) {
//                    waitingForResponse.remove(currentPlayer.getUniqueId().toString());
//                    // Once authenticated, cancel the callback timeout
//                    if (callbackRunnable != null) {
//                        callbackRunnable.cancel();
//                        callbackRunnable = null;
//                    }
//                    currentPlayer.sendMessage("Authentication successful. Happy mapling!");
//                } else {
//                    currentPlayer.sendMessage("Invalid code, please try again.");
//                }
//            } else {
//                currentPlayer.sendMessage("No pending authentication session found. Please initiate the process first.");
//            }
//            return true;
//        }
//        return false;
//    }
////    @Override
//    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
//        currentPlayer = (Player) sender;
//        minecraftUser = minecraftUser.getCurrentUser();
//        if (cmd.getName().equalsIgnoreCase("patreon") || cmd.getName().equalsIgnoreCase("discord")) {
//            try {
//                if (callbackTimer != null) {
//                    callbackTimer.cancel(); // Cancel any existing timer
//                    callbackTimer = null; // Allow for garbage collection
//                    listeningForCallback = false; // We aren't waiting now
//                    sender.sendMessage("Previous callback waiting has been interrupted. Starting over.");
//                }
//                listeningForCallback = true;
//                OAuthSession session = new OAuthUserSession(this, minecraftUser, cmd.getName());
//                waitingForResponse.put(String.valueOf(currentPlayer.getUniqueId()), session);
//                if (cmd.getName().equalsIgnoreCase("patreon")) {
//                    authUrl = patreonOAuth.getAuthorizationUrl();
//                } else if (cmd.getName().equalsIgnoreCase("discord")) {
//                    authUrl = discordOAuth.getAuthorizationUrl();
//                }
//                sender.sendMessage("Please visit the following URL to authorize: " + authUrl);
//                callbackRunnable = new BukkitRunnable() {
//                    @Override
//                    public void run() {
//                        // Remove session if timed out and notify the player
//                        listeningForCallback = false;
//                        waitingForResponse.remove(currentPlayer.getUniqueId().toString());
//                        currentPlayer.sendMessage("Waiting for callback has timed out.");
//                    }
//                };
//                callbackRunnable.runTaskLater(this, 12000L);
//                return true;
//            } catch (Exception e) {}
//        if (cmd.getName().equalsIgnoreCase("code")) {
//            if (args.length < 1) {
//                sender.sendMessage("Please provide an access code after /code.");
//                return false;
//            }
//            String providedCode = args[0];
//            OAuthUserSession session = waitingForResponse.get(currentPlayer.getUniqueId().toString());
//            if (session != null) {
//                String expectedToken = session.getAccessToken();
//                if (providedCode.equals(expectedToken)) {
//                    waitingForResponse.remove(currentPlayer.getUniqueId().toString());
//                    // Once authenticated, cancel the callback timeout
//                    if (callbackRunnable != null) {
//                        callbackRunnable.cancel();
//                        callbackRunnable = null;
//                    }
//                    currentPlayer.sendMessage("Authentication successful. Happy mapling!");
//                } else {
//                    currentPlayer.sendMessage("Invalid code, please try again.");
//                }
//            } else {
//                currentPlayer.sendMessage("No pending authentication session found. Please initiate the process first.");
//            }
//            return true;
//        }
//        return false;
//    }

    private Logger setupLogging() {
        logger = Logger.getLogger("Vyrtuous");
        return logger;
    }
}

