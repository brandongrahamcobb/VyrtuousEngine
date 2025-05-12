/*  MetadataType.java The primary purpose of this interface is to
 *  be a contract for describing the type of a metadata value.
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
package com.brandongcobb.metadata;

public interface MetadataType<T> {

    Class<T> getType();

    default MetadataCodec<T> getCodec() {
        throw new UnsupportedOperationException("No codec available of this type.");
    }

    default MetadataRenderer<T> getRenderer() {
        throw new UnsupportedOperationException("No codec available of this type.");
    }

    default MetadataBehavior<T> getBehavior() {
        throw new UnsupportedOperationException("No behavior defined for this type.");
    }
}
