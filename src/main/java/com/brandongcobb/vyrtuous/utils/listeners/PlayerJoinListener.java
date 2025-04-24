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
package com.brandongcobb.vyrtuous.utils.listeners;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.handlers.MinecraftUser;
import com.brandongcobb.vyrtuous.utils.handlers.PatreonUser;
import com.brandongcobb.vyrtuous.utils.handlers.UserManager;
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
    private final Vyrtuous app;
    private String minecraftId;
    private PatreonUser discordUser;
    private PatreonUser patreonUser;
    private MinecraftUser minecraftUser;
    private UserManager userManager;
    private Timestamp timestamp;

    public PlayerJoinListener(Vyrtuous application) {
        this.app = application;
        this.minecraftUser = app.minecraftUser;
        this.timestamp = app.timestamp;
        this.userManager = app.userManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.minecraftId = event.getPlayer().getUniqueId().toString();
        minecraftUser.userExists(minecraftId, exists -> {
            if (!exists) {
                minecraftUser.createUser(timestamp, 0L, 0, "", 1, minecraftId, "", 0, "", 0L, "", "", "", "", () -> {
                    Bukkit.getPlayer(UUID.fromString(minecraftId)).sendMessage("Your minecraft user has been registered in the database. Please link your Discord and Patreon with /discord and /patreon.");
                });
            }
        });
    }
}
