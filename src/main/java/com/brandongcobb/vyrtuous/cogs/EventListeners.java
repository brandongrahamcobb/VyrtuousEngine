/*  EventListeners.java The purpose of this program is to listen for any of the program's endpoints and handles them.
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
package com.brandongcobb.vyrtuous.cogs;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.utils.handlers.ModerationManager;
import com.brandongcobb.vyrtuous.utils.handlers.PatreonUser;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import com.zaxxer.hikari.HikariDataSource;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;
import java.util.List;
import javax.annotation.Nonnull;
//import org.javacord.api.DiscordApi;
//import org.javacord.api.DiscordApiBuilder;
//import org.javacord.api.entity.message.Message;
//import org.javacord.api.entity.message.MessageAttachment;
//import org.javacord.api.entity.channel.PrivateChannel;
//import org.javacord.api.entity.user.User;
//import org.javacord.api.event.message.MessageCreateEvent;
//import org.javacord.api.listener.message.MessageCreateListener;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.Guild;
//import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EventListeners extends ListenerAdapter implements Cog {

    private final Vyrtuous app;
//    private DiscordApi api;
    private JDA api;
    private Lock lock;
    private long senderId;

    public EventListeners (Vyrtuous application) {
        this.app = application;
        this.lock = app.lock;
    }

    @Override
//    public void register (DiscordApi api) {
    public void register (JDA api) {
//        api.addMessageCreateListener(new MessageCreateListener() {
        api.addEventListener(this);
    }

//    MessageCreateListener(new MessageCreateListener() {
    @Override
  //          public void onMessageCreate(MessageCreateEvent event) {
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
       Message message = event.getMessage();
//                if (message.getAuthor().isBotUser()) {
       if (message.getAuthor().isBot()) {
           return;
       }
//                boolean isMentioned = message.getMentionedUsers().contains(api.getYourself());
       boolean isMentioned = message.getMentions().getUsers().contains(api.getSelfUser());
//                String content = event.getMessageContent();
       String content = message.getContentDisplay();
//                User sender = message.getAuthor().asUser().orElse(null);
       User sender = event.getAuthor();
//       senderId = sender.getId();
       senderId = sender.getIdLong();
//                List<MessageAttachment> attachments = message.getAttachments();
       List<Message.Attachment> attachments = message.getAttachments();
       if (!Predicator.isDeveloper(sender)) {
            AIManager.handleConversation(senderId, content, attachments).thenAccept(result -> {
                boolean handled = false;
                if ("chat".equals(content.substring(1)) || isMentioned) {
                    handled = true;
                    if (result.getValue()) {
                        ModerationManager.handleModeration(message, result.getKey());
                    } else {
                        MessageManager.sendDiscordMessage(message, result.getKey());
                    }
                }
                if (result.getValue() && !handled) {
                    ModerationManager.handleModeration(message, result.getKey());
                }
            })
            .join();
        }
    }
}
