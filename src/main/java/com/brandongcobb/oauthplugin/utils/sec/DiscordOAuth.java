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
package com.brandongcobb.oauthplugin.utils.sec;
 
import com.brandongcobb.oauthplugin.utils.handlers.ConfigManager;
import java.util.concurrent.CompletableFuture;

public class DiscordOAuth {


    public CompletableFuture<String> completeGetAuthorizationUrl() {
        ConfigManager cm = ConfigManager.getInstance();
        return cm.completeGetConfigValue("DISCORD_CLIENT_ID", String.class)
                    .thenCombine(cm.completeGetConfigValue("DISCORD_REDIRECT_URI", String.class),
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
        ConfigManager cm = ConfigManager.getInstance();
        CompletableFuture<String> clientIdF = cm.completeGetConfigValue("DISCORD_CLIENT_ID", String.class);
        CompletableFuture<String> clientSecretF = cm.completeGetConfigValue("DISCORD_CLIENT_SECRET", String.class);
        CompletableFuture<String> redirectUriF = cm.completeGetConfigValue("DISCORD_REDIRECT_URI", String.class);
        return CompletableFuture.allOf(clientIdF, clientSecretF, redirectUriF)
            .thenCompose(v -> {
                String clientId = clientIdF.join();
                String clientSecret = clientSecretF.join();
                String redirectUri = redirectUriF.join();
                // Use OkHttp for token exchange, similar to PatreonOAuth
                okhttp3.OkHttpClient httpClient = new okhttp3.OkHttpClient();
                okhttp3.RequestBody form = new okhttp3.FormBody.Builder()
                    .add("client_id", clientId)
                    .add("client_secret", clientSecret)
                    .add("grant_type", "authorization_code")
                    .add("code", code)
                    .add("redirect_uri", redirectUri)
                    .add("scope", "bot")
                    .build();
                okhttp3.Request request = new okhttp3.Request.Builder()
                    .url("https://discord.com/api/oauth2/token")
                    .post(form)
                    .build();
                return CompletableFuture.supplyAsync(() -> {
                    try (okhttp3.Response resp = httpClient.newCall(request).execute()) {
                        if (!resp.isSuccessful()) {
                            String body = resp.body() != null ? resp.body().string() : "";
                            System.err.println("Discord token exchange failed: " + resp.code() + " - " + body);
                            return null;
                        }
                        String json = resp.body() != null ? resp.body().string() : null;
                        if (json == null) return null;
                        com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(json);
                        com.google.gson.JsonObject obj = el.getAsJsonObject();
                        return obj.has("access_token") ? obj.get("access_token").getAsString() : null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                });
            });
    }

}
