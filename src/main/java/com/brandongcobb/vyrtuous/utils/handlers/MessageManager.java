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

    public static CompletableFuture<List<MessageContent>> processArray(String content, List<Message.Attachment> attachments) {
        List<MessageContent> array = new ArrayList<>();
        if (content != null && !content.trim().isEmpty()) {
            return processTextMessage(content).thenCompose(processedText -> {
                array.addAll(processedText);
                if (attachments != null && !attachments.isEmpty()) {
                    return processAttachments(attachments).thenApply(processedAttachments -> {
                        array.addAll(processedAttachments);
                        return array;
                    });
                }
                return CompletableFuture.completedFuture(array);
            });
        }
        return CompletableFuture.completedFuture(array);
    }

    public static CompletableFuture<List<MessageContent>> processAttachments(List<Message.Attachment> attachments) {
        logger.info("Entered processAttachments with " + attachments.size() + " attachments");
        List<MessageContent> processedAttachments = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        if (!tempDirectory.exists()) {
            tempDirectory.mkdirs();
        }
        for (Message.Attachment attachment : attachments) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                File file = new File(tempDirectory, attachment.getFileName());
                try (InputStream in = new URL(attachment.getUrl()).openStream()) {
                    Files.copy(in, file.toPath());
                    String contentType = getContentTypeFromFileName(attachment.getFileName());
                    if (contentType.startsWith("image/")) {
                        byte[] imageBytes = Files.readAllBytes(file.toPath());
                        String base64Image = encodeImage(imageBytes);
                        Map<String, String> urlData = new HashMap<>();
                        urlData.put("data_url", "data:image/jpeg;base64, " + base64Image);
                        String contentJson = mapper.writeValueAsString(urlData);
                        Map<String, String> messageMap = new HashMap<>();
                        messageMap.put("content", contentJson);
                        String messageJson = mapper.writeValueAsString(messageMap);
                        MessageContent attachmentMessageContent = new MessageContent("user", messageJson);
                        processedAttachments.add(attachmentMessageContent);
                    } else if (contentType.startsWith("text/")) {
                        String textContent = new String(Files.readAllBytes(file.toPath()));
                        MessageContent textMessageContent = new MessageContent("user", textContent);
                        processedAttachments.add(textMessageContent);
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

    public static CompletableFuture<List<MessageContent>> processTextMessage(String content) {
        MessageContent textMessageContent = new MessageContent("user", content.replace("<@1318597210119864385>", ""));
        List<MessageContent> messageList = List.of(textMessageContent);
        return CompletableFuture.completedFuture(messageList);
    }

    public boolean validateArray(List<MessageContent> array) {
        boolean valid = true;
        for (MessageContent item : array) {
            if ("image_base64".equals(item.getType())) {
                if (item.getImageData() == null || item.getContentType() == null) {
                    logger.severe("Invalid Base64 image data: " + item);
                    valid = false;
                }
            } else if ("text".equals(item.getType())) {
                if (item.getText() == null || item.getText().trim().isEmpty()) {
                    logger.severe("Invalid text content: " + item);
                    valid = false;
                }
            }
        }
        return valid;
    }

    public static CompletableFuture<Message> completeSendDM(User user, String content) {
        return user.openPrivateChannel()
            .submit()
            .thenCompose(channel ->
                channel.sendMessage(content)
                    .submit()
                    );
    }

    public static CompletableFuture<Message> completeSendDiscordMessage(Message message, String content, MessageEmbed embed) {
//        return message.getTextChannel()
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

    public static class MessageContent {
        private final String type;
        private final String text;
        private final String imageData; // Only for images
        private final String contentType; // Content type for attachments

        public MessageContent(String type, String content) {
            this.type = type;
            this.text = content;
            this.imageData = null;
            this.contentType = null;
        }
        public MessageContent(String type, String imageData, String contentType) {
            this.type = type;
            this.text = null;
            this.imageData = imageData;
            this.contentType = contentType;
        }
        public String getType() {
            return type;
        }
        public String getText() {
            return text;
        }
        public String getImageData() {
            return imageData;
        }
        public String getContentType() {
            return contentType;
        }
    }
}
