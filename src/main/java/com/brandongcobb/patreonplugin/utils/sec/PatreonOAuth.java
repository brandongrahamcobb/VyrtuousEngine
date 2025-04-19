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
import com.brandongcobb.patreonplugin.utils.sec.TokenResponse;
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

public class PatreonOAuth {
    private final JavaPlugin plugin;
    private String accessToken;
    private static String clientId;
    private static String clientSecret;
    private static String redirectUri;
    private String minecraftId;

    public PatreonOAuth(JavaPlugin plugin, String client_id, String clientSecret, String redirectUri) {
        this.plugin = plugin;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

//    public void handleOAuthCallback(String code, String platform, String username) {
//        try {
//            accessToken = exchangeToken(code); // Implemented previously
//            PatreonAPI apiClient = new PatreonAPI(accessToken);
//            JSONAPIDocument<User> userResponse = apiClient.fetchUser();
//            User user = userResponse.get();
//            // Create or update user in the database
////            addUser(user.getFullName(), platform);
//            // Also update specific details as needed, e.g., Patreon ID
//            if (!userExists(minecraftId)) {
//                addUserToDatabase(user, username, minecraftId); // Linking new user from Patreon
//            } else {
//            // If the user already exists, update the patreon ID
//                updateUserPatreonId(user.getId(), minecraftId);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public String getAuthorizationUrl() {
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
                System.out.println("Failed to get token: " + response.code() + " - " + response.body().string());
                return null;
            }
            String json = response.body().string();
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.get("access_token").getAsString();
        }
    }

    private String parseAccessToken(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        String accessToken = "";
        try {
            TokenResponse tokenResponse = objectMapper.readValue(jsonResponse, TokenResponse.class);
            accessToken = tokenResponse.getAccessToken(); // Get the access token from the POJO
        } catch (IOException e) {
            e.printStackTrace(); // Handle exceptions as needed
        }
        return accessToken;
    }
}
