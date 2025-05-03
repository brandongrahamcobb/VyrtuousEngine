/*  MessageManager.java The purpose of this program is to manage responding to
    users on Discord.
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
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.metadata.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.FileUpload;

public class MessageManager {

    private static Vyrtuous app;
    private static ObjectMapper mapper = new ObjectMapper();
    private static Lock lock;
    private static Logger logger;
    private static File tempDirectory;

    public MessageManager (Vyrtuous application) {
        this.app = application;
        this.logger = app.logger;
        this.tempDirectory = app.tempDirectory;
    }

    public static String encodeImage(byte[] imageBytes) {
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static CompletableFuture<MetadataContainer> completeProcessConversation(long senderId, String content, List<Attachment> attachments) {
        return AIManager.completeGetConversationContainer(senderId).thenCompose(container -> {
            if (content != null && !content.trim().isEmpty()) {
                String role = "user";
                String name = null;
                String processedText = content;
                if (content.startsWith("[") && content.contains("]")) {
                    int closingBracket = content.indexOf("]");
                    name = content.substring(1, closingBracket).trim();
                    processedText = content.substring(closingBracket + 1).trim();
                }
                MetadataKey<String> textKey = new MetadataKey<>("conversation.text", String.class);
                MetadataKey<String> roleKey = new MetadataKey<>("conversation.role", String.class);
                MetadataKey<String> nameKey = new MetadataKey<>("conversation.name", String.class);
                container.put(textKey, processedText);
                container.put(roleKey, role);
                if (name != null && !name.isEmpty()) {
                    container.put(nameKey, name);
                }
            }
            if (attachments != null && !attachments.isEmpty()) {
                return completeProcessAttachments(attachments).thenApply(attachmentData -> {
                    MetadataKey<List<String>> attachmentsKey = new MetadataKey<>("conversation.attachments", List.class);
                    container.put(attachmentsKey, attachmentData);
                    return container;
                });
            }

            return CompletableFuture.completedFuture(container);
        });
    }

    public static CompletableFuture<MetadataContainer> completeGetAppConversationContainer() {
        return app.completeGetMetadataContainer();
    }

    public static CompletableFuture<List<String>> completeProcessAttachments(List<Attachment> attachments) {
        logger.info("Entered processAttachments with " + attachments.size() + " attachments");
        List<String> processedAttachments = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        if (!tempDirectory.exists()) {
            tempDirectory.mkdirs();
        }
    
        for (Attachment attachment : attachments) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                File file = new File(tempDirectory, attachment.getFileName());
                try (InputStream in = new URL(attachment.getUrl()).openStream()) {
                    Files.copy(in, file.toPath());
                    String contentType = getContentTypeFromFileName(attachment.getFileName());
                    if (contentType.startsWith("image/")) {
                        byte[] imageBytes = Files.readAllBytes(file.toPath());
                        String base64Image = encodeImage(imageBytes);
                        Map<String, String> urlData = new HashMap<>();
                        urlData.put("data_url", "data:" + contentType + ";base64," + base64Image);
                        String json = mapper.writeValueAsString(urlData);
                        processedAttachments.add(json);
                    } else if (contentType.startsWith("text/")) {
                        String textContent = new String(Files.readAllBytes(file.toPath()));
                        processedAttachments.add(textContent);
                    }
                } catch (IOException e) {
                    logger.severe("Error processing file " + attachment.getFileName() + ": " + e.getMessage());
                }
            });
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                logger.info("Finished processing " + attachments.size() + " attachments");
                return processedAttachments;
            });
    }

    private static String getContentTypeFromFileName(String fileName) {
        if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".txt")) {
            return "text/plain";
        }
        return "application/octet-stream"; // Default type
    }

    public static CompletableFuture<Message> completeSendDM(User user, String content) {
        return user.openPrivateChannel()
            .submit()
            .thenCompose(channel -> channel.sendMessage(content).submit());
    }

    public static CompletableFuture<Message> completeSendDiscordMessage(Message message, String content, MessageEmbed embed) {
        return message.getGuildChannel()
            .asTextChannel()
            .sendMessage(content)
            .addEmbeds(embed)
            .submit();
    }

    public static CompletableFuture<Message> completeSendDiscordMessage(Message message, String content) {
        return message.getGuildChannel()
            .asTextChannel()
            .sendMessage(content)
            .submit();
    }

    public static CompletableFuture<Message> completeSendDiscordMessage(Message message, String content, File file) {
        return message.getGuildChannel()
            .asTextChannel()
            .sendMessage(content)
            .addFiles(FileUpload.fromData(file))
            .submit();
    }

    public static CompletableFuture<Message> completeSendDiscordMessage(PrivateChannel channel, String content, File file) {
        return channel.sendMessage(content)
            .addFiles(FileUpload.fromData(file))
            .submit();
    }

    public static CompletableFuture<Message> completeSendDiscordMessage(PrivateChannel channel, String content, MessageEmbed embed) {
        return channel.sendMessage(content)
            .addEmbeds(embed)
            .submit();
    }

    public boolean hasAttachments(Message message) {
        return !message.getAttachments().isEmpty();
    }

    public String getMessageContent(Message message) {
        return message.getContentDisplay();
    }
}
