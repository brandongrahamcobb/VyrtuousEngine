package com.brandongcobb.lucy.commands;

import com.brandongcobb.lucy.Lucy;
import com.brandongcobb.lucy.utils.handlers.*;
import com.brandongcobb.lucy.utils.sec.PatreonOAuth;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class PatreonCommand implements CommandExecutor {

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player)) return false;
    Player p = (Player) sender;
    Lucy plugin = Lucy.getInstance();

    if (!p.hasPermission("lucy.patreon")) {
      p.sendMessage("You lack permission to link Patreon.");
      return true;
    }

    try {
      BukkitRunnable old = plugin.getCallbackRunnable();
      if (old != null) old.cancel();

      MinecraftUser mu = new MinecraftUser(p.getUniqueId().toString());
      OAuthUserSession session = new OAuthUserSession(plugin, mu, "patreon");
      plugin.getSessions().put(mu, session);

      String state = URLEncoder.encode(p.getUniqueId().toString(), StandardCharsets.UTF_8);
      new PatreonOAuth()
        .completeGetAuthorizationUrl()
        .thenAccept(url ->
          p.sendMessage("Please visit to authorize: " + url + "&state=" + state)
        );

      BukkitRunnable timeout = new BukkitRunnable() {
        @Override public void run() {
          plugin.getSessions().remove(mu);
          p.sendMessage("Patreon auth timed out.");
        }
      };
      plugin.setCallbackRunnable(timeout);
      timeout.runTaskLater(plugin, 20 * 60 * 10);

    } catch (Exception ex) {
      plugin.getLogger().warning("Patreon OAuth error: " + ex.getMessage());
      ex.printStackTrace();
    }

    return true;
  }
}
