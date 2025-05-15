/*  OAuthServer.java The purpose of this program is to serve as OAuth server for Lucy.
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
package com.brandongcobb.lucy.utils.handlers;

import com.brandongcobb.lucy.utils.sec.DiscordOAuth;
import com.brandongcobb.lucy.utils.sec.PatreonOAuth;
import com.brandongcobb.lucy.Lucy;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import spark.Spark;
import java.util.HashMap;
import java.util.Map;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class OAuthServer {

    private Lucy app;
    private ConfigManager configManager;

    public CompletableFuture<Void> completeConnectSpark() {
        ConfigManager cm = ConfigManager.getInstance();
        return cm.completeGetConfigValue("SPARK_PORT", Integer.class)
             .thenCompose(port -> {
                int portObj = (int) port;
                Spark.port(portObj);
                CompletableFuture<String> discordEndpointFuture = ConfigManager.getInstance().completeGetConfigValue("SPARK_DISCORD_ENDPOINT", String.class);
                CompletableFuture<String> patreonEndpointFuture = ConfigManager.getInstance().completeGetConfigValue("SPARK_PATREON_ENDPOINT", String.class);
                return discordEndpointFuture.thenCombine(patreonEndpointFuture, (discordEndpoint, patreonEndpoint) -> {
                    Spark.get(discordEndpoint, (req, res) -> {
                        String code = req.queryParams("code");
                        String stateParam = req.queryParams("state");
                        String userId = URLDecoder.decode(stateParam, "UTF-8");
                        DiscordOAuth dAuth = new DiscordOAuth();
                        dAuth.completeExchangeCodeForToken(code).thenAccept(accessToken -> {
                        });
                        return "Discord OAuth callback received. You may now use /code with your token in Minecraft.";
                    });
                    Spark.get(patreonEndpoint, (req, res) -> {
                        String code = req.queryParams("code");
                        String stateParam = req.queryParams("state");
                        String userId = URLDecoder.decode(stateParam, "UTF-8");
                        PatreonOAuth pAuth = new PatreonOAuth();
                        pAuth.completeExchangeCodeForToken(code).thenAccept(accessToken -> {
                        });
                        return "Patreon OAuth callback received. You may now use /code with your token in Minecraft.";
                    });
                    Spark.init();
                    return null;
                }).thenApply(ignored -> null);
            });
    }

    public void stop() {
        Spark.stop();
    }

    public CompletableFuture<Void> completeCancelOAuth(Timer callbackTimer) {
        boolean listeningForCallback = false;
        if (callbackTimer != null) {
            callbackTimer.cancel();
            callbackTimer = null;
        }
        return CompletableFuture.completedFuture(null);
    }
}
