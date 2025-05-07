/*  DiscordBot.java The purpose of this class is to manage the
 *  JDA discord api.
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
package com.brandongcobb.vyrtuous.bots;

import com.brandongcobb.vyrtuous.cogs.Cog;
import com.brandongcobb.vyrtuous.cogs.EventListeners;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.Vyrtuous;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import java.util.logging.Level;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot {

    private static Vyrtuous app;
    private static JDA bot;
    private static ConfigManager cm;
    private static final Logger logger = Logger.getLogger("Vyrtuous");;
    private static final ReentrantLock lock = new ReentrantLock();


    public DiscordBot(ConfigManager cm) {
        this.cm = cm;
    }


    public static CompletableFuture<JDA> completeGetBot() {
        return CompletableFuture.supplyAsync(() -> this);
    }

    public static CompletableFuture<Void> completeInitializeJDA() {
        return app.completeGetInstance().thenAcceptAsync(app -> {
            lock.lock();
            try {
                cm.completeGetConfigNestedValue("api_keys", "Discord")
                    .thenCompose(discordApiKeys ->
                        discordApiKeys.completeGetConfigValue("api_key", String.class)
                    )
                    .thenCombine(
                        cm.completeGetConfigValue("discord_owner_id", Long.class),
                        (apiKey, ownerId) -> {
                            bot = JDABuilder.createDefault(apiKey,
                                    GatewayIntent.GUILD_MESSAGES,
                                    GatewayIntent.MESSAGE_CONTENT,
                                    GatewayIntent.GUILD_MEMBERS)
                                .setActivity(Activity.playing("I take pharmacology personally."))
                                .build();
                            List<Cog> cogs = new ArrayList<>();
                            cogs.add(new EventListeners());
                            for (Cog cog : cogs) {
                                cog.register(bot);
                            }
                            return null;
                        }
                    )
                    .exceptionally(ex -> {
                        logger.log(Level.SEVERE, "Error starting Discord bot", ex);
                        return null;
                    }).join();
            } finally {
                lock.unlock();
            }
        });
    }

}
