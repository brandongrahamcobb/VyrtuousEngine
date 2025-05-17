/*  EventHandlers.java The purpose of this program is to listen to
 *  Minecraft player joins and execute a database create user function to create
 *  a new minecraft user if non-existant.
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
package com.brandongcobb.oauthplugin.listeners;

import com.brandongcobb.oauthplugin.OAuthPlugin;
// Removed unused command imports
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.brandongcobb.oauthplugin.utils.handlers.*;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.Location;
import java.util.Set;

public class EventHandlers implements Listener {

    private String minecraftId;
    private UserManager um;
    private MinecraftUser minecraftUser;
    private Timestamp timestamp;
    private final OAuthPlugin plugin = OAuthPlugin.getInstance();
    private final File dataFile = new File(plugin.getDataFolder(), "data.yml");
    public EventHandlers() {
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        // Check if a user record exists for this Minecraft UUID
        MinecraftUser mUser = new MinecraftUser(uuid);
        mUser.userExists(uuid, exists -> {
            // Initialize UserManager with the plugin's Database instance
            UserManager um = new UserManager(Database.completeGetInstance());
            if (!exists) {
                java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
                // Create a placeholder entry with only Minecraft ID
                um.createUser(now, 0L, uuid, "", 0, "", 0L, "", "", "", "")
                  .thenRun(() -> {
                      um.cacheUser(mUser);
                      player.sendMessage("Your Minecraft user has been registered. Please link Discord and Patreon via /discord and /patreon.");
                  });
            } else {
                // Cache existing user for quick lookup
                um.cacheUser(mUser);
            }
        });
    }

}
