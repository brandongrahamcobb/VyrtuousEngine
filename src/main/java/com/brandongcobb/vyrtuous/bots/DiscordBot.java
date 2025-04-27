/*  DiscordBot.java The purpose of this program is to be a drop in replacement
    for any discord bot library (JDA, Javacord, Discord4J). Current: Javacord.
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
package com.brandongcobb.vyrtuous.bots;

import com.brandongcobb.vyrtuous.cogs.EventListeners;
import com.brandongcobb.vyrtuous.cogs.Cog;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.Vyrtuous;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.intent.Intent;

public class DiscordBot {

    private static Vyrtuous app;
    private static DiscordApi api;
    private static String discordApiKey;
    private static long discordOwnerId;
    private static HikariDataSource dbPool;
    private static Lock lock;
    private static Logger logger;

    public DiscordBot(Vyrtuous application) {
        this.app = application;
    }

    private static void initiateDiscordApi() {
        api = new DiscordApiBuilder().setToken(discordApiKey).addIntents(Intent.MESSAGE_CONTENT).login().join();
        loadCogs();
    }

    private static void loadCogs() {
        List<Cog> cogs = new ArrayList<>();
        cogs.add(new EventListeners(ConfigManager.getApp()));
        for (Cog cog : cogs) {
            cog.register(api);
        }
    }

    public DiscordApi getApi() {
        return this.api;
    }

    public static void start() {
        app = ConfigManager.getApp();
        logger = app.logger;
        discordApiKey = ConfigManager.getNestedConfigValue("api_keys", "Discord").getStringValue("api_key");
        discordOwnerId = ConfigManager.getLongValue("discord_owner_id");
        dbPool = app.dbPool;
        lock = app.lock;
        initiateDiscordApi();
        app.logger.info("Discord bot started!");
    }
}
