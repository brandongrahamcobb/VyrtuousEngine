package com.brandongcobb.lucy.commands;

import com.brandongcobb.lucy.listeners.*;
import com.brandongcobb.lucy.*;
import java.io.File;
import java.io.IOException;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class SortSecureCommand implements CommandExecutor {

    private final Lucy plugin = Lucy.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players may use this.");
            return true;
        }
        Player player = (Player) sender;
        Block b = player.getTargetBlock((java.util.Set<Material>)null, 5);
        if (!(b.getState() instanceof Chest)) {
            player.sendMessage(color(plugin.getConfig().getString("messages.secure-message-error")));
            return true;
        }
        String key = EventHandlers.locationKey(b.getLocation());
        File dataFile = new File(plugin.getDataFolder(), "data.yml");
        YamlConfiguration data = new YamlConfiguration();
        try {
            if (!dataFile.exists()) dataFile.createNewFile();
                data.load(dataFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "§cError loading secure data.");
            return true;
        }
        if (data.contains(key)) {
            player.sendMessage(color(plugin.getConfig().getString("messages.secure-message-duplicate")));
        } else {
            data.set(key, player.getUniqueId().toString());
            try {
                data.save(dataFile);
            } catch (IOException e) {
                e.printStackTrace();
                player.sendMessage(ChatColor.RED + "§cError saving secure data.");
                return true;
            }
            player.sendMessage(color(plugin.getConfig().getString("messages.secure-message")));
        }
        return true;
    }

    private String color(String in) {
        return ChatColor.translateAlternateColorCodes('&', in == null ? "" : in);
    }
}
