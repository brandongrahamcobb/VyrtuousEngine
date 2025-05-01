/*  DiscordOAuth.java The purpose of this program is to handle the OAuth
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
import com.brandongcobb.vyrtuous.Vyrtuous;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets; // For StandardCharsets
import java.lang.StringBuilder;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class DiscordOAuth {

    private static Vyrtuous app;
    private static ConfigSection discordApiKeys; // = ConfigManager.new ConfigSection();
    private static String clientId;
    private static String clientSecret;
    private static String discordId;
    private static String redirectUri;

    public DiscordOAuth(Vyrtuous application) {
        this.app = application;
    }

    public static CompletableFuture<String> completeGetAuthorizationUrl() {
        return ConfigManager.completeGetNestedConfigValue("api_keys", "Discord")
            .thenCompose(discordApiKeys -> {
                return discordApiKeys.completeGetConfigStringValue("client_id")
                    .thenCombine(discordApiKeys.completeGetConfigStringValue("redirect_uri"),
                        (clientId, redirectUri) -> {
                            return "https://discord.com/api/oauth2/authorize" +
                                "?client_id=" + clientId +
                                "&redirect_uri=" + redirectUri +
                                "&response_type=code" +
                                "&permissions=2141842833124" +
                                "&integration_type=0" +
                                "&scope=identify";
                        });
            });
    }

    public static CompletableFuture<String> completeExchangeCodeForToken(String code) {
        return ConfigManager.completeGetNestedConfigValue("api_keys", "Discord")
            .thenCompose(discordApiKeys -> {
                CompletableFuture<String> clientIdFuture = discordApiKeys.completeGetConfigStringValue("client_id");
                CompletableFuture<String> clientSecretFuture = discordApiKeys.completeGetConfigStringValue("client_secret");
                CompletableFuture<String> redirectUriFuture = discordApiKeys.completeGetConfigStringValue("redirect_uri");
                return CompletableFuture.allOf(clientIdFuture, clientSecretFuture, redirectUriFuture)
                    .thenCompose(v -> {
                        String clientId = clientIdFuture.join();
                        String clientSecret = clientSecretFuture.join();
                        String redirectUri = redirectUriFuture.join();
                        return CompletableFuture.supplyAsync(() -> {
                            String tokenUrl = "https://discord.com/api/oauth2/token";
                            Map<String, String> data = new HashMap<>();
                            data.put("client_id", clientId);
                            data.put("client_secret", clientSecret);
                            data.put("code", code);
                            data.put("grant_type", "authorization_code");
                            data.put("redirect_uri", redirectUri);
                            data.put("scope", "bot");
                            try {
                                URL url = new URL(tokenUrl);
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("POST");
                                connection.setDoOutput(true);
                                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                                StringBuilder requestBody = new StringBuilder();
                                for (Map.Entry<String, String> entry : data.entrySet()) {
                                    if (requestBody.length() != 0) {
                                        requestBody.append('&');
                                    }
                                    requestBody.append(entry.getKey()).append('=').append(entry.getValue());
                                }
                                try (OutputStream os = connection.getOutputStream()) {
                                    os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                                }
                                int responseCode = connection.getResponseCode();
                                if (responseCode == HttpURLConnection.HTTP_OK) {
                                    StringBuilder response = new StringBuilder();
                                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                                        String inputLine;
                                        while ((inputLine = in.readLine()) != null) {
                                            response.append(inputLine);
                                        }
                                    }
                                    String jsonResponse = response.toString();
                                    Map<String, Object> tokenData = parseJsonResponse(jsonResponse);
                                    if (tokenData != null && tokenData.containsKey("access_token")) {
                                        return (String) tokenData.get("access_token");
                                    } else {
                                        System.err.println("Discord token exchange failed: " + tokenData);
                                        return null;
                                    }
                                } else {
                                    System.err.println("Error: " + responseCode);
                                    return null;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        });
                    });
            });
    }

    private static Map<String, Object> parseJsonResponse(String jsonResponse) {
        Map<String, Object> tokenData = new HashMap<>();
        Gson gson = new Gson();
        tokenData = gson.fromJson(jsonResponse, Map.class);
        return tokenData;
    }
}
