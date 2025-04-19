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

import java.util.HashMap;
import java.util.Map;

public class UserManager {

//    private final Map<String, String> userTiers; // Mapping of player UUID to their donation tier
    private PatreonPlugin plugin;

    public UserManager(PatreonPlugin plugin) {
        this.plugin = plugin;
    }

    public void consolidateUsers() {
        try (Connection connection = plugin.getConnection()) {
            // Find users with the same Discord ID, Patreon ID or Minecraft ID
            String findDuplicatesSql = "SELECT MIN(id) AS min_id, " +
                                        "discord_id, minecraft_id, patreon_id " +
                                        "FROM users " +
                                        "GROUP BY discord_id, minecraft_id, patreon_id " +
                                        "HAVING COUNT(*) > 1";
            try (PreparedStatement findDuplicatesStmt = connection.prepareStatement(findDuplicatesSql);
                 ResultSet rs = findDuplicatesStmt.executeQuery()) {
                while (rs.next()) {
                    long minId = rs.getLong("min_id"); // Keep the smallest `id` as the main entry
                    Long discordId = rs.getLong("discord_id");
                    String minecraftId = rs.getString("minecraft_id");
                    Long patreonId = rs.getLong("patreon_id");

                    // Consolidate users that match
                    consolidateDuplicateUsers(connection, minId, discordId, minecraftId, patreonId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Handle SQL exceptions
        }
    }

    private void consolidateDuplicateUsers(Connection connection, long mainId, long discordId, String minecraftId, long patreonId) {
        try {
            // Aggregate necessary data
            String consolidateSql = "UPDATE users SET " +
                                    "discord_id = CASE WHEN discord_id IS NULL THEN ? ELSE discord_id END, " +
                                    "minecraft_id = CASE WHEN minecraft_id IS NULL THEN ? ELSE minecraft_id END, " +
                                    "patreon_id = CASE WHEN patreon_id IS NULL THEN ? ELSE patreon_id END " +
                                    "WHERE discord_id = ? OR minecraft_id = ? OR patreon_id = ? " +
                                    "AND id != ?"; // Make sure we're not updating the main entry
            try (PreparedStatement consolidateStmt = connection.prepareStatement(consolidateSql)) {
                consolidateStmt.setLong(1, discordId);
                consolidateStmt.setString(2, minecraftId);
                consolidateStmt.setLong(3, patreonId);
                consolidateStmt.setLong(4, discordId);
                consolidateStmt.setString(5, minecraftId);
                consolidateStmt.setLong(6, patreonId);
                consolidateStmt.setLong(7, mainId); // Exclude the main entry from this update
                consolidateStmt.executeUpdate();
            }
            // Now delete duplicates
            String deleteDuplicatesSql = "DELETE FROM users WHERE discord_id = ? OR minecraft_id = ? OR patreon_id = ? AND id != ?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteDuplicatesSql)) {
                deleteStmt.setLong(1, discordId);
                deleteStmt.setString(2, minecraftId);
                deleteStmt.setLong(3, patreonId);
                deleteStmt.setLong(4, mainId); // Make sure again to exclude the main entry
                deleteStmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace(); // Handle SQL exceptions
        }
    }
}
