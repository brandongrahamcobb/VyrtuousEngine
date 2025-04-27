/*  OAuthUserSession.java The primary purpose of this program is to
 *  multithread the parallel requests to the OAuth server.
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

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.DiscordUser;
import com.brandongcobb.vyrtuous.utils.handlers.MinecraftUser;
import java.util.concurrent.ScheduledFuture;

public class OAuthUserSession {
    private Vyrtuous app;
    private String accessToken;
    private User associatedUser;
    private String commandName;
    private DiscordUser discordUser;
    private MinecraftUser minecraftUser;
    private String minecraftUserId;
    private boolean waiting = false;
    private ScheduledFuture<?> timeoutTask;

    public boolean isWaiting() { return waiting; }

    public OAuthUserSession(Vyrtuous application, MinecraftUser minecraftUser, String accessToken) {
        this.app = application;
        this.accessToken = accessToken;
        this.minecraftUser = minecraftUser;
        this.minecraftUserId = minecraftUserId;
    }

    public OAuthUserSession(Vyrtuous application,  DiscordUser discordUser, String accessToken) {
        this.app = application;
        this.accessToken = accessToken;
    }

    public ScheduledFuture<?> getTimeoutTask() { return timeoutTask; }

    public void setTimeoutTask(ScheduledFuture<?> timeoutTask) {
        if (this.timeoutTask != null && !this.timeoutTask.isDone()) {
            this.timeoutTask.cancel(false);
        }
        this.timeoutTask = timeoutTask;
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

    public void setWaiting(boolean value) {
        this.waiting = value;
    }
}

