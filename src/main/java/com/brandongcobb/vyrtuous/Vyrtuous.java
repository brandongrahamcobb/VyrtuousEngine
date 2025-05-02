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
import com.brandongcobb.vyrtuous.utils.handlers.PlayerMessageQueueManager;
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
import com.brandongcobb.vyrtuous.metadata.MetadataContainer;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletionException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.net.URI;
import java.net.URISyntaxException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Vyrtuous {

    private static final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);
    public static Vyrtuous app;
    public static Connection connection;
    private CompletableFuture<Void> databaseTask;
    public static HikariDataSource dbPool;
    public static String discordApiKey;
    public static Long discordOwnerId;
    public static DiscordBot discordBot;
    private CompletableFuture<Void> discordTask;
    private static Vyrtuous instance;
    private boolean listeningForCallback;
    public static Lock lock;
    public static Logger logger;
    private CompletableFuture<Void> loggingTask;
    private CompletableFuture<Void> managerTask;
    public MetadataContainer metadataContainer;
    private CompletableFuture<Void> minecraftTask;
    public static OAuthUserSession oAuthUserSession;
    public static Map<MinecraftUser, OAuthUserSession> sessions = new HashMap<>();
    public static File tempDirectory;
    public static Timer callbackTimer;
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RESET = "\u001B[0m";

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

    public Vyrtuous() {
        app = this;
        instance = this;
        this.logger = Logger.getLogger("Vyrtuous");
        this.metadataContainer = new MetadataContainer();
        this.tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        this.lock = null;
        this.loggingTask = new CompletableFuture<>();
        CompletableFuture<Void> initializationFuture = ConfigManager.completeSetApp(this)
            .thenCompose(ignored -> ConfigManager.completeLoadConfig())
            .thenCompose(ignored -> ConfigManager.completeIsConfigSameAsDefault())
            .thenCompose(isDefault -> {
                if (isDefault) {
                    return CompletableFuture.failedFuture(
                            new IllegalStateException("Could not load Vyrtuous, the config is invalid."));
                } else {
                    return ConfigManager.completeValidateConfig();
                }
            })
            .thenCompose(ignored -> completeConnectDatabase(() -> {})) // database aits for config
            .thenCompose(ignored -> completeSetupLogging()) // wait for logging setup
            .thenRun(() -> {
                OAuthServer.start(this);
                new PlayerMessageQueueManager();
            })
            .exceptionally(ex -> {
                logger.severe("Error initializing the application: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
        initializationFuture.join();
    }

    public static CompletableFuture<Void> closeDatabase() {
        return CompletableFuture.supplyAsync(() -> {
            if (dbPool != null && !dbPool.isClosed()) {
                dbPool.close();
            }
            return null;
        });
    }

    public static CompletableFuture<Vyrtuous> getInstance() {
       return CompletableFuture.supplyAsync(() -> instance);
    }

    public CompletableFuture<Void> completeConnectDatabase(Runnable afterConnect) {
        logger.log(Level.INFO, "Initializing PostgreSQL connection pool asynchronously...");
        return CompletableFuture.allOf(
            ConfigManager.completeGetConfigObjectValue("postgres_host"),
            ConfigManager.completeGetConfigObjectValue("postgres_database"),
            ConfigManager.completeGetConfigObjectValue("postgres_user"),
            ConfigManager.completeGetConfigObjectValue("postgres_password"),
            ConfigManager.completeGetConfigObjectValue("postgres_port")
        ).thenApplyAsync(v -> {
            String host = String.valueOf(ConfigManager.completeGetConfigObjectValue("postgres_host").join());
            String db = String.valueOf(ConfigManager.completeGetConfigObjectValue("postgres_database").join());
            String user = String.valueOf(ConfigManager.completeGetConfigObjectValue("postgres_user").join());
            String password = String.valueOf(ConfigManager.completeGetConfigObjectValue("postgres_password").join());
            String port = String.valueOf(ConfigManager.completeGetConfigObjectValue("postgres_port").join());
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
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize PostgreSQL connection pool!", e);
                throw new RuntimeException(e);
            }
            return null;
        }, dbExecutor)
        .thenRun(afterConnect)
        .exceptionally(ex -> {
            logger.log(Level.SEVERE, "Error connecting to the database asynchronously", ex);
            return null;
        });
    }

    public CompletableFuture<Void> completeGetConnection(Consumer<Connection> callback) {
        return CompletableFuture.supplyAsync(() -> {
            if (dbPool == null) {
                logger.warning("DataSource not initialized");
                return null;
            }
            try {
                Connection conn = dbPool.getConnection();
                logger.log(Level.INFO, "PostgreSQL connection opened.");
                return conn;
            } catch (SQLException e) {
                throw new CompletionException(e);
            }
        }, dbExecutor).thenAccept(conn -> {
            if (conn != null) {
                callback.accept(conn);
            } else {
                logger.warning("No connection returned from DB pool");
            }
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Error obtaining connection asynchronously", ex);
            return null;
        });
    }

    public static CompletableFuture<Void> onDisable() {
        // Chain the operations in sequence, each producing Void.
        return closeDatabase()
              .thenRun(() -> OAuthServer.cancelOAuthSession(callbackTimer))
              .thenRun(() -> OAuthServer.stop())
              .thenRun(() -> dbExecutor.shutdown())
              .thenRun(() -> logger.log(Level.INFO, "PostgreSQL Example plugin disabled."));
    }

    public static void main(String[] args) {
        Vyrtuous app = new Vyrtuous();
        try {
            DiscordBot.start().join();
        } catch (Exception e) {
            e.printStackTrace();
            onDisable();
        }
    }

    private CompletableFuture<Logger> completeSetupLogging() {
        return CompletableFuture.supplyAsync(() -> {
            logger = Logger.getLogger("Vyrtuous");
            return logger;
        });
    }
}
