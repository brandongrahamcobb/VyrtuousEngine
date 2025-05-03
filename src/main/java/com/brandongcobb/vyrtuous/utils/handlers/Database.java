package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Database {

    private static Vyrtuous app;
    public static HikariDataSource dbPool;

    public CompletableFuture<Void> completeConnectDatabase(Vyrtuous application, Runnable afterConnect) {
        return application.completeGetInstance().thenCompose(app ->
            app.completeGetDatabaseExecutor().thenCompose(dbExecutor ->
                CompletableFuture.allOf(
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
                    app.logger.info("Connecting to: " + jdbcUrl);
                    HikariConfig hikariConfig = new HikariConfig();
                    hikariConfig.setJdbcUrl(jdbcUrl);
                    hikariConfig.setUsername(user);
                    hikariConfig.setPassword(password);
                    hikariConfig.setDriverClassName("org.postgresql.Driver");
                    hikariConfig.setLeakDetectionThreshold(2000);
                    return hikariConfig;
                }, dbExecutor)
                .thenApply(config -> {
                    try {
                        app.dbPool = new HikariDataSource(config);
                        app.logger.log(Level.INFO, "PostgreSQL connection pool initialized.");
                    } catch (Exception e) {
                        app.logger.log(Level.SEVERE, "Failed to initialize PostgreSQL connection pool!", e);
                        throw new RuntimeException(e);
                    }
                    return app;
                })
            ).thenRun(afterConnect)
             .exceptionally(ex -> {
                 app.logger.log(Level.SEVERE, "Error connecting to the database asynchronously", ex);
                 return null;
             })
        );
    }

    public static CompletableFuture<Void> completeCloseDatabase(Vyrtuous application) {
        return application.completeGetInstance().thenCompose(app -> {
            if (app.dbPool != null && !app.dbPool.isClosed()) {
                app.dbPool.close();
            }
    
            return app.completeGetDatabaseExecutor().thenAccept(dbExecutor -> {
                dbExecutor.shutdown();
            });
        });
    }
}
