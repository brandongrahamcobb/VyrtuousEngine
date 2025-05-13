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
package com.brandongcobb.vegan.utils.handlers;

import java.util.Timer;
import java.util.concurrent.CompletableFuture;
import spark.Spark;

public class OAuthServer {

    private ConfigManager configManager;

    public CompletableFuture<Void> completeConnectSpark() {
        ConfigManager cm = ConfigManager.getInstance();
        return cm.completeGetConfigValue("SPARK_PORT", Integer.class)
             .thenCompose(port -> {
                int portObj = (int) port;
                Spark.port(portObj);
                Spark.init();
                return null;
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
