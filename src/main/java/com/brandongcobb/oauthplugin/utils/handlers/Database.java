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
package com.brandongcobb.oauthplugin.utils.handlers;

import com.brandongcobb.oauthplugin.OAuthPlugin;
import com.brandongcobb.oauthplugin.utils.handlers.ConfigManager;
import java.sql.DriverManager;            // ← new
import java.sql.PreparedStatement;       // ← new
import java.sql.ResultSet;               // ← new
import java.sql.Statement;               // for database creation and table checks
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
    private final Logger logger = Logger.getLogger("OAuthPlugin");

    private static final String CREATE_USERS_TABLE = 
        "CREATE TABLE IF NOT EXISTS public.users (\n" +
        "    id SERIAL PRIMARY KEY,\n" +
        "    create_date TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,\n" +
        "    discord_id BIGINT UNIQUE,\n" +
        "    minecraft_id VARCHAR(255),\n" +
        "    patreon_about TEXT,\n" +
        "    patreon_amount_cents INTEGER,\n" +
        "    patreon_email VARCHAR(255),\n" +
        "    patreon_id BIGINT,\n" +
        "    patreon_name VARCHAR(255),\n" +
        "    patreon_status VARCHAR(50),\n" +
        "    patreon_tier VARCHAR(255),\n" +
        "    patreon_vanity VARCHAR(255)\n" +
        ");";

    public Database() {
        completeConnectDatabase(() -> {});
        instance = this;
    }

    public static Database completeGetInstance() {
        return instance;
    }

    private CompletableFuture<Void> completeConnectDatabase(Runnable afterConnect) {
        ConfigManager cm = ConfigManager.getInstance();

        // Step 1: fetch all config values in parallel
        return CompletableFuture.allOf(
            cm.completeGetConfigValue("POSTGRES_HOST", String.class),
            cm.completeGetConfigValue("POSTGRES_DATABASE", String.class),
            cm.completeGetConfigValue("POSTGRES_USER", String.class),
            cm.completeGetConfigValue("POSTGRES_PASSWORD", String.class),
            cm.completeGetConfigValue("POSTGRES_PORT", String.class)
        )
        // Step 2: turn futures into a small settings holder
        .thenApplyAsync(v -> {
            try {
                // Retrieve and cast configuration values
                String host     = (String) cm.completeGetConfigValue("POSTGRES_HOST", String.class).get();
                String dbName   = (String) cm.completeGetConfigValue("POSTGRES_DATABASE", String.class).get();
                String user     = (String) cm.completeGetConfigValue("POSTGRES_USER", String.class).get();
                String password = (String) cm.completeGetConfigValue("POSTGRES_PASSWORD", String.class).get();
                String port     = (String) cm.completeGetConfigValue("POSTGRES_PORT", String.class).get();
                return new DbSettings(host, port, dbName, user, password);
            } catch (Exception e) {
                throw new CompletionException("Failed to read DB config", e);
            }
        }, dbExecutor)

        // Step 3: ensure the database itself exists
        .thenComposeAsync(this::ensureDatabaseExists, dbExecutor)

        // Step 4: now build our HikariConfig to that database
        .thenApplyAsync(settings -> {
            String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s",
                                 settings.host, settings.port, settings.dbName);
            logger.info("Connecting to: " + jdbcUrl);
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(settings.user);
            cfg.setPassword(settings.password);
            cfg.setDriverClassName("org.postgresql.Driver");
            cfg.setLeakDetectionThreshold(2_000);
            return cfg;
        }, dbExecutor)

        // Step 5: initialize the pool
        .thenApplyAsync(cfg -> {
            this.dbPool = new HikariDataSource(cfg);
            logger.info("PostgreSQL connection pool initialized.");
            return null;
        }, dbExecutor)

        // Step 6: ensure our users table is there
        .thenCompose(ignored -> ensureUsersTable())

        // Step 7: finally run the caller’s callback
        .thenRun(afterConnect)

        // Error handling
        .exceptionally(ex -> {
            logger.log(Level.SEVERE, "Error during database setup", ex);
            return null;
        });
    }

    private static class DbSettings {
        final String host, port, dbName, user, password;
        DbSettings(String host, String port, String dbName, String user, String password) {
            this.host     = host;
            this.port     = port;
            this.dbName   = dbName;
            this.user     = user;
            this.password = password;
        }
    }
    
    private CompletableFuture<DbSettings> ensureDatabaseExists(DbSettings s) {
        return CompletableFuture.runAsync(() -> {
            try {
                Class.forName("org.postgresql.Driver");
            } catch (ClassNotFoundException e) {
                throw new CompletionException("PostgreSQL JDBC Driver not found", e);
            }
            String maintenanceUrl =
                String.format("jdbc:postgresql://%s:%s/postgres", s.host, s.port);

            try (Connection conn = DriverManager.getConnection(
                     maintenanceUrl, s.user, s.password))
            {
                // 1) Check pg_database
                try (PreparedStatement ps = conn.prepareStatement(
                         "SELECT 1 FROM pg_database WHERE datname = ?"))
                {
                    ps.setString(1, s.dbName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            logger.info("Database '" + s.dbName + "' already exists.");
                            return; // nothing to do
                        }
                    }
                }

                // 2) Create it
                try (Statement stmt = conn.createStatement()) {
                    logger.info("Creating database '" + s.dbName + "' …");
                    stmt.execute("CREATE DATABASE \"" + s.dbName + "\"");
                    logger.info("Database created.");
                }
            } catch (SQLException e) {
                throw new CompletionException("Failed to ensure database exists", e);
            }
        }, dbExecutor)
        // After ensuring the database exists, propagate the settings object
        .thenApply(ignored -> s);
    }
    
    private CompletableFuture<Void> ensureUsersTable() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dbPool.getConnection();
                 Statement stmt = conn.createStatement())
            {
                stmt.execute(CREATE_USERS_TABLE);
                logger.info("Ensured users table exists.");
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to create users table", e);
                // wrap in CompletionException so it bubbles to exceptionally()
                throw new CompletionException(e);
            }
        }, dbExecutor);
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
