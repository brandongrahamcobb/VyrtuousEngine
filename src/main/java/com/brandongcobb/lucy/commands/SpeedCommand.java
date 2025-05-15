package com.brandongcobb.lucy.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpeedCommand implements CommandExecutor {

    // Holds each player's last set speeds
    public static final Map<UUID, PlayerSpeedData> speedMap = new ConcurrentHashMap<>();

    // Simple holder for walk/fly speeds
    public static class PlayerSpeedData {
        public float walkSpeed = 0.2f;
        public float flySpeed  = 0.1f;
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        Player player = (Player) sender;

        // Permission check (change permission node as needed)
        if (!player.hasPermission("lucy.speed")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to change your speed.");
            return true;
        }

        // /speed <value>
        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Usage: /speed <0.0–1.0>");
            return false;
        }

        // Parse
        float speed;
        try {
            speed = Float.parseFloat(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid number format.");
            return true;
        }

        // Range check
        if (speed < 0.0f || speed > 1.0f) {
            player.sendMessage(ChatColor.RED + "Speed must be between 0.0 and 1.0.");
            return true;
        }

        // Fetch or create this player's data
        UUID uuid = player.getUniqueId();
        PlayerSpeedData data = speedMap.computeIfAbsent(uuid, k -> new PlayerSpeedData());

        // Apply based on whether they're currently flying
        if (player.isFlying()) {
            data.flySpeed = speed;
            player.setFlySpeed(speed);
            player.sendMessage(ChatColor.GREEN + "Fly speed set to " + speed);
        } else {
            data.walkSpeed = speed;
            player.setWalkSpeed(speed);
            player.sendMessage(ChatColor.GREEN + "Walk speed set to " + speed);
        }

        return true;
    }
}
