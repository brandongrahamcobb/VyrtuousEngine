package com.brandongcobb.lucy.commands;

import com.brandongcobb.lucy.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class SortTypeCommand implements CommandExecutor {

    private final Lucy plugin = Lucy.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may use this.");
            return true;
        }
        Player player = (Player) sender;
       if (player.hasMetadata("sort-type")) {
           player.removeMetadata("sort-type", plugin);
           player.sendMessage(color(plugin.getConfig().getString("messages.sort-type-message-name")));
       } else {
           player.setMetadata("sort-type", new FixedMetadataValue(plugin, true));
           player.sendMessage(color(plugin.getConfig().getString("messages.sort-type-message-material")));
       }
       return true;
  }

    private String color(String in) {
        return ChatColor.translateAlternateColorCodes('&', in == null ? "" : in);
    }
}
