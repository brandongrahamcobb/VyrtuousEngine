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

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.Vyrtuous;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.util.Optional;
import java.util.Set;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.channel.PrivateChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

public class ModerationManager {

    private Vyrtuous app;
    private ConfigManager configManager;
    private HikariDataSource dbPool;
    private DiscordBot discordBot;
    private Lock lock;
    private Logger logger;
    private MessageManager messageManager;

    public ModerationManager(Vyrtuous application) {
        Vyrtuous.moderationManager = this;
        this.app = application;
        this.configManager = app.configManager;
        this.dbPool = app.dbPool;
        this.discordBot = app.discordBot;
        this.logger = app.logger;
    }

    public void handleModeration(Message message, String reasonStr) {
        User author = message.getAuthor().asUser().orElse(null);
        Server server = message.getServer().orElse(null);
        if (server == null || author == null) {
            return;
        }
        if (author.getRoles(server).stream().anyMatch(role -> role.getName().equals(configManager.getConfigValue("discord_role_pass")))) {
            return;
        }
        long userId = author.getId();
        try (Connection connection = dbPool.getConnection()) {
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT flagged_count FROM moderation_counts WHERE user_id = ?");
            statement.setLong(1, userId);
            ResultSet row = statement.executeQuery();
            int flaggedCount;
            if (row.next()) {
                flaggedCount = row.getInt("flagged_count") + 1;
                PreparedStatement updateStatement = connection.prepareStatement(
                        "UPDATE moderation_counts SET flagged_count = ? WHERE user_id = ?");
                updateStatement.setInt(1, flaggedCount);
                updateStatement.setLong(2, userId);
                updateStatement.executeUpdate();
                logger.info("Updated flagged count for user " + userId + ": " + flaggedCount);
            } else {
                flaggedCount = 1;
                PreparedStatement insertStatement = connection.prepareStatement(
                        "INSERT INTO moderation_counts (user_id, flagged_count) VALUES (?, ?)");
                insertStatement.setLong(1, userId);
                insertStatement.setInt(2, flaggedCount);
                insertStatement.executeUpdate();
                logger.info("Inserted new flagged count for user " + userId + ": " + flaggedCount);
            }
            connection.commit(); // Commit transaction
            message.delete();
            String moderationWarning = (String) configManager.getConfigValue("discord_moderation_warning");
            if (flaggedCount == 1) {
                messageManager.sendDiscordMessage(message, moderationWarning + ". Your message was flagged for: " + reasonStr);
            } else if (flaggedCount >= 2 && flaggedCount <= 4) {
                if (flaggedCount == 4) {
                    messageManager.sendDiscordMessage(message, moderationWarning + ". Your message was flagged for: " + reasonStr);
                }
            } else if (flaggedCount >= 5) {
                messageManager.sendDiscordMessage(message, moderationWarning + ". Your message was flagged for: " + reasonStr);
                messageManager.sendDiscordMessage(message, "You have been timed out for 5 minutes due to repeated violations.");
                author.timeout(server, Duration.ofSeconds(300), reasonStr);
                PreparedStatement resetStatement = connection.prepareStatement(
                        "UPDATE moderation_counts SET flagged_count = 0 WHERE user_id = ?");
                resetStatement.setLong(1, userId);
                resetStatement.executeUpdate();
            }
        } catch (SQLException e) {
            logger.severe("Error processing moderation for user " + userId + ": " + e.getMessage());
        }
    }

    public Server getServerById(long serverId) {
        Set<Server> servers = discordBot.getApi().getServers();
        for (Server server : servers) {
            if (server.getId() == serverId) {
                return server;
            }
        }
        return null;
    }
}
