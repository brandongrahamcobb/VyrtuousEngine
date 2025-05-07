/*  OAuthServer.java The purpose of this program is to serve as OAuth server for Vyrtuous.
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
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.utils.sec.DiscordOAuth;
import com.brandongcobb.vyrtuous.utils.sec.PatreonOAuth;
import com.brandongcobb.vyrtuous.Vyrtuous;
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

    private Vyrtuous app;
    private ConfigManager cm;

    public OAuthServer(ConfigManager cm) {
        this.cm = cm.completeGetInstance();
    }

    public CompletableFuture<Void> completeConnectSpark() {
        return cm.completeGetConfigValue("spark_port", Integer.class)
             .thenCompose(portObj -> {
                int port = (int) portObj;
                Spark.port(port);
                CompletableFuture<String> discordEndpointFuture = cm.completeGetConfigValue("spark_discord_endpoint", String.class);
                CompletableFuture<String> patreonEndpointFuture = cm.completeGetConfigValue("spark_patreon_endpoint", String.class);
                return discordEndpointFuture.thenCombine(patreonEndpointFuture, (discordEndpoint, patreonEndpoint) -> {
                    Spark.get(discordEndpoint, (req, res) -> {
                        String code = req.queryParams("code");
                        String stateParam = req.queryParams("state");
                        String userId = URLDecoder.decode(stateParam, "UTF-8");
                        DiscordOAuth dAuth = new DiscordOAuth(this.cm);
                        dAuth.completeExchangeCodeForToken(code).thenAccept(accessToken -> {
                        });
                        return "Discord OAuth callback received. You may now use /code with your token in Minecraft.";
                    });
                    Spark.get(patreonEndpoint, (req, res) -> {
                        String code = req.queryParams("code");
                        String stateParam = req.queryParams("state");
                        String userId = URLDecoder.decode(stateParam, "UTF-8");
                        PatreonOAuth pAuth = new PatreonOAuth(this.cm);
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
