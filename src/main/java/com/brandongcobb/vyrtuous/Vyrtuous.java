/*  Vyrtuous.java The primary purpose of this class is to integrate
 *  Discord, LinkedIn, OpenAI, Patreon, Twitch and many more into one
 *  hub.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.brandongcobb.vyrtuous.utils.handlers.*;
import com.brandongcobb.vyrtuous.utils.inc.*;
import com.brandongcobb.vyrtuous.utils.sec.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.net.URLEncoder;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Vyrtuous extends JavaPlugin{

    private static Vyrtuous app;
    private BukkitRunnable callbackRunnable;
    private DiscordOAuth discordOAuth;
    private PatreonOAuth patreonOAuth;
    private Map<MinecraftUser, OAuthUserSession> sessions = new ConcurrentHashMap<>();
    private Logger logger = Logger.getLogger("Vyrtuous");

    public static Vyrtuous getInstance() {
        return app;
    }

    public void onEnable() {
        app = this;
        ConfigManager cm = new ConfigManager(app);
        cm.completeSetAndLoadConfig();
        Database db = new Database();
        OAuthServer oa = new OAuthServer();
        oa.completeConnectSpark().thenRun(() -> {
            DiscordBot bot = new DiscordBot();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    db.completeCloseDatabase();
                    oa.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }).join();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return false;
        Player currentPlayer = (Player)sender;
        if (cmd.getName().equalsIgnoreCase("patreon") || cmd.getName().equalsIgnoreCase("discord")) {
            try {
                if (callbackRunnable != null) callbackRunnable.cancel();
                MinecraftUser minecraftUser = new MinecraftUser(currentPlayer.getUniqueId().toString());
                OAuthUserSession session = new OAuthUserSession(this, minecraftUser, cmd.getName());
                sessions.put(minecraftUser, session);
                String state = URLEncoder.encode(currentPlayer.getUniqueId().toString(), "UTF-8");
                if (cmd.getName().equalsIgnoreCase("patreon")) {
                    PatreonOAuth poa = new PatreonOAuth();
                    poa.completeGetAuthorizationUrl().thenAccept(url -> {
                        String authUrl = url + "&state=" + state;
                        currentPlayer.sendMessage("Please visit the following URL to authorize: " + authUrl);
                    });
                } else {
                    DiscordOAuth doa = new DiscordOAuth();
                    doa.completeGetAuthorizationUrl().thenAccept(url -> {
                        String authUrl = url + "&state=" + state;
                        currentPlayer.sendMessage("Please visit the following URL to authorize: " + authUrl);
                    });
                }
                callbackRunnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        sessions.remove(minecraftUser);
                        currentPlayer.sendMessage("Waiting for callback has timed out.");
                    }
                };
                callbackRunnable.runTaskLater(this, 20 * 60 * 10); // 10 min
                return true;
            } catch (Exception e) {
                logger.warning("Error starting OAuth flow: " + e.getMessage());
            }
        }
        if (cmd.getName().equalsIgnoreCase("code")) {
            MinecraftUser minecraftUser = new MinecraftUser(currentPlayer.getUniqueId().toString());
            if (args.length < 1) {
                sender.sendMessage("Please provide an access code after /code.");
                return false;
            }
            OAuthUserSession session = sessions.get(minecraftUser);
            if (session != null && session.getAccessToken() != null) {
                String providedCode = args[0];
                if (providedCode.equals(session.getAccessToken())) {
                    sessions.remove(currentPlayer.getUniqueId().toString());
                    if (callbackRunnable != null) {
                        callbackRunnable.cancel();
                        callbackRunnable = null;
                    }
                    currentPlayer.sendMessage("Authentication successful. Happy mapling!");
                } else {
                    currentPlayer.sendMessage("Invalid code, please try again.");
                }
            } else {
                sender.sendMessage("No pending authentication or token not yet received.");
            }
            return true;
        }
        return false;
    }
}
