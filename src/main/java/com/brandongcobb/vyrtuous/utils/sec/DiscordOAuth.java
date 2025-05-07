/*  DiscordOAuth.java The purpose of this program is to handle the OAuth
 *  url and the program's access to it.
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
package com.brandongcobb.vyrtuous.utils.sec;

import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.Vyrtuous;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets; // For StandardCharsets
import java.lang.StringBuilder;
import java.util.concurrent.CompletableFuture;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

public class DiscordOAuth {

    private Vyrtuous app;
    private ConfigManager cm;
    private String clientId;
    private String clientSecret;
    private String discordId;
    private String redirectUri;

    public DiscordOAuth(ConfigManager cm) {
        this.cm = cm.completeGetInstance();
    }

    public CompletableFuture<String> completeGetAuthorizationUrl() {
        return cm.completeGetConfigValue("discord_client_id", String.class)
                    .thenCombine(cm.completeGetConfigValue("discord_redirect_uri", String.class),
                        (clientId, redirectUri) -> {
                            return "https://discord.com/api/oauth2/authorize" +
                                "?client_id=" + clientId +
                                "&redirect_uri=" + redirectUri +
                                "&response_type=code" +
                                "&permissions=2141842833124" +
                                "&integration_type=0" +
                                "&scope=identify";
                        }
            );
    }

    public CompletableFuture<String> completeExchangeCodeForToken(String code) {
        CompletableFuture<String> clientIdFuture = cm.completeGetConfigValue("discord_client_id", String.class);
        CompletableFuture<String> clientSecretFuture = cm.completeGetConfigValue("discord_client_secret", String.class);
        CompletableFuture<String> redirectUriFuture = cm.completeGetConfigValue("discord_redirect_uri", String.class);
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
                            requestBody.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                                       .append('=')
                                       .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
                        }
                        try (OutputStream os = connection.getOutputStream()) {
                            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
                        }
                        int responseCode = connection.getResponseCode();
                        InputStream inputStream = (responseCode == HttpURLConnection.HTTP_OK)
                                ? connection.getInputStream()
                                : connection.getErrorStream();
                        StringBuilder response = new StringBuilder();
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
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
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
            });
    }

    private Map<String, Object> parseJsonResponse(String jsonResponse) {
        Map<String, Object> tokenData = new HashMap<>();
        Gson gson = new Gson();
        tokenData = gson.fromJson(jsonResponse, Map.class);
        return tokenData;
    }
}
