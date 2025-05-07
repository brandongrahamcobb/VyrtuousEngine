package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Database {

    private static ConfigManager cm;
    private static final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);
    private static HikariDataSource dbPool;
    private static final Logger logger = Logger.getLogger("Vyrtuous");

    public Database(ConfigManager cm) {
        this.cm = cm;
    }

    public CompletableFuture<Void> completeConnectDatabase(Runnable afterConnect) {
        return CompletableFuture.allOf(
                cm.completeGetConfigValue("postgres_host", String.class),
                cm.completeGetConfigValue("postgres_database", String.class),
                cm.completeGetConfigValue("postgres_user", String.class),
                cm.completeGetConfigValue("postgres_password", String.class),
                cm.completeGetConfigValue("postgres_port", String.class)
        ).thenApplyAsync(v -> {
            try {
                String host = String.valueOf(cm.completeGetConfigValue("postgres_host", String.class).get());
                String db = String.valueOf(cm.completeGetConfigValue("postgres_database", String.class).get());
                String user = String.valueOf(cm.completeGetConfigValue("postgres_user", String.class).get());
                String password = String.valueOf(cm.completeGetConfigValue("postgres_password", String.class).get());
                String port = String.valueOf(cm.completeGetConfigValue("postgres_port", String.class).get());
                String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", host, port, db);
                logger.info("Connecting to: " + jdbcUrl);
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setJdbcUrl(jdbcUrl);
                hikariConfig.setUsername(user);
                hikariConfig.setPassword(password);
                hikariConfig.setDriverClassName("org.postgresql.Driver");
                hikariConfig.setLeakDetectionThreshold(2000);
                return hikariConfig;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error fetching configuration values", e);
                throw new RuntimeException(e);
            }
        }, dbExecutor).thenApplyAsync(config -> {
            try {
                this.dbPool = new HikariDataSource(config);
                logger.log(Level.INFO, "PostgreSQL connection pool initialized.");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to initialize PostgreSQL connection pool!", e);
                throw new RuntimeException(e);
            }
            return null;
        }, dbExecutor).thenRun(afterConnect)
        .exceptionally(ex -> {
            logger.log(Level.SEVERE, "Error connecting to the database asynchronously", ex);
            return null;
        });
    }

    public CompletableFuture<Void> completeCloseDatabase() {
        return CompletableFuture.runAsync(() -> {
            if (this.dbPool != null && !this.dbPool.isClosed()) {
                this.dbPool.close();
            }
            dbExecutor.shutdown();
        });
    }

    public CompletableFuture<Void> completeGetConnection(Consumer<Connection> callback) {
        return CompletableFuture.supplyAsync(() -> {
            if (this.dbPool == null) {
                logger.warning("DataSource not initialized");
                return null;
            }
            try {
                Connection conn = this.dbPool.getConnection();
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
}
