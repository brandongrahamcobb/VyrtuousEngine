/*  DiscordUser.java The purpose of this program be a DiscordUser
 *  object class which holds information about Discord users.
 *  Scope: To preserve and be capable of restoring user data.
 *  Current state: bugged 25 04 2025.
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
import com.brandongcobb.vyrtuous.utils.handlers.Database;
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

public class DiscordUser implements User {

    private String accessToken;
    private Vyrtuous app;
    private Connection[] conn;
    private Connection connection;
    private LocalDateTime createDate = LocalDateTime.now();
    private static Database db;
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
    private final String USER_INFO_URL = "https://discord.com/api/v10/users/@me";

    public DiscordUser(Database db, long discordId) {
        this.accessToken = accessToken;
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
    }

    @Override
    public CompletableFuture<Void> createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity) {
        return UserManager.createUser(timestamp, discordId, exp, factionName, level, minecraftId, patreonAbout, patreonAmountCents, patreonEmail, patreonId, patreonName, patreonStatus, patreonTier, patreonVanity);
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
                return (long) userData.get("id");
            }
        }
        System.err.println("Failed to retrieve user information: " + response.getBody());
        return 0L;
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
