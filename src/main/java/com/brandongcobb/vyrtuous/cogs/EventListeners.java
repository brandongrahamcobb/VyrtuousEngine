/*  EventListeners.java The purpose of this program is to listen for any
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
import com.brandongcobb.vyrtuous.bots.*;
import com.brandongcobb.vyrtuous.utils.handlers.*;
import com.brandongcobb.vyrtuous.utils.sec.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

public class EventListeners extends ListenerAdapter implements Cog {

    private static Vyrtuous app;
    private Database db;
    private String authUrl;
    private static Lock lock;
    private long senderId;
    public static Map<DiscordUser, OAuthUserSession> discordSessions = new HashMap<>();

    @Override
    public void register (JDA api, DiscordBot bot) {
        api.addEventListener(this);
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Message message = event.getMessage();
        if (message.getAuthor().isBot()) return;
        User sender = message.getAuthor();
        long senderId = sender.getIdLong();
        String content = message.getContentDisplay();
        ConfigManager cm = ConfigManager.getInstance();
        cm.completeGetConfigValue("DISCORD_COMMAND_PREFIX", String.class).thenAcceptAsync(prefix -> {
            String prefixObj = (String) prefix;
            if (!content.startsWith(prefixObj)) return;
            String[] args = content.substring(prefixObj.length()).split(" ");
            if (args.length == 0) return;
            String command = args[0].toLowerCase();
            MessageManager mem = new MessageManager();
            if (command.equals("discord") || command.equals("patreon") || command.equals("code")) {
                if (command.equals("discord") || command.equals("patreon")) {
                    DiscordUser discordUser = new DiscordUser(senderId);
                    OAuthUserSession session = new OAuthUserSession(app, discordUser, command);
                    discordSessions.put(discordUser, session);
                    String state = URLEncoder.encode(String.valueOf(senderId), StandardCharsets.UTF_8);
                    if (command.equals("discord")) {
                        DiscordOAuth doa = new DiscordOAuth();
                        doa.completeGetAuthorizationUrl().thenCompose(authUrl -> {
                            String fullUrl = authUrl + "&state=" + state;
                            session.setWaiting(true);
                            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                            session.setTimeoutTask(scheduler.schedule(() -> {
                                session.setWaiting(false);
                                discordSessions.remove(discordUser);
                                mem.completeSendDiscordMessage(message, "Authentication timed out. Please try again.");
                            }, 10, TimeUnit.MINUTES));
                            return mem.completeSendDiscordMessage(message, "Please visit the following URL to authorize: " + fullUrl);
                        }).exceptionally(ex -> { ex.printStackTrace(); return null; });
                    } else {
                        PatreonOAuth poa = new PatreonOAuth();
                        poa.completeGetAuthorizationUrl().thenCompose(authUrl -> {
                            String fullUrl = authUrl + "&state=" + state;
                            session.setWaiting(true);
                            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                            session.setTimeoutTask(scheduler.schedule(() -> {
                                session.setWaiting(false);
                                discordSessions.remove(discordUser);
                                mem.completeSendDiscordMessage(message, "Authentication timed out. Please try again.");
                            }, 10, TimeUnit.MINUTES));
                            return mem.completeSendDiscordMessage(message, "Please visit the following URL to authorize: " + fullUrl);
                        }).exceptionally(ex -> { ex.printStackTrace(); return null; });
                    }
                } else if (command.equals("code")) {
                    if (args.length < 2) {
                        mem.completeSendDiscordMessage(message, "You must provide a code. Usage: `/code <your-code>`");
                        return;
                    }
                    DiscordUser discordUser = new DiscordUser(senderId);
                    OAuthUserSession session = discordSessions.get(discordUser);
                    if (session == null || !session.isWaiting()) {
                        mem.completeSendDiscordMessage(message, "You don't have an active authentication session. Start with `/patreon` or `/discord` first.");
                        return;
                    }
                    String providedCode = args[1];
                    if (providedCode.equals(session.getAccessToken())) {
                        session.getTimeoutTask().cancel(false);
                        discordSessions.remove(discordUser);
                        mem.completeSendDiscordMessage(message, "Authentication successful! You are now linked.");
                    } else {
                        mem.completeSendDiscordMessage(message, "Invalid code. Please try again.");
                    }
                }
            }
        });
    }
}

