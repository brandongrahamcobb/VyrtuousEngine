/*  PatreonUser.java The purpose of this program is to support the vyrtuous class by handling the PatreonUser which is distinct from DiscordUser or MinecraftUser.
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

import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.UserManager;
import com.brandongcobb.vyrtuous.utils.listeners.PlayerJoinListener;
import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.sec.PatreonOAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.patreon.PatreonAPI;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement; // For SQL prepared statements
import java.sql.ResultSet; // For SQL result handling
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.List;
import java.util.logging.Level; // For logging
import java.util.Map; // Don't forget to import Map
import java.util.UUID; // For handling player UUIDs
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.bukkit.Bukkit; // For Bukkit API
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable; // For creating scheduled tasks
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;

public class DiscordUser implements User {

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
    private final String USER_INFO_URL = "https://discord.com/api/v10/users/@me";

    public DiscordUser(Vyrtuous application) {
        Vyrtuous.discordUser = this;
        this.app = application;
        this.accessToken = accessToken;
        this.configManager = app.configManager;
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
        this.timestamp = timestamp;
        this.userManager = app.userManager;
    }

    @Override
    public void createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity, Runnable callback) {
        userManager.createUser(timestamp, discordId, exp, factionName, level, minecraftId, patreonAbout, patreonAmountCents, patreonEmail, patreonId, patreonName, patreonStatus, patreonTier, patreonVanity, () -> {
            callback.run();
        });
    }

    public long getDiscordId(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(USER_INFO_URL, HttpMethod.GET, requestEntity, Map.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> userData = response.getBody();
            if (userData != null) {
                // Extract the user ID from the response
                return (long) userData.get("id");
            }
        }
        System.err.println("Failed to retrieve user information: " + response.getBody());
        return 0L; // or throw an exception depending on your error handling strategy
    }

    public void userExists(long discordId, Consumer<Boolean> callback) {
        app.getConnection(connection -> {
            try {
                boolean exists = false; // Default to false
                if (connection != null) {
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE discord_id = ?")) {
                        stmt.setLong(1, discordId);
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
