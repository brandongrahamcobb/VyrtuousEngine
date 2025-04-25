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

import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.sec.DiscordOAuth;
import com.brandongcobb.vyrtuous.utils.sec.PatreonOAuth;
import com.brandongcobb.vyrtuous.Vyrtuous;
import java.security.SecureRandom;
import java.util.Base64;
import spark.Spark;
import java.util.HashMap;
import java.util.Map;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class OAuthServer {

    private Vyrtuous app;
    private ConfigManager configManager;
    private Map<MinecraftUser, OAuthUserSession> sessions;

    public OAuthServer(Vyrtuous application) {
        Vyrtuous.oAuthServer = this;
        this.app = application;
        configManager = app.configManager;
        this.sessions = app.sessions;
        Spark.port(Integer.parseInt(configManager.getStringValue("spark_port")));
        Spark.get(String.valueOf(configManager.getConfigValue("spark_discord_endpoint")), (req, res) -> {
            String code = req.queryParams("code");
            String stateParam = req.queryParams("state");
            String userId = URLDecoder.decode(stateParam, "UTF-8");
            String accessToken = DiscordOAuth.exchangeCodeForToken(code);
            MinecraftUser.link(accessToken, userId);
            return "Discord OAuth callback processed. Type /code with your code in Minecraft :\n " + accessToken;
        });
        Spark.get(String.valueOf(configManager.getConfigValue("spark_patreon_endpoint")), (req, res) -> {
            String code = req.queryParams("code");
            String stateParam = req.queryParams("state");
            String userId = URLDecoder.decode(stateParam, "UTF-8");
            String accessToken = PatreonOAuth.exchangeCodeForToken(code);
            MinecraftUser.link(accessToken, userId);
            return "Patreon OAuth callback processed. Type /code with your code in Minecraft:  \n " + accessToken;
        });
    }

    public void start() {
        Spark.init();
    }

    public void stop() {
        Spark.stop();
    }


}
