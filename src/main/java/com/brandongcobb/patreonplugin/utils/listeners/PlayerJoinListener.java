package com.brandongcobb.patreonplugin.utils.listeners;

import java.time.format.DateTimeFormatter;
import org.bukkit.scheduler.BukkitRunnable; // For creating scheduled tasks
import java.sql.Connection;
import java.sql.SQLException;
import org.bukkit.Bukkit; // For Bukkit API
import org.bukkit.entity.Player; // For Player entity
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.brandongcobb.patreonplugin.utils.handlers.UserManager;
import com.brandongcobb.patreonplugin.PatreonPlugin;
import com.brandongcobb.patreonplugin.utils.handlers.PatreonUser;
import java.sql.PreparedStatement;

import java.time.LocalDateTime;
import java.time.Instant;
import java.sql.Connection; // For database connections
import java.sql.Timestamp;

public class PlayerJoinListener implements Listener {


    private String createDate;
    private final PatreonPlugin plugin;
    public PlayerJoinListener(PatreonPlugin plugin) {
        this.plugin = plugin;
    }//    private final PatreonPlugin plugin;
//
//    public PlayerJoinListener(PatreonPlugin plugin) {
//        this.plugin = plugin;
//        this.userManager = new UserManager(plugin);
//    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String minecraftId = event.getPlayer().getUniqueId().toString();
        LocalDateTime createDate = LocalDateTime.now();
        Timestamp timestamp = Timestamp.valueOf(createDate);
    
        // Run database operations asynchronously
        PatreonUser.userExists(minecraftId, exists -> {
            if (!exists) {
                PatreonUser.createUser(timestamp, 0L, 0, "", 1, minecraftId, "", 0, "", 0L, "", "", "", "", () -> {
                    Bukkit.getPlayer(UUID.fromString(minecraftId)).sendMessage("You have been registered in the database. Please link your patreon with /patreon.");
                });
            } else {
                // Handle the case where the user already exists
                Player player = Bukkit.getPlayer(UUID.fromString(minecraftId));
                if (player != null) {
                    player.sendMessage("Welcome back!");
                }
            }
        });
    }
}
