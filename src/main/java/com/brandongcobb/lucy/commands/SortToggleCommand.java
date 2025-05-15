package com.brandongcobb.lucy.commands;

import com.brandongcobb.lucy.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class SortToggleCommand implements CommandExecutor {

    private final Lucy plugin = Lucy.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
         if (!(sender instanceof Player)) {
             sender.sendMessage("Only players may use this.");
             return true;
         }
         Player player = (Player) sender;
         if (args.length == 0) {
             if (player.hasMetadata("commandToggle")) {
                 player.removeMetadata("commandToggle", plugin);
                 player.sendMessage(color(plugin.getConfig().getString("messages.toggle-message-on")));
             } else {
                 player.setMetadata("commandToggle", new FixedMetadataValue(plugin, true));
                 player.sendMessage(color(plugin.getConfig().getString("messages.toggle-message-off")));
             }
             return true;
         }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(color(plugin.getConfig().getString("debug.toggle-player-offline")));
            return true;
        }
        if (target.hasMetadata("commandToggle")) {
            target.removeMetadata("commandToggle", plugin);
            target.sendMessage(color(plugin.getConfig().getString("messages.toggle-message-on")));
        } else {
            target.setMetadata("commandToggle", new FixedMetadataValue(plugin, true));
            target.sendMessage(color(plugin.getConfig().getString("messages.toggle-message-off")));
        }
        player.sendMessage(ChatColor.GRAY + "Toggled for " + target.getName());
        return true;
   }

    private String color(String in) {
        return ChatColor.translateAlternateColorCodes('&', in == null ? "" : in);
    }
}
