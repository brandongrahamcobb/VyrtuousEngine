/*  PatreonUser.java The purpose of this object is to be the session data for a given OAuthUserSession
 *  when accessed via the /patreon command on Discord.app(coming soon), Minecraft or Twitch.tv(coming soon).
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
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement; // For SQL prepared statements
import java.sql.ResultSet; // For SQL result handling
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.List;
import java.util.logging.Level;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PatreonUser implements User {

    private Vyrtuous app;
    private ConfigManager cm;
    private Connection connection;
    private Database db;
    private long discordId;
    private int exp;
    private String factionName;
    private int level;
    private String minecraftId;
    private String patreonAbout;
    private int patreonAmountCents;
    private long patreonClientId;
    private String patreonEmail;
    private long patreonId;
    private String patreonName;
    private String patreonStatus;
    private String patreonTier;
    private String patreonVanity;
    private Timestamp timestamp;
    private UserManager userManager;

    public PatreonUser(ConfigManager cm, Database db) {
        this.cm = cm.completeGetInstance();
        this.db = db;
        this.discordId = 0L;
        this.exp = 0;
        this.factionName = "";
        this.level = 1;
        this.minecraftId = "";
        this.patreonAbout = "";
        this.patreonAmountCents = 0;
        this.patreonEmail = "";
        long patreonId = 0;
        this.patreonName = "";
        this.patreonStatus = "";
        this.patreonTier = "";
        this.patreonVanity = "";
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public CompletableFuture<Void> createUser(Timestamp timestamp, long discordId, int exp, String factionName, int level, String minecraftId, String patreonAbout, int patreonAmountCents, String patreonEmail, long patreonId, String patreonName, String patreonStatus, String patreonTier, String patreonVanity) {
        UserManager um = new UserManager(db);
        return um.createUser(timestamp, discordId, exp, factionName, level, minecraftId, patreonAbout, patreonAmountCents, patreonEmail, patreonId, patreonName, patreonStatus, patreonTier, patreonVanity);
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

    public int getPatreonAmountCents() {
        return patreonAmountCents;
    }

    public long getPatreonId() {
        return patreonId;
    }

    public void userExists(long patreonId, Consumer<Boolean> callback) {
        this.db.completeGetConnection(connection -> {
            try {
                boolean exists = false;
                if (connection != null) {
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE patreon_id = ?")) {
                        stmt.setLong(1, patreonId);
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
