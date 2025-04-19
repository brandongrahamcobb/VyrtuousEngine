package com.brandongcobb.patreonplugin.utils.sec;

public class TokenResponse {
    private String access_token;
    // Other fields can be added here, such as refresh_token, etc.

    public String getAccessToken() {
        return access_token;
    }

    public void setAccessToken(String access_token) {
        this.access_token = access_token;
    }
}
