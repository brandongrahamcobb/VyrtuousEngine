/*  PatreonUser.java The purpose of this program is to support the User classes by reducing duplicate entries in the database.
 *  The way this program functions is uniquely broken. It PatreonPlugin creates duplicates in the database and this program
 *  is intended to be run after every new user and is likely going to be changed because its methodology is a bandaid not a solution.
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
package com.brandongcobb.patreonplugin.utils.handlers;

import com.brandongcobb.patreonplugin.PatreonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.scheduler.BukkitRunnable;
import com.patreon.PatreonAPI;
import com.patreon.resources.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UserManager {

    private static PatreonPlugin plugin;
    private static Connection connection;
    private static Connection incomingConnection;

    public UserManager(PatreonPlugin plugin) {
        this.plugin = plugin;
        this.connection = plugin.connection;
    }

    public void consolidateUsers() {
        plugin.getConnection(connection -> {
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
}
