package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.MinecraftUser;

public class OAuthUserSession {
    private Vyrtuous app;
    private String accessToken;
    private User associatedUser;
    private String commandName;
    private MinecraftUser minecraftUser;
    private String minecraftUserId;

    public OAuthUserSession(Vyrtuous application, MinecraftUser minecraftUser, String accessToken) {
        Vyrtuous.oAuthUserSession = this;
        this.app = application;
        this.accessToken = accessToken;
        this.commandName = commandName;
        this.minecraftUser = minecraftUser;
        this.minecraftUserId = minecraftUserId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getMinecraftUserId() {
        return minecraftUserId;
    }

    public String getCommandName() {
        return commandName;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
    }
}
