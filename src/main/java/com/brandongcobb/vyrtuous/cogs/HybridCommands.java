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
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
//import org.javacord.api.DiscordApi;
//import org.javacord.api.DiscordApiBuilder;
//import org.javacord.api.entity.message.Message;
//import org.javacord.api.entity.user.User;
//import org.javacord.api.event.message.MessageCreateEvent;
//import org.javacord.api.listener.message.MessageCreateListener;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HybridCommands extends ListenerAdapter implements Cog {

//    private static DiscordApi api;
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
//    public void register (DiscordApi api) {
    public void register (JDA api) {
//        api.addMessageCreateListener(new MessageCreateListener() {
        api.addEventListener(this);
    }

//    public void onMessageCreate(MessageCreateEvent event) {
    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Message message = event.getMessage();
//                if (message.getAuthor().isBotUser()) {
        if (message.getAuthor().isBot()) {
            return;
        }
//        User sender = message.getAuthor().asUser().orElse(null);
        User sender = message.getAuthor();
        senderId = sender.getIdLong();
//                String content = event.getMessageContent();
        String content = event.getMessage().getContentDisplay();
        if (content.startsWith(ConfigManager.getStringValue("discord_command_prefix"))) {
            String[] args = content.substring(1).split(" ");
            if (args[0].toLowerCase().equals("discord") || args[0].toLowerCase().equals("patreon")) {
                DiscordUser discordUser = new DiscordUser(app, senderId);
                OAuthUserSession session = new OAuthUserSession(app, discordUser, args[0]);
                discordSessions.put(discordUser, session);
                try {
                    String state = URLEncoder.encode(String.valueOf(senderId), "UTF-8");
                    switch (args[0].toLowerCase()) {
                        case "patreon":
                            authUrl = PatreonOAuth.getAuthorizationUrl() + "&state=" + state;
                        case "discord":
                            authUrl = DiscordOAuth.getAuthorizationUrl() + "&state=" + state;
                        default:
                            break;
                    }
                    MessageManager.sendDiscordMessage(message, "Please visit the following URL to authorize: " + authUrl);
                } catch (UnsupportedEncodingException uee) {
                    uee.printStackTrace();
                }
                session.setWaiting(true);
                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                session.setTimeoutTask(scheduler.schedule(() -> {
                    session.setWaiting(false);
                    discordSessions.remove(discordUser);
                    MessageManager.sendDiscordMessage(message, "Authentication timed out. Please try again.");
                }, 10, TimeUnit.MINUTES));
            }
            switch (args[0].toLowerCase()) {
                case "code":
                    DiscordUser discordUser = new DiscordUser(app, senderId);
                    OAuthUserSession session = discordSessions.get(discordUser);
                    if (session == null || !session.isWaiting()) {
                         MessageManager.sendDiscordMessage(message, "You don't have an active authentication session. Start with `/patreon` or `/discord` first.");
                         return;
                    }
                    String providedCode = args[1];
                    if (providedCode.equals(session.getAccessToken())) {
                        session.getTimeoutTask().cancel(false);
                        discordSessions.remove(discordUser);
                        MessageManager.sendDiscordMessage(message, "Authentication successful! You are now linked.");
                    } else {
                        MessageManager.sendDiscordMessage(message, "Invalid code. Please try again.");
                    }
                default:
                    break;
            }
        }
    }
}

