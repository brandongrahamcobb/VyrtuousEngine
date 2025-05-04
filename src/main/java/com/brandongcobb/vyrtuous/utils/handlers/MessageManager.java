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
import java.util.Locale;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.nio.file.Files;

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

    public static CompletableFuture<List<String>> completeProcessAttachments(List<Attachment> attachments) {
        logger.info("Attachments found: " + attachments.size());

        List<String> results = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Attachment attachment : attachments) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    String url = attachment.getUrl(); // Get the URL of the attachment
                    String fileName = attachment.getFileName(); // Get the file name
                    String contentType = attachment.getContentType(); // Get the content type (MIME type)
                    logger.info("Downloading: " + url + " as " + contentType);

                    // Download the attachment and save to temporary file
                    File tempFile = new File(tempDirectory, fileName);
                    try (InputStream in = new URL(url).openStream()) {
                        Files.copy(in, tempFile.toPath());
                        logger.info("Downloaded " + tempFile.length() + " bytes from " + url);

                        if (contentType.startsWith("image/")) {
                            // Process image as Base64
                            String base64Image = encodeImage(Files.readAllBytes(tempFile.toPath()));
                            results.add("data:" + contentType + ";base64," + base64Image);
                        } else if (contentType.startsWith("text/")) {
                            // Process text files
                            String textContent = new String(Files.readAllBytes(tempFile.toPath()), StandardCharsets.UTF_8);
                            results.add(textContent);
                        } else {
                            // Skip non-text and non-image files
                            results.add("Skipped non-text/non-image attachment: " + fileName);
                        }
                    } catch (IOException e) {
                        logger.severe("Error downloading or reading attachment: " + e.getMessage());
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    logger.severe("Error in attachment thread: " + e.getMessage());
                    e.printStackTrace();
                }
            });
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                logger.info("Attachment processing completed.");
                return results;
            }).exceptionally(ex -> {
                logger.severe("Attachment processing failed: " + ex.getMessage());
                ex.printStackTrace();
                return List.of("Error occurred");
            });
    }

    private static String encodeImage(byte[] imageBytes) {
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    private static String getContentTypeFromFileName(String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);

        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".gif")) return "image/gif";
        if (lowerName.endsWith(".bmp")) return "image/bmp";
        if (lowerName.endsWith(".webp")) return "image/webp";
        if (lowerName.endsWith(".svg")) return "image/svg+xml";

        if (lowerName.endsWith(".txt")) return "text/plain";
        if (lowerName.endsWith(".md")) return "text/markdown";
        if (lowerName.endsWith(".csv")) return "text/csv";
        if (lowerName.endsWith(".json")) return "application/json";
        if (lowerName.endsWith(".xml")) return "application/xml";
        if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) return "text/html";

        return "application/octet-stream"; // fallback
    }

    public static CompletableFuture<Void> completeSendResponse(Message message, String response) {
        // Check if response is wrapped by triple backticks with an explicit file type
        // Example: ```java\n<content>\n```
        if (response.startsWith("```")) {
            // Use a regex to capture the file type and content.
            // The pattern expects a header (file type) followed by a newline,
            // then the content, and ending with triple-backticks.
            Pattern codeBlockPattern = Pattern.compile("^```(\\w+)\\s+([\\s\\S]+)```$");
            Matcher matcher = codeBlockPattern.matcher(response);
            if (matcher.find()) {
                String fileType = matcher.group(1);   // e.g., "java" or "txt"
                String fileContent = matcher.group(2);
                // Build a file name. Here we use "response.<fileType>".
                File file = new File(tempDirectory, "response." + fileType);
                try {
                    Files.writeString(file.toPath(), fileContent, StandardCharsets.UTF_8);
                } catch (IOException e) {
                    e.printStackTrace();
                    // Fallback: send as normal text message if file write fails.
                    return completeSendDiscordMessage(message, "Error writing file: " + e.getMessage())
                        .thenApply(m -> null);
                }
                // You may choose to send a header message before (or along with) the file.
                return completeSendDiscordMessage(message, "Response is attached as a file:")
                        .thenCompose(sentMessage -> completeSendDiscordMessage(message, "", file)
                        .thenApply(m -> null));
            }
        }
    
        // If the response is longer than Discord's 2000 character limit
        if (response.length() > 2000) {
            // Write the full response into a .txt file.
            File file = new File(tempDirectory, "response.txt");
            try {
                Files.writeString(file.toPath(), response, StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
                return completeSendDiscordMessage(message, "Error writing long response file: " + e.getMessage())
                        .thenApply(m -> null);
            }
    
            // Send a short message and attach the file
            return completeSendDiscordMessage(message, "Response was too long; see attached file:", file)
                    .thenApply(m -> null);
        }
    
        // Otherwise, the response is short enough to be sent normally
        return completeSendDiscordMessage(message, response)
                .thenApply(m -> null);
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
}
