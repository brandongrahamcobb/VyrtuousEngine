/*  EventListeners.java The purpose of this program is to listen for any of the program's endpoints and handles them.
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
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.utils.handlers.ModerationManager;
import com.brandongcobb.vyrtuous.utils.handlers.PatreonUser;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.util.List;
import javax.annotation.Nonnull;
//import org.javacord.api.DiscordApi;
//import org.javacord.api.DiscordApiBuilder;
//import org.javacord.api.entity.message.Message;
//import org.javacord.api.entity.message.MessageAttachment;
//import org.javacord.api.entity.channel.PrivateChannel;
//import org.javacord.api.entity.user.User;
//import org.javacord.api.event.message.MessageCreateEvent;
//import org.javacord.api.listener.message.MessageCreateListener;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EventListeners extends ListenerAdapter implements Cog {

    private final Vyrtuous app;
    private JDA api;
    private Lock lock;
    private long senderId;

    public EventListeners (Vyrtuous application) {
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
        if (message.getAuthor().isBot()) return;
        boolean isMentioned = message.getMentions().getUsers().contains(api.getSelfUser());
        String content = message.getContentDisplay();
        User sender = event.getAuthor();
        long senderId = sender.getIdLong();
        List<Message.Attachment> attachments = message.getAttachments();
        Predicator.isDeveloper(sender).thenAccept(isDev -> {
            if (isDev) return;
            MessageManager.processArray(content, attachments)
            .thenCompose(inputArray ->
                AIManager.completeModeration(senderId, CompletableFuture.completedFuture(inputArray))
                    .thenCompose(moderationResponse -> {
                        if (moderationResponse != null && !moderationResponse.isEmpty()) {
                            return ModerationManager.completeHandleModeration(message, moderationResponse);
                        }
                        boolean shouldChat = content.length() > 1 &&
                                             "chat".equalsIgnoreCase(content.substring(1)) ||
                                             isMentioned;
                        if (!shouldChat) {
                            return CompletableFuture.completedFuture(null);
                        }
                        return AIManager.completeChat(senderId, CompletableFuture.completedFuture(inputArray))
                            .thenCompose(response ->
                                MessageManager.completeSendDiscordMessage(message, response)
                            )
                            // Convert the Message (or whatever is returned) to Void
                            .thenApply(sentMsg -> null);
                    })
            ).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        });
    }
}
