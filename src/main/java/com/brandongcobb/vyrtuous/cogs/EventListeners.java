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
import com.brandongcobb.vyrtuous.utils.handlers.RequestObject;
import com.brandongcobb.vyrtuous.utils.handlers.ResponseObject;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class EventListeners extends ListenerAdapter implements Cog {

    private final Map<Long, ResponseObject> userResponseMap = new ConcurrentHashMap<>();
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
        String content = message.getContentDisplay().replace("@Vyrtuous", "");
        User sender = event.getAuthor();
        long senderId = sender.getIdLong();
        List<Attachment> attachments = message.getAttachments();
    
        ResponseObject previousResponse = userResponseMap.get(senderId);
    
        app.completeGetInstance().thenCompose(appInstance -> {
            CompletableFuture<String> fullContentFuture;
    
            if (attachments != null && !attachments.isEmpty()) {
                fullContentFuture = MessageManager.completeProcessAttachments(attachments)
                    .thenApply(attachmentContentList -> {
                        String joinedAttachmentContent = String.join("\n", attachmentContentList);
                        String fullCombined = joinedAttachmentContent + "\n" + content;
                        return fullCombined;
                    });
            } else {
                fullContentFuture = CompletableFuture.completedFuture(content);
            }
    
            return fullContentFuture.thenCompose(fullContent -> {
                System.out.println("Processing fullContent: " + fullContent);
    
                CompletableFuture<ResponseObject> moderationFuture = AIManager.completeModeration(fullContent);
    
                CompletableFuture<CompletableFuture<ResponseObject>> chatFutureWrapper;
                if (previousResponse != null) {
                    chatFutureWrapper = previousResponse.completeGetPreviousResponseId()
                        .thenApply(prevId -> AIManager.completeChat(fullContent, prevId));
                } else {
                    chatFutureWrapper = CompletableFuture.completedFuture(AIManager.completeChat(fullContent, null));
                }
    
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
    
                            return chatFutureWrapper.thenCompose(chatFuture ->
                                chatFuture.thenCompose(chatResponseObject -> {
                                    CompletableFuture<Void> setPrevFuture;
                                    if (previousResponse != null) {
                                        setPrevFuture = previousResponse.completeGetPreviousResponseId()
                                            .thenCompose(prevId -> chatResponseObject.completeSetPreviousResponseId(prevId));
                                    } else {
                                        setPrevFuture = chatResponseObject.completeSetPreviousResponseId(null);
                                    }
    
                                    return setPrevFuture.thenCompose(v -> {
                                        userResponseMap.put(senderId, chatResponseObject);
                                        return chatResponseObject.completeGetOutput()
                                            .thenCompose(outputContent ->
                                                MessageManager.completeSendResponse(message, outputContent)
                                                    .thenApply(m -> null)
                                            );
                                    });
                                })
                            );
                        })
                );
            });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }


}

