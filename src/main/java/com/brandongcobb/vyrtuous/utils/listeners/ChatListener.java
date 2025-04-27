/*  ChatListenerjava
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
package com.brandongcobb.vyrtuous.utils.listeners;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.AIManager;
import com.brandongcobb.vyrtuous.utils.handlers.MinecraftUser;
import com.brandongcobb.vyrtuous.utils.handlers.PlayerMessageQueueManager;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final Vyrtuous app;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final PlayerMessageQueueManager chatQueuer;

    public ChatListener(Vyrtuous application, PlayerMessageQueueManager chatQueuer) {
        this.app = application;
        this.chatQueuer = chatQueuer;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        long playerId = player.getEntityId();
        if (message.contains("Vyrtuous")) {
            chatQueuer.addPlayer(playerId);
            Bukkit.getScheduler().runTaskAsynchronously(app, () -> {
                handleVyrtuousInteraction(player, message, playerId);
            });
        }
    }

    private void handleVyrtuousInteraction(Player player, String message, long playerId) {
        boolean continueInteraction = true;
        AIManager.handleConversation(playerId, message, null).thenAccept(result -> {
            if (result.getValue()) {
                return;
            } else {
                Bukkit.getScheduler().runTask(app, () -> {
                    Bukkit.broadcastMessage("[Vyrtuous]: " + result.getKey());
                });
            }
        });
        while (continueInteraction) {
            try {
                String response = chatQueuer.dequeueMessage(playerId, 30, TimeUnit.SECONDS);
                if (response == null) {
                    break;
                }
                if (response.equalsIgnoreCase("STOP")) {
                    Bukkit.getScheduler().runTask(app, () -> {
                        player.sendMessage("[Vyrtuous] " + player.getName() + " stopped the interaction.");
                    });
                    break;
                }
                AIManager.handleConversation(playerId, message, null).thenAccept(result -> {
                    if (result.getValue()) {
                        return;
                    } else {
                        Bukkit.getScheduler().runTask(app, () -> {
                            Bukkit.broadcastMessage("[Vyrtuous]: " + result.getKey());
                        });
                   }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        chatQueuer.removePlayer(playerId);
    }
}
