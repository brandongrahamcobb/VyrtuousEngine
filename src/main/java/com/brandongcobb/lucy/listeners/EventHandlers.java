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
package com.brandongcobb.lucy.listeners;

import com.brandongcobb.lucy.Lucy;
import com.brandongcobb.lucy.commands.*;
import com.brandongcobb.lucy.commands.SpeedCommand.PlayerSpeedData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.brandongcobb.lucy.utils.handlers.*;
import java.sql.Timestamp;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import java.util.Set;

public class EventHandlers implements Listener {

    private String minecraftId;
    private MinecraftUser minecraftUser;
    private Timestamp timestamp;
    private final Lucy plugin = Lucy.getInstance();

    public EventHandlers() {
        this.timestamp = new Timestamp(System.currentTimeMillis());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player currentPlayer = event.getPlayer();
        String minecraftId = currentPlayer.getUniqueId().toString();
        MinecraftUser minecraftUser = new MinecraftUser(minecraftId);
        minecraftUser.userExists(minecraftId, exists -> {
            if (!exists) {
                minecraftUser.createUser(timestamp, 0L, 0, "", 1, minecraftId, "", 0, "", 0L, "", "", "", "");
                Bukkit.getPlayer(UUID.fromString(minecraftId)).sendMessage("Your minecraft user has been registered in the database. Please link your Discord and Patreon with /discord and /patreon.");
            }
        });
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerSpeedData data = SpeedCommand.speedMap.get(uuid);
        if (data != null) {
            player.setFlySpeed(data.flySpeed);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PlayerSpeedData data = SpeedCommand.speedMap.get(uuid);
        if (data != null && !player.isFlying()) {
            player.setWalkSpeed(data.walkSpeed);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (e.getClickedBlock() == null) return;

        if (!(e.getClickedBlock().getState() instanceof Chest)) return;
        e.setCancelled(true);
        e.getPlayer().performCommand("sort internalChestSort");
    }
  
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getClick() != ClickType.MIDDLE) return;
        Player p = (Player)e.getWhoClicked();
        InventoryView view = p.getOpenInventory();
        // the “type” here is the VIEW type, not block type:
        if (view.getType() == InventoryType.CHEST) {
            p.performCommand("sort internalChestSort");
        } else {
            p.performCommand("sort internalPlayerSort");
        }
        e.setCancelled(true);
    }
  
    /** used by CommandSortSecure to build the map key */
    public static String locationKey(org.bukkit.Location l) {
        return l.getWorld().getName()
               + "~" + l.getBlockX()
               + "~" + l.getBlockY()
               + "~" + l.getBlockZ();
        }

}
