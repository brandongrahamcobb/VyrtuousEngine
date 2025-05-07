/*  EventListeners.java The purpose of this program is to listen for any of the program's endpoints and handles them.
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
package com.brandongcobb.vyrtuous.cogs;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.brandongcobb.vyrtuous.utils.handlers.*;
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
    private ConfigManager cm;
    private DiscordBot bot;

    @Override
    public void register (JDA api, DiscordBot bot, ConfigManager cm) {
        this.bot = bot.completeGetBot();
        this.api = api;
        api.addEventListener(this);
        this.cm = cm.completeGetInstance();
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Message message = event.getMessage();
        AIManager aim = new AIManager(cm);
        MessageManager mem = new MessageManager(cm);
        User selfUser = this.api.getSelfUser();
        if (message.getAuthor().isBot()) return;
        boolean isMentioned = message.getMentions().getUsers().contains(this.api.getSelfUser());
        String content = message.getContentDisplay().replace("@Vyrtuous", "");
        User sender = event.getAuthor();
        long senderId = sender.getIdLong();
        List<Attachment> attachments = message.getAttachments();
        ResponseObject previousResponse = userResponseMap.get(senderId);
        final boolean[] multimodal = new boolean[]{false};
        CompletableFuture<String> fullContentFuture;
        if (attachments != null && !attachments.isEmpty()) {
            fullContentFuture = mem.completeProcessAttachments(attachments)
                .thenApply(attachmentContentList -> {
                    String joinedAttachmentContent = String.join("\n", attachmentContentList);
                    return joinedAttachmentContent + "\n" + content;
                });
            multimodal[0] = true;
        } else {
            fullContentFuture = CompletableFuture.completedFuture(content);
        }
        fullContentFuture.thenCompose(fullContent -> {
            System.out.println("Processing fullContent: " + fullContent);
            return aim.completeModeration(fullContent)
                .thenCompose(moderationResponseObject ->
                    moderationResponseObject.completeGetFlagged()
                        .thenCompose(flagged -> {
                            if (flagged) {
                                ModerationManager mom = new ModerationManager(cm);
                                return moderationResponseObject.completeGetFormatFlaggedReasons()
                                    .thenCompose(reason ->
                                        mom.completeHandleModeration(message, reason)
                                            .thenApply(ignored -> null)
                                    );
                            } else {
                                boolean shouldChat = isMentioned;
                                if (!shouldChat) {
                                    return CompletableFuture.completedFuture(null);
                                }
                                return aim.completeResolveModel(fullContent, multimodal[0])
                                    .thenCompose(model -> {
                                        CompletableFuture<String> previousResponseIdFuture;
                                        if (previousResponse != null) {
                                            previousResponseIdFuture = previousResponse.completeGetResponseId();
                                        } else {
                                            previousResponseIdFuture = CompletableFuture.completedFuture(null);
                                        }
                                        return previousResponseIdFuture
                                            .thenCompose(previousResponseId -> aim.completeChat(fullContent, previousResponseId, model))
                                            .thenCompose(chatResponseObject -> {
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
                                                            mem.completeSendResponse(message, outputContent)
                                                                .thenApply(ignored -> null)
                                                        );
                                                });
                                            });
                                    });
                            }
                        })
                );
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
}

