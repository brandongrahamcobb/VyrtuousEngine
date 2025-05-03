/*  OAuthServer.java The purpose of this program is to serve as OAuth server for Vyrtuous.
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

    private static Vyrtuous app;

    public OAuthServer(Vyrtuous application) {
        this.app = application;
    }

    public static CompletableFuture<OAuthServer> start(Vyrtuous application) {
        OAuthServer server = new OAuthServer(application);
        return ConfigManager.completeGetConfigStringValue("spark_port")
            .thenApply(Integer::parseInt)
            .thenCompose(port -> {
                Spark.port(port);
                return ConfigManager.completeGetConfigObjectValue("spark_discord_endpoint")
                    .thenCombine(
                        ConfigManager.completeGetConfigObjectValue("spark_patreon_endpoint"),
                        (discordEndpoint, patreonEndpoint) -> {
                            Spark.get(discordEndpoint.toString(), (req, res) -> {
                                String code = req.queryParams("code");
                                String stateParam = req.queryParams("state");
                                String userId = URLDecoder.decode(stateParam, "UTF-8");

                                DiscordOAuth.completeExchangeCodeForToken(code).thenAccept(accessToken -> {
                                });

                                return "Discord OAuth callback received. You may now use /code with your token in Minecraft.";
                            });

                            Spark.get(patreonEndpoint.toString(), (req, res) -> {
                                String code = req.queryParams("code");
                                String stateParam = req.queryParams("state");
                                String userId = URLDecoder.decode(stateParam, "UTF-8");

                                PatreonOAuth.completeExchangeCodeForToken(code).thenAccept(accessToken -> {
                                });

                                return "Patreon OAuth callback received. You may now use /code with your token in Minecraft.";
                            });
                            Spark.init();
                            return server;
                        }
                    );
            });
    }

    public static void stop() {
        Spark.stop();
    }

    public static CompletableFuture<Void> cancelOAuthSession(Timer callbackTimer) {
        boolean listeningForCallback = false; // End the current OAuth flow
        if (callbackTimer != null) {
            callbackTimer.cancel(); // Cancel the timer
            callbackTimer = null;
        }
        return CompletableFuture.completedFuture(null);
    }
}
