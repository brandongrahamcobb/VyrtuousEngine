/*  DiscordUser.java The purpose of this program be a DiscordUser
 *  object class which holds information about Discord users.
 *  Scope: To preserve and be capable of restoring user data.
 *  Current state: bugged 25 04 2025.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.oauthplugin.utils.handlers;

import com.brandongcobb.oauthplugin.OAuthPlugin;
import com.brandongcobb.oauthplugin.utils.handlers.*;
import com.brandongcobb.oauthplugin.utils.sec.PatreonOAuth;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.function.Consumer;
import java.util.List;
import java.util.logging.Level;
import java.util.Map;
import java.util.UUID;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;
import java.util.concurrent.CompletableFuture;

public class DiscordUser {

    private String accessToken;
    private long discordId;
    private OAuthPlugin app;
    private Connection[] conn;
    private Connection connection;
    private LocalDateTime createDate = LocalDateTime.now();
    private static Database db;
    private String minecraftId;
    // removed unused PatreonAPI field
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
    private final String USER_INFO_URL = "https://discord.com/api/v10/users/@me";

    public DiscordUser(String accessToken) {
        this.accessToken = accessToken;
        this.db = Database.completeGetInstance();
        // exchange access token for Discord ID
        this.discordId = getDiscordId(accessToken);
        this.minecraftId = "";
        this.patreonAbout = "";
        this.patreonAmountCents = 0;
        // this.patreonApi removed
        this.patreonEmail = "";
        this.patreonId = 0L;
        this.patreonName = "";
        this.patreonStatus = "";
        this.patreonTier = "";
        this.patreonVanity = "";
        this.timestamp = timestamp;
    }

    public CompletableFuture<Void> createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity) {
        UserManager um = new UserManager(db);
        return um.createUser(timestamp, discordId, minecraftId, patreonAbout, patreonAmountCents, patreonEmail, patreonId, patreonName, patreonStatus, patreonTier, patreonVanity);
    }

    public long getDiscordId(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(USER_INFO_URL, HttpMethod.GET, requestEntity, Map.class);
        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> userData = response.getBody();
            if (userData != null && userData.containsKey("id")) {
                Object idVal = userData.get("id");
                if (idVal instanceof Number) {
                    return ((Number) idVal).longValue();
                } else if (idVal instanceof String) {
                    try {
                        return Long.parseLong((String) idVal);
                    } catch (NumberFormatException nfe) {
                        System.err.println("Invalid Discord ID format: " + idVal);
                        return 0L;
                    }
                }
            }
        }
        System.err.println("Failed to retrieve user information: " + response.getBody());
        return 0L;
    }
    /**
     * Return the stored Discord user ID.
     */
    public long getDiscordId() {
        return this.discordId;
    }

    public void userExists(long discordId, Consumer<Boolean> callback) {
        db.completeGetConnection(connection -> {
            try {
                boolean exists = false;
                if (connection != null) {
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE discord_id = ?")) {
                        stmt.setLong(1, discordId);
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            exists = rs.getInt(1) > 0;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                callback.accept(exists);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
