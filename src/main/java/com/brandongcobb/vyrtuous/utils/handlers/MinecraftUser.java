package com.brandongcobb.vyrtuous.utils.handlers;


import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.UserManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import java.util.HashMap;
import java.util.Map;
public class MinecraftUser implements User {

    private static Vyrtuous app;
    private static Player player;
    private String minecraftId;
    private UserManager userManager;
    public static Map<MinecraftUser, OAuthUserSession> sessions;

    public MinecraftUser(Vyrtuous application, Player currentPlayer) {
        this.app = application;
        this.sessions = app.sessions;
        this.player = currentPlayer;
        this.userManager = app.userManager;
    }

    @Override
    public void createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity, Runnable callback) {
        userManager.createUser(timestamp, discordId, exp, factionName, level, minecraftId, patreonAbout, patreonAmountCents, patreonEmail, patreonId, patreonName, patreonStatus, patreonTier, patreonVanity, () -> {
            callback.run();
        });
    }


    // Override equals() and hashCode() based on UUID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MinecraftUser)) return false;
        MinecraftUser other = (MinecraftUser) o;
        return this.player.getUniqueId().equals(other.player.getUniqueId());
    }

    @Override
    public int hashCode() {
        return this.player.getUniqueId().hashCode();
    }

    public MinecraftUser getCurrentUser() {
        return this;
    }

    public String getCurrentUserId() {
        String uuidString = player.getUniqueId().toString(); // UUID as string
        return uuidString;
    }

    public static void userExists(String minecraftId, Consumer<Boolean> callback) {
        app.getConnection(connection -> {
            try {
                boolean exists = false; // Default to false
                if (connection != null) {
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE minecraft_id = ?")) {
                        stmt.setString(1, minecraftId);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            exists = rs.getInt(1) > 0; // Set true if count is greater than 0
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); // Handle exceptions
                    }
                }
                callback.accept(exists);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    connection.close(); // Close the connection after use
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void link(String accessToken, String userId) {
         for (MinecraftUser minecraftUser : sessions.keySet()) {
             if (minecraftUser.getCurrentUserId().equals(userId)) {
        // found your user
                 sessions.get(minecraftUser).setAccessToken(accessToken);
                 break;
             }
        }
    }
}
