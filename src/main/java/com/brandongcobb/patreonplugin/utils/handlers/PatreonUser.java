package com.brandongcobb.patreonplugin.utils.handlers;

import java.util.function.Consumer;
import com.brandongcobb.patreonplugin.Config;
import java.sql.Timestamp;
import com.brandongcobb.patreonplugin.utils.listeners.PlayerJoinListener;
import com.brandongcobb.patreonplugin.PatreonPlugin;
import com.brandongcobb.patreonplugin.utils.sec.PatreonOAuth;
import com.brandongcobb.patreonplugin.utils.handlers.UserManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.patreon.PatreonAPI;
import com.patreon.resources.User;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.patreon.PatreonAPI;
import com.patreon.resources.User;
import com.patreon.resources.Campaign;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import org.bukkit.Bukkit; // For Bukkit API
import org.bukkit.entity.Player; // For Player entity
import org.bukkit.plugin.java.JavaPlugin; // For main plugin class
import org.bukkit.configuration.file.FileConfiguration; // For configuration handling
import java.sql.PreparedStatement; // For SQL prepared statements
import java.sql.ResultSet; // For SQL result handling
import java.sql.SQLException; // For SQL exceptions
import java.util.UUID; // For handling player UUIDs
import org.bukkit.scheduler.BukkitRunnable; // For creating scheduled tasks
import java.util.logging.Level; // For logging
import java.sql.Connection; // For database connections
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PatreonUser {

    private static String accessToken;
    private static Config configMaster;
    private static Connection[] conn;
    private static Connection connection;
    private static LocalDateTime createDate = LocalDateTime.now();
    private static long discordId;
    private static int exp;
    private static String factionName;
    private static int level;
    private static String minecraftId;
    private static PatreonPlugin plugin;
    private static PatreonAPI patreonApi;
    private static String patreonAbout;
    private static int patreonAmountCents;
    private static String patreonEmail;
    private static long patreonId;
    private static String patreonName;
    private PatreonOAuth patreonOAuth;
    private static String patreonStatus;
    private static String patreonTier;
    private static String patreonVanity;
    private static Timestamp timestamp = Timestamp.valueOf(createDate);
   private static UserManager userManager;

    public PatreonUser(String accessToken, Config configMaster, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, PatreonAPI patreonApi, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity, PatreonPlugin plugin, UserManager userManager) {
        this.accessToken = accessToken;
        this.configMaster = configMaster;
        this.discordId = discordId;
        this.exp = exp;
        this.factionName = factionName;
        this.level = level;
        this.minecraftId = minecraftId;
        this.patreonAbout = patreonAbout;
        this.patreonAmountCents = patreonAmountCents;
        this.patreonApi = patreonApi;
        this.patreonEmail = patreonEmail;
        this.patreonId = patreonId;
        this.patreonName = patreonName;
        this.patreonStatus = patreonStatus;
        this.patreonTier = patreonTier;
        this.patreonVanity = patreonVanity;
        this.plugin = plugin;
        this.timestamp = timestamp;
        this.userManager = userManager;
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
        plugin.getConnection(connection -> {
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
                                Bukkit.getScheduler().runTask(plugin, callback);
                            }
                        }.runTaskAsynchronously(plugin); // Run asynchronously
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

    private static final String API_URL = "https://www.patreon.com/api/oauth2/v2/identity" +
            "?include=memberships&fields[member]=currently_entitled_amount_cents";

    public static long getCurrentUserId(String accessToken) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Failed to fetch Patreon user info: " + response.code());
                return 0L;
            }

            String body = response.body().string();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");

            JsonObject result = new JsonObject();
            patreonId = Long.parseLong(data.get("id").getAsString());
            return patreonId;
        } catch (IOException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public static int getCurrentPatreonAmountCents(String accessToken) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Failed to fetch Patreon user info: " + response.code());
                return 0;
            }

            String body = response.body().string();
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            JsonObject data = root.getAsJsonObject("data");
            JsonObject result = new JsonObject();
            JsonArray included = root.getAsJsonArray("included");
            if (included != null) {
                for (JsonElement el : included) {
                    JsonObject obj = el.getAsJsonObject();
                    if (obj.get("type").getAsString().equals("member")) {
                        JsonObject attr = obj.getAsJsonObject("attributes");
                        patreonAmountCents = attr.get("currently_entitled_amount_cents").getAsInt();
                        break;
                    }
                }
            }
            return Integer.parseInt(String.valueOf(patreonAmountCents));
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String getPatreonAbout() {
        return patreonAbout;
    }

    public int getPatreonAmountCents() {
        return patreonAmountCents;
    }

    public String getPatreonEmail() {
        return patreonEmail;
    }

    public long getPatreonId() {
        return patreonId;
    }

    public String getPatreonName() {
        return patreonName;
    }

    public String getPatreonStatus() {
        return patreonStatus;
    }

    public String getPatreonTier() {
        return patreonTier;
    }

    public String getPatreonVanity() {
        return patreonVanity; // Assuming you added this field
    }

    public static void userExists(String minecraftId, Consumer<Boolean> callback) {
        plugin.getConnection(connection -> {
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
            // Call the callback with the result
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
