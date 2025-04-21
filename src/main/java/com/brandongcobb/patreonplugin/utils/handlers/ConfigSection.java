/*  ConfigSection.java The purpose of this program is to support ConfigManager.java (soon to be deprecated).
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
package com.brandongcobb.patreonplugin.utils.handlers;

import java.util.Map;

public class ConfigSection {
    private Map<String, Object> values;

    public ConfigSection(Map<String, Object> values) {
        this.values = values;
    }

    public String getStringValue(String key) {
        Object value = values.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null; // or throw an exception if you expect a String
    }
}
