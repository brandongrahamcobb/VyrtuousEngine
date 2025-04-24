/*  PatreonUser.java The purpose of this program is to support the PatreonPlugin class by handling the PatreonUser which is distinct from DiscordUser or MinecraftUser.
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
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.User;
import com.brandongcobb.vyrtuous.utils.handlers.UserManager;
import com.brandongcobb.vyrtuous.utils.listeners.PlayerJoinListener;
import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.sec.PatreonOAuth;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.patreon.PatreonAPI;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement; // For SQL prepared statements
import java.sql.ResultSet; // For SQL result handling
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.logging.Level;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.Bukkit; // For Bukkit API
import org.bukkit.scheduler.BukkitRunnable; // For creating scheduled tasks
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.List;
import java.util.logging.Level; // For logging
import java.util.UUID; // For handling player UUIDs

public class PatreonUser implements User {

    private String accessToken;
    private Vyrtuous app;
    private ConfigManager configManager;
    private Connection[] conn;
    private Connection connection;
    private LocalDateTime createDate = LocalDateTime.now();
    private long discordId;
    private int exp;
    private String factionName;
    private int level;
    private String minecraftId;
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
    private Timestamp timestamp = Timestamp.valueOf(createDate);
    private UserManager userManager;

    public PatreonUser(Vyrtuous application) {
        Vyrtuous.patreonUser = this;
        this.app = application;
        this.configManager = app.configManager;
        this.discordId = app.discordId;
        this.exp = app.exp;
        this.factionName = app.factionName;
        this.level = app.level;
        this.minecraftId = app.minecraftId;
        this.patreonAbout = app.patreonAbout;
        this.patreonAmountCents = app.patreonAmountCents;
        this.patreonApi = app.patreonApi;
        this.patreonEmail = app.patreonEmail;
        this.patreonId = app.patreonId;
        this.patreonName = app.patreonName;
        this.patreonStatus = app.patreonStatus;
        this.patreonTier = app.patreonTier;
        this.patreonVanity = app.patreonVanity;
        this.timestamp = app.timestamp;
        this.userManager = app.userManager;
    }

    @Override
    public void createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity, Runnable callback) {
        userManager.createUser(timestamp, discordId, exp, factionName, level, minecraftId, patreonAbout, patreonAmountCents, patreonEmail, patreonId, patreonName, patreonStatus, patreonTier, patreonVanity, () -> {
            callback.run();
        });
    }

    private final String API_URL = "https://www.patreon.com/api/oauth2/v2/identity" + "?include=memberships&fields[member]=currently_entitled_amount_cents";

    public long getCurrentUserId(String accessToken) {
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

    public int getCurrentPatreonAmountCents(String accessToken) {
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

    public void userExists(long patreonId, Consumer<Boolean> callback) {
        app.getConnection(connection -> {
            try {
                boolean exists = false; // Default to false
                if (connection != null) {
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE patreon_id = ?")) {
                        stmt.setLong(1, patreonId);
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
