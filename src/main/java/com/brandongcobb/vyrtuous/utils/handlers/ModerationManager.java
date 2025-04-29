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
//import org.javacord.api.DiscordApi;
//import org.javacord.api.entity.message.Message;
//import org.javacord.api.entity.channel.PrivateChannel;
//import org.javacord.api.entity.server.Server;
//import org.javacord.api.entity.user.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

public class ModerationManager {

    private static Vyrtuous app;
    private static final Object fileLock = new Object();
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

    public static void handleModeration(Message message, String reasonStr) {
//        User author = message.getAuthor().asUser().orElse(null);
        User author = message.getAuthor();
//        Server server = message.getServer().orElse(null);
        Guild guild = message.getGuild();
//        if (server == null || author == null) {
        Member member = guild.getMember(author);
        if (guild == null || author == null || member == null) {
            return;
        }
//        if (author.getRoles(server).stream().anyMatch(role -> role.getName().equals(ConfigManager.getConfigValue("discord_role_pass")))) {
        if (member.getRoles().stream().anyMatch(role ->
            role.getId().equals(ConfigManager.getConfigValue("discord_role_pass"))
            )
        ) {
            return;
        }
//        long userId = author.getId();
        long userId = author.getIdLong();
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
                            } catch (NumberFormatException e) {
                                // ignore malformed lines
                            }
                        }
                    }
                } catch (IOException e) {
                    logger.severe("Failed to read temporaryFile: " + e.getMessage());
                    return;
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
                return;
            }
        }
        message.delete();
        String moderationWarning = (String) ConfigManager.getConfigValue("discord_moderation_warning");
        if (flaggedCount == 1) {
            MessageManager.sendDiscordMessage(message, moderationWarning + ". Your message was flagged for: " + reasonStr);
        } else if (flaggedCount >= 2 && flaggedCount <= 4) {
            if (flaggedCount == 4) {
                MessageManager.sendDiscordMessage(message, moderationWarning + ". Your message was flagged for: " + reasonStr);
            }
        } else if (flaggedCount >= 5) {
            MessageManager.sendDiscordMessage(message, moderationWarning + ". Your message was flagged for: " + reasonStr);
            MessageManager.sendDiscordMessage(message, "You have been timed out for 5 minutes due to repeated violations.");
//            author.timeout(server, Duration.ofSeconds(300), reasonStr);
            member.timeoutFor(Duration.ofSeconds(300));
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
        }
    }
}
