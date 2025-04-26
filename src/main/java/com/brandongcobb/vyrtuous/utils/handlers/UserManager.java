/*  UserManager.java The purpose of this program is to support the User classes by
 *  reducing duplicate entries in the database. The way this program functions is uniquely broken.
 *  It creates duplicates in the database and this program is intended to be run after every new user
 *  and is likely going to be changed because its methodology is a bandaid not a solution.
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
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

public class UserManager {

    private static Vyrtuous app;
    private static Connection incomingConnection;

    public UserManager(Vyrtuous application) {
        Vyrtuous.userManager = this;
        this.app = application;
        this.incomingConnection = app.connection;
    }

    public void consolidateUsers() {
        app.getConnection(connection -> {
            if (connection != null) {
                String findDuplicatesSql = "SELECT MIN(id) AS min_id, " +
                    "discord_id, minecraft_id, patreon_id, SUM(exp) AS total_exp " + // Get total experience
                    "FROM users " +
                    "GROUP BY discord_id, minecraft_id, patreon_id " +
                    "HAVING COUNT(*) > 1";
                try (PreparedStatement findDuplicatesStmt = connection.prepareStatement(findDuplicatesSql)) {
                    ResultSet rs = findDuplicatesStmt.executeQuery();
                    while (rs.next()) {
                        long minId = rs.getLong("min_id"); // Keep the smallest `id` as the main entry
                        Long discordId = rs.getLong("discord_id");
                        String minecraftId = rs.getString("minecraft_id");
                        Long patreonId = rs.getLong("patreon_id");
                        long totalExp = rs.getLong("total_exp"); // Get the total experience
                        consolidateDuplicateUsers(connection, minId, discordId, minecraftId, patreonId, totalExp);
                    }
                } catch (SQLException e) {
                    e.printStackTrace(); // Handle potential SQLException from closing
                } finally {
                    try {
                        connection.close(); // Close the connection after use
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void consolidateDuplicateUsers(Connection incomingConnection, long mainId, long discordId, String minecraftId, long patreonId, long totalExp) {
        String consolidateSql = "UPDATE users SET " +
            "discord_id = CASE WHEN discord_id IS NULL THEN ? ELSE discord_id END, " +
            "minecraft_id = CASE WHEN minecraft_id IS NULL THEN ? ELSE minecraft_id END, " +
            "patreon_id = CASE WHEN patreon_id IS NULL THEN ? ELSE patreon_id END, " +
            "exp = exp + ? " + // Add the experience to the main entry
            "WHERE (discord_id = ? OR minecraft_id = ? OR patreon_id = ?) " +
            "AND id != ?"; // Make sure we're not updating the main entry
        try (PreparedStatement consolidateStmt = incomingConnection.prepareStatement(consolidateSql)) {
            consolidateStmt.setLong(1, discordId);
            consolidateStmt.setString(2, minecraftId);
            consolidateStmt.setLong(3, patreonId);
            consolidateStmt.setLong(4, totalExp); // Add total experience to main entry
            consolidateStmt.setLong(5, discordId);
            consolidateStmt.setString(6, minecraftId);
            consolidateStmt.setLong(7, patreonId);
            consolidateStmt.setLong(8, mainId); // Exclude the main entry from this update
            consolidateStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace(); // Handle SQL exceptions
        }
        String deleteDuplicatesSql = "DELETE FROM users WHERE (discord_id = ? OR minecraft_id = ? OR patreon_id = ?) AND id != ?";
        try (PreparedStatement deleteStmt = incomingConnection.prepareStatement(deleteDuplicatesSql)) {
            deleteStmt.setLong(1, discordId);
            deleteStmt.setString(2, minecraftId);
            deleteStmt.setLong(3, patreonId);
            deleteStmt.setLong(4, mainId); // Make sure again to exclude the main entry
            deleteStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace(); // Handle SQL exceptions
        }
    }

    public static void createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level,
                                   String minecraftId, String patreonAbout, int patreonAmountCents,
                                   String patreonEmail, long patreonId, String patreonName,
                                   String patreonStatus, String patreonTier, String patreonVanity,
                                   Runnable callback) {
        String sql = """
            INSERT INTO users (
                create_date, discord_id, exp, faction_name, level, minecraft_id,
                patreon_about, patreon_amount_cents, patreon_email,
                patreon_id, patreon_name, patreon_status, patreon_tier, patreon_vanity
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                create_date = EXCLUDED.create_date,
                discord_id = EXCLUDED.discord_id,
                exp = EXCLUDED.exp,
                faction_name = EXCLUDED.faction_name,
                level = EXCLUDED.level,
                minecraft_id = EXCLUDED.minecraft_id,
                patreon_about = EXCLUDED.patreon_about,
                patreon_amount_cents = EXCLUDED.patreon_amount_cents,
                patreon_email = EXCLUDED.patreon_email,
                patreon_id = EXCLUDED.patreon_id,
                patreon_name = EXCLUDED.patreon_name,
                patreon_status = EXCLUDED.patreon_status,
                patreon_tier = EXCLUDED.patreon_tier,
                patreon_vanity = EXCLUDED.patreon_vanity;
        """;
        app.getConnection(connection -> {
            if (connection != null) {
                try {
                    PreparedStatement stmt = connection.prepareStatement(sql);
                    stmt.setTimestamp(1, timestamp);
                    stmt.setLong(2, discordId);
                    stmt.setInt(3, exp);
                    stmt.setString(4, factionName);
                    stmt.setInt(5, level);
                    stmt.setString(6, minecraftId);
                    stmt.setString(7, patreonAbout);
                    stmt.setInt(8, patreonAmountCents);
                    stmt.setString(9, patreonEmail);
                    stmt.setLong(10, patreonId);
                    stmt.setString(11, patreonName);
                    stmt.setString(12, patreonStatus);
                    stmt.setString(13, patreonTier);
                    stmt.setString(14, patreonVanity);
                    stmt.executeUpdate();
                    try {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                // Run the callback on the main thread after the database operation
                                Bukkit.getScheduler().runTask(app, callback);
                            }
                        }.runTaskAsynchronously(app); // Run asynchronously
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        connection.close(); // Close the connection after use
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
}
