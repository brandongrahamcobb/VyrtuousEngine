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
import com.brandongcobb.vyrtuous.utils.handlers.RequestObject;
import com.brandongcobb.vyrtuous.utils.handlers.ResponseObject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.util.List;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListeners extends ListenerAdapter implements Cog {

    private JDA api;
    private final Vyrtuous app;
    private Lock lock;

    public EventListeners (Vyrtuous application) {
        this.app = application;
        this.lock = app.lock;
    }

    @Override
    public void register (JDA api) {
        api.addEventListener(this);
        this.api = api;
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Message message = event.getMessage();
        if (message.getAuthor().isBot()) return;
    
        boolean isMentioned = message.getMentions().getUsers().contains(api.getSelfUser());
        String content = message.getContentDisplay();
        User sender = event.getAuthor();
        long senderId = sender.getIdLong();
        List<Attachment> attachments = message.getAttachments();
    
        app.completeGetInstance().thenAccept(appInstance -> {
            CompletableFuture<String> fullContentFuture;
    
            if (attachments != null && !attachments.isEmpty()) {
                fullContentFuture = MessageManager.completeProcessAttachments(attachments)
                    .thenApply(attachmentContent -> attachmentContent + content);
            } else {
                fullContentFuture = CompletableFuture.completedFuture(content);
            }
    
            fullContentFuture.thenCompose(fullContent -> {
                System.out.println("fullContent: " + fullContent);
    
                CompletableFuture<ResponseObject> moderationFuture =
                    AIManager.completeModeration(fullContent);
                CompletableFuture<ResponseObject> chatFuture =
                    AIManager.completeChat(fullContent);
    
                return moderationFuture.thenCompose(moderationResponseObject ->
                    moderationResponseObject.completeGetFlagged()
                        .thenCompose(flagged -> {
                            if (flagged) {
                                return moderationResponseObject.completeGetFormatFlaggedReasons()
                                    .thenCompose(reason ->
                                        ModerationManager.completeHandleModeration(message, reason)
                                            .thenApply(m -> null)
                                    );
                            }
    
                            boolean shouldChat = (fullContent.length() > 1 &&
                                    "chat".equalsIgnoreCase(fullContent.substring(1))) || isMentioned;
    
                            if (!shouldChat) {
                                return CompletableFuture.completedFuture(null);
                            }
    
                            return chatFuture.thenCompose(chatResponseObject ->
                                chatResponseObject.completeGetOutput()
                                    .thenCompose(outputContent ->
                                        MessageManager.completeSendDiscordMessage(message, outputContent)
                                            .thenApply(m -> null)
                                    )
                            );
                        })
                );
            }).exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
        });
    }
}

