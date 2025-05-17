package com.brandongcobb.oauthplugin.commands;

import com.brandongcobb.oauthplugin.OAuthPlugin;
import com.brandongcobb.oauthplugin.utils.handlers.*;
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

        // Look up pending session by player UUID
        OAuthPlugin plugin = OAuthPlugin.getInstance();
        String uuid = player.getUniqueId().toString();
        OAuthUserSession session = plugin.getSessions().get(uuid);

        if (session == null || session.getAccessToken() == null) {
            player.sendMessage("No pending authentication or code received. Start with /discord or /patreon.");
            return true;
        }
        // Verify user provided the same authorization code
        String authCode = session.getAccessToken();
        if (!args[0].equals(authCode)) {
            player.sendMessage("Invalid code, please try again.");
            return true;
        }
        // Consume session and cancel timeout
        plugin.getSessions().remove(uuid);
        BukkitRunnable timeout = session.getTimeoutTask();
        if (timeout != null) timeout.cancel();
        player.sendMessage("Processing authentication...");
        // Initialize UserManager with the plugin's Database instance
        UserManager um = new UserManager(Database.completeGetInstance());
        java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
        if ("discord".equals(session.getCommand())) {
            // Exchange code for token and link Discord
            new com.brandongcobb.oauthplugin.utils.sec.DiscordOAuth()
                .completeExchangeCodeForToken(authCode)
                .thenAccept(accessToken -> {
                    if (accessToken == null || accessToken.isEmpty()) {
                        plugin.getLogger().warning("Discord token exchange returned no access token");
                        player.sendMessage("Failed to link Discord account.");
                        return;
                    }
                    DiscordUser dUser = new DiscordUser(accessToken);
                    long discordId = dUser.getDiscordId();
                    um.createUser(ts, discordId, uuid, "", 0, "", 0L, "", "", "", "")
                      .thenRun(um::consolidateUsers);
                    player.sendMessage("Discord account linked successfully.");
                }).exceptionally(ex -> {
                    plugin.getLogger().warning("Discord token exchange failed: " + ex.getMessage());
                    player.sendMessage("Failed to link Discord account.");
                    return null;
                });
        } else if ("patreon".equals(session.getCommand())) {
            // Exchange code for token and link Patreon
            new com.brandongcobb.oauthplugin.utils.sec.PatreonOAuth()
                .completeExchangeCodeForToken(authCode)
                .thenAccept(accessToken -> {
                    OAuthService svc = new OAuthService();
                    String about = svc.getPatreonAbout(accessToken);
                    int amount = svc.getPatreonAmountCents(accessToken);
                    String email = svc.getPatreonEmail(accessToken);
                    long patreonId = svc.getPatreonId(accessToken);
                    String name = svc.getPatreonName(accessToken);
                    String status = svc.getPatreonStatus(accessToken);
                    String tier = svc.getPatreonTier(accessToken);
                    String vanity = svc.getPatreonVanity(accessToken);
                    um.createUser(ts, 0L, uuid, about, amount, email, patreonId, name, status, tier, vanity)
                      .thenRun(um::consolidateUsers);
                    player.sendMessage("Patreon account linked successfully.");
                }).exceptionally(ex -> {
                    plugin.getLogger().warning("Patreon token exchange failed: " + ex.getMessage());
                    player.sendMessage("Failed to link Patreon account.");
                    return null;
                });
        }
        return true;
    }
}
