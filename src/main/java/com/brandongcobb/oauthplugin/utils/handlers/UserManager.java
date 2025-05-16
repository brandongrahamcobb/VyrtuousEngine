/*  UserManager.java The purpose of this program is to support the User classes by
 *  reducing duplicate entries in the database. The way this program functions is uniquely broken.
 *  It creates duplicates in the database and this program is intended to be run after every new user
 *  and is likely going to be changed because its methodology is a bandaid not a solution.
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
import com.brandongcobb.oauthplugin.utils.handlers.*;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class UserManager {

    private final Database db;
    private final Map<Long, User> users = new ConcurrentHashMap<>(); // Keyed by discord ID

    public UserManager(Database db) {
        this.db = db;
    }


    public void cacheUser(User user) {
        if (user instanceof DiscordUser) {
            users.put(((DiscordUser) user).getDiscordId(), user);
        } else if (user instanceof PatreonUser) {
            users.put(((PatreonUser) user).getPatreonId(), user);
        } else if (user instanceof MinecraftUser) {
            // Cache by Minecraft UUID hash
            users.put(((MinecraftUser) user).getMinecraftId().hashCode() * 1L, user);
        }
    }

    public User getUserByDiscordId(long discordId) {
        return users.get(discordId);
    }

    public User getUserByPatreonId(long patreonId) {
        return users.get(patreonId);
    }

    public User getUserByMinecraftId(String minecraftId) {
        return users.get(minecraftId.hashCode() * 1L);
    }

    public void consolidateUsers() {
        CompletableFuture.runAsync(() -> {
            db.completeGetConnection(connection -> {
                if (connection != null) {
                    String findDuplicatesSql = """
                        SELECT MIN(id) AS min_id, discord_id, minecraft_id, patreon_id
                        FROM users
                        GROUP BY discord_id, minecraft_id, patreon_id
                        HAVING COUNT(*) > 1
                    """;
                    try (PreparedStatement stmt = connection.prepareStatement(findDuplicatesSql);
                         ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            long minId = rs.getLong("min_id");
                            long discordId = rs.getLong("discord_id");
                            String minecraftId = rs.getString("minecraft_id");
                            long patreonId = rs.getLong("patreon_id");
                            consolidateDuplicateUsers(connection, minId, discordId, minecraftId, patreonId);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            connection.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        });
    }

    private void consolidateDuplicateUsers(Connection conn, long mainId, long discordId, String minecraftId, long patreonId) {
        CompletableFuture.runAsync(() -> {
            String consolidateSql = """
                UPDATE users SET
                    discord_id = COALESCE(discord_id, ?),
                    minecraft_id = COALESCE(minecraft_id, ?),
                    patreon_id = COALESCE(patreon_id, ?)
                WHERE (discord_id = ? OR minecraft_id = ? OR patreon_id = ?) AND id != ?
            """;
            try (PreparedStatement stmt = conn.prepareStatement(consolidateSql)) {
                stmt.setLong(1, discordId);
                stmt.setString(2, minecraftId);
                stmt.setLong(3, patreonId);
                stmt.setLong(4, discordId);
                stmt.setString(5, minecraftId);
                stmt.setLong(6, patreonId);
                stmt.setLong(7, mainId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            String deleteSql = "DELETE FROM users WHERE (discord_id = ? OR minecraft_id = ? OR patreon_id = ?) AND id != ?";
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                stmt.setLong(1, discordId);
                stmt.setString(2, minecraftId);
                stmt.setLong(3, patreonId);
                stmt.setLong(4, mainId);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> createUser(
        Timestamp timestamp,
        long discordId,
        String minecraftId,
        String patreonAbout,
        int patreonAmountCents,
        String patreonEmail,
        long patreonId,
        String patreonName,
        String patreonStatus,
        String patreonTier,
        String patreonVanity
    ) {
        String sql = """
            INSERT INTO users (
                create_date, discord_id, minecraft_id,
                patreon_about, patreon_amount_cents, patreon_email,
                patreon_id, patreon_name, patreon_status, patreon_tier, patreon_vanity
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (discord_id) DO UPDATE SET
                create_date = EXCLUDED.create_date,
                minecraft_id = EXCLUDED.minecraft_id,
                patreon_about = EXCLUDED.patreon_about,
                patreon_amount_cents = EXCLUDED.patreon_amount_cents,
                patreon_email = EXCLUDED.patreon_email,
                patreon_id = EXCLUDED.patreon_id,
                patreon_name = EXCLUDED.patreon_name,
                patreon_status = EXCLUDED.patreon_status,
                patreon_tier = EXCLUDED.patreon_tier,
                patreon_vanity = EXCLUDED.patreon_vanity
        """;

        return CompletableFuture.runAsync(() -> {
            db.completeGetConnection(connection -> {
                if (connection != null) {
                    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                        stmt.setTimestamp(1, timestamp);
                        stmt.setLong(2, discordId);
                        stmt.setString(3, minecraftId);
                        stmt.setString(4, patreonAbout);
                        stmt.setInt(5, patreonAmountCents);
                        stmt.setString(6, patreonEmail);
                        stmt.setLong(7, patreonId);
                        stmt.setString(8, patreonName);
                        stmt.setString(9, patreonStatus);
                        stmt.setString(10, patreonTier);
                        stmt.setString(11, patreonVanity);
                        stmt.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            });

            // Register in memory
            UserImpl user = new UserImpl(timestamp, discordId, minecraftId, patreonAbout, patreonAmountCents,
                                         patreonEmail, patreonId, patreonName, patreonStatus, patreonTier, patreonVanity);
            users.put(discordId, user);
        });
    }

    public boolean isUserLoaded(long discordId) {
        return users.containsKey(discordId);
    }

    public void unloadUser(long discordId) {
        users.remove(discordId);
    }

    public Map<Long, User> getAllUsers() {
        return users;
    }
}
