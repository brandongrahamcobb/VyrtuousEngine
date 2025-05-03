/*  MetadataKey.java The primary purpose of this class is to
 *  be an object with a key whos name and type are known.
 *
 *  Copyright (C) 2024  github.com/brandongrahamcobb
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
package com.brandongcobb.vyrtuous.metadata;

import java.util.Objects;


/**
 * A unique key that identifies a metadata entry, parameterized by the type
 * of its associated value.
 *
 * <p>Each {@code MetadataKey<T>} has a human-readable name and a
 * {@code Type<T>} descriptor that specifies the Java type of the metadata value.
 *
 * @param <T> the Java type of the metadata value
 */
 public final class MetadataKey<T> {
 
    private final String name;
    private final Class<?> type;
 
    /**
     * Creates a new metadata key.
     *
     * @param name the unique name for this metadata key
     * @param type a descriptor of the Java type for values stored under this key
     */
    public MetadataKey(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the unique name of this metadata key.
     *
     * @return the name of the key
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the {@code Type<T>} descriptor that specifies the Java type of
     * values associated with this key.
     *
     * @return the type descriptor for this key
     */
    public Class<?> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetadataKey)) return false;
        MetadataKey<?> that = (MetadataKey<?>) o;
        return name.equals(that.name); // Optional: && type.equals(that.type)
    }

    @Override
    public int hashCode() {
        return Objects.hash(name); // Optional: include type
    }
}
