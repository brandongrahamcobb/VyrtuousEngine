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
import com.brandongcobb.vyrtuous.metadata.MetadataContainer;
import com.brandongcobb.vyrtuous.utils.handlers.Database;
import com.brandongcobb.vyrtuous.utils.handlers.PlayerMessageQueueManager;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.MinecraftUser;
import com.brandongcobb.vyrtuous.utils.handlers.OAuthServer;
import com.brandongcobb.vyrtuous.utils.handlers.OAuthUserSession;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletionException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

public class Vyrtuous {

    public static final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);
    public JDA api;
    public static Vyrtuous app;
    public static Connection connection;
    public static HikariDataSource dbPool;
    public static String discordApiKey;
    public static long discordOwnerId;
    public static Lock lock;
    public static Logger logger;
    public MetadataContainer metadataContainer;
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
        this.logger = Logger.getLogger("Vyrtuous");
        this.metadataContainer = new MetadataContainer();
        this.tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        this.lock = null;
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
            .thenCompose(ignored -> new Database().completeConnectDatabase(this, () -> {})) // database aits for config
            .thenCompose(ignored -> completeSetupLogging()) // wait for logging setup
            .thenCompose(ignored -> completeInitializeDiscordConfiguration())
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

    public CompletableFuture<Void> completeInitializeDiscordConfiguration() {
        return completeGetInstance().thenCompose(appInstance ->
            ConfigManager.completeGetNestedConfigValue("api_keys", "Discord")
                .thenCompose(discordApiKeys ->
                    discordApiKeys.completeGetConfigStringValue("api_key")
                )
                .thenCombine(
                    ConfigManager.completeGetConfigLongValue("discord_owner_id"),
                    (apiKey, ownerId) -> {
                        this.discordApiKey = apiKey;
                        this.discordOwnerId = ownerId;
                        this.api = JDABuilder.createDefault(discordApiKey,
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT,
                                GatewayIntent.GUILD_MEMBERS)
                            .setActivity(Activity.playing("Starting up..."))
                            .build();
                        return null;
                    }
                )
        );
    }

    public static CompletableFuture<ExecutorService> completeGetDatabaseExecutor() {
       return CompletableFuture.supplyAsync(() -> app.dbExecutor);
    }
    
    public CompletableFuture<JDA> completeGetApi() {
        return CompletableFuture.supplyAsync(() -> api);
    }

    public static CompletableFuture<Void> completeSetDatabasePool(HikariDataSource dbPool) {
        return CompletableFuture.runAsync(() -> {
            app.dbPool = dbPool;
        });
    }

    public static CompletableFuture<Vyrtuous> completeGetInstance() {
       return CompletableFuture.supplyAsync(() -> app);
    }

    public static CompletableFuture<MetadataContainer> completeGetMetadataContainer() {
       return CompletableFuture.supplyAsync(() -> {
           return app.metadataContainer;
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

    public static CompletableFuture<Void> completeSetAppContainer(MetadataContainer metadataContainer) {
        return CompletableFuture.runAsync(() -> {
            app.metadataContainer = metadataContainer;
        });
    }

    public static CompletableFuture<Void> onDisable() {
        return OAuthServer.cancelOAuthSession(callbackTimer)
              .thenRun(() -> OAuthServer.stop())
              .thenRun(() -> logger.log(Level.INFO, "PostgreSQL Example plugin disabled."));
    }

    public static void main(String[] args) {
        Vyrtuous app = new Vyrtuous();
        try {
            DiscordBot.start(app.api).join();
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
