/*  OAuthUserSession.java The primary purpose of this program is to
 *  multithread the parallel requests to the OAuth server.
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
package com.brandongcobb.oauthplugin.utils.handlers;

import com.brandongcobb.oauthplugin.OAuthPlugin;
import com.brandongcobb.oauthplugin.utils.handlers.DiscordUser;
import com.brandongcobb.oauthplugin.utils.handlers.MinecraftUser;
import java.util.concurrent.ScheduledFuture;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Represents a pending OAuth session for a player.
 */
public class OAuthUserSession {
    private final String playerUuid;
    private final String command;  // "discord" or "patreon"
    private String accessToken;
    private BukkitRunnable timeoutTask;

    /**
     * @param playerUuid  Minecraft player's UUID string
     * @param command     Type of OAuth flow: "discord" or "patreon"
     */
    public OAuthUserSession(String playerUuid, String command) {
        this.playerUuid = playerUuid;
        this.command = command;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public String getCommand() {
        return command;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public BukkitRunnable getTimeoutTask() {
        return timeoutTask;
    }

    /**
     * Store and cancel any existing timeout task.
     */
    public void setTimeoutTask(BukkitRunnable timeoutTask) {
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }
        this.timeoutTask = timeoutTask;
    }
}

