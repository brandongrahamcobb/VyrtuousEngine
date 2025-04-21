/*  OAuthServer.java The purpose of this program is to serve as OAuth server for PatreonPlugin.
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
package com.brandongcobb.patreonplugin.utils.handlers;

import com.brandongcobb.patreonplugin.PatreonPlugin;
import spark.Spark;

public class OAuthServer {
    private final PatreonPlugin plugin;

    public OAuthServer(PatreonPlugin plugin) {
        this.plugin = plugin;
        Spark.port(Integer.parseInt(plugin.configManager.getConfigValue("api_keys", "Spark").getStringValue("port"))); // or any port you wish to use
        Spark.get(plugin.configManager.getConfigValue("api_keys", "Spark").getStringValue("redirect_uri_endpoint"), (req, res) -> {
            String code = req.queryParams("code");
            plugin.handleOAuthCallback(code); // Pass the code to your plugin
            res.status(200);
            return "Your Patreon has been added to the database!";
        });
    }

    public void start() {
        Spark.init();
    }

    public void stop() {
        Spark.stop();
    }
}
