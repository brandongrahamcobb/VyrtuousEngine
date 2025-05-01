/*  ModerationManager.java The purpose of this program is to handle all explicit
 *  strings from the program's endpoints.
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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.logging.Logger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class ModerationManager {

    private static Vyrtuous app;
    private static final Object fileLock = new Object();
    private static Map<Long, Integer> userCounts;
    private static Lock lock;
    private static Logger logger;
    private static File temporaryFile;

    static {
        temporaryFile = new File(app.tempDirectory, "config.yml");
    }

    public ModerationManager(Vyrtuous application) {
        this.app = application;
        this.logger = app.logger;
    }

    public static CompletableFuture<Void> completeHandleModeration(Message message, String reasonStr) {
        User author = message.getAuthor();
        Guild guild = message.getGuild();
        if (guild == null || author == null) {
            return CompletableFuture.completedFuture(null);
        }
        Member member = guild.getMember(author);
        if (member == null) {
            return CompletableFuture.completedFuture(null);
        }
        return ConfigManager.completeGetConfigStringValue("discord_role_pass").thenCompose(passRoleId -> {
            boolean hasPassRole = member.getRoles().stream()
                .anyMatch(role -> role.getId().equals(passRoleId));
            if (hasPassRole) {
                return CompletableFuture.completedFuture(null);
            }
            long userId = author.getIdLong();
            return CompletableFuture.supplyAsync(() -> {
                Map<Long, Integer> userCounts = new HashMap<>();
                synchronized (fileLock) {
                    if (temporaryFile.exists()) {
                        try (BufferedReader reader = new BufferedReader(new FileReader(temporaryFile))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                String[] parts = line.split(":");
                                if (parts.length == 2) {
                                    try {
                                        long id = Long.parseLong(parts[0]);
                                        int count = Integer.parseInt(parts[1]);
                                        userCounts.put(id, count);
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        } catch (IOException e) {
                            logger.severe("Failed to read temporaryFile: " + e.getMessage());
                            return null;
                        }
                    }
                }
                int flaggedCount = userCounts.getOrDefault(userId, 0) + 1;
                userCounts.put(userId, flaggedCount);
                synchronized (fileLock) {
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(temporaryFile, false))) {
                        for (Map.Entry<Long, Integer> entry : userCounts.entrySet()) {
                            writer.write(entry.getKey() + ":" + entry.getValue());
                            writer.newLine();
                        }
                    } catch (IOException e) {
                        logger.severe("Failed to write to temporaryFile: " + e.getMessage());
                        return null;
                    }
                }
                return flaggedCount;
            }).thenCompose(flaggedCount -> {
                if (flaggedCount == null) return CompletableFuture.completedFuture(null);
                message.delete().queue();
                return ConfigManager.completeGetConfigStringValue("discord_moderation_warning").thenCompose(warning -> {
                    String warningMsg = warning + ". Your message was flagged for: " + reasonStr;
                    CompletableFuture<Void> action = CompletableFuture.completedFuture(null);
                    if (flaggedCount == 1) {
                        action = MessageManager.completeSendDiscordMessage(message, warningMsg).thenApply(msg -> null);
                    } else if (flaggedCount >= 2 && flaggedCount <= 4) {
                        if (flaggedCount == 4) {
                            action = MessageManager.completeSendDiscordMessage(message, warningMsg).thenApply(msg -> null);
                        }
                    } else if (flaggedCount >= 5) {
                        action = MessageManager.completeSendDiscordMessage(message, warningMsg)
                            .thenCompose(msg -> MessageManager.completeSendDiscordMessage(message,
                                    "You have been timed out for 5 minutes due to repeated violations."))
                            .thenRun(() -> member.timeoutFor(Duration.ofSeconds(300)).queue())
                            .thenRunAsync(() -> {
                                userCounts.put(userId, 0);
                                synchronized (fileLock) {
                                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(temporaryFile, false))) {
                                        for (Map.Entry<Long, Integer> entry : userCounts.entrySet()) {
                                            writer.write(entry.getKey() + ":" + entry.getValue());
                                            writer.newLine();
                                        }
                                    } catch (IOException e) {
                                        logger.severe("Failed to reset counts in temporaryFile: " + e.getMessage());
                                    }
                                }
                            });
                    }
                    return action;
                });
            });
        });
    }
}
