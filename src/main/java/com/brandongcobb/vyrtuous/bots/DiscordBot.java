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

import com.brandongcobb.vyrtuous.cogs.Cog;
import com.brandongcobb.vyrtuous.cogs.EventListeners;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.Vyrtuous;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import net.dv8tion.jda.api.JDA;

public class DiscordBot {

    private static JDA api;
    private static Vyrtuous app;
    public static String discordApiKey;
    private static long discordOwnerId;
    private static HikariDataSource dbPool;
    private static Lock lock;
    private static Logger logger;

    public DiscordBot(Vyrtuous application) {
        this.app = application;
    }

    public static CompletableFuture<Void> start(JDA jda) {
        return CompletableFuture.runAsync(() -> {
            api = jda;
            initiateDiscordApi();
        }).exceptionally(ex -> {
            logger.log(Level.SEVERE, "Error starting Discord bot", ex);
            return null;
        });
    }

    private static CompletableFuture<Void> initiateDiscordApi() {
        return app.completeGetInstance().thenAcceptAsync(app -> {
            loadCogs();
        });
    }

    private static CompletableFuture<Void> loadCogs() {
        return app.completeGetInstance().thenAcceptAsync(app -> {
            List<Cog> cogs = new ArrayList<>();
            cogs.add(new EventListeners(app));
            for (Cog cog : cogs) {
                cog.register(api);
            }
        });
    }

}
