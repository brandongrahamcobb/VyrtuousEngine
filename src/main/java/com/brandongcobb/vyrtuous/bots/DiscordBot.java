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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.locks.Lock;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordBot {

    private static Vyrtuous app;
    private static JDA api;
    private static String discordApiKey;
    private static long discordOwnerId;
    private static HikariDataSource dbPool;
    private static Lock lock;
    private static Logger logger;

    public DiscordBot(Vyrtuous application) {
        this.app = application;
    }

    public static CompletableFuture<Void> start() {
        return ConfigManager.completeGetApp().thenCompose(appResult -> {
            app = appResult;
            logger = app.logger;
            dbPool = app.dbPool;
            lock = app.lock;
    
            // Get the Discord API key and owner ID
            return ConfigManager.completeGetNestedConfigValue("api_keys", "Discord")
                .thenCompose(discordApiKeys ->
                    discordApiKeys.completeGetConfigStringValue("api_key"))
                .thenCombine(
                    ConfigManager.completeGetLongValue("discord_owner_id"),
                    (apiKey, ownerId) -> {
                        discordApiKey = apiKey;
                        discordOwnerId = ownerId;
                        return null;
                    }
                );
        }).thenCompose(ignore -> {
            // Initialize the JDA client with the retrieved API key
            JDABuilder builder = JDABuilder.createDefault(discordApiKey)
                .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MEMBERS); // Add necessary intents
    
            // Start the JDA bot and wait for it to be ready
            try {
                builder.build().awaitReady();  // This blocks the current thread and keeps the bot alive
                return CompletableFuture.completedFuture(null); // Return completed future to continue the chain
            } catch (Exception e) {
                logger.severe("Failed to start the Discord bot: " + e.getMessage());
                e.printStackTrace();
                return CompletableFuture.failedFuture(e);
            }
        }).thenRun(() -> {
            // Once the bot is up and running, log that the bot has started
            app.logger.info("Discord bot started!");
        }).exceptionally(ex -> {
            // Handle any exceptions that occurred during the bot setup process
            app.logger.severe("Error starting the Discord bot: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        });
    }

    private static CompletableFuture<Void> initiateDiscordApi() {
        return CompletableFuture.runAsync(() -> {
            try {
                api = JDABuilder.createDefault(discordApiKey).build();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }).thenCompose(ignore -> loadCogs());
    }

    private static CompletableFuture<Void> loadCogs() {
        return ConfigManager.completeGetApp().thenAcceptAsync(appResult -> {
            app = appResult;
            List<Cog> cogs = new ArrayList<>();
            cogs.add(new EventListeners(app));
            for (Cog cog : cogs) {
                cog.register(api);
            }
        });
    }

    public CompletableFuture<JDA> completeGetApi() {
        return CompletableFuture.supplyAsync(() -> api);
    }
}
