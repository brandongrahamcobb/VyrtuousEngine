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
import java.util.HashMap;
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
    private static Vyrtuous app;
    private DiscordBot bot;

    @Override
    public void register (JDA api, DiscordBot bot) {
        this.bot = bot.completeGetBot();
        this.api = api;
        api.addEventListener(this);
    }
    private static final Logger logger = Logger.getLogger(Vyrtuous.class.getName());

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        Message message = event.getMessage();
        if (message.getAuthor().isBot()) return;
    
        String messageContent = message.getContentDisplay();
    
        // Skip prefix check entirely — all messages will be processed
        AIManager aim = new AIManager();
        MessageManager mem = new MessageManager();
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
                                    ModerationManager mom = new ModerationManager();
                                    return moderationResponseObject.completeGetFormatFlaggedReasons()
                                        .thenCompose(reason ->
                                            mom.completeHandleModeration(message, reason)
                                                .thenApply(ignored -> null)
                                        );
                                } else {
                                    return app.completeGetInstance().thenCompose(instance ->
                                        instance.completeGetUserModelSettings()
                                            .thenCompose(userModelSettings -> {
                                                String setting = userModelSettings.getOrDefault(
                                                    senderId,
                                                    ModelRegistry.OPENAI_RESPONSE_MODEL.asString()
                                                );
    
                                                return aim.completeResolveModel(fullContent, multimodal[0], setting)
                                                    .thenCompose(model -> {
                                                        CompletableFuture<String> previousResponseIdFuture =
                                                            (previousResponse != null)
                                                                ? previousResponse.completeGetResponseId()
                                                                : CompletableFuture.completedFuture(null);
    
                                                        return previousResponseIdFuture.thenCompose(previousResponseId -> {
                                                            System.out.println("[Chat] Invoking completeChat...");
                                                            return aim.completeChat(fullContent, previousResponseId, model)
                                                                .thenCompose(chatResponseObject -> {
                                                                    System.out.println("[Chat] completeChat returned: " + chatResponseObject);
    
                                                                    CompletableFuture<Void> setPrevFuture;
                                                                    if (previousResponse != null) {
                                                                        System.out.println("[Chat] previousResponse is not null. Getting previousResponseId...");
                                                                        setPrevFuture = previousResponse.completeGetPreviousResponseId()
                                                                            .thenCompose(prevId -> {
                                                                                System.out.println("[Chat] Got previousResponseId: " + prevId);
                                                                                return chatResponseObject.completeSetPreviousResponseId(prevId);
                                                                            });
                                                                    } else {
                                                                        System.out.println("[Chat] previousResponse is null. Setting previousResponseId to null...");
                                                                        setPrevFuture = chatResponseObject.completeSetPreviousResponseId(null);
                                                                    }
    
                                                                    return setPrevFuture.thenCompose(v -> {
                                                                        System.out.println("[Chat] previousResponseId set. Putting in userResponseMap...");
                                                                        userResponseMap.put(senderId, chatResponseObject);
    
                                                                        System.out.println("[Chat] Getting output...");
                                                                        return chatResponseObject.completeGetOutput()
                                                                            .thenCompose(outputContent -> {
                                                                                System.out.println("[Chat] Got output: " + outputContent);
                                                                                return mem.completeSendResponse(message, outputContent)
                                                                                    .thenApply(ignored -> {
                                                                                        System.out.println("[Chat] Response sent successfully.");
                                                                                        return null;
                                                                                    });
                                                                            });
                                                                    });
                                                                });
                                                        });
                                                    });
                                            })
                                    );
                                }
                            }));
            })
            .exceptionally(ex -> {
                ex.printStackTrace();
                mem.completeSendResponse(message, "An internal error occurred while processing your message.")
                    .thenApply(ignored -> null);
                return null;
            });
    }
}
////        @Override
////    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
//////        Message message = event.getMessage();
//////        if (message.getAuthor().isBot()) {
//////            logger.info("Message from bot ignored.");
//////            return;
//////        }
//////
//////        String messageContent = message.getContentDisplay();
//////        logger.info("Received message: " + messageContent);
//////
//////        // Initialize managers
//////        AIManager aim = new AIManager();
//////        MessageManager mem = new MessageManager();
//////
//////        User sender = event.getAuthor();
//////        long senderId = sender.getIdLong();
//////        List<Attachment> attachments = message.getAttachments();
//////
//////        logger.info("Processing message from user ID: " + senderId);
//////
//////        ResponseObject previousResponse = userResponseMap.get(senderId);
//////        final boolean[] multimodal = new boolean[]{false};
//////
//////        String content = messageContent.replace("@Vyrtuous", "");
//////
//////        CompletableFuture<String> fullContentFuture;
//////
//////        if (attachments != null && !attachments.isEmpty()) {
//////            logger.info("Processing attachments");
//////            fullContentFuture = mem.completeProcessAttachments(attachments)
//////                .thenApply(attachmentContentList -> {
//////                    String joined = String.join("\n", attachmentContentList);
//////                    multimodal[0] = true;
//////                    return joined + "\n" + content;
//////                });
//////        } else {
//////            logger.info("No attachments detected");
//////            fullContentFuture = CompletableFuture.completedFuture(content);
//////        }
//////
//////        fullContentFuture
//////            .thenCompose(fullContent -> {
//////                logger.info("Processing fullContent: " + fullContent);
//////
//////                return aim.completeModeration(fullContent)
//////                    .thenCompose(moderationResponseObject -> {
//////                        logger.info("Received moderation response");
//////                        return moderationResponseObject.completeGetFlagged()
//////                            .thenCompose(flagged -> {
//////                                if (flagged) {
//////                                    logger.info("Message flagged for moderation");
//////                                    ModerationManager mom = new ModerationManager();
//////                                    return moderationResponseObject.completeGetFormatFlaggedReasons()
//////                                        .thenCompose(reason -> {
//////                                            logger.info("Handling flagged message");
//////                                            return mom.completeHandleModeration(message, reason)
//////                                                .thenApply(ignored -> null);
//////                                        });
//////                                } else {
//////                                    // Log the normal flow
//////                                    logger.info("Message passed moderation");
//////                                    return app.completeGetInstance().completeGetUserModelSettings()
//////                                        .thenCompose(userModelSettings -> {
//////                                            Map<Long, String> userModelObject = (Map<Long, String>) userModelSettings;
//////                                            String setting = userModelObject.getOrDefault(senderId, ModelRegistry.OPENAI_RESPONSE_MODEL.asString());
//////
//////                                            logger.info("User setting: " + setting);
//////
//////                                            return aim.completeResolveModel(fullContent, multimodal[0], setting)
//////                                                .thenCompose(model -> {
//////                                                    logger.info("Resolved model: " + model);
//////                                                    CompletableFuture<String> previousResponseIdFuture =
//////                                                        (previousResponse != null)
//////                                                            ? previousResponse.completeGetResponseId()
//////                                                            : CompletableFuture.completedFuture(null);
//////
//////                                                    return previousResponseIdFuture
//////                                                        .thenCompose(previousResponseId -> {
//////                                                            logger.info("Previous response ID: " + previousResponseId);
//////                                                            return aim.completeChat(fullContent, previousResponseId, model)
//////                                                                .thenCompose(chatResponseObject -> {
//////                                                                    logger.info("Chat response received");
//////                                                                    ResponseObject chatObject = (ResponseObject) chatResponseObject;
//////                                                                    // Set previous response id
//////                                                                    CompletableFuture<Void> setPrevFuture =
//////                                                                        (previousResponse != null)
//////                                                                            ? previousResponse.completeGetPreviousResponseId()
//////                                                                                .thenCompose(prevId -> chatObject.completeSetPreviousResponseId(prevId))
//////                                                                            : chatObject.completeSetPreviousResponseId(null);
//////
//////                                                                    return setPrevFuture.thenCompose(v -> {
//////                                                                        logger.info("Updating user response map");
//////                                                                        userResponseMap.put(senderId, 
//////                                                                        return chatObject.completeGetOutput()
//////                                                                            .thenCompose(outputContent -> {
//////                                                                                logger.info("Sending response to user");
//////                                                                                return mem.completeSendResponse(message, outputContent)
//////                                                                                    .thenApply(ignored -> null);
//////                                                                            });
//////                                                                    });
//////                                                                });
//////                                                        });
//////                                                });
//////                                        });
//////                                }
//////                            });
//////                    });
//////            })
//////            .exceptionally(ex -> {
//////                logger.warning("Error processing message: " + ex.getMessage());
//////                // Notify user or log error further if needed
//////                mem.completeSendResponse(message, "An internal error occurred while processing your message.")
//////                    .thenApply(ignored -> null);
//////                return null;
////            });
//    }

