/*  DiscordBot.java The purpose of this program is to incorporate Javacord.
 *  Copyright (C) 2024  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.bots;

import com.brandongcobb.vyrtuous.bots.DiscordBot;
import com.brandongcobb.vyrtuous.cogs.EventListeners;
import com.brandongcobb.vyrtuous.cogs.Cog;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.MessageManager;
import com.brandongcobb.vyrtuous.Vyrtuous;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;
import org.javacord.api.entity.intent.Intent;

public class DiscordBot implements MessageCreateListener {

    private Vyrtuous app;
    private DiscordApi api;
    private String discordApiKey;
    private ConfigManager configManager;
    private HikariDataSource dbPool;
    private Lock lock;
    private static Logger logger;
    private static MessageManager messageManager;

    public DiscordBot(Vyrtuous application) {
        Vyrtuous.discordBot = this;
        this.app = application;
        this.configManager = app.configManager;
        this.discordApiKey = configManager.getNestedConfigValue("api_keys", "Discord").getStringValue("api_key");
        this.dbPool = app.dbPool;
        this.lock = app.lock;
        this.logger = app.logger;
        initiateDiscordApi();
        loadCogs();
    }

    private void initiateDiscordApi() {
        this.api = new DiscordApiBuilder().setToken(discordApiKey).addIntents(Intent.MESSAGE_CONTENT).login().join();
        this.api.addMessageCreateListener(this);
        loadCogs();
    }

    private void loadCogs() {
        List<Cog> cogs = new ArrayList<>();
//        cogs.add(new HybridCommands()); // Add your cogs here
//        cogs.add(new AdministratorCommands()); // Add your cogs here
        cogs.add(new EventListeners(app)); // Add your cogs here
//        cogs.add(new ScheduledTasks()); // Add your cogs here

        for (Cog cog : cogs) {
            cog.register(this.api);
        }
    }

    public DiscordApi getApi() {
        return this.api;
    }

    @Override
    public void onMessageCreate(MessageCreateEvent event) {
        // Handle the incoming message event
    }

    public void start() {
        logger.info("Discord bot started!");
    }
}
