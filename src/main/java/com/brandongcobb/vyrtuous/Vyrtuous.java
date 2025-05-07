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
import com.brandongcobb.vyrtuous.metadata.MetadataContainer;
import com.brandongcobb.vyrtuous.utils.handlers.Database;
import com.brandongcobb.vyrtuous.utils.handlers.ConfigManager;
import com.brandongcobb.vyrtuous.utils.handlers.MinecraftUser;
import com.brandongcobb.vyrtuous.utils.handlers.OAuthServer;
import com.brandongcobb.vyrtuous.utils.handlers.OAuthUserSession;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletionException;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import net.dv8tion.jda.api.JDA;

public class Vyrtuous {

    public static void main(String[] args) {
        Vyrtuous app = new Vyrtuous();
        ConfigManager cm = new ConfigManager(app);
        cm.completeSetAndLoadConfig().thenRun(() -> {
            Database db = new Database(cm);
            DiscordBot bot = new DiscordBot(cm);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    db.completeCloseDatabase();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }).join(); // Block main thread here
    }
}
