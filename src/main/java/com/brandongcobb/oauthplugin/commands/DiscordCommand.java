package com.brandongcobb.oauthplugin.commands;

import com.brandongcobb.oauthplugin.OAuthPlugin;
import com.brandongcobb.oauthplugin.utils.handlers.*;
import com.brandongcobb.oauthplugin.utils.sec.DiscordOAuth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DiscordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        OAuthPlugin plugin = OAuthPlugin.getInstance();
        String minecraftId = p.getUniqueId().toString();
        try {
            // Begin a new OAuth session for this player
            String uuid = p.getUniqueId().toString();
            OAuthUserSession session = new OAuthUserSession(uuid, "discord");
            plugin.getSessions().put(uuid, session);
            // Build state and authorization URL
            String state = URLEncoder.encode(uuid, StandardCharsets.UTF_8);
            new DiscordOAuth()
                .completeGetAuthorizationUrl()
                .thenAccept(url ->
                    p.sendMessage("Please visit to authorize: " + url + "&state=" + state)
                );
            // Schedule a timeout to cancel the session
            BukkitRunnable timeout = new BukkitRunnable() {
                @Override public void run() {
                    plugin.getSessions().remove(uuid);
                    p.sendMessage("Discord auth timed out.");
                }
            };
            session.setTimeoutTask(timeout);
            timeout.runTaskLater(plugin, 20 * 60 * 10);
        } catch (Exception ex) {
            plugin.getLogger().warning("Discord OAuth error: " + ex.getMessage());
            ex.printStackTrace();
        }
        return true;
    }
}
