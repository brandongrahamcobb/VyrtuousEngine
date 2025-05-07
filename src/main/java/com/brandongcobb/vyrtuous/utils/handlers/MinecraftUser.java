package com.brandongcobb.vyrtuous.utils.handlers;


import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.Database;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.Map;
public class MinecraftUser implements User {

    private static Vyrtuous app;
    private static Database db;
    private String minecraftId;
    public static Map<MinecraftUser, OAuthUserSession> sessions;

    public MinecraftUser(Database db) {
        this.db = db;
        this.sessions = app.sessions;
    }

    @Override
    public CompletableFuture<Void> createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity) {
        return UserManager.createUser(timestamp, discordId, exp, factionName, level, minecraftId, patreonAbout, patreonAmountCents, patreonEmail, patreonId, patreonName, patreonStatus, patreonTier, patreonVanity);
    }

    public MinecraftUser getCurrentUser() {
        return this;
    }

    public static void userExists(String minecraftId, Consumer<Boolean> callback) {
        db.completeGetConnection(connection -> {
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
