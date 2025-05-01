/*  PatreonOAuth.java The purpose of this program is to handle the OAuth
 *  url and the program's access to it.
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
package com.brandongcobb.vyrtuous.utils.sec;

import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager.ConfigSection;
import com.brandongcobb.vyrtuous.utils.handlers.PatreonUser;
import com.brandongcobb.vyrtuous.Vyrtuous;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.patreon.PatreonAPI;
import com.patreon.resources.User;
import java.io.IOException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PatreonOAuth {
    private Vyrtuous app;
    private static String clientId;
    private static String clientSecret;
    private static ConfigSection patreonApiKeys;
    private static String redirectUri;
    private String minecraftId;

    public PatreonOAuth(Vyrtuous application) {
        this.app = application;
    }

    public static CompletableFuture<String> completeGetAuthorizationUrl() {
        return ConfigManager.completeGetNestedConfigValue("api_keys", "Patreon")
            .thenCompose(patreonApiKeys -> {
                return patreonApiKeys.completeGetConfigStringValue("client_id")
                    .thenCombine(patreonApiKeys.completeGetConfigStringValue("redirect_uri"),
                        (clientId, redirectUri) -> {
                            return "https://www.patreon.com/oauth2/authorize" +
                                   "?response_type=code" +
                                   "&client_id=" + clientId +
                                   "&redirect_uri=" + redirectUri +
                                   "&scope=identity%20campaigns";
                        });
            });
    }

    public static CompletableFuture<String> completeExchangeCodeForToken(String code) {
        return ConfigManager.completeGetNestedConfigValue("api_keys", "Patreon")
            .thenCompose(patreonApiKeys -> {
                CompletableFuture<String> clientIdFuture = patreonApiKeys.completeGetConfigStringValue("client_id");
                CompletableFuture<String> clientSecretFuture = patreonApiKeys.completeGetConfigStringValue("client_secret");
                CompletableFuture<String> redirectUriFuture = patreonApiKeys.completeGetConfigStringValue("redirect_uri");
                return CompletableFuture.allOf(clientIdFuture, clientSecretFuture, redirectUriFuture)
                    .thenCompose(v -> {
                        String clientId = clientIdFuture.join();
                        String clientSecret = clientSecretFuture.join();
                        String redirectUri = redirectUriFuture.join();
                        OkHttpClient client = new OkHttpClient();
                        RequestBody body = new FormBody.Builder()
                            .add("code", code)
                            .add("grant_type", "authorization_code")
                            .add("client_id", clientId)
                            .add("client_secret", clientSecret)
                            .add("redirect_uri", redirectUri)
                            .build();
                        Request request = new Request.Builder()
                            .url("https://www.patreon.com/api/oauth2/token")
                            .post(body)
                            .build();
                        return CompletableFuture.supplyAsync(() -> {
                            try (Response response = client.newCall(request).execute()) {
                                if (!response.isSuccessful()) {
                                    System.out.println("Failed to get token: " + response.code() + " - " + response.body());
                                    return null;
                                }
                                String json = response.body().string();
                                JsonElement jsonElement = JsonParser.parseString(json);
                                JsonObject obj = jsonElement.getAsJsonObject();
                                return obj.get("access_token").getAsString();
                            } catch (IOException e) {
                                e.printStackTrace();
                                return null;
                            }
                        });
                    });
            });
    }
}
