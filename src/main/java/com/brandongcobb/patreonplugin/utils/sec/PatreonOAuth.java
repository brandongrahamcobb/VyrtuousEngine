package com.brandongcobb.patreonplugin.utils.sec;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.patreon.PatreonAPI;
import com.patreon.resources.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jasminb.jsonapi.JSONAPIDocument;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import com.brandongcobb.patreonplugin.utils.handlers.PatreonUser;

public class PatreonOAuth {
    private final JavaPlugin plugin;
    private String accessToken;
    private static String clientId;
    private static String clientSecret;
    private static String redirectUri;
    private String minecraftId;

    public PatreonOAuth(JavaPlugin plugin, String clientId, String clientSecret, String redirectUri) {
        this.plugin = plugin;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public static String getAuthorizationUrl() {
        return "https://www.patreon.com/oauth2/authorize?response_type=code&client_id=" + clientId +
                "&redirect_uri=" + redirectUri + "&scope=identity%20campaigns";
    }

    public static String exchangeCodeForToken(String code) throws IOException {
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
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Failed to get token: " + response.code() + " - " + response.body());
                return null;
            }
            String json = response.body().string();
            JsonElement jsonElement = JsonParser.parseString(json);
            JsonObject obj = jsonElement.getAsJsonObject();
            return obj.get("access_token").getAsString();
        }
    }
}
