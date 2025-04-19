package com.brandongcobb.patreonplugin.utils.listeners;

import org.bukkit.scheduler.BukkitRunnable; // For creating scheduled tasks
import java.sql.Connection;
import java.sql.SQLException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.brandongcobb.patreonplugin.utils.handlers.UserManager;
import com.brandongcobb.patreonplugin.PatreonPlugin;
import com.brandongcobb.patreonplugin.utils.handlers.PatreonUser;
import java.sql.PreparedStatement;
import java.time.Instant;
import java.sql.Connection; // For database connections

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
        String username = event.getPlayer().getName();
        String minecraftId = event.getPlayer().getUniqueId().toString();
        String createDate = Instant.now().toString();
    
        // Run database operations asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!PatreonUser.userExists(minecraftId)) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            PatreonUser.createUser(createDate, 0L, 0, "", 1, minecraftId, "", 0, "", 0L, "", "", "", "");
                        }
                    }.runTaskAsynchronously(PatreonPlugin.getPlugin(PatreonPlugin.class));
                }
            }
        }.runTaskAsynchronously(PatreonPlugin.getPlugin(PatreonPlugin.class));
    }
}
