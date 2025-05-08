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
import com.brandongcobb.vyrtuous.cogs.*;
import com.brandongcobb.vyrtuous.utils.handlers.*;
import com.brandongcobb.vyrtuous.utils.inc.*;
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
        if (message.getAuthor().isBot()) return;
    
        cm.completeGetConfigValue("discord_command_prefix", String.class)
          .thenAcceptAsync(prefixObj -> {
            String prefix = (String) prefixObj;
            String messageContent = message.getContentDisplay();
    
            if (messageContent.startsWith(prefix)) return;  // Skip command handling
    
            AIManager aim = new AIManager(cm);
            MessageManager mem = new MessageManager(cm);
            User sender = event.getAuthor();
            long senderId = sender.getIdLong();
            List<Attachment> attachments = message.getAttachments();
            ResponseObject previousResponse = userResponseMap.get(senderId);
            final boolean[] multimodal = new boolean[]{false};
    
            String content = messageContent.replace("@Vyrtuous", "");
    
            CompletableFuture<String> fullContentFuture;
    
            if (attachments != null && !attachments.isEmpty()) {
                fullContentFuture = mem.completeProcessAttachments(attachments)
                    .thenApply(attachmentContentList -> {
                        String joined = String.join("\n", attachmentContentList);
                        multimodal[0] = true;
                        return joined + "\n" + content;
                    });
            } else {
                fullContentFuture = CompletableFuture.completedFuture(content);
            }
    
            fullContentFuture
                .thenCompose(fullContent -> {
                    System.out.println("Processing fullContent: " + fullContent);
    
                    return aim.completeModeration(fullContent)
                        .thenCompose(moderationResponseObject ->
                            moderationResponseObject.completeGetFlagged()
                                .thenCompose(flagged -> {
                                    if (flagged) {
                                        ModerationManagerv2 mom = new ModerationManagerv2(cm);
                                        return moderationResponseObject.completeGetFormatFlaggedReasons()
                                            .thenCompose(reason ->
                                                mom.completeHandleModeration(message, reason)
                                                    .thenApply(ignored -> null)
                                            );
                                    } else {
                                        return cm.completeGetUserModelSettings()
                                            .thenCompose(userModelSettings -> {
                                                Map<Long, String> userModelObject = (Map<Long, String>) userModelSettings;
                                                String setting = userModelObject.getOrDefault(senderId, Helpers.OPENAI_CHAT_MODEL);
    
                                                return aim.completeResolveModel(fullContent, multimodal[0], setting)
                                                    .thenCompose(model -> {
                                                        CompletableFuture<String> previousResponseIdFuture =
                                                            (previousResponse != null)
                                                                ? previousResponse.completeGetResponseId()
                                                                : CompletableFuture.completedFuture(null);
    
                                                        return previousResponseIdFuture
                                                            .thenCompose(previousResponseId ->
                                                                aim.completeChat(fullContent, previousResponseId, model)
                                                                    .thenCompose(chatResponseObject -> {
                                                                        CompletableFuture<Void> setPrevFuture =
                                                                            (previousResponse != null)
                                                                                ? previousResponse.completeGetPreviousResponseId()
                                                                                      .thenCompose(prevId ->
                                                                                          chatResponseObject.completeSetPreviousResponseId(prevId))
                                                                                : chatResponseObject.completeSetPreviousResponseId(null);
    
                                                                        return setPrevFuture.thenCompose(v -> {
                                                                            userResponseMap.put(senderId, chatResponseObject);
                                                                            return chatResponseObject.completeGetOutput()
                                                                                .thenCompose(outputContent ->
                                                                                    mem.completeSendResponse(message, outputContent)
                                                                                        .thenApply(ignored -> null));
                                                                        });
                                                                    }));
                                                    });
                                            });
                                    }
                                }));
                })
                .exceptionally(ex -> {
                    return null;
                });
        });
    }
}

