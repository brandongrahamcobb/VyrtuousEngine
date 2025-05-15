/*  Database.java The purpose of this class is to integrate
 *  host a PostgresSQL database connenction for the main program..
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.lucy.utils.handlers;

import com.brandongcobb.lucy.Lucy;
import com.brandongcobb.lucy.utils.handlers.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Database {

    private ConfigManager cm;
    private static Database instance;
    private final ExecutorService dbExecutor = Executors.newFixedThreadPool(4);
    private HikariDataSource dbPool;
    private final Logger logger = Logger.getLogger("Lucy");

    public Database() {
        completeConnectDatabase(() -> {});
        instance = this;
    }

    public static Database completeGetInstance() {
        return instance;
    }

    private CompletableFuture<Void> completeConnectDatabase(Runnable afterConnect) {
        ConfigManager cm = ConfigManager.getInstance();
        return CompletableFuture.allOf(
                cm.completeGetConfigValue("POSTGRES_HOST", String.class),
                cm.completeGetConfigValue("POSTGRES_DATABASE", String.class),
                cm.completeGetConfigValue("POSTGRES_USER", String.class),
                cm.completeGetConfigValue("POSTGRES_PASSWORD", String.class),
                cm.completeGetConfigValue("POSTGRES_PORT", String.class)
        ).thenApplyAsync(v -> {
            try {
                String host = String.valueOf(cm.completeGetConfigValue("POSTGRES_HOST", String.class).get());
                String db = String.valueOf(cm.completeGetConfigValue("POSTGRES_DATABASE", String.class).get());
                String user = String.valueOf(cm.completeGetConfigValue("POSTGRES_USER", String.class).get());
                String password = String.valueOf(cm.completeGetConfigValue("POSTGRES_PASSWORD", String.class).get());
                String port = String.valueOf(cm.completeGetConfigValue("POSTGRES_PORT", String.class).get());
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
