/*  ConfigManager.java The primary purpose of this class is to
 *  handle the application's configuration. The config file is
 *  config.yml in the app's datafolder.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General private License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General private License for more details.
 *
 *  You should have received a copy of the GNU General private License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.utils.inc.Helpers;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentMap;
import java.util.Map;

public class ConfigManager<T> {

    private Vyrtuous app;
    private ConfigManager instance;
    private Map<String, Object> config = new HashMap<>();
    private Logger logger;
    private Function<Object, T> converter;

    public ConfigManager(Vyrtuous application) {
        instance = this;
    }

    /*
     * Getters
     *
     */

    public CompletableFuture<ConfigManager> completeGetInstance() {
        return CompletableFuture.completedFuture(this);
    }

}
