/*  OAuthPlugin.java The primary purpose of this class is to integrate
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
package com.brandongcobb.oauthplugin;

import com.brandongcobb.oauthplugin.bots.DiscordBot;
import com.brandongcobb.oauthplugin.commands.*;
import com.brandongcobb.oauthplugin.listeners.*;
import com.brandongcobb.oauthplugin.utils.handlers.*;
import com.brandongcobb.oauthplugin.utils.sec.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class OAuthPlugin extends JavaPlugin{

    private static OAuthPlugin app;
    // Holds pending OAuth sessions keyed by player UUID
    private Map<String, OAuthUserSession> sessions = new ConcurrentHashMap<>();
    private Logger logger = Logger.getLogger("OAuthPlugin");

    /**
     * Pending OAuth sessions, keyed by player's UUID string.
     */
    public Map<String, OAuthUserSession> getSessions() {
        return sessions;
    }


    public static OAuthPlugin getInstance() {
        return app;
    }

    public class NotEnoughArgumentsException extends Exception {
        public NotEnoughArgumentsException() {
            super("Not enough arguments provided.");
        }
    }
 
    public void onEnable() {
        app = this;
        ConfigManager cm = new ConfigManager(app);
        cm.completeSetAndLoadConfig();
        Database db = new Database();
        OAuthServer oa = new OAuthServer();
        oa.completeConnectSpark().thenRun(() -> {
//            File dataFile = new File(getDataFolder(), "config.yml");
//            if (!dataFile.exists()) {
//                saveResource("config.yml", false);
//            }
            Bukkit.getPluginManager().registerEvents(new EventHandlers(), this);
            getCommand("code").setExecutor(new CodeCommand());
            getCommand("discord").setExecutor(new DiscordCommand());
            getCommand("patreon").setExecutor(new PatreonCommand());
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
}
