/*  VegBench.java This program is intended to be a software benchmarking
 *  program which evaluates most modern LLM model code suites ability to
 *  a. create a robust, repeatable and efficient program
 *  b. center the program on veganism
 *  This program should contain a variety of applications which emulate
 *  ways to achieve a vegan world sooner like online shop or even a builder
 *  to create online shops easier.
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
package com.brandongcobb.vegan;

import com.brandongcobb.vegan.utils.handlers.*;
import java.io.*;
import java.net.URI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.concurrent.CompletableFuture;

import java.util.logging.Logger;

@SpringBootApplication
public class Application {//extends JavaPlugin {

    private static Application instance;
    private static Logger logger = Logger.getLogger("Application");

    public static Application  getInstance() {
        return instance;
    }

    public static void main(String[] args) {
        ConfigManager cm = new ConfigManager();
        cm.completeSetAndLoadConfig();
        Database db = new Database();
        SpringApplication.run(Application.class, args);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                db.completeCloseDatabase();
            } catch (Exception e) {
                logger.warning(e.getMessage());
            }
        }));
    }
    public void onEnable() {
        instance = this;
    }

    public File getDataFolder() {
        try {
           URI location = Application.class.getProtectionDomain().getCodeSource().getLocation().toURI();
           File currentDir = new File(location).getParentFile();
           return currentDir;
       } catch (Exception e) {
           throw new RuntimeException("Failed to determine or create program folder path", e);
       }
    }
}

