/*  Lucy.java The primary purpose of this class is to integrate
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
package com.brandongcobb.lucy;

import com.brandongcobb.lucy.bots.DiscordBot;
import com.brandongcobb.lucy.commands.*;
import com.brandongcobb.lucy.listeners.*;
import com.brandongcobb.lucy.utils.handlers.*;
import com.brandongcobb.lucy.utils.sec.*;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Lucy extends JavaPlugin{

    private static Lucy app;
    private BukkitRunnable callbackRunnable;
    private DiscordOAuth discordOAuth;
    private PatreonOAuth patreonOAuth;
    private Map<MinecraftUser, OAuthUserSession> sessions = new ConcurrentHashMap<>();
    private Logger logger = Logger.getLogger("Lucy");

    public Map<MinecraftUser, OAuthUserSession> getSessions() {
        return sessions;
    }

    public BukkitRunnable getCallbackRunnable() {
        return callbackRunnable;
    }

    public void setCallbackRunnable(BukkitRunnable cb) {
        this.callbackRunnable = cb;
    }

    public static Lucy getInstance() {
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
            File dataFile = new File(getDataFolder(), "data.yml");
            if (!dataFile.exists()) {
                saveResource("data.yml", false);
            }
            Bukkit.getPluginManager().registerEvents(new EventHandlers(), this);
            getCommand("code").setExecutor(new CodeCommand());
            getCommand("discord").setExecutor(new DiscordCommand());
            getCommand("fly").setExecutor(new FlyCommand());
            getCommand("patreon").setExecutor(new PatreonCommand());
            getCommand("sort").setExecutor(new SortCommand());
            getCommand("sort-secure").setExecutor(new SortSecureCommand());
            getCommand("sort-toggle").setExecutor(new SortToggleCommand());
            getCommand("sort-type"  ).setExecutor(new SortTypeCommand());
            getCommand("speed").setExecutor(new SpeedCommand());
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
