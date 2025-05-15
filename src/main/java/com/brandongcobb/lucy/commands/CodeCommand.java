package com.brandongcobb.lucy.commands;

import com.brandongcobb.lucy.Lucy;
import com.brandongcobb.lucy.utils.handlers.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CodeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
          sender.sendMessage("Only players may run this command.");
          return true;
        }
        Player player = (Player) sender;

        if (args.length < 1) {
          player.sendMessage("Please provide an access code after /code.");
          return false;
        }

        Lucy plugin = Lucy.getInstance();
        MinecraftUser mu = new MinecraftUser(player.getUniqueId().toString());
        OAuthUserSession session = plugin.getSessions().get(mu);

        if (session != null && session.getAccessToken() != null) {
            if (args[0].equals(session.getAccessToken())) {
                plugin.getSessions().remove(mu);
                BukkitRunnable cb = plugin.getCallbackRunnable();
                if (cb != null) {
                    cb.cancel();
                    plugin.setCallbackRunnable(null);
                }
                player.sendMessage("Authentication successful. Happy mapling!");
            } else {
              player.sendMessage("Invalid code, please try again.");
            }
        } else {
            player.sendMessage("No pending authentication or token not yet received.");
        }
        return true;
    }
}
