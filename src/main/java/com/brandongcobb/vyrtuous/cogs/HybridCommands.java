/*  HybridCommands.java The purpose of this class is to be a cog
 *  with both slash and text commands on Discord
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
import com.brandongcobb.vyrtuous.utils.inc.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import java.util.concurrent.CompletionException;

public class HybridCommands extends ListenerAdapter implements Cog {

    private JDA api;
    private ConfigManager cm;
    private DiscordBot bot;

    @Override
    public void register (JDA api, DiscordBot bot, ConfigManager cm) {
        this.api = api;
        this.bot = bot.completeGetBot();
        api.addEventListener(this);
        this.cm = cm.completeGetInstance();
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        cm.completeGetConfigValue("discord_command_prefix", String.class)
          .thenAcceptAsync(prefix -> {
            String prefixObj = (String) prefix;
            String messageContent = event.getMessage().getContentRaw().trim();
            if (!messageContent.startsWith(prefixObj)) return;
            String[] args = messageContent.substring(prefixObj.length()).split("\\s+");
            if (args.length == 0) return;
            String command = args[0].toLowerCase();
            User sender = event.getAuthor();
            if (command.equals("openai")) {
                if (args.length > 2 && "model".equalsIgnoreCase(args[1])) {
                    cm.completeGetUserModelSettings().thenCompose(userModelSettings -> {
                        Map<Long, String> userModelObject = (Map<Long, String>) userModelSettings;
                        TextChannel channel = (TextChannel) event.getChannel();
                        String arg = args[2].toLowerCase();
                        if (Helpers.containsString(ModelRegistry.OPENAI_RESPONSE_MODELS, arg)) {
                            userModelObject.put(sender.getIdLong(), arg);
                            cm.completeSetUserModelSettings(userModelObject);
                            channel.sendMessage("OpenAI model:" + arg + " for " + sender.getName()).queue();
                        } else {
                            String[] options = ModelRegistry.OPENAI_RESPONSE_MODELS;
                            StringBuilder sb = new StringBuilder("[");
                            for (int i = 0; i < options.length; i++) {
                                sb.append(options[i]);
                                if (i < options.length - 1) sb.append(", ");
                            }
                            sb.append("]");
                            channel.sendMessage("Your options are " + sb.toString()).queue();
                        }
                        return null;
                    });
                }
            } else if (command.equals("wipe")) {
                boolean wipeAll = false, wipeBot = false, wipeCommands = false;
                String targetUserId = null;
                for (int i = 1; i < args.length; i++) {
                    String arg = args[i].toLowerCase();
                    switch (arg) {
                        case "all" -> wipeAll = true;
                        case "bot" -> wipeBot = true;
                        case "commands" -> wipeCommands = true;
                        case "user" -> {
                            if (i + 1 < args.length) {
                                String userMention = args[i + 1];
                                User user = parseUserFromMention(userMention, event.getGuild());
                                if (user != null) targetUserId = user.getId();
                                i++;
                            }
                        }
                    }
                }
                TextChannel channel = (TextChannel) event.getChannel();
                String guildId = event.getGuild().getId();
                String channelId = channel.getId();
                wipeMessages(guildId, channelId, wipeAll, wipeBot, wipeCommands, targetUserId)
                  .thenRun(() -> channel.sendMessage("Message wipe completed.").queue())
                  .exceptionally(ex -> { return null; });
            }
          })
          .exceptionally(ex -> { return null; });
    }

    private User parseUserFromMention(String mention, Guild guild) {
        if (mention.startsWith("<@") && mention.endsWith(">")) {
            String id = mention.replaceAll("[<@!>]", "");
            return this.api.getUserById(id);
        }
        return null;
    }

    public CompletableFuture<Void> wipeMessages(
            String guildId,
            String channelId,
            boolean wipeAll,
            boolean wipeBot,
            boolean wipeCommands,
            String userId
    ) {
        return cm.completeGetConfigValue("discord_command_prefix", String.class).thenCompose(prefix -> {
            String prefixObj = (String) prefix;
            Guild guild = this.api.getGuildById(guildId);
            if (guild == null) {
                return CompletableFuture.failedFuture(new Exception("Guild not found"));
            }
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel == null) {
                return CompletableFuture.failedFuture(new Exception("Channel not found"));
            }
            MessageHistory history = channel.getHistory();
            return fetchAllMessages(history).thenCompose(messages -> {
                List<Message> toDelete = messages.stream()
                        .filter(msg -> {
                            if (userId != null && !msg.getAuthor().getId().equals(userId))
                                return false;
                            if (wipeBot && !msg.getAuthor().isBot())
                                return false;
                            if (wipeCommands) {
                                String content = msg.getContentDisplay();
                                if (content.startsWith(prefixObj)) return true;
                                return false;
                            }
                            if (wipeAll) return true;
                            return false;
                        })
                        .collect(Collectors.toList());
                return deleteMessagesInChunks(toDelete, channel);
            });
        });
    }

    private CompletableFuture<List<Message>> fetchAllMessages(MessageHistory history) {
        CompletableFuture<List<Message>> future = new CompletableFuture<>();
        history.retrievePast(100).queue(messages -> {
            List<Message> allMessages = messages;
            fetchRemainingMessages(history, allMessages, future);
        }, failure -> future.completeExceptionally(failure));
        return future;
    }

    private void fetchRemainingMessages(MessageHistory history, List<Message> accumulated, CompletableFuture<List<Message>> future) {
        if (history.getRetrievedHistory().size() == 0) {
            future.complete(accumulated);
        } else {
            history.retrievePast(100).queue(messages -> {
                if (messages.isEmpty()) {
                    future.complete(accumulated);
                } else {
                    accumulated.addAll(messages);
                    fetchRemainingMessages(history, accumulated, future);
                }
            }, failure -> future.completeExceptionally(failure));
        }
    }

    private CompletableFuture<Void> deleteMessagesInChunks(List<Message> messages, TextChannel channel) {
        if (messages.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        List<Message> chunk = messages.stream().limit(100).collect(Collectors.toList());
        List<String> messageIds = chunk.stream().map(Message::getId).collect(Collectors.toList());
        CompletableFuture<Void> deletionFuture = new CompletableFuture<>();
        channel.deleteMessagesByIds(messageIds).queue(
            success -> deletionFuture.complete(null),
            failure -> deletionFuture.completeExceptionally(failure)
        );
        return deletionFuture.thenCompose(v -> {
            List<Message> remaining = messages.stream().skip(100).collect(Collectors.toList());
            return deleteMessagesInChunks(remaining, channel);
        });
    }
}
