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
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.MinecraftUser;
import com.brandongcobb.vyrtuous.utils.handlers.PlayerMessageQueueManager;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.dataholder.OverloadedWorldHolder;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final Vyrtuous app;
    private GroupManager groupManager;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final PlayerMessageQueueManager chatQueuer;
    volatile boolean continueInteraction;

    public ChatListener(Vyrtuous application, PlayerMessageQueueManager chatQueuer) {
        this.app = ConfigManager.getApp();
        this.chatQueuer = chatQueuer;
    }

    public boolean hasGroupManager() {
        if (groupManager != null) return true;
            final PluginManager pluginManager = app.getServer().getPluginManager();
            final Plugin GMplugin = pluginManager.getPlugin("GroupManager");
            if (GMplugin != null && GMplugin.isEnabled()) {
                groupManager = (GroupManager)GMplugin;
                return true;
            }
            return false;
    }

    public static String removeStringIgnoreCase(String base, String remove) {
        if (base == null || remove == null) {
            return base;
        }

        int index = -1;
        while ((index = base.toLowerCase().indexOf(remove.toLowerCase())) != -1) {
            base = base.substring(0, index) + base.substring(index + remove.length());
        }
        return base;
    }

    public boolean hasPermission(final Player player, final String node) {
        if (!hasGroupManager()) return false;
        final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
        if (handler == null) return false;
        return handler.has(player, node);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String decantedMessage = removeStringIgnoreCase(message, "vyrtuous");
        long playerId = player.getEntityId();
        if (chatQueuer.hasPlayer(playerId)) {
            chatQueuer.enqueueMessage(playerId, decantedMessage);
            return;
        }
        chatQueuer.addPlayer(playerId);
        Bukkit.getScheduler().runTaskAsynchronously(app, () -> {
            continueInteraction = true;
            AIManager.handleConversation(playerId, decantedMessage, null).thenAccept(result -> {
                if (result.getValue()) {
                    event.setCancelled(true);
                    player.sendMessage(result.getKey() + "." + ChatColor.RED + " Nobody saw your message.");
                    continueInteraction = false;
                    return;
                } else if (hasPermission(player, "vyrtuous.openai") && message.toLowerCase().contains("vyrtuous")) {
                    Bukkit.getScheduler().runTask(app, () -> {
                        Bukkit.broadcastMessage(ChatColor.GREEN + "[Vyrtuous]: " + ChatColor.WHITE + result.getKey());
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
                            player.sendMessage(ChatColor.GREEN + "[Vyrtuous] " + ChatColor.WHITE + player.getName() + " stopped the interaction.");
                        });
                        continueInteraction = false;
                        break;
                    }
                    AIManager.handleConversation(playerId, response, null).thenAccept(result -> {
                        if (result.getValue()) {
                            event.setCancelled(true);
                            player.sendMessage(result.getKey() + "." + ChatColor.RED + " Nobody saw your message.");
                            continueInteraction = false;
                            return;
                        } else {
                            Bukkit.getScheduler().runTask(app, () -> {
                                Bukkit.broadcastMessage(ChatColor.GREEN + "[Vyrtuous]: " + ChatColor.WHITE + result.getKey());
                            });
                            return;
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            chatQueuer.removePlayer(playerId);
        });
    }
}
