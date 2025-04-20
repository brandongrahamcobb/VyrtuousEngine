package com.brandongcobb.patreonplugin.utils.handlers;

import com.brandongcobb.patreonplugin.PatreonPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import com.patreon.PatreonAPI;
import com.patreon.resources.User;
import org.bukkit.permissions.PermissionAttachment;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.scheduler.BukkitRunnable; 
import java.util.HashMap;
import java.util.Map;

public class UserManager {

//    private final Map<String, String> userTiers; // Mapping of player UUID to their donation tier

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
    
                        // Consolidate users that match
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
        // Update entries and accumulate experience points
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
    
        // Now delete duplicates
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
