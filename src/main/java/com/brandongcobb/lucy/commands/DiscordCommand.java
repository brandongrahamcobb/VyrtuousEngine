package com.brandongcobb.lucy.commands;

import com.brandongcobb.lucy.Lucy;
import com.brandongcobb.lucy.utils.handlers.*;
import com.brandongcobb.lucy.utils.sec.DiscordOAuth;
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
    Lucy plugin = Lucy.getInstance();

    // permission check
    if (!p.hasPermission("lucy.discord")) {
      p.sendMessage("You lack permission to link Discord.");
      return true;
    }

    try {
      // cancel any existing timeout
      BukkitRunnable old = plugin.getCallbackRunnable();
      if (old != null) old.cancel();

      MinecraftUser mu = new MinecraftUser(p.getUniqueId().toString());
      OAuthUserSession session = new OAuthUserSession(plugin, mu, "discord");
      plugin.getSessions().put(mu, session);

      String state = URLEncoder.encode(p.getUniqueId().toString(), StandardCharsets.UTF_8);
      new DiscordOAuth()
        .completeGetAuthorizationUrl()
        .thenAccept(url ->
          p.sendMessage("Please visit to authorize: " + url + "&state=" + state)
        );

      BukkitRunnable timeout = new BukkitRunnable() {
        @Override public void run() {
          plugin.getSessions().remove(mu);
          p.sendMessage("Discord auth timed out.");
        }
      };
      plugin.setCallbackRunnable(timeout);
      timeout.runTaskLater(plugin, 20 * 60 * 10); // 10m

    } catch (Exception ex) {
      plugin.getLogger().warning("Discord OAuth error: " + ex.getMessage());
      ex.printStackTrace();
    }

    return true;
  }
}
