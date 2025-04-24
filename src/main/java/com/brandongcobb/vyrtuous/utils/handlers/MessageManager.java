/*  MessageManager.java The purpose of this program is to listen to all messages received and handle them.
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
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Logger;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAttachment;
import org.javacord.api.entity.user.User;

public class MessageManager {

    private Lock lock;
    private Logger logger;
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RESET = "\u001B[0m";

    public MessageManager (Vyrtuous app) {
        Vyrtuous.messageManager = this;
    }

    public CompletableFuture<List<MessageContent>> processArray(String content, List<MessageAttachment> attachments) {
        List<MessageContent> array = new ArrayList<>();
        System.out.println(ANSI_CYAN + content + ANSI_RESET);
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

    public CompletableFuture<List<MessageContent>> processAttachments(List<MessageAttachment> attachments) {
        List<MessageContent> processedAttachments = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        File tempDirectory = new File(System.getProperty("java.io.tmpdir"));
        if (!tempDirectory.exists()) {
            tempDirectory.mkdirs(); // Create directory if it doesn't exist
        }
        for (MessageAttachment attachment : attachments) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                File file = new File(tempDirectory, attachment.getFileName());
                try (InputStream in = attachment.getUrl().openStream()) {
                    Files.copy(in, file.toPath()); // Download the file from the URL
                    String contentType = getContentTypeFromFileName(attachment.getFileName());
                    if (contentType.startsWith("image/")) {
                        byte[] imageBytes = Files.readAllBytes(file.toPath());
                        String imageBase64 = java.util.Base64.getEncoder().encodeToString(imageBytes);
                        processedAttachments.add(new MessageContent("image_url", "data:" + contentType + ";base64," + imageBase64));
                    } else if (contentType.startsWith("text/")) {
                        String textContent = new String(Files.readAllBytes(file.toPath()));
                        processedAttachments.add(new MessageContent("text", textContent));
                    }
                } catch (IOException e) {
                    logger.severe("Error processing file " + attachment.getFileName() + ": " + e.getMessage());
                }
            });
            futures.add(future);
        }
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).thenApply(v -> processedAttachments);
    }

   private String getContentTypeFromFileName(String fileName) {
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

    public CompletableFuture<List<MessageContent>> processTextMessage(String content) {
        return CompletableFuture.completedFuture(List.of(new MessageContent("user", content.replace("<@1318597210119864385>", ""))));
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

    public CompletableFuture<Message> sendDM(User user, String content) {
        return user.openPrivateChannel().thenCompose(channel -> channel.sendMessage(content));
    }

    public CompletableFuture<Message> sendDiscordMessage(Message message, String content, EmbedBuilder embed) {
        return message.getServerTextChannel()
                .map(channel -> channel.sendMessage(content, embed)) // Use .sendMessage with content and embed
                .orElseThrow(() -> new IllegalArgumentException("Message is not in a server text channel."));
    }

    public CompletableFuture<Message> sendDiscordMessage(Message message, String content) {
        return message.getServerTextChannel()
                .map(channel -> channel.sendMessage(content)) // Send message content only
                .orElseThrow(() -> new IllegalArgumentException("Message is not in a server text channel."));
    }

    public CompletableFuture<Message> sendDiscordMessage(Message message, String content, File file) {
        return message.getChannel().sendMessage(content, file);
    }

    private CompletableFuture<Message> sendDiscordMessage(PrivateChannel channel, String content, File file) {
        return channel.sendMessage(content, file);
    }

    public CompletableFuture<Message> sendDiscordMessage(PrivateChannel channel, String content, EmbedBuilder embed) {
        return channel.sendMessage(content, embed); // Sending message to private channel
    }

    public boolean hasAttachments(Message message) {
        return !message.getAttachments().isEmpty();
    }

    public String getMessageContent(Message message) {
        return message.getContent();
    }

    public class MessageContent {
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
