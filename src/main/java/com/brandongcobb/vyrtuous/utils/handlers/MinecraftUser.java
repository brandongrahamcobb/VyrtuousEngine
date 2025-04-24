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

public class MinecraftUser implements User {

    private static Vyrtuous app;
    private String minecraftId;
    private UserManager userManager;

    public MinecraftUser(Vyrtuous application) {
        Vyrtuous.minecraftUser = this;
        this.app = application;
        this.userManager = app.userManager;
    }

    @Override
    public void createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity, Runnable callback) {
        userManager.createUser(timestamp, discordId, exp, factionName, level, minecraftId, patreonAbout, patreonAmountCents, patreonEmail, patreonId, patreonName, patreonStatus, patreonTier, patreonVanity, () -> {
            callback.run();
        });
    }


    public MinecraftUser getCurrentUser() {
        return this;
    }

    public String getCurrentUserId(Player player) {
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
}
