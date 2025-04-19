package com.brandongcobb.patreonplugin.utils.handlers;

import com.brandongcobb.patreonplugin.Config;
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

import java.time.Instant;

public class PatreonUser {

    private static String accessToken;
    private Config configMaster;
    private static Connection connection;

    private String createDate;
    private long discordId;
    private int exp;
    private String factionName;
    private int level;
    private static String minecraftId;
    private static PatreonPlugin plugin;
    private PatreonAPI patreonApi;
    private String patreonAbout;
    private int patreonAmountCents;
    private String patreonEmail;
    private long patreonId;
    private String patreonName;
    private PatreonOAuth patreonOAuth;
    private String patreonStatus;
    private String patreonTier;
    private String patreonVanity;
    private UserManager userManager;

    public PatreonUser(String accessToken, Config configMaster, Connection connection, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, PatreonAPI patreonApi, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity, PatreonPlugin plugin, UserManager userManager) {
        this.accessToken = accessToken;
        this.configMaster = configMaster;
        this.connection = connection;
        this.createDate = Instant.now().toString();
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
        this.userManager = userManager;
    }

    public static void createUser(String createDate, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity) {
        String sql = """
            INSERT INTO users (
                create_date, discord_id, exp, faction_name, level, minecraft_id,
                patreon_about, patreon_amount_cents, patreon_email,
                patreon_id, patreon_name, patreon_status, patreon_tier, patreon_vanity
            ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                patreon_vanity = EXCLUDED.patreon_vanity
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, createDate);
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
        } catch (SQLException ioe){}
    }

    private String getPatreonAbout() {
        return patreonAbout;
    }

    private int getPatreonAmountCents() {
        return patreonAmountCents;
    }

    private String getPatreonEmail() {
        return patreonEmail;
    }

    private long getPatreonId() {
        return patreonId;
    }

    private String getPatreonName() {
        return patreonName;
    }

    private String getPatreonStatus() {
        return patreonStatus;
    }

    private String getPatreonTier() {
        return patreonTier;
    }

    private String getPatreonVanity() {
        return patreonVanity; // Assuming you added this field
    }

    private JsonObject getUserPledgeInfo() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://www.patreon.com/api/oauth2/v2/identity?include=memberships.currently_entitled_tiers&fields[user]=full_name,email,vanity,about&fields[member]=patron_status,currently_entitled_amount_cents&fields[tier]=title,amount_cents")
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Failed to get user info: " + response.code() + " - " + response.body().string());
                return null;
            }
            String json = response.body().string();
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (IOException ioe) {}
        return null;
    }

    private void handleOAuthCallback(String code) {
        try {
            PatreonAPI apiClient = new PatreonAPI(accessToken);
            JSONAPIDocument<User> userResponse = apiClient.fetchUser();
            User user = userResponse.get();
            JsonObject userData = getUserPledgeInfo();
            try {
                PatreonUser patreonUser = loadPatreonUser(userData);
                String userAbout = patreonUser.getPatreonAbout();
                int userAmountCents = patreonUser.getPatreonAmountCents();
                String userEmail = patreonUser.getPatreonEmail();
                long userId = patreonUser.getPatreonId();
                String userName = patreonUser.getPatreonName();
                String userStatus = patreonUser.getPatreonStatus();
                String userTier = patreonUser.getPatreonTier();
                String userVanity = patreonUser.getPatreonVanity();
                createUser(createDate, 0L, 0, "", 1, "", userAbout, userAmountCents, userEmail, userId, userName, userStatus, userTier, userVanity);
            } catch (SQLException e) {}
            userManager.consolidateUsers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private PatreonUser loadPatreonUser(JsonObject userData) throws SQLException {
        JsonObject user = userData.getAsJsonObject("data");
        JsonObject attributes = user.getAsJsonObject("attributes");
        long patreonId = Long.parseLong(user.get("id").getAsString());
        String name = attributes.has("full_name") ? attributes.get("full_name").getAsString() : "Unknown";
        String email = attributes.has("email") ? attributes.get("email").getAsString() : null;
        String vanity = attributes.has("vanity") ? attributes.get("vanity").getAsString() : null;
        String about = attributes.has("about") ? attributes.get("about").getAsString() : null;
        String status = null;
        String tier = null;
        int amountCents = 0;
        JsonArray included = userData.getAsJsonArray("included");
        if (included != null) {
            for (JsonElement element : included) {
                JsonObject obj = element.getAsJsonObject();
                if (obj.get("type").getAsString().equals("member")) {
                    status = obj.getAsJsonObject("attributes").get("patron_status").getAsString();
                    amountCents = obj.getAsJsonObject("attributes").get("currently_entitled_amount_cents").getAsInt();
                } else if (obj.get("type").getAsString().equals("tier")) {
                    tier = obj.getAsJsonObject("attributes").get("title").getAsString();
                }
            }
        }
        PatreonUser patreonUser = new PatreonUser(configMaster.getNestedConfigValue("api_keys", "Patreon").getStringValue("api_key"),
                                          configMaster,
                                          connection,
                                          discordId,
                                          exp,
                                          factionName,
                                          level,
                                          minecraftId,
                                          patreonAbout,
                                          patreonAmountCents,
                                          patreonApi,
                                          patreonEmail,
                                          patreonId,
                                          patreonName,
                                          patreonStatus,
                                          patreonTier,
                                          patreonVanity,
                                          plugin,
                                          userManager
        );
        return patreonUser;
    }

    public static boolean userExists(String patreonId) {
         try (Connection connection = plugin.getConnection();
             PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE minecraft_id = ?")) {
             stmt.setString(1, minecraftId);
             ResultSet rs = stmt.executeQuery();
             if (rs.next()) {
                 return rs.getInt(1) > 0; // Returns true if count is greater than 0
             }
         } catch (SQLException e) {
             e.printStackTrace(); // Handle SQL exceptions
         }
         return false; // Default return false if any issue or user does not exist
     }
}
