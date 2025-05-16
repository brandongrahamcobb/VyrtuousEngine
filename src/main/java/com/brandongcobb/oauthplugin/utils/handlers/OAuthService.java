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
package com.brandongcobb.oauthplugin.utils.handlers;

import com.brandongcobb.oauthplugin.OAuthPlugin;
import com.brandongcobb.oauthplugin.utils.handlers.*;
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

public class OAuthService {

    private static final String API_URL = "https://www.patreon.com/api/oauth2/v2/identity?include=memberships&fields%5Buser%5D=about,email,vanity,name,social_connections&fields%5Bmember%5D=currently_entitled_amount_cents,last_charge_status,patron_status,full_name";

    public int getPatreonAmountCents(String accessToken) {
        try (Response response = makeRequest(accessToken)) {
            if (!response.isSuccessful()) {
                System.out.println("Failed to fetch Patreon user info: " + response.code());
                return 0;
            }
            JsonArray included = getIncludedArray(response);
            for (JsonElement el : included) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.get("type").getAsString().equals("member")) {
                    JsonObject attr = obj.getAsJsonObject("attributes");
                    return attr.get("currently_entitled_amount_cents").getAsInt();
                }
            }
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String getPatreonAbout(String accessToken) {
        try (Response response = makeRequest(accessToken)) {
            if (!response.isSuccessful()) return "";
            JsonObject data = getDataObject(response);
            return data.getAsJsonObject("attributes").get("about").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getPatreonEmail(String accessToken) {
        try (Response response = makeRequest(accessToken)) {
            if (!response.isSuccessful()) return "";
            JsonObject data = getDataObject(response);
            return data.getAsJsonObject("attributes").get("email").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public long getPatreonId(String accessToken) {
        try (Response response = makeRequest(accessToken)) {
            if (!response.isSuccessful()) return 0;
            JsonObject data = getDataObject(response);
            return Long.parseLong(data.get("id").getAsString());
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String getPatreonName(String accessToken) {
        try (Response response = makeRequest(accessToken)) {
            if (!response.isSuccessful()) return "";
            JsonObject data = getDataObject(response);
            return data.getAsJsonObject("attributes").get("full_name").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getPatreonStatus(String accessToken) {
        try (Response response = makeRequest(accessToken)) {
            if (!response.isSuccessful()) return "";
            JsonArray included = getIncludedArray(response);
            for (JsonElement el : included) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.get("type").getAsString().equals("member")) {
                    return obj.getAsJsonObject("attributes").get("patron_status").getAsString();
                }
            }
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getPatreonTier(String accessToken) {
        try (Response response = makeRequest(accessToken)) {
            if (!response.isSuccessful()) return "";
            JsonArray included = getIncludedArray(response);
            for (JsonElement el : included) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.get("type").getAsString().equals("member")) {
                    JsonObject relationships = obj.getAsJsonObject("relationships");
                    if (relationships.has("currently_entitled_tiers")) {
                        JsonObject tier = relationships.getAsJsonObject("currently_entitled_tiers");
                        JsonArray dataArray = tier.getAsJsonArray("data");
                        if (dataArray.size() > 0) {
                            JsonObject tierInfo = dataArray.get(0).getAsJsonObject();
                            return tierInfo.get("id").getAsString();
                        }
                    }
                }
            }
            return "";
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getPatreonVanity(String accessToken) {
        try (Response response = makeRequest(accessToken)) {
            if (!response.isSuccessful()) return "";
            JsonObject data = getDataObject(response);
            return data.getAsJsonObject("attributes").get("vanity").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private Response makeRequest(String accessToken) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + accessToken)
                .build();
        return client.newCall(request).execute();
    }

    private JsonObject getDataObject(Response response) throws IOException {
        String body = response.body().string();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        return root.getAsJsonObject("data");
    }

    private JsonArray getIncludedArray(Response response) throws IOException {
        String body = response.body().string();
        JsonObject root = JsonParser.parseString(body).getAsJsonObject();
        return root.getAsJsonArray("included");
    }
}
