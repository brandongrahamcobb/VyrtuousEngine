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
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.Predicator;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import java.util.concurrent.CompletionException;

public class HybridCommands extends ListenerAdapter implements Cog {

    private ConfigManager cm;

    public HybridCommands (ConfigManager cm) {
        this.cm = cm.completeGetInstance();
    }

    @Override
    public void register (JDA api) {
        api.addEventListener(this);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        String content = event.getMessage().getContentRaw().trim();
        User sender = event.getAuthor();
        Predicator predicator = new Predicator(cm, event.getJDA());
        predicator.isDeveloper(sender).thenAcceptAsync(isDev -> {
            if (isDev && content.equalsIgnoreCase(".config")) {
                event.getChannel().sendMessage("Reloading configuration...").queue();
        
                cm.completeSetAndLoadConfig()
                    .thenRun(() -> {
                        event.getChannel().sendMessage("Configuration reloaded successfully.").queue();
                    })
                    .exceptionally(ex -> {
                        event.getChannel().sendMessage("Failed to reload configuration: ").queue();
                        return null; // still works — type inferred from thenRun (Void)
                    });
            }
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return null; // again, inferred to (Void)
        });
    }
}
