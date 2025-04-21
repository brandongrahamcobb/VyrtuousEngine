/*  PlayerJoinListener.java The purpose of this program is to listen to player joins and execute a create user function to create a new minecraft user if non-existant.
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
package com.brandongcobb.patreonplugin.utils.listeners;

import com.brandongcobb.patreonplugin.PatreonPlugin;
import com.brandongcobb.patreonplugin.utils.handlers.PatreonUser;
import com.brandongcobb.patreonplugin.utils.handlers.UserManager;
import org.bukkit.scheduler.BukkitRunnable; // For creating scheduled tasks
import java.sql.Connection;
import java.sql.Connection; // For database connections
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.bukkit.Bukkit; // For Bukkit API
import org.bukkit.entity.Player; // For Player entity
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private String createDate;
    private final PatreonPlugin plugin;
    public PlayerJoinListener(PatreonPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String minecraftId = event.getPlayer().getUniqueId().toString();
        LocalDateTime createDate = LocalDateTime.now();
        Timestamp timestamp = Timestamp.valueOf(createDate);
        PatreonUser.userExists(minecraftId, exists -> {
            if (!exists) {
                PatreonUser.createUser(timestamp, 0L, 0, "", 1, minecraftId, "", 0, "", 0L, "", "", "", "", () -> {
                    Bukkit.getPlayer(UUID.fromString(minecraftId)).sendMessage("You have been registered in the database. Please link your patreon with /patreon.");
                });
            }
        });
    }
}
