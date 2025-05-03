/*  HybridCommands.java The purpose of this program is to listen for any
 *  of the program's endpoints and handles them.
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
package com.brandongcobb.vyrtuous.cogs;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.DiscordUser;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.utils.sec.DiscordOAuth;
import com.brandongcobb.vyrtuous.utils.sec.PatreonOAuth;
import com.brandongcobb.vyrtuous.utils.handlers.OAuthUserSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import java.util.concurrent.CompletionException;

public class HybridCommands extends ListenerAdapter implements Cog {

    private static Vyrtuous app;
    private String authUrl;
    private static Lock lock;
    private long senderId;
    public static Map<DiscordUser, OAuthUserSession> discordSessions = new HashMap<>();

    public HybridCommands (Vyrtuous application) {
        this.app = application;
        this.lock = app.lock;
    }

    @Override
    public void register (JDA api) {
        api.addEventListener(this);
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Message message = event.getMessage();
        if (message.getAuthor().isBot()) {
            return;
        }
        User sender = message.getAuthor();
        long senderId = sender.getIdLong();
        String content = message.getContentDisplay();
        ConfigManager.completeGetConfigStringValue("discord_command_prefix").thenAccept(prefix -> {
            if (!content.startsWith(prefix)) return;
            String[] args = content.substring(1).split(" ");
            if (args.length == 0) return;
            String command = args[0].toLowerCase();
            DiscordUser discordUser = new DiscordUser(app, senderId);
            switch (command) {
                case "discord":
                case "patreon":
                    OAuthUserSession session = new OAuthUserSession(app, discordUser, command);
                    discordSessions.put(discordUser, session);
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            String state = URLEncoder.encode(String.valueOf(senderId), StandardCharsets.UTF_8.name());
                            String authUrl = switch (command) {
                                case "patreon" -> PatreonOAuth.completeGetAuthorizationUrl() + "&state=" + state;
                                case "discord" -> DiscordOAuth.completeGetAuthorizationUrl() + "&state=" + state;
                                default -> null;
                            };
                            return authUrl;
                        } catch (UnsupportedEncodingException e) {
                            throw new CompletionException(e);
                        }
                    }).thenCompose(authUrl ->
                        MessageManager.completeSendDiscordMessage(message, "Please visit the following URL to authorize: " + authUrl)
                    ).exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
                    session.setWaiting(true);
                    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                    session.setTimeoutTask(scheduler.schedule(() -> {
                        session.setWaiting(false);
                        discordSessions.remove(discordUser);
                        MessageManager.completeSendDiscordMessage(message, "Authentication timed out. Please try again.");
                    }, 10, TimeUnit.MINUTES));
                    break;
                case "code":
                    if (args.length < 2) {
                        MessageManager.completeSendDiscordMessage(message, "Please provide an authentication code.");
                        return;
                    }
                    OAuthUserSession existingSession = discordSessions.get(discordUser);
                    if (existingSession == null || !existingSession.isWaiting()) {
                        MessageManager.completeSendDiscordMessage(message, "You don't have an active authentication session. Start with `/patreon` or `/discord` first.");
                        return;
                    }
                    String providedCode = args[1];
                    if (providedCode.equals(existingSession.getAccessToken())) {
                        existingSession.getTimeoutTask().cancel(false);
                        discordSessions.remove(discordUser);
                        MessageManager.completeSendDiscordMessage(message, "Authentication successful! You are now linked.");
                    } else {
                        MessageManager.completeSendDiscordMessage(message, "Invalid code. Please try again.");
                    }
                    break;
                default:
                    break;
            }
        });
    }
}

